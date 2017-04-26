/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.expression.BinaryArithmeticSqmExpression;
import org.hibernate.query.sqm.tree.expression.CaseSearchedSqmExpression;
import org.hibernate.query.sqm.tree.expression.CaseSimpleSqmExpression;
import org.hibernate.query.sqm.tree.expression.CoalesceSqmExpression;
import org.hibernate.query.sqm.tree.expression.CollectionSizeSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConcatSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConstantEnumSqmExpression;
import org.hibernate.query.sqm.tree.expression.ConstantFieldSqmExpression;
import org.hibernate.query.sqm.tree.expression.EntityTypeLiteralSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralBigDecimalSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralBigIntegerSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralCharacterSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralDoubleSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralFalseSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralFloatSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralIntegerSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralLongSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralNullSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralStringSqmExpression;
import org.hibernate.query.sqm.tree.expression.LiteralTrueSqmExpression;
import org.hibernate.query.sqm.tree.expression.NamedParameterSqmExpression;
import org.hibernate.query.sqm.tree.expression.NullifSqmExpression;
import org.hibernate.query.sqm.tree.expression.ParameterizedEntityTypeSqmExpression;
import org.hibernate.query.sqm.tree.expression.PositionalParameterSqmExpression;
import org.hibernate.query.sqm.tree.expression.SubQuerySqmExpression;
import org.hibernate.query.sqm.tree.expression.UnaryOperationSqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.AbstractSpecificSqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityIdentifierReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityTypeSqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmMapEntryBinding;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReferenceBasic;
import org.hibernate.query.sqm.tree.expression.function.AvgFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CastFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.ConcatFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CountFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.CountStarFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.GenericFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.LowerFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.MaxFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.MinFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SubstringFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.SumFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.TrimFunctionSqmExpression;
import org.hibernate.query.sqm.tree.expression.function.UpperFunctionSqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.order.SqmOrderByClause;
import org.hibernate.query.sqm.tree.order.SqmSortSpecification;
import org.hibernate.query.sqm.tree.paging.SqmLimitOffsetClause;
import org.hibernate.query.sqm.tree.predicate.AndSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BetweenSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.BooleanExpressionSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.EmptinessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.GroupedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InListSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.InSubQuerySqmPredicate;
import org.hibernate.query.sqm.tree.predicate.LikeSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.MemberOfSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NegatedSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.NullnessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.OrSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.RelationalSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.set.SqmAssignment;
import org.hibernate.query.sqm.tree.set.SqmSetClause;

/**
 * @author Steve Ebersole
 */
public interface SemanticQueryWalker<T> {
	T visitStatement(SqmStatement statement);

	T visitUpdateStatement(SqmUpdateStatement statement);

	T visitSetClause(SqmSetClause setClause);

	T visitAssignment(SqmAssignment assignment);

	T visitInsertSelectStatement(SqmInsertSelectStatement statement);

	T visitDeleteStatement(SqmDeleteStatement statement);

	T visitSelectStatement(SqmSelectStatement statement);

	T visitQuerySpec(SqmQuerySpec querySpec);

	T visitFromClause(SqmFromClause fromClause);

	T visitFromElementSpace(SqmFromElementSpace fromElementSpace);

	T visitRootEntityFromElement(SqmRoot rootEntityFromElement);

	T visitCrossJoinedFromElement(SqmCrossJoin joinedFromElement);

	T visitQualifiedEntityJoinFromElement(SqmEntityJoin joinedFromElement);

	T visitQualifiedAttributeJoinFromElement(SqmAttributeJoin joinedFromElement);

	T visitSelectClause(SqmSelectClause selectClause);

	T visitSelection(SqmSelection selection);

	T visitDynamicInstantiation(SqmDynamicInstantiation dynamicInstantiation);

	T visitWhereClause(SqmWhereClause whereClause);

	T visitGroupedPredicate(GroupedSqmPredicate predicate);

	T visitAndPredicate(AndSqmPredicate predicate);

	T visitOrPredicate(OrSqmPredicate predicate);

	T visitRelationalPredicate(RelationalSqmPredicate predicate);

	T visitIsEmptyPredicate(EmptinessSqmPredicate predicate);

	T visitIsNullPredicate(NullnessSqmPredicate predicate);

	T visitBetweenPredicate(BetweenSqmPredicate predicate);

	T visitLikePredicate(LikeSqmPredicate predicate);

	T visitMemberOfPredicate(MemberOfSqmPredicate predicate);

