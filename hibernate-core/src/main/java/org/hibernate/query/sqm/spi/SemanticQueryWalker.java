/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.spi;

import java.util.List;

import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPath;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.spi.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.spi.domain.NonAggregatedCompositeSimplePath;
import org.hibernate.query.sqm.tree.spi.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.spi.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedBagJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedCrossJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedCteJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedDerivedJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedEntityJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedListJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedMapJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedPluralPartJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedRoot;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedRootJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedSetJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelatedSingularJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.spi.domain.SqmCteRoot;
import org.hibernate.query.sqm.tree.spi.domain.SqmDerivedRoot;
import org.hibernate.query.sqm.tree.spi.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.spi.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.spi.domain.SqmFkExpression;
import org.hibernate.query.sqm.tree.spi.domain.SqmFunctionPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmFunctionRoot;
import org.hibernate.query.sqm.tree.spi.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.spi.domain.SqmElementAggregateFunction;
import org.hibernate.query.sqm.tree.spi.domain.SqmIndexAggregateFunction;
import org.hibernate.query.sqm.tree.spi.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmPluralPartJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.spi.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.spi.expression.AsWrapperSqmExpression;
import org.hibernate.query.sqm.tree.spi.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.spi.expression.SqmAny;
import org.hibernate.query.sqm.tree.spi.expression.SqmAnyDiscriminatorValue;
import org.hibernate.query.sqm.tree.spi.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.spi.expression.SqmByUnit;
import org.hibernate.query.sqm.tree.spi.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.spi.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.spi.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.spi.expression.SqmCoalesce;
import org.hibernate.query.sqm.tree.spi.expression.SqmCollation;
import org.hibernate.query.sqm.tree.spi.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.spi.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.spi.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.spi.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.spi.expression.SqmEvery;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.spi.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.spi.expression.SqmFormat;
import org.hibernate.query.sqm.tree.spi.expression.SqmFunction;
import org.hibernate.query.sqm.tree.spi.expression.SqmHqlNumericLiteral;
import org.hibernate.query.sqm.tree.spi.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.spi.expression.SqmLiteralEmbeddableType;
import org.hibernate.query.sqm.tree.spi.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.spi.expression.SqmModifiedSubQueryExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmNamedExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.spi.expression.SqmOver;
import org.hibernate.query.sqm.tree.spi.expression.SqmOverflow;
import org.hibernate.query.sqm.tree.spi.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.spi.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.spi.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.spi.expression.SqmStar;
import org.hibernate.query.sqm.tree.spi.expression.SqmSummarization;
import org.hibernate.query.sqm.tree.spi.expression.SqmToDuration;
import org.hibernate.query.sqm.tree.spi.expression.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.spi.expression.SqmTuple;
import org.hibernate.query.sqm.tree.spi.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.spi.expression.SqmWindow;
import org.hibernate.query.sqm.tree.spi.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmCteJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmDerivedJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmFromClause;
import org.hibernate.query.sqm.tree.spi.from.SqmFunctionJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.query.sqm.tree.spi.insert.SqmConflictClause;
import org.hibernate.query.sqm.tree.spi.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.spi.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.spi.insert.SqmValues;
import org.hibernate.query.sqm.tree.spi.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmExistsPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmJunctionPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmTruthnessPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.spi.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.spi.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.spi.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.spi.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.spi.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.spi.select.SqmSelection;
import org.hibernate.query.sqm.tree.spi.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.spi.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.spi.update.SqmAssignment;
import org.hibernate.query.sqm.tree.spi.update.SqmSetClause;
import org.hibernate.query.sqm.tree.spi.update.SqmUpdateStatement;

/**
 * Support for walking a Semantic Query Model (SQM) tree
 *
 * @author Steve Ebersole
 */
public interface SemanticQueryWalker<T> {
	T visitUpdateStatement(SqmUpdateStatement<?> statement);

	T visitSetClause(SqmSetClause setClause);

	T visitAssignment(SqmAssignment<?> assignment);

	T visitInsertSelectStatement(SqmInsertSelectStatement<?> statement);

	T visitInsertValuesStatement(SqmInsertValuesStatement<?> statement);

	T visitConflictClause(SqmConflictClause<?> sqmConflictClause);

	T visitDeleteStatement(SqmDeleteStatement<?> statement);

	T visitSelectStatement(SqmSelectStatement<?> statement);

	T visitCteStatement(SqmCteStatement<?> sqmCteStatement);

	T visitCteContainer(SqmCteContainer consumer);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// from-clause / domain paths

	T visitFromClause(SqmFromClause fromClause);

	T visitRootPath(SqmRoot<?> sqmRoot);

	T visitRootDerived(SqmDerivedRoot<?> sqmRoot);

	T visitRootFunction(SqmFunctionRoot<?> sqmRoot);

	T visitRootCte(SqmCteRoot<?> sqmRoot);

