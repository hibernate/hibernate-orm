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

import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.results.TableGroupImpl;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.MultiTableSqmMutationConverter;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcInsert;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcUpdate;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

/**
 * @author Steve Ebersole
 */
public class UpdateExecutionDelegate implements TableBasedUpdateHandler.ExecutionDelegate {
	private final SqmUpdateStatement<?> sqmUpdate;
	private final MultiTableSqmMutationConverter sqmConverter;
	private final TemporaryTable idTable;
	private final AfterUseAction afterUseAction;
	private final Function<SharedSessionContractImplementor, String> sessionUidAccess;
	private final DomainParameterXref domainParameterXref;
	private final TableGroup updatingTableGroup;
	private final Predicate suppliedPredicate;

	private final EntityMappingType entityDescriptor;

	private final JdbcParameterBindings jdbcParameterBindings;

	private final Map<TableReference, List<Assignment>> assignmentsByTable;
	private final Map<SqmParameter<?>, MappingModelExpressible<?>> paramTypeResolutions;
	private final SessionFactoryImplementor sessionFactory;

	public UpdateExecutionDelegate(
			SqmUpdateStatement<?> sqmUpdate,
			MultiTableSqmMutationConverter sqmConverter,
			TemporaryTable idTable,
			AfterUseAction afterUseAction,
			Function<SharedSessionContractImplementor, String> sessionUidAccess,
			DomainParameterXref domainParameterXref,
			TableGroup updatingTableGroup,
			TableReference hierarchyRootTableReference,
			Map<String, TableReference> tableReferenceByAlias,
			List<Assignment> assignments,
			Predicate suppliedPredicate,
			Map<SqmParameter<?>, List<List<JdbcParameter>>> parameterResolutions,
			Map<SqmParameter<?>, MappingModelExpressible<?>> paramTypeResolutions,
			DomainQueryExecutionContext executionContext) {
		this.sqmUpdate = sqmUpdate;
		this.sqmConverter = sqmConverter;
		this.idTable = idTable;
		this.afterUseAction = afterUseAction;
		this.sessionUidAccess = sessionUidAccess;
		this.domainParameterXref = domainParameterXref;
		this.updatingTableGroup = updatingTableGroup;
		this.suppliedPredicate = suppliedPredicate;
		this.paramTypeResolutions = paramTypeResolutions;

		this.sessionFactory = executionContext.getSession().getFactory();

		final ModelPartContainer updatingModelPart = updatingTableGroup.getModelPart();
		assert updatingModelPart instanceof EntityMappingType;

		this.entityDescriptor = (EntityMappingType) updatingModelPart;

		this.assignmentsByTable = CollectionHelper.mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );

		jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref(
						domainParameterXref,
						() -> parameterResolutions
				),
				sessionFactory.getRuntimeMetamodels().getMappingMetamodel(),
				navigablePath -> updatingTableGroup,
				new SqmParameterMappingModelResolutionAccess() {
					@Override @SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) paramTypeResolutions.get(parameter);
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

	private TableReference resolveTableReference(
			ColumnReference columnReference,
			Map<String, TableReference> tableReferenceByAlias) {
		final TableReference tableReferenceByQualifier = tableReferenceByAlias.get( columnReference.getQualifier() );
		if ( tableReferenceByQualifier != null ) {
			return tableReferenceByQualifier;
		}

		throw new SemanticException( "Assignment referred to column of a joined association: " + columnReference );
	}