	T visitNegatedPredicate(NegatedSqmPredicate predicate);

	T visitInListPredicate(InListSqmPredicate predicate);

	T visitInSubQueryPredicate(InSubQuerySqmPredicate predicate);

	T visitBooleanExpressionPredicate(BooleanExpressionSqmPredicate predicate);

	T visitOrderByClause(SqmOrderByClause orderByClause);

	T visitSortSpecification(SqmSortSpecification sortSpecification);

	T visitLimitOffsetClause(SqmLimitOffsetClause limitOffsetClause);

	T visitPositionalParameterExpression(PositionalParameterSqmExpression expression);

	T visitNamedParameterExpression(NamedParameterSqmExpression expression);

	T visitEntityTypeLiteralExpression(EntityTypeLiteralSqmExpression expression);

	T visitEntityTypeExpression(SqmEntityTypeSqmExpression expression);

	T visitParameterizedEntityTypeExpression(ParameterizedEntityTypeSqmExpression expression);

	T visitUnaryOperationExpression(UnaryOperationSqmExpression expression);

	T visitAttributeReferenceExpression(SqmAttributeReference expression);

	T visitGenericFunction(GenericFunctionSqmExpression expression);

	T visitCastFunction(CastFunctionSqmExpression expression);

	T visitAvgFunction(AvgFunctionSqmExpression expression);

	T visitCountStarFunction(CountStarFunctionSqmExpression expression);

	T visitCountFunction(CountFunctionSqmExpression expression);

	T visitMaxFunction(MaxFunctionSqmExpression expression);

	T visitMinFunction(MinFunctionSqmExpression expression);

	T visitSumFunction(SumFunctionSqmExpression expression);

	T visitPluralAttributeSizeFunction(CollectionSizeSqmExpression function);

	T visitPluralAttributeElementBinding(SqmCollectionElementReference binding);

	T visitPluralAttributeIndexFunction(SqmCollectionIndexReference function);

	T visitMapKeyBinding(SqmCollectionIndexReference binding);

	T visitMapEntryFunction(SqmMapEntryBinding function);

	T visitMaxElementBinding(SqmMaxElementReference binding);

	T visitMinElementBinding(SqmMinElementReference binding);

	T visitMaxIndexFunction(AbstractSpecificSqmCollectionIndexReference function);

	T visitMinIndexFunction(SqmMinIndexReferenceBasic function);

	T visitLiteralStringExpression(LiteralStringSqmExpression expression);

	T visitLiteralCharacterExpression(LiteralCharacterSqmExpression expression);

	T visitLiteralDoubleExpression(LiteralDoubleSqmExpression expression);

	T visitLiteralIntegerExpression(LiteralIntegerSqmExpression expression);

	T visitLiteralBigIntegerExpression(LiteralBigIntegerSqmExpression expression);

	T visitLiteralBigDecimalExpression(LiteralBigDecimalSqmExpression expression);

	T visitLiteralFloatExpression(LiteralFloatSqmExpression expression);

	T visitLiteralLongExpression(LiteralLongSqmExpression expression);

	T visitLiteralTrueExpression(LiteralTrueSqmExpression expression);

	T visitLiteralFalseExpression(LiteralFalseSqmExpression expression);

	T visitLiteralNullExpression(LiteralNullSqmExpression expression);

	T visitConcatExpression(ConcatSqmExpression expression);

	T visitConcatFunction(ConcatFunctionSqmExpression expression);

	T visitConstantEnumExpression(ConstantEnumSqmExpression expression);

	T visitConstantFieldExpression(ConstantFieldSqmExpression expression);

	T visitBinaryArithmeticExpression(BinaryArithmeticSqmExpression expression);

	T visitSubQueryExpression(SubQuerySqmExpression expression);

	T visitSimpleCaseExpression(CaseSimpleSqmExpression expression);

	T visitSearchedCaseExpression(CaseSearchedSqmExpression expression);

	T visitCoalesceExpression(CoalesceSqmExpression expression);

	T visitNullifExpression(NullifSqmExpression expression);

	T visitSubstringFunction(SubstringFunctionSqmExpression expression);

	T visitTrimFunction(TrimFunctionSqmExpression expression);

	T visitUpperFunction(UpperFunctionSqmExpression expression);

	T visitLowerFunction(LowerFunctionSqmExpression expression);

	T visitEntityIdentifierBinding(SqmEntityIdentifierReference sqmEntityIdentifierBinding);
}
