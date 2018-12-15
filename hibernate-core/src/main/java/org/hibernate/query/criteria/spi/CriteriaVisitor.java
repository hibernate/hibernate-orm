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
	Object visitRootQuery(RootQuery rootQuery);

	Object visitQueryStructure(QueryStructure<?> queryStructure);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Paths

	Object visitRoot(RootImplementor<?> root);

	Object visitSingularAttributePath(SingularPath<?> singularAttributePath);

	Object visitTreatedPath(TreatedPath<?> treatedPath);

	Object visitCorrelationDelegate(CorrelationDelegate<?,?> correlationDelegate);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Selections

	Object visitMultiSelect(MultiSelectSelection<?> selection);

	Object visitDynamicInstantiation(ConstructorSelection<?> selection);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	Object visitLiteral(LiteralExpression<?> expression);
	Object visitNullLiteral(NullLiteralExpression<?> expression);

	Object visitParameter(ParameterExpression<?> expression);

	Object visitSearchedCase(SearchedCase<?> expression);
	Object visitSimpleCase(SimpleCase<?,?> expression);

	Object visitCoalesceExpression(CoalesceExpression<?> expression);

	Object visitNullifExpression(NullifExpression<?> expression);

	Object visitConcatExpression(ConcatExpression expression);

	Object visitPathType(PathTypeExpression<?> expression);

	Object acceptRestrictedSubQueryExpression(RestrictedSubQueryExpression<?> expression);

	Object visitBinaryArithmetic(BinaryArithmetic<?> expression);

	Object visitUnaryArithmetic(UnaryArithmetic<?> expression);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Functions

	Object visitGenericFunction(GenericFunction<?> function);

	Object visitAbsFunction(AbsFunction<? extends Number> function);
	Object visitAvgFunction(AggregationFunction.AVG function);
	Object visitCastFunction(CastFunction<?,?> function);
	Object visitCountFunction(AggregationFunction.COUNT function);
	Object visitCurrentDateFunction(CurrentDateFunction function);
	Object visitCurrentTimeFunction(CurrentTimeFunction function);
	Object visitCurrentTimestampFunction(CurrentTimestampFunction function);
	Object visitGreatestFunction(AggregationFunction.GREATEST<? extends Comparable> function);
	Object visitLeastFunction(AggregationFunction.LEAST<? extends Comparable> function);
	Object visitLengthFunction(LengthFunction function);
	Object visitLocateFunction(LocateFunction function);
	Object visitLowerFunction(LowerFunction function);
	Object visitMaxFunction(AggregationFunction.MAX<? extends Number> function);
	Object visitMinFunction(AggregationFunction.MIN<? extends Number> function);
	Object visitSqrtFunction(SqrtFunction function);
	Object visitSubstringFunction(SubstringFunction function);
	Object visitSumFunction(AggregationFunction.SUM<? extends Number> function);
	Object visitTrimFunction(TrimFunction function);
	Object visitUpperFunction(UpperFunction function);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates

	Object visitNegatedPredicate(NegatedPredicateWrapper predicate);

	Object visitJunctionPredicate(Junction predicate);

	Object visitBooleanExpressionPredicate(BooleanExpressionPredicate predicate);

	Object visitBooleanAssertionPredicate(BooleanAssertionPredicate predicate);

	Object visitNullnessPredicate(NullnessPredicate predicate);

	Object visitComparisonPredicate(ComparisonPredicate predicate);

	Object visitBetweenPredicate(BetweenPredicate predicate);

	Object visitLikePredicate(LikePredicate predicate);

	Object visitInPredicate(InPredicate<?> predicate);

	Object visitExistsPredicate(ExistsPredicate predicate);

	Object visitEmptinessPredicate(EmptinessPredicate predicate);

	Object visitMembershipPredicate(MembershipPredicate<?> membershipPredicate);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Sorting

	Object visitSortSpecification(SortSpecification sortSpecification);
}
