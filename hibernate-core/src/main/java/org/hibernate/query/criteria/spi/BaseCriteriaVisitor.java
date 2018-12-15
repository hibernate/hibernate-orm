/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public class BaseCriteriaVisitor implements CriteriaVisitor {
	@Override
	public Object visitRootQuery(RootQuery criteriaQuery) {
		visitQueryStructure( criteriaQuery.getQueryStructure() );

		return criteriaQuery;
	}

	@Override
	public Object visitQueryStructure(QueryStructure<?> queryStructure) {
		visitFromClause( queryStructure );
		visitSelectClause( queryStructure );
		visitWhereClause( queryStructure );
		visitOrderByClause( queryStructure );
		visitGrouping( queryStructure );
		visitLimit( queryStructure );

		return queryStructure;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query structure

	protected Object visitFromClause(QueryStructure<?> querySpec) {
		querySpec.getRoots().forEach( root -> root.accept( this ) );
		return querySpec;
	}

	protected Object visitSelectClause(QueryStructure<?> querySpec) {
		querySpec.getSelection().accept( this );
		return querySpec;
	}

	protected Object visitWhereClause(QueryStructure<?> querySpec) {
		querySpec.visitRestriction( restriction -> restriction.accept( this ) );

		return querySpec;
	}

	protected Object visitOrderByClause(QueryStructure<?> querySpec) {
		querySpec.visitSortSpecifications( sortSpec -> sortSpec.accept( this ) );
		return querySpec;
	}

	protected Object visitGrouping(QueryStructure<?> querySpec) {
		final List<? extends ExpressionImplementor<?>> groupByExpressions = querySpec.getGroupingExpressions();
		if ( CollectionHelper.isEmpty( groupByExpressions ) ) {
			return querySpec;
		}

		groupByExpressions.forEach( gb -> gb.accept( this ) );

		if ( querySpec.getGroupRestriction() != null ) {
			querySpec.getGroupRestriction().accept( this );
		}

		return querySpec;
	}

	protected Object visitLimit(QueryStructure<?> querySpec) {
		if ( querySpec.getLimit() == null ) {
			return null;
		}

		querySpec.getLimit().accept( this );

		if ( querySpec.getOffset() != null ) {
			querySpec.getOffset().accept( this );
		}

		return querySpec;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Paths

	@Override
	public Object visitRoot(RootImplementor<?> root) {
		root.visitJoins( join -> join.accept( this ) );
		root.visitFetches( fetch -> fetch.accept( this ) );
		return root;
	}

	@Override
	public Object visitSingularAttributePath(SingularPath<?> path) {
		return path;
	}

	@Override
	public Object visitTreatedPath(TreatedPath<?> treatedPath) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Object visitCorrelationDelegate(CorrelationDelegate<?, ?> correlationDelegate) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Object visitMultiSelect(MultiSelectSelection<?> selection) {
		selection.getSelectionItems().forEach( item -> item.accept( this ) );

		return selection;
	}

	@Override
	public Object visitDynamicInstantiation(ConstructorSelection<?> selection) {
		selection.getSelectionItems().forEach( item -> item.accept( this ) );

		return selection;
	}

	@Override
	public Object visitLiteral(LiteralExpression<?> expression) {
		return expression;
	}

	@Override
	public Object visitNullLiteral(NullLiteralExpression<?> expression) {
		return expression;
	}

	@Override
	public Object visitParameter(ParameterExpression<?> expression) {
		return expression;
	}

	@Override
	public Object visitSearchedCase(SearchedCase<?> expression) {
		expression.getWhenClauses().forEach(
				whenClause -> {
					whenClause.getCondition().accept( this );
					whenClause.getResult().accept( this );
				}
		);

		expression.getOtherwiseResult().accept( this );

		return expression;
	}

	@Override
	public Object visitSimpleCase(SimpleCase<?, ?> expression) {
		expression.getExpression().accept( this );

		expression.getWhenClauses().forEach(
				whenClause -> {
					whenClause.getCondition().accept( this );
					whenClause.getResult().accept( this );
				}
		);

		expression.getOtherwiseResult().accept( this );

		return expression;
	}

	@Override
	public Object visitCoalesceExpression(CoalesceExpression<?> expression) {
		expression.getExpressions().forEach( subExpression -> subExpression.accept( this ) );

		return expression;
	}

	@Override
	public Object visitNullifExpression(NullifExpression<?> expression) {
		expression.getPrimaryExpression().accept( this );
		expression.getSecondaryExpression().accept( this );

		return expression;
	}

	@Override
	public Object visitConcatExpression(ConcatExpression expression) {
		expression.getFirstExpression().accept( this );
		expression.getSecondExpression().accept( this );

		return expression;
	}

	@Override
	public Object visitPathType(PathTypeExpression<?> expression) {
		return expression;
	}

	@Override
	public Object acceptRestrictedSubQueryExpression(RestrictedSubQueryExpression<?> expression) {
		expression.getSubQuery().accept( this );
		return expression;
	}

	@Override
	public Object visitBinaryArithmetic(BinaryArithmetic<?> expression) {
		expression.getLeftHandOperand().accept( this );
		expression.getRightHandOperand().accept( this );
		return expression;
	}

	@Override
	public Object visitUnaryArithmetic(UnaryArithmetic<?> expression) {
		expression.getOperand().accept( this );
		return expression;
	}

	@Override
	public Object visitGenericFunction(GenericFunction<?> function) {
		function.getFunctionArguments().forEach( arg -> arg.accept( this ) );
		return function;
	}

	@Override
	public Object visitAbsFunction(AbsFunction<? extends Number> function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitAvgFunction(AggregationFunction.AVG function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitCastFunction(CastFunction<?, ?> function) {
		function.getCastSource().accept( this );
		return function;
	}

	@Override
	public Object visitCountFunction(AggregationFunction.COUNT function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitCurrentDateFunction(CurrentDateFunction function) {
		return function;
	}

	@Override
	public Object visitCurrentTimeFunction(CurrentTimeFunction function) {
		return function;
	}

	@Override
	public Object visitCurrentTimestampFunction(CurrentTimestampFunction function) {
		return function;
	}

	@Override
	public Object visitGreatestFunction(AggregationFunction.GREATEST<? extends Comparable> function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitLeastFunction(AggregationFunction.LEAST<? extends Comparable> function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitLengthFunction(LengthFunction function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitLocateFunction(LocateFunction function) {
		function.getPattern().accept( this );
		function.getString().accept( this );
		if ( function.getStart() != null ) {
			function.getStart().accept( this );
		}
		return function;
	}

	@Override
	public Object visitLowerFunction(LowerFunction function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitMaxFunction(AggregationFunction.MAX<? extends Number> function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitMinFunction(AggregationFunction.MIN<? extends Number> function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitSqrtFunction(SqrtFunction function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitSubstringFunction(SubstringFunction function) {
		function.getValue().accept( this );
		if ( function.getStart() != null ) {
			function.getStart().accept( this );
		}
		if ( function.getLength() != null ) {
			function.getLength().accept( this );
		}
		return function;
	}

	@Override
	public Object visitSumFunction(AggregationFunction.SUM<? extends Number> function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitTrimFunction(TrimFunction function) {
		if ( function.getTrimCharacter() != null ) {
			function.getTrimCharacter().accept( this );
		}
		function.getTrimSource().accept( this );
		return function;
	}

	@Override
	public Object visitUpperFunction(UpperFunction function) {
		function.getArgument().accept( this );
		return function;
	}

	@Override
	public Object visitNegatedPredicate(NegatedPredicateWrapper predicate) {
		predicate.getWrappedPredicate().accept( this );
		return predicate;
	}

	@Override
	public Object visitJunctionPredicate(Junction predicate) {
		predicate.visitExpressions( expr -> expr.accept( this ) );
		return predicate;
	}

	@Override
	public Object visitBooleanExpressionPredicate(BooleanExpressionPredicate predicate) {
		predicate.getExpression().accept( this );
		return predicate;
	}

	@Override
	public Object visitBooleanAssertionPredicate(BooleanAssertionPredicate predicate) {
		predicate.getExpression().accept( this );
		return predicate;
	}

	@Override
	public Object visitNullnessPredicate(NullnessPredicate predicate) {
		predicate.getExpression().accept( this );
		return predicate;
	}

	@Override
	public Object visitComparisonPredicate(ComparisonPredicate predicate) {
		predicate.getLeftHandOperand().accept( this );
		predicate.getRightHandOperand().accept( this );
		return predicate;
	}

	@Override
	public Object visitBetweenPredicate(BetweenPredicate predicate) {
		predicate.getExpression().accept( this );
		predicate.getLowerBound().accept( this );
		predicate.getUpperBound().accept( this );
		return predicate;
	}

	@Override
	public Object visitLikePredicate(LikePredicate predicate) {
		predicate.getMatchExpression().accept( this );
		predicate.getPattern().accept( this );
		if ( predicate.getEscapeCharacter() != null ) {
			predicate.getEscapeCharacter().accept( this );
		}
		return predicate;
	}

	@Override
	public Object visitInPredicate(InPredicate<?> predicate) {
		predicate.getExpression().accept( this );
		predicate.getValues().forEach( expr -> expr.accept( this ) );
		return predicate;
	}

	@Override
	public Object visitExistsPredicate(ExistsPredicate predicate) {
		predicate.getSubQuery().accept( this );
		return predicate;
	}

	@Override
	public Object visitEmptinessPredicate(EmptinessPredicate predicate) {
		predicate.getPluralPath().accept( this );
		return predicate;
	}

	@Override
	public Object visitMembershipPredicate(MembershipPredicate<?> predicate) {
		predicate.getElementExpression().accept( this );
		predicate.getPluralPath().accept( this );
		return predicate;
	}

	@Override
	public Object visitSortSpecification(SortSpecification sortSpecification) {
		return sortSpecification;
	}
}
