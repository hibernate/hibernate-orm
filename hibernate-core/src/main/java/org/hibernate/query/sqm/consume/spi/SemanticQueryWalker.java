/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import java.lang.reflect.Field;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmMaxElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMaxIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmMinElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMinIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmConcat;
import org.hibernate.query.sqm.tree.expression.SqmCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmRestrictedSubQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.function.SqmAbsFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmAvgFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmBitLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCastFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.function.SqmCeilingFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCoalesceFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmConcatFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCountStarFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentDateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentInstantFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimeFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmCurrentTimestampFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmExpFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.function.SqmFloorFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmGenericFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLengthFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLnFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLocateFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmLowerFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMaxFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmMinFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmModFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmNullifFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmPowerFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmReplaceFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSqrtFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmStrFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSubstringFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmSumFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmUpperFunction;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmOrPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmGroupByClause;
import org.hibernate.query.sqm.tree.select.SqmHavingClause;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;

/**
 * @author Steve Ebersole
 */
public interface SemanticQueryWalker<T> {
	T visitUpdateStatement(SqmUpdateStatement<?> statement);

	T visitSetClause(SqmSetClause setClause);

	T visitAssignment(SqmAssignment assignment);

	T visitInsertSelectStatement(SqmInsertSelectStatement<?> statement);

	T visitDeleteStatement(SqmDeleteStatement<?> statement);

	T visitSelectStatement(SqmSelectStatement<?> statement);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// from-clause / domain paths

	T visitFromClause(SqmFromClause fromClause);

	T visitRootPath(SqmRoot<?> sqmRoot);

	T visitCrossJoin(SqmCrossJoin<?> joinedFromElement);

	T visitQualifiedEntityJoin(SqmEntityJoin<?> joinedFromElement);

	T visitQualifiedAttributeJoin(SqmAttributeJoin<?,?> joinedFromElement);

	T visitBasicValuedPath(SqmBasicValuedSimplePath<?> path);

