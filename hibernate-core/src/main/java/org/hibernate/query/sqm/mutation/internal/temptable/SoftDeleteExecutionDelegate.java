/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.MutatingTableReferenceGroupWrapper;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateCollector;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter.omittingLockingAndPaging;

/**
 * @author Steve Ebersole
 */
public class SoftDeleteExecutionDelegate extends AbstractDeleteExecutionDelegate {
	public SoftDeleteExecutionDelegate(
			EntityMappingType entityDescriptor,
			TemporaryTable idTable,
			TemporaryTableStrategy temporaryTableStrategy,
			boolean forceDropAfterUse,
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			QueryParameterBindings queryParameterBindings,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			SessionFactoryImplementor sessionFactory) {
		super(
				entityDescriptor,
				idTable,
				temporaryTableStrategy,
				forceDropAfterUse,
				sqmDelete,
				domainParameterXref,
				queryOptions,
				loadQueryInfluencers,
				queryParameterBindings,
				sessionUidAccess,
				sessionFactory
		);
	}

	@Override
	public int execute(DomainQueryExecutionContext domainQueryExecutionContext) {
		final String targetEntityName = getSqmDelete().getTarget().getEntityName();
		final EntityPersister targetEntityDescriptor =
				getSessionFactory().getMappingMetamodel()
						.getEntityDescriptor( targetEntityName );

		final EntityMappingType rootEntityDescriptor = targetEntityDescriptor.getRootEntityDescriptor();

		// determine if we need to use a sub-query for matching ids -
		//		1. if the target is not the root we will
		//		2. if the supplied predicate (if any) refers to columns from a table
		//			other than the identifier table we will
		final SqmJdbcExecutionContextAdapter executionContext = omittingLockingAndPaging( domainQueryExecutionContext );

		final TableGroup deletingTableGroup = getConverter().getMutatingTableGroup();
		final TableDetails softDeleteTable = rootEntityDescriptor.getSoftDeleteTableDetails();
		final NamedTableReference rootTableReference = (NamedTableReference) deletingTableGroup.resolveTableReference(
				deletingTableGroup.getNavigablePath(),
				softDeleteTable.getTableName()
		);
		assert rootTableReference != null;

		// NOTE : `converter.visitWhereClause` already applies the soft-delete restriction
		final Predicate specifiedRestriction = getConverter().visitWhereClause( getSqmDelete().getWhereClause() );

		final PredicateCollector predicateCollector = new PredicateCollector( specifiedRestriction );
		targetEntityDescriptor.applyBaseRestrictions(
				predicateCollector,
				deletingTableGroup,
				true,
				executionContext.getSession().getLoadQueryInfluencers().getEnabledFilters(),
				false,
				null,
				getConverter()
		);

		getConverter().pruneTableGroupJoins();
		final ColumnReferenceCheckingSqlAstWalker walker = new ColumnReferenceCheckingSqlAstWalker(
				rootTableReference.getIdentificationVariable()
		);
		if ( predicateCollector.getPredicate() != null ) {
			predicateCollector.getPredicate().accept( walker );
		}

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				getDomainParameterXref(),
				SqmUtil.generateJdbcParamsXref( getDomainParameterXref(), getConverter() ),
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) getConverter().getSqmParameterMappingModelExpressibleResolutions().get(parameter);
					}
				},
				executionContext.getSession()
		);

		final boolean needsSubQuery = !walker.isAllColumnReferencesFromIdentificationVariable()
				|| targetEntityDescriptor != rootEntityDescriptor;
		if ( needsSubQuery ) {
			if ( getSessionFactory().getJdbcServices().getDialect().supportsSubqueryOnMutatingTable() ) {
				return performDeleteWithSubQuery(
						rootEntityDescriptor,
						deletingTableGroup,
						rootTableReference,
						predicateCollector,
						jdbcParameterBindings,
						getConverter(),
						executionContext
				);
			}
			else {
				return performDeleteWithIdTable(
						rootEntityDescriptor,
						rootTableReference,
						predicateCollector,
						jdbcParameterBindings,
						executionContext
				);
			}
		}
		else {
			return performDirectDelete(
					rootEntityDescriptor,
					rootTableReference,
					predicateCollector,
					jdbcParameterBindings,
					executionContext
			);
		}
	}

	private int performDeleteWithIdTable(
			EntityMappingType rootEntityDescriptor,
			NamedTableReference targetTableReference,
			PredicateCollector predicateCollector,
			JdbcParameterBindings jdbcParameterBindings,
			SqmJdbcExecutionContextAdapter executionContext) {
		ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions(
				getIdTable(),
				getTemporaryTableStrategy(),
				executionContext
		);

		try {
			return deleteUsingIdTable(
					rootEntityDescriptor,
					targetTableReference,
					predicateCollector,
					jdbcParameterBindings,
					executionContext
			);
		}
		finally {
			ExecuteWithTemporaryTableHelper.performAfterTemporaryTableUseActions(
					getIdTable(),
					getSessionUidAccess(),
					getAfterUseAction(),
					executionContext
			);
		}
	}

	private int deleteUsingIdTable(
			EntityMappingType rootEntityDescriptor,
			NamedTableReference targetTableReference,
			PredicateCollector predicateCollector,
			JdbcParameterBindings jdbcParameterBindings,
			SqmJdbcExecutionContextAdapter executionContext) {
		final int rows = ExecuteWithTemporaryTableHelper.saveMatchingIdsIntoIdTable(
				getConverter(),
				predicateCollector.getPredicate(),
				getIdTable(),
				getSessionUidAccess(),
				jdbcParameterBindings,
				executionContext
		);

		final QuerySpec idTableIdentifierSubQuery = ExecuteWithTemporaryTableHelper.createIdTableSelectQuerySpec(
				getIdTable(),
				getSessionUidAccess(),
				getEntityDescriptor(),
				executionContext
		);

		SqmMutationStrategyHelper.cleanUpCollectionTables(
				getEntityDescriptor(),
				(tableReference, attributeMapping) -> {
					final ForeignKeyDescriptor fkDescriptor = attributeMapping.getKeyDescriptor();
					final QuerySpec idTableFkSubQuery;
					if ( fkDescriptor.getTargetPart().isEntityIdentifierMapping() ) {
						idTableFkSubQuery = idTableIdentifierSubQuery;
					}
					else {
						idTableFkSubQuery = ExecuteWithTemporaryTableHelper.createIdTableSelectQuerySpec(
								getIdTable(),
								fkDescriptor.getTargetPart(),
								getSessionUidAccess(),
								getEntityDescriptor(),
								executionContext
						);
					}
					return new InSubQueryPredicate(
							MappingModelCreationHelper.buildColumnReferenceExpression(
									new MutatingTableReferenceGroupWrapper(
											new NavigablePath( attributeMapping.getRootPathName() ),
											attributeMapping,
											(NamedTableReference) tableReference
									),
									fkDescriptor,
									null,
									getSessionFactory()
							),
							idTableFkSubQuery,
							false
					);

				},
				JdbcParameterBindings.NO_BINDINGS,
				executionContext
		);

		final Assignment softDeleteAssignment = rootEntityDescriptor
				.getSoftDeleteMapping()
				.createSoftDeleteAssignment( targetTableReference );
		final Expression idExpression = createIdExpression( rootEntityDescriptor, targetTableReference );
		final UpdateStatement updateStatement = new UpdateStatement(
				targetTableReference,
				singletonList( softDeleteAssignment ),
				new InSubQueryPredicate( idExpression, idTableIdentifierSubQuery, false )
		);

		executeUpdate( updateStatement, jdbcParameterBindings, executionContext );

		return rows;
	}

	private static Expression createIdExpression(EntityMappingType rootEntityDescriptor, NamedTableReference targetTableReference) {
		final TableDetails softDeleteTable = rootEntityDescriptor.getSoftDeleteTableDetails();
		final TableDetails.KeyDetails keyDetails = softDeleteTable.getKeyDetails();
		final List<Expression> idExpressions = new ArrayList<>( keyDetails.getColumnCount() );
		keyDetails.forEachKeyColumn( (position, column) -> idExpressions.add(
				new ColumnReference( targetTableReference, column )
		) );
		final Expression idExpression = idExpressions.size() == 1
				? idExpressions.get( 0 )
				: new SqlTuple( idExpressions, rootEntityDescriptor.getIdentifierMapping() );
		return idExpression;
	}

	private int performDeleteWithSubQuery(
			EntityMappingType rootEntityDescriptor,
			TableGroup deletingTableGroup,
			NamedTableReference rootTableReference,
			PredicateCollector predicateCollector,
			JdbcParameterBindings jdbcParameterBindings,
			MultiTableSqmMutationConverter converter,
			SqmJdbcExecutionContextAdapter executionContext) {
		final QuerySpec matchingIdSubQuery = new QuerySpec( false, 1 );
		matchingIdSubQuery.getFromClause().addRoot( deletingTableGroup );

		final TableDetails identifierTableDetails = rootEntityDescriptor.getIdentifierTableDetails();
		final TableDetails.KeyDetails keyDetails = identifierTableDetails.getKeyDetails();

		final NamedTableReference targetTable = new NamedTableReference(
				identifierTableDetails.getTableName(),
				DeleteStatement.DEFAULT_ALIAS,
				false
		);

		final List<Expression> idExpressions = new ArrayList<>( keyDetails.getColumnCount() );
		keyDetails.forEachKeyColumn( (position, column) -> {
			final Expression columnReference = converter.getSqlExpressionResolver().resolveSqlExpression(
					rootTableReference,
					column
			);
			matchingIdSubQuery.getSelectClause().addSqlSelection(
					new SqlSelectionImpl( position, columnReference )
			);
			idExpressions.add( new ColumnReference( targetTable, column ) );
		} );

		matchingIdSubQuery.applyPredicate( predicateCollector.getPredicate() );
		final Expression idExpression = idExpressions.size() == 1
				? idExpressions.get( 0 )
				: new SqlTuple( idExpressions, rootEntityDescriptor.getIdentifierMapping() );

		final Assignment softDeleteAssignment = rootEntityDescriptor
				.getSoftDeleteMapping()
				.createSoftDeleteAssignment( targetTable );

		final UpdateStatement updateStatement = new UpdateStatement(
				targetTable,
				singletonList( softDeleteAssignment ),
				new InSubQueryPredicate( idExpression, matchingIdSubQuery, false )
		);

		return executeUpdate( updateStatement, jdbcParameterBindings, executionContext );
	}

	private int performDirectDelete(
			EntityMappingType rootEntityDescriptor,
			NamedTableReference rootTableReference,
			PredicateCollector predicateCollector,
			JdbcParameterBindings jdbcParameterBindings,
			SqmJdbcExecutionContextAdapter executionContext) {
		final Assignment softDeleteAssignment = rootEntityDescriptor
				.getSoftDeleteMapping()
				.createSoftDeleteAssignment( rootTableReference );

		final UpdateStatement updateStatement = new UpdateStatement(
				rootTableReference,
				singletonList( softDeleteAssignment ),
				predicateCollector.getPredicate()
		);

		return executeUpdate( updateStatement, jdbcParameterBindings, executionContext );
	}

	private int executeUpdate(
			UpdateStatement updateStatement,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();
		final JdbcServices jdbcServices = factory.getJdbcServices();

		final JdbcOperationQueryMutation jdbcUpdate = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, updateStatement )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcUpdate,
				jdbcParameterBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContext
		);
	}
}
