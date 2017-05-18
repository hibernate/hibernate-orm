/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria;

import java.util.List;

import org.hibernate.metamodel.queryable.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.produce.spi.criteria.from.JpaFrom;
import org.hibernate.query.sqm.produce.spi.criteria.from.JpaRoot;
import org.hibernate.query.sqm.produce.spi.criteria.path.JpaPluralAttributePath;
import org.hibernate.query.sqm.tree.expression.BinaryArithmeticSqmExpression;
import org.hibernate.query.sqm.tree.expression.CoalesceSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConcatSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConstantEnumSqmExpression;
import org.hibernate.query.sqm.tree.expression.EntityTypeLiteralSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SubQuerySqmExpression;
import org.hibernate.query.sqm.tree.expression.UnaryOperationSqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.expression.function.AvgFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CastFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CountFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CountStarFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.GenericFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.MaxFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.MinFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SumFunctionSqmExpression;
import org.hibernate.query.sqm.tree.predicate.AndSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BetweenSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BooleanExpressionSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.EmptinessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InListSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InSubQuerySqmPredicate;
import org.hibernate.query.sqm.tree.predicate.LikeSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.MemberOfSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NegatedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NullnessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.OrSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.RelationalPredicateOperator;
import org.hibernate.query.sqm.tree.predicate.RelationalSqmPredicate;

/**
 * @author Steve Ebersole
 */
public interface CriteriaVisitor {
	ParsingContext getParsingContext();

	<T extends Enum> ConstantEnumSqmExpression<T> visitEnumConstant(T value);

	<T> LiteralSqmExpression<T> visitConstant(T value);
	<T> LiteralSqmExpression<T> visitConstant(T value, Class<T> javaType);

	UnaryOperationSqmExpression visitUnaryOperation(
			UnaryOperationSqmExpression.Operation operation,
			JpaExpression<?> expression);

	UnaryOperationSqmExpression visitUnaryOperation(
			UnaryOperationSqmExpression.Operation operation,
			JpaExpression<?> expression,
			BasicValuedExpressableType resultType);

	BinaryArithmeticSqmExpression visitArithmetic(
			BinaryArithmeticSqmExpression.Operation operation,
			JpaExpression<?> expression1,
			JpaExpression<?> expression2);

	BinaryArithmeticSqmExpression visitArithmetic(
			BinaryArithmeticSqmExpression.Operation operation,
			JpaExpression<?> expression1,
			JpaExpression<?> expression2,
			BasicValuedExpressableType resultType);

//	SingularAttributeBinding visitSingularAttributePath(JpaSingularAttributePath attributePath);
//	SingularAttributeBinding visitPluralAttributePath(JpaPluralAttributePath attributePath);
//	// todo : visitPluralAttributeElementPath and visitPluralAttributeIndex

	SqmSingularAttributeReference visitAttributeReference(JpaFrom<?, ?> attributeSource, String attributeName);

	GenericFunctionSqmExpression visitFunction(
			String name,
			BasicValuedExpressableType resultTypeDescriptor,
			List<JpaExpression<?>> arguments);
	GenericFunctionSqmExpression visitFunction(
			String name,
			BasicValuedExpressableType resultTypeDescriptor,
			JpaExpression<?>... arguments);

	AvgFunctionSqmExpression visitAvgFunction(JpaExpression<?> expression, boolean distinct);
	AvgFunctionSqmExpression visitAvgFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType);

	CountFunctionSqmExpression visitCountFunction(JpaExpression<?> expression, boolean distinct);
	CountFunctionSqmExpression visitCountFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType);

	CountStarFunctionSqmExpression visitCountStarFunction(boolean distinct);
	CountStarFunctionSqmExpression visitCountStarFunction(boolean distinct, BasicValuedExpressableType resultType);

	MaxFunctionSqmExpression visitMaxFunction(JpaExpression<?> expression, boolean distinct);
	MaxFunctionSqmExpression visitMaxFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType);

	MinFunctionSqmExpression visitMinFunction(JpaExpression<?> expression, boolean distinct);
	MinFunctionSqmExpression visitMinFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType);

	SumFunctionSqmExpression visitSumFunction(JpaExpression<?> expression, boolean distinct);
	SumFunctionSqmExpression visitSumFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType);

	ConcatSqmExpression visitConcat(
			JpaExpression<?> expression1,
			JpaExpression<?> expression2);

	ConcatSqmExpression visitConcat(
			JpaExpression<?> expression1,
			JpaExpression<?> expression2,
			BasicValuedExpressableType resultType);

	CoalesceSqmExpression visitCoalesce(List<JpaExpression<?>> expressions);

	EntityTypeLiteralSqmExpression visitEntityType(String identificationVariable);
	EntityTypeLiteralSqmExpression visitEntityType(String identificationVariable, String attributeName);

//	CollectionSizeFunction visitPluralAttributeSizeFunction();
//
//	CollectionValueFunction visitPluralAttributeElementBinding();
//	MapKeyFunction visitMapKeyBinding();
//	MapEntryFunction visitMapEntryFunction();

	SubQuerySqmExpression visitSubQuery(JpaSubquery jpaSubquery);

	AndSqmPredicate visitAndPredicate(List<JpaPredicate> predicates);
	OrSqmPredicate visitOrPredicate(List<JpaPredicate> predicates);

	EmptinessSqmPredicate visitEmptinessPredicate(JpaPluralAttributePath pluralAttributePath, boolean negated);
	MemberOfSqmPredicate visitMemberOfPredicate(JpaPluralAttributePath pluralAttributePath, boolean negated);

	BetweenSqmPredicate visitBetweenPredicate(
			JpaExpression<?> expression,
			JpaExpression<?> lowerBound,
			JpaExpression<?> upperBound,
			boolean negated);


	LikeSqmPredicate visitLikePredicate(
			JpaExpression<String> matchExpression,
			JpaExpression<String> pattern,
			JpaExpression<Character> escapeCharacter,
			boolean negated);

	InSubQuerySqmPredicate visitInSubQueryPredicate(
			JpaExpression<?> testExpression,
			JpaSubquery<?> subquery,
			boolean negated);

	InListSqmPredicate visitInTupleListPredicate(
			JpaExpression<?> testExpression,
			List<JpaExpression<?>> listExpressions,
			boolean negated);

	SqmExpression visitRoot(JpaRoot root);

	SqmExpression visitParameter(String name, int position, Class javaType);

	<T,C> CastFunctionSqmExpression visitCastFunction(JpaExpression<T> expressionToCast, Class<C> castTarget);

	GenericFunctionSqmExpression visitGenericFunction(
			String functionName,
			BasicValuedExpressableType resultType,
			List<JpaExpression<?>> arguments);

	NegatedSqmPredicate visitNegatedPredicate(JpaPredicate affirmativePredicate);

	BooleanExpressionSqmPredicate visitBooleanExpressionPredicate(
			JpaExpression<Boolean> testExpression,
			Boolean assertValue);

	NullnessSqmPredicate visitNullnessPredicate(JpaExpression<?> testExpression);

	RelationalSqmPredicate visitRelationalPredicate(
			RelationalPredicateOperator operator,
			JpaExpression<?> lhs,
			JpaExpression<?> rhs);

	void visitDynamicInstantiation(Class target, List<JpaExpression<?>> arguments);
}
