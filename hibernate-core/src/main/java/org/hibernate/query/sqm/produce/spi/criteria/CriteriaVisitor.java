/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria;

import java.util.List;

import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.produce.spi.criteria.from.JpaFrom;
import org.hibernate.query.sqm.produce.spi.criteria.from.JpaRoot;
import org.hibernate.query.sqm.produce.spi.criteria.path.JpaPluralAttributePath;
import org.hibernate.query.sqm.tree.expression.BinaryArithmeticSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SqmCoalesceFunction;
import org.hibernate.query.sqm.tree.expression.ConcatSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConstantEnumSqmExpression;
import org.hibernate.query.sqm.tree.expression.EntityTypeLiteralSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SubQuerySqmExpression;
import org.hibernate.query.sqm.tree.expression.UnaryOperationSqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountStarFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
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

	SqmGenericFunction visitFunction(
			String name,
			BasicValuedExpressableType resultTypeDescriptor,
			List<JpaExpression<?>> arguments);
	SqmGenericFunction visitFunction(
			String name,
			BasicValuedExpressableType resultTypeDescriptor,
			JpaExpression<?>... arguments);

	SqmAvgFunction visitAvgFunction(JpaExpression<?> expression, boolean distinct);
	SqmAvgFunction visitAvgFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType);

	SqmCountFunction visitCountFunction(JpaExpression<?> expression, boolean distinct);
	SqmCountFunction visitCountFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType);

	SqmCountStarFunction visitCountStarFunction(boolean distinct);
	SqmCountStarFunction visitCountStarFunction(boolean distinct, BasicValuedExpressableType resultType);

	SqmMaxFunction visitMaxFunction(JpaExpression<?> expression, boolean distinct);
	SqmMaxFunction visitMaxFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType);

	SqmMinFunction visitMinFunction(JpaExpression<?> expression, boolean distinct);
	SqmMinFunction visitMinFunction(
			JpaExpression<?> expression,
			boolean distinct,
			BasicValuedExpressableType resultType);

	SqmSumFunction visitSumFunction(JpaExpression<?> expression, boolean distinct);
	SqmSumFunction visitSumFunction(
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

	SqmCoalesceFunction visitCoalesce(List<JpaExpression<?>> expressions);

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

	<T,C> SqmCastFunction visitCastFunction(JpaExpression<T> expressionToCast, Class<C> castTarget);

	SqmGenericFunction visitGenericFunction(
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
