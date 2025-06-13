/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.loader.ast.internal.LoaderSqlAstCreationState;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.spi.SingleResultConsumer;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public class SqlAstBasedLockingStrategy implements LockingStrategy {
	private final EntityMappingType entityToLock;
	private final LockMode lockMode;
	private final Locking.Scope lockScope;

	public SqlAstBasedLockingStrategy(EntityPersister lockable, LockMode lockMode, Locking.Scope lockScope) {
		this.entityToLock = lockable.getRootEntityDescriptor();
		this.lockMode = lockMode;
		this.lockScope = lockScope;
	}

	@Override
	public void lock(
			Object id,
			Object version,
			Object object,
			int timeout,
			SharedSessionContractImplementor session) throws StaleObjectStateException, LockingStrategyException {
		if ( session instanceof EventSource eventSource ) {
			doLock( id, version, timeout, eventSource );
		}
		else {
			throw new UnsupportedOperationException( "Optimistic locking strategies not supported in stateless session" );
		}
	}

	private void doLock(Object id, Object version, int timeout, EventSource eventSource) {
		final SessionFactoryImplementor factory = eventSource.getFactory();

		final LockOptions lockOptions = new LockOptions( lockMode );
		lockOptions.setScope( lockScope );
		lockOptions.setTimeOut( timeout );

		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final NavigablePath entityPath = new NavigablePath( entityToLock.getRootPathName() );

		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				(fetchParent, creationState) -> {
					return null;
				},
				true,
				new LoadQueryInfluencers( factory ),
				factory.getSqlTranslationEngine()
		);

		final TableGroup rootTableGroup = entityToLock.createRootTableGroup(
				true,
				entityPath,
				null,
				null,
				() -> p -> {
				},
				sqlAstCreationState
		);
		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( entityPath, rootTableGroup );

		final EntityIdentifierMapping idMapping = entityToLock.getIdentifierMapping();
		final DomainResult<?> idResult = idMapping.createDomainResult(
				entityPath.append( idMapping.getPartName() ),
				rootTableGroup,
				null,
				sqlAstCreationState
		);

		final int jdbcParamCount = entityToLock.getVersionMapping() != null
				? idMapping.getJdbcTypeCount() + 1
				: idMapping.getJdbcTypeCount();
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParamCount );
		idMapping.breakDownJdbcValues(
				id,
				(valueIndex, value, jdbcValueMapping) -> handleRestriction(
						value,
						jdbcValueMapping,
						rootQuerySpec,
						sqlAstCreationState,
						rootTableGroup,
						jdbcParameterBindings
				),
				eventSource
		);

		if ( entityToLock.getVersionMapping() != null ) {
			entityToLock.getVersionMapping().breakDownJdbcValues(
					version,
					(valueIndex, value, jdbcValueMapping) -> handleRestriction(
							value,
							jdbcValueMapping,
							rootQuerySpec,
							sqlAstCreationState,
							rootTableGroup,
							jdbcParameterBindings
					),
					eventSource
			);
		}

		final SelectStatement selectStatement = new SelectStatement( rootQuerySpec, List.of( idResult ) );
		final JdbcOperationQuerySelect selectOperation = eventSource
				.getDialect()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( factory, selectStatement )
				.translate( jdbcParameterBindings, sqlAstCreationState );

		final JdbcSelectExecutor jdbcSelectExecutor = factory.getJdbcServices().getJdbcSelectExecutor();
		final LockingExecutionContext lockingExecutionContext = new LockingExecutionContext( eventSource );
		jdbcSelectExecutor.executeQuery(
				selectOperation,
				jdbcParameterBindings,
				lockingExecutionContext,
				null,
				idResult.getResultJavaType().getJavaTypeClass(),
				1,
				SingleResultConsumer.instance()
		);
	}

	private static void handleRestriction(Object value, SelectableMapping jdbcValueMapping, QuerySpec rootQuerySpec, LoaderSqlAstCreationState sqlAstCreationState, TableGroup rootTableGroup, JdbcParameterBindings jdbcParameterBindings) {
		final JdbcMapping jdbcMapping = jdbcValueMapping.getJdbcMapping();
		final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );
		rootQuerySpec.applyPredicate(
				new ComparisonPredicate(
						sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
								rootTableGroup.getTableReference( jdbcValueMapping.getContainingTableExpression() ),
								jdbcValueMapping
						),
						ComparisonOperator.EQUAL,
						jdbcParameter
				)
		);

		final JdbcParameterBindingImpl jdbcParameterBinding = new JdbcParameterBindingImpl( jdbcMapping, value );
		jdbcParameterBindings.addBinding( jdbcParameter, jdbcParameterBinding );
	}

}
