/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.boot.model.internal.SoftDeleteHelper;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.results.TableGroupImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.mutation.internal.TableKeyExpressionCollector;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

/**
 * @author Steve Ebersole
 */
public class UpdateExecutionDelegate implements TableBasedUpdateHandler.ExecutionDelegate {
	private final MultiTableSqmMutationConverter sqmConverter;
	private final TemporaryTable idTable;
	private final AfterUseAction afterUseAction;
	private final Function<SharedSessionContractImplementor, String> sessionUidAccess;
	private final TableGroup updatingTableGroup;
	private final Predicate suppliedPredicate;

	private final EntityMappingType entityDescriptor;

	private final JdbcParameterBindings jdbcParameterBindings;

	private final Map<TableReference, List<Assignment>> assignmentsByTable;
	private final SessionFactoryImplementor sessionFactory;

	public UpdateExecutionDelegate(
			MultiTableSqmMutationConverter sqmConverter,
			TemporaryTable idTable,
			AfterUseAction afterUseAction,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainParameterXref domainParameterXref,
			TableGroup updatingTableGroup,
			Map<String, TableReference> tableReferenceByAlias,
			List<Assignment> assignments,
			Predicate suppliedPredicate,
			DomainQueryExecutionContext executionContext) {
		this.sqmConverter = sqmConverter;
		this.idTable = idTable;
		this.afterUseAction = afterUseAction;
		this.sessionUidAccess = sessionUidAccess;
		this.updatingTableGroup = updatingTableGroup;
		this.sessionFactory = executionContext.getSession().getFactory();

		final ModelPartContainer updatingModelPart = updatingTableGroup.getModelPart();
		assert updatingModelPart instanceof EntityMappingType;
		this.entityDescriptor = (EntityMappingType) updatingModelPart;

		final SoftDeleteMapping softDeleteMapping = entityDescriptor.getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			final NamedTableReference rootTableReference = (NamedTableReference) updatingTableGroup.resolveTableReference(
					updatingTableGroup.getNavigablePath(),
					entityDescriptor.getIdentifierTableDetails().getTableName()
			);
			this.suppliedPredicate = Predicate.combinePredicates(
					suppliedPredicate,
					SoftDeleteHelper.createNonSoftDeletedRestriction( rootTableReference, softDeleteMapping )
			);
		}
		else {
			this.suppliedPredicate = suppliedPredicate;
		}