	private NamedTableReference resolveUnionTableReference(
			TableReference tableReference,
			String tableExpression) {
		if ( tableReference instanceof UnionTableReference ) {
			return new NamedTableReference(
					tableExpression,
					tableReference.getIdentificationVariable(),
					tableReference.isOptional(),
					sessionFactory
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
		final TableReference updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				true,
				true
		);

		final List<Assignment> assignments = assignmentsByTable.get( updatingTableReference );
		if ( assignments == null || assignments.isEmpty() ) {
			// no assignments for this table - skip it
			return;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create the in-subquery predicate to restrict the updates to just
		// matching ids

		final TableKeyExpressionCollector keyColumnCollector = new TableKeyExpressionCollector( entityDescriptor );

		tableKeyColumnVisitationSupplier.get().accept(
				(columnIndex, selection) -> {
					assert selection.getContainingTableExpression().equals( tableExpression );
					keyColumnCollector.apply(
							new ColumnReference(
									(String) null,
									selection,
									sessionFactory
							)
					);
				}
		);

		final Expression keyExpression = keyColumnCollector.buildKeyExpression();
		final InSubQueryPredicate idTableSubQueryPredicate = new InSubQueryPredicate(
				keyExpression,
				idTableSubQuery,
				false
		);


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the SQL AST and convert it into a JdbcOperation
		final NamedTableReference dmlTableReference = resolveUnionTableReference( updatingTableReference, tableExpression );
		final UpdateStatement sqlAst = new UpdateStatement(
				dmlTableReference,
				assignments,
				idTableSubQueryPredicate
		);

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcUpdate jdbcUpdate = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildUpdateTranslator( sessionFactory, sqlAst )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

		final int updateCount = jdbcServices.getJdbcMutationExecutor().execute(
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

		if ( updateCount == expectedUpdateCount ) {
			// We are done when the update count matches
			return;
		}
		// Otherwise we have to check if the table is nullable, and if so, insert into that table
		final AbstractEntityPersister entityPersister = (AbstractEntityPersister) entityDescriptor.getEntityPersister();
		boolean isNullable = false;
		for (int i = 0; i < entityPersister.getTableSpan(); i++) {
			if ( tableExpression.equals( entityPersister.getTableName( i ) ) && entityPersister.isNullableTable( i ) ) {
				isNullable = true;
				break;
			}
		}
		if ( isNullable ) {
			// Copy the subquery contents into a root query
			final QuerySpec querySpec = new QuerySpec( true );
			for ( TableGroup root : idTableSubQuery.getFromClause().getRoots() ) {
				querySpec.getFromClause().addRoot( root );
			}
			for ( SqlSelection sqlSelection : idTableSubQuery.getSelectClause().getSqlSelections() ) {
				querySpec.getSelectClause().addSqlSelection( sqlSelection );
			}
			querySpec.applyPredicate( idTableSubQuery.getWhereClauseRestrictions() );

			// Prepare a not exists sub-query to avoid violating constraints
			final QuerySpec existsQuerySpec = new QuerySpec( false );
			existsQuerySpec.getSelectClause().addSqlSelection(
					new SqlSelectionImpl(
							-1,
							0,
							new QueryLiteral<>(
									1,
									sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( Integer.class )
							)
					)
			);
			final NamedTableReference existsTableReference = new NamedTableReference(
					tableExpression,
					"dml_",
					false,
					sessionFactory
			);
			existsQuerySpec.getFromClause().addRoot(
					new TableGroupImpl(
							null,
							null,
							existsTableReference,
							entityPersister
					)
			);

			final TableKeyExpressionCollector existsKeyColumnCollector = new TableKeyExpressionCollector( entityDescriptor );
			tableKeyColumnVisitationSupplier.get().accept(
					(columnIndex, selection) -> {
						assert selection.getContainingTableExpression().equals( tableExpression );
						existsKeyColumnCollector.apply(
								new ColumnReference(
										existsTableReference,
										selection,
										sessionFactory
								)
						);
					}
			);
			existsQuerySpec.applyPredicate(
					new ComparisonPredicate(
							existsKeyColumnCollector.buildKeyExpression(),
							ComparisonOperator.EQUAL,
							asExpression(idTableSubQuery.getSelectClause())
					)
			);

			querySpec.applyPredicate(
					new ExistsPredicate(
							existsQuerySpec,
							true,
							sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( Boolean.class )
					)
			);

			// Collect the target column references from the key expressions
			final List<ColumnReference> targetColumnReferences = new ArrayList<>();
			if ( keyExpression instanceof SqlTuple ) {
				//noinspection unchecked
				targetColumnReferences.addAll( (Collection<? extends ColumnReference>) ( (SqlTuple) keyExpression ).getExpressions() );
			}
			else {
				targetColumnReferences.add( (ColumnReference) keyExpression );
			}
			// And transform assignments to target column references and selections
			for ( Assignment assignment : assignments ) {
				targetColumnReferences.addAll( assignment.getAssignable().getColumnReferences() );
				querySpec.getSelectClause().addSqlSelection(
						new SqlSelectionImpl(
								0,
								-1,
								assignment.getAssignedValue()
						)
				);
			}

			final InsertStatement insertSqlAst = new InsertStatement(
					dmlTableReference
			);
			insertSqlAst.addTargetColumnReferences( targetColumnReferences.toArray( new ColumnReference[0] ) );
			insertSqlAst.setSourceSelectStatement( querySpec );

			final JdbcInsert jdbcInsert = jdbcServices.getJdbcEnvironment()
					.getSqlAstTranslatorFactory()
					.buildInsertTranslator( sessionFactory, insertSqlAst )
					.translate( jdbcParameterBindings, executionContext.getQueryOptions() );

			final int insertCount = jdbcServices.getJdbcMutationExecutor().execute(
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
			assert insertCount + updateCount == expectedUpdateCount;
		}
	}

	private Expression asExpression(SelectClause selectClause) {
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
}
