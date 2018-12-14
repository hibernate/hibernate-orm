/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * @author Steve Ebersole
 */
public interface CriteriaVisitor {
	<R> R visitRootQuery(RootQuery rootQuery);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Paths

	<R> R visitRoot(RootImpl<?> root);

	<R> R visitSingularAttributePath(SingularPath<?> singularAttributePath);

	<R> R visitTreatedPath(TreatedPath<?> treatedPath);

	<R> R visitCorrelationDelegate(CorrelationDelegate<?,?> correlationDelegate);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Selections

	<R> R visitMultiSelect(MultiSelectSelection<?> selection);

	<R> R visitDynamicInstantiation(ConstructorSelection<?> selection);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	<R> R visitLiteral(LiteralExpression<?> expression);
	<R> R visitNullLiteral(NullLiteralExpression<?> expression);

	<R> R visitParameter(ParameterExpression<?> expression);

	<R> R visitSearchedCase(SearchedCase<?> expression);
	<R> R visitSimpleCase(SimpleCase<?,?> expression);

	<R> R visitCoalesceExpression(CoalesceExpression<?> expression);

	<R> R visitNullifExpression(NullifExpression<?> expression);

	<R> R visitConcatExpression(ConcatExpression expression);

	<R> R visitPathType(PathTypeExpression<?> expression);

	<R> R acceptRestrictedSubQueryExpression(RestrictedSubQueryExpression<?> expression);

	<R> R visitBinaryArithmetic(BinaryArithmetic<?> expression);

	<R> R visitUnaryArithmetic(UnaryArithmetic<?> expression);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Functions

	<R> R visitGenericFunction(GenericFunction<?> function);

	<R> R visitAbsFunction(AbsFunction<? extends Number> function);
	<R> R visitAvgFunction(AggregationFunction.AVG function);
	<R> R visitCastFunction(CastFunction<?,?> function);
	<R> R visitCountFunction(AggregationFunction.COUNT function);
	<R> R visitCurrentDateFunction(CurrentDateFunction function);
	<R> R visitCurrentTimeFunction(CurrentTimeFunction function);
	<R> R visitCurrentTimestampFunction(CurrentTimestampFunction function);
	<R> R visitGreatestFunction(AggregationFunction.GREATEST<? extends Comparable> function);
	<R> R visitLeastFunction(AggregationFunction.LEAST<? extends Comparable> function);
	<R> R visitLengthFunction(LengthFunction function);
	<R> R visitLocateFunction(LocateFunction function);
	<R> R visitLowerFunction(LowerFunction function);
	<R> R visitMaxFunction(AggregationFunction.MAX<? extends Number> function);
	<R> R visitMinFunction(AggregationFunction.MIN<? extends Number> function);
	<R> R visitSqrtFunction(SqrtFunction function);
	<R> R visitSubstringFunction(SubstringFunction function);
	<R> R visitSumFunction(AggregationFunction.SUM<? extends Number> function);
	<R> R visitTrimFunction(TrimFunction function);
	<R> R visitUpperFunction(UpperFunction function);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates

	<R> R visitNegatedPredicate(NegatedPredicateWrapper predicate);

	<R> R visitJunctionPredicate(Junction predicate);

	<R> R visitBooleanExpressionPredicate(BooleanExpressionPredicate predicate);

	<R> R visitBooleanAssertionPredicate(BooleanAssertionPredicate predicate);

	<R> R visitNullnessPredicate(NullnessPredicate predicate);

	<R> R visitComparisonPredicate(ComparisonPredicate predicate);

	<R> R visitBetweenPredicate(BetweenPredicate predicate);

	<R> R visitLikePredicate(LikePredicate predicate);

	<R> R visitInPredicate(InPredicate<?> predicate);

	<R> R visitExistsPredicate(ExistsPredicate predicate);

	<R> R visitEmptinessPredicate(EmptinessPredicate predicate);

	<R> R visitMembershipPredicate(MembershipPredicate<?> membershipPredicate);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Sorting

	<R> R visitSortSpecification(SortSpecification sortSpecification);
}
