/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.mutation.internal.TableKeyExpressionCollector;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.MutatingTableReferenceGroupWrapper;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateCollector;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

import static org.hibernate.query.sqm.mutation.internal.MutationQueryLogging.MUTATION_QUERY_LOGGER;

/**
 * @author Steve Ebersole
 */
public class RestrictedDeleteExecutionDelegate extends AbstractDeleteExecutionDelegate {
	public RestrictedDeleteExecutionDelegate(
			EntityMappingType entityDescriptor,
			TemporaryTable idTable,
			AfterUseAction afterUseAction,
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
				afterUseAction,
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
	public int execute(DomainQueryExecutionContext executionContext) {
		final EntityPersister entityDescriptor = getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( getSqmDelete().getTarget().getEntityName() );
		final String hierarchyRootTableName = entityDescriptor.getTableName();

		final TableGroup deletingTableGroup = getConverter().getMutatingTableGroup();

		final TableReference hierarchyRootTableReference = deletingTableGroup.resolveTableReference(
				deletingTableGroup.getNavigablePath(),
				hierarchyRootTableName
		);
		assert hierarchyRootTableReference != null;

		// Use the converter to interpret the where-clause.  We do this for 2 reasons:
		//		1) the resolved Predicate is ultimately the base for applying restriction to the deletes
		//		2) we also inspect each ColumnReference that is part of the where-clause to see which
		//			table it comes from.  if all of the referenced columns (if any at all) are from the root table
		//			we can perform all of the deletes without using an id-table
		final Predicate specifiedRestriction = getConverter().visitWhereClause( getSqmDelete().getWhereClause() );

		final PredicateCollector predicateCollector = new PredicateCollector( specifiedRestriction );
		entityDescriptor.applyBaseRestrictions(
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
				hierarchyRootTableReference.getIdentificationVariable()
		);
		if ( predicateCollector.getPredicate() != null ) {
			predicateCollector.getPredicate().accept( walker );
		}

		// We need an id table if we want to delete from an intermediate table to avoid FK violations
		// The intermediate table has a FK to the root table, so we can't delete from the root table first
		// Deleting from the intermediate table first also isn't possible,
		// because that is the source for deletion in other tables, hence we need an id table
		final boolean needsIdTable = !walker.isAllColumnReferencesFromIdentificationVariable()
				|| entityDescriptor != entityDescriptor.getRootEntityDescriptor();

		final SqmJdbcExecutionContextAdapter executionContextAdapter = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext );

		if ( needsIdTable ) {
			return executeWithIdTable(
					predicateCollector.getPredicate(),
					deletingTableGroup,
					getConverter().getJdbcParamsBySqmParam(),
					getConverter().getSqmParameterMappingModelExpressibleResolutions(),
					executionContextAdapter
			);
		}
		else {
			return executeWithoutIdTable(
					predicateCollector.getPredicate(),
					deletingTableGroup,
					getConverter().getJdbcParamsBySqmParam(),
					getConverter().getSqmParameterMappingModelExpressibleResolutions(),
					getConverter().getSqlExpressionResolver(),
					executionContextAdapter
			);
		}
	}

