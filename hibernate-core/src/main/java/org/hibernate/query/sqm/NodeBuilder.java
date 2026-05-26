/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.query.spi.ImmutableEntityUpdateQueryHandlingMode;
import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCastTarget;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.query.criteria.JpaWindow;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmJsonExistsExpression;
import org.hibernate.query.sqm.tree.expression.SqmJsonQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmJsonTableFunction;
import org.hibernate.query.sqm.tree.expression.SqmJsonValueExpression;
import org.hibernate.query.sqm.tree.expression.SqmModifiedSubQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.expression.SqmXmlElementExpression;
import org.hibernate.query.sqm.tree.expression.SqmXmlTableFunction;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.query.sqm.tree.predicate.SqmInPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.type.BasicType;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;

/**
 * Adapts the JPA CriteriaBuilder to generate SQM nodes.
 *
 * @author Steve Ebersole
 * @author Yoobin Yoon
 */
public interface NodeBuilder extends HibernateCriteriaBuilder, SqmCreationContext {
	default JpaMetamodel getDomainModel() {
		return getJpaMetamodel();
	}

	default boolean isJpaQueryComplianceEnabled() {
		return getJpaCompliance().isJpaQueryComplianceEnabled();
	}

	@Override
	default NodeBuilder getNodeBuilder() {
		return this;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Array functions for array types

	@Override
	<T> SqmExpression<T[]> arrayAgg(JpaOrder order, Expression<? extends T> argument);

	@Override
	<T> SqmExpression<T[]> arrayAgg(JpaOrder order, JpaPredicate filter, Expression<? extends T> argument);

	@Override
	<T> SqmExpression<T[]> arrayAgg(JpaOrder order, JpaWindow window, Expression<? extends T> argument);

	@Override
	<T> SqmExpression<T[]> arrayAgg(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<? extends T> argument);

	@Override
	<T> SqmExpression<T[]> arrayLiteral(T... elements);

	@Override
	<T> SqmExpression<Integer> arrayLength(Expression<T[]> arrayExpression);

	@Override
	<T> SqmExpression<Integer> arrayPosition(Expression<T[]> arrayExpression, T element);

	@Override
	<T> SqmExpression<Integer> arrayPosition(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	@Override
	<T> SqmExpression<int[]> arrayPositions(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	@Override
	<T> SqmExpression<int[]> arrayPositions(Expression<T[]> arrayExpression, T element);

	@Override
	<T> SqmExpression<List<Integer>> arrayPositionsList(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	@Override
	<T> SqmExpression<List<Integer>> arrayPositionsList(Expression<T[]> arrayExpression, T element);

	@Override
	<T> SqmExpression<T[]> arrayConcat(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2);

	@Override
	<T> SqmExpression<T[]> arrayConcat(Expression<T[]> arrayExpression1, T[] array2);

	@Override
	<T> SqmExpression<T[]> arrayConcat(T[] array1, Expression<T[]> arrayExpression2);

	@Override
	<T> SqmExpression<T[]> arrayAppend(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	@Override
	<T> SqmExpression<T[]> arrayAppend(Expression<T[]> arrayExpression, T element);

	@Override
	<T> SqmExpression<T[]> arrayPrepend(Expression<T> elementExpression, Expression<T[]> arrayExpression);

	@Override
	<T> SqmExpression<T[]> arrayPrepend(T element, Expression<T[]> arrayExpression);

	@Override
	<T> SqmExpression<T> arrayGet(Expression<T[]> arrayExpression, Expression<Integer> indexExpression);

	@Override
	<T> SqmExpression<T> arrayGet(Expression<T[]> arrayExpression, Integer index);

	@Override
	<T> SqmExpression<T[]> arraySet(Expression<T[]> arrayExpression, Expression<Integer> indexExpression, Expression<T> elementExpression);

	@Override
	<T> SqmExpression<T[]> arraySet(Expression<T[]> arrayExpression, Expression<Integer> indexExpression, T element);

	@Override
	<T> SqmExpression<T[]> arraySet(Expression<T[]> arrayExpression, Integer index, Expression<T> elementExpression);

	@Override
	<T> SqmExpression<T[]> arraySet(Expression<T[]> arrayExpression, Integer index, T element);

	@Override
	<T> SqmExpression<T[]> arrayRemove(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	@Override
	<T> SqmExpression<T[]> arrayRemove(Expression<T[]> arrayExpression, T element);

	@Override
	<T> SqmExpression<T[]> arrayRemoveIndex(Expression<T[]> arrayExpression, Expression<Integer> indexExpression);

	@Override
	<T> SqmExpression<T[]> arrayRemoveIndex(Expression<T[]> arrayExpression, Integer index);

	@Override
	<T> SqmExpression<T[]> arraySlice(Expression<T[]> arrayExpression, Expression<Integer> lowerIndexExpression, Expression<Integer> upperIndexExpression);

	@Override
	<T> SqmExpression<T[]> arraySlice(Expression<T[]> arrayExpression, Expression<Integer> lowerIndexExpression, Integer upperIndex);

	@Override
	<T> SqmExpression<T[]> arraySlice(Expression<T[]> arrayExpression, Integer lowerIndex, Expression<Integer> upperIndexExpression);

	@Override
	<T> SqmExpression<T[]> arraySlice(Expression<T[]> arrayExpression, Integer lowerIndex, Integer upperIndex);

	@Override
	<T> SqmExpression<T[]> arrayReplace(Expression<T[]> arrayExpression, Expression<T> oldElementExpression, Expression<T> newElementExpression);

	@Override
	<T> SqmExpression<T[]> arrayReplace(Expression<T[]> arrayExpression, Expression<T> oldElementExpression, T newElement);

	@Override
	<T> SqmExpression<T[]> arrayReplace(Expression<T[]> arrayExpression, T oldElement, Expression<T> newElementExpression);

	@Override
	<T> SqmExpression<T[]> arrayReplace(Expression<T[]> arrayExpression, T oldElement, T newElement);

	@Override
	<T> SqmExpression<T[]> arrayTrim(Expression<T[]> arrayExpression, Expression<Integer> elementCountExpression);

	@Override
	<T> SqmExpression<T[]> arrayTrim(Expression<T[]> arrayExpression, Integer elementCount);

	@Override
	<T> SqmExpression<T[]> arrayReverse(Expression<T[]> arrayExpression);

	@Override
	<T> SqmExpression<T[]> arraySort(Expression<T[]> arrayExpression);

	@Override
	<T> SqmExpression<T[]> arraySort(Expression<T[]> arrayExpression, boolean descending);

	@Override
	<T> SqmExpression<T[]> arraySort(Expression<T[]> arrayExpression, Expression<Boolean> descendingExpression);

	@Override
	<T> SqmExpression<T[]> arraySort(Expression<T[]> arrayExpression, boolean descending, boolean nullsFirst);

	@Override
	<T> SqmExpression<T[]> arraySort(Expression<T[]> arrayExpression, Expression<Boolean> descendingExpression, Expression<Boolean> nullsFirstExpression);

	@Override
	<T> SqmExpression<T[]> arrayFill(Expression<T> elementExpression, Expression<Integer> elementCountExpression);

	@Override
	<T> SqmExpression<T[]> arrayFill(Expression<T> elementExpression, Integer elementCount);

	@Override
	<T> SqmExpression<T[]> arrayFill(T element, Expression<Integer> elementCountExpression);

	@Override
	<T> SqmExpression<T[]> arrayFill(T element, Integer elementCount);

	@Override
	SqmExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, Expression<String> separatorExpression);

	@Override
	SqmExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, String separator);

	@Override
	SqmExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, Expression<String> separatorExpression, Expression<String> defaultExpression);

	@Override
	SqmExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, Expression<String> separatorExpression, String defaultValue);

	@Override
	SqmExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, String separator, Expression<String> defaultExpression);

	@Override
	SqmExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, String separator, String defaultValue);