	T visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath<?> path);

	T visitEntityValuedPath(SqmEntityValuedSimplePath<?> path);

	T visitPluralValuedPath(SqmPluralValuedSimplePath<?> path);

	T visitIndexedPluralAccessPath(SqmIndexedCollectionAccessPath path);

	T visitMaxElementPath(SqmMaxElementPath path);

	T visitMinElementPath(SqmMinElementPath path);

	T visitMaxIndexPath(SqmMaxIndexPath path);

	T visitMinIndexPath(SqmMinIndexPath path);

	T visitTreatedPath(SqmTreatedPath<?,?> sqmTreatedPath);

	T visitCorrelation(SqmCorrelation correlation);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query spec

	T visitQuerySpec(SqmQuerySpec<?> querySpec);

	T visitSelectClause(SqmSelectClause selectClause);

	T visitSelection(SqmSelection selection);

	T visitGroupByClause(SqmGroupByClause clause);

	T visitGrouping(SqmGroupByClause.SqmGrouping grouping);

	T visitHavingClause(SqmHavingClause clause);

	T visitDynamicInstantiation(SqmDynamicInstantiation<?> sqmDynamicInstantiation);

	default T visitJpaCompoundSelection(SqmJpaCompoundSelection selection) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - general

	T visitLiteral(SqmLiteral<?> literal);

	T visitTuple(SqmTuple<?> sqmTuple);

	T visitConcatExpression(SqmConcat<?> expression);

	T visitConcatFunction(SqmConcatFunction<?> expression);

	T visitBinaryArithmeticExpression(SqmBinaryArithmetic<?> expression);

	T visitSubQueryExpression(SqmSubQuery<?> expression);

	T visitRestrictedSubQueryExpression(SqmRestrictedSubQueryExpression<?> sqmRestrictedSubQueryExpression);

	T visitSimpleCaseExpression(SqmCaseSimple<?,?> expression);

	T visitSearchedCaseExpression(SqmCaseSearched<?> expression);

	T visitPositionalParameterExpression(SqmPositionalParameter<?> expression);

	T visitNamedParameterExpression(SqmNamedParameter<?> expression);

	T visitCriteriaParameter(SqmCriteriaParameter<?> expression);

	T visitEntityTypeLiteralExpression(SqmLiteralEntityType<?> expression);

	T visitParameterizedEntityTypeExpression(SqmParameterizedEntityType<?> expression);

	T visitUnaryOperationExpression(SqmUnaryOperation<?> expression);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - non-standard functions

	T visitGenericFunction(SqmGenericFunction<?> expression);

	T visitSqlAstFunctionProducer(SqlAstFunctionProducer<?> sqlAstFunctionProducer);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - standard functions

	T visitAbsFunction(SqmAbsFunction<?> function);

	T visitAvgFunction(SqmAvgFunction<?> function);

	T visitBitLengthFunction(SqmBitLengthFunction<?> function);

	T visitCastFunction(SqmCastFunction<?> function);

	T visitCoalesceFunction(SqmCoalesceFunction<?> function);

	T visitCountFunction(SqmCountFunction<?> function);

	T visitCountStarFunction(SqmCountStarFunction<?> function);

	T visitCurrentDateFunction(SqmCurrentDateFunction function);

	T visitCurrentTimeFunction(SqmCurrentTimeFunction function);

	T visitCurrentTimestampFunction(SqmCurrentTimestampFunction function);

	T visitCurrentInstantFunction(SqmCurrentInstantFunction function);

	T visitExtractFunction(SqmExtractFunction<?> function);

	T visitLengthFunction(SqmLengthFunction<?> function);

	T visitLocateFunction(SqmLocateFunction<?> function);

	T visitReplaceFunction(SqmReplaceFunction<?> function);

	T visitLowerFunction(SqmLowerFunction<?> function);

	T visitMaxFunction(SqmMaxFunction<?> function);

	T visitMinFunction(SqmMinFunction<?> function);

	T visitModFunction(SqmModFunction<?> function);

	T visitNullifFunction(SqmNullifFunction<?> function);

	T visitSqrtFunction(SqmSqrtFunction<?> function);

	T visitStrFunction(SqmStrFunction<?> function);

	T visitSubstringFunction(SqmSubstringFunction<?> function);

	T visitSumFunction(SqmSumFunction<?> function);

	T visitTrimFunction(SqmTrimFunction<?> function);

	T visitUpperFunction(SqmUpperFunction<?> function);

	T visitExpFunction(SqmExpFunction<?> function);

	T visitLnFunction(SqmLnFunction<?> function);

	T visitPowerFunction(SqmPowerFunction<?> function);

	T visitCeilingFunction(SqmCeilingFunction<?> function);

	T visitFloorFunction(SqmFloorFunction<?> function);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// predicates

	T visitWhereClause(SqmWhereClause whereClause);

	T visitGroupedPredicate(SqmGroupedPredicate predicate);

	T visitAndPredicate(SqmAndPredicate predicate);

	T visitOrPredicate(SqmOrPredicate predicate);

	T visitComparisonPredicate(SqmComparisonPredicate predicate);

	T visitIsEmptyPredicate(SqmEmptinessPredicate predicate);

	T visitIsNullPredicate(SqmNullnessPredicate predicate);

	T visitBetweenPredicate(SqmBetweenPredicate predicate);

	T visitLikePredicate(SqmLikePredicate predicate);

	T visitMemberOfPredicate(SqmMemberOfPredicate predicate);

	T visitNegatedPredicate(SqmNegatedPredicate predicate);

	T visitInListPredicate(SqmInListPredicate<?> predicate);

	T visitInSubQueryPredicate(SqmInSubQueryPredicate<?> predicate);

	T visitBooleanExpressionPredicate(SqmBooleanExpressionPredicate predicate);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sorting

	T visitOrderByClause(SqmOrderByClause orderByClause);

	T visitSortSpecification(SqmSortSpecification sortSpecification);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// paging

	T visitOffsetExpression(SqmExpression<?> expression);
	T visitLimitExpression(SqmExpression<?> expression);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// misc

	T visitPluralAttributeSizeFunction(SqmCollectionSize function);

	T visitMapEntryFunction(SqmMapEntryReference function);

	T visitFullyQualifiedClass(Class<?> namedClass);

	T visitFullyQualifiedEnum(Enum<?> value);

	T visitFullyQualifiedField(Field field);

	T visitExtractUnit(SqmExtractUnit extractUnit);

	T visitCastTarget(SqmCastTarget sqmCastTarget);

}
