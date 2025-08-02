/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.MatchingIdSelectionHelper;
import org.hibernate.query.sqm.mutation.internal.UpdateHandler;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBaseImpl;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableGroup;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * @author Steve Ebersole
 */
public class InlineUpdateHandler extends AbstractInlineHandler implements UpdateHandler {

	private final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamsXref;
	private final Map<SqmParameter<?>, MappingModelExpressible<?>> resolvedParameterMappingModelTypes;

	private final List<TableUpdater> tableUpdaters;

	public InlineUpdateHandler(
			MatchingIdRestrictionProducer matchingIdsPredicateProducer,
			SqmUpdateStatement<?> sqmStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context,
			MutableObject<JdbcParameterBindings> firstJdbcParameterBindings) {
		super( matchingIdsPredicateProducer, sqmStatement, domainParameterXref, context, firstJdbcParameterBindings );

		// Clear expansions created by matching id select statement
		domainParameterXref.clearExpansions();

		final SessionFactoryImplementor sessionFactory = context.getSession().getFactory();
		final SqmTranslator<? extends MutationStatement> translator = sessionFactory.getQueryEngine()
				.getSqmTranslatorFactory()
				.createMutationTranslator(
						sqmStatement,
						context.getQueryOptions(),
						domainParameterXref,
						context.getQueryParameterBindings(),
						context.getSession().getLoadQueryInfluencers(),
						sessionFactory.getSqlTranslationEngine()
				);
		//noinspection unchecked
		final SqmTranslation<UpdateStatement> translation = (SqmTranslation<UpdateStatement>) translator.translate();
		final TableGroup updatingTableGroup = translation.getSqlAst().getFromClause().getRoots().get( 0 );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, bu the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias = CollectionHelper.mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( updatingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < updatingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( updatingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// segment the assignments by table-reference
		final Map<TableReference, List<Assignment>> assignmentsByTable = new HashMap<>();
		final List<Assignment> assignments = translation.getSqlAst().getAssignments();
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
		final List<TableUpdater> tableUpdaters = new ArrayList<>();
		final ExecutionContext executionContext = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( context );
		getEntityDescriptor().visitConstraintOrderedTables(
				(tableExpression, tableKeyColumnVisitationSupplier) -> {
					final TableUpdater tableUpdater = createTableUpdater(
							tableExpression,
							tableKeyColumnVisitationSupplier,
							getEntityDescriptor(),
							updatingTableGroup,
							assignmentsByTable,
							executionContext
					);
					if ( tableUpdater != null ) {
						tableUpdaters.add( tableUpdater );
					}
				}
		);
		this.tableUpdaters = tableUpdaters;

		this.jdbcParamsXref = SqmUtil.generateJdbcParamsXref( domainParameterXref, translator );
		this.resolvedParameterMappingModelTypes = translation.getSqmParameterMappingModelTypeResolutions();

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
		firstJdbcParameterBindings.get().visitBindings( jdbcParameterBindings::addBinding );
		firstJdbcParameterBindings.set( jdbcParameterBindings );
	}

	@Override
	public JdbcParameterBindings createJdbcParameterBindings(DomainQueryExecutionContext context) {
		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				context.getQueryParameterBindings(),
				getDomainParameterXref(),
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
		// super.createJdbcParameterBindings() is for the matching id select statement only,
		// so combine the bindings for the update statement with the ones for the select statement
		super.createJdbcParameterBindings( context ).visitBindings( jdbcParameterBindings::addBinding );
		return jdbcParameterBindings;
	}

	@Override
	public int execute(JdbcParameterBindings jdbcParameterBindings, DomainQueryExecutionContext executionContext) {
		final List<Object> ids = MatchingIdSelectionHelper.selectMatchingIds(
				getMatchingIdsInterpretation(),
				jdbcParameterBindings,
				executionContext
		);

		if ( ids == null || ids.isEmpty() ) {
			return 0;
		}

		final List<Expression> inListExpressions = getMatchingIdsPredicateProducer().produceIdExpressionList( ids, getEntityDescriptor() );
		final int rows = ids.size();

		final SqmJdbcExecutionContextAdapter executionContextAdapter = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext );
		for ( TableUpdater tableUpdater : tableUpdaters ) {
			updateTable(
					tableUpdater,
					inListExpressions,
					rows,
					jdbcParameterBindings,
					executionContextAdapter
			);
		}
		return rows;
	}

	protected record TableUpdater(
			UpdateStatement updateStatement,
			@Nullable InsertSelectStatement nullableInsert,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier
	) {}

	// For Hibernate Reactive
	protected List<TableUpdater> getTableUpdaters() {
		return tableUpdaters;
	}

	private TableUpdater createTableUpdater(
			String tableExpression,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			EntityPersister entityDescriptor,
			TableGroup updatingTableGroup,
			Map<TableReference, List<Assignment>> assignmentsByTable,
			ExecutionContext executionContext) {
		final TableReference updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				false
		);

		final List<Assignment> assignments = assignmentsByTable.get( updatingTableReference );
		if ( assignments == null || assignments.isEmpty() ) {
			// no assignments for this table - skip it
			return null;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create the in-subquery predicate to restrict the updates to just
		// matching ids

		final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
		final int idColumnCount = identifierMapping.getJdbcTypeCount();
		assert idColumnCount > 0;
		final Expression keyExpression;
		if ( idColumnCount == 1 ) {
			final BasicValuedModelPart basicIdMapping = castNonNull( identifierMapping.asBasicValuedModelPart() );
			final String idColumn = basicIdMapping.getSelectionExpression();
			keyExpression = new ColumnReference(
					updatingTableReference,
					idColumn,
					// id columns cannot be formulas and cannot have custom read and write expressions
					false,
					null,
					basicIdMapping.getJdbcMapping()
			);
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( idColumnCount );
			tableKeyColumnVisitationSupplier.get().accept( (columnIndex, selection) -> columnReferences.add(
					new ColumnReference( updatingTableReference, selection )
			) );
			keyExpression = new SqlTuple( columnReferences, identifierMapping );
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the SQL AST and convert it into a JdbcOperation
		final NamedTableReference dmlTableReference = resolveUnionTableReference( updatingTableReference, tableExpression );
		final UpdateStatement sqlAst = new UpdateStatement( dmlTableReference, assignments, null );

		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();

		// Otherwise we have to check if the table is nullable, and if so, insert into that table
		final EntityPersister entityPersister = entityDescriptor.getEntityPersister();
		boolean isNullable = false;
		for (int i = 0; i < entityPersister.getTableSpan(); i++) {
			if ( tableExpression.equals( entityPersister.getTableName( i ) ) && entityPersister.isNullableTable( i ) ) {
				isNullable = true;
				break;
			}
		}
		final InsertSelectStatement insertSqlAst;
		if ( isNullable ) {
			// Copy the subquery contents into a root query
			final QuerySpec querySpec = new QuerySpec( true );
			final NavigablePath valuesPath = new NavigablePath( "id" );
			final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
					true,
					updatingTableGroup.getNavigablePath(),
					updatingTableGroup.getSourceAlias(),
					new SqlAliasBaseImpl( updatingTableGroup.getGroupAlias() ),
					() -> predicate -> {},
					null
			);
			final List<String> columnNames;
			final Predicate joinPredicate;
			if ( keyExpression instanceof SqlTuple ) {
				final List<? extends Expression> expressions = ( (SqlTuple) keyExpression ).getExpressions();
				final List<Expression> lhs = new ArrayList<>( expressions.size() );
				final List<Expression> rhs = new ArrayList<>( expressions.size() );
				columnNames = new ArrayList<>( expressions.size() );
				entityDescriptor.getIdentifierMapping().forEachSelectable(
						(i, selectableMapping) -> {
							final Expression expression = expressions.get( i );
							final ColumnReference columnReference = expression.getColumnReference();
							final ColumnReference valuesColumnReference = new ColumnReference(
									valuesPath.getLocalName(),
									columnReference.getColumnExpression(),
									false,
									null,
									columnReference.getJdbcMapping()
							);
							columnNames.add( columnReference.getColumnExpression() );
							lhs.add( valuesColumnReference );
							rhs.add(
									new ColumnReference(
											rootTableGroup.getPrimaryTableReference(),
											selectableMapping.getSelectionExpression(),
											false,
											null,
											columnReference.getJdbcMapping()
									)
							);
							querySpec.getSelectClause().addSqlSelection(
									new SqlSelectionImpl( valuesColumnReference )
							);
						}
				);
				joinPredicate = new ComparisonPredicate(
						new SqlTuple( lhs, entityDescriptor.getIdentifierMapping() ),
						ComparisonOperator.EQUAL,
						new SqlTuple( rhs, entityDescriptor.getIdentifierMapping() )
				);
			}
			else {
				final ColumnReference columnReference = keyExpression.getColumnReference();
				final ColumnReference valuesColumnReference = new ColumnReference(
						valuesPath.getLocalName(),
						columnReference.getColumnExpression(),
						false,
						null,
						columnReference.getJdbcMapping()
				);
				columnNames = Collections.singletonList( columnReference.getColumnExpression() );
				joinPredicate = new ComparisonPredicate(
						valuesColumnReference,
						ComparisonOperator.EQUAL,
						new ColumnReference(
								rootTableGroup.getPrimaryTableReference(),
								( (BasicEntityIdentifierMapping) entityDescriptor.getIdentifierMapping() ).getSelectionExpression(),
								false,
								null,
								columnReference.getJdbcMapping()
						)
				);
				querySpec.getSelectClause().addSqlSelection(
						new SqlSelectionImpl( valuesColumnReference )
				);
			}
			final ValuesTableGroup valuesTableGroup = new ValuesTableGroup(
					valuesPath,
					null,
					new ArrayList<>(),
					valuesPath.getLocalName(),
					columnNames,
					true,
					sessionFactory
			);
			valuesTableGroup.addNestedTableGroupJoin(
					new TableGroupJoin(
							rootTableGroup.getNavigablePath(),
							SqlAstJoinType.LEFT,
							rootTableGroup,
							joinPredicate
					)
			);
			querySpec.getFromClause().addRoot( valuesTableGroup );
			// Only when the target row does not exist
			querySpec.applyPredicate(
					new NullnessPredicate(
							new ColumnReference(
									rootTableGroup.resolveTableReference( tableExpression ),
									columnNames.get( 0 ),
									entityDescriptor.getIdentifierMapping().getSingleJdbcMapping()
							)
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
								assignment.getAssignedValue()
						)
				);
			}

			insertSqlAst = new InsertSelectStatement( dmlTableReference );
			insertSqlAst.addTargetColumnReferences( targetColumnReferences.toArray( new ColumnReference[0] ) );
			insertSqlAst.setSourceSelectStatement( querySpec );
		}
		else {
			insertSqlAst = null;
		}
		return new TableUpdater( sqlAst, insertSqlAst, tableKeyColumnVisitationSupplier );
	}