	@Override
	<T> SqmPredicate arrayContains(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	@Override
	<T> SqmPredicate arrayContains(Expression<T[]> arrayExpression, T element);

	@Override
	<T> SqmPredicate arrayContains(T[] array, Expression<T> elementExpression);

	@Override
	<T> SqmPredicate arrayContainsNullable(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	@Override
	<T> SqmPredicate arrayContainsNullable(Expression<T[]> arrayExpression, T element);

	@Override
	<T> SqmPredicate arrayContainsNullable(T[] array, Expression<T> elementExpression);

	@Override @Deprecated
	default <T> SqmPredicate arrayContainsAll(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression) {
		return arrayIncludes( arrayExpression, subArrayExpression );
	}

	@Override @Deprecated
	default <T> SqmPredicate arrayContainsAll(Expression<T[]> arrayExpression, T[] subArray) {
		return arrayIncludes( arrayExpression, subArray );
	}

	@Override @Deprecated
	default <T> SqmPredicate arrayContainsAll(T[] array, Expression<T[]> subArrayExpression) {
		return arrayIncludes( array, subArrayExpression );
	}

	@Override @Deprecated
	default <T> SqmPredicate arrayContainsAllNullable(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression) {
		return arrayIncludesNullable( arrayExpression, subArrayExpression );
	}

	@Override @Deprecated
	default <T> SqmPredicate arrayContainsAllNullable(Expression<T[]> arrayExpression, T[] subArray) {
		return arrayIncludesNullable( arrayExpression, subArray );
	}

	@Override @Deprecated
	default <T> SqmPredicate arrayContainsAllNullable(T[] array, Expression<T[]> subArrayExpression) {
		return arrayIncludesNullable( array, subArrayExpression );
	}

	@Override
	<T> SqmPredicate arrayIncludes(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression);

	@Override
	<T> SqmPredicate arrayIncludes(Expression<T[]> arrayExpression, T[] subArray);

	@Override
	<T> SqmPredicate arrayIncludes(T[] array, Expression<T[]> subArrayExpression);

	@Override
	<T> SqmPredicate arrayIncludesNullable(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression);

	@Override
	<T> SqmPredicate arrayIncludesNullable(Expression<T[]> arrayExpression, T[] subArray);

	@Override
	<T> SqmPredicate arrayIncludesNullable(T[] array, Expression<T[]> subArrayExpression);

	@Override @Deprecated
	default <T> SqmPredicate arrayOverlaps(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return arrayIntersects( arrayExpression1, arrayExpression2 );
	}

	@Override @Deprecated
	default <T> SqmPredicate arrayOverlaps(Expression<T[]> arrayExpression1, T[] array2) {
		return arrayIntersects( arrayExpression1, array2 );
	}

	@Override @Deprecated
	default <T> SqmPredicate arrayOverlaps(T[] array1, Expression<T[]> arrayExpression2) {
		return arrayIntersects( array1, arrayExpression2 );
	}

	@Override @Deprecated
	default <T> SqmPredicate arrayOverlapsNullable(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return arrayIntersectsNullable( arrayExpression1, arrayExpression2 );
	}

	@Override @Deprecated
	default <T> SqmPredicate arrayOverlapsNullable(Expression<T[]> arrayExpression1, T[] array2) {
		return arrayIntersectsNullable( arrayExpression1, array2 );
	}

	@Override @Deprecated
	default <T> SqmPredicate arrayOverlapsNullable(T[] array1, Expression<T[]> arrayExpression2) {
		return arrayIntersectsNullable( array1, arrayExpression2 );
	}

	@Override
	<T> SqmPredicate arrayIntersects(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2);

	@Override
	<T> SqmPredicate arrayIntersects(Expression<T[]> arrayExpression1, T[] array2);

	@Override
	<T> SqmPredicate arrayIntersects(T[] array1, Expression<T[]> arrayExpression2);

	@Override
	<T> SqmPredicate arrayIntersectsNullable(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2);

	@Override
	<T> SqmPredicate arrayIntersectsNullable(Expression<T[]> arrayExpression1, T[] array2);

	@Override
	<T> SqmPredicate arrayIntersectsNullable(T[] array1, Expression<T[]> arrayExpression2);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Array functions for collection types

	@Override
	<E, C extends Collection<E>> SqmExpression<C> collectionLiteral(E... elements);

	@Override
	SqmExpression<Integer> collectionLength(Expression<? extends Collection<?>> collectionExpression);

	@Override
	<E> SqmExpression<Integer> collectionPosition(Expression<? extends Collection<? extends E>> collectionExpression, E element);

	@Override
	<E> SqmExpression<Integer> collectionPosition(Expression<? extends Collection<? extends E>> collectionExpression, Expression<E> elementExpression);

	@Override
	<T> SqmExpression<int[]> collectionPositions(Expression<? extends Collection<? super T>> collectionExpression, Expression<T> elementExpression);

	@Override
	<T> SqmExpression<int[]> collectionPositions(Expression<? extends Collection<? super T>> collectionExpression, T element);

	@Override
	<T> SqmExpression<List<Integer>> collectionPositionsList(Expression<? extends Collection<? super T>> collectionExpression, Expression<T> elementExpression);

	@Override
	<T> SqmExpression<List<Integer>> collectionPositionsList(Expression<? extends Collection<? super T>> collectionExpression, T element);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(Expression<C> collectionExpression1, Expression<? extends Collection<? extends E>> collectionExpression2);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(Expression<C> collectionExpression1, Collection<? extends E> collection2);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(C collection1, Expression<? extends Collection<? extends E>> collectionExpression2);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(Expression<C> collectionExpression, Expression<? extends E> elementExpression);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(Expression<C> collectionExpression, E element);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(Expression<? extends E> elementExpression, Expression<C> collectionExpression);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(E element, Expression<C> collectionExpression);

	@Override
	<E> SqmExpression<E> collectionGet(Expression<? extends Collection<E>> collectionExpression, Expression<Integer> indexExpression);

	@Override
	<E> SqmExpression<E> collectionGet(Expression<? extends Collection<E>> collectionExpression, Integer index);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(Expression<C> collectionExpression, Expression<Integer> indexExpression, Expression<? extends E> elementExpression);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(Expression<C> collectionExpression, Expression<Integer> indexExpression, E element);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(Expression<C> collectionExpression, Integer index, Expression<? extends E> elementExpression);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(Expression<C> collectionExpression, Integer index, E element);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(Expression<C> collectionExpression, Expression<? extends E> elementExpression);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(Expression<C> collectionExpression, E element);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(Expression<C> collectionExpression, Expression<Integer> indexExpression);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(Expression<C> collectionExpression, Integer index);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSlice(Expression<C> collectionExpression, Expression<Integer> lowerIndexExpression, Expression<Integer> upperIndexExpression);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSlice(Expression<C> collectionExpression, Expression<Integer> lowerIndexExpression, Integer upperIndex);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSlice(Expression<C> collectionExpression, Integer lowerIndex, Expression<Integer> upperIndexExpression);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSlice(Expression<C> collectionExpression, Integer lowerIndex, Integer upperIndex);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(Expression<C> collectionExpression, Expression<? extends E> oldElementExpression, Expression<? extends E> newElementExpression);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(Expression<C> collectionExpression, Expression<? extends E> oldElementExpression, E newElement);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(Expression<C> collectionExpression, E oldElement, Expression<? extends E> newElementExpression);

	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(Expression<C> collectionExpression, E oldElement, E newElement);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionTrim(Expression<C> arrayExpression, Expression<Integer> elementCountExpression);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionTrim(Expression<C> arrayExpression, Integer elementCount);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionReverse(Expression<C> collectionExpression);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSort(Expression<C> collectionExpression);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSort(Expression<C> collectionExpression, boolean descending);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSort(
			Expression<C> collectionExpression,
			Expression<Boolean> descendingExpression);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSort(
			Expression<C> collectionExpression,
			boolean descending,
			boolean nullsFirst);

	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSort(
			Expression<C> collectionExpression,
			Expression<Boolean> descendingExpression,
			Expression<Boolean> nullsFirstExpression);

	@Override
	<T> SqmExpression<Collection<T>> collectionFill(Expression<T> elementExpression, Expression<Integer> elementCountExpression);

	@Override
	<T> SqmExpression<Collection<T>> collectionFill(Expression<T> elementExpression, Integer elementCount);

	@Override
	<T> SqmExpression<Collection<T>> collectionFill(T element, Expression<Integer> elementCountExpression);

	@Override
	<T> SqmExpression<Collection<T>> collectionFill(T element, Integer elementCount);

	@Override
	SqmExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, Expression<String> separatorExpression);

	@Override
	SqmExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, String separator);