		this.assignmentsByTable = CollectionHelper.mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );

		jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref(
						domainParameterXref,
						sqmConverter::getJdbcParamsBySqmParam
				),
				sessionFactory.getRuntimeMetamodels().getMappingMetamodel(),
				navigablePath -> updatingTableGroup,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) sqmConverter.getSqmParameterMappingModelExpressibleResolutions().get(parameter);
					}
				},
				executionContext.getSession()
		);


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
	}

	@Override
	public int execute(ExecutionContext executionContext) {
		ExecuteWithTemporaryTableHelper.performBeforeTemporaryTableUseActions(
				idTable,
				executionContext
		);

		try {
			final int rows = ExecuteWithTemporaryTableHelper.saveMatchingIdsIntoIdTable(
					sqmConverter,
					suppliedPredicate,
					idTable,
					sessionUidAccess,
					jdbcParameterBindings,
					executionContext
			);

			final QuerySpec idTableSubQuery = ExecuteWithTemporaryTableHelper.createIdTableSelectQuerySpec(
					idTable,
					sessionUidAccess,
					entityDescriptor,
					executionContext
			);

			entityDescriptor.visitConstraintOrderedTables(
					(tableExpression, tableKeyColumnVisitationSupplier) -> updateTable(
							tableExpression,
							tableKeyColumnVisitationSupplier,
							rows,
							idTableSubQuery,
							executionContext
					)
			);

			return rows;
		}
		finally {
			ExecuteWithTemporaryTableHelper.performAfterTemporaryTableUseActions(
					idTable,
					sessionUidAccess,
					afterUseAction,
					executionContext
			);
		}
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

	private void updateTable(
			String tableExpression,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			int expectedUpdateCount,
			QuerySpec idTableSubQuery,
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
			return;
		}

		final NamedTableReference dmlTableReference = resolveUnionTableReference( updatingTableReference, tableExpression );
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcServices.getJdbcEnvironment().getSqlAstTranslatorFactory();
		final JdbcMutationExecutor jdbcMutationExecutor = jdbcServices.getJdbcMutationExecutor();

		final Expression keyExpression = resolveMutatingTableKeyExpression( tableExpression, tableKeyColumnVisitationSupplier );

		final int updateCount = executeUpdate( idTableSubQuery, executionContext, assignments, dmlTableReference, sqlAstTranslatorFactory, jdbcMutationExecutor, keyExpression );

		// We are done when the update count matches
		if ( updateCount == expectedUpdateCount ) {
			return;
		}

		// If the table is optional, execute an insert
		if ( isTableOptional( tableExpression ) ) {
			final int insertCount = executeInsert(
					tableExpression,
					dmlTableReference,
					keyExpression,
					tableKeyColumnVisitationSupplier,
					idTableSubQuery,
					assignments,
					sqlAstTranslatorFactory,
					jdbcMutationExecutor,
					executionContext
			);
			assert insertCount + updateCount == expectedUpdateCount;
		}
	}

	protected boolean isTableOptional(String tableExpression) {
		final EntityPersister entityPersister = entityDescriptor.getEntityPersister();
		for ( int i = 0; i < entityPersister.getTableSpan(); i++ ) {
			if ( tableExpression.equals( entityPersister.getTableName( i ) )
					&& entityPersister.isNullableTable( i ) ) {
				return true;
			}
		}
		return false;
	}

	private int executeInsert(
			String targetTableExpression,
			NamedTableReference targetTableReference,
			Expression targetTableKeyExpression,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			QuerySpec idTableSubQuery,
			List<Assignment> assignments,
			SqlAstTranslatorFactory sqlAstTranslatorFactory,
			JdbcMutationExecutor jdbcMutationExecutor,
			ExecutionContext executionContext) {

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
		final QuerySpec existsSubQuerySpec = createExistsSubQuerySpec( targetTableExpression, tableKeyColumnVisitationSupplier, idTableSubQuery );
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

		final JdbcOperationQueryMutation jdbcInsert = sqlAstTranslatorFactory
				.buildMutationTranslator( sessionFactory, insertSqlAst )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

		return jdbcMutationExecutor.execute(
				jdbcInsert,
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

	protected QuerySpec createExistsSubQuerySpec(String targetTableExpression, Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier, QuerySpec idTableSubQuery) {
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
				entityDescriptor
		) );

		final TableKeyExpressionCollector existsKeyColumnCollector = new TableKeyExpressionCollector( entityDescriptor );
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

	private int executeUpdate(QuerySpec idTableSubQuery, ExecutionContext executionContext, List<Assignment> assignments, NamedTableReference dmlTableReference, SqlAstTranslatorFactory sqlAstTranslatorFactory, JdbcMutationExecutor jdbcMutationExecutor, Expression keyExpression) {
		final UpdateStatement sqlAst = new UpdateStatement(
				dmlTableReference,
				assignments,
				new InSubQueryPredicate( keyExpression, idTableSubQuery, false )
		);

		final JdbcOperationQueryMutation jdbcUpdate = sqlAstTranslatorFactory
				.buildMutationTranslator( sessionFactory, sqlAst )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

		final int updateCount = jdbcMutationExecutor.execute(
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
		return updateCount;
	}

	protected Expression resolveMutatingTableKeyExpression(String tableExpression, Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier) {
		final TableKeyExpressionCollector keyColumnCollector = new TableKeyExpressionCollector( entityDescriptor );

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

	/**
	 * For Hibernate Reactive
	 */

	protected TemporaryTable getIdTable() {
		return idTable;
	}

	protected Predicate getSuppliedPredicate() {
		return suppliedPredicate;
	}

	protected MultiTableSqmMutationConverter getSqmConverter() {
		return sqmConverter;
	}

	protected Function<SharedSessionContractImplementor, String> getSessionUidAccess() {
		return sessionUidAccess;
	}

	protected JdbcParameterBindings getJdbcParameterBindings() {
		return jdbcParameterBindings;
	}

	protected EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	protected AfterUseAction getAfterUseAction() {
		return afterUseAction;
	}

	protected TableGroup getUpdatingTableGroup() {
		return updatingTableGroup;
	}

	protected Map<TableReference, List<Assignment>> getAssignmentsByTable() {
		return assignmentsByTable;
	}

	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

}
