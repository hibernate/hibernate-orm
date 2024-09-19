/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.query.criteria.JpaWindow;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmModifiedSubQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
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
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
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
 */
@SuppressWarnings("unchecked")
public interface NodeBuilder extends HibernateCriteriaBuilder {
	JpaMetamodel getDomainModel();

	TypeConfiguration getTypeConfiguration();

	boolean isJpaQueryComplianceEnabled();

	QueryEngine getQueryEngine();

	<R> SqmTuple<R> tuple(
			Class<R> tupleType,
			SqmExpression<?>... expressions);

	<R> SqmTuple<R> tuple(
			Class<R> tupleType,
			List<? extends SqmExpression<?>> expressions);

	<R> SqmTuple<R> tuple(
			SqmExpressible<R> tupleType,
			SqmExpression<?>... expressions);

	<R> SqmTuple<R> tuple(
			SqmExpressible<R> tupleType,
			List<? extends SqmExpression<?>> expressions);

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

	@Override
	default <T> SqmPredicate arrayContainsAll(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression) {
		return arrayIncludes( arrayExpression, subArrayExpression );
	}

	@Override
	default <T> SqmPredicate arrayContainsAll(Expression<T[]> arrayExpression, T[] subArray) {
		return arrayIncludes( arrayExpression, subArray );
	}

	@Override
	default <T> SqmPredicate arrayContainsAll(T[] array, Expression<T[]> subArrayExpression) {
		return arrayIncludes( array, subArrayExpression );
	}

	@Override
	default <T> SqmPredicate arrayContainsAllNullable(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression) {
		return arrayIncludesNullable( arrayExpression, subArrayExpression );
	}

	@Override
	default <T> SqmPredicate arrayContainsAllNullable(Expression<T[]> arrayExpression, T[] subArray) {
		return arrayIncludesNullable( arrayExpression, subArray );
	}

	@Override
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

	@Override
	default <T> SqmPredicate arrayOverlaps(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return arrayIntersects( arrayExpression1, arrayExpression2 );
	}

	@Override
	default <T> SqmPredicate arrayOverlaps(Expression<T[]> arrayExpression1, T[] array2) {
		return arrayIntersects( arrayExpression1, array2 );
	}

	@Override
	default <T> SqmPredicate arrayOverlaps(T[] array1, Expression<T[]> arrayExpression2) {
		return arrayIntersects( array1, arrayExpression2 );
	}

	@Override
	default <T> SqmPredicate arrayOverlapsNullable(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return arrayIntersectsNullable( arrayExpression1, arrayExpression2 );
	}

	@Override
	default <T> SqmPredicate arrayOverlapsNullable(Expression<T[]> arrayExpression1, T[] array2) {
		return arrayIntersectsNullable( arrayExpression1, array2 );
	}

	@Override
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
	<T> SqmExpression<Collection<T>> collectionFill(Expression<T> elementExpression, Expression<Integer> elementCountExpression);

	@Override
	<T> SqmExpression<Collection<T>> collectionFill(Expression<T> elementExpression, Integer elementCount);

	@Override
	<T> SqmExpression<Collection<T>> collectionFill(T element, Expression<Integer> elementCountExpression);

	@Override
	<T> SqmExpression<Collection<T>> collectionFill(T element, Integer elementCount);