	@Override
	SqmExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, Expression<String> separatorExpression, Expression<String> defaultExpression);

	@Override
	SqmExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, Expression<String> separatorExpression, String defaultValue);

	@Override
	SqmExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, String separator, Expression<String> defaultExpression);

	@Override
	SqmExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, String separator, String defaultValue);

	@Override
	<E> SqmPredicate collectionContains(Expression<? extends Collection<E>> collectionExpression, Expression<? extends E> elementExpression);

	@Override
	<E> SqmPredicate collectionContains(Expression<? extends Collection<E>> collectionExpression, E element);

	@Override
	<E> SqmPredicate collectionContains(Collection<E> collection, Expression<E> elementExpression);

	@Override
	<E> SqmPredicate collectionContainsNullable(Expression<? extends Collection<E>> collectionExpression, Expression<? extends E> elementExpression);

	@Override
	<E> SqmPredicate collectionContainsNullable(Expression<? extends Collection<E>> collectionExpression, E element);

	@Override
	<E> SqmPredicate collectionContainsNullable(Collection<E> collection, Expression<E> elementExpression);

	@Override
	default <E> SqmPredicate collectionContainsAll(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return collectionIncludes( collectionExpression, subCollectionExpression );
	}

	@Override
	default <E> SqmPredicate collectionContainsAll(
			Expression<? extends Collection<E>> collectionExpression,
			Collection<? extends E> subCollection) {
		return collectionIncludes( collectionExpression, subCollection );
	}

	@Override
	default <E> SqmPredicate collectionContainsAll(
			Collection<E> collection,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return collectionIncludes( collection, subCollectionExpression );
	}

	@Override
	default <E> SqmPredicate collectionContainsAllNullable(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return collectionIncludesNullable( collectionExpression, subCollectionExpression );
	}

	@Override
	default <E> SqmPredicate collectionContainsAllNullable(
			Expression<? extends Collection<E>> collectionExpression,
			Collection<? extends E> subCollection) {
		return collectionIncludesNullable( collectionExpression, subCollection );
	}

	@Override
	default <E> SqmPredicate collectionContainsAllNullable(Collection<E> collection, Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return collectionIncludesNullable( collection, subCollectionExpression );
	}

	@Override
	<E> SqmPredicate collectionIncludes(Expression<? extends Collection<E>> collectionExpression, Expression<? extends Collection<? extends E>> subCollectionExpression);

	@Override
	<E> SqmPredicate collectionIncludes(Expression<? extends Collection<E>> collectionExpression, Collection<? extends E> subCollection);

	@Override
	<E> SqmPredicate collectionIncludes(Collection<E> collection, Expression<? extends Collection<? extends E>> subArrayExpression);

	@Override
	<E> SqmPredicate collectionIncludesNullable(Expression<? extends Collection<E>> collectionExpression, Expression<? extends Collection<? extends E>> subCollectionExpression);

	@Override
	<E> SqmPredicate collectionIncludesNullable(Expression<? extends Collection<E>> collectionExpression, Collection<? extends E> subCollection);

	@Override
	<E> SqmPredicate collectionIncludesNullable(Collection<E> collection, Expression<? extends Collection<? extends E>> subCollectionExpression);

	@Override
	default <E> SqmPredicate collectionOverlaps(Expression<? extends Collection<E>> collectionExpression1, Expression<? extends Collection<? extends E>> collectionExpression2) {
		return collectionIntersects( collectionExpression1, collectionExpression2 );
	}

	@Override
	default <E> SqmPredicate collectionOverlaps(Expression<? extends Collection<E>> collectionExpression1, Collection<? extends E> collection2) {
		return collectionIntersects( collectionExpression1, collection2 );
	}

	@Override
	default <E> SqmPredicate collectionOverlaps(Collection<E> collection1, Expression<? extends Collection<? extends E>> collectionExpression2) {
		return collectionIntersects( collection1, collectionExpression2 );
	}

	@Override
	default <E> SqmPredicate collectionOverlapsNullable(Expression<? extends Collection<E>> collectionExpression1, Expression<? extends Collection<? extends E>> collectionExpression2) {
		return collectionIntersectsNullable( collectionExpression1, collectionExpression2 );
	}

	@Override
	default <E> SqmPredicate collectionOverlapsNullable(Expression<? extends Collection<E>> collectionExpression1, Collection<? extends E> collection2) {
		return collectionIntersectsNullable( collectionExpression1, collection2 );
	}

	@Override
	default <E> SqmPredicate collectionOverlapsNullable(Collection<E> collection1, Expression<? extends Collection<? extends E>> collectionExpression2) {
		return collectionIntersectsNullable( collection1, collectionExpression2 );
	}

	@Override
	<E> SqmPredicate collectionIntersects(Expression<? extends Collection<E>> collectionExpression1, Expression<? extends Collection<? extends E>> collectionExpression2);

	@Override
	<E> SqmPredicate collectionIntersects(Expression<? extends Collection<E>> collectionExpression1, Collection<? extends E> collection2);

	@Override
	<E> SqmPredicate collectionIntersects(Collection<E> collection1, Expression<? extends Collection<? extends E>> collectionExpression2);

	@Override
	<E> SqmPredicate collectionIntersectsNullable(Expression<? extends Collection<E>> collectionExpression1, Expression<? extends Collection<? extends E>> collectionExpression2);

	@Override
	<E> SqmPredicate collectionIntersectsNullable(Expression<? extends Collection<E>> collectionExpression1, Collection<? extends E> collection2);

	@Override
	<E> SqmPredicate collectionIntersectsNullable(Collection<E> collection1, Expression<? extends Collection<? extends E>> collectionExpression2);

	@Override
	<T> SqmJsonValueExpression<T> jsonValue(
			Expression<?> jsonDocument,
			Expression<String> jsonPath,
			Class<T> returningType);

	@Override
	SqmJsonValueExpression<String> jsonValue(Expression<?> jsonDocument, Expression<String> jsonPath);

	@Override
	<T> SqmJsonValueExpression<T> jsonValue(Expression<?> jsonDocument, String jsonPath, Class<T> returningType);

	@Override
	SqmJsonValueExpression<String> jsonValue(Expression<?> jsonDocument, String jsonPath);

	@Override
	SqmJsonQueryExpression jsonQuery(Expression<?> jsonDocument, Expression<String> jsonPath);

	@Override
	SqmJsonQueryExpression jsonQuery(Expression<?> jsonDocument, String jsonPath);

	@Override
	SqmJsonExistsExpression jsonExists(Expression<?> jsonDocument, Expression<String> jsonPath);

	@Override
	SqmJsonExistsExpression jsonExists(Expression<?> jsonDocument, String jsonPath);

	@Override
	SqmExpression<String> jsonArrayWithNulls(Expression<?>... values);

	@Override
	SqmExpression<String> jsonArray(Expression<?>... values);

	@Override
	SqmExpression<String> jsonObjectWithNulls(Map<?, ? extends Expression<?>> keyValues);

	@Override
	SqmExpression<String> jsonObject(Map<?, ? extends Expression<?>> keyValues);

	@Override
	SqmExpression<String> jsonArrayAgg(Expression<?> value);

	@Override
	SqmExpression<String> jsonArrayAggWithNulls(Expression<?> value);

	@Override
	SqmExpression<String> jsonArrayAggWithNulls(Expression<?> value, Predicate filter, JpaOrder... orderBy);

	@Override
	SqmExpression<String> jsonArrayAggWithNulls(Expression<?> value, Predicate filter);

	@Override
	SqmExpression<String> jsonArrayAggWithNulls(Expression<?> value, JpaOrder... orderBy);

	@Override
	SqmExpression<String> jsonArrayAgg(Expression<?> value, Predicate filter, JpaOrder... orderBy);

	@Override
	SqmExpression<String> jsonArrayAgg(Expression<?> value, Predicate filter);

	@Override
	SqmExpression<String> jsonArrayAgg(Expression<?> value, JpaOrder... orderBy);

	@Override
	SqmExpression<String> jsonObjectAggWithUniqueKeysAndNulls(Expression<?> key, Expression<?> value);

	@Override
	SqmExpression<String> jsonObjectAggWithUniqueKeys(Expression<?> key, Expression<?> value);

	@Override
	SqmExpression<String> jsonObjectAggWithNulls(Expression<?> key, Expression<?> value);

	@Override
	SqmExpression<String> jsonObjectAgg(Expression<?> key, Expression<?> value);

	@Override
	SqmExpression<String> jsonObjectAggWithUniqueKeysAndNulls(Expression<?> key, Expression<?> value, Predicate filter);

	@Override
	SqmExpression<String> jsonObjectAggWithUniqueKeys(Expression<?> key, Expression<?> value, Predicate filter);

	@Override
	SqmExpression<String> jsonObjectAggWithNulls(Expression<?> key, Expression<?> value, Predicate filter);

	@Override
	SqmExpression<String> jsonObjectAgg(Expression<?> key, Expression<?> value, Predicate filter);

	@Override
	SqmExpression<String> jsonSet(Expression<?> jsonDocument, Expression<String> jsonPath, Object value);

	@Override
	SqmExpression<String> jsonSet(Expression<?> jsonDocument, String jsonPath, Object value);

	@Override
	SqmExpression<String> jsonSet(Expression<?> jsonDocument, Expression<String> jsonPath, Expression<?> value);

	@Override
	SqmExpression<String> jsonSet(Expression<?> jsonDocument, String jsonPath, Expression<?> value);

	@Override
	SqmExpression<String> jsonRemove(Expression<?> jsonDocument, String jsonPath);

	@Override
	SqmExpression<String> jsonRemove(Expression<?> jsonDocument, Expression<String> jsonPath);

	@Override
	SqmExpression<String> jsonInsert(Expression<?> jsonDocument, Expression<String> jsonPath, Object value);

	@Override
	SqmExpression<String> jsonInsert(Expression<?> jsonDocument, String jsonPath, Object value);

	@Override
	SqmExpression<String> jsonInsert(Expression<?> jsonDocument, Expression<String> jsonPath, Expression<?> value);

	@Override
	SqmExpression<String> jsonInsert(Expression<?> jsonDocument, String jsonPath, Expression<?> value);

	@Override
	SqmExpression<String> jsonReplace(Expression<?> jsonDocument, Expression<String> jsonPath, Object value);

	@Override
	SqmExpression<String> jsonReplace(Expression<?> jsonDocument, String jsonPath, Object value);

	@Override
	SqmExpression<String> jsonReplace(Expression<?> jsonDocument, Expression<String> jsonPath, Expression<?> value);

	@Override
	SqmExpression<String> jsonReplace(Expression<?> jsonDocument, String jsonPath, Expression<?> value);

	@Override
	SqmExpression<String> jsonMergepatch(String document, Expression<?> patch);

	@Override
	SqmExpression<String> jsonMergepatch(Expression<?> document, String patch);

	@Override
	SqmExpression<String> jsonMergepatch(Expression<?> document, Expression<?> patch);

	@Override
	SqmXmlElementExpression xmlelement(String elementName);

	@Override
	SqmExpression<String> xmlcomment(String comment);

	@Override
	<T> SqmExpression<T> named(Expression<T> expression, String name);

	@Override
	SqmExpression<String> xmlforest(List<? extends Expression<?>> elements);

	@Override
	SqmExpression<String> xmlforest(Expression<?>... elements);

	@Override
	SqmExpression<String> xmlconcat(Expression<?>... elements);

	@Override
	SqmExpression<String> xmlconcat(List<? extends Expression<?>> elements);

	@Override
	SqmExpression<String> xmlpi(String elementName);

	@Override
	SqmExpression<String> xmlpi(String elementName, Expression<String> content);

	@Override
	SqmExpression<String> xmlquery(String query, Expression<?> xmlDocument);

	@Override
	SqmExpression<String> xmlquery(Expression<String> query, Expression<?> xmlDocument);

	@Override
	SqmExpression<Boolean> xmlexists(String query, Expression<?> xmlDocument);

	@Override
	SqmExpression<Boolean> xmlexists(Expression<String> query, Expression<?> xmlDocument);

	@Override
	SqmExpression<String> xmlagg(JpaOrder order, Expression<?> argument);

	@Override
	SqmExpression<String> xmlagg(JpaOrder order, JpaPredicate filter, Expression<?> argument);

	@Override
	SqmExpression<String> xmlagg(JpaOrder order, JpaWindow window, Expression<?> argument);

	@Override
	SqmExpression<String> xmlagg(JpaOrder order, JpaPredicate filter, JpaWindow window, Expression<?> argument);

	@Override
	<E> SqmSetReturningFunction<E> setReturningFunction(String name, Expression<?>... args);

	@Override
	<E> SqmSetReturningFunction<E> unnestArray(Expression<E[]> array);

	@Override
	<E> SqmSetReturningFunction<E> unnestCollection(Expression<? extends Collection<E>> collection);

	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(Expression<E> start, Expression<E> stop, Expression<? extends TemporalAmount> step);

	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(E start, E stop, TemporalAmount step);

	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(E start, Expression<E> stop, TemporalAmount step);

	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(Expression<E> start, E stop, TemporalAmount step);

	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(Expression<E> start, Expression<E> stop, TemporalAmount step);

	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(E start, E stop, Expression<? extends TemporalAmount> step);

	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(Expression<E> start, E stop, Expression<? extends TemporalAmount> step);

	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(E start, Expression<E> stop, Expression<? extends TemporalAmount> step);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, Expression<E> stop, Expression<E> step);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(E start, E stop, E step);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(E start, E stop, Expression<E> step);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, E stop, E step);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(E start, Expression<E> stop, E step);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, Expression<E> stop, E step);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, E stop, Expression<E> step);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(E start, Expression<E> stop, Expression<E> step);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, Expression<E> stop);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(Expression<E> start, E stop);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(E start, Expression<E> stop);

	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(E start, E stop);

	@Override
	SqmJsonTableFunction<?> jsonTable(Expression<?> jsonDocument);

	@Override
	SqmJsonTableFunction<?> jsonTable(Expression<?> jsonDocument, String jsonPath);

	@Override
	SqmJsonTableFunction<?> jsonTable(Expression<?> jsonDocument, Expression<String> jsonPath);

	@Override
	SqmXmlTableFunction<?> xmlTable(String xpath, Expression<?> xmlDocument);

	@Override
	SqmXmlTableFunction<?> xmlTable(Expression<String> xpath, Expression<?> xmlDocument);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Nonnull
	@Override
	SqmSelectStatement<Object> createQuery();

	@Nonnull
	@Override
	<T> SqmSelectStatement<T> createQuery(@Nonnull Class<T> resultClass);

	@Override
	<T> SqmSelectStatement<T> createQuery(String hql, Class<T> resultClass);

	@Nonnull
	@Override
	SqmSelectStatement<Tuple> createTupleQuery();

	@Nonnull
	@Override
	<Y> JpaCompoundSelection<Y> construct(@Nonnull Class<Y> resultClass, @Nonnull Selection<?>... selections);

	@Override
	<Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, List<? extends Selection<?>> arguments);

	@Nonnull
	@Override
	JpaCompoundSelection<Tuple> tuple(@Nonnull Selection<?>... selections);

	@Nonnull
	@Override
	JpaCompoundSelection<Tuple> tuple(@Nonnull List<Selection<?>> selections);

	@Nonnull
	@Override
	JpaCompoundSelection<Object[]> array(@Nonnull Selection<?>... selections);

	@Nonnull
	@Override
	JpaCompoundSelection<Object[]> array(@Nonnull List<Selection<?>> selections);

	@Nonnull
	@Override
	<T> SqmUpdateStatement<T> createCriteriaUpdate(@Nonnull Class<T> targetEntity);

	@Nonnull
	@Override
	<T> SqmDeleteStatement<T> createCriteriaDelete(@Nonnull Class<T> targetEntity);

	@Override
	<T> SqmInsertValuesStatement<T> createCriteriaInsertValues(Class<T> targetEntity);

	@Override
	<T> SqmInsertSelectStatement<T> createCriteriaInsertSelect(Class<T> targetEntity);

	@Override
	SqmValues values(Expression<?>... expressions);

	@Override
	SqmValues values(List<? extends Expression<?>> expressions);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> abs(@Nonnull Expression<N> x);

	@Override
	<X, T> SqmExpression<X> cast(JpaExpression<T> expression, Class<X> castTargetJavaType);

	@Override
	<X, T> SqmExpression<X> cast(JpaExpression<T> expression, JpaCastTarget<X> castTarget);

	@Override
	<X> SqmCastTarget<X> castTarget(Class<X> castTargetJavaType);

	@Override
	<X> SqmCastTarget<X> castTarget(Class<X> castTargetJavaType, long length);

	@Override
	<X> SqmCastTarget<X> castTarget(Class<X> castTargetJavaType, int precision, int scale);

	@Override
	SqmPredicate wrap(Expression<Boolean> expression);

	@Override @SuppressWarnings("unchecked")
	SqmPredicate wrap(Expression<Boolean>... expressions);

	@Override
	SqmPredicate wrap(BooleanExpression... expressions);

	SqmPredicate wrap(List<? extends Expression<Boolean>> restrictions);

	@Override
	SqmExpression<?> fk(Path<?> path);

	@Override
	SqmExpression<?> id(Path<?> path);

	@Override
	SqmExpression<?> version(Path<?> path);

	@Nonnull
	@Override
	<X, T extends X> SqmPath<T> treat(@Nonnull Path<X> path, @Nonnull Class<T> type);

	@Nonnull
	@Override
	<X, T extends X> SqmRoot<T> treat(@Nonnull Root<X> root, @Nonnull Class<T> type);

	@Nonnull
	@Override
	<X, Y, T extends Y> SqmFrom<X, T> treat(@Nonnull From<X, Y> from, @Nonnull Class<T> type);

	@Nonnull
	@Override
	<X, T, V extends T> SqmSingularJoin<X, V> treat(@Nonnull Join<X, T> join, @Nonnull Class<V> type);

	@Nonnull
	@Override
	<X, T, E extends T> SqmBagJoin<X, E> treat(@Nonnull CollectionJoin<X, T> join, @Nonnull Class<E> type);

	@Nonnull
	@Override
	<X, T, E extends T> SqmSetJoin<X, E> treat(@Nonnull SetJoin<X, T> join, @Nonnull Class<E> type);

	@Nonnull
	@Override
	<X, T, E extends T> SqmListJoin<X, E> treat(@Nonnull ListJoin<X, T> join, @Nonnull Class<E> type);

	@Nonnull
	@Override
	<X, K, T, V extends T> SqmMapJoin<X, K, V> treat(@Nonnull MapJoin<X, K, T> join, @Nonnull Class<V> type);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<Double> avg(@Nonnull Expression<N> argument);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> sum(@Nonnull Expression<N> argument);

	@Nonnull
	@Override
	SqmExpression<Long> sumAsLong(@Nonnull Expression<Integer> argument);

	@Nonnull
	@Override
	SqmExpression<Double> sumAsDouble(@Nonnull Expression<Float> argument);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> max(@Nonnull Expression<N> argument);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> min(@Nonnull Expression<N> argument);

	@Nonnull
	@Override
	<X extends Comparable<? super X>> SqmExpression<X> greatest(@Nonnull Expression<X> argument);

	@Nonnull
	@Override
	<X extends Comparable<? super X>> SqmExpression<X> least(@Nonnull Expression<X> argument);

	@Nonnull
	@Override
	SqmExpression<Long> count(@Nonnull Expression<?> argument);

	@Nonnull
	@Override
	SqmExpression<Long> countDistinct(@Nonnull Expression<?> x);

	@Override
	SqmExpression<Long> count();

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> neg(@Nonnull Expression<N> x);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> sum(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> sum(@Nonnull Expression<? extends N> x, N y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> sum(N x, @Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> prod(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> prod(@Nonnull Expression<? extends N> x, N y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> prod(N x, @Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> diff(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> diff(@Nonnull Expression<? extends N> x, N y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> diff(N x, @Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmExpression<Number> quot(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmExpression<Number> quot(@Nonnull Expression<? extends Number> x, Number y);

	@Nonnull
	@Override
	SqmExpression<Number> quot(Number x, @Nonnull Expression<? extends Number> y);

	SqmExpression<Number> quotPortable(Expression<? extends Number> x, Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmExpression<Integer> mod(@Nonnull Expression<Integer> x, @Nonnull Expression<Integer> y);

	@Nonnull
	@Override
	SqmExpression<Integer> mod(@Nonnull Expression<Integer> x, Integer y);

	@Nonnull
	@Override
	SqmExpression<Integer> mod(Integer x, @Nonnull Expression<Integer> y);

	@Nonnull
	@Override
	SqmExpression<Double> sqrt(@Nonnull Expression<? extends Number> x);

	@Nonnull
	@Override
	SqmExpression<Long> toLong(@Nonnull Expression<? extends Number> number);

	@Nonnull
	@Override
	SqmExpression<Integer> toInteger(@Nonnull Expression<? extends Number> number);

	@Nonnull
	@Override
	SqmExpression<Float> toFloat(@Nonnull Expression<? extends Number> number);

	@Nonnull
	@Override
	SqmExpression<Double> toDouble(@Nonnull Expression<? extends Number> number);

	@Nonnull
	@Override
	SqmExpression<BigDecimal> toBigDecimal(@Nonnull Expression<? extends Number> number);

	@Nonnull
	@Override
	SqmExpression<BigInteger> toBigInteger(@Nonnull Expression<? extends Number> number);

	@Nonnull
	@Override
	SqmExpression<String> toString(@Nonnull Expression<Character> character);

	@Nonnull
	@Override
	<T> SqmExpression<T> literal(@Nonnull T value);

	@Override
	<T> List<? extends SqmExpression<T>> literals(T[] values);

	@Override
	<T> List<? extends SqmExpression<T>> literals(List<T> values);

	@Nonnull
	@Override
	<T> SqmExpression<T> nullLiteral(@Nonnull Class<T> resultClass);

	/**
	 * @implNote Notice that this returns a JPA parameter not the SqmParameter
	 * @see JpaParameterExpression
	 *
	 */
	@Nonnull
	@Override
	<T> JpaParameterExpression<T> parameter(@Nonnull Class<T> paramClass);

	@Nonnull
	@Override
	<T> JpaParameterExpression<T> parameter(@Nonnull Class<T> paramClass, @Nonnull String name);

	@Nonnull
	@Override
	SqmExpression<String> concat(@Nonnull Expression<String> x, @Nonnull Expression<String> y);

	@Nonnull
	@Override
	SqmExpression<String> concat(@Nonnull Expression<String> x, @Nonnull String y);

	@Nonnull
	@Override
	SqmExpression<String> concat(@Nonnull String x, @Nonnull Expression<String> y);

	@Override
	SqmExpression<String> concat(String x, String y);

	@Nonnull
	@Override
	SqmFunction<String> substring(@Nonnull Expression<String> x, @Nonnull Expression<Integer> from);

	@Nonnull
	@Override
	SqmFunction<String> substring(@Nonnull Expression<String> x, int from);

	@Nonnull
	@Override
	SqmFunction<String> substring(@Nonnull Expression<String> x, @Nonnull Expression<Integer> from, @Nonnull Expression<Integer> len);

	@Nonnull
	@Override
	SqmFunction<String> substring(@Nonnull Expression<String> x, int from, int len);

	@Nonnull
	@Override
	SqmFunction<String> trim(@Nonnull Expression<String> x);

	@Nonnull
	@Override
	SqmFunction<String> trim(@Nonnull Trimspec ts, @Nonnull Expression<String> x);

	@Nonnull
	@Override
	SqmFunction<String> trim(@Nonnull Expression<Character> t, @Nonnull Expression<String> x);

	@Nonnull
	@Override
	SqmFunction<String> trim(@Nonnull Trimspec ts, @Nonnull Expression<Character> t, @Nonnull Expression<String> x);

	@Nonnull
	@Override
	SqmFunction<String> trim(char t, @Nonnull Expression<String> x);

	@Nonnull
	@Override
	SqmFunction<String> trim(@Nonnull Trimspec ts, char t, @Nonnull Expression<String> x);

	@Nonnull
	@Override
	SqmFunction<String> lower(@Nonnull Expression<String> x);

	@Nonnull
	@Override
	SqmFunction<String> upper(@Nonnull Expression<String> x);

	@Nonnull
	@Override
	SqmFunction<Integer> length(@Nonnull Expression<String> x);

	@Nonnull
	@Override
	SqmFunction<Integer> locate(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern);

	@Nonnull
	@Override
	SqmFunction<Integer> locate(@Nonnull Expression<String> x, @Nonnull String pattern);

	@Nonnull
	@Override
	SqmFunction<Integer> locate(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Integer> from);

	@Nonnull
	@Override
	SqmFunction<Integer> locate(@Nonnull Expression<String> x, @Nonnull String pattern, int from);

	@Nonnull
	@Override
	SqmFunction<Date> currentDate();

	@Nonnull
	@Override
	SqmFunction<Timestamp> currentTimestamp();

	@Nonnull
	@Override
	SqmFunction<Time> currentTime();

	SqmFunction<Instant> currentInstant();

	@Nonnull
	@Override
	SqmExpression<LocalDate> localDate();

	@Nonnull
	@Override
	SqmExpression<LocalDateTime> localDateTime();

	@Nonnull
	@Override
	SqmExpression<LocalTime> localTime();

	@Nonnull
	@Override
	<T> SqmFunction<T> function(@Nonnull String name, @Nonnull Class<T> type, @Nonnull Expression<?>[] args);

	@Nonnull
	@Override
	<Y> SqmModifiedSubQueryExpression<Y> all(@Nonnull Subquery<Y> subquery);

	@Nonnull
	@Override
	<Y> SqmModifiedSubQueryExpression<Y> some(@Nonnull Subquery<Y> subquery);

	@Nonnull
	@Override
	<Y> SqmModifiedSubQueryExpression<Y> any(@Nonnull Subquery<Y> subquery);

	@Override
	<K, L extends List<?>> SqmExpression<Set<K>> indexes(L list);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<Integer> size(@Nonnull Expression<C> collection);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<Integer> size(@Nonnull C collection);

	@Nonnull
	@Override
	<T> JpaCoalesce<T> coalesce();

	@Nonnull
	@Override
	<Y> JpaCoalesce<Y> coalesce(
			@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y);

	@Nonnull
	@Override
	<Y> JpaCoalesce<Y> coalesce(@Nonnull Expression<? extends Y> x, Y y);

	@Nonnull
	@Override
	<Y> SqmExpression<Y> nullif(@Nonnull Expression<Y> x, @Nonnull Expression<?> y);

	@Nonnull
	@Override
	<Y> SqmExpression<Y> nullif(@Nonnull Expression<Y> x, Y y);

	@Nonnull
	@Override
	<C, R> JpaSimpleCase<C, R> selectCase(@Nonnull Expression<? extends C> expression);

	@Nonnull
	@Override
	<C, R> JpaSimpleCase<C, R> selectCase(@Nonnull Expression<? extends C> expression, @Nonnull Class<R> resultType);

	@Nonnull
	@Override
	<R> JpaSearchedCase<R> selectCase();

	@Nonnull
	@Override
	<R> JpaSearchedCase<R> selectCase(@Nonnull Class<R> resultType);

	@Nonnull
	@Override
	SqmPredicate and(@Nonnull Expression<Boolean> x, @Nonnull Expression<Boolean> y);

	@Nonnull
	@Override
	SqmPredicate and(@Nonnull Predicate... restrictions);

	@Nonnull
	@Override
	SqmPredicate or(@Nonnull Expression<Boolean> x, @Nonnull Expression<Boolean> y);

	@Nonnull
	@Override
	SqmPredicate or(@Nonnull Predicate... restrictions);

	@Nonnull
	@Override
	SqmPredicate not(@Nonnull Expression<Boolean> restriction);

	@Nonnull
	@Override
	SqmPredicate conjunction();

	@Nonnull
	@Override
	SqmPredicate disjunction();

	@Nonnull
	@Override
	SqmPredicate isTrue(@Nonnull Expression<Boolean> x);

	@Nonnull
	@Override
	SqmPredicate isFalse(@Nonnull Expression<Boolean> x);

	@Nonnull
	@Override
	SqmPredicate isNull(@Nonnull Expression<?> x);

	@Nonnull
	@Override
	SqmPredicate isNotNull(@Nonnull Expression<?> x);

	@Nonnull
	@Override
	SqmPredicate equal(@Nonnull Expression<?> x, @Nonnull Expression<?> y);

	@Nonnull
	@Override
	SqmPredicate equal(@Nonnull Expression<?> x, Object y);

	@Nonnull
	@Override
	SqmPredicate notEqual(@Nonnull Expression<?> x, @Nonnull Expression<?> y);

	@Nonnull
	@Override
	SqmPredicate notEqual(@Nonnull Expression<?> x, Object y);

	@Override
	SqmPredicate distinctFrom(Expression<?> x, Expression<?> y);

	@Override
	SqmPredicate distinctFrom(Expression<?> x, Object y);

	@Override
	SqmPredicate notDistinctFrom(Expression<?> x, Expression<?> y);

	@Override
	SqmPredicate notDistinctFrom(Expression<?> x, Object y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThan(@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThan(@Nonnull Expression<? extends Y> x, Y y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(@Nonnull Expression<? extends Y> x, Y y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThan(@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThan(@Nonnull Expression<? extends Y> x, Y y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(@Nonnull Expression<? extends Y> x, Y y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate between(
			@Nonnull Expression<? extends Y> value,
			@Nonnull Expression<? extends Y> lower,
			@Nonnull Expression<? extends Y> upper);

	SqmPredicate between(Expression<?> value, Expression<?> lower, Expression<?> upper, boolean negated);

	SqmPredicate comparison(Expression<?> x, ComparisonOperator operator, Expression<?> y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate between(@Nonnull Expression<? extends Y> value, Y lower, Y upper);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate between(
			Y value,
			@Nonnull Expression<? extends Y> lower,
			@Nonnull Expression<? extends Y> upper);

	@Nonnull
	@Override
	SqmPredicate gt(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate gt(@Nonnull Expression<? extends Number> x, Number y);

	@Nonnull
	@Override
	SqmPredicate ge(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate ge(@Nonnull Expression<? extends Number> x, Number y);

	@Nonnull
	@Override
	SqmPredicate lt(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate lt(@Nonnull Expression<? extends Number> x, Number y);

	@Nonnull
	@Override
	SqmPredicate le(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate le(@Nonnull Expression<? extends Number> x, Number y);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmPredicate isEmpty(@Nonnull Expression<C> collection);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmPredicate isNotEmpty(@Nonnull Expression<C> collection);

	@Nonnull
	@Override
	<E, C extends Collection<E>> SqmPredicate isMember(@Nonnull Expression<E> elem, @Nonnull Expression<C> collection);

	@Nonnull
	@Override
	<E, C extends Collection<E>> SqmPredicate isMember(E elem, @Nonnull Expression<C> collection);

	@Nonnull
	@Override
	<E, C extends Collection<E>> SqmPredicate isNotMember(@Nonnull Expression<E> elem, @Nonnull Expression<C> collection);

	@Nonnull
	@Override
	<E, C extends Collection<E>> SqmPredicate isNotMember(E elem, @Nonnull Expression<C> collection);

	@Nonnull
	@Override
	SqmPredicate like(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern);

	@Nonnull
	@Override
	SqmPredicate like(@Nonnull Expression<String> x, @Nonnull String pattern);

	@Nonnull
	@Override
	SqmPredicate like(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar);

	@Nonnull
	@Override
	SqmPredicate like(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar);

	@Nonnull
	@Override
	SqmPredicate like(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull Expression<Character> escapeChar);

	@Nonnull
	@Override
	SqmPredicate like(@Nonnull Expression<String> x, @Nonnull String pattern, char escapeChar);

	@Nonnull
	@Override
	SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern);

	@Nonnull
	@Override
	SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern);

	@Nonnull
	@Override
	SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar);

	@Nonnull
	@Override
	SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar);

	@Nonnull
	@Override
	SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull Expression<Character> escapeChar);

	@Nonnull
	@Override
	SqmPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern, char escapeChar);

	@Nonnull
	@Override
	<T> SqmInPredicate<T> in(@Nonnull Expression<? extends T> expression);

	@Override
	<T> SqmInPredicate<T> in(Expression<? extends T> expression, Expression<? extends T>... values);

	@Override
	<T> SqmInPredicate<T> in(Expression<? extends T> expression, T... values);

	@Override
	<T> SqmInPredicate<T> in(Expression<? extends T> expression, Collection<T> values);

	<T> SqmInPredicate<T> in(Expression<? extends T> expression, SqmSubQuery<T> subQuery);

	@Nonnull
	@Override
	SqmPredicate exists(@Nonnull Subquery<?> subquery);

	@Override
	<M extends Map<?, ?>> SqmPredicate isMapEmpty(JpaExpression<M> mapExpression);

	@Override
	<M extends Map<?, ?>> SqmPredicate isMapNotEmpty(JpaExpression<M> mapExpression);

	@Override
	<M extends Map<?,?>> SqmExpression<Integer> mapSize(JpaExpression<M> mapExpression);

	@Override
	<M extends Map<?, ?>> SqmExpression<Integer> mapSize(M map);

	@Override
	SqmSortSpecification sort(JpaExpression<?> sortExpression, SortDirection sortOrder, Nulls nullPrecedence);

	@Override
	SqmSortSpecification sort(
			JpaExpression<?> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence,
			boolean ignoreCase);

	@Override
	SqmSortSpecification sort(JpaExpression<?> sortExpression, SortDirection sortOrder);

	@Override
	SqmSortSpecification sort(JpaExpression<?> sortExpression);

	@Nonnull
	@Override
	SqmSortSpecification asc(@Nonnull Expression<?> x);

	@Nonnull
	@Override
	SqmSortSpecification desc(@Nonnull Expression<?> x);

	BasicType<Boolean> getBooleanType();

	BasicType<Integer> getIntegerType();

	BasicType<Long> getLongType();

	BasicType<Character> getCharacterType();

	BasicType<String> getStringType();

	JpaCompliance getJpaCompliance();

	@Deprecated(since = "7.0", forRemoval = true)
	ImmutableEntityUpdateQueryHandlingMode getImmutableEntityUpdateQueryHandlingMode();

	@Deprecated(since = "8.0", forRemoval = true)
	boolean allowImmutableEntityUpdate();
}
