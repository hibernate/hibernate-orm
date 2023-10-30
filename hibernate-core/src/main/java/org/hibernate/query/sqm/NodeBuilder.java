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

import org.hibernate.Incubating;
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

	/**
	 * @see #arrayAgg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 * @since 6.4
	 */
	<T> JpaExpression<T[]> arrayAgg(JpaOrder order, Expression<? extends T> argument);

	/**
	 * @see #arrayAgg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 * @since 6.4
	 */
	<T> JpaExpression<T[]> arrayAgg(JpaOrder order, JpaPredicate filter, Expression<? extends T> argument);

	/**
	 * @see #arrayAgg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 * @since 6.4
	 */
	<T> JpaExpression<T[]> arrayAgg(JpaOrder order, JpaWindow window, Expression<? extends T> argument);

	/**
	 * Create a {@code array_agg} ordered set-aggregate function expression.
	 *
	 * @param order order by clause used in within group
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 * @param argument values to aggregate
	 *
	 * @return ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 * @since 6.4
	 */
	<T> JpaExpression<T[]> arrayAgg(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<? extends T> argument);

	/**
	 * Creates an array literal with the {@code array} constructor function.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayLiteral(T... elements);

	/**
	 * Determines the 1-based position of an element in an array.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<Integer> arrayPosition(SqmExpression<T[]> arrayExpression, T element);

	/**
	 * Determines the 1-based position of an element in an array.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<Integer> arrayPosition(SqmExpression<T[]> arrayExpression, SqmExpression<T> elementExpression);

	/**
	 * Determines the length of an array.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<Integer> arrayLength(SqmExpression<T[]> arrayExpression);

	/**
	 * Concatenates arrays with each other in order.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayConcat(SqmExpression<T[]> arrayExpression1, SqmExpression<T[]> arrayExpression2);

	/**
	 * Concatenates arrays with each other in order.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayConcat(SqmExpression<T[]> arrayExpression1, T[] array2);

	/**
	 * Concatenates arrays with each other in order.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayConcat(T[] array1, SqmExpression<T[]> arrayExpression2);

	/**
	 * Concatenates arrays with each other in order.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayConcat(T[] array1, T[] array2);

	/**
	 * Appends element to array.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayAppend(SqmExpression<T[]> arrayExpression, SqmExpression<T> elementExpression);

	/**
	 * Appends element to array.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayAppend(SqmExpression<T[]> arrayExpression, T element);

	/**
	 * Appends element to array.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayAppend(T[] array, SqmExpression<T> elementExpression);

	/**
	 * Appends element to array.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayAppend(T[] array, T element);

	/**
	 * Prepends element to array.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayPrepend(SqmExpression<T> elementExpression, SqmExpression<T[]> arrayExpression);

	/**
	 * Prepends element to array.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayPrepend(T element, SqmExpression<T[]> arrayExpression);

	/**
	 * Prepends element to array.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayPrepend(SqmExpression<T> elementExpression, T[] array);

	/**
	 * Prepends element to array.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayPrepend(T element, T[] array);

	/**
	 * Whether an array contains an element.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContains(SqmExpression<T[]> arrayExpression, SqmExpression<T> elementExpression);

	/**
	 * Whether an array contains an element.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContains(SqmExpression<T[]> arrayExpression, T element);

	/**
	 * Whether an array contains an element.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContains(T[] array, SqmExpression<T> elementExpression);

	/**
	 * Whether an array contains an element.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContains(T[] array, T element);

	/**
	 * Whether an array contains a nullable element.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsNullable(SqmExpression<T[]> arrayExpression, SqmExpression<T> elementExpression);

	/**
	 * Whether an array contains a nullable element.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsNullable(SqmExpression<T[]> arrayExpression, T element);

	/**
	 * Whether an array contains a nullable element.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsNullable(T[] array, SqmExpression<T> elementExpression);

	/**
	 * Whether an array contains a nullable element.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsNullable(T[] array, T element);

	/**
	 * Whether an array contains another array.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsAll(SqmExpression<T[]> arrayExpression, SqmExpression<T[]> subArrayExpression);

	/**
	 * Whether an array contains another array.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsAll(SqmExpression<T[]> arrayExpression, T[] subArray);

	/**
	 * Whether an array contains another array.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsAll(T[] array, SqmExpression<T[]> subArrayExpression);

	/**
	 * Whether an array contains another array.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsAll(T[] array, T[] subArray);

	/**
	 * Whether an array contains another array with nullable elements.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsAllNullable(SqmExpression<T[]> arrayExpression, SqmExpression<T[]> subArrayExpression);

	/**
	 * Whether an array contains another array with nullable elements.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsAllNullable(SqmExpression<T[]> arrayExpression, T[] subArray);

	/**
	 * Whether an array contains another array with nullable elements.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsAllNullable(T[] array, SqmExpression<T[]> subArrayExpression);

	/**
	 * Whether an array contains another array with nullable elements.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayContainsAllNullable(T[] array, T[] subArray);

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayOverlaps(SqmExpression<T[]> arrayExpression1, SqmExpression<T[]> arrayExpression2);

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayOverlaps(SqmExpression<T[]> arrayExpression1, T[] array2);

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayOverlaps(T[] array1, SqmExpression<T[]> arrayExpression2);

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayOverlaps(T[] array1, T[] array2);

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayOverlapsNullable(SqmExpression<T[]> arrayExpression1, SqmExpression<T[]> arrayExpression2);

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayOverlapsNullable(SqmExpression<T[]> arrayExpression1, T[] array2);

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayOverlapsNullable(T[] array1, SqmExpression<T[]> arrayExpression2);

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.4
	 */
	<T> SqmPredicate arrayOverlapsNullable(T[] array1, T[] array2);

	/**
	 * Accesses the element of an array by 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T> arrayGet(SqmExpression<T[]> arrayExpression, SqmExpression<Integer> indexExpression);

	/**
	 * Accesses the element of an array by 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T> arrayGet(SqmExpression<T[]> arrayExpression, Integer index);

	/**
	 * Accesses the element of an array by 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T> arrayGet(T[] array, SqmExpression<Integer> indexExpression);

	/**
	 * Accesses the element of an array by 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T> arrayGet(T[] array, Integer index);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySet(SqmExpression<T[]> arrayExpression, SqmExpression<Integer> indexExpression, SqmExpression<T> elementExpression);
	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySet(SqmExpression<T[]> arrayExpression, SqmExpression<Integer> indexExpression, T element);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySet(SqmExpression<T[]> arrayExpression, Integer index, SqmExpression<T> elementExpression);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySet(SqmExpression<T[]> arrayExpression, Integer index, T element);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySet(T[] array, SqmExpression<Integer> indexExpression, SqmExpression<T> elementExpression);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySet(T[] array, SqmExpression<Integer> indexExpression, T element);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySet(T[] array, Integer index, SqmExpression<T> elementExpression);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySet(T[] array, Integer index, T element);

	/**
	 * Creates array copy with given element removed.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayRemove(SqmExpression<T[]> arrayExpression, SqmExpression<T> elementExpression);

	/**
	 * Creates array copy with given element removed.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayRemove(SqmExpression<T[]> arrayExpression, T element);

	/**
	 * Creates array copy with given element removed.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayRemove(T[] array, SqmExpression<T> elementExpression);

	/**
	 * Creates array copy with given element removed.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayRemove(T[] array, T element);

	/**
	 * Creates array copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayRemoveIndex(SqmExpression<T[]> arrayExpression, SqmExpression<Integer> indexExpression);

	/**
	 * Creates array copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayRemoveIndex(SqmExpression<T[]> arrayExpression, Integer index);

	/**
	 * Creates array copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayRemoveIndex(T[] array, SqmExpression<Integer> indexExpression);

	/**
	 * Creates array copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayRemoveIndex(T[] array, Integer index);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySlice(SqmExpression<T[]> arrayExpression, SqmExpression<Integer> lowerIndexExpression, SqmExpression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySlice(SqmExpression<T[]> arrayExpression, SqmExpression<Integer> lowerIndexExpression, Integer upperIndex);
	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySlice(SqmExpression<T[]> arrayExpression, Integer lowerIndex, SqmExpression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySlice(SqmExpression<T[]> arrayExpression, Integer lowerIndex, Integer upperIndex);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySlice(T[] array, SqmExpression<Integer> lowerIndexExpression, SqmExpression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySlice(T[] array, SqmExpression<Integer> lowerIndexExpression, Integer upperIndex);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySlice(T[] array, Integer lowerIndex, SqmExpression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arraySlice(T[] array, Integer lowerIndex, Integer upperIndex);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayReplace(SqmExpression<T[]> arrayExpression, SqmExpression<T> oldElementExpression, SqmExpression<T> newElementExpression);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayReplace(SqmExpression<T[]> arrayExpression, SqmExpression<T> oldElementExpression, T newElement);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayReplace(SqmExpression<T[]> arrayExpression, T oldElement, SqmExpression<T> newElementExpression);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayReplace(SqmExpression<T[]> arrayExpression, T oldElement, T newElement);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayReplace(T[] array, SqmExpression<T> oldElementExpression, SqmExpression<T> newElementExpression);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayReplace(T[] array, SqmExpression<T> oldElementExpression, T newElement);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayReplace(T[] array, T oldElement, SqmExpression<T> newElementExpression);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayReplace(T[] array, T oldElement, T newElement);

	/**
	 * Creates array copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayTrim(SqmExpression<T[]> arrayExpression, SqmExpression<Integer> elementCountExpression);

	/**
	 * Creates array copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayTrim(SqmExpression<T[]> arrayExpression, Integer elementCount);

	/**
	 * Creates array copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayTrim(T[] array, SqmExpression<Integer> elementCountExpression);

	/**
	 * Creates array copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayTrim(T[] array, Integer elementCount);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayFill(SqmExpression<T> elementExpression, SqmExpression<Integer> elementCountExpression);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayFill(SqmExpression<T> elementExpression, Integer elementCount);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayFill(T element, SqmExpression<Integer> elementCountExpression);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<T[]> arrayFill(T element, Integer elementCount);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Array functions for collection types

	/**
	 * Creates a basic collection literal with the {@code array} constructor function.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionLiteral(E... elements);

	/**
	 * Determines the 1-based position of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	<E> SqmExpression<Integer> collectionPosition(SqmExpression<? extends Collection<? extends E>> collectionExpression, E element);

	/**
	 * Determines the 1-based position of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	<E> SqmExpression<Integer> collectionPosition(SqmExpression<? extends Collection<? extends E>> collectionExpression, SqmExpression<E> elementExpression);

	/**
	 * Determines the length of a basic collection.
	 *
	 * @since 6.4
	 */
	SqmExpression<Integer> collectionLength(SqmExpression<? extends Collection<?>> collectionExpression);

	/**
	 * Concatenates basic collections with each other in order.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(SqmExpression<C> collectionExpression1, SqmExpression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Concatenates basic collections with each other in order.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(SqmExpression<C> collectionExpression1, Collection<? extends E> collection2);

	/**
	 * Concatenates basic collections with each other in order.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(C collection1, SqmExpression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Concatenates basic collections with each other in order.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(C collection1, Collection<? extends E> collection2);

	/**
	 * Appends element to basic collection.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(SqmExpression<C> collectionExpression, SqmExpression<? extends E> elementExpression);

	/**
	 * Appends element to basic collection.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(SqmExpression<C> collectionExpression, E element);

	/**
	 * Appends element to basic collection.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(C collection, SqmExpression<? extends E> elementExpression);

	/**
	 * Appends element to basic collection.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(C collection, E element);

	/**
	 * Prepends element to basic collection.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(SqmExpression<? extends E> elementExpression, SqmExpression<C> collectionExpression);

	/**
	 * Prepends element to basic collection.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(E element, SqmExpression<C> collectionExpression);

	/**
	 * Prepends element to basic collection.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(SqmExpression<? extends E> elementExpression, C collection);

	/**
	 * Prepends element to basic collection.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(E element, C collection);

	/**
	 * Whether a basic collection contains an element.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContains(SqmExpression<? extends Collection<? super E>> collectionExpression, SqmExpression<? extends E> elementExpression);

	/**
	 * Whether a basic collection contains an element.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContains(SqmExpression<? extends Collection<? super E>> collectionExpression, E element);

	/**
	 * Whether a basic collection contains an element.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContains(Collection<? super E> collection, SqmExpression<? extends E> elementExpression);

	/**
	 * Whether a basic collection contains an element.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContains(Collection<? super E> collection, E element);

	/**
	 * Whether a basic collection contains a nullable element.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsNullable(SqmExpression<? extends Collection<? super E>> collectionExpression, SqmExpression<? extends E> elementExpression);

	/**
	 * Whether a basic collection contains a nullable element.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsNullable(SqmExpression<? extends Collection<? super E>> collectionExpression, E element);

	/**
	 * Whether a basic collection contains a nullable element.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsNullable(Collection<? super E> collection, SqmExpression<? extends E> elementExpression);

	/**
	 * Whether a basic collection contains a nullable element.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsNullable(Collection<? super E> collection, E element);

	/**
	 * Whether a basic collection contains another basic collection.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsAll(SqmExpression<? extends Collection<? extends E>> collectionExpression, SqmExpression<? extends Collection<? extends E>> subCollectionExpression);

	/**
	 * Whether a basic collection contains another basic collection.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsAll(SqmExpression<? extends Collection<? extends E>> collectionExpression, Collection<? extends E> subCollection);

	/**
	 * Whether a basic collection contains another basic collection.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsAll(Collection<? extends E> collection, SqmExpression<? extends Collection<? extends E>> subArrayExpression);

	/**
	 * Whether a basic collection contains another basic collection.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsAll(Collection<? extends E> collection, Collection<? extends E> subCollection);

	/**
	 * Whether a basic collection contains another basic collection with nullable elements.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsAllNullable(SqmExpression<? extends Collection<? extends E>> collectionExpression, SqmExpression<? extends Collection<? extends E>> subCollectionExpression);

	/**
	 * Whether a basic collection contains another basic collection with nullable elements.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsAllNullable(SqmExpression<? extends Collection<? extends E>> collectionExpression, Collection<? extends E> subCollection);

	/**
	 * Whether a basic collection contains another basic collection with nullable elements.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsAllNullable(Collection<? extends E> collection, SqmExpression<? extends Collection<? extends E>> subCollectionExpression);

	/**
	 * Whether a basic collection contains another basic collection with nullable elements.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionContainsAllNullable(Collection<? extends E> collection, Collection<? extends E> subCollection);

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionOverlaps(SqmExpression<? extends Collection<? extends E>> collectionExpression1, SqmExpression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionOverlaps(SqmExpression<? extends Collection<? extends E>> collectionExpression1, Collection<? extends E> collection2);

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionOverlaps(Collection<? extends E> collection1, SqmExpression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionOverlaps(Collection<? extends E> collection1, Collection<? extends E> collection2);

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionOverlapsNullable(SqmExpression<? extends Collection<? extends E>> collectionExpression1, SqmExpression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionOverlapsNullable(SqmExpression<? extends Collection<? extends E>> collectionExpression1, Collection<? extends E> collection2);

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionOverlapsNullable(Collection<? extends E> collection1, SqmExpression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.4
	 */
	<E> SqmPredicate collectionOverlapsNullable(Collection<? extends E> collection1, Collection<? extends E> collection2);

	/**
	 * Accesses the element of the basic collection by 1-based index.
	 *
	 * @since 6.4
	 */
	<E> SqmExpression<E> collectionGet(SqmExpression<? extends Collection<E>> collectionExpression, SqmExpression<Integer> indexExpression);

	/**
	 * Accesses the element of the basic collection by 1-based index.
	 *
	 * @since 6.4
	 */
	<E> SqmExpression<E> collectionGet(SqmExpression<? extends Collection<E>> collectionExpression, Integer index);

	/**
	 * Accesses the element of the basic collection by 1-based index.
	 *
	 * @since 6.4
	 */
	<E> SqmExpression<E> collectionGet(Collection<E> collection, SqmExpression<Integer> indexExpression);

	/**
	 * Accesses the element of the basic collection by 1-based index.
	 *
	 * @since 6.4
	 */
	<E> SqmExpression<E> collectionGet(Collection<E> collection, Integer index);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(SqmExpression<C> collectionExpression, SqmExpression<Integer> indexExpression, SqmExpression<? extends E> elementExpression);
	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(SqmExpression<C> collectionExpression, SqmExpression<Integer> indexExpression, E element);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(SqmExpression<C> collectionExpression, Integer index, SqmExpression<? extends E> elementExpression);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(SqmExpression<C> collectionExpression, Integer index, E element);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(C collection, SqmExpression<Integer> indexExpression, SqmExpression<? extends E> elementExpression);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(C collection, SqmExpression<Integer> indexExpression, E element);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(C collection, Integer index, SqmExpression<? extends E> elementExpression);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(C collection, Integer index, E element);

	/**
	 * Creates basic collection copy with given element removed.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(SqmExpression<C> collectionExpression, SqmExpression<? extends E> elementExpression);

	/**
	 * Creates basic collection copy with given element removed.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(SqmExpression<C> collectionExpression, E element);

	/**
	 * Creates basic collection copy with given element removed.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(C collection, SqmExpression<? extends E> elementExpression);

	/**
	 * Creates basic collection copy with given element removed.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(C collection, E element);

	/**
	 * Creates basic collection copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(SqmExpression<C> collectionExpression, SqmExpression<Integer> indexExpression);

	/**
	 * Creates basic collection copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(SqmExpression<C> collectionExpression, Integer index);

	/**
	 * Creates basic collection copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(C collection, SqmExpression<Integer> indexExpression);

	/**
	 * Creates basic collection copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(C collection, Integer index);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionSlice(SqmExpression<C> collectionExpression, SqmExpression<Integer> lowerIndexExpression, SqmExpression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionSlice(SqmExpression<C> collectionExpression, SqmExpression<Integer> lowerIndexExpression, Integer upperIndex);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionSlice(SqmExpression<C> collectionExpression, Integer lowerIndex, SqmExpression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionSlice(SqmExpression<C> collectionExpression, Integer lowerIndex, Integer upperIndex);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionSlice(C collection, SqmExpression<Integer> lowerIndexExpression, SqmExpression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionSlice(C collection, SqmExpression<Integer> lowerIndexExpression, Integer upperIndex);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionSlice(C collection, Integer lowerIndex, SqmExpression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionSlice(C collection, Integer lowerIndex, Integer upperIndex);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(SqmExpression<C> collectionExpression, SqmExpression<? extends E> oldElementExpression, SqmExpression<? extends E> newElementExpression);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(SqmExpression<C> collectionExpression, SqmExpression<? extends E> oldElementExpression, E newElement);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(SqmExpression<C> collectionExpression, E oldElement, SqmExpression<? extends E> newElementExpression);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(SqmExpression<C> collectionExpression, E oldElement, E newElement);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(C collection, SqmExpression<? extends E> oldElementExpression, SqmExpression<? extends E> newElementExpression);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(C collection, SqmExpression<? extends E> oldElementExpression, E newElement);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(C collection, E oldElement, SqmExpression<? extends E> newElementExpression);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(C collection, E oldElement, E newElement);

	/**
	 * Creates basic collection copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionTrim(SqmExpression<C> arrayExpression, SqmExpression<Integer> elementCountExpression);

	/**
	 * Creates basic collection copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionTrim(SqmExpression<C> arrayExpression, Integer elementCount);

	/**
	 * Creates basic collection copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionTrim(C collection, SqmExpression<Integer> elementCountExpression);

	/**
	 * Creates basic collection copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	<C extends Collection<?>> SqmExpression<C> collectionTrim(C collection, Integer elementCount);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<Collection<T>> collectionFill(SqmExpression<T> elementExpression, SqmExpression<Integer> elementCountExpression);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<Collection<T>> collectionFill(SqmExpression<T> elementExpression, Integer elementCount);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<Collection<T>> collectionFill(T element, SqmExpression<Integer> elementCountExpression);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	<T> SqmExpression<Collection<T>> collectionFill(T element, Integer elementCount);

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
	<V, C extends Collection<V>> SqmExpression<Collection<V>> values(C collection);

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