	T visitCrossJoin(SqmCrossJoin<?, ?> joinedFromElement);

	T visitPluralPartJoin(SqmPluralPartJoin<?, ?> joinedFromElement);

	T visitQualifiedEntityJoin(SqmEntityJoin<?,?> joinedFromElement);

	T visitQualifiedAttributeJoin(SqmAttributeJoin<?, ?> joinedFromElement);

	default T visitCorrelatedCteJoin(SqmCorrelatedCteJoin<?> join){
		return visitQualifiedCteJoin( join );
	}

	default T visitCorrelatedDerivedJoin(SqmCorrelatedDerivedJoin<?> join){
		return visitQualifiedDerivedJoin( join );
	}

	default T visitCorrelatedCrossJoin(SqmCorrelatedCrossJoin<?, ?> join) {
		return visitCrossJoin( join );
	}

	default T visitCorrelatedEntityJoin(SqmCorrelatedEntityJoin<?,?> join) {
		return visitQualifiedEntityJoin( join );
	}

	default T visitCorrelatedPluralPartJoin(SqmCorrelatedPluralPartJoin<?, ?> join) {
		return visitPluralPartJoin( join );
	}

	default T visitBagJoin(SqmBagJoin<?,?> join){
		return visitQualifiedAttributeJoin( join );
	}

	default T visitCorrelatedBagJoin(SqmCorrelatedBagJoin<?, ?> join) {
		return visitQualifiedAttributeJoin( join );
	}

	default T visitCorrelatedListJoin(SqmCorrelatedListJoin<?, ?> join) {
		return visitQualifiedAttributeJoin( join );
	}

	default T visitCorrelatedMapJoin(SqmCorrelatedMapJoin<?, ?, ?> join) {
		return visitQualifiedAttributeJoin( join );
	}

	default T visitCorrelatedSetJoin(SqmCorrelatedSetJoin<?, ?> join) {
		return visitQualifiedAttributeJoin( join );
	}

	default T visitCorrelatedSingularJoin(SqmCorrelatedSingularJoin<?, ?> join) {
		return visitQualifiedAttributeJoin( join );
	}

	default T visitListJoin(SqmListJoin<?, ?> join) {
		return visitQualifiedAttributeJoin( join );
	}

	default T visitMapJoin(SqmMapJoin<?, ?, ?> join) {
		return visitQualifiedAttributeJoin( join );
	}

	default T visitSetJoin(SqmSetJoin<?, ?> join) {
		return visitQualifiedAttributeJoin( join );
	}

	default T visitSingularJoin(SqmSingularJoin<?, ?> join) {
		return visitQualifiedAttributeJoin( join );
	}

	T visitQualifiedDerivedJoin(SqmDerivedJoin<?> joinedFromElement);

	T visitQualifiedFunctionJoin(SqmFunctionJoin<?> joinedFromElement);

	T visitQualifiedCteJoin(SqmCteJoin<?> joinedFromElement);

	T visitBasicValuedPath(SqmBasicValuedSimplePath<?> path);

