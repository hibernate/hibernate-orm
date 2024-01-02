/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.inline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.query.sqm.internal.SqmUtil;
import org.hibernate.query.sqm.mutation.internal.MatchingIdSelectionHelper;
import org.hibernate.query.sqm.mutation.internal.UpdateHandler;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmTranslation;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBaseImpl;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.from.ValuesTableGroup;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.SqlSelectionImpl;

/**
 * @author Steve Ebersole
 */
public class InlineUpdateHandler implements UpdateHandler {
	private final SqmUpdateStatement<?> sqmUpdate;
	private final DomainParameterXref domainParameterXref;
	private final MatchingIdRestrictionProducer matchingIdsPredicateProducer;
	private final SessionFactoryImplementor sessionFactory;

	public InlineUpdateHandler(
			MatchingIdRestrictionProducer matchingIdsPredicateProducer,
			SqmUpdateStatement<?> sqmUpdate,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		this.matchingIdsPredicateProducer = matchingIdsPredicateProducer;
		this.domainParameterXref = domainParameterXref;
		this.sqmUpdate = sqmUpdate;
		this.sessionFactory = context.getSession().getFactory();
	}

	@Override
	public int execute(DomainQueryExecutionContext executionContext) {
		final List<Object> ids = MatchingIdSelectionHelper.selectMatchingIds(
				sqmUpdate,
				domainParameterXref,
				executionContext
		);

		if ( ids == null || ids.isEmpty() ) {
			return 0;
		}

		domainParameterXref.clearExpansions();
		final MappingMetamodel domainModel = sessionFactory.getRuntimeMetamodels().getMappingMetamodel();

		final String mutatingEntityName = sqmUpdate.getTarget().getModel().getHibernateEntityName();
		final EntityPersister entityDescriptor = domainModel.getEntityDescriptor( mutatingEntityName );
		final List<Expression> inListExpressions = matchingIdsPredicateProducer.produceIdExpressionList( ids, entityDescriptor );

		//noinspection unchecked
		final SqmTranslation<UpdateStatement> translation = (SqmTranslation<UpdateStatement>) sessionFactory.getQueryEngine()
				.getSqmTranslatorFactory()
				.createMutationTranslator(
						sqmUpdate,
						executionContext.getQueryOptions(),
						domainParameterXref,
						executionContext.getQueryParameterBindings(),
						executionContext.getSession().getLoadQueryInfluencers(),
						sessionFactory
				)
				.translate();
		final TableGroup updatingTableGroup = translation.getSqlAst().getFromClause().getRoots().get( 0 );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// cross-reference the TableReference by alias.  The TableGroup already
		// cross-references it by name, bu the ColumnReference only has the alias

		final Map<String, TableReference> tableReferenceByAlias = CollectionHelper.mapOfSize( updatingTableGroup.getTableReferenceJoins().size() + 1 );
		collectTableReference( updatingTableGroup.getPrimaryTableReference(), tableReferenceByAlias::put );
		for ( int i = 0; i < updatingTableGroup.getTableReferenceJoins().size(); i++ ) {
			collectTableReference( updatingTableGroup.getTableReferenceJoins().get( i ), tableReferenceByAlias::put );
		}

		final JdbcParameterBindings jdbcParameterBindings = SqmUtil.createJdbcParameterBindings(
				executionContext.getQueryParameterBindings(),
				domainParameterXref,
				SqmUtil.generateJdbcParamsXref(
						domainParameterXref,
						translation::getJdbcParamsBySqmParam
				),
				sessionFactory.getRuntimeMetamodels().getMappingMetamodel(),
				navigablePath -> updatingTableGroup,
				new SqmParameterMappingModelResolutionAccess() {
					@Override
					@SuppressWarnings("unchecked")
					public <T> MappingModelExpressible<T> getResolvedMappingModelType(SqmParameter<T> parameter) {
						return (MappingModelExpressible<T>) translation.getSqmParameterMappingModelTypeResolutions().get( parameter );
					}
				},
				executionContext.getSession()
		);


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

		final int rows = ids.size();

		final SqmJdbcExecutionContextAdapter executionContextAdapter = SqmJdbcExecutionContextAdapter.omittingLockingAndPaging( executionContext );
		entityDescriptor.visitConstraintOrderedTables(
				(tableExpression, tableKeyColumnVisitationSupplier) -> updateTable(
						tableExpression,
						tableKeyColumnVisitationSupplier,
						entityDescriptor,
						updatingTableGroup,
						assignmentsByTable,
						inListExpressions,
						rows,
						jdbcParameterBindings,
						executionContextAdapter
				)
		);

		return rows;
	}

	private void updateTable(
			String tableExpression,
			Supplier<Consumer<SelectableConsumer>> tableKeyColumnVisitationSupplier,
			EntityPersister entityDescriptor,
			TableGroup updatingTableGroup,
			Map<TableReference, List<Assignment>> assignmentsByTable,
			List<Expression> inListExpressions,
			int expectedUpdateCount,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext) {
		final TableReference updatingTableReference = updatingTableGroup.getTableReference(
				updatingTableGroup.getNavigablePath(),
				tableExpression,
				false
		);

		final List<Assignment> assignments = assignmentsByTable.get( updatingTableReference );
		if ( assignments == null || assignments.isEmpty() ) {
			// no assignments for this table - skip it
			return;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// create the in-subquery predicate to restrict the updates to just
		// matching ids

		final InListPredicate idListPredicate = (InListPredicate) matchingIdsPredicateProducer.produceRestriction(
				inListExpressions,
				entityDescriptor,
				0,
				null,
				updatingTableReference,
				tableKeyColumnVisitationSupplier,
				executionContext
		);
		final Expression keyExpression = idListPredicate.getTestExpression();


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Create the SQL AST and convert it into a JdbcOperation
		final NamedTableReference dmlTableReference = resolveUnionTableReference( updatingTableReference, tableExpression );
		final UpdateStatement sqlAst = new UpdateStatement( dmlTableReference, assignments, idListPredicate );

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcOperationQueryMutation jdbcUpdate = jdbcServices.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildMutationTranslator( sessionFactory, sqlAst )
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
			final NavigablePath valuesPath = new NavigablePath( "id" );
			final List<Values> valuesList = new ArrayList<>( inListExpressions.size() );
			for ( Expression inListExpression : inListExpressions ) {
				if ( inListExpression instanceof SqlTuple ) {
					//noinspection unchecked
					valuesList.add( new Values( (List<Expression>) ( (SqlTuple) inListExpression ).getExpressions() ) );
				}
				else {
					valuesList.add( new Values( Collections.singletonList( inListExpression ) ) );
				}
			}
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
					valuesList,
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

			final InsertSelectStatement insertSqlAst = new InsertSelectStatement(
					dmlTableReference
			);
			insertSqlAst.addTargetColumnReferences( targetColumnReferences.toArray( new ColumnReference[0] ) );
			insertSqlAst.setSourceSelectStatement( querySpec );

			final JdbcOperationQueryMutation jdbcInsert = jdbcServices.getJdbcEnvironment()
					.getSqlAstTranslatorFactory()
					.buildMutationTranslator( sessionFactory, insertSqlAst )
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
