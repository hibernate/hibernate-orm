/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmConstantEnum;
import org.hibernate.query.sqm.tree.expression.SqmConstantFieldReference;
import org.hibernate.query.sqm.tree.expression.SqmLiteralBigDecimal;
import org.hibernate.query.sqm.tree.expression.SqmLiteralBigInteger;
import org.hibernate.query.sqm.tree.expression.SqmLiteralCharacter;
import org.hibernate.query.sqm.tree.expression.SqmLiteralDate;
import org.hibernate.query.sqm.tree.expression.SqmLiteralDouble;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmLiteralFalse;
import org.hibernate.query.sqm.tree.expression.SqmLiteralFloat;
import org.hibernate.query.sqm.tree.expression.SqmLiteralInteger;
import org.hibernate.query.sqm.tree.expression.SqmLiteralLong;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmLiteralString;
import org.hibernate.query.sqm.tree.expression.SqmLiteralTime;
import org.hibernate.query.sqm.tree.expression.SqmLiteralTimestamp;
import org.hibernate.query.sqm.tree.expression.SqmLiteralTrue;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSubQuery;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.domain.AbstractSpecificSqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityIdentifierReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityTypeExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmMapEntryBinding;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceAny;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEntity;
import org.hibernate.query.sqm.tree.expression.function.SqmAbsFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmBitLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCoalesceFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmConcatFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountStarFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentDateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimeFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimestampFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLocateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLowerFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmModFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmNullifFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSqrtFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSubstringFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmUpperFunction;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
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
import org.hibernate.sql.ast.produce.ordering.internal.SqmColumnReference;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;

/**
 * @author Steve Ebersole
 */
public interface SemanticQueryWalker<T> {
	SessionFactoryImplementor getSessionFactory();

	T visitUpdateStatement(SqmUpdateStatement statement);

	T visitSetClause(SqmSetClause setClause);

	T visitAssignment(SqmAssignment assignment);

	T visitInsertSelectStatement(SqmInsertSelectStatement statement);

	T visitDeleteStatement(SqmDeleteStatement statement);

	T visitSelectStatement(SqmSelectStatement statement);

	T visitQuerySpec(SqmQuerySpec querySpec);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// from-clause

	T visitFromClause(SqmFromClause fromClause);

	T visitFromElementSpace(SqmFromElementSpace fromElementSpace);

	T visitRootEntityFromElement(SqmRoot rootEntityFromElement);

	T visitRootEntityReference(SqmEntityReference sqmEntityReference);

	T visitCrossJoinedFromElement(SqmCrossJoin joinedFromElement);

	T visitQualifiedEntityJoinFromElement(SqmEntityJoin joinedFromElement);

	T visitQualifiedAttributeJoinFromElement(SqmNavigableJoin joinedFromElement);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// selections

	T visitSelectClause(SqmSelectClause selectClause);

	T visitSelection(SqmSelection selection);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - navigable

	T visitEntityIdentifierReference(SqmEntityIdentifierReference sqmEntityIdentifierBinding);

	T visitBasicValuedSingularAttribute(SqmSingularAttributeReferenceBasic sqmAttributeReference);

	T visitEntityValuedSingularAttribute(SqmSingularAttributeReferenceEntity sqmAttributeReference);

	T visitEmbeddableValuedSingularAttribute(SqmSingularAttributeReferenceEmbedded sqmAttributeReference);

	T visitAnyValuedSingularAttribute(SqmSingularAttributeReferenceAny sqmAttributeReference);

	T visitPluralAttribute(SqmPluralAttributeReference reference);

	// todo (6.0) : split this based on the element type like we did for singular attributes
	//		aka:
	//			#visit

	T visitPluralAttributeElementBinding(SqmCollectionElementReference binding);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - general

	T visitPositionalParameterExpression(SqmPositionalParameter expression);

	T visitNamedParameterExpression(SqmNamedParameter expression);

	T visitEntityTypeLiteralExpression(SqmLiteralEntityType expression);

	T visitEntityTypeExpression(SqmEntityTypeExpression expression);

	T visitParameterizedEntityTypeExpression(SqmParameterizedEntityType expression);

	T visitUnaryOperationExpression(SqmUnaryOperation expression);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - non-standard functions

	T visitGenericFunction(SqmGenericFunction expression);

