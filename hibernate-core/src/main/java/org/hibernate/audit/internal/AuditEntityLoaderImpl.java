/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.internal;

import org.hibernate.LockOptions;
import org.hibernate.audit.AuditLog;
import org.hibernate.audit.ModificationType;
import org.hibernate.audit.spi.AuditEntityLoader;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.query.sqm.ComparisonOperator.NOT_EQUAL;

/**
 * Pre-built SQL AST load plan for {@code AuditLog.find()}.
 * <p>
 * Two {@link JdbcSelect} variants are built at construction time:
 * one excluding deletions ({@code REVTYPE <> DEL}), one including all.
 */
public class AuditEntityLoaderImpl implements AuditEntityLoader {
	private final EntityMappingType entityMappingType;
	private final SelectableMapping revMapping;
	private final JdbcParametersList jdbcParams;
	private final JdbcSelect excludingDeletions;
	private final JdbcSelect includingDeletions;

	public AuditEntityLoaderImpl(EntityMappingType entityMappingType, SessionFactoryImplementor sessionFactory) {
		this.entityMappingType = entityMappingType;

		final var auditMapping = entityMappingType.getAuditMapping();
		final var revTableName = entityMappingType.getMappedTableDetails().getTableName();
		final var revTypeTableName = entityMappingType.getIdentifierTableDetails().getTableName();
		this.revMapping = auditMapping.getChangesetIdMapping( revTableName );
		final var revTypeMapping = auditMapping.getModificationTypeMapping( revTypeTableName );

		// Build SQL AST: SELECT ... WHERE id = ? AND REV = (SELECT MAX(REV) ... WHERE REV <= ?)
		// Uses ALL_REVISIONS so LoaderSelectBuilder doesn't add its own temporal restriction,
		// then applies the audit mapping's restriction with our own JDBC parameter.
		final var influencers = new LoadQueryInfluencers( sessionFactory );
		influencers.setTemporalIdentifier( AuditLog.ALL_REVISIONS );
		final var paramsBuilder = JdbcParametersList.newBuilder();
		final var sqlAliasBaseManager = new SqlAliasBaseManager();
		final var sqlAst = LoaderSelectBuilder.createSelect(
				entityMappingType,
				null,
				entityMappingType.getIdentifierMapping(),
				null,
				1,
				influencers,
				LockOptions.NONE,
				paramsBuilder::add,
				sqlAliasBaseManager,
				sessionFactory
		);
		final var querySpec = sqlAst.getQueryPart().getFirstQuerySpec();
		final var rootTableGroup = querySpec.getFromClause().getRoots().get( 0 );
		final var navPath = rootTableGroup.getNavigablePath();
		final var revTableRef = rootTableGroup.resolveTableReference( navPath, revTableName );

		// Build MAX(REV) WHERE REV <= ? restriction without REVTYPE filter
		final var revParam = new SqlTypedMappingJdbcParameter( revMapping );
		paramsBuilder.add( revParam );
		final var keySelectables = new ArrayList<SelectableMapping>();
		entityMappingType.getIdentifierMapping()
				.forEachSelectable( (index, mapping) -> keySelectables.add( mapping ) );
		querySpec.applyPredicate( auditMapping.createRestriction(
				entityMappingType,
				revTableRef,
				keySelectables,
				sqlAliasBaseManager,
				revTableName,
				revParam,
				true // includeDeletions: no REVTYPE filter
		) );

		// Translate once: including deletions (no REVTYPE filter)
		this.jdbcParams = paramsBuilder.build();
		this.includingDeletions = translate( sqlAst, sessionFactory );

		// Add REVTYPE <> DEL predicate and translate again: excluding deletions
		// (REVTYPE may be null for JOINED subclass tables where it only exists on root)
		if ( revTypeMapping != null ) {
			final var revTypeTableRef = rootTableGroup.resolveTableReference( navPath, revTypeTableName );
			querySpec.applyPredicate( new ComparisonPredicate(
					new ColumnReference( revTypeTableRef, revTypeMapping ),
					NOT_EQUAL,
					new JdbcLiteral<>( ModificationType.DEL, revTypeMapping.getJdbcMapping() )
			) );
		}
		this.excludingDeletions = translate( sqlAst, sessionFactory );
	}

	@Override
	public <T> T find(
			Object id,
			Object changesetId,
			boolean includeDeletions,
			SharedSessionContractImplementor session) {
		final var select = includeDeletions ? includingDeletions : excludingDeletions;
		return execute( select, id, changesetId, session );
	}

	private static JdbcSelect translate(SelectStatement sqlAst, SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getJdbcServices().getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( null, QueryOptions.NONE );
	}

	// --- execution ---

	private <T> T execute(
			JdbcSelect select,
			Object id,
			Object transactionId,
			SharedSessionContractImplementor session) {
		final var bindings = new JdbcParameterBindingsImpl( jdbcParams.size() );
		int offset = bindings.registerParametersForEachJdbcValue(
				id, 0,
				entityMappingType.getIdentifierMapping(),
				jdbcParams, session
		);
		bindings.addBinding(
				jdbcParams.get( offset ),
				new JdbcParameterBindingImpl( revMapping.getJdbcMapping(), transactionId )
		);

		final var callback = new CallbackImpl();
		final List<T> list = session.getJdbcServices().getJdbcSelectExecutor().list(
				select,
				bindings,
				new BaseExecutionContext( session ) {
					@Override
					public Object getEntityId() {
						return id;
					}

					@Override
					public EntityMappingType getRootEntityDescriptor() {
						return entityMappingType.getRootEntityDescriptor();
					}

					@Override
					public Callback getCallback() {
						return callback;
					}
				},
				RowTransformerStandardImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.FILTER,
				1
		);

		if ( list.isEmpty() ) {
			return null;
		}
		final T entity = list.get( 0 );
		callback.invokeAfterLoadActions( entity, entityMappingType, session );
		return entity;
	}
}
