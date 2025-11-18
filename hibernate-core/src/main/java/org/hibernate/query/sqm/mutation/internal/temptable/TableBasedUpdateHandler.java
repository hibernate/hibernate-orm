/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableSessionUidColumn;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.results.internal.TableGroupImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.internal.CacheableSqmInterpretation;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.AbstractMutationHandler;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.TableKeyExpressionCollector;
import org.hibernate.query.sqm.mutation.internal.UpdateHandler;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateCollector;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
* @author Steve Ebersole
*/
public class TableBasedUpdateHandler
		extends AbstractMutationHandler
		implements UpdateHandler {
	private static final Logger log = Logger.getLogger( TableBasedUpdateHandler.class );

	private final TemporaryTable idTable;
	private final TemporaryTableStrategy temporaryTableStrategy;
	private final boolean forceDropAfterUse;
	private final Function<SharedSessionContractImplementor,String> sessionUidAccess;
	private final DomainParameterXref domainParameterXref;
	private final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref;
	private final Map<SqmParameter<?>, MappingModelExpressible<?>> resolvedParameterMappingModelTypes;
	private final @Nullable JdbcParameter sessionUidParameter;

	private final CacheableSqmInterpretation<InsertSelectStatement, JdbcOperationQueryMutation> matchingIdsIntoIdTableInsert;
	private final List<TableUpdater> tableUpdaters;

	public TableBasedUpdateHandler(
			SqmUpdateStatement<?> sqmUpdate,
			DomainParameterXref domainParameterXref,
			TemporaryTable idTable,
			TemporaryTableStrategy temporaryTableStrategy,
			boolean forceDropAfterUse,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindingsConsumer) {
		super( sqmUpdate, context.getSession().getSessionFactory() );
		this.idTable = idTable;
		this.temporaryTableStrategy  = temporaryTableStrategy;
		this.forceDropAfterUse = forceDropAfterUse;
		this.sessionUidAccess = sessionUidAccess;

		final TemporaryTableSessionUidColumn sessionUidColumn = idTable.getSessionUidColumn();
		if ( sessionUidColumn == null ) {
			this.sessionUidParameter = null;
		}
		else {
			this.sessionUidParameter = new SqlTypedMappingJdbcParameter( sessionUidColumn );
		}
		final SessionFactoryImplementor sessionFactory = getSessionFactory();
		final MappingMetamodel domainModel = sessionFactory.getMappingMetamodel();
		final EntityPersister entityDescriptor =
				domainModel.getEntityDescriptor( sqmUpdate.getTarget().getEntityName() );

		final String rootEntityName = entityDescriptor.getRootEntityName();
		final EntityPersister rootEntityDescriptor = domainModel.getEntityDescriptor( rootEntityName );

		final String hierarchyRootTableName = rootEntityDescriptor.getTableName();

		final MultiTableSqmMutationConverter converterDelegate = new MultiTableSqmMutationConverter(
				entityDescriptor,
				sqmUpdate,
				sqmUpdate.getTarget(),
				domainParameterXref,
				context.getQueryOptions(),
				context.getSession().getLoadQueryInfluencers(),
				context.getQueryParameterBindings(),
				sessionFactory.getSqlTranslationEngine()
		);

		final TableGroup updatingTableGroup = converterDelegate.getMutatingTableGroup();

		final TableReference hierarchyRootTableReference = updatingTableGroup.resolveTableReference(
				updatingTableGroup.getNavigablePath(),
				hierarchyRootTableName
		);
		assert hierarchyRootTableReference != null;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the set-clause using our special converter, collecting
		// information about the assignments

		final List<Assignment> assignments = converterDelegate.visitSetClause( sqmUpdate.getSetClause() );
		converterDelegate.addVersionedAssignment( assignments::add, sqmUpdate );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// visit the where-clause using our special converter, collecting information
		// about the restrictions

		final PredicateCollector predicateCollector = new PredicateCollector(
				converterDelegate.visitWhereClause( sqmUpdate.getWhereClause() )
		);

		entityDescriptor.applyBaseRestrictions(
				predicateCollector::applyPredicate,
				updatingTableGroup,
				true,
				context.getSession().getLoadQueryInfluencers().getEnabledFilters(),
				false,
				null,
				converterDelegate
		);

		converterDelegate.pruneTableGroupJoins();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, bu the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias = CollectionHelper.mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( updatingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < updatingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( updatingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		final SoftDeleteMapping softDeleteMapping = entityDescriptor.getSoftDeleteMapping();
		final Predicate suppliedPredicate;
		if ( softDeleteMapping != null ) {
			final NamedTableReference rootTableReference = (NamedTableReference) updatingTableGroup.resolveTableReference(
					updatingTableGroup.getNavigablePath(),
					entityDescriptor.getIdentifierTableDetails().getTableName()
			);
			suppliedPredicate = Predicate.combinePredicates(
					predicateCollector.getPredicate(),
					softDeleteMapping.createNonDeletedRestriction( rootTableReference )
			);
		}
		else {
			suppliedPredicate = predicateCollector.getPredicate();
		}

		Map<TableReference, List<Assignment>> assignmentsByTable = mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );


		this.domainParameterXref = domainParameterXref;
		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref( domainParameterXref, converterDelegate );
		this.resolvedParameterMappingModelTypes = converterDelegate.getSqmParameterMappingModelExpressibleResolutions();

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
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


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// segment the assignments by table-reference

		for ( int i = 0; i < assignments.size(); i++ ) {
			final Assignment assignment = assignments.get( i );
			final List<ColumnReference> assignmentColumnRefs = assignment.getAssignable().getColumnReferences();

			TableReference assignmentTableReference = null;

			for ( int c = 0; c < assignmentColumnRefs.size(); c++ ) {
				final ColumnReference columnReference = assignmentColumnRefs.get( c );
				final TableReference tableReference = resolveTableReference(
						columnReference,
						tableReferenceByAlias
				);

				if ( assignmentTableReference != null && assignmentTableReference != tableReference ) {
					throw new SemanticException( "Assignment referred to columns from multiple tables: " + assignment.getAssignable() );
				}

				assignmentTableReference = tableReference;
			}

			List<Assignment> assignmentsForTable = assignmentsByTable.get( assignmentTableReference );
			if ( assignmentsForTable == null ) {
				assignmentsForTable = new ArrayList<>();
				assignmentsByTable.put( assignmentTableReference, assignmentsForTable );
			}
			assignmentsForTable.add( assignment );
		}

		final SqmJdbcExecutionContextAdapter executionContext = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( context );
		this.matchingIdsIntoIdTableInsert = ExecuteWithTemporaryTableHelper.createMatchingIdsIntoIdTableInsert(
				converterDelegate,
				suppliedPredicate,
				idTable,
				sessionUidParameter,
				jdbcParameterBindings,
				executionContext
		);

		final QuerySpec idTableSubQuery = ExecuteWithTemporaryTableHelper.createIdTableSelectQuerySpec(
				idTable,
				sessionUidParameter,
				entityDescriptor,
				executionContext
		);
		final ArrayList<TableUpdater> tableUpdaters = new ArrayList<>();
		entityDescriptor.visitConstraintOrderedTables(
				(tableExpression, tableKeyColumnVisitationSupplier) -> tableUpdaters.add(
						createTableUpdater(
								updatingTableGroup,
								tableExpression,
								tableKeyColumnVisitationSupplier,
								assignmentsByTable,
								idTableSubQuery,
								jdbcParameterBindings,
								executionContext
						)
				)
		);
		this.tableUpdaters = tableUpdaters;
		firstJdbcParameterBindingsConsumer.set( jdbcParameterBindings );
	}

	@Override
	public JdbcParameterBindings createJdbcParameterBindings(DomainQueryExecutionContext context) {
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
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
		if ( matchingIdsIntoIdTableInsert.jdbcOperation().dependsOnParameterBindings() ) {
			return true;
		}
		for ( TableUpdater updater : tableUpdaters ) {
			if ( updater.jdbcUpdate.dependsOnParameterBindings() ) {
				return true;
			}
			if ( updater.jdbcInsert != null && updater.jdbcInsert.dependsOnParameterBindings() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isCompatibleWith(JdbcParameterBindings jdbcParameterBindings, QueryOptions queryOptions) {
		if ( !matchingIdsIntoIdTableInsert.jdbcOperation().isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
			return false;
		}
		for ( TableUpdater updater : tableUpdaters ) {
			if ( !updater.jdbcUpdate.isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
				return false;
			}
			if ( updater.jdbcInsert != null
				&& !updater.jdbcInsert.isCompatibleWith( jdbcParameterBindings, queryOptions ) ) {
				return false;
			}
		}
		return true;
	}

	protected TableReference resolveTableReference(
			ColumnReference columnReference,
			Map<String, TableReference> tableReferenceByAlias) {
		final TableReference tableReferenceByQualifier = tableReferenceByAlias.get( columnReference.getQualifier() );
		if ( tableReferenceByQualifier != null ) {
			return tableReferenceByQualifier;
		}

		throw new SemanticException( "Assignment referred to column of a joined association: " + columnReference );
	}

	protected NamedTableReference resolveUnionTableReference(
			TableReference tableReference,
			String tableExpression) {
		if ( tableReference instanceof UnionTableReference ) {
			return new NamedTableReference(
					tableExpression,
					tableReference.getIdentificationVariable(),
					tableReference.isOptional()
			);
		}
		return (NamedTableReference) tableReference;
	}

	private TableUpdater createTableUpdater(
			TableGroup updatingTableGroup,
			String tableExpression,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			Map<TableReference, List<Assignment>> assignmentsByTable,
			QuerySpec idTableSubQuery,
			JdbcParameterBindings firstJdbcParameterBindings,
			ExecutionContext executionContext) {

		// update `updatingTableReference`
		// set ...
		// where `keyExpression` in ( `idTableSubQuery` )

		final TableReference updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				true
		);

		final List<Assignment> assignments = assignmentsByTable.get( updatingTableReference );
		if ( assignments == null || assignments.isEmpty() ) {
			// no assignments for this table - skip it
			return null;
		}

		final NamedTableReference dmlTableReference = resolveUnionTableReference( updatingTableReference, tableExpression );
		final JdbcServices jdbcServices = executionContext.getSession().getFactory().getJdbcServices();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcServices.getJdbcEnvironment().getSqlAstTranslatorFactory();

		final Expression keyExpression =
				resolveMutatingTableKeyExpression( tableExpression, tableKeyColumnVisitationSupplier );
		return new TableUpdater(
				createTableUpdate(
						idTableSubQuery,
						executionContext,
						assignments,
						dmlTableReference,
						sqlAstTranslatorFactory,
						firstJdbcParameterBindings,
						keyExpression
				),
				isTableOptional( tableExpression ) ? createTableInsert(
						tableExpression,
						dmlTableReference,
						keyExpression,
						tableKeyColumnVisitationSupplier,
						idTableSubQuery,
						assignments,
						sqlAstTranslatorFactory,
						firstJdbcParameterBindings,
						executionContext
				) : null
		);
	}

	protected boolean isTableOptional(String tableExpression) {
		final EntityPersister entityPersister = getEntityDescriptor().getEntityPersister();
		for ( int i = 0; i < entityPersister.getTableSpan(); i++ ) {
			if ( tableExpression.equals( entityPersister.getTableName( i ) )
				&& entityPersister.isNullableTable( i ) ) {
				return true;
			}
		}
		return false;
	}

	private JdbcOperationQueryMutation createTableInsert(
			String targetTableExpression,
			NamedTableReference targetTableReference,
			Expression targetTableKeyExpression,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			QuerySpec idTableSubQuery,
			List<Assignment> assignments,
			SqlAstTranslatorFactory sqlAstTranslatorFactory,
			JdbcParameterBindings firstJdbcParameterBindings,
			ExecutionContext executionContext) {

		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();
		// Execute a query in the form -
		//
		// insert into <target> (...)
		// 		select ...
		// 		from <id-table> temptable_
		// 		where not exists (
		// 			select 1
		//			from <target> dml_
		//			where dml_.<key> = temptable_.<key>
		// 		)

		// Create a new QuerySpec for the "insert source" select query.  This
		// is mostly a copy of the incoming `idTableSubQuery` along with the
		// NOT-EXISTS predicate
		final QuerySpec insertSourceSelectQuerySpec = makeInsertSourceSelectQuerySpec( idTableSubQuery );

		// create the `select 1 ...` sub-query and apply the not-exists predicate
		final QuerySpec existsSubQuerySpec = createExistsSubQuerySpec( targetTableExpression, tableKeyColumnVisitationSupplier, idTableSubQuery, sessionFactory );
		insertSourceSelectQuerySpec.applyPredicate(
				new ExistsPredicate(
						existsSubQuerySpec,
						true,
						sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( Boolean.class )
				)
		);

		// Collect the target column references from the key expressions
		final List<ColumnReference> targetColumnReferences = new ArrayList<>();
		if ( targetTableKeyExpression instanceof SqlTuple ) {
			//noinspection unchecked
			targetColumnReferences.addAll( (Collection<? extends ColumnReference>) ( (SqlTuple) targetTableKeyExpression ).getExpressions() );
		}
		else {
			targetColumnReferences.add( (ColumnReference) targetTableKeyExpression );
		}

		// And transform assignments to target column references and selections
		for ( Assignment assignment : assignments ) {
			targetColumnReferences.addAll( assignment.getAssignable().getColumnReferences() );
			insertSourceSelectQuerySpec.getSelectClause().addSqlSelection(
					new SqlSelectionImpl( assignment.getAssignedValue() )
			);
		}

		final InsertSelectStatement insertSqlAst = new InsertSelectStatement( targetTableReference );
		insertSqlAst.addTargetColumnReferences( targetColumnReferences.toArray( new ColumnReference[0] ) );
		insertSqlAst.setSourceSelectStatement( insertSourceSelectQuerySpec );

		return sqlAstTranslatorFactory
				.buildMutationTranslator( sessionFactory, insertSqlAst )
				.translate( firstJdbcParameterBindings, executionContext.getQueryOptions() );
	}

	protected QuerySpec createExistsSubQuerySpec(
			String targetTableExpression,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			QuerySpec idTableSubQuery,
			SessionFactoryImplementor sessionFactory) {
		final NamedTableReference existsTableReference = new NamedTableReference(
				targetTableExpression,
				"dml_"
		);

		// Prepare a not exists sub-query to avoid violating constraints
		final QuerySpec existsSubQuerySpec = new QuerySpec( false );
		existsSubQuerySpec.getSelectClause().addSqlSelection(
				new SqlSelectionImpl(
						new QueryLiteral<>(
								1,
								sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( Integer.class )
						)
				)
		);
		existsSubQuerySpec.getFromClause().addRoot( new TableGroupImpl(
				null,
				null,
				existsTableReference,
				getEntityDescriptor()
		) );

		final TableKeyExpressionCollector existsKeyColumnCollector = new TableKeyExpressionCollector( getEntityDescriptor() );
		tableKeyColumnVisitationSupplier.get().accept( (columnIndex, selection) -> {
			assert selection.getContainingTableExpression().equals( targetTableExpression );
			existsKeyColumnCollector.apply( new ColumnReference( existsTableReference, selection ) );
		} );
		existsSubQuerySpec.applyPredicate(
				new ComparisonPredicate(
						existsKeyColumnCollector.buildKeyExpression(),
						ComparisonOperator.EQUAL,
						asExpression( idTableSubQuery.getSelectClause())
				)
		);
		return existsSubQuerySpec;
	}

	protected static QuerySpec makeInsertSourceSelectQuerySpec(QuerySpec idTableSubQuery) {
		final QuerySpec idTableQuerySpec = new QuerySpec( true );
		for ( TableGroup root : idTableSubQuery.getFromClause().getRoots() ) {
			idTableQuerySpec.getFromClause().addRoot( root );
		}
		for ( SqlSelection sqlSelection : idTableSubQuery.getSelectClause().getSqlSelections() ) {
			idTableQuerySpec.getSelectClause().addSqlSelection( sqlSelection );
		}
		idTableQuerySpec.applyPredicate( idTableSubQuery.getWhereClauseRestrictions() );
		return idTableQuerySpec;
	}

	private JdbcOperationQueryMutation createTableUpdate(
			QuerySpec idTableSubQuery,
			ExecutionContext executionContext,
			List<Assignment> assignments,
			NamedTableReference dmlTableReference,
			SqlAstTranslatorFactory sqlAstTranslatorFactory,
			JdbcParameterBindings firstJdbcParameterBindings,
			Expression keyExpression) {
		final UpdateStatement sqlAst = new UpdateStatement(
				dmlTableReference,
				assignments,
				new InSubQueryPredicate( keyExpression, idTableSubQuery, false )
		);

		return sqlAstTranslatorFactory
				.buildMutationTranslator( executionContext.getSession().getFactory(), sqlAst )
				.translate( firstJdbcParameterBindings, executionContext.getQueryOptions() );
	}

	protected Expression resolveMutatingTableKeyExpression(String tableExpression, Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier) {
		final TableKeyExpressionCollector keyColumnCollector = new TableKeyExpressionCollector( getEntityDescriptor() );

		tableKeyColumnVisitationSupplier.get().accept(
				(columnIndex, selection) -> {
					assert selection.getContainingTableExpression().equals( tableExpression );
					keyColumnCollector.apply( new ColumnReference( (String) null, selection ) );
				}
		);

		return keyColumnCollector.buildKeyExpression();
	}

	protected Expression asExpression(SelectClause selectClause) {
		final List<SqlSelection> sqlSelections = selectClause.getSqlSelections();
		if ( sqlSelections.size() == 1 ) {
			return sqlSelections.get( 0 ).getExpression();
		}
		final List<Expression> expressions = new ArrayList<>( sqlSelections.size() );
		for ( SqlSelection sqlSelection : sqlSelections ) {
			expressions.add( sqlSelection.getExpression() );
		}
		return new SqlTuple( expressions, null );
	}

	protected void collectTableReference(
			TableReference tableReference,
			BiConsumer<String, TableReference> consumer) {
		consumer.accept( tableReference.getIdentificationVariable(), tableReference );
	}

	protected void collectTableReference(
			TableReferenceJoin tableReferenceJoin,
			BiConsumer<String, TableReference> consumer) {
		collectTableReference( tableReferenceJoin.getJoinedTableReference(), consumer );
	}

	@Override
	public int execute(JdbcParameterBindings jdbcParameterBindings, DomainQueryExecutionContext context) {
		if ( log.isTraceEnabled() ) {
			log.tracef(
					"Starting multi-table update execution - %s",
					getSqmStatement().getTarget().getModel().getName()
			);
		}

		final SqmJdbcExecutionContextAdapter executionContext = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( context );
		ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions(
				idTable,
				temporaryTableStrategy,
				executionContext
		);

		try {
			final int rows = ExecuteWithTemporaryTableHelper.saveIntoTemporaryTable(
					matchingIdsIntoIdTableInsert.jdbcOperation(),
					jdbcParameterBindings,
					executionContext
			);
			for ( TableUpdater tableUpdater : tableUpdaters ) {
				updateTable(
						tableUpdater,
						rows,
						jdbcParameterBindings,
						executionContext
				);
			}
			return rows;
		}
		finally {
			ExecuteWithTemporaryTableHelper.performAfterTemporaryTableUseActions(
					idTable,
					sessionUidAccess,
					getAfterUseAction(),
					executionContext
			);
		}
	}

	private void updateTable(
			TableUpdater tableUpdater,
			int expectedUpdateCount,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		if ( tableUpdater == null ) {
			// no assignments for this table - skip it
			return;
		}
		final int updateCount = executeMutation( tableUpdater.jdbcUpdate, jdbcParameterBindings, executionContext );

		// We are done when the update count matches
		if ( updateCount == expectedUpdateCount ) {
			return;
		}

		// If the table is optional, execute an insert
		if ( tableUpdater.jdbcInsert != null ) {
			final int insertCount = executeMutation( tableUpdater.jdbcInsert, jdbcParameterBindings, executionContext );
			assert insertCount + updateCount == expectedUpdateCount;
		}
	}

	private int executeMutation(JdbcOperationQueryMutation jdbcUpdate, JdbcParameterBindings jdbcParameterBindings, ExecutionContext executionContext) {
		return executionContext.getSession().getFactory().getJdbcServices().getJdbcMutationExecutor().execute(
				jdbcUpdate,
				jdbcParameterBindings,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(integer, preparedStatement) -> {
				},
				executionContext
		);
	}

	protected record TableUpdater(
			JdbcOperationQueryMutation jdbcUpdate,
			@Nullable JdbcOperationQueryMutation jdbcInsert) {
	}

	// For Hibernate reactive
	protected List<TableUpdater> getTableUpdaters() {
		return tableUpdaters;
	}

	// For Hibernate reactive
	protected AfterUseAction getAfterUseAction() {
		return forceDropAfterUse ? AfterUseAction.DROP : temporaryTableStrategy.getTemporaryTableAfterUseAction();
	}

	// For Hibernate reactive
	protected TemporaryTable getIdTable() {
		return idTable;
	}

	// For Hibernate reactive
	protected TemporaryTableStrategy getTemporaryTableStrategy() {
		return temporaryTableStrategy;
	}

	// For Hibernate reactive
	protected CacheableSqmInterpretation<InsertSelectStatement, JdbcOperationQueryMutation> getMatchingIdsIntoIdTableInsert() {
		return matchingIdsIntoIdTableInsert;
	}

	// For Hibernate reactive
	protected Function<SharedSessionContractImplementor, String> getSessionUidAccess() {
		return sessionUidAccess;
	}

	// For Hibernate reactive
	protected @Nullable JdbcParameter getSessionUidParameter() {
		return sessionUidParameter;
	}
}
