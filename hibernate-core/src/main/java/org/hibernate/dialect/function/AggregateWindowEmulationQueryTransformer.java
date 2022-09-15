/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.produce.function.StandardFunctions;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.function.SelfRenderingAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.sql.ast.spi.ExpressionReplacementWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.QueryPartTableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.results.internal.ResolvedSqlSelection;
import org.hibernate.type.BasicType;

/**
 * Since the query spec will render a hypothetical set window function instead of an aggregate,
 * the following query transformer will wrap the query spec and apply aggregation in the outer query.
 * We can do this because these functions can only be used in the select clause.
 * A hypothetical set aggregate function like e.g. "rank" returns the rank of a passed value within
 * the ordered set as defined through the WITHIN GROUP clause.
 * When used as window function, the function provides the rank of the current row within the ordered set
 * as defined for the window frame through the OVER clause, but does not do aggregation.
 * The aggregation effect can be achieved by:
 * 1. Selecting the elements by which the ordered set is sorted
 * 2. In the outer query, add a comparison predicate `function_args`=`sort_expressions`
 * 3. Use an arbitrary row produced by the inner query by using e.g. the "min" function
 *
 * The following query
 *
 * <code>
 * select rank(5) within group (order by e.num)
 * from (values (1), (2), (5)) e(num)
 * </code>
 *
 * can be rewritten to
 *
 * <code>
 * select min(t.c1) from (
 *   select rank() over (order by e.num), e.num
 *   from (values (1), (2), (5)) e(num)
 * ) t(c1,c2)
 * where t.c2 = 5
 * </code>
 *
 * @author Christian Beikov
 */
public class AggregateWindowEmulationQueryTransformer implements QueryTransformer {
	private final Over<Object> windowFunction;
	private final List<SortSpecification> withinGroup;
	private final List<SqlAstNode> arguments;

	public AggregateWindowEmulationQueryTransformer(
			Over<Object> windowFunction,
			List<SortSpecification> withinGroup,
			List<SqlAstNode> arguments) {
		this.windowFunction = windowFunction;
		this.withinGroup = withinGroup;
		this.arguments = arguments;
	}

