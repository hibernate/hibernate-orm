/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal.lock;

import org.hibernate.AssertionFailure;
import org.hibernate.ScrollMode;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.StandardStatementCreator;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hibernate.sql.ops.internal.DatabaseOperationLogging.DB_OP_DEBUG_ENABLED;
import static org.hibernate.sql.ops.internal.DatabaseOperationLogging.DB_OP_LOGGER;

/**
 * @author Steve Ebersole
 */
class TableSegment {
	private final TableDetails tableDetails;
	private final EntityMappingType entityMappingType;

	private final QuerySpec querySpec = new QuerySpec( true );

	private final NavigablePath rootPath;
	private final TableReference tableReference;
	private final TableGroup tableGroup;

	private final FollowOnLockingCreationStates creationStates;

	private final List<Integer> statePositions = new ArrayList<>();
	private final List<DomainResult<?>> domainResults = new ArrayList<>();

	private final JdbcParameterBindings jdbcParameterBindings;

	public TableSegment(
			TableDetails tableDetails,
			EntityMappingType entityMappingType,
			List<EntityKey> entityKeys,
			SharedSessionContractImplementor session) {
		if ( DB_OP_DEBUG_ENABLED ) {
			DB_OP_LOGGER.debugf( "Adding table `%s` for follow-on locking - %s", tableDetails.getTableName(), entityMappingType.getEntityName() );
		}

		this.tableDetails = tableDetails;

		this.rootPath = new NavigablePath( tableDetails.getTableName() );
		this.tableReference = new NamedTableReference( tableDetails.getTableName(), "tbl" );
		this.entityMappingType = entityMappingType;
		this.tableGroup = new SimpleTableGroup(
				tableReference,
				tableDetails.getTableName(),
				entityMappingType
		);

		querySpec.getFromClause().addRoot( tableGroup );

		creationStates = new FollowOnLockingCreationStates( querySpec, tableGroup,
				entityMappingType.getEntityPersister().getFactory() );

		final DomainResult<Object> idDomainResult = entityMappingType.getIdentifierMapping().createDomainResult(
				rootPath.append( "{key}" ),
				tableGroup,
				null,
				creationStates
		);
		domainResults.add( idDomainResult );

		final int expectedParamCount = entityKeys.size() * entityMappingType.getIdentifierMapping().getJdbcTypeCount();
		jdbcParameterBindings = new JdbcParameterBindingsImpl( expectedParamCount );

		applyKeyRestrictions( entityKeys, session );
	}

	public TableDetails getTableDetails() {
		return tableDetails;
	}

	public NavigablePath getRootPath() {
		return rootPath;
	}

	public TableReference getTableReference() {
		return tableReference;
	}

	public TableGroup getTableGroup() {
		return tableGroup;
	}

	public FollowOnLockingCreationStates getCreationStates() {
		return creationStates;
	}

	public void applyDomainResult(int index, AttributeMapping attributeMapping) {
		final DomainResult<Object> domainResult = attributeMapping.createDomainResult(
				rootPath.append( attributeMapping.getPartName() ),
				tableGroup,
				null,
				creationStates
		);
		domainResults.add( domainResult );
		statePositions.add( index );
	}

	public void applyKeyRestrictions(List<EntityKey> entityKeys, SharedSessionContractImplementor session) {
		if ( entityMappingType.getIdentifierMapping().getJdbcTypeCount() == 1 ) {
			applySimpleKeyRestriction( entityKeys, session );
		}
		else {
			applyCompositeKeyRestriction( entityKeys, session );
		}
	}

	private void applySimpleKeyRestriction(List<EntityKey> entityKeys, SharedSessionContractImplementor session) {
		final EntityIdentifierMapping identifierMapping = entityMappingType.getIdentifierMapping();

		final TableDetails.KeyColumn keyColumn = tableDetails.getKeyDetails().getKeyColumn( 0 );
		final ColumnReference columnReference = new ColumnReference( tableReference, keyColumn );

		final InListPredicate restriction = new InListPredicate( columnReference );
		querySpec.applyPredicate( restriction );

		entityKeys.forEach( (entityKey) -> identifierMapping.breakDownJdbcValues(
				entityKey.getIdentifierValue(),
				(valueIndex, value, jdbcValueMapping) -> {
					final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl(
							jdbcValueMapping.getJdbcMapping() );
					restriction.addExpression( jdbcParameter );

					jdbcParameterBindings.addBinding(
							jdbcParameter,
							new JdbcParameterBindingImpl( jdbcValueMapping.getJdbcMapping(), value )
					);
				},
				session
		) );
	}

	private void applyCompositeKeyRestriction(List<EntityKey> entityKeys, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	public void performActions(Map<Object, EntityDetails> entityDetailsMap, ExecutionContext lockingExecutionContext) {
		final SessionFactoryImplementor sessionFactory = lockingExecutionContext.getSession().getSessionFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

		final SelectStatement selectStatement = new SelectStatement( querySpec, domainResults );
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcServices.getDialect().getSqlAstTranslatorFactory();
		final SqlAstTranslator<JdbcOperationQuerySelect> translator = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, selectStatement );
		final JdbcOperationQuerySelect jdbcOperation = translator.translate( jdbcParameterBindings, lockingExecutionContext.getQueryOptions() );

		final JdbcSelectExecutor jdbcSelectExecutor = jdbcServices.getJdbcSelectExecutor();
		final List<Object[]> results = jdbcSelectExecutor.executeQuery(
				jdbcOperation,
				jdbcParameterBindings,
				lockingExecutionContext,
				row -> row,
				Object[].class,
				StandardStatementCreator.getStatementCreator( ScrollMode.FORWARD_ONLY ),
				ListResultsConsumer.instance( ListResultsConsumer.UniqueSemantic.ALLOW )
		);

		if ( CollectionHelper.isEmpty( results ) ) {
			throw new AssertionFailure( "Expecting results" );
		}

		results.forEach( (row) -> {
			final Object id = row[0];
			final EntityDetails entityDetails = entityDetailsMap.get( id );
			for ( int i = 0; i < statePositions.size(); i++ ) {
				final int statePosition = statePositions.get( i );
				// offset 1 because of the id at position 0
				final Object stateValue = row[ i + 1 ];
				entityDetails.entry().getLoadedState()[statePosition] = stateValue;
				entityDetails.key().getPersister().getAttributeMapping( statePosition ).setValue( entityDetails.instance(), stateValue );
			}
		} );
	}
}
