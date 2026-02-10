/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.CacheableSqmInterpretation;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.internal.DeleteHandler;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.TableKeyExpressionCollector;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.MutatingTableReferenceGroupWrapper;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateCollector;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.buildColumnReferenceExpression;
import static org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter.omittingLockingAndPaging;
import static org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper.visitCollectionTableDeletes;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.createIdTableSelectQuerySpec;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.createMatchingIdsIntoIdTableInsert;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.performAfterTemporaryTableUseActions;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithTemporaryTableHelper.saveIntoTemporaryTable;
import static org.hibernate.query.sqm.mutation.internal.temptable.ExecuteWithoutIdTableHelper.createIdMatchingSubQuerySpec;

/**
* @author Steve Ebersole
*/
public class TableBasedDeleteHandler
		extends AbstractMutationHandler
		implements DeleteHandler {
	private static final Logger LOG = Logger.getLogger( TableBasedDeleteHandler.class );

	private final TemporaryTable idTable;
	private final TemporaryTableStrategy temporaryTableStrategy;
	private final boolean forceDropAfterUse;
	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;
	private final DomainParameterXref domainParameterXref;
	private final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref;
	private final Map<SqmParameter<?>, MappingModelExpressible<?>> resolvedParameterMappingModelTypes;
	private final @Nullable JdbcParameter sessionUidParameter;

	private final @Nullable CacheableSqmInterpretation<InsertSelectStatement, JdbcOperationQueryMutation> idTableInsert;
	private final ArrayList<JdbcOperationQueryMutation> deletes;
	private final ArrayList<JdbcOperationQueryMutation> collectionTableDeletes;

	public TableBasedDeleteHandler(
			SqmDeleteStatement<?> sqmDelete,
			DomainParameterXref domainParameterXref,
			TemporaryTable idTable,
			TemporaryTableStrategy temporaryTableStrategy,
			boolean forceDropAfterUse,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		super( sqmDelete, context.getSession().getSessionFactory() );
		this.idTable = idTable;

		this.temporaryTableStrategy  = temporaryTableStrategy;
		this.forceDropAfterUse = forceDropAfterUse;

		this.sessionUidAccess = sessionUidAccess;

		final var sessionUidColumn = idTable.getSessionUidColumn();
		this.sessionUidParameter =
				sessionUidColumn == null
						? null
						: new SqlTypedMappingJdbcParameter( sessionUidColumn );

		final var converter = new MultiTableSqmMutationConverter(
				getEntityDescriptor(),
				sqmDelete,
				sqmDelete.getTarget(),
				domainParameterXref,
				context.getQueryOptions(),
				context.getSession().getLoadQueryInfluencers(),
				context.getQueryParameterBindings(),
				getSessionFactory().getSqlTranslationEngine()
		);

		final var entityDescriptor =
				getSessionFactory().getMappingMetamodel()
						.getEntityDescriptor( sqmDelete.getTarget().getEntityName() );
		final String hierarchyRootTableName = entityDescriptor.getTableName();

		final var deletingTableGroup = converter.getMutatingTableGroup();

		final var hierarchyRootTableReference = deletingTableGroup.resolveTableReference(
				deletingTableGroup.getNavigablePath(),
				hierarchyRootTableName
		);
		assert hierarchyRootTableReference != null;

		// Use the converter to interpret the where-clause.  We do this for 2 reasons:
		//		1) the resolved Predicate is ultimately the base for applying restriction to the deletes
		//		2) we also inspect each ColumnReference that is part of the where-clause to see which
		//			table it comes from.  if all of the referenced columns (if any at all) are from the root table
		//			we can perform all of the deletes without using an id-table
		final var specifiedRestriction = converter.visitWhereClause( sqmDelete.getWhereClause() );

		final var predicateCollector = new PredicateCollector( specifiedRestriction );
		entityDescriptor.applyBaseRestrictions(
				predicateCollector,
				deletingTableGroup,
				true,
				context.getSession().getLoadQueryInfluencers().getEnabledFilters(),
				false,
				null,
				converter
		);

		converter.pruneTableGroupJoins();
		final var walker = new ColumnReferenceCheckingSqlAstWalker(
				hierarchyRootTableReference.getIdentificationVariable()
		);
		if ( predicateCollector.getPredicate() != null ) {
			predicateCollector.getPredicate().accept( walker );
		}

		// We need an id table if we want to delete from an intermediate table to avoid FK violations
		// The intermediate table has a FK to the root table, so we can't delete from the root table first
		// Deleting from the intermediate table first also isn't possible,
		// because that is the source for deletion in other tables, hence we need an id table
		final boolean needsIdTable =
				!walker.isAllColumnReferencesFromIdentificationVariable()
						|| entityDescriptor != entityDescriptor.getRootEntityDescriptor();

		final var executionContextAdapter = omittingLockingAndPaging( context );

		this.domainParameterXref = domainParameterXref;
		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref( domainParameterXref, converter );
		this.resolvedParameterMappingModelTypes = converter.getSqmParameterMappingModelExpressibleResolutions();

		final var jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				context.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) resolvedParameterMappingModelTypes.get( parameter );
					}
				},
				context.getSession()
		);
		if ( sessionUidParameter != null ) {
			jdbcParameterBindings.addBinding(
					sessionUidParameter,
					new JdbcParameterBindingImpl(
							idTable.getSessionUidColumn().getJdbcMapping(),
							UUID.fromString( sessionUidAccess.apply( context.getSession() ) )
					)
			);
		}

		this.idTableInsert =
				!needsIdTable
						? null
						: createMatchingIdsIntoIdTableInsert(
								converter,
								predicateCollector.getPredicate(),
								idTable,
								sessionUidParameter,
								jdbcParameterBindings,
								executionContextAdapter
						);

		final ArrayList<JdbcOperationQueryMutation> deletes = new ArrayList<>();
		final ArrayList<JdbcOperationQueryMutation> collectionTableDeletes = new ArrayList<>();
		if ( needsIdTable ) {
			final var idTableIdentifierSubQuery = createIdTableSelectQuerySpec(
					idTable,
					sessionUidParameter,
					getEntityDescriptor(),
					executionContextAdapter
			);

			visitCollectionTableDeletes(
					getEntityDescriptor(),
					(tableReference, attributeMapping) -> {
						final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
						final QuerySpec idTableFkSubQuery;
						if ( foreignKeyDescriptor.getTargetPart().isEntityIdentifierMapping() ) {
							idTableFkSubQuery = idTableIdentifierSubQuery;
						}
						else {
							idTableFkSubQuery = createIdTableSelectQuerySpec(
									idTable,
									foreignKeyDescriptor.getTargetPart(),
									sessionUidParameter,
									getEntityDescriptor(),
									executionContextAdapter
							);
						}
						return new InSubQueryPredicate(
								buildColumnReferenceExpression(
										new MutatingTableReferenceGroupWrapper(
												new NavigablePath( attributeMapping.getRootPathName() ),
												attributeMapping,
												(NamedTableReference) tableReference
										),
										foreignKeyDescriptor,
										null,
										getSessionFactory()
								),
								idTableFkSubQuery,
								false
						);

					},
					JdbcParameterBindings.NO_BINDINGS,
					executionContextAdapter.getQueryOptions(),
					collectionTableDeletes::add
			);

			getEntityDescriptor().visitConstraintOrderedTables(
					(tableExpression, tableKeyColumnVisitationSupplier) ->
							deletes.add( createTableDeleteUsingIdTable(
									tableExpression,
									tableKeyColumnVisitationSupplier,
									idTableIdentifierSubQuery,
									executionContextAdapter
							) )
			);
		}
		else {
			final var rootEntityPersister = getEntityDescriptor().getEntityPersister();
			final String rootTableName = rootEntityPersister.getTableName();
			final var rootTableReference =
					(NamedTableReference)
							deletingTableGroup.resolveTableReference(
									deletingTableGroup.getNavigablePath(),
									rootTableName
							);

			final var matchingIdSubQuerySpec = createIdMatchingSubQuerySpec(
					deletingTableGroup.getNavigablePath(),
					rootTableReference,
					predicateCollector.getPredicate(),
					rootEntityPersister,
					converter.getSqlExpressionResolver(),
					getSessionFactory()
			);

			visitCollectionTableDeletes(
					getEntityDescriptor(),
					(tableReference, attributeMapping) -> {
						// No need for a predicate if there is no supplied predicate i.e. this is a full cleanup
						if ( predicateCollector.getPredicate() == null ) {
							return null;
						}
						final var foreignKeyDescriptor = attributeMapping.getKeyDescriptor();
						final QuerySpec idSelectFkSubQuery;
						// todo (6.0): based on the location of the attribute mapping, we could prune the table group of the subquery
						if ( foreignKeyDescriptor.getTargetPart().isEntityIdentifierMapping() ) {
							idSelectFkSubQuery = matchingIdSubQuerySpec;
						}
						else {
							idSelectFkSubQuery = createIdMatchingSubQuerySpec(
									deletingTableGroup.getNavigablePath(),
									rootTableReference,
									predicateCollector.getPredicate(),
									rootEntityPersister,
									converter.getSqlExpressionResolver(),
									getSessionFactory()
							);
						}
						return new InSubQueryPredicate(
								buildColumnReferenceExpression(
										new MutatingTableReferenceGroupWrapper(
												new NavigablePath( attributeMapping.getRootPathName() ),
												attributeMapping,
												(NamedTableReference) tableReference
										),
										foreignKeyDescriptor,
										null,
										getSessionFactory()
								),
								idSelectFkSubQuery,
								false
						);

					},
					jdbcParameterBindings,
					executionContextAdapter.getQueryOptions(),
					collectionTableDeletes::add
			);

			if ( rootTableReference instanceof UnionTableReference ) {
				getEntityDescriptor().visitConstraintOrderedTables(
						(tableExpression, tableKeyColumnVisitationSupplier) -> {
							deletes.add( createNonRootTableDeleteWithoutIdTable(
									new NamedTableReference( tableExpression,
											deletingTableGroup.getPrimaryTableReference()
													.getIdentificationVariable() ),
									tableKeyColumnVisitationSupplier,
									converter.getSqlExpressionResolver(),
									predicateCollector.getPredicate() == null
											// No need for a predicate if there is no supplied predicate
											// i.e. this is a full cleanup
											? null
											: matchingIdSubQuerySpec,
									jdbcParameterBindings,
									executionContextAdapter
							) );
						}
				);
			}
			else {
				getEntityDescriptor().visitConstraintOrderedTables(
						(tableExpression, tableKeyColumnVisitationSupplier) -> {
							if ( !tableExpression.equals( rootTableName ) ) {
								final var tableReference =
										(NamedTableReference)
												deletingTableGroup.getTableReference(
														deletingTableGroup.getNavigablePath(),
														tableExpression,
														true
												);
								deletes.add( createNonRootTableDeleteWithoutIdTable(
										tableReference,
										tableKeyColumnVisitationSupplier,
										converter.getSqlExpressionResolver(),
										predicateCollector.getPredicate() == null
												// No need for a predicate if there is no supplied predicate
												// i.e. this is a full cleanup
												? null
												: matchingIdSubQuerySpec,
										jdbcParameterBindings,
										executionContextAdapter
								) );
							}
						}
				);

				deletes.add( createRootTableDeleteWithoutIdTable(
						rootTableReference,
						predicateCollector.getPredicate(),
						jdbcParameterBindings,
						executionContextAdapter
				) );
			}
		}
		this.deletes = deletes;
		this.collectionTableDeletes = collectionTableDeletes;
		firstJdbcParameterBindingsConsumer.set( jdbcParameterBindings );
	}

	private JdbcOperationQueryMutation createTableDeleteUsingIdTable(
			String tableExpression,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			QuerySpec idTableSubQuery,
			ExecutionContext executionContext) {
		final var keyColumnCollector = new TableKeyExpressionCollector( getEntityDescriptor() );
		final var targetTable =
				new NamedTableReference( tableExpression, DeleteStatement.DEFAULT_ALIAS, true );
		tableKeyColumnVisitationSupplier.get().accept(
				(columnIndex, selection) -> {
					assert selection.getContainingTableExpression().equals( tableExpression );
					assert ! selection.isFormula();
					assert selection.getCustomReadExpression() == null;
					assert selection.getCustomWriteExpression() == null;
					keyColumnCollector.apply( new ColumnReference( targetTable, selection ) );
				}
		);

		final var predicate = new InSubQueryPredicate(
				keyColumnCollector.buildKeyExpression(),
				idTableSubQuery,
				false
		);

		final var factory = executionContext.getSession().getFactory();
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, new DeleteStatement( targetTable, predicate ) )
				.translate( JdbcParameterBindings.NO_BINDINGS, executionContext.getQueryOptions() );
	}


	private JdbcOperationQueryMutation createRootTableDeleteWithoutIdTable(
			NamedTableReference rootTableReference,
			Predicate predicate,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final var factory = executionContext.getSession().getFactory();
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, new DeleteStatement( rootTableReference, predicate ) )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
	}

	private JdbcOperationQueryMutation createNonRootTableDeleteWithoutIdTable(
			NamedTableReference targetTableReference,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			SqlExpressionResolver sqlExpressionResolver,
			QuerySpec matchingIdSubQuerySpec,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		assert targetTableReference != null;
		final var deleteTableReference =
				new NamedTableReference(
						targetTableReference.getTableExpression(),
						DeleteStatement.DEFAULT_ALIAS,
						true
				);
		final var sqlAstDelete =
				new DeleteStatement( deleteTableReference,
						tableDeletePredicate( tableKeyColumnVisitationSupplier, sqlExpressionResolver,
								matchingIdSubQuerySpec, deleteTableReference ) );
		final var factory = executionContext.getSession().getFactory();
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildMutationTranslator( factory, sqlAstDelete )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
	}

	private @Nullable Predicate tableDeletePredicate(
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			SqlExpressionResolver sqlExpressionResolver,
			QuerySpec matchingIdSubQuerySpec,
			NamedTableReference deleteTableReference) {
		if ( matchingIdSubQuerySpec == null ) {
			return null;
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
						final var expression =
								sqlExpressionResolver.resolveSqlExpression( deleteTableReference, selection );
						deletingTableColumnRefs.add( (ColumnReference) expression );
					}
			);

			return new InSubQueryPredicate(
					deletingTableColumnRefs.size() == 1
							? deletingTableColumnRefs.get( 0 )
							: new SqlTuple( deletingTableColumnRefs, getEntityDescriptor().getIdentifierMapping() ),
					matchingIdSubQuerySpec,
					false
			);
		}
	}

	@Override
	public JdbcParameterBindings createJdbcParameterBindings(DomainQueryExecutionContext context) {
		final var jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				context.getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				new SqmParameterMappingModelResolutionAccess() {
					@Override
					@SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) resolvedParameterMappingModelTypes.get( parameter );
					}
				},
				context.getSession()
		);
		if ( sessionUidParameter != null ) {
			jdbcParameterBindings.addBinding(
					sessionUidParameter,
					new JdbcParameterBindingImpl(
							idTable.getSessionUidColumn().getJdbcMapping(),
							UUID.fromString( sessionUidAccess.apply( context.getSession() ) )
					)
			);
		}
		return jdbcParameterBindings;
	}

	@Override
	public boolean dependsOnParameterBindings() {
		if ( idTableInsert != null && idTableInsert.jdbcOperation().dependsOnParameterBindings() ) {
			return true;
		}
		for ( var delete : deletes ) {
			if ( delete.dependsOnParameterBindings() ) {
				return true;
			}
		}
		for ( var delete : collectionTableDeletes ) {
			if ( delete.dependsOnParameterBindings() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		if ( idTableInsert != null
			&& !idTableInsert.jdbcOperation().isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
			return false;
		}
		for ( var delete : deletes ) {
			if ( !delete.isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
				return false;
			}
		}
		for ( var delete : collectionTableDeletes ) {
			if ( !delete.isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int execute(JdbcParameterBindings jdbcParameterBindings, DomainQueryExecutionContext context) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef(
					"Starting multi-table delete execution - %s",
					getSqmStatement().getTarget().getModel().getName()
			);
		}
		final var executionContext = omittingLockingAndPaging( context );
		if ( idTableInsert != null ) {
			performBeforeTemporaryTableUseActions(
					idTable,
					temporaryTableStrategy,
					executionContext
			);

			try {
				final int rows = saveIntoTemporaryTable(
						idTableInsert.jdbcOperation(),
						jdbcParameterBindings,
						executionContext
				);
				final var sessionUidBindings = new JdbcParameterBindingsImpl( 1 );
				if ( sessionUidParameter != null ) {
					sessionUidBindings.addBinding(
							sessionUidParameter,
							new JdbcParameterBindingImpl(
									sessionUidParameter.getExpressionType().getSingleJdbcMapping(),
									UUID.fromString( sessionUidAccess.apply( executionContext.getSession() ) )
							)
					);
				}
				final var jdbcMutationExecutor =
						executionContext.getSession().getFactory()
								.getJdbcServices().getJdbcMutationExecutor();
				for ( var delete : collectionTableDeletes ) {
					jdbcMutationExecutor.execute(
							delete,
							sessionUidBindings,
							sql -> executionContext.getSession()
									.getJdbcCoordinator()
									.getStatementPreparer()
									.prepareStatement( sql ),
							(integer, preparedStatement) -> {},
							executionContext
					);
				}
				for ( var delete : deletes ) {
					jdbcMutationExecutor.execute(
							delete,
							sessionUidBindings,
							sql -> executionContext.getSession()
									.getJdbcCoordinator()
									.getStatementPreparer()
									.prepareStatement( sql ),
							(integer, preparedStatement) -> {},
							executionContext
					);
				}
				return rows;
			}
			finally {
				performAfterTemporaryTableUseActions(
						idTable,
						sessionUidAccess,
						getAfterUseAction(),
						executionContext
				);
			}
		}
		else {
			final var jdbcMutationExecutor =
					executionContext.getSession().getFactory()
							.getJdbcServices().getJdbcMutationExecutor();
			for ( var delete : collectionTableDeletes ) {
				jdbcMutationExecutor.execute(
						delete,
						jdbcParameterBindings,
						sql -> executionContext.getSession().getJdbcCoordinator()
								.getStatementPreparer().prepareStatement( sql ),
						(integer, preparedStatement) -> {},
						executionContext
				);
			}

			if ( getEntityDescriptor() instanceof UnionSubclassEntityPersister ) {
				int rows = 0;
				for ( var delete : deletes ) {
					rows += jdbcMutationExecutor.execute(
							delete,
							jdbcParameterBindings,
							sql -> executionContext.getSession().getJdbcCoordinator()
									.getStatementPreparer().prepareStatement( sql ),
							(integer, preparedStatement) -> {},
							executionContext
					);
				}
				return rows;
			}
			else {
				int rows = 0;
				for ( var delete : deletes ) {
					rows = jdbcMutationExecutor.execute(
							delete,
							jdbcParameterBindings,
							sql -> executionContext.getSession().getJdbcCoordinator()
									.getStatementPreparer().prepareStatement( sql ),
							(integer, preparedStatement) -> {},
							executionContext
					);
				}
				return rows;
			}
		}
	}

	// For Hibernate Reactive
	public @Nullable CacheableSqmInterpretation<InsertSelectStatement, JdbcOperationQueryMutation> getIdTableInsert() {
		return idTableInsert;
	}

	// For Hibernate Reactive
	protected ArrayList<JdbcOperationQueryMutation> getDeletes() {
		return deletes;
	}

	// For Hibernate Reactive
	protected ArrayList<JdbcOperationQueryMutation> getCollectionTableDeletes() {
		return collectionTableDeletes;
	}

	// For Hibernate Reactive
	protected AfterUseAction getAfterUseAction() {
		return forceDropAfterUse ? AfterUseAction.DROP : temporaryTableStrategy.getTemporaryTableAfterUseAction();
	}

	// For Hibernate Reactive
	protected TemporaryTable getIdTable() {
		return idTable;
	}

	// For Hibernate Reactive
	protected TemporaryTableStrategy getTemporaryTableStrategy() {
		return temporaryTableStrategy;
	}

	// For Hibernate Reactive
	protected Function<SharedSessionContractImplementor, String> getSessionUidAccess() {
		return sessionUidAccess;
	}

	// For Hibernate Reactive
	protected @Nullable JdbcParameter getSessionUidParameter() {
		return sessionUidParameter;
	}
}