	T visitSqlAstFunctionProducer(SqlAstFunctionProducer sqlAstFunctionProducer);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - standard functions

	T visitAbsFunction(SqmAbsFunction function);

	T visitAvgFunction(SqmAvgFunction expression);

	T visitBitLengthFunction(SqmBitLengthFunction sqmBitLengthFunction);

	T visitCastFunction(SqmCastFunction expression);

	T visitCoalesceFunction(SqmCoalesceFunction expression);

	T visitCountFunction(SqmCountFunction expression);

	T visitCountStarFunction(SqmCountStarFunction expression);

	T visitCurrentDateFunction(SqmCurrentDateFunction sqmCurrentDate);

	T visitCurrentTimeFunction(SqmCurrentTimeFunction sqmCurrentTimeFunction);

	T visitCurrentTimestampFunction(SqmCurrentTimestampFunction sqmCurrentTimestampFunction);

	T visitExtractFunction(SqmExtractFunction function);

	T visitLengthFunction(SqmLengthFunction sqmLengthFunction);

	T visitLocateFunction(SqmLocateFunction function);

	T visitLowerFunction(SqmLowerFunction expression);

	T visitMaxFunction(SqmMaxFunction expression);

	T visitMinFunction(SqmMinFunction expression);

	T visitModFunction(SqmModFunction sqmModFunction);

	T visitNullifFunction(SqmNullifFunction expression);

	T visitSqrtFunction(SqmSqrtFunction sqmSqrtFunction);

	T visitSubstringFunction(SqmSubstringFunction expression);

	T visitSumFunction(SqmSumFunction expression);

	T visitTrimFunction(SqmTrimFunction expression);

	T visitUpperFunction(SqmUpperFunction expression);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// predicates

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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sorting

	T visitOrderByClause(SqmOrderByClause orderByClause);

	T visitSortSpecification(SqmSortSpecification sortSpecification);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// paging

	T visitLimitOffsetClause(SqmLimitOffsetClause limitOffsetClause);




	T visitPluralAttributeSizeFunction(SqmCollectionSize function);


	T visitPluralAttributeIndexFunction(SqmCollectionIndexReference function);

	T visitMapKeyBinding(SqmCollectionIndexReference binding);

	T visitMapEntryFunction(SqmMapEntryBinding function);

	T visitMaxElementBinding(SqmMaxElementReference binding);

	T visitMinElementBinding(SqmMinElementReference binding);

	T visitMaxIndexFunction(AbstractSpecificSqmCollectionIndexReference function);

	T visitMinIndexFunction(SqmMinIndexReferenceBasic function);

	T visitLiteralStringExpression(SqmLiteralString expression);

	T visitLiteralCharacterExpression(SqmLiteralCharacter expression);

	T visitLiteralDoubleExpression(SqmLiteralDouble expression);

	T visitLiteralIntegerExpression(SqmLiteralInteger expression);

	T visitLiteralBigIntegerExpression(SqmLiteralBigInteger expression);

	T visitLiteralBigDecimalExpression(SqmLiteralBigDecimal expression);

	T visitLiteralFloatExpression(SqmLiteralFloat expression);

	T visitLiteralLongExpression(SqmLiteralLong expression);

	T visitLiteralTrueExpression(SqmLiteralTrue expression);

	T visitLiteralFalseExpression(SqmLiteralFalse expression);

	T visitLiteralNullExpression(SqmLiteralNull expression);

	T visitLiteralTimestampExpression(SqmLiteralTimestamp literal);

	T visitLiteralDateExpression(SqmLiteralDate literal);

	T visitLiteralTimeExpression(SqmLiteralTime literal);

	T visitConcatExpression(SqmConcat expression);

	T visitConcatFunction(SqmConcatFunction expression);

	T visitConstantEnumExpression(SqmConstantEnum expression);

	T visitConstantFieldReference(SqmConstantFieldReference expression);

	T visitBinaryArithmeticExpression(SqmBinaryArithmetic expression);

	T visitSubQueryExpression(SqmSubQuery expression);

	T visitSimpleCaseExpression(SqmCaseSimple expression);

	T visitSearchedCaseExpression(SqmCaseSearched expression);

	T visitExplicitColumnReference(SqmColumnReference sqmColumnReference);



	T visitDynamicInstantiation(SqmDynamicInstantiation sqmDynamicInstantiation);
}