	@Override
	<T> SqmExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, Expression<String> separatorExpression);

	@Override
	<T> SqmExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, String separator);

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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Override
	SqmSelectStatement<Object> createQuery();

	@Override
	<T> SqmSelectStatement<T> createQuery(Class<T> resultClass);

	@Override
	<T> SqmSelectStatement<T> createQuery(String hql, Class<T> resultClass);

	@Override
	SqmSelectStatement<Tuple> createTupleQuery();

	@Override
	<Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, Selection<?>[] selections);

	@Override
	<Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, List<? extends JpaSelection<?>> arguments);

	@Override
	JpaCompoundSelection<Tuple> tuple(Selection<?>[] selections);

	@Override
	JpaCompoundSelection<Tuple> tuple(List<? extends JpaSelection<?>> selections);

	@Override
	JpaCompoundSelection<Object[]> array(Selection<?>[] selections);

	@Override
	JpaCompoundSelection<Object[]> array(List<? extends JpaSelection<?>> selections);

	@Override
	<T> SqmUpdateStatement<T> createCriteriaUpdate(Class<T> targetEntity);

	@Override
	<T> SqmDeleteStatement<T> createCriteriaDelete(Class<T> targetEntity);

	@Override
	<T> SqmInsertValuesStatement<T> createCriteriaInsertValues(Class<T> targetEntity);

	@Override
	<T> SqmInsertSelectStatement<T> createCriteriaInsertSelect(Class<T> targetEntity);

	@Override
	SqmValues values(Expression<?>... expressions);

	@Override
	SqmValues values(List<? extends Expression<?>> expressions);

	@Override
	<N extends Number> SqmExpression<N> abs(Expression<N> x);

	@Override
	<X, T> SqmExpression<X> cast(JpaExpression<T> expression, Class<X> castTargetJavaType);

	@Override
	SqmPredicate wrap(Expression<Boolean> expression);

	@Override
	SqmPredicate wrap(Expression<Boolean>... expressions);

	@Override
	<P, F> SqmExpression<F> fk(Path<P> path);

	@Override
	<X, T extends X> SqmPath<T> treat(Path<X> path, Class<T> type);

	@Override
	<X, T extends X> SqmRoot<T> treat(Root<X> root, Class<T> type);

	@Override
	<X, T, V extends T> SqmSingularJoin<X, V> treat(Join<X, T> join, Class<V> type);

	@Override
	<X, T, E extends T> SqmBagJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type);

	@Override
	<X, T, E extends T> SqmSetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type);

	@Override
	<X, T, E extends T> SqmListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type);

	@Override
	<X, K, T, V extends T> SqmMapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type);

	@Override
	<N extends Number> SqmExpression<Double> avg(Expression<N> argument);

	@Override
	<N extends Number> SqmExpression<N> sum(Expression<N> argument);

	@Override
	SqmExpression<Long> sumAsLong(Expression<Integer> argument);

	@Override
	SqmExpression<Double> sumAsDouble(Expression<Float> argument);

	@Override
	<N extends Number> SqmExpression<N> max(Expression<N> argument);

	@Override
	<N extends Number> SqmExpression<N> min(Expression<N> argument);

	@Override
	<X extends Comparable<? super X>> SqmExpression<X> greatest(Expression<X> argument);

	@Override
	<X extends Comparable<? super X>> SqmExpression<X> least(Expression<X> argument);

	@Override
	SqmExpression<Long> count(Expression<?> argument);

	@Override
	SqmExpression<Long> countDistinct(Expression<?> x);

	@Override
	SqmExpression<Long> count();

	@Override
	<N extends Number> SqmExpression<N> neg(Expression<N> x);

	@Override
	<N extends Number> SqmExpression<N> sum(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> SqmExpression<N> sum(Expression<? extends N> x, N y);

	@Override
	<N extends Number> SqmExpression<N> sum(N x, Expression<? extends N> y);

	@Override
	<N extends Number> SqmExpression<N> prod(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> SqmExpression<N> prod(Expression<? extends N> x, N y);

	@Override
	<N extends Number> SqmExpression<N> prod(N x, Expression<? extends N> y);

	@Override
	<N extends Number> SqmExpression<N> diff(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> SqmExpression<N> diff(Expression<? extends N> x, N y);

	@Override
	<N extends Number> SqmExpression<N> diff(N x, Expression<? extends N> y);

	@Override
	SqmExpression<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	SqmExpression<Number> quot(Expression<? extends Number> x, Number y);

	@Override
	SqmExpression<Number> quot(Number x, Expression<? extends Number> y);

	@Override
	SqmExpression<Integer> mod(Expression<Integer> x, Expression<Integer> y);

	@Override
	SqmExpression<Integer> mod(Expression<Integer> x, Integer y);

	@Override
	SqmExpression<Integer> mod(Integer x, Expression<Integer> y);

	@Override
	SqmExpression<Double> sqrt(Expression<? extends Number> x);

	@Override
	SqmExpression<Long> toLong(Expression<? extends Number> number);

	@Override
	SqmExpression<Integer> toInteger(Expression<? extends Number> number);

	@Override
	SqmExpression<Float> toFloat(Expression<? extends Number> number);

	@Override
	SqmExpression<Double> toDouble(Expression<? extends Number> number);

	@Override
	SqmExpression<BigDecimal> toBigDecimal(Expression<? extends Number> number);

	@Override
	SqmExpression<BigInteger> toBigInteger(Expression<? extends Number> number);

	@Override
	SqmExpression<String> toString(Expression<Character> character);

	@Override
	<T> SqmExpression<T> literal(T value);

	@Override
	<T> List<? extends SqmExpression<T>> literals(T[] values);

	@Override
	<T> List<? extends SqmExpression<T>> literals(List<T> values);

	@Override
	<T> SqmExpression<T> nullLiteral(Class<T> resultClass);

	/**
	 * @implNote Notice that this returns a JPA parameter not the SqmParameter
	 * @see JpaParameterExpression
	 *
	 */
	@Override
	<T> JpaParameterExpression<T> parameter(Class<T> paramClass);

	@Override
	<T> JpaParameterExpression<T> parameter(Class<T> paramClass, String name);

	@Override
	SqmExpression<String> concat(Expression<String> x, Expression<String> y);

	@Override
	SqmExpression<String> concat(Expression<String> x, String y);

	@Override
	SqmExpression<String> concat(String x, Expression<String> y);

	@Override
	SqmExpression<String> concat(String x, String y);

	@Override
	SqmFunction<String> substring(Expression<String> x, Expression<Integer> from);

	@Override
	SqmFunction<String> substring(Expression<String> x, int from);

	@Override
	SqmFunction<String> substring(Expression<String> x, Expression<Integer> from, Expression<Integer> len);

	@Override
	SqmFunction<String> substring(Expression<String> x, int from, int len);

	@Override
	SqmFunction<String> trim(Expression<String> x);

	@Override
	SqmFunction<String> trim(Trimspec ts, Expression<String> x);

	@Override
	SqmFunction<String> trim(Expression<Character> t, Expression<String> x);

	@Override
	SqmFunction<String> trim(Trimspec ts, Expression<Character> t, Expression<String> x);

	@Override
	SqmFunction<String> trim(char t, Expression<String> x);

	@Override
	SqmFunction<String> trim(Trimspec ts, char t, Expression<String> x);

	@Override
	SqmFunction<String> lower(Expression<String> x);

	@Override
	SqmFunction<String> upper(Expression<String> x);

	@Override
	SqmFunction<Integer> length(Expression<String> x);

	@Override
	SqmFunction<Integer> locate(Expression<String> x, Expression<String> pattern);

	@Override
	SqmFunction<Integer> locate(Expression<String> x, String pattern);

	@Override
	SqmFunction<Integer> locate(Expression<String> x, Expression<String> pattern, Expression<Integer> from);

	@Override
	SqmFunction<Integer> locate(Expression<String> x, String pattern, int from);

	@Override
	SqmFunction<Date> currentDate();

	@Override
	SqmFunction<Timestamp> currentTimestamp();

	@Override
	SqmFunction<Time> currentTime();

	SqmFunction<Instant> currentInstant();

	@Override
	<T> SqmFunction<T> function(String name, Class<T> type, Expression<?>[] args);

	@Override
	<Y> SqmModifiedSubQueryExpression<Y> all(Subquery<Y> subquery);

	@Override
	<Y> SqmModifiedSubQueryExpression<Y> some(Subquery<Y> subquery);

	@Override
	<Y> SqmModifiedSubQueryExpression<Y> any(Subquery<Y> subquery);

	@Override
	<K, M extends Map<K, ?>> SqmExpression<Set<K>> keys(M map);

	@Override
	<K, L extends List<?>> SqmExpression<Set<K>> indexes(L list);

	@Override
	<V, M extends Map<?, V>> Expression<Collection<V>> values(M map);

	@Override
	<C extends Collection<?>> SqmExpression<Integer> size(Expression<C> collection);

	@Override
	<C extends Collection<?>> SqmExpression<Integer> size(C collection);

	@Override
	<T> JpaCoalesce<T> coalesce();

	@Override
	<Y> JpaCoalesce<Y> coalesce(
			Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Y y);

	@Override
	<Y> SqmExpression<Y> nullif(Expression<Y> x, Expression<?> y);

	@Override
	<Y> SqmExpression<Y> nullif(Expression<Y> x, Y y);

	@Override
	<C, R> JpaSimpleCase<C, R> selectCase(Expression<? extends C> expression);

	@Override
	<R> JpaSearchedCase<R> selectCase();

	@Override
	SqmPredicate and(Expression<Boolean> x, Expression<Boolean> y);

	@Override
	SqmPredicate and(Predicate... restrictions);

	@Override
	SqmPredicate or(Expression<Boolean> x, Expression<Boolean> y);

	@Override
	SqmPredicate or(Predicate... restrictions);

	@Override
	SqmPredicate not(Expression<Boolean> restriction);

	@Override
	SqmPredicate conjunction();

	@Override
	SqmPredicate disjunction();

	@Override
	SqmPredicate isTrue(Expression<Boolean> x);

	@Override
	SqmPredicate isFalse(Expression<Boolean> x);

	@Override
	SqmPredicate isNull(Expression<?> x);

	@Override
	SqmPredicate isNotNull(Expression<?> x);

	@Override
	SqmPredicate equal(Expression<?> x, Expression<?> y);

	@Override
	SqmPredicate equal(Expression<?> x, Object y);

	@Override
	SqmPredicate notEqual(Expression<?> x, Expression<?> y);

	@Override
	SqmPredicate notEqual(Expression<?> x, Object y);

	@Override
	SqmPredicate distinctFrom(Expression<?> x, Expression<?> y);

	@Override
	SqmPredicate distinctFrom(Expression<?> x, Object y);

	@Override
	SqmPredicate notDistinctFrom(Expression<?> x, Expression<?> y);

	@Override
	SqmPredicate notDistinctFrom(Expression<?> x, Object y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThan(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThan(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThan(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThan(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate between(
			Expression<? extends Y> value,
			Expression<? extends Y> lower,
			Expression<? extends Y> upper);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate between(Expression<? extends Y> value, Y lower, Y upper);

	@Override
	SqmPredicate gt(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	SqmPredicate gt(Expression<? extends Number> x, Number y);

	@Override
	SqmPredicate ge(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	SqmPredicate ge(Expression<? extends Number> x, Number y);

	@Override
	SqmPredicate lt(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	SqmPredicate lt(Expression<? extends Number> x, Number y);

	@Override
	SqmPredicate le(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	SqmPredicate le(Expression<? extends Number> x, Number y);

	@Override
	<C extends Collection<?>> SqmPredicate isEmpty(Expression<C> collection);

	@Override
	<C extends Collection<?>> SqmPredicate isNotEmpty(Expression<C> collection);

	@Override
	<E, C extends Collection<E>> SqmPredicate isMember(Expression<E> elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> SqmPredicate isMember(E elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> SqmPredicate isNotMember(Expression<E> elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> SqmPredicate isNotMember(E elem, Expression<C> collection);

	@Override
	SqmPredicate like(Expression<String> x, Expression<String> pattern);

	@Override
	SqmPredicate like(Expression<String> x, String pattern);

	@Override
	SqmPredicate like(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	@Override
	SqmPredicate like(Expression<String> x, Expression<String> pattern, char escapeChar);

	@Override
	SqmPredicate like(Expression<String> x, String pattern, Expression<Character> escapeChar);

	@Override
	SqmPredicate like(Expression<String> x, String pattern, char escapeChar);

	@Override
	SqmPredicate notLike(Expression<String> x, Expression<String> pattern);

	@Override
	SqmPredicate notLike(Expression<String> x, String pattern);

	@Override
	SqmPredicate notLike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	@Override
	SqmPredicate notLike(Expression<String> x, Expression<String> pattern, char escapeChar);

	@Override
	SqmPredicate notLike(Expression<String> x, String pattern, Expression<Character> escapeChar);

	@Override
	SqmPredicate notLike(Expression<String> x, String pattern, char escapeChar);

	@Override
	<T> SqmInPredicate<T> in(Expression<? extends T> expression);

	@Override
	<T> SqmInPredicate<T> in(Expression<? extends T> expression, Expression<? extends T>... values);

	@Override
	<T> SqmInPredicate<T> in(Expression<? extends T> expression, T... values);

	@Override
	<T> SqmInPredicate<T> in(Expression<? extends T> expression, Collection<T> values);

	<T> SqmInPredicate<T> in(Expression<? extends T> expression, SqmSubQuery<T> subQuery);

	@Override
	SqmPredicate exists(Subquery<?> subquery);

	@Override
	<M extends Map<?, ?>> SqmPredicate isMapEmpty(JpaExpression<M> mapExpression);

	@Override
	<M extends Map<?, ?>> SqmPredicate isMapNotEmpty(JpaExpression<M> mapExpression);

	@Override
	<M extends Map<?,?>> SqmExpression<Integer> mapSize(JpaExpression<M> mapExpression);

	@Override
	<M extends Map<?, ?>> SqmExpression<Integer> mapSize(M map);

	@Override
	SqmSortSpecification sort(
			JpaExpression<?> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	@Override
	SqmSortSpecification sort(
			JpaExpression<?> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence,
			boolean ignoreCase);

	@Override
	SqmSortSpecification sort(JpaExpression<?> sortExpression, SortDirection sortOrder);

	@Override
	SqmSortSpecification sort(JpaExpression<?> sortExpression);

	@Override
	SqmSortSpecification asc(Expression<?> x);

	@Override
	SqmSortSpecification desc(Expression<?> x);

	BasicType<Boolean> getBooleanType();

	BasicType<Integer> getIntegerType();

	BasicType<Long> getLongType();

	BasicType<Character> getCharacterType();

	SessionFactoryImplementor getSessionFactory();
}
