package org.hibernate.query.sqm.spi;

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

import jakarta.annotation.Nullable;
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
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.tree.spi.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.spi.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.spi.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmFunction;
import org.hibernate.query.sqm.tree.spi.expression.SqmJsonExistsExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmJsonQueryExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmJsonTableFunction;
import org.hibernate.query.sqm.tree.spi.expression.SqmJsonValueExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmModifiedSubQueryExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.spi.expression.SqmXmlElementExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmXmlTableFunction;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.query.sqm.tree.spi.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.spi.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.spi.insert.SqmValues;
import org.hibernate.query.sqm.tree.spi.predicate.SqmInPredicate;
import org.hibernate.query.sqm.tree.spi.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.spi.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.spi.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.spi.update.SqmUpdateStatement;
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

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nonnull Expression<? extends T> argument);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<? extends T> argument);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<? extends T> argument);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayAgg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<? extends T> argument);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayLiteral(@Nullable T... elements);

	@Nonnull
	@Override
	<T> SqmExpression<Integer> arrayLength(@Nonnull Expression<T[]> arrayExpression);

	@Nonnull
	@Override
	<T> SqmExpression<Integer> arrayPosition(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	@Nonnull
	@Override
	<T> SqmExpression<Integer> arrayPosition(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<int[]> arrayPositions(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<int[]> arrayPositions(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	@Nonnull
	@Override
	<T> SqmExpression<List<Integer>> arrayPositionsList(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<List<Integer>> arrayPositionsList(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayConcat(@Nonnull Expression<T[]> arrayExpression1, @Nonnull Expression<T[]> arrayExpression2);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayConcat(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayConcat(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayAppend(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayAppend(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayPrepend(@Nonnull Expression<T> elementExpression, @Nonnull Expression<T[]> arrayExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayPrepend(@Nullable T element, @Nonnull Expression<T[]> arrayExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T> arrayGet(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> indexExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T> arrayGet(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySet(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> indexExpression, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySet(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> indexExpression, @Nullable T element);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySet(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySet(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index, @Nullable T element);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayRemove(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayRemove(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayRemoveIndex(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> indexExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayRemoveIndex(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySlice(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> lowerIndexExpression, @Nonnull Expression<Integer> upperIndexExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySlice(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> lowerIndexExpression, @Nullable Integer upperIndex);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySlice(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer lowerIndex, @Nonnull Expression<Integer> upperIndexExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySlice(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer lowerIndex, @Nullable Integer upperIndex);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayReplace(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> oldElementExpression, @Nonnull Expression<T> newElementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayReplace(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> oldElementExpression, @Nullable T newElement);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayReplace(@Nonnull Expression<T[]> arrayExpression, @Nullable T oldElement, @Nonnull Expression<T> newElementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayReplace(@Nonnull Expression<T[]> arrayExpression, @Nullable T oldElement, @Nullable T newElement);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayTrim(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> elementCountExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayTrim(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer elementCount);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayReverse(@Nonnull Expression<T[]> arrayExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, boolean descending);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Boolean> descendingExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, boolean descending, boolean nullsFirst);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Boolean> descendingExpression, @Nonnull Expression<Boolean> nullsFirstExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayFill(@Nonnull Expression<T> elementExpression, @Nonnull Expression<Integer> elementCountExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayFill(@Nonnull Expression<T> elementExpression, @Nullable Integer elementCount);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayFill(@Nullable T element, @Nonnull Expression<Integer> elementCountExpression);

	@Nonnull
	@Override
	<T> SqmExpression<T[]> arrayFill(@Nullable T element, @Nullable Integer elementCount);

	@Nonnull
	@Override
	SqmExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nonnull Expression<String> separatorExpression);

	@Nonnull
	@Override
	SqmExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nullable String separator);

	@Nonnull
	@Override
	SqmExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nonnull Expression<String> separatorExpression, @Nonnull Expression<String> defaultExpression);

	@Nonnull
	@Override
	SqmExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nonnull Expression<String> separatorExpression, @Nullable String defaultValue);

	@Nonnull
	@Override
	SqmExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nullable String separator, @Nonnull Expression<String> defaultExpression);

	@Nonnull
	@Override
	SqmExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nullable String separator, @Nullable String defaultValue);

	@Nonnull
	@Override
	<T> SqmPredicate arrayContains(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmPredicate arrayContains(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	@Nonnull
	@Override
	<T> SqmPredicate arrayContains(@Nullable T[] array, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmPredicate arrayContainsNullable(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmPredicate arrayContainsNullable(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	@Nonnull
	@Override
	<T> SqmPredicate arrayContainsNullable(@Nullable T[] array, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIncludes(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T[]> subArrayExpression);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIncludes(@Nonnull Expression<T[]> arrayExpression, @Nullable T[] subArray);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIncludes(@Nullable T[] array, @Nonnull Expression<T[]> subArrayExpression);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIncludesNullable(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T[]> subArrayExpression);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIncludesNullable(@Nonnull Expression<T[]> arrayExpression, @Nullable T[] subArray);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIncludesNullable(@Nullable T[] array, @Nonnull Expression<T[]> subArrayExpression);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIntersects(@Nonnull Expression<T[]> arrayExpression1, @Nonnull Expression<T[]> arrayExpression2);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIntersects(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIntersects(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIntersectsNullable(@Nonnull Expression<T[]> arrayExpression1, @Nonnull Expression<T[]> arrayExpression2);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIntersectsNullable(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2);

	@Nonnull
	@Override
	<T> SqmPredicate arrayIntersectsNullable(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Array functions for collection types

	@Nonnull
	@Override
	<E, C extends Collection<E>> SqmExpression<C> collectionLiteral(@Nullable E... elements);

	@Nonnull
	@Override
	SqmExpression<Integer> collectionLength(@Nonnull Expression<? extends Collection<?>> collectionExpression);

	@Nonnull
	@Override
	<E> SqmExpression<Integer> collectionPosition(@Nonnull Expression<? extends Collection<? extends E>> collectionExpression, @Nullable E element);

	@Nonnull
	@Override
	<E> SqmExpression<Integer> collectionPosition(@Nonnull Expression<? extends Collection<? extends E>> collectionExpression, @Nonnull Expression<E> elementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<int[]> collectionPositions(@Nonnull Expression<? extends Collection<? super T>> collectionExpression, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<int[]> collectionPositions(@Nonnull Expression<? extends Collection<? super T>> collectionExpression, @Nullable T element);

	@Nonnull
	@Override
	<T> SqmExpression<List<Integer>> collectionPositionsList(@Nonnull Expression<? extends Collection<? super T>> collectionExpression, @Nonnull Expression<T> elementExpression);

	@Nonnull
	@Override
	<T> SqmExpression<List<Integer>> collectionPositionsList(@Nonnull Expression<? extends Collection<? super T>> collectionExpression, @Nullable T element);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(@Nonnull Expression<C> collectionExpression1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(@Nonnull Expression<C> collectionExpression1, @Nullable Collection<? extends E> collection2);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionConcat(@Nullable C collection1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<? extends E> elementExpression);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionAppend(@Nonnull Expression<C> collectionExpression, @Nullable E element);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(@Nonnull Expression<? extends E> elementExpression, @Nonnull Expression<C> collectionExpression);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionPrepend(@Nullable E element, @Nonnull Expression<C> collectionExpression);

	@Nonnull
	@Override
	<E> SqmExpression<E> collectionGet(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nonnull Expression<Integer> indexExpression);

	@Nonnull
	@Override
	<E> SqmExpression<E> collectionGet(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable Integer index);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<Integer> indexExpression, @Nonnull Expression<? extends E> elementExpression);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<Integer> indexExpression, @Nullable E element);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(@Nonnull Expression<C> collectionExpression, @Nullable Integer index, @Nonnull Expression<? extends E> elementExpression);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionSet(@Nonnull Expression<C> collectionExpression, @Nullable Integer index, @Nullable E element);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<? extends E> elementExpression);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionRemove(@Nonnull Expression<C> collectionExpression, @Nullable E element);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<Integer> indexExpression);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionRemoveIndex(@Nonnull Expression<C> collectionExpression, @Nullable Integer index);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSlice(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<Integer> lowerIndexExpression, @Nonnull Expression<Integer> upperIndexExpression);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSlice(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<Integer> lowerIndexExpression, @Nullable Integer upperIndex);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSlice(@Nonnull Expression<C> collectionExpression, @Nullable Integer lowerIndex, @Nonnull Expression<Integer> upperIndexExpression);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSlice(@Nonnull Expression<C> collectionExpression, @Nullable Integer lowerIndex, @Nullable Integer upperIndex);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<? extends E> oldElementExpression, @Nonnull Expression<? extends E> newElementExpression);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<? extends E> oldElementExpression, @Nullable E newElement);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(@Nonnull Expression<C> collectionExpression, @Nullable E oldElement, @Nonnull Expression<? extends E> newElementExpression);

	@Nonnull
	@Override
	<E, C extends Collection<? super E>> SqmExpression<C> collectionReplace(@Nonnull Expression<C> collectionExpression, @Nullable E oldElement, @Nullable E newElement);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionTrim(@Nonnull Expression<C> arrayExpression, @Nonnull Expression<Integer> elementCountExpression);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionTrim(@Nonnull Expression<C> arrayExpression, @Nullable Integer elementCount);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionReverse(@Nonnull Expression<C> collectionExpression);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSort(@Nonnull Expression<C> collectionExpression);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSort(@Nonnull Expression<C> collectionExpression, boolean descending);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Boolean> descendingExpression);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			boolean descending,
			boolean nullsFirst);

	@Nonnull
	@Override
	<C extends Collection<?>> SqmExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Boolean> descendingExpression,
			@Nonnull Expression<Boolean> nullsFirstExpression);

	@Nonnull
	@Override
	<T> SqmExpression<Collection<T>> collectionFill(@Nonnull Expression<T> elementExpression, @Nonnull Expression<Integer> elementCountExpression);

	@Nonnull
	@Override
	<T> SqmExpression<Collection<T>> collectionFill(@Nonnull Expression<T> elementExpression, @Nullable Integer elementCount);

	@Nonnull
	@Override
	<T> SqmExpression<Collection<T>> collectionFill(@Nullable T element, @Nonnull Expression<Integer> elementCountExpression);

	@Nonnull
	@Override
	<T> SqmExpression<Collection<T>> collectionFill(@Nullable T element, @Nullable Integer elementCount);

	@Nonnull
	@Override
	SqmExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nonnull Expression<String> separatorExpression);

	@Nonnull
	@Override
	SqmExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nullable String separator);

	@Nonnull
	@Override
	SqmExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nonnull Expression<String> separatorExpression, @Nonnull Expression<String> defaultExpression);

	@Nonnull
	@Override
	SqmExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nonnull Expression<String> separatorExpression, @Nullable String defaultValue);

	@Nonnull
	@Override
	SqmExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nullable String separator, @Nonnull Expression<String> defaultExpression);

	@Nonnull
	@Override
	SqmExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nullable String separator, @Nullable String defaultValue);

	@Nonnull
	@Override
	<E> SqmPredicate collectionContains(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nonnull Expression<? extends E> elementExpression);

	@Nonnull
	@Override
	<E> SqmPredicate collectionContains(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable E element);

	@Nonnull
	@Override
	<E> SqmPredicate collectionContains(@Nonnull Collection<E> collection, @Nonnull Expression<E> elementExpression);

	@Nonnull
	@Override
	<E> SqmPredicate collectionContainsNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nonnull Expression<? extends E> elementExpression);

	@Nonnull
	@Override
	<E> SqmPredicate collectionContainsNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable E element);

	@Nonnull
	@Override
	<E> SqmPredicate collectionContainsNullable(@Nonnull Collection<E> collection, @Nonnull Expression<E> elementExpression);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIncludes(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIncludes(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable Collection<? extends E> subCollection);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIncludes(@Nullable Collection<E> collection, @Nonnull Expression<? extends Collection<? extends E>> subArrayExpression);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIncludesNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIncludesNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable Collection<? extends E> subCollection);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIncludesNullable(@Nullable Collection<E> collection, @Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIntersects(@Nonnull Expression<? extends Collection<E>> collectionExpression1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIntersects(@Nonnull Expression<? extends Collection<E>> collectionExpression1, @Nullable Collection<? extends E> collection2);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIntersects(@Nullable Collection<E> collection1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIntersectsNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIntersectsNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression1, @Nullable Collection<? extends E> collection2);

	@Nonnull
	@Override
	<E> SqmPredicate collectionIntersectsNullable(@Nullable Collection<E> collection1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	@Nonnull
	@Override
	<T> SqmJsonValueExpression<T> jsonValue(
			@Nonnull Expression<?> jsonDocument,
			@Nonnull Expression<String> jsonPath,
			@Nullable Class<T> returningType);

	@Nonnull
	@Override
	SqmJsonValueExpression<String> jsonValue(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath);

	@Nonnull
	@Override
	<T> SqmJsonValueExpression<T> jsonValue(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Class<T> returningType);

	@Nonnull
	@Override
	SqmJsonValueExpression<String> jsonValue(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath);

	@Nonnull
	@Override
	SqmJsonQueryExpression jsonQuery(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath);

	@Nonnull
	@Override
	SqmJsonQueryExpression jsonQuery(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath);

	@Nonnull
	@Override
	SqmJsonExistsExpression jsonExists(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath);

	@Nonnull
	@Override
	SqmJsonExistsExpression jsonExists(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath);

	@Nonnull
	@Override
	SqmExpression<String> jsonArrayWithNulls(@Nonnull Expression<?>... values);

	@Nonnull
	@Override
	SqmExpression<String> jsonArray(@Nonnull Expression<?>... values);

	@Nonnull
	@Override
	SqmExpression<String> jsonObjectWithNulls(@Nonnull Map<?, ? extends Expression<?>> keyValues);

	@Nonnull
	@Override
	SqmExpression<String> jsonObject(@Nonnull Map<?, ? extends Expression<?>> keyValues);

	@Nonnull
	@Override
	SqmExpression<String> jsonArrayAgg(@Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nullable Predicate filter, @Nonnull JpaOrder... orderBy);

	@Nonnull
	@Override
	SqmExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nullable Predicate filter);

	@Nonnull
	@Override
	SqmExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nonnull JpaOrder... orderBy);

	@Nonnull
	@Override
	SqmExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nullable Predicate filter, @Nonnull JpaOrder... orderBy);

	@Nonnull
	@Override
	SqmExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nullable Predicate filter);

	@Nonnull
	@Override
	SqmExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nonnull JpaOrder... orderBy);

	@Nonnull
	@Override
	SqmExpression<String> jsonObjectAggWithUniqueKeysAndNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonObjectAggWithUniqueKeys(@Nonnull Expression<?> key, @Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonObjectAggWithNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonObjectAgg(@Nonnull Expression<?> key, @Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonObjectAggWithUniqueKeysAndNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter);

	@Nonnull
	@Override
	SqmExpression<String> jsonObjectAggWithUniqueKeys(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter);

	@Nonnull
	@Override
	SqmExpression<String> jsonObjectAggWithNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter);

	@Nonnull
	@Override
	SqmExpression<String> jsonObjectAgg(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter);

	@Nonnull
	@Override
	SqmExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value);

	@Nonnull
	@Override
	SqmExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value);

	@Nonnull
	@Override
	SqmExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonRemove(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath);

	@Nonnull
	@Override
	SqmExpression<String> jsonRemove(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath);

	@Nonnull
	@Override
	SqmExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value);

	@Nonnull
	@Override
	SqmExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value);

	@Nonnull
	@Override
	SqmExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value);

	@Nonnull
	@Override
	SqmExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value);

	@Nonnull
	@Override
	SqmExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value);

	@Nonnull
	@Override
	SqmExpression<String> jsonMergepatch(@Nullable String document, @Nonnull Expression<?> patch);

	@Nonnull
	@Override
	SqmExpression<String> jsonMergepatch(@Nonnull Expression<?> document, @Nullable String patch);

	@Nonnull
	@Override
	SqmExpression<String> jsonMergepatch(@Nonnull Expression<?> document, @Nonnull Expression<?> patch);

	@Nonnull
	@Override
	SqmXmlElementExpression xmlelement(@Nonnull String elementName);

	@Nonnull
	@Override
	SqmExpression<String> xmlcomment(@Nullable String comment);

	@Nonnull
	@Override
	<T> SqmExpression<T> named(@Nonnull Expression<T> expression, @Nonnull String name);

	@Nonnull
	@Override
	SqmExpression<String> xmlforest(@Nonnull List<? extends Expression<?>> elements);

	@Nonnull
	@Override
	SqmExpression<String> xmlforest(@Nonnull Expression<?>... elements);

	@Nonnull
	@Override
	SqmExpression<String> xmlconcat(@Nonnull Expression<?>... elements);

	@Nonnull
	@Override
	SqmExpression<String> xmlconcat(@Nonnull List<? extends Expression<?>> elements);

	@Nonnull
	@Override
	SqmExpression<String> xmlpi(@Nonnull String elementName);

	@Nonnull
	@Override
	SqmExpression<String> xmlpi(@Nonnull String elementName, @Nonnull Expression<String> content);

	@Nonnull
	@Override
	SqmExpression<String> xmlquery(@Nullable String query, @Nonnull Expression<?> xmlDocument);

	@Nonnull
	@Override
	SqmExpression<String> xmlquery(@Nonnull Expression<String> query, @Nonnull Expression<?> xmlDocument);

	@Nonnull
	@Override
	SqmExpression<Boolean> xmlexists(@Nullable String query, @Nonnull Expression<?> xmlDocument);

	@Nonnull
	@Override
	SqmExpression<Boolean> xmlexists(@Nonnull Expression<String> query, @Nonnull Expression<?> xmlDocument);

	@Nonnull
	@Override
	SqmExpression<String> xmlagg(@Nullable JpaOrder order, @Nonnull Expression<?> argument);

	@Nonnull
	@Override
	SqmExpression<String> xmlagg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<?> argument);

	@Nonnull
	@Override
	SqmExpression<String> xmlagg(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<?> argument);

	@Nonnull
	@Override
	SqmExpression<String> xmlagg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nullable JpaWindow window, @Nonnull Expression<?> argument);

	@Nonnull
	@Override
	<E> SqmSetReturningFunction<E> setReturningFunction(@Nonnull String name, @Nonnull Expression<?>... args);

	@Nonnull
	@Override
	<E> SqmSetReturningFunction<E> unnestArray(@Nonnull Expression<E[]> array);

	@Nonnull
	@Override
	<E> SqmSetReturningFunction<E> unnestCollection(@Nonnull Expression<? extends Collection<E>> collection);

	@Nonnull
	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nonnull Expression<? extends TemporalAmount> step);

	@Nonnull
	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nullable E stop, @Nullable TemporalAmount step);

	@Nonnull
	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nullable TemporalAmount step);

	@Nonnull
	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nullable TemporalAmount step);

	@Nonnull
	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nullable TemporalAmount step);

	@Nonnull
	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nullable E stop, @Nonnull Expression<? extends TemporalAmount> step);

	@Nonnull
	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nonnull Expression<? extends TemporalAmount> step);

	@Nonnull
	@Override
	<E extends Temporal> SqmSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nonnull Expression<? extends TemporalAmount> step);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nonnull Expression<E> step);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop, @Nullable E step);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop, @Nonnull Expression<E> step);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nullable E step);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nullable E step);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nullable E step);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nonnull Expression<E> step);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nonnull Expression<E> step);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop);

	@Nonnull
	@Override
	<E extends Number> SqmSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop);

	@Nonnull
	@Override
	SqmJsonTableFunction<?> jsonTable(@Nonnull Expression<?> jsonDocument);

	@Nonnull
	@Override
	SqmJsonTableFunction<?> jsonTable(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath);

	@Nonnull
	@Override
	SqmJsonTableFunction<?> jsonTable(@Nonnull Expression<?> jsonDocument, @Nullable Expression<String> jsonPath);

	@Nonnull
	@Override
	SqmXmlTableFunction<?> xmlTable(@Nullable String xpath, @Nonnull Expression<?> xmlDocument);

	@Nonnull
	@Override
	SqmXmlTableFunction<?> xmlTable(@Nonnull Expression<String> xpath, @Nonnull Expression<?> xmlDocument);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Nonnull
	@Override
	SqmSelectStatement<Object> createQuery();

	@Nonnull
	@Override
	<T> SqmSelectStatement<T> createQuery(@Nonnull Class<T> resultClass);

	@Nonnull
	@Override
	<T> SqmSelectStatement<T> createQuery(@Nonnull String hql, @Nonnull Class<T> resultClass);

	@Nonnull
	@Override
	SqmSelectStatement<Tuple> createTupleQuery();

	@Nonnull
	@Override
	<Y> JpaCompoundSelection<Y> construct(@Nonnull Class<Y> resultClass, @Nonnull Selection<?>... selections);

	@Nonnull
	@Override
	<Y> JpaCompoundSelection<Y> construct(@Nonnull Class<Y> resultClass, @Nonnull List<? extends Selection<?>> arguments);

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

	@Nonnull
	@Override
	<T> SqmInsertValuesStatement<T> createCriteriaInsertValues(@Nonnull Class<T> targetEntity);

	@Nonnull
	@Override
	<T> SqmInsertSelectStatement<T> createCriteriaInsertSelect(@Nonnull Class<T> targetEntity);

	@Nonnull
	@Override
	SqmValues values(@Nonnull Expression<?>... expressions);

	@Nonnull
	@Override
	SqmValues values(@Nonnull List<? extends Expression<?>> expressions);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> abs(@Nonnull Expression<N> x);

	@Nonnull
	@Override
	<X, T> SqmExpression<X> cast(@Nonnull JpaExpression<T> expression, @Nonnull Class<X> castTargetJavaType);

	@Nonnull
	@Override
	<X, T> SqmExpression<X> cast(@Nonnull JpaExpression<T> expression, @Nonnull JpaCastTarget<X> castTarget);

	@Nonnull
	@Override
	<X> SqmCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType);

	@Nonnull
	@Override
	<X> SqmCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType, long length);

	@Nonnull
	@Override
	<X> SqmCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType, int precision, int scale);

	@Nonnull
	@Override
	SqmPredicate wrap(@Nonnull Expression<Boolean> expression);

	@Nonnull
	@Override @SuppressWarnings("unchecked")
	SqmPredicate wrap(@Nonnull Expression<Boolean>... expressions);

	@Nonnull
	@Override
	SqmPredicate wrap(@Nonnull BooleanExpression... expressions);

	SqmPredicate wrap(List<? extends Expression<Boolean>> restrictions);

	@Nonnull
	@Override
	SqmExpression<?> fk(@Nonnull Path<?> path);

	@Nonnull
	@Override
	SqmExpression<?> id(@Nonnull Path<?> path);

	@Nonnull
	@Override
	SqmExpression<?> version(@Nonnull Path<?> path);

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
	<X, T, V extends T> SqmJoin<X, V> treat(@Nonnull Join<X, T> join, @Nonnull Class<V> type);

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

	@Nonnull
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
	<N extends Number> SqmExpression<N> sum(@Nonnull Expression<? extends N> x, @Nullable N y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> sum(@Nullable N x, @Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> prod(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> prod(@Nonnull Expression<? extends N> x, @Nullable N y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> prod(@Nullable N x, @Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> diff(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> diff(@Nonnull Expression<? extends N> x, @Nullable N y);

	@Nonnull
	@Override
	<N extends Number> SqmExpression<N> diff(@Nullable N x, @Nonnull Expression<? extends N> y);

	@Nonnull
	@Override
	SqmExpression<Number> quot(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmExpression<Number> quot(@Nonnull Expression<? extends Number> x, @Nullable Number y);

	@Nonnull
	@Override
	SqmExpression<Number> quot(@Nullable Number x, @Nonnull Expression<? extends Number> y);

	SqmExpression<Number> quotPortable(Expression<? extends Number> x, Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmExpression<Integer> mod(@Nonnull Expression<Integer> x, @Nonnull Expression<Integer> y);

	@Nonnull
	@Override
	SqmExpression<Integer> mod(@Nonnull Expression<Integer> x, @Nullable Integer y);

	@Nonnull
	@Override
	SqmExpression<Integer> mod(@Nullable Integer x, @Nonnull Expression<Integer> y);

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

	@Nonnull
	@Override
	<T> List<? extends SqmExpression<T>> literals(@Nonnull T[] values);

	@Nonnull
	@Override
	<T> List<? extends SqmExpression<T>> literals(@Nonnull List<T> values);

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

	@Nonnull
	@Override
	SqmExpression<String> concat(@Nullable String x, @Nullable String y);

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

	@Nonnull
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

	@Nonnull
	@Override
	<K, L extends List<?>> SqmExpression<Set<K>> indexes(@Nonnull L list);

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
	<Y> JpaCoalesce<Y> coalesce(@Nonnull Expression<? extends Y> x, @Nullable Y y);

	@Nonnull
	@Override
	<Y> SqmExpression<Y> nullif(@Nonnull Expression<Y> x, @Nonnull Expression<?> y);

	@Nonnull
	@Override
	<Y> SqmExpression<Y> nullif(@Nonnull Expression<Y> x, @Nullable Y y);

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
	SqmPredicate equal(@Nonnull Expression<?> x, @Nullable Object y);

	@Nonnull
	@Override
	SqmPredicate notEqual(@Nonnull Expression<?> x, @Nonnull Expression<?> y);

	@Nonnull
	@Override
	SqmPredicate notEqual(@Nonnull Expression<?> x, @Nullable Object y);

	@Nonnull
	@Override
	SqmPredicate distinctFrom(@Nonnull Expression<?> x, @Nonnull Expression<?> y);

	@Nonnull
	@Override
	SqmPredicate distinctFrom(@Nonnull Expression<?> x, @Nullable Object y);

	@Nonnull
	@Override
	SqmPredicate notDistinctFrom(@Nonnull Expression<?> x, @Nonnull Expression<?> y);

	@Nonnull
	@Override
	SqmPredicate notDistinctFrom(@Nonnull Expression<?> x, @Nullable Object y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThan(@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThan(@Nonnull Expression<? extends Y> x, @Nullable Y y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(@Nonnull Expression<? extends Y> x, @Nullable Y y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThan(@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThan(@Nonnull Expression<? extends Y> x, @Nullable Y y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(@Nonnull Expression<? extends Y> x, @Nullable Y y);

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
	<Y extends Comparable<? super Y>> SqmPredicate between(@Nonnull Expression<? extends Y> value, @Nullable Y lower, @Nullable Y upper);

	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> SqmPredicate between(
			@Nullable Y value,
			@Nonnull Expression<? extends Y> lower,
			@Nonnull Expression<? extends Y> upper);

	@Nonnull
	@Override
	SqmPredicate gt(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate gt(@Nonnull Expression<? extends Number> x, @Nullable Number y);

	@Nonnull
	@Override
	SqmPredicate ge(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate ge(@Nonnull Expression<? extends Number> x, @Nullable Number y);

	@Nonnull
	@Override
	SqmPredicate lt(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate lt(@Nonnull Expression<? extends Number> x, @Nullable Number y);

	@Nonnull
	@Override
	SqmPredicate le(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	@Nonnull
	@Override
	SqmPredicate le(@Nonnull Expression<? extends Number> x, @Nullable Number y);

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
	<E, C extends Collection<E>> SqmPredicate isMember(@Nullable E elem, @Nonnull Expression<C> collection);

	@Nonnull
	@Override
	<E, C extends Collection<E>> SqmPredicate isNotMember(@Nonnull Expression<E> elem, @Nonnull Expression<C> collection);

	@Nonnull
	@Override
	<E, C extends Collection<E>> SqmPredicate isNotMember(@Nullable E elem, @Nonnull Expression<C> collection);

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

	@Nonnull
	@Override
	<T> SqmInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull Expression<? extends T>... values);

	@Nonnull
	@Override
	<T> SqmInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull T... values);

	@Nonnull
	@Override
	<T> SqmInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull Collection<T> values);

	<T> SqmInPredicate<T> in(Expression<? extends T> expression, SqmSubQuery<T> subQuery);

	@Nonnull
	@Override
	SqmPredicate exists(@Nonnull Subquery<?> subquery);

	@Nonnull
	@Override
	<M extends Map<?, ?>> SqmPredicate isMapEmpty(@Nonnull JpaExpression<M> mapExpression);

	@Nonnull
	@Override
	<M extends Map<?, ?>> SqmPredicate isMapNotEmpty(@Nonnull JpaExpression<M> mapExpression);

	@Nonnull
	@Override
	<M extends Map<?,?>> SqmExpression<Integer> mapSize(@Nonnull JpaExpression<M> mapExpression);

	@Nonnull
	@Override
	<M extends Map<?, ?>> SqmExpression<Integer> mapSize(@Nonnull M map);

	@Nonnull
	@Override
	SqmSortSpecification sort(@Nonnull JpaExpression<?> sortExpression, @Nonnull SortDirection sortOrder, @Nonnull Nulls nullPrecedence);

	@Nonnull
	@Override
	SqmSortSpecification sort(
			@Nonnull JpaExpression<?> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence,
			boolean ignoreCase);

	@Nonnull
	@Override
	SqmSortSpecification sort(@Nonnull JpaExpression<?> sortExpression, @Nonnull SortDirection sortOrder);

	@Nonnull
	@Override
	SqmSortSpecification sort(@Nonnull JpaExpression<?> sortExpression);

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