	// For Hibernate Reactive
	protected JdbcOperationQueryMutation createTableUpdate(
			TableUpdater tableUpdater,
			List<Expression> inListExpressions,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create the in-subquery predicate to restrict the updates to just
		// matching ids

		final UpdateStatement updateStatement = new UpdateStatement(
				tableUpdater.updateStatement,
				tableUpdater.updateStatement.getTargetTable(),
				tableUpdater.updateStatement.getFromClause(),
				tableUpdater.updateStatement.getAssignments(),
				Predicate.combinePredicates(
						tableUpdater.updateStatement.getRestriction(),
						getMatchingIdsPredicateProducer().produceRestriction(
								inListExpressions,
								getEntityDescriptor(),
								0,
								null,
								tableUpdater.updateStatement.getTargetTable(),
								tableUpdater.tableKeyColumnVisitationSupplier,
								executionContext
						)
				),
				tableUpdater.updateStatement.getReturningColumns()
		);

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the SQL AST and convert it into a JdbcOperation

		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		return jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( sessionFactory, updateStatement )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
	}

	// For Hibernate Reactive
	protected JdbcOperationQueryMutation createTableInsert(
			TableUpdater tableUpdater,
			List<Expression> inListExpressions,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();
		final InsertSelectStatement insertStatement = new InsertSelectStatement(
				tableUpdater.nullableInsert,
				tableUpdater.nullableInsert.getTargetTable(),
				tableUpdater.nullableInsert.getReturningColumns()
		);
		final QuerySpec originalQuerySpec = (QuerySpec) tableUpdater.nullableInsert.getSourceSelectStatement();
		assert originalQuerySpec.getFromClause().getRoots().size() == 1;

		final QuerySpec querySpec = new QuerySpec( true, 1 );

		// Copy over everything except the FromClause
		querySpec.getSelectClause().makeDistinct( originalQuerySpec.getSelectClause().isDistinct() );
		for ( SqlSelection sqlSelection : originalQuerySpec.getSelectClause().getSqlSelections() ) {
			querySpec.getSelectClause().addSqlSelection( sqlSelection );
		}
		querySpec.applyPredicate( originalQuerySpec.getWhereClauseRestrictions() );
		querySpec.setGroupByClauseExpressions( originalQuerySpec.getGroupByClauseExpressions() );
		querySpec.setHavingClauseRestrictions( originalQuerySpec.getHavingClauseRestrictions() );
		for ( SortSpecification sortSpecification : originalQuerySpec.getSortSpecifications() ) {
			querySpec.addSortSpecification( sortSpecification );
		}
		querySpec.setOffsetClauseExpression( originalQuerySpec.getOffsetClauseExpression() );
		querySpec.setFetchClauseExpression( originalQuerySpec.getFetchClauseExpression(),
				originalQuerySpec.getFetchClauseType() );

		// Rebuild the from clause values list based on the inListExpressions
		final List<Values> valuesList = new ArrayList<>( inListExpressions.size() );
		for ( Expression inListExpression : inListExpressions ) {
			if ( inListExpression instanceof SqlTuple ) {
				//noinspection unchecked
				valuesList.add( new Values( (List<Expression>) ((SqlTuple) inListExpression).getExpressions() ) );
			}
			else {
				valuesList.add( new Values( Collections.singletonList( inListExpression ) ) );
			}
		}

		final ValuesTableGroup originalValuesTableGroup =
				(ValuesTableGroup) originalQuerySpec.getFromClause().getRoots().get( 0 );
		final ValuesTableGroup valuesTableGroup = new ValuesTableGroup(
				originalValuesTableGroup.getNavigablePath(),
				(TableGroupProducer) originalValuesTableGroup.getModelPart(),
				valuesList,
				originalValuesTableGroup.getNavigablePath().getLocalName(),
				originalValuesTableGroup.getPrimaryTableReference().getColumnNames(),
				originalValuesTableGroup.canUseInnerJoins(),
				sessionFactory
		);
		valuesTableGroup.addNestedTableGroupJoin( originalValuesTableGroup.getNestedTableGroupJoins().get( 0 ) );
		querySpec.getFromClause().addRoot( valuesTableGroup );

		insertStatement.addTargetColumnReferences( tableUpdater.nullableInsert.getTargetColumns() );
		insertStatement.setSourceSelectStatement( querySpec );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the SQL AST and convert it into a JdbcOperation

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		return jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( sessionFactory, insertStatement )
				.translate( jdbcParameterBindings, executionContext.getQueryOptions() );
	}

	private void updateTable(
			TableUpdater tableUpdater,
			List<Expression> inListExpressions,
			int expectedUpdateCount,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final SessionFactoryImplementor sessionFactory = executionContext.getSession().getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final int updateCount = jdbcServices.getJdbcMutationExecutor().execute(
				createTableUpdate( tableUpdater, inListExpressions, jdbcParameterBindings, executionContext ),
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
		if ( tableUpdater.nullableInsert != null ) {
			final int insertCount = jdbcServices.getJdbcMutationExecutor().execute(
					createTableInsert(  tableUpdater, inListExpressions, jdbcParameterBindings, executionContext ),
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

	private void collectTableReference(
			TableReference tableReference,
			BiConsumer<String, TableReference> consumer) {
		consumer.accept( tableReference.getIdentificationVariable(), tableReference );
	}

	private void collectTableReference(
			TableReferenceJoin tableReferenceJoin,
			BiConsumer<String, TableReference> consumer) {
		collectTableReference( tableReferenceJoin.getJoinedTableReference(), consumer );
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
					tableReference.isOptional()
			);
		}
		return (NamedTableReference) tableReference;
	}
}