	private int executeWithoutIdTable(
			Predicate suppliedPredicate,
			TableGroup tableGroup,
			Map<SqmParameter<?>, List<List<JdbcParameter>>> restrictionSqmParameterResolutions,
			Map<SqmParameter<?>, MappingModelExpressible<?>> paramTypeResolutions,
			SqlExpressionResolver sqlExpressionResolver,
			ExecutionContext executionContext) {
		assert getEntityDescriptor() == getEntityDescriptor().getRootEntityDescriptor();

		final EntityPersister rootEntityPersister = getEntityDescriptor().getEntityPersister();
		final String rootTableName = rootEntityPersister.getTableName();
		final NamedTableReference rootTableReference = (NamedTableReference) tableGroup.resolveTableReference(
				tableGroup.getNavigablePath(),
				rootTableName
		);

		final QuerySpec matchingIdSubQuerySpec = ExecuteWithoutIdTableHelper.createIdMatchingSubQuerySpec(
				tableGroup.getNavigablePath(),
				rootTableReference,
				suppliedPredicate,
				rootEntityPersister,
				sqlExpressionResolver,
				getSessionFactory()
		);

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				getDomainParameterXref(),
				SqmUtil.generateJdbcParamsXref(
						getDomainParameterXref(),
						() -> restrictionSqmParameterResolutions
				),
				getSessionFactory().getRuntimeMetamodels().getMappingMetamodel(),
				navigablePath -> tableGroup,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) paramTypeResolutions.get(parameter);
					}
				},
				executionContext.getSession()
		);

		SqmMutationStrategyHelper.cleanUpCollectionTables(
				getEntityDescriptor(),
				(tableReference, attributeMapping) -> {
					// No need for a predicate if there is no supplied predicate i.e. this is a full cleanup
					if ( suppliedPredicate == null ) {
						return null;
					}
					final ForeignKeyDescriptor fkDescriptor = attributeMapping.getKeyDescriptor();
					final QuerySpec idSelectFkSubQuery;
					// todo (6.0): based on the location of the attribute mapping, we could prune the table group of the subquery
					if ( fkDescriptor.getTargetPart().isEntityIdentifierMapping() ) {
						idSelectFkSubQuery = matchingIdSubQuerySpec;
					}
					else {
						idSelectFkSubQuery = ExecuteWithoutIdTableHelper.createIdMatchingSubQuerySpec(
								tableGroup.getNavigablePath(),
								rootTableReference,
								suppliedPredicate,
								rootEntityPersister,
								sqlExpressionResolver,
								getSessionFactory()
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
							idSelectFkSubQuery,
							false
					);

				},
				jdbcParameterBindings,
				executionContext
		);

		if ( rootTableReference instanceof UnionTableReference ) {
			final MutableInteger rows = new MutableInteger();
			getEntityDescriptor().visitConstraintOrderedTables(
					(tableExpression, tableKeyColumnVisitationSupplier) -> {
						final NamedTableReference tableReference = new NamedTableReference(
								tableExpression,
								tableGroup.getPrimaryTableReference().getIdentificationVariable()
						);
						final QuerySpec idMatchingSubQuerySpec;
						// No need for a predicate if there is no supplied predicate i.e. this is a full cleanup
						if ( suppliedPredicate == null ) {
							idMatchingSubQuerySpec = null;
						}
						else {
							idMatchingSubQuerySpec = matchingIdSubQuerySpec;
						}
						rows.plus(
								deleteFromNonRootTableWithoutIdTable(
										tableReference,
										tableKeyColumnVisitationSupplier,
										sqlExpressionResolver,
										tableGroup,
										idMatchingSubQuerySpec,
										jdbcParameterBindings,
										executionContext
								)
						);
					}
			);
			return rows.get();
		}
		else {
			getEntityDescriptor().visitConstraintOrderedTables(
					(tableExpression, tableKeyColumnVisitationSupplier) -> {
						if ( !tableExpression.equals( rootTableName ) ) {
							final NamedTableReference tableReference = (NamedTableReference) tableGroup.getTableReference(
									tableGroup.getNavigablePath(),
									tableExpression,
									true
							);
							final QuerySpec idMatchingSubQuerySpec;
							// No need for a predicate if there is no supplied predicate i.e. this is a full cleanup
							if ( suppliedPredicate == null ) {
								idMatchingSubQuerySpec = null;
							}
							else {
								idMatchingSubQuerySpec = matchingIdSubQuerySpec;
							}
							deleteFromNonRootTableWithoutIdTable(
									tableReference,
									tableKeyColumnVisitationSupplier,
									sqlExpressionResolver,
									tableGroup,
									idMatchingSubQuerySpec,
									jdbcParameterBindings,
									executionContext
							);
						}
					}
			);

			return deleteFromRootTableWithoutIdTable(
					rootTableReference,
					suppliedPredicate,
					jdbcParameterBindings,
					executionContext
			);
		}
	}

	private int deleteFromRootTableWithoutIdTable(
			NamedTableReference rootTableReference,
			Predicate predicate,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		return executeSqlDelete(
				new DeleteStatement( rootTableReference, predicate ),
				jdbcParameterBindings,
				executionContext
		);
	}

	private int deleteFromNonRootTableWithoutIdTable(
			NamedTableReference targetTableReference,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			SqlExpressionResolver sqlExpressionResolver,
			TableGroup rootTableGroup,
			QuerySpec matchingIdSubQuerySpec,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		assert targetTableReference != null;
		MUTATION_QUERY_LOGGER.tracef( "deleteFromNonRootTable - %s", targetTableReference.getTableExpression() );

		final NamedTableReference deleteTableReference = new NamedTableReference(
				targetTableReference.getTableExpression(),
				DeleteStatement.DEFAULT_ALIAS,
				true
		);
		final Predicate tableDeletePredicate;
		if ( matchingIdSubQuerySpec == null ) {
			tableDeletePredicate = null;
		}
		else {
			/*
			 * delete from sub_table
			 * where sub_id in (
			 * 		select root_id from root_table
			 * 		where {predicate}
			 * )
			 */

			/*
			 * Create the `sub_id` reference as the LHS of the in-subquery predicate
			 */
			final List<ColumnReference> deletingTableColumnRefs = new ArrayList<>();
			tableKeyColumnVisitationSupplier.get().accept(
					(columnIndex, selection) -> {
						assert deleteTableReference.getTableReference( selection.getContainingTableExpression() ) != null;

						final Expression expression = sqlExpressionResolver.resolveSqlExpression(
								deleteTableReference,
								selection
						);

						deletingTableColumnRefs.add( (ColumnReference) expression );
					}
			);

			final Expression deletingTableColumnRefsExpression;
			if ( deletingTableColumnRefs.size() == 1 ) {
				deletingTableColumnRefsExpression = deletingTableColumnRefs.get( 0 );
			}
			else {
				deletingTableColumnRefsExpression = new SqlTuple( deletingTableColumnRefs, getEntityDescriptor().getIdentifierMapping() );
			}

			tableDeletePredicate = new InSubQueryPredicate(
					deletingTableColumnRefsExpression,
					matchingIdSubQuerySpec,
					false
			);
		}

		final DeleteStatement sqlAstDelete = new DeleteStatement( deleteTableReference, tableDeletePredicate );
		final int rows = executeSqlDelete(
				sqlAstDelete,
				jdbcParameterBindings,
				executionContext
		);
		MUTATION_QUERY_LOGGER.debugf( "deleteFromNonRootTable - `%s` : %s rows", targetTableReference, rows );
		return rows;
	}


	private static int executeSqlDelete(
			DeleteStatement sqlAst,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final SessionFactoryImplementor factory = executionContext.getSession().getFactory();

		final JdbcServices jdbcServices = factory.getJdbcServices();

		final JdbcOperationQueryMutation jdbcDelete = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, sqlAst )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

		return jdbcServices.getJdbcMutationExecutor().execute(
				jdbcDelete,
				jdbcParameterBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {},
				executionContext
		);
	}

	private int executeWithIdTable(
			Predicate predicate,
			TableGroup deletingTableGroup,
			Map<SqmParameter<?>, List<List<JdbcParameter>>> restrictionSqmParameterResolutions,
			Map<SqmParameter<?>, MappingModelExpressible<?>> paramTypeResolutions,
			ExecutionContext executionContext) {
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				getDomainParameterXref(),
				SqmUtil.generateJdbcParamsXref(
						getDomainParameterXref(),
						() -> restrictionSqmParameterResolutions
				),
				getSessionFactory().getRuntimeMetamodels().getMappingMetamodel(),
				navigablePath -> deletingTableGroup,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) paramTypeResolutions.get(parameter);
					}
				},
				executionContext.getSession()
		);

		ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions(
				getIdTable(),
				executionContext
		);

		try {
			return executeUsingIdTable( predicate, executionContext, jdbcParameterBindings );
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

	private int executeUsingIdTable(
			Predicate predicate,
			ExecutionContext executionContext,
			JdbcParameterBindings jdbcParameterBindings) {
		final int rows = ExecuteWithTemporaryTableHelper.saveMatchingIdsIntoIdTable(
				getConverter(),
				predicate,
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

		getEntityDescriptor().visitConstraintOrderedTables(
				(tableExpression, tableKeyColumnVisitationSupplier) -> deleteFromTableUsingIdTable(
						tableExpression,
						tableKeyColumnVisitationSupplier,
						idTableIdentifierSubQuery,
						executionContext
				)
		);

		return rows;
	}

	private void deleteFromTableUsingIdTable(
			String tableExpression,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			QuerySpec idTableSubQuery,
			ExecutionContext executionContext) {
		MUTATION_QUERY_LOGGER.tracef( "deleteFromTableUsingIdTable - %s", tableExpression );

		final TableKeyExpressionCollector keyColumnCollector = new TableKeyExpressionCollector( getEntityDescriptor() );
		final NamedTableReference targetTable = new NamedTableReference(
				tableExpression,
				DeleteStatement.DEFAULT_ALIAS,
				true
		);

		tableKeyColumnVisitationSupplier.get().accept(
				(columnIndex, selection) -> {
					assert selection.getContainingTableExpression().equals( tableExpression );
					assert ! selection.isFormula();
					assert selection.getCustomReadExpression() == null;
					assert selection.getCustomWriteExpression() == null;

					keyColumnCollector.apply(
							new ColumnReference(
									targetTable,
									selection
							)
					);
				}
		);

		final InSubQueryPredicate predicate = new InSubQueryPredicate(
				keyColumnCollector.buildKeyExpression(),
				idTableSubQuery,
				false
		);

		executeSqlDelete(
				new DeleteStatement( targetTable, predicate ),
				JdbcParameterBindings.NO_BINDINGS,
				executionContext
		);
	}

}