	T visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath<?> path);

	T visitAnyValuedValuedPath(SqmAnyValuedSimplePath<?> path);

	T visitNonAggregatedCompositeValuedPath(NonAggregatedCompositeSimplePath<?> path);

	T visitEntityValuedPath(SqmEntityValuedSimplePath<?> path);

	T visitPluralValuedPath(SqmPluralValuedSimplePath<?> path);

	T visitFkExpression(SqmFkExpression<?> fkExpression);

	T visitDiscriminatorPath(DiscriminatorSqmPath<?> sqmPath);

	T visitIndexedPluralAccessPath(SqmIndexedCollectionAccessPath<?> path);

	T visitElementAggregateFunction(SqmElementAggregateFunction<?> path);

	T visitIndexAggregateFunction(SqmIndexAggregateFunction<?> path);

	T visitFunctionPath(SqmFunctionPath<?> functionPath);

	T visitTreatedPath(SqmTreatedPath<?, ?> sqmTreatedPath);

	T visitCorrelation(SqmCorrelation<?, ?> correlation);

	default T visitCorrelatedRootJoin(SqmCorrelatedRootJoin<?> correlatedRootJoin){
		return visitCorrelation( correlatedRootJoin );
	}

	default T visitCorrelatedRoot(SqmCorrelatedRoot<?> correlatedRoot){
		return visitCorrelation( correlatedRoot );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query spec

	T visitQueryGroup(SqmQueryGroup<?> queryGroup);

	T visitQuerySpec(SqmQuerySpec<?> querySpec);

	T visitSelectClause(SqmSelectClause selectClause);

	T visitSelection(SqmSelection<?> selection);

	T visitValues(SqmValues values);

	T visitGroupByClause(List<SqmExpression<?>> groupByClauseExpressions);

	T visitHavingClause(SqmPredicate clause);

	T visitDynamicInstantiation(SqmDynamicInstantiation<?> sqmDynamicInstantiation);

	T visitJpaCompoundSelection(SqmJpaCompoundSelection<?> selection);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// expressions - general

	T visitLiteral(SqmLiteral<?> literal);

	T visitEnumLiteral(SqmEnumLiteral<?> sqmEnumLiteral);

	T visitFieldLiteral(SqmFieldLiteral<?> sqmFieldLiteral);

	<N extends Number> T visitHqlNumericLiteral(SqmHqlNumericLiteral<N> numericLiteral);

	T visitTuple(SqmTuple<?> sqmTuple);

	T visitCollation(SqmCollation sqmCollate);

	T visitBinaryArithmeticExpression(SqmBinaryArithmetic<?> expression);

	T visitSubQueryExpression(SqmSubQuery<?> expression);

	T visitModifiedSubQueryExpression(SqmModifiedSubQueryExpression<?> expression);

	T visitSimpleCaseExpression(SqmCaseSimple<?, ?> expression);

	T visitSearchedCaseExpression(SqmCaseSearched<?> expression);

	T visitAny(SqmAny<?> sqmAny);

	T visitEvery(SqmEvery<?> sqmEvery);

	T visitSummarization(SqmSummarization<?> sqmSummarization);

	T visitPositionalParameterExpression(SqmPositionalParameter<?> expression);

	T visitNamedParameterExpression(SqmNamedParameter<?> expression);

	T visitJpaCriteriaParameter(JpaCriteriaParameter<?> expression);

	T visitEntityTypeLiteralExpression(SqmLiteralEntityType<?> expression);

	T visitEmbeddableTypeLiteralExpression(SqmLiteralEmbeddableType<?> expression);

	T visitAnyDiscriminatorTypeExpression(AnyDiscriminatorSqmPath<?> expression);

	T visitAnyDiscriminatorTypeValueExpression(SqmAnyDiscriminatorValue<?> expression);

	T visitParameterizedEntityTypeExpression(SqmParameterizedEntityType<?> expression);

	T visitUnaryOperationExpression(SqmUnaryOperation<?> expression);

	T visitFunction(SqmFunction<?> tSqmFunction);

	T visitSetReturningFunction(SqmSetReturningFunction<?> tSqmFunction);

	T visitExtractUnit(SqmExtractUnit<?> extractUnit);

	T visitFormat(SqmFormat sqmFormat);

	T visitCastTarget(SqmCastTarget<?> sqmCastTarget);

	T visitTrimSpecification(SqmTrimSpecification trimSpecification);

	T visitDistinct(SqmDistinct<?> distinct);

	T visitStar(SqmStar sqmStar);

	T visitOver(SqmOver<?> over);

	T visitWindow(SqmWindow widow);

	T visitOverflow(SqmOverflow<?> sqmOverflow);

	T visitCoalesce(SqmCoalesce<?> sqmCoalesce);

	T visitToDuration(SqmToDuration<?> toDuration);

	T visitByUnit(SqmByUnit sqmByUnit);

	T visitDurationUnit(SqmDurationUnit<?> durationUnit);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// predicates

	T visitWhereClause(@Nullable SqmWhereClause whereClause);

	T visitGroupedPredicate(SqmGroupedPredicate predicate);

	T visitJunctionPredicate(SqmJunctionPredicate predicate);

	T visitComparisonPredicate(SqmComparisonPredicate predicate);

	T visitIsEmptyPredicate(SqmEmptinessPredicate predicate);

	T visitIsNullPredicate(SqmNullnessPredicate predicate);

	T visitIsTruePredicate(SqmTruthnessPredicate predicate);

	T visitBetweenPredicate(SqmBetweenPredicate predicate);

	T visitLikePredicate(SqmLikePredicate predicate);

	T visitMemberOfPredicate(SqmMemberOfPredicate predicate);

	T visitNegatedPredicate(SqmNegatedPredicate predicate);

	T visitInListPredicate(SqmInListPredicate<?> predicate);

	T visitInSubQueryPredicate(SqmInSubQueryPredicate<?> predicate);

	T visitBooleanExpressionPredicate(SqmBooleanExpressionPredicate predicate);

	T visitExistsPredicate(SqmExistsPredicate sqmExistsPredicate);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sorting

	T visitOrderByClause(SqmOrderByClause orderByClause);

	T visitSortSpecification(SqmSortSpecification sortSpecification);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// paging

	T visitOffsetExpression(SqmExpression<?> expression);
	T visitFetchExpression(SqmExpression<?> expression);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// misc

	T visitPluralAttributeSizeFunction(SqmCollectionSize function);

	T visitMapEntryFunction(SqmMapEntryReference<?, ?> function);

	T visitFullyQualifiedClass(Class<?> namedClass);

	T visitAsWrapperExpression(AsWrapperSqmExpression<?> expression);

	T visitNamedExpression(SqmNamedExpression<?> expression);
}
