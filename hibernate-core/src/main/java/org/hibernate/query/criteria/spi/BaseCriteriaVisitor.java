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
	public <R> R visitRootQuery(RootQuery criteriaQuery) {
		visitQueryStructure( criteriaQuery.getQueryStructure() );

		return (R) criteriaQuery;
	}

	@Override
	public <R> R visitQueryStructure(QueryStructure<?> queryStructure) {
		visitFromClause( queryStructure );
		visitSelectClause( queryStructure );
		visitWhereClause( queryStructure );
		visitOrderByClause( queryStructure );
		visitGrouping( queryStructure );
		visitLimit( queryStructure );

		return (R) queryStructure;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query structure

	protected <R> R visitFromClause(QueryStructure<?> querySpec) {
		querySpec.getRoots().forEach( root -> root.accept( this ) );
		return (R) querySpec;
	}

	protected <R> R visitSelectClause(QueryStructure<?> querySpec) {
		querySpec.getSelection().accept( this );
		return (R) querySpec;
	}

	protected <R> R visitWhereClause(QueryStructure<?> querySpec) {
		querySpec.visitRestriction( restriction -> restriction.accept( this ) );

		return (R) querySpec;
	}

	protected <R> R visitOrderByClause(QueryStructure<?> querySpec) {
		querySpec.visitSortSpecifications( sortSpec -> sortSpec.accept( this ) );
		return (R) querySpec;
	}

	protected <R> R visitGrouping(QueryStructure<?> querySpec) {
		final List<? extends ExpressionImplementor<?>> groupByExpressions = querySpec.getGroupingExpressions();
		if ( CollectionHelper.isEmpty( groupByExpressions ) ) {
			return (R) querySpec;
		}

		groupByExpressions.forEach( gb -> gb.accept( this ) );

		if ( querySpec.getGroupRestriction() != null ) {
			querySpec.getGroupRestriction().accept( this );
		}

		return (R) querySpec;
	}

	protected <R> R visitLimit(QueryStructure<?> querySpec) {
		if ( querySpec.getLimit() == null ) {
			return null;
		}

		querySpec.getLimit().accept( this );

		if ( querySpec.getOffset() != null ) {
			querySpec.getOffset().accept( this );
		}

		return (R) querySpec;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Paths

	@Override
	public <R> R visitRoot(RootImpl<?> root) {
		return (R) root;
	}

	@Override
	public <R> R visitSingularAttributePath(SingularPath<?> path) {
		return (R) path;
	}

	@Override
	public <R> R visitTreatedPath(TreatedPath<?> treatedPath) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <R> R visitCorrelationDelegate(CorrelationDelegate<?, ?> correlationDelegate) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public <R> R visitMultiSelect(MultiSelectSelection<?> selection) {
		selection.getSelectionItems().forEach( item -> item.accept( this ) );

		return (R) selection;
	}

	@Override
	public <R> R visitDynamicInstantiation(ConstructorSelection<?> selection) {
		selection.getSelectionItems().forEach( item -> item.accept( this ) );

		return (R) selection;
	}

	@Override
	public <R> R visitLiteral(LiteralExpression<?> expression) {
		return (R) expression;
	}

	@Override
	public <R> R visitNullLiteral(NullLiteralExpression<?> expression) {
		return (R) expression;
	}

	@Override
	public <R> R visitParameter(ParameterExpression<?> expression) {
		return (R) expression;
	}

	@Override
	public <R> R visitSearchedCase(SearchedCase<?> expression) {
		expression.getWhenClauses().forEach(
				whenClause -> {
					whenClause.getCondition().accept( this );
					whenClause.getResult().accept( this );
				}
		);

		expression.getOtherwiseResult().accept( this );

		return (R) expression;
	}

	@Override
	public <R> R visitSimpleCase(SimpleCase<?, ?> expression) {
		expression.getExpression().accept( this );

		expression.getWhenClauses().forEach(
				whenClause -> {
					whenClause.getCondition().accept( this );
					whenClause.getResult().accept( this );
				}
		);

		expression.getOtherwiseResult().accept( this );

		return (R) expression;
	}

	@Override
	public <R> R visitCoalesceExpression(CoalesceExpression<?> expression) {
		expression.getExpressions().forEach( subExpression -> subExpression.accept( this ) );

		return (R) expression;
	}

	@Override
	public <R> R visitNullifExpression(NullifExpression<?> expression) {
		expression.getPrimaryExpression().accept( this );
		expression.getSecondaryExpression().accept( this );

		return (R) expression;
	}

	@Override
	public <R> R visitConcatExpression(ConcatExpression expression) {
		expression.getFirstExpression().accept( this );
		expression.getSecondExpression().accept( this );

		return (R) expression;
	}

	@Override
	public <R> R visitPathType(PathTypeExpression<?> expression) {
		return (R) expression;
	}

	@Override
	public <R> R acceptRestrictedSubQueryExpression(RestrictedSubQueryExpression<?> expression) {
		expression.getSubQuery().accept( this );
		return (R) expression;
	}

	@Override
	public <R> R visitBinaryArithmetic(BinaryArithmetic<?> expression) {
		expression.getLeftHandOperand().accept( this );
		expression.getRightHandOperand().accept( this );
		return (R) expression;
	}

	@Override
	public <R> R visitUnaryArithmetic(UnaryArithmetic<?> expression) {
		expression.getOperand().accept( this );
		return (R) expression;
	}

	@Override
	public <R> R visitGenericFunction(GenericFunction<?> function) {
		function.getFunctionArguments().forEach( arg -> arg.accept( this ) );
		return (R) function;
	}

	@Override
	public <R> R visitAbsFunction(AbsFunction<? extends Number> function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitAvgFunction(AggregationFunction.AVG function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitCastFunction(CastFunction<?, ?> function) {
		function.getCastSource().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitCountFunction(AggregationFunction.COUNT function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitCurrentDateFunction(CurrentDateFunction function) {
		return (R) function;
	}

	@Override
	public <R> R visitCurrentTimeFunction(CurrentTimeFunction function) {
		return (R) function;
	}

	@Override
	public <R> R visitCurrentTimestampFunction(CurrentTimestampFunction function) {
		return (R) function;
	}

	@Override
	public <R> R visitGreatestFunction(AggregationFunction.GREATEST<? extends Comparable> function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitLeastFunction(AggregationFunction.LEAST<? extends Comparable> function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitLengthFunction(LengthFunction function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitLocateFunction(LocateFunction function) {
		function.getPattern().accept( this );
		function.getString().accept( this );
		if ( function.getStart() != null ) {
			function.getStart().accept( this );
		}
		return (R) function;
	}

	@Override
	public <R> R visitLowerFunction(LowerFunction function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitMaxFunction(AggregationFunction.MAX<? extends Number> function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitMinFunction(AggregationFunction.MIN<? extends Number> function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitSqrtFunction(SqrtFunction function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitSubstringFunction(SubstringFunction function) {
		function.getValue().accept( this );
		if ( function.getStart() != null ) {
			function.getStart().accept( this );
		}
		if ( function.getLength() != null ) {
			function.getLength().accept( this );
		}
		return (R) function;
	}

	@Override
	public <R> R visitSumFunction(AggregationFunction.SUM<? extends Number> function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitTrimFunction(TrimFunction function) {
		if ( function.getTrimCharacter() != null ) {
			function.getTrimCharacter().accept( this );
		}
		function.getTrimSource().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitUpperFunction(UpperFunction function) {
		function.getArgument().accept( this );
		return (R) function;
	}

	@Override
	public <R> R visitNegatedPredicate(NegatedPredicateWrapper predicate) {
		predicate.getWrappedPredicate().accept( this );
		return (R) predicate;
	}

	@Override
	public <R> R visitJunctionPredicate(Junction predicate) {
		predicate.visitExpressions( expr -> expr.accept( this ) );
		return (R) predicate;
	}

	@Override
	public <R> R visitBooleanExpressionPredicate(BooleanExpressionPredicate predicate) {
		predicate.getExpression().accept( this );
		return (R) predicate;
	}

	@Override
	public <R> R visitBooleanAssertionPredicate(BooleanAssertionPredicate predicate) {
		predicate.getExpression().accept( this );
		return (R) predicate;
	}

	@Override
	public <R> R visitNullnessPredicate(NullnessPredicate predicate) {
		predicate.getExpression().accept( this );
		return (R) predicate;
	}

	@Override
	public <R> R visitComparisonPredicate(ComparisonPredicate predicate) {
		predicate.getLeftHandOperand().accept( this );
		predicate.getRightHandOperand().accept( this );
		return (R) predicate;
	}

	@Override
	public <R> R visitBetweenPredicate(BetweenPredicate predicate) {
		predicate.getExpression().accept( this );
		predicate.getLowerBound().accept( this );
		predicate.getUpperBound().accept( this );
		return (R) predicate;
	}

	@Override
	public <R> R visitLikePredicate(LikePredicate predicate) {
		predicate.getMatchExpression().accept( this );
		predicate.getPattern().accept( this );
		if ( predicate.getEscapeCharacter() != null ) {
			predicate.getEscapeCharacter().accept( this );
		}
		return (R) predicate;
	}

	@Override
	public <R> R visitInPredicate(InPredicate<?> predicate) {
		predicate.getExpression().accept( this );
		predicate.getValues().forEach( expr -> expr.accept( this ) );
		return (R) predicate;
	}

	@Override
	public <R> R visitExistsPredicate(ExistsPredicate predicate) {
		predicate.getSubQuery().accept( this );
		return (R) predicate;
	}

	@Override
	public <R> R visitEmptinessPredicate(EmptinessPredicate predicate) {
		predicate.getPluralPath().accept( this );
		return (R) predicate;
	}

	@Override
	public <R> R visitMembershipPredicate(MembershipPredicate<?> predicate) {
		predicate.getElementExpression().accept( this );
		predicate.getPluralPath().accept( this );
		return (R) predicate;
	}

	@Override
	public <R> R visitSortSpecification(SortSpecification sortSpecification) {
		return (R) sortSpecification;
	}
}