	@Override
	public QuerySpec transform(
			CteContainer cteContainer,
			QuerySpec querySpec,
			SqmToSqlAstConverter converter) {
		final SessionFactoryImplementor factory = converter.getCreationContext()
				.getSessionFactory();
		final QuerySpec outerQuerySpec = new QuerySpec( querySpec.isRoot() );
		final String identifierVariable = "hhh_";
		final NavigablePath navigablePath = new NavigablePath(
				identifierVariable,
				identifierVariable
		);
		final SelectClause selectClause = outerQuerySpec.getSelectClause();
		final QuerySpec subQuerySpec = querySpec.asSubQuery();
		final SelectClause subSelectClause = subQuerySpec.getSelectClause();
		final List<SqlSelection> subSelections = subSelectClause.getSqlSelections();
		final List<String> columnNames = new ArrayList<>( subSelections.size() );
		// A map to find the select item position for an expression
		// which is needed to decide if we need to introduce synthetic select items
		// for group by items, since these group by items are migrated to the outer query
		final Map<Expression, Integer> selectionMapping = new HashMap<>( subSelections.size() );
		// Create the expressions/selections for the outer query and the columnNames list
		// for the QueryPartTableGroup within which the subquery spec is embedded
		for ( int i = 0; i < subSelections.size(); i++ ) {
			final BasicValuedMapping mapping = (BasicValuedMapping) subSelections.get( i )
					.getExpressionType();
			final String columnName = "col" + i;
			final ColumnReference columnReference = new ColumnReference(
					identifierVariable,
					columnName,
					false,
					null,
					null,
					mapping.getJdbcMapping(),
					factory
			);
			final Expression expression = subSelections.get( i ).getExpression();
			final Expression finalExpression;
			if ( expression == windowFunction ) {
				finalExpression = new SelfRenderingAggregateFunctionSqlAstExpression(
						StandardFunctions.MIN,
						(sqlAppender, sqlAstArguments, walker1) -> {
							sqlAppender.appendSql( "min(" );
							sqlAstArguments.get( 0 ).accept( walker1 );
							sqlAppender.append( ')' );
						},
						Collections.singletonList( columnReference ),
						null,
						(ReturnableType<?>) mapping.getMappedType(),
						expression.getExpressionType()
				);
			}
			else {
				finalExpression = columnReference;
				selectionMapping.put( expression, i );
			}
			columnNames.add( columnName );
			selectClause.addSqlSelection(
					new ResolvedSqlSelection(
							i + 1,
							i,
							finalExpression,
							(BasicType<Object>) mapping.getJdbcMapping()
					)
			);
		}

		// Migrate the group by clause to the outer query
		// and push group by expressions into the partition by clause of the window function
		final List<Expression> groupByExpressions = new ArrayList<>(
				subQuerySpec.getGroupByClauseExpressions().size()
		);
		for ( Expression groupByClauseExpression : subQuerySpec.getGroupByClauseExpressions() ) {
			final Expression realExpression;
			final Expression outerGroupByExpression;
			if ( groupByClauseExpression instanceof SqlSelectionExpression ) {
				final SqlSelection selection = ( (SqlSelectionExpression) groupByClauseExpression ).getSelection();
				outerGroupByExpression = new SqlSelectionExpression(
						selectClause.getSqlSelections().get( selection.getValuesArrayPosition() )
				);
				realExpression = selection.getExpression();
			}
			else {
				if ( groupByClauseExpression instanceof SqmPathInterpretation<?> ) {
					realExpression = ( (SqmPathInterpretation<?>) groupByClauseExpression ).getSqlExpression();
				}
				else {
					realExpression = groupByClauseExpression;
				}
				final Integer position = selectionMapping.get( realExpression );
				if ( position == null ) {
					// Group by something that has no corresponding selection item,
					// so we need to introduce an intermediate selection item
					final int valuesPosition = selectClause.getSqlSelections().size();
					final String columnName = "col" + valuesPosition;
					final JdbcMapping jdbcMapping = realExpression.getExpressionType()
							.getJdbcMappings()
							.get( 0 );
					final ColumnReference columnReference = new ColumnReference(
							identifierVariable,
							columnName,
							false,
							null,
							null,
							jdbcMapping,
							factory
					);
					final int subValuesPosition = subSelectClause.getSqlSelections().size();
					final SqlSelection subSelection = new ResolvedSqlSelection(
							subValuesPosition + 1,
							subValuesPosition,
							realExpression,
							(BasicType<Object>) jdbcMapping
					);
					columnNames.add( columnName );
					subSelectClause.addSqlSelection( subSelection );
					outerGroupByExpression = columnReference;
					selectionMapping.put( realExpression, subValuesPosition );
				}
				else {
					outerGroupByExpression = new SqlSelectionExpression(
							selectClause.getSqlSelections().get( position )
					);
				}
			}
			windowFunction.getPartitions().add( realExpression );
			groupByExpressions.add( outerGroupByExpression );
		}
		outerQuerySpec.setGroupByClauseExpressions( groupByExpressions );
		subQuerySpec.setGroupByClauseExpressions( null );

		// Migrate the having clause to the outer query
		if ( subQuerySpec.getHavingClauseRestrictions() != null ) {
			final Predicate predicate = new ExpressionReplacementWalker() {
				@Override
				protected <X extends SqlAstNode> X replaceExpression(X expression) {
					if ( expression instanceof Literal || expression instanceof JdbcParameter ) {
						return expression;
					}
					final Expression outerExpression;
					if ( expression instanceof SqlSelectionExpression ) {
						final SqlSelection selection = ( (SqlSelectionExpression) expression ).getSelection();
						outerExpression = selectClause.getSqlSelections()
								.get( selection.getValuesArrayPosition() )
								.getExpression();
					}
					else {
						final Expression realExpression;
						if ( expression instanceof SqmPathInterpretation<?> ) {
							realExpression = ( (SqmPathInterpretation<?>) expression ).getSqlExpression();
						}
						else {
							realExpression = (Expression) expression;
						}
						final Integer position = selectionMapping.get( realExpression );
						if ( position == null ) {
							// An expression that has no corresponding selection item,
							// so we need to introduce an intermediate selection item
							final int valuesPosition = selectClause.getSqlSelections().size();
							final String columnName = "col" + valuesPosition;
							final JdbcMapping jdbcMapping = realExpression.getExpressionType()
									.getJdbcMappings()
									.get( 0 );
							final ColumnReference columnReference = new ColumnReference(
									identifierVariable,
									columnName,
									false,
									null,
									null,
									jdbcMapping,
									factory
							);
							final int subValuesPosition = subSelectClause.getSqlSelections().size();
							final SqlSelection subSelection = new ResolvedSqlSelection(
									subValuesPosition + 1,
									subValuesPosition,
									realExpression,
									(BasicType<Object>) jdbcMapping
							);
							columnNames.add( columnName );
							subSelectClause.addSqlSelection( subSelection );
							outerExpression = columnReference;
							selectionMapping.put( realExpression, subValuesPosition );
						}
						else {
							outerExpression = selectClause.getSqlSelections().get( position )
									.getExpression();
						}
					}
					return (X) outerExpression;
				}
			}.replaceExpressions( subQuerySpec.getHavingClauseRestrictions() );
			outerQuerySpec.setHavingClauseRestrictions( predicate );
			subQuerySpec.setHavingClauseRestrictions( null );
		}

		// Migrate the order by clause to the outer query
		if ( subQuerySpec.hasSortSpecifications() ) {
			for ( SortSpecification sortSpecification : subQuerySpec.getSortSpecifications() ) {
				final Expression sortExpression = sortSpecification.getSortExpression();
				final Expression outerSortExpression;
				if ( sortExpression instanceof SqlSelectionExpression ) {
					final SqlSelection selection = ( (SqlSelectionExpression) sortExpression ).getSelection();
					outerSortExpression = new SqlSelectionExpression(
							selectClause.getSqlSelections()
									.get( selection.getValuesArrayPosition() )
					);
				}
				else {
					final Expression realExpression;
					if ( sortExpression instanceof SqmPathInterpretation<?> ) {
						realExpression = ( (SqmPathInterpretation<?>) sortExpression ).getSqlExpression();
					}
					else {
						realExpression = sortExpression;
					}
					final Integer position = selectionMapping.get( realExpression );
					if ( position == null ) {
						// Group by something that has no corresponding selection item,
						// so we need to introduce an intermediate selection item
						final int valuesPosition = selectClause.getSqlSelections().size();
						final String columnName = "col" + valuesPosition;
						final JdbcMapping jdbcMapping = realExpression.getExpressionType()
								.getJdbcMappings()
								.get( 0 );
						final ColumnReference columnReference = new ColumnReference(
								identifierVariable,
								columnName,
								false,
								null,
								null,
								jdbcMapping,
								factory
						);
						final int subValuesPosition = subSelectClause.getSqlSelections().size();
						final SqlSelection subSelection = new ResolvedSqlSelection(
								subValuesPosition + 1,
								subValuesPosition,
								realExpression,
								(BasicType<Object>) jdbcMapping
						);
						columnNames.add( columnName );
						subSelectClause.addSqlSelection( subSelection );
						outerSortExpression = columnReference;
						selectionMapping.put( realExpression, subValuesPosition );
					}
					else {
						outerSortExpression = new SqlSelectionExpression(
								selectClause.getSqlSelections().get( position )
						);
					}
				}
				outerQuerySpec.addSortSpecification(
						new SortSpecification(
								outerSortExpression,
								sortSpecification.getSortOrder(),
								sortSpecification.getNullPrecedence()
						)
				);
			}
			subQuerySpec.getSortSpecifications().clear();
		}

		// We need to add selection items for the expressions we order by to the subquery spec.
		final int selectionOffset = columnNames.size();
		// Collect the sorting column references so we can apply the filter later
		final List<ColumnReference> sortingColumns = new ArrayList<>( withinGroup.size() );
		for ( int i = 0; i < withinGroup.size(); i++ ) {
			final int valueIndex = selectionOffset + i;
			final Expression sortExpression = withinGroup.get( i ).getSortExpression();
			final BasicValuedMapping mapping = (BasicValuedMapping) sortExpression.getExpressionType();
			final String columnName = "col" + valueIndex;
			final int oldValueIndex = subSelectClause.getSqlSelections().size();
			columnNames.add( columnName );
			subSelectClause.addSqlSelection(
					new ResolvedSqlSelection(
							oldValueIndex + 1,
							oldValueIndex,
							sortExpression,
							(BasicType<Object>) mapping.getJdbcMapping()
					)
			);
			sortingColumns.add(
					new ColumnReference(
							identifierVariable,
							columnName,
							false,
							null,
							null,
							mapping.getJdbcMapping(),
							factory
					)
			);
		}

		if ( arguments != null ) {
			// Hypothetical set aggregate functions usually provide some rank based on a value
			// i.e. which rank does the value 5 have when ordering by a column ascending
			// So we add a filter to the outer query so we can extract the rank
			switch ( arguments.size() ) {
				case 0:
					break;
				case 1:
					outerQuerySpec.applyPredicate(
							new ComparisonPredicate(
									sortingColumns.get( 0 ),
									ComparisonOperator.EQUAL,
									(Expression) arguments.get( 0 )
							)
					);
					break;
				default:
					outerQuerySpec.applyPredicate(
							new ComparisonPredicate(
									new SqlTuple( sortingColumns, null ),
									ComparisonOperator.EQUAL,
									new SqlTuple(
											(List<? extends Expression>) (List<?>) arguments,
											null
									)
							)
					);
			}
		}

		final QueryPartTableGroup queryPartTableGroup = new QueryPartTableGroup(
				navigablePath,
				null,
				subQuerySpec,
				identifierVariable,
				columnNames,
				false,
				true,
				factory
		);
		outerQuerySpec.getFromClause().addRoot( queryPartTableGroup );

		// Migrate the offset/fetch clause
		outerQuerySpec.setOffsetClauseExpression( subQuerySpec.getOffsetClauseExpression() );
		outerQuerySpec.setFetchClauseExpression(
				subQuerySpec.getFetchClauseExpression(),
				subQuerySpec.getFetchClauseType()
		);
		subQuerySpec.setOffsetClauseExpression( null );
		subQuerySpec.setFetchClauseExpression( null, null );

		return outerQuerySpec;
	}
}
