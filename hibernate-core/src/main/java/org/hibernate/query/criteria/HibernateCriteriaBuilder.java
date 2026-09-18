/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.query.SortDirection;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.common.TemporalUnit;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
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
import jakarta.persistence.criteria.TemporalField;

/**
 * A JPA {@link CriteriaBuilder} is a source of objects which may be composed
 * to express a criteria query. The JPA-standard API defines all the operations
 * needed to express any query written in standard JPQL. This interface extends
 * {@code CriteriaBuilder}, adding operations needed to express features of HQL
 * which are not available in standard JPQL. For example:
 * <ul>
 * <li>JPQL does not have a {@code format()} function, so
 *     {@link #format(Expression, String)} is declared here, and
 * <li>since JPQL does not have {@code insert} statements, this interface
 *     defines the operations {@link #createCriteriaInsertSelect(Class)} and
 *     {@link #createCriteriaInsertValues(Class)}.
 * </ul>
 * <p>
 * Furthermore, the operations of this interface return types defined in the
 * package {@link org.hibernate.query.criteria}, which extend the equivalent
 * types in {@link jakarta.persistence.criteria} with additional operations.
 * For example {@link JpaCriteriaQuery} adds the methods:
 * <ul>
 * <li>{@link JpaCriteriaQuery#from(Subquery)}, which allows the use of a
 *     subquery in the {@code from} clause of the query, and
 * <li>{@link JpaCriteriaQuery#with(AbstractQuery)}, which allows the creation
 *     of {@link JpaCteCriteria common table expressions}.
 * </ul>
 * <p>
 * Finally, the method {@link #createQuery(String, Class)} allows a query
 * written in HQL to be translated to a tree of criteria objects for further
 * manipulation and execution.
 * <p>
 * An instance of this interface may be obtained by calling
 * {@link org.hibernate.SessionFactory#getCriteriaBuilder()}.
 *
 * @see org.hibernate.SessionFactory#getCriteriaBuilder()
 * @see JpaCriteriaQuery
 * @see JpaCriteriaUpdate
 * @see JpaCriteriaDelete
 * @see JpaCriteriaInsertValues
 * @see JpaCriteriaInsertSelect
 * @see JpaCteCriteria
 * @see JpaSubQuery
 * @see JpaExpression
 *
 * @since 6.0
 *
 * @author Steve Ebersole
 * @author Yoobin Yoon
 */
@Incubating(since = "6.3")
public interface HibernateCriteriaBuilder extends CriteriaBuilder {

	/**
	 * Create an expression that casts the given expression to the specified Java type.
	 *
	 * @param expression The expression to cast
	 * @param castTargetJavaType The target Java type
	 * @param <X> The target expression type
	 * @param <T> The source expression type
	 * @return the cast expression
	 */
	@Nonnull
	<X, T> JpaExpression<X> cast(@Nonnull JpaExpression<T> expression, @Nonnull Class<X> castTargetJavaType);

	/**
	 * Create an expression that casts the given expression to the specified cast target.
	 *
	 * @param expression The expression to cast
	 * @param castTarget The cast target descriptor
	 * @param <X> The target expression type
	 * @param <T> The source expression type
	 * @return the cast expression
	 */
	@Nonnull
	<X, T> JpaExpression<X> cast(@Nonnull JpaExpression<T> expression, @Nonnull JpaCastTarget<X> castTarget);

	/**
	 * Create a cast target for the given Java type.
	 *
	 * @param castTargetJavaType The target Java type
	 * @param <X> The target type
	 * @return the cast target descriptor
	 */
	@Nonnull
	<X> JpaCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType);

	/**
	 * Create a cast target for the given Java type and SQL type length.
	 *
	 * @param castTargetJavaType The target Java type
	 * @param length The SQL type length
	 * @param <X> The target type
	 * @return the cast target descriptor
	 */
	@Nonnull
	<X> JpaCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType, long length);

	/**
	 * Create a cast target for the given Java type, precision, and scale.
	 *
	 * @param castTargetJavaType The target Java type
	 * @param precision The SQL type precision
	 * @param scale The SQL type scale
	 * @param <X> The target type
	 * @return the cast target descriptor
	 */
	@Nonnull
	<X> JpaCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType, int precision, int scale);

	/**
	 * Wrap a boolean expression as a predicate.
	 *
	 * @param expression The boolean expression
	 * @return the predicate representing the expression
	 */
	@Nonnull
	JpaPredicate wrap(@Nonnull Expression<Boolean> expression);

	/**
	 * Wrap boolean expressions as predicates and combine them with conjunction.
	 *
	 * @param expressions The boolean expressions
	 * @return the conjunction of the wrapped predicates
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	JpaPredicate wrap(@Nonnull Expression<Boolean>... expressions);

	/**
	 * Wrap boolean expressions as predicates and combine them with conjunction.
	 *
	 * @param expressions The boolean expressions
	 * @return the conjunction of the wrapped predicates
	 */
	@Nonnull
	JpaPredicate wrap(@Nonnull BooleanExpression... expressions);

	/**
	 * Unwrap this criteria builder to a Hibernate criteria builder extension.
	 *
	 * @param clazz The extension type to obtain
	 * @param <T> The extension type
	 * @return the requested extension
	 */
	@Nonnull
	<T extends HibernateCriteriaBuilder> T unwrap(@Nonnull Class<T> clazz);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Criteria creation

	/**
	 * Create a criteria query.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<Object> createQuery();

	/**
	 * Create a criteria query for the given result type.
	 */
	@Nonnull
	@Override
	<T> JpaCriteriaQuery<T> createQuery(@Nonnull Class<T> resultClass);

	/**
	 * Create a tuple-valued criteria query.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<Tuple> createTupleQuery();

	/**
	 * Create a criteria update statement for the given entity type.
	 */
	@Nonnull
	@Override
	<T> JpaCriteriaUpdate<T> createCriteriaUpdate(@Nonnull Class<T> targetEntity);

	/**
	 * Create a criteria delete statement for the given entity type.
	 */
	@Nonnull
	@Override
	<T> JpaCriteriaDelete<T> createCriteriaDelete(@Nonnull Class<T> targetEntity);

	/**
	 * Create a criteria insert statement which supplies explicit values.
	 *
	 * @param targetEntity The target entity type
	 * @param <T> The target entity type
	 * @return a new insert-values criteria statement
	 */
	@Nonnull
	<T> JpaCriteriaInsertValues<T> createCriteriaInsertValues(@Nonnull Class<T> targetEntity);

	/**
	 * Create a criteria insert statement which obtains inserted values from a select query.
	 *
	 * @param targetEntity The target entity type
	 * @param <T> The target entity type
	 * @return a new insert-select criteria statement
	 */
	@Nonnull
	<T> JpaCriteriaInsertSelect<T> createCriteriaInsertSelect(@Nonnull Class<T> targetEntity);

	/**
	 * Create a row of values for an insert-values criteria statement.
	 *
	 * @param expressions The value expressions in row order
	 * @return the values row
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaValues values(@Nonnull Expression<?>... expressions);

	/**
	 * Create a row of values for an insert-values criteria statement.
	 *
	 * @param expressions The value expressions in row order
	 * @return the values row
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaValues values(@Nonnull List<? extends Expression<?>> expressions);

	/**
	 * Transform the given HQL {@code select} query to an equivalent criteria query.
	 *
	 * @param hql The HQL {@code select} query
	 * @param resultClass The result type of the query
	 *
	 * @return the equivalent criteria query
	 *
	 * @since 6.3
	 */
	@Nonnull
	<T> JpaCriteriaQuery<T> createQuery(@Nonnull String hql, @Nonnull Class<T> resultClass);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Set operation

	/**
	 * Create a {@code union all} set operation over the given criteria queries.
	 */
	@Nonnull
	default <T> JpaCriteriaQuery<T> unionAll(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return union( true, query1, queries );
	}

	/**
	 * Create a {@code union} set operation over the given criteria queries.
	 */
	@Nonnull
	default <T> JpaCriteriaQuery<T> union(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return union( false, query1, queries );
	}

	/**
	 * Create a {@code union} or {@code union all} set operation over the given criteria queries.
	 */
	@Nonnull
	<T> JpaCriteriaQuery<T> union(boolean all, @Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries);

	/**
	 * Create an {@code intersect all} set operation over the given criteria queries.
	 */
	@Nonnull
	default <T> JpaCriteriaQuery<T> intersectAll(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return intersect( true, query1, queries );
	}

	/**
	 * Create an {@code intersect} set operation over the given criteria queries.
	 */
	@Nonnull
	default <T> JpaCriteriaQuery<T> intersect(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return intersect( false, query1, queries );
	}

	/**
	 * Create an {@code intersect} or {@code intersect all} set operation over the given criteria queries.
	 */
	@Nonnull
	<T> JpaCriteriaQuery<T> intersect(boolean all, @Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries);

	/**
	 * Create an {@code except all} set operation over the given criteria queries.
	 */
	@Nonnull
	default <T> JpaCriteriaQuery<T> exceptAll(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return except( true, query1, queries );
	}

	/**
	 * Create an {@code except} set operation over the given criteria queries.
	 */
	@Nonnull
	default <T> JpaCriteriaQuery<T> except(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return except( false, query1, queries );
	}

	/**
	 * Create an {@code except} or {@code except all} set operation over the given criteria queries.
	 */
	@Nonnull
	<T> JpaCriteriaQuery<T> except(boolean all, @Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries);

	/**
	 * Create a {@code union} set operation over two criteria selects.
	 */
	@Nonnull
	@Override
	<T> CriteriaSelect<T> union(@Nonnull CriteriaSelect<? extends T> left, @Nonnull CriteriaSelect<? extends T> right);

	/**
	 * Create a {@code union} set operation over two criteria queries.
	 */
	@Nonnull
	<T> JpaCriteriaQuery<T> union(@Nonnull CriteriaQuery<? extends T> left, @Nonnull CriteriaQuery<? extends T> right);

	/**
	 * Create a {@code union} set operation over the given subqueries.
	 */
	@Nonnull
	default <T> JpaSubQuery<T> union(@Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return union( false, query1, queries );
	}

	/**
	 * Create a {@code union} or {@code union all} set operation over the given subqueries.
	 */
	@Nonnull
	<T> JpaSubQuery<T> union(boolean all, @Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries);

	/**
	 * Create a {@code union all} set operation over two subqueries.
	 */
	@Nonnull
	default <T> JpaSubQuery<T> unionAll(@Nonnull JpaSubQuery<? extends T> query1, @Nonnull JpaSubQuery<? extends T> query2) {
		return union( true, query1, query2 );
	}

	/**
	 * Create a {@code union all} set operation over two criteria selects.
	 */
	@Nonnull
	@Override
	<T> CriteriaSelect<T> unionAll(@Nonnull CriteriaSelect<? extends T> left, @Nonnull CriteriaSelect<? extends T> right);

	/**
	 * Create a {@code union all} set operation over two criteria queries.
	 */
	@Nonnull
	<T> JpaCriteriaQuery<T> unionAll(@Nonnull CriteriaQuery<? extends T> left, @Nonnull CriteriaQuery<? extends T> right);

	/**
	 * Create a {@code intersect} set operation over two criteria selects.
	 */
	@Nonnull
	@Override
	<T> CriteriaSelect<T> intersect(@Nonnull CriteriaSelect<? super T> left, @Nonnull CriteriaSelect<? super T> right);

	/**
	 * Create a {@code intersect all} set operation over two criteria selects.
	 */
	@Nonnull
	@Override
	<T> CriteriaSelect<T> intersectAll(@Nonnull CriteriaSelect<? super T> left, @Nonnull CriteriaSelect<? super T> right);

	/**
	 * Create an {@code intersect} set operation over two criteria queries.
	 */
	@Nonnull
	<T> JpaCriteriaQuery<T> intersect(@Nonnull CriteriaQuery<? super T> left, @Nonnull CriteriaQuery<? super T> right);

	/**
	 * Create an {@code intersect all} set operation over two criteria queries.
	 */
	@Nonnull
	<T> JpaCriteriaQuery<T> intersectAll(@Nonnull CriteriaQuery<? super T> left, @Nonnull CriteriaQuery<? super T> right);

	/**
	 * Create an {@code intersect all} set operation over the given subqueries.
	 */
	@Nonnull
	default <T> JpaSubQuery<T> intersectAll(@Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return intersect( true, query1, queries );
	}

	/**
	 * Create an {@code intersect} set operation over the given subqueries.
	 */
	@Nonnull
	default <T> JpaSubQuery<T> intersect(@Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return intersect( false, query1, queries );
	}

	/**
	 * Create an {@code intersect} or {@code intersect all} set operation over the given subqueries.
	 */
	@Nonnull
	<T> JpaSubQuery<T> intersect(boolean all, @Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries);

	/**
	 * Create a {@code except} set operation over two criteria selects.
	 */
	@Nonnull
	@Override
	<T> CriteriaSelect<T> except(@Nonnull CriteriaSelect<T> left, @Nonnull CriteriaSelect<?> right);

	/**
	 * Create a {@code except all} set operation over two criteria selects.
	 */
	@Nonnull
	@Override
	<T> CriteriaSelect<T> exceptAll(@Nonnull CriteriaSelect<T> left, @Nonnull CriteriaSelect<?> right);

	/**
	 * Create an {@code except} set operation over two criteria queries.
	 */
	@Nonnull
	<T> JpaCriteriaQuery<T> except(@Nonnull CriteriaQuery<T> left, @Nonnull CriteriaQuery<?> right);

	/**
	 * Create an {@code except all} set operation over two criteria queries.
	 */
	@Nonnull
	<T> JpaCriteriaQuery<T> exceptAll(@Nonnull CriteriaQuery<T> left, @Nonnull CriteriaQuery<?> right);

	/**
	 * Create an {@code except all} set operation over the given subqueries.
	 */
	@Nonnull
	default <T> JpaSubQuery<T> exceptAll(@Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return except( true, query1, queries );
	}

	/**
	 * Create an {@code except} set operation over the given subqueries.
	 */
	@Nonnull
	default <T> JpaSubQuery<T> except(@Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return except( false, query1, queries );
	}

	/**
	 * Create an {@code except} or {@code except all} set operation over the given subqueries.
	 */
	@Nonnull
	<T> JpaSubQuery<T> except(boolean all, @Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA 3.1

	/**
	 * Create an expression that returns the sign of its
	 * argument, that is, {@code 1} if its argument is
	 * positive, {@code -1} if its argument is negative,
	 * or {@code 0} if its argument is exactly zero.
	 * @param x The expression
	 * @return the sign
	 */
	@Nonnull
	JpaExpression<Integer> sign(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the ceiling of its
	 * argument, that is, the smallest integer greater than
	 * or equal to its argument.
	 * @param x The expression
	 * @return the ceiling
	 */
	@Nonnull
	<N extends Number> JpaExpression<N> ceiling(@Nonnull Expression<N> x);

	/**
	 * Create an expression that returns the floor of its
	 * argument, that is, the largest integer smaller than
	 * or equal to its argument.
	 * @param x The expression
	 * @return the floor
	 */
	@Nonnull
	<N extends Number> JpaExpression<N> floor(@Nonnull Expression<N> x);

	/**
	 * Create an expression that returns the exponential
	 * of its argument, that is, Euler's number <i>e</i>
	 * raised to the power of its argument.
	 * @param x The expression
	 * @return the exponential
	 */
	@Nonnull
	JpaExpression<Double> exp(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the natural logarithm
	 * of its argument.
	 * @param x The expression
	 * @return the natural logarithm
	 */
	@Nonnull
	JpaExpression<Double> ln(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the first argument
	 * raised to the power of its second argument.
	 * @param x The base
	 * @param y The exponent
	 * @return the base raised to the power of the exponent
	 */
	@Nonnull
	JpaExpression<Double> power(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	/**
	 * Create an expression that returns the first argument
	 * raised to the power of its second argument.
	 * @param x The base
	 * @param y The exponent
	 * @return the base raised to the power of the exponent
	 */
	@Nonnull
	JpaExpression<Double> power(@Nonnull Expression<? extends Number> x, @Nullable Number y);

	/**
	 * Create an expression that returns the first argument
	 * rounded to the number of decimal places given by the
	 * second argument.
	 * @param x The base
	 * @param n The number of decimal places
	 * @return the rounded value
	 */
	@Nonnull
	<T extends Number> JpaExpression<T> round(@Nonnull Expression<T> x, @Nonnull Integer n);

	/**
	 * Create an expression that returns the first argument
	 * truncated to the number of decimal places given by the
	 * second argument.
	 * @param x The base
	 * @param n The number of decimal places
	 * @return the truncated value
	 */
	@Nonnull
	<T extends Number> JpaExpression<T> truncate(@Nonnull Expression<T> x, @Nullable Integer n);

	/**
	 *  Create expression to return current local date.
	 * @return the expression for the current date
	 */
	@Nonnull
	JpaExpression<java.time.LocalDate> localDate();

	/**
	 *  Create expression to return current local datetime.
	 * @return the expression for the current timestamp
	 */
	@Nonnull
	JpaExpression<java.time.LocalDateTime> localDateTime();

	/**
	 *  Create expression to return current local time.
	 * @return the expression for the current time
	 */
	@Nonnull
	JpaExpression<java.time.LocalTime> localTime();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Paths

	/**
	 * Create an expression representing the identifier of the given path.
	 *
	 * @param path The entity path
	 * @return the identifier expression
	 */
	@Nonnull
	JpaExpression<?> id(@Nonnull Path<?> path);

	/**
	 * Create an expression representing the version of the given path.
	 *
	 * @param path The entity path
	 * @return the version expression
	 */
	@Nonnull
	JpaExpression<?> version(@Nonnull Path<?> path);

	/**
	 * Create an expression representing the foreign key of the given association path.
	 *
	 * @param path The association path
	 * @return the foreign-key expression
	 */
	@Nonnull
	JpaExpression<?> fk(@Nonnull Path<?> path);

	/**
	 * Downcast the given path to the specified subtype.
	 */
	@Nonnull
	@Override
	<X, T extends X> JpaPath<T> treat(@Nonnull Path<X> path, @Nonnull Class<T> type);

	/**
	 * Downcast the given root to the specified subtype.
	 */
	@Nonnull
	@Override
	<X, T extends X> JpaRoot<T> treat(@Nonnull Root<X> root, @Nonnull Class<T> type);

	/**
	 * Downcast the given from element to the specified subtype.
	 */
	@Nonnull
	@Override
	<X, Y, T extends Y> JpaFrom<X, T> treat(@Nonnull From<X, Y> from, @Nonnull Class<T> type);

	/**
	 * Downcast the given join to the specified subtype.
	 */
	@Nonnull
	@Override
	<X, T, V extends T> JpaJoin<X, V> treat(@Nonnull Join<X, T> join, @Nonnull Class<V> type);

	/**
	 * Downcast the given collection join to the specified subtype.
	 */
	@Nonnull
	@Override
	<X, T, E extends T> JpaCollectionJoin<X, E> treat(@Nonnull CollectionJoin<X, T> join, @Nonnull Class<E> type);

	/**
	 * Downcast the given set join to the specified subtype.
	 */
	@Nonnull
	@Override
	<X, T, E extends T> JpaSetJoin<X, E> treat(@Nonnull SetJoin<X, T> join, @Nonnull Class<E> type);

	/**
	 * Downcast the given list join to the specified subtype.
	 */
	@Nonnull
	@Override
	<X, T, E extends T> JpaListJoin<X, E> treat(@Nonnull ListJoin<X, T> join, @Nonnull Class<E> type);

	/**
	 * Downcast the given map join to the specified subtype.
	 */
	@Nonnull
	@Override
	<X, K, T, V extends T> JpaMapJoin<X, K, V> treat(@Nonnull MapJoin<X, K, T> join, @Nonnull Class<V> type);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Selections

	/**
	 * Create a compound selection using the specified result type constructor.
	 */
	@Nonnull
	@Override
	<Y> JpaCompoundSelection<Y> construct(@Nonnull Class<Y> resultClass, @Nonnull Selection<?>... selections);

	/**
	 * Create a compound selection whose arguments are supplied as a list.
	 *
	 * @param resultClass The compound selection result type
	 * @param arguments The selection arguments
	 * @param <Y> The result type
	 * @return the compound selection
	 */
	@Nonnull
	<Y> JpaCompoundSelection<Y> construct(@Nonnull Class<Y> resultClass, @Nonnull List<? extends Selection<?>> arguments);

	/**
	 * Create a tuple-valued compound selection.
	 */
	@Nonnull
	@Override
	JpaCompoundSelection<Tuple> tuple(@Nonnull Selection<?>... selections);

	/**
	 * Create a tuple-valued compound selection from a list of selections.
	 */
	@Nonnull
	JpaCompoundSelection<Tuple> tuple(@Nonnull List<Selection<?>> selections);

	/**
	 * Create an array-valued compound selection.
	 */
	@Nonnull
	@Override
	JpaCompoundSelection<Object[]> array(@Nonnull Selection<?>... selections);

	/**
	 * Create an array-valued compound selection from a list of selections.
	 */
	@Nonnull
	JpaCompoundSelection<Object[]> array(@Nonnull List<Selection<?>> selections);

	/**
	 * Create an array-valued compound selection with the given result array type.
	 */
	@Nonnull
	<Y> JpaCompoundSelection<Y> array(@Nonnull Class<Y> resultClass, @Nonnull Selection<?>... selections);

	/**
	 * Create an array-valued compound selection with the given result array type.
	 */
	@Nonnull
	<Y> JpaCompoundSelection<Y> array(@Nonnull Class<Y> resultClass, @Nonnull List<? extends Selection<?>> selections);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	/**
	 * Create an aggregate expression returning the average of numeric values.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<Double> avg(@Nonnull Expression<N> argument);

	/**
	 * Create an expression returning a numeric sum.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> sum(@Nonnull Expression<N> argument);

	/**
	 * Create an aggregate expression returning the sum as a Long.
	 */
	@Nonnull
	@Override
	JpaExpression<Long> sumAsLong(@Nonnull Expression<Integer> argument);

	/**
	 * Create an aggregate expression returning the sum as a Double.
	 */
	@Nonnull
	@Override
	JpaExpression<Double> sumAsDouble(@Nonnull Expression<Float> argument);

	/**
	 * Create an aggregate expression returning the maximum numeric value.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> max(@Nonnull Expression<N> argument);

	/**
	 * Create an aggregate expression returning the minimum numeric value.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> min(@Nonnull Expression<N> argument);

	/**
	 * Create an aggregate expression returning the greatest comparable value.
	 */
	@Nonnull
	@Override
	<X extends Comparable<? super X>> JpaExpression<X> greatest(@Nonnull Expression<X> argument);

	/**
	 * Create an aggregate expression returning the least comparable value.
	 */
	@Nonnull
	@Override
	<X extends Comparable<? super X>> JpaExpression<X> least(@Nonnull Expression<X> argument);

	/**
	 * Create an aggregate expression returning a row count.
	 */
	@Nonnull
	@Override
	JpaExpression<Long> count(@Nonnull Expression<?> argument);

	/**
	 * Create an aggregate expression returning the count of distinct values.
	 */
	@Nonnull
	@Override
	JpaExpression<Long> countDistinct(@Nonnull Expression<?> x);

	/**
	 * Equivalent to HQL {@code count(*)}.
	 */
	@Nonnull
	JpaExpression<Long> count();

	/**
	 * Create an expression returning the arithmetic negation of its argument.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> neg(@Nonnull Expression<N> x);

	/**
	 * Create an expression returning the absolute value of its argument.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> abs(@Nonnull Expression<N> x);

	/**
	 * Create an expression returning a numeric sum.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> sum(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y);

	/**
	 * Create an expression returning a numeric sum.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> sum(@Nonnull Expression<? extends N> x, @Nullable N y);

	/**
	 * Create an expression returning a numeric sum.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> sum(@Nullable N x, @Nonnull Expression<? extends N> y);

	/**
	 * Create an expression returning a numeric product.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> prod(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y);

	/**
	 * Create an expression returning a numeric product.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> prod(@Nonnull Expression<? extends N> x, @Nullable N y);

	/**
	 * Create an expression returning a numeric product.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> prod(@Nullable N x, @Nonnull Expression<? extends N> y);

	/**
	 * Create an expression returning a numeric difference.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> diff(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y);

	/**
	 * Create an expression returning a numeric difference.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> diff(@Nonnull Expression<? extends N> x, @Nullable N y);

	/**
	 * Create an expression returning a numeric difference.
	 */
	@Nonnull
	@Override
	<N extends Number> JpaExpression<N> diff(@Nullable N x, @Nonnull Expression<? extends N> y);

	/**
	 * Create an expression returning a numeric quotient.
	 */
	@Nonnull
	@Override
	JpaExpression<Number> quot(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	/**
	 * Create an expression returning a numeric quotient.
	 */
	@Nonnull
	@Override
	JpaExpression<Number> quot(@Nonnull Expression<? extends Number> x, @Nullable Number y);

	/**
	 * Create an expression returning a numeric quotient.
	 */
	@Nonnull
	@Override
	JpaExpression<Number> quot(@Nullable Number x, @Nonnull Expression<? extends Number> y);

	/**
	 * Create an expression returning the remainder of integer division.
	 */
	@Nonnull
	@Override
	JpaExpression<Integer> mod(@Nonnull Expression<Integer> x, @Nonnull Expression<Integer> y);

	/**
	 * Create an expression returning the remainder of integer division.
	 */
	@Nonnull
	@Override
	JpaExpression<Integer> mod(@Nonnull Expression<Integer> x, @Nullable Integer y);

	/**
	 * Create an expression returning the remainder of integer division.
	 */
	@Nonnull
	@Override
	JpaExpression<Integer> mod(@Nullable Integer x, @Nonnull Expression<Integer> y);

	/**
	 * Create an expression returning the square root of its argument.
	 */
	@Nonnull
	@Override
	JpaExpression<Double> sqrt(@Nonnull Expression<? extends Number> x);

	/**
	 * Add two {@linkplain Duration durations}.
	 * @since 6.3
	 */
	@Nonnull
	JpaExpression<Duration> durationSum(@Nonnull Expression<Duration> x, @Nonnull Expression<Duration> y);

	/**
	 * Add two {@linkplain Duration durations}.
	 * @since 6.3
	 */
	@Nonnull
	JpaExpression<Duration> durationSum(@Nonnull Expression<Duration> x, @Nullable Duration y);

	/**
	 * Subtract one {@linkplain Duration duration} from another.
	 * @since 6.3
	 */
	@Nonnull
	JpaExpression<Duration> durationDiff(@Nonnull Expression<Duration> x, @Nonnull Expression<Duration> y);

	/**
	 * Subtract one {@linkplain Duration duration} from another.
	 * @since 6.3
	 */
	@Nonnull
	JpaExpression<Duration> durationDiff(@Nonnull Expression<Duration> x, @Nullable Duration y);

	/**
	 * Scale a {@linkplain Duration duration} by a number.
	 * @since 6.3
	 */
	@Nonnull
	JpaExpression<Duration> durationScaled(@Nonnull Expression<? extends Number> number, @Nonnull Expression<Duration> duration);

	/**
	 * Scale a {@linkplain Duration duration} by a number.
	 * @since 6.3
	 */
	@Nonnull
	JpaExpression<Duration> durationScaled(@Nullable Number number, @Nonnull Expression<Duration> duration);

	/**
	 * Scale a {@linkplain Duration duration} by a number.
	 * @since 6.3
	 */
	@Nonnull
	JpaExpression<Duration> durationScaled(@Nonnull Expression<? extends Number> number, @Nullable Duration duration);

	/**
	 * A literal {@link Duration}, for example, "five days" or "30 minutes".
	 * @since 6.3
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Duration> duration(long magnitude, @Nonnull TemporalUnit unit);

	/**
	 * Convert a {@link Duration} to a numeric magnitude in the given units.
	 * @param unit The temporal granularity
	 * @param duration The duration in a "unit-free" form
	 * @return the magnitude of the duration measured in the given units
	 * @since 6.3
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Long> durationByUnit(@Nonnull TemporalUnit unit, @Nonnull Expression<Duration> duration);

	/**
	 * Subtract two dates or two datetimes, returning the duration between the
	 * two dates or between two datetimes.
	 * @since 6.3
	 */
	@Nonnull
	<T extends Temporal> JpaExpression<Duration> durationBetween(@Nonnull Expression<T> x, @Nonnull Expression<T> y);

	/**
	 * Subtract two dates or two datetimes, returning the duration between the
	 * two dates or between two datetimes.
	 * @since 6.3
	 */
	@Nonnull
	<T extends Temporal> JpaExpression<Duration> durationBetween(@Nonnull Expression<T> x, @Nullable T y);

	/**
	 * Add a duration to a date or datetime, that is, return a later date or
	 * datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	@Nonnull
	<T extends Temporal> JpaExpression<T> addDuration(@Nonnull Expression<T> datetime, @Nonnull Expression<Duration> duration);

	/**
	 * Add a duration to a date or datetime, that is, return a later date or
	 * datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	@Nonnull
	<T extends Temporal> JpaExpression<T> addDuration(@Nonnull Expression<T> datetime, @Nullable Duration duration);

	/**
	 * Add a duration to a date or datetime, that is, return a later date or
	 * datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	@Nonnull
	<T extends Temporal> JpaExpression<T> addDuration(@Nullable T datetime, @Nonnull Expression<Duration> duration);

	/**
	 * Subtract a duration to a date or datetime, that is, return an earlier date
	 * or datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	@Nonnull
	<T extends Temporal> JpaExpression<T> subtractDuration(@Nonnull Expression<T> datetime, @Nonnull Expression<Duration> duration);

	/**
	 * Subtract a duration to a date or datetime, that is, return an earlier date
	 * or datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	@Nonnull
	<T extends Temporal> JpaExpression<T> subtractDuration(@Nonnull Expression<T> datetime, @Nullable Duration duration);

	/**
	 * Subtract a duration to a date or datetime, that is, return an earlier date
	 * or datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	@Nonnull
	<T extends Temporal> JpaExpression<T> subtractDuration(@Nullable T datetime, @Nonnull Expression<Duration> duration);

	/**
	 * Create an expression converted to Long.
	 */
	@Nonnull
	@Override
	JpaExpression<Long> toLong(@Nonnull Expression<? extends Number> number);

	/**
	 * Create an expression converted to Integer.
	 */
	@Nonnull
	@Override
	JpaExpression<Integer> toInteger(@Nonnull Expression<? extends Number> number);

	/**
	 * Create an expression converted to Float.
	 */
	@Nonnull
	@Override
	JpaExpression<Float> toFloat(@Nonnull Expression<? extends Number> number);

	/**
	 * Create an expression converted to Double.
	 */
	@Nonnull
	@Override
	JpaExpression<Double> toDouble(@Nonnull Expression<? extends Number> number);

	/**
	 * Create an expression converted to BigDecimal.
	 */
	@Nonnull
	@Override
	JpaExpression<BigDecimal> toBigDecimal(@Nonnull Expression<? extends Number> number);

	/**
	 * Create an expression converted to BigInteger.
	 */
	@Nonnull
	@Override
	JpaExpression<BigInteger> toBigInteger(@Nonnull Expression<? extends Number> number);

	/**
	 * Create an expression converted to String.
	 */
	@Nonnull
	@Override
	JpaExpression<String> toString(@Nonnull Expression<Character> character);

	/**
	 * Create a literal expression for the given value.
	 */
	@Nonnull
	@Override
	<T> JpaExpression<T> literal(@Nonnull T value);

	/**
	 * Create literal expressions for each of the given values.
	 */
	@Nonnull
	<T> List<? extends JpaExpression<T>> literals(@Nullable T... values);

	/**
	 * Create literal expressions for each value in the given list.
	 */
	@Nonnull
	<T> List<? extends JpaExpression<T>> literals(@Nullable List<T> values);

	/**
	 * Create a null literal expression of the given type.
	 */
	@Nonnull
	@Override
	<T> JpaExpression<T> nullLiteral(@Nonnull Class<T> resultClass);

	/**
	 * Create a parameter expression.
	 */
	@Nonnull
	@Override
	<T> JpaParameterExpression<T> parameter(@Nonnull Class<T> paramClass);

	/**
	 * Create a parameter expression.
	 */
	@Nonnull
	@Override
	<T> JpaParameterExpression<T> parameter(@Nonnull Class<T> paramClass, @Nonnull String name);

	/**
	 * Create a multivalued parameter accepting multiple arguments
	 * packaged together as a {@link List}.
	 * @param paramClass The type of each argument to the parameter
	 * @param <T> The type of each argument to the parameter
	 * @since 7.0
	 */
	@Nonnull
	<T> JpaParameterExpression<List<T>> listParameter(@Nonnull Class<T> paramClass);

	/**
	 * Create a multivalued parameter accepting multiple arguments
	 * packaged together as a {@link List}.
	 * @param paramClass The type of each argument to the parameter
	 * @param name The parameter name
	 * @param <T> The type of each argument to the parameter
	 * @since 7.0
	 */
	@Nonnull
	<T> JpaParameterExpression<List<T>> listParameter(@Nonnull Class<T> paramClass, @Nullable String name);

	/**
	 * Create a string concatenation expression.
	 */
	@Nonnull
	@Override
	JpaExpression<String> concat(@Nonnull Expression<String> x, @Nonnull Expression<String> y);

	/**
	 * Create a string concatenation expression.
	 */
	@Nonnull
	@Override
	JpaExpression<String> concat(@Nonnull Expression<String> x, @Nonnull String y);

	/**
	 * Create a string concatenation expression.
	 */
	@Nonnull
	@Override
	JpaExpression<String> concat(@Nonnull String x, @Nonnull Expression<String> y);

	/**
	 * Create an expression that concatenates two string literals.
	 */
	@Nonnull
	JpaExpression<String> concat(@Nullable String x, @Nullable String y);

	/**
	 * Create a substring expression.
	 */
	@Nonnull
	@Override
	JpaFunction<String> substring(@Nonnull Expression<String> x, @Nonnull Expression<Integer> from);

	/**
	 * Create a substring expression.
	 */
	@Nonnull
	@Override
	JpaFunction<String> substring(@Nonnull Expression<String> x, int from);

	/**
	 * Create a substring expression.
	 */
	@Nonnull
	@Override
	JpaFunction<String> substring(
			@Nonnull Expression<String> x,
			@Nonnull Expression<Integer> from,
			@Nonnull Expression<Integer> len);

	/**
	 * Create a substring expression.
	 */
	@Nonnull
	@Override
	JpaFunction<String> substring(@Nonnull Expression<String> x, int from, int len);

	/**
	 * Create a trim expression.
	 */
	@Nonnull
	@Override
	JpaFunction<String> trim(@Nonnull Expression<String> x);

	/**
	 * Create a trim expression.
	 */
	@Nonnull
	@Override
	JpaFunction<String> trim(@Nonnull Trimspec ts, @Nonnull Expression<String> x);

	/**
	 * Create a trim expression.
	 */
	@Nonnull
	@Override
	JpaFunction<String> trim(@Nonnull Expression<Character> t, @Nonnull Expression<String> x);

	/**
	 * Create a trim expression.
	 */
	@Nonnull
	@Override
	JpaFunction<String> trim(@Nonnull Trimspec ts, @Nonnull Expression<Character> t, @Nonnull Expression<String> x);

	/**
	 * Create a trim expression.
	 */
	@Nonnull
	@Override
	JpaFunction<String> trim(char t, @Nonnull Expression<String> x);

	/**
	 * Create a trim expression.
	 */
	@Nonnull
	@Override
	JpaFunction<String> trim(@Nonnull Trimspec ts, char t, @Nonnull Expression<String> x);

	/**
	 * Create an expression returning a lowercase string.
	 */
	@Nonnull
	@Override
	JpaFunction<String> lower(@Nonnull Expression<String> x);

	/**
	 * Create an expression returning an uppercase string.
	 */
	@Nonnull
	@Override
	JpaFunction<String> upper(@Nonnull Expression<String> x);

	/**
	 * Create an expression returning the length of a string.
	 */
	@Nonnull
	@Override
	JpaFunction<Integer> length(@Nonnull Expression<String> x);

	/**
	 * Create an expression returning the position of a substring.
	 */
	@Nonnull
	@Override
	JpaFunction<Integer> locate(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern);

	/**
	 * Create an expression returning the position of a substring.
	 */
	@Nonnull
	@Override
	JpaFunction<Integer> locate(@Nonnull Expression<String> x, @Nonnull String pattern);

	/**
	 * Create an expression returning the position of a substring.
	 */
	@Nonnull
	@Override
	JpaFunction<Integer> locate(
			@Nonnull Expression<String> x,
			@Nonnull Expression<String> pattern,
			@Nonnull Expression<Integer> from);

	/**
	 * Create an expression returning the position of a substring.
	 */
	@Nonnull
	@Override
	JpaFunction<Integer> locate(@Nonnull Expression<String> x, @Nonnull String pattern, int from);

	/**
	 * Create an expression for the current SQL date.
	 */
	@Nonnull
	@Override
	JpaFunction<Date> currentDate();

	/**
	 * Create an expression for the current SQL time.
	 */
	@Nonnull
	@Override
	JpaFunction<Time> currentTime();

	/**
	 * Create an expression for the current SQL timestamp.
	 */
	@Nonnull
	@Override
	JpaFunction<Timestamp> currentTimestamp();

	/**
	 * Create an expression for the current instant.
	 */
	@Nonnull
	JpaFunction<Instant> currentInstant();

	/**
	 * Create an expression invoking a database function.
	 */
	@Nonnull
	@Override
	<T> JpaFunction<T> function(@Nonnull String name, @Nonnull Class<T> type, @Nonnull Expression<?>... args);

	/**
	 * Create an {@code all} quantified expression over a subquery.
	 */
	@Nonnull
	@Override
	<Y> JpaExpression<Y> all(@Nonnull Subquery<Y> subquery);

	/**
	 * Create a {@code some} quantified expression over a subquery.
	 */
	@Nonnull
	@Override
	<Y> JpaExpression<Y> some(@Nonnull Subquery<Y> subquery);

	/**
	 * Create an {@code any} quantified expression over a subquery.
	 */
	@Nonnull
	@Override
	<Y> JpaExpression<Y> any(@Nonnull Subquery<Y> subquery);

	/**
	 * Create an expression representing the indexes of the given list.
	 */
	@Nonnull
	<K, L extends List<?>> JpaExpression<Set<K>> indexes(@Nonnull L list);

	/**
	 * Create an expression for a Java value, using the configured value handling mode.
	 */
	@Nonnull
	<T> JpaExpression<T> value(@Nullable T value);

	/**
	 * Create an expression returning the size of a collection.
	 */
	@Nonnull
	@Override
	<C extends Collection<?>> JpaExpression<Integer> size(@Nonnull Expression<C> collection);

	/**
	 * Create an expression returning the size of a collection.
	 */
	@Nonnull
	@Override
	<C extends Collection<?>> JpaExpression<Integer> size(@Nonnull C collection);

	/**
	 * Create a coalesce expression.
	 */
	@Nonnull
	@Override
	<T> JpaCoalesce<T> coalesce();

	/**
	 * Create a coalesce expression.
	 */
	@Nonnull
	@Override
	<Y> JpaCoalesce<Y> coalesce(@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y);

	/**
	 * Create a coalesce expression.
	 */
	@Nonnull
	@Override
	<Y> JpaCoalesce<Y> coalesce(@Nonnull Expression<? extends Y> x, @Nullable Y y);

	/**
	 * Create a nullif expression.
	 */
	@Nonnull
	@Override
	<Y> JpaExpression<Y> nullif(@Nonnull Expression<Y> x, @Nonnull Expression<?> y);

	/**
	 * Create a nullif expression.
	 */
	@Nonnull
	@Override
	<Y> JpaExpression<Y> nullif(@Nonnull Expression<Y> x, @Nullable Y y);

	/**
	 * Create a simple case expression.
	 */
	@Nonnull
	@Override
	<C, R> JpaSimpleCase<C, R> selectCase(@Nonnull Expression<? extends C> expression);

	/**
	 * Create a case expression with the given result type.
	 */
	@Nonnull
	@Override
	<C, R> JpaSimpleCase<C, R> selectCase(@Nonnull Expression<? extends C> expression, @Nonnull Class<R> resultType);

	/**
	 * Create a searched case expression.
	 */
	@Nonnull
	@Override
	<R> JpaSearchedCase<R> selectCase();

	/**
	 * Create a case expression with the given result type.
	 */
	@Nonnull
	@Override
	<R> JpaSearchedCase<R> selectCase(@Nonnull Class<R> resultType);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates


	/**
	 * Create a conjunction predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate and(@Nonnull Expression<Boolean> x, @Nonnull Expression<Boolean> y);

	/**
	 * Create a conjunction predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate and(@Nonnull BooleanExpression... restrictions);

	/**
	 * Create a conjunction predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate and(@Nonnull List<? extends Expression<Boolean>> restrictions);

	/**
	 * @deprecated Prefer {@linkplain #and(BooleanExpression...)} instead.  This method used to be
	 * defined as part of Jakarta Persistence, which removed it as of 4.0.
	 */
	@Nonnull
	@Deprecated
	JpaPredicate and(@Nonnull Predicate... restrictions);

	/**
	 * Create a disjunction predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate or(@Nonnull Expression<Boolean> x, @Nonnull Expression<Boolean> y);

	/**
	 * Create a disjunction predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate or(@Nonnull BooleanExpression... restrictions);

	/**
	 * Create a disjunction predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate or(@Nonnull List<? extends Expression<Boolean>> restrictions);

	/**
	 * @deprecated Prefer {@linkplain #or(BooleanExpression...)} instead.  This method used to be
	 * defined as part of Jakarta Persistence, which removed it as of 4.0.
	 */
	@Nonnull
	@Deprecated
	JpaPredicate or(@Nonnull Predicate... restrictions);

	/**
	 * Create a negated predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate not(@Nonnull Expression<Boolean> restriction);

	/**
	 * Create a predicate that is always true.
	 */
	@Nonnull
	@Override
	JpaPredicate conjunction();

	/**
	 * Create a predicate that is always false.
	 */
	@Nonnull
	@Override
	JpaPredicate disjunction();

	/**
	 * Create a predicate testing whether an expression is true.
	 */
	@Nonnull
	@Override
	JpaPredicate isTrue(@Nonnull Expression<Boolean> x);

	/**
	 * Create a predicate testing whether an expression is false.
	 */
	@Nonnull
	@Override
	JpaPredicate isFalse(@Nonnull Expression<Boolean> x);

	/**
	 * Create a predicate testing whether an expression is null.
	 */
	@Nonnull
	@Override
	JpaPredicate isNull(@Nonnull Expression<?> x);

	/**
	 * Create a predicate testing whether an expression is not null.
	 */
	@Nonnull
	@Override
	JpaPredicate isNotNull(@Nonnull Expression<?> x);

	/**
	 * Create an equality predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate equal(@Nonnull Expression<?> x, @Nonnull Expression<?> y);

	/**
	 * Create an equality predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate equal(@Nonnull Expression<?> x, @Nullable Object y);

	/**
	 * Create an inequality predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate notEqual(@Nonnull Expression<?> x, @Nonnull Expression<?> y);

	/**
	 * Create an inequality predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate notEqual(@Nonnull Expression<?> x, @Nullable Object y);

	/**
	 * Create a predicate testing whether two expressions are distinct, treating nulls as comparable values.
	 */
	@Nonnull
	JpaPredicate distinctFrom(@Nonnull Expression<?> x, @Nonnull Expression<?> y);

	/**
	 * Create a predicate testing whether an expression and a value are distinct, treating nulls as comparable values.
	 */
	@Nonnull
	JpaPredicate distinctFrom(@Nonnull Expression<?> x, @Nullable Object y);

	/**
	 * Create a predicate testing whether two expressions are not distinct, treating nulls as comparable values.
	 */
	@Nonnull
	JpaPredicate notDistinctFrom(@Nonnull Expression<?> x, @Nonnull Expression<?> y);

	/**
	 * Create a predicate testing whether an expression and a value are not distinct, treating nulls as comparable values.
	 */
	@Nonnull
	JpaPredicate notDistinctFrom(@Nonnull Expression<?> x, @Nullable Object y);

	/**
	 * Create a greater-than predicate for comparable expressions.
	 */
	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThan(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y);

	/**
	 * Create a greater-than predicate for comparable expressions.
	 */
	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThan(@Nonnull Expression<? extends Y> x, @Nullable Y y);

	/**
	 * Create a greater-than-or-equal predicate for comparable expressions.
	 */
	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThanOrEqualTo(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y);

	/**
	 * Create a greater-than-or-equal predicate for comparable expressions.
	 */
	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThanOrEqualTo(@Nonnull Expression<? extends Y> x, @Nullable Y y);

	/**
	 * Create a less-than predicate for comparable expressions.
	 */
	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThan(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y);

	/**
	 * Create a less-than predicate for comparable expressions.
	 */
	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThan(@Nonnull Expression<? extends Y> x, @Nullable Y y);

	/**
	 * Create a less-than-or-equal predicate for comparable expressions.
	 */
	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThanOrEqualTo(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y);

	/**
	 * Create a less-than-or-equal predicate for comparable expressions.
	 */
	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThanOrEqualTo(@Nonnull Expression<? extends Y> x, @Nullable Y y);

	/**
	 * Create a predicate testing whether a value is between two bounds.
	 */
	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> JpaPredicate between(
			@Nonnull Expression<? extends Y> value,
			@Nonnull Expression<? extends Y> lower,
			@Nonnull Expression<? extends Y> upper);

	/**
	 * Create a predicate testing whether a value is between two bounds.
	 */
	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> JpaPredicate between(@Nonnull Expression<? extends Y> value, @Nullable Y lower, @Nullable Y upper);

	/**
	 * Create a predicate testing whether a value is between two bounds.
	 */
	@Nonnull
	@Override
	<Y extends Comparable<? super Y>> JpaPredicate between(
			@Nullable Y value,
			@Nonnull Expression<? extends Y> lower,
			@Nonnull Expression<? extends Y> upper);

	/**
	 * Create a greater-than predicate for numeric expressions.
	 */
	@Nonnull
	@Override
	JpaPredicate gt(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	/**
	 * Create a greater-than predicate for numeric expressions.
	 */
	@Nonnull
	@Override
	JpaPredicate gt(@Nonnull Expression<? extends Number> x, @Nullable Number y);

	/**
	 * Create a greater-than-or-equal predicate for numeric expressions.
	 */
	@Nonnull
	@Override
	JpaPredicate ge(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	/**
	 * Create a greater-than-or-equal predicate for numeric expressions.
	 */
	@Nonnull
	@Override
	JpaPredicate ge(@Nonnull Expression<? extends Number> x, @Nullable Number y);

	/**
	 * Create a less-than predicate for numeric expressions.
	 */
	@Nonnull
	@Override
	JpaPredicate lt(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	/**
	 * Create a less-than predicate for numeric expressions.
	 */
	@Nonnull
	@Override
	JpaPredicate lt(@Nonnull Expression<? extends Number> x, @Nullable Number y);

	/**
	 * Create a less-than-or-equal predicate for numeric expressions.
	 */
	@Nonnull
	@Override
	JpaPredicate le(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y);

	/**
	 * Create a less-than-or-equal predicate for numeric expressions.
	 */
	@Nonnull
	@Override
	JpaPredicate le(@Nonnull Expression<? extends Number> x, @Nullable Number y);

	/**
	 * Create a predicate testing whether a collection is empty.
	 */
	@Nonnull
	@Override
	<C extends Collection<?>> JpaPredicate isEmpty(@Nonnull Expression<C> collection);

	/**
	 * Create a predicate testing whether a collection is not empty.
	 */
	@Nonnull
	@Override
	<C extends Collection<?>> JpaPredicate isNotEmpty(@Nonnull Expression<C> collection);

	/**
	 * Create a predicate testing whether an element is a member of a collection.
	 */
	@Nonnull
	@Override
	<E, C extends Collection<E>> JpaPredicate isMember(@Nonnull Expression<E> elem, @Nonnull Expression<C> collection);

	/**
	 * Create a predicate testing whether an element is a member of a collection.
	 */
	@Nonnull
	@Override
	<E, C extends Collection<E>> JpaPredicate isMember(@Nullable E elem, @Nonnull Expression<C> collection);

	/**
	 * Create a predicate testing whether an element is not a member of a collection.
	 */
	@Nonnull
	@Override
	<E, C extends Collection<E>> JpaPredicate isNotMember(@Nonnull Expression<E> elem, @Nonnull Expression<C> collection);

	/**
	 * Create a predicate testing whether an element is not a member of a collection.
	 */
	@Nonnull
	@Override
	<E, C extends Collection<E>> JpaPredicate isNotMember(@Nullable E elem, @Nonnull Expression<C> collection);

	/**
	 * Create a {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate like(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern);

	/**
	 * Create a {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate like(@Nonnull Expression<String> x, @Nonnull String pattern);

	/**
	 * Create a {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate like(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar);

	/**
	 * Create a {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate like(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar);

	/**
	 * Create a {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate like(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull Expression<Character> escapeChar);

	/**
	 * Create a {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate like(@Nonnull Expression<String> x, @Nonnull String pattern, char escapeChar);

	/**
	 * Create a case-insensitive {@code like} predicate.
	 */
	@Nonnull
	JpaPredicate ilike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern);

	/**
	 * Create a case-insensitive {@code like} predicate.
	 */
	@Nonnull
	JpaPredicate ilike(@Nonnull Expression<String> x, @Nullable String pattern);

	/**
	 * Create a case-insensitive {@code like} predicate with an escape character expression.
	 */
	@Nonnull
	JpaPredicate ilike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar);

	/**
	 * Create a case-insensitive {@code like} predicate with an escape character.
	 */
	@Nonnull
	JpaPredicate ilike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar);

	/**
	 * Create a case-insensitive {@code like} predicate with an escape character expression.
	 */
	@Nonnull
	JpaPredicate ilike(@Nonnull Expression<String> x, @Nullable String pattern, @Nonnull Expression<Character> escapeChar);

	/**
	 * Create a case-insensitive {@code like} predicate with an escape character.
	 */
	@Nonnull
	JpaPredicate ilike(@Nonnull Expression<String> x, @Nullable String pattern, char escapeChar);

	/**
	 * Create a negated {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern);

	/**
	 * Create a negated {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern);

	/**
	 * Create a negated {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar);

	/**
	 * Create a negated {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar);

	/**
	 * Create a negated {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull Expression<Character> escapeChar);

	/**
	 * Create a negated {@code like} predicate.
	 */
	@Nonnull
	@Override
	JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern, char escapeChar);

	/**
	 * Create a negated case-insensitive {@code like} predicate.
	 */
	@Nonnull
	JpaPredicate notIlike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern);

	/**
	 * Create a negated case-insensitive {@code like} predicate.
	 */
	@Nonnull
	JpaPredicate notIlike(@Nonnull Expression<String> x, @Nullable String pattern);

	/**
	 * Create a negated case-insensitive {@code like} predicate with an escape character expression.
	 */
	@Nonnull
	JpaPredicate notIlike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar);

	/**
	 * Create a negated case-insensitive {@code like} predicate with an escape character.
	 */
	@Nonnull
	JpaPredicate notIlike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar);

	/**
	 * Create a negated case-insensitive {@code like} predicate with an escape character expression.
	 */
	@Nonnull
	JpaPredicate notIlike(@Nonnull Expression<String> x, @Nullable String pattern, @Nonnull Expression<Character> escapeChar);

	/**
	 * Create a negated case-insensitive {@code like} predicate with an escape character.
	 */
	@Nonnull
	JpaPredicate notIlike(@Nonnull Expression<String> x, @Nullable String pattern, char escapeChar);

	/**
	 * Create a predicate testing whether a string expression matches a regular expression.
	 */
	@Nonnull
	JpaPredicate likeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern);

	/**
	 * Create a case-insensitive predicate testing whether a string expression matches a regular expression.
	 */
	@Nonnull
	JpaPredicate ilikeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern);

	/**
	 * Create a negated predicate testing whether a string expression matches a regular expression.
	 */
	@Nonnull
	JpaPredicate notLikeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern);

	/**
	 * Create a negated case-insensitive predicate testing whether a string expression matches a regular expression.
	 */
	@Nonnull
	JpaPredicate notIlikeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern);

	/**
	 * Create an {@code in} predicate builder for the given expression.
	 */
	@Nonnull
	@Override
	<T> JpaInPredicate<T> in(@Nonnull Expression<? extends T> expression);

	/**
	 * Create an {@code in} predicate with expression values.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	<T> JpaInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull Expression<? extends T>... values);

	/**
	 * Create an {@code in} predicate with literal values.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	<T> JpaInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull T... values);

	/**
	 * Create an {@code in} predicate with values supplied as a collection.
	 */
	@Nonnull
	<T> JpaInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull Collection<T> values);

	/**
	 * Create a predicate testing whether a subquery returns results.
	 */
	@Nonnull
	@Override
	JpaPredicate exists(@Nonnull Subquery<?> subquery);

	/**
	 * Create a predicate that tests whether a Map is empty.
	 *
	 * @apiNote Due to type-erasure we cannot name this the same as
	 *          {@link CriteriaBuilder#isEmpty}.
	 *
	 * @param mapExpression The map expression to check for emptiness
	 *
	 * @return the is-empty predicate
	 */
	@Nonnull
	<M extends Map<?,?>> JpaPredicate isMapEmpty(@Nonnull JpaExpression<M> mapExpression);

	/**
	 * Create a predicate that tests whether a Map is not empty.
	 *
	 * @apiNote Due to type-erasure we cannot name this the same as
	 *          {@link CriteriaBuilder#isNotEmpty}
	 *
	 * @param mapExpression The map expression to check for non-emptiness
	 *
	 * @return the is-not-empty predicate
	 */
	@Nonnull
	<M extends Map<?,?>> JpaPredicate isMapNotEmpty(@Nonnull JpaExpression<M> mapExpression);

	/**
	 * Create an expression that tests the size of a map.
	 *
	 * @apiNote Due to type-erasure we cannot name this the same as
	 *          {@link CriteriaBuilder#size}
	 *
	 * @param mapExpression The map expression for which to calculate the size
	 *
	 * @return the size expression
	 */
	@Nonnull
	<M extends Map<?,?>> JpaExpression<Integer> mapSize(@Nonnull JpaExpression<M> mapExpression);

	/**
	 * Create an expression that tests the size of a map.
	 *
	 * @param map The map for which to calculate the size
	 *
	 * @return the size expression
	 */
	@Nonnull
	<M extends Map<?, ?>> JpaExpression<Integer> mapSize(@Nonnull M map);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordering


	/**
	 * Create an ordering for the given sort expression.
	 */
	@Nonnull
	JpaOrder sort(@Nonnull JpaExpression<?> sortExpression);

	/**
	 * Create an ordering for the given sort expression and direction.
	 */
	@Nonnull
	JpaOrder sort(@Nonnull JpaExpression<?> sortExpression, @Nonnull SortDirection sortOrder);

	/**
	 * Create an ordering for the given sort expression, direction, and null precedence.
	 */
	@Nonnull
	JpaOrder sort(@Nonnull JpaExpression<?> sortExpression, @Nonnull SortDirection sortOrder, @Nonnull Nulls nullPrecedence);

	/**
	 * Create an ordering for the given sort expression with optional case-insensitive comparison.
	 */
	@Nonnull
	JpaOrder sort(@Nonnull JpaExpression<?> sortExpression, @Nonnull SortDirection sortOrder, @Nonnull Nulls nullPrecedence, boolean ignoreCase);

	/**
	 * Create an ascending ordering for the given expression.
	 */
	@Nonnull
	@Override
	JpaOrder asc(@Nonnull Expression<?> x);

	/**
	 * Create a descending ordering for the given expression.
	 */
	@Nonnull
	@Override
	JpaOrder desc(@Nonnull Expression<?> x);

	/**
	 * Create an ordering by the ascending value of the expression.
	 * @param x The expression used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return the ascending ordering corresponding to the expression
	 */
	@Nonnull
	JpaOrder asc(@Nonnull Expression<?> x, boolean nullsFirst);

	/**
	 * Create an ordering by the descending value of the expression.
	 * @param x The expression used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return the descending ordering corresponding to the expression
	 */
	@Nonnull
	JpaOrder desc(@Nonnull Expression<?> x, boolean nullsFirst);

	/**
	 * Create a search ordering based on the sort order and null precedence of the value of the CTE attribute.
	 * @param cteAttribute The CTE attribute used to define the ordering
	 * @param sortOrder The sort order
	 * @param nullPrecedence The null precedence
	 * @return the ordering corresponding to the CTE attribute
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaSearchOrder search(@Nonnull JpaCteCriteriaAttribute cteAttribute, @Nonnull SortDirection sortOrder, @Nonnull Nulls nullPrecedence);

	/**
	 * Create a search ordering based on the sort order of the value of the CTE attribute.
	 * @param cteAttribute The CTE attribute used to define the ordering
	 * @param sortOrder The sort order
	 * @return the ordering corresponding to the CTE attribute
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaSearchOrder search(@Nonnull JpaCteCriteriaAttribute cteAttribute, @Nonnull SortDirection sortOrder);

	/**
	 * Create a search ordering based on the ascending value of the CTE attribute.
	 * @param cteAttribute The CTE attribute used to define the ordering
	 * @return the ascending ordering corresponding to the CTE attribute
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaSearchOrder search(@Nonnull JpaCteCriteriaAttribute cteAttribute);

	/**
	 * Create a search ordering by the ascending value of the CTE attribute.
	 * @param x The CTE attribute used to define the ordering
	 * @return the ascending ordering corresponding to the CTE attribute
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaSearchOrder asc(@Nonnull JpaCteCriteriaAttribute x);

	/**
	 * Create a search ordering by the descending value of the CTE attribute.
	 * @param x The CTE attribute used to define the ordering
	 * @return the descending ordering corresponding to the CTE attribute
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaSearchOrder desc(@Nonnull JpaCteCriteriaAttribute x);

	/**
	 * Create a search ordering by the ascending value of the CTE attribute.
	 * @param x The CTE attribute used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return the ascending ordering corresponding to the CTE attribute
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaSearchOrder asc(@Nonnull JpaCteCriteriaAttribute x, boolean nullsFirst);

	/**
	 * Create a search ordering by the descending value of the CTE attribute.
	 * @param x The CTE attribute used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return the descending ordering corresponding to the CTE attribute
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaSearchOrder desc(@Nonnull JpaCteCriteriaAttribute x, boolean nullsFirst);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Non-standard HQL functions

	/**
	 * Embed native {@code pattern} that will be unquoted and embedded in the generated SQL.
	 * Occurrences of {@code ?} in the pattern are replaced with the remaining {@code arguments}
	 * of the function.
	 *
	 * @param pattern The native SQL pattern
	 * @param type The type of this expression
	 * @param arguments The optional arguments to the SQL pattern
	 * @param <T> The type of this expression
	 *
	 * @return the native SQL expression
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> sql(@Nonnull String pattern, @Nonnull Class<T> type, @Nonnull Expression<?>... arguments);

	/**
	 * Format a date, time, or datetime according to a pattern.
	 * The pattern must be written in a subset of the pattern language defined by
	 * Java’s {@link java.time.format.DateTimeFormatter}.
	 * <p>
	 * See {@link org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport#appendFormat}
	 * for a full list of pattern elements.
	 *
	 * @param datetime The datetime expression to format
	 * @param pattern The pattern to use for formatting
	 *
	 * @return the format expression
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> format(@Nonnull Expression<? extends TemporalAccessor> datetime, @Nonnull String pattern);

	/**
	 * Extracts the {@link TemporalUnit#YEAR} of a date, time, or datetime expression.
	 *
	 * @param datetime The date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<Integer> year(@Nonnull Expression<? extends TemporalAccessor> datetime);

	/**
	 * Extracts the {@link TemporalUnit#MONTH} of a date, time, or datetime expression.
	 *
	 * @param datetime The date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<Integer> month(@Nonnull Expression<? extends TemporalAccessor> datetime);

	/**
	 * Extracts the {@link TemporalUnit#DAY} of a date, time, or datetime expression.
	 *
	 * @param datetime The date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<Integer> day(@Nonnull Expression<? extends TemporalAccessor> datetime);

	/**
	 * Extracts the {@link TemporalUnit#HOUR} of a date, time, or datetime expression.
	 *
	 * @param datetime The date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<Integer> hour(@Nonnull Expression<? extends TemporalAccessor> datetime);

	/**
	 * Extracts the {@link TemporalUnit#MINUTE} of a date, time, or datetime expression.
	 *
	 * @param datetime The date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<Integer> minute(@Nonnull Expression<? extends TemporalAccessor> datetime);

	/**
	 * Extracts the {@link TemporalUnit#SECOND} of a date, time, or datetime expression.
	 *
	 * @param datetime The date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<Float> second(@Nonnull Expression<? extends TemporalAccessor> datetime);

	/**
	 * Truncates a date, time or datetime expression to the given {@link TemporalUnit}.
	 * Supported units are: {@code YEAR}, {@code MONTH}, {@code DAY},  {@code HOUR}, {@code MINUTE}, {@code SECOND}.
	 * <p>
	 * Truncating translates to obtaining a value of the same type in which all temporal units smaller than {@code field} have been pruned.
	 * For hours, minutes and second this means setting them to {@code 00}. For months and days, this means setting them to {@code 01}.
	 *
	 * @param datetime The date, time, or datetime expression to truncate
	 * @param temporalUnit The temporal unit for truncation
	 *
	 * @return the truncated value
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T extends TemporalAccessor> JpaFunction<T> truncate(@Nonnull Expression<T> datetime, @Nonnull TemporalUnit temporalUnit);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nullable String replacement, int start);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nonnull Expression<String> replacement, int start);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nullable String replacement, @Nonnull Expression<Integer> start);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nonnull Expression<String> replacement, @Nonnull Expression<Integer> start);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nullable String replacement, int start, int length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nonnull Expression<String> replacement, int start, int length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nullable String replacement, @Nonnull Expression<Integer> start, int length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			@Nonnull Expression<Integer> start,
			int length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nullable String replacement, int start, @Nullable Expression<Integer> length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			int start,
			@Nullable Expression<Integer> length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nullable String replacement,
			@Nonnull Expression<Integer> start,
			@Nullable Expression<Integer> length);

	/**
	 * Overlay the {@code string} expression with the {@code replacement} expression,
	 * starting from index {@code start} and substituting a number of characters
	 * corresponding to the length of the {@code replacement} expression or the
	 * {@code length} parameter if specified.
	 *
	 * @param string The string expression to manipulate
	 * @param replacement The replacement string expression
	 * @param start The start position
	 * @param length The optional number of characters to substitute
	 *
	 * @return the overlay expression
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			@Nonnull Expression<Integer> start,
			@Nullable Expression<Integer> length);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(@Nonnull Expression<String> x, int length);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, int length);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, @Nonnull Expression<Integer> length);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(@Nonnull Expression<String> x, int length, char padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, int length, char padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length, char padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, @Nonnull Expression<Integer> length, char padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(@Nonnull Expression<String> x, int length, @Nullable Expression<Character> padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, int length, @Nullable Expression<Character> padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length, @Nullable Expression<Character> padChar);

	/**
	 * Pad the specified string expression with whitespace or with the {@code padChar} character if specified.
	 * Optionally pass a {@link jakarta.persistence.criteria.CriteriaBuilder.Trimspec} to pad the
	 * string expression with {@code LEADING} or {@code TRAILING} (default) characters.
	 *
	 * @param ts The optional {@link jakarta.persistence.criteria.CriteriaBuilder.Trimspec}
	 * @param x The string expression to pad
	 * @param length The length of the result string after padding
	 * @param padChar The optional pad character
	 *
	 * @return the pad expression
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> pad(
			@Nullable Trimspec ts,
			@Nonnull Expression<String> x,
			@Nonnull Expression<Integer> length,
			@Nullable Expression<Character> padChar);

	/**
	 * Concatenate the given string expression with itself the given number of times.
	 *
	 * @param x The string expression to concatenate
	 * @param times The number of times it should be repeated
	 *
	 * @return the repeat expression
	 */
	@Nonnull
	JpaFunction<String> repeat(@Nonnull Expression<String> x, @Nonnull Expression<Integer> times);

	/**
	 * Concatenate the given string expression with itself the given number of times.
	 *
	 * @param x The string expression to concatenate
	 * @param times The number of times it should be repeated
	 *
	 * @return the repeat expression
	 */
	@Nonnull
	JpaFunction<String> repeat(@Nonnull Expression<String> x, int times);

	/**
	 * Concatenate the given string expression with itself the given number of times.
	 *
	 * @param x The string expression to concatenate
	 * @param times The number of times it should be repeated
	 *
	 * @return the repeat expression
	 */
	@Nonnull
	JpaFunction<String> repeat(@Nullable String x, @Nonnull Expression<Integer> times);

	/**
	 * @see #left(Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> left(@Nonnull Expression<String> x, int length);

	/**
	 * Extract the {@code length} leftmost characters of a string.
	 *
	 * @param x The original string
	 * @param length The number of characters
	 *
	 * @return the left expression
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> left(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length);

	/**
	 * @see #right(Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> right(@Nonnull Expression<String> x, int length);

	/**
	 * Extract the {@code length} rightmost characters of a string.
	 *
	 * @param x The original string
	 * @param length The number of characters
	 *
	 * @return the left expression
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> right(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length);

	/**
	 * @see #replace(Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> replace(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull String replacement);

	/**
	 * @see #replace(Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> replace(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull Expression<String> replacement);

	/**
	 * @see #replace(Expression, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> replace(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull String replacement);

	/**
	 * Replace all occurrences of {@code pattern} within the original string with {@code replacement}.
	 *
	 * @param x The original string
	 * @param pattern The string to be replaced
	 * @param replacement The new replacement string
	 *
	 * @return the replace expression
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> replace(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<String> replacement);

	/**
	 * Create an expression that applies the named collation to a string expression.
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaFunction<String> collate(@Nonnull Expression<String> x, @Nonnull String collation);

	/**
	 * Create an expression that returns the base-10 logarithm
	 * of its argument.
	 *
	 * @param x The expression
	 *
	 * @return the base-10 logarithm
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> log10(@Nonnull Expression<? extends Number> x);

	/**
	 * @see #log(Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> log(@Nullable Number b, @Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the logarithm of {@code x} to the base {@code b}.
	 *
	 * @param b The base
	 * @param x The expression
	 *
	 * @return the arbitrary-base logarithm
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> log(@Nonnull Expression<? extends Number> b, @Nonnull Expression<? extends Number> x);

	/**
	 * Literal expression corresponding to the value of pi.
	 *
	 * @return the pi expression
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> pi();

	/**
	 * Create an expression that returns the sine of its argument.
	 *
	 * @param x The expression
	 *
	 * @return the sine
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> sin(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the cosine of its argument.
	 *
	 * @param x The expression
	 *
	 * @return the cosine
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> cos(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the tangent of its argument.
	 *
	 * @param x The expression
	 *
	 * @return the tangent
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> tan(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the inverse sine of its argument.
	 *
	 * @param x The expression
	 *
	 * @return the inverse sine
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> asin(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the inverse cosine of its argument.
	 *
	 * @param x The expression
	 *
	 * @return the inverse cosine
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> acos(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the inverse tangent of its argument.
	 *
	 * @param x The expression
	 *
	 * @return the inverse tangent
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> atan(@Nonnull Expression<? extends Number> x);

	/**
	 * @see #atan2(Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> atan2(@Nullable Number y, @Nonnull Expression<? extends Number> x);

	/**
	 * @see #atan2(Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> atan2(@Nonnull Expression<? extends Number> y, @Nullable Number x);

	/**
	 * Create an expression that returns the inverse tangent of {@code y} over {@code x}.
	 *
	 * @param y The y coordinate
	 * @param x The x coordinate
	 *
	 * @return the 2-argument inverse tangent
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> atan2(@Nonnull Expression<? extends Number> y, @Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the hyperbolic sine of its argument.
	 *
	 * @param x The expression
	 *
	 * @return the hyperbolic sine
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> sinh(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the hyperbolic cosine of its argument.
	 *
	 * @param x The expression
	 *
	 * @return the hyperbolic cosine
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> cosh(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that returns the hyperbolic tangent of its argument.
	 *
	 * @param x The expression
	 *
	 * @return the hyperbolic tangent
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> tanh(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that converts an angle measured in radians
	 * to an approximately equivalent angle measured in degrees.
	 *
	 * @param x The expression
	 *
	 * @return the angle in degrees
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> degrees(@Nonnull Expression<? extends Number> x);

	/**
	 * Create an expression that converts an angle measured in degrees
	 * to an approximately equivalent angle measured in radians.
	 *
	 * @param x The expression
	 *
	 * @return the angle in radians
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> radians(@Nonnull Expression<? extends Number> x);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Window functions

	/**
	 * Create an empty {@link JpaWindow} to use with window and aggregate functions.
	 *
	 * @return the empty window
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaWindow createWindow();

	/**
	 * Create a window frame of type {@link FrameKind#UNBOUNDED_PRECEDING} to use with {@link JpaWindow}s.
	 *
	 * @return the window frame
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaWindowFrame frameUnboundedPreceding();

	/**
	 * @see #frameBetweenPreceding(Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaWindowFrame frameBetweenPreceding(int offset);

	/**
	 * Create window frame of type {@link FrameKind#OFFSET_PRECEDING} to use with {@link JpaWindow}s.
	 *
	 * @param offset The {@code offset} expression
	 *
	 * @return the window frame
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaWindowFrame frameBetweenPreceding(@Nonnull Expression<?> offset);

	/**
	 * Create a window frame of type {@link FrameKind#CURRENT_ROW} to use with {@link JpaWindow}s.
	 *
	 * @return the window frame
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaWindowFrame frameCurrentRow();

	/**
	 * @see #frameBetweenFollowing(Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaWindowFrame frameBetweenFollowing(int offset);

	/**
	 * Create a window frame of type {@link FrameKind#OFFSET_FOLLOWING} to use with {@link JpaWindow}s.
	 *
	 * @param offset The {@code offset} expression
	 *
	 * @return the window frame
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaWindowFrame frameBetweenFollowing(@Nonnull Expression<?> offset);

	/**
	 * Create a window frame of type {@link FrameKind#UNBOUNDED_FOLLOWING} to use with {@link JpaWindow}s.
	 *
	 * @return the window frame
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaWindowFrame frameUnboundedFollowing();

	/**
	 * Create a generic window function expression that will be applied
	 * over the specified {@link JpaWindow window}.
	 *
	 * @param name The name of the window function
	 * @param type The type of this expression
	 * @param window The window over which the function will be applied
	 * @param args The arguments to the function
	 * @param <T> The type of this expression
	 *
	 * @return the window function expression
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> windowFunction(@Nonnull String name, @Nullable Class<T> type, @Nonnull JpaWindow window, @Nonnull Expression<?>... args);

	/**
	 * Create a {@code row_number} window function expression.
	 *
	 * @param window The window over which the function will be applied
	 *
	 * @return the window function expression
	 *
	 * @see #windowFunction
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Long> rowNumber(@Nonnull JpaWindow window);

	/**
	 * Create a {@code first_value} window function expression.
	 *
	 * @param argument The argument expression to pass to {@code first_value}
	 * @param window The window over which the function will be applied
	 * @param <T> The type of the expression
	 *
	 * @return the window function expression
	 *
	 * @see #windowFunction
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> firstValue(@Nonnull Expression<T> argument, @Nonnull JpaWindow window);

	/**
	 * Create a {@code last_value} window function expression.
	 *
	 * @param argument The argument expression to pass to {@code last_value}
	 * @param window The window over which the function will be applied
	 * @param <T> The type of the expression
	 *
	 * @return the window function expression
	 *
	 * @see #windowFunction
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> lastValue(@Nonnull Expression<T> argument, @Nonnull JpaWindow window);

	/**
	 * @see #nthValue(Expression, Expression, JpaWindow) nthValue
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> nthValue(@Nonnull Expression<T> argument, int n, @Nonnull JpaWindow window);

	/**
	 * Create a {@code nth_value} window function expression.
	 *
	 * @param argument The argument expression to pass to {@code nth_value}
	 * @param n The {@code N} argument for the function
	 * @param window The window over which the function will be applied
	 * @param <T> The type of the expression
	 *
	 * @return the window function expression
	 *
	 * @see #windowFunction
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> nthValue(@Nonnull Expression<T> argument, @Nonnull Expression<Integer> n, @Nonnull JpaWindow window);

	/**
	 * Create a {@code rank} window function expression.
	 *
	 * @param window The window over which the function will be applied
	 *
	 * @return the window function expression
	 *
	 * @see #windowFunction
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Long> rank(@Nonnull JpaWindow window);

	/**
	 * Create a {@code dense_rank} window function expression.
	 *
	 * @param window The window over which the function will be applied
	 *
	 * @return the window function expression
	 *
	 * @see #windowFunction
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Long> denseRank(@Nonnull JpaWindow window);

	/**
	 * Create a {@code percent_rank} window function expression.
	 *
	 * @param window The window over which the function will be applied
	 *
	 * @return the window function expression
	 *
	 * @see #windowFunction
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> percentRank(@Nonnull JpaWindow window);

	/**
	 * Create a {@code cume_dist} window function expression.
	 *
	 * @param window The window over which the function will be applied
	 *
	 * @return the window function expression
	 *
	 * @see #windowFunction
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> cumeDist(@Nonnull JpaWindow window);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Aggregate functions

	/**
	 * @see #functionAggregate(String, Class, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> functionAggregate(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<?>... args);

	/**
	 * @see #functionAggregate(String, Class, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> functionAggregate(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... args);

	/**
	 * Create a generic aggregate function expression.
	 *
	 * @param name The name of the ordered set-aggregate function
	 * @param type The type of this expression
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param args The optional arguments to the function
	 * @param <T> The type of this expression
	 *
	 * @return the aggregate function expression
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> functionAggregate(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... args);

	/**
	 * @see #sum(Expression, JpaPredicate, JpaWindow)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<N extends Number> JpaExpression<Number> sum(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter);

	/**
	 * @see #sum(Expression, JpaPredicate, JpaWindow)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<N extends Number> JpaExpression<Number> sum(@Nonnull Expression<N> argument, @Nullable JpaWindow window);

	/**
	 * Create a {@code sum} aggregate function expression.
	 *
	 * @param argument The argument to the function
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param <N> The type of the input expression
	 *
	 * @return the aggregate function expression
	 *
	 * @see #functionAggregate(String, Class, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<N extends Number> JpaExpression<Number> sum(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter, @Nullable JpaWindow window);

	/**
	 * @see #avg(Expression, JpaPredicate, JpaWindow)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<N extends Number> JpaExpression<Double> avg(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter);

	/**
	 * @see #avg(Expression, JpaPredicate, JpaWindow)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<N extends Number> JpaExpression<Double> avg(@Nonnull Expression<N> argument, @Nullable JpaWindow window);

	/**
	 * Create an {@code avg} aggregate function expression.
	 *
	 * @param argument The argument to the function
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param <N> The type of the input expression
	 *
	 * @return the aggregate function expression
	 *
	 * @see #functionAggregate(String, Class, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<N extends Number> JpaExpression<Double> avg(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter, @Nullable JpaWindow window);

	/**
	 * @see #count(Expression, JpaPredicate, JpaWindow)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Long> count(@Nonnull Expression<?> argument, @Nullable JpaPredicate filter);

	/**
	 * @see #count(Expression, JpaPredicate, JpaWindow)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Long> count(@Nonnull Expression<?> argument, @Nullable JpaWindow window);

	/**
	 * Create a {@code count} aggregate function expression.
	 *
	 * @param argument The argument to the function
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 *
	 * @return the aggregate function expression
	 *
	 * @see #functionAggregate(String, Class, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Long> count(@Nonnull Expression<?> argument, @Nullable JpaPredicate filter, @Nullable JpaWindow window);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordered-Set Aggregate functions

	/**
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> functionWithinGroup(@Nonnull String name, @Nullable Class<T> type, @Nullable JpaOrder order, @Nonnull Expression<?>... args);

	/**
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> functionWithinGroup(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<?>... args);

	/**
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> functionWithinGroup(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaOrder order,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... args);

	/**
	 * Create a generic ordered set-aggregate function expression.
	 *
	 * @param name The name of the ordered set-aggregate function
	 * @param type The type of this expression
	 * @param order The order-by clause used in the within group
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param args The optional arguments to the function
	 * @param <T> The type of this expression
	 *
	 * @return the ordered set-aggregate function expression
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> functionWithinGroup(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... args);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> listagg(@Nullable JpaOrder order, @Nonnull Expression<String> argument, @Nonnull String separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> listagg(@Nullable JpaOrder order, @Nonnull Expression<String> argument, @Nonnull Expression<String> separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> listagg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<String> argument, @Nonnull String separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<String> argument,
			@Nonnull Expression<String> separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> listagg(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<String> argument, @Nonnull String separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaWindow window,
			@Nonnull Expression<String> argument,
			@Nonnull Expression<String> separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<String> argument,
			@Nonnull String separator);

	/**
	 * Create a {@code listagg} ordered set-aggregate function expression.
	 *
	 * @param order The order-by clause used in the within group
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param argument The values to join
	 * @param separator The separator used to join the values
	 *
	 * @return the ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<String> argument,
			@Nonnull Expression<String> separator);

	/**
	 * @see #mode(JpaPredicate, JpaWindow, Expression, SortDirection, Nulls)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> mode(@Nonnull Expression<T> sortExpression, @Nonnull SortDirection sortOrder, @Nonnull Nulls nullPrecedence);

	/**
	 * @see #mode(JpaPredicate, JpaWindow, Expression, SortDirection, Nulls)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> mode(
			@Nullable JpaPredicate filter,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence);

	/**
	 * @see #mode(JpaPredicate, JpaWindow, Expression, SortDirection, Nulls)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> mode(
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence);

	/**
	 * Create a {@code mode} ordered set-aggregate function expression.
	 *
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param sortExpression The sort expression
	 * @param sortOrder The sort order
	 * @param nullPrecedence The null precedence
	 * @param <T> The type of this expression
	 *
	 * @return the ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> mode(
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence);

	/**
	 * @see #percentileCont(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, Nulls)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence);

	/**
	 * @see #percentileCont(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, Nulls)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence);

	/**
	 * @see #percentileCont(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, Nulls)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence);

	/**
	 * Create a {@code percentile_cont} ordered set-aggregate function expression.
	 *
	 * @param argument The argument to the function
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param sortExpression The sort expression
	 * @param sortOrder The sort order
	 * @param nullPrecedence The null precedence
	 *
	 * @return the ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence);

	/**
	 * @see #percentileDisc(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, Nulls)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence);

	/**
	 * @see #percentileDisc(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, Nulls)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence);

	/**
	 * @see #percentileDisc(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, Nulls)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence);

	/**
	 * Create a {@code percentile_disc} ordered set-aggregate function expression.
	 *
	 * @param argument The argument to the function
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param sortExpression The sort expression
	 * @param sortOrder The sort order
	 * @param nullPrecedence The null precedence
	 *
	 * @return the ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence);

	/**
	 * @see #rank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Long> rank(@Nullable JpaOrder order, @Nonnull Expression<?>... arguments);

	/**
	 * @see #rank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Long> rank(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<?>... arguments);

	/**
	 * @see #rank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Long> rank(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<?>... arguments);

	/**
	 * Create a {@code rank} ordered set-aggregate function expression.
	 *
	 * @param order The order-by clause used in the within group
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param arguments The arguments to the function
	 *
	 * @return the ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Long> rank(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nullable JpaWindow window, @Nonnull Expression<?>... arguments);

	/**
	 * @see #percentRank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> percentRank(@Nullable JpaOrder order, @Nonnull Expression<?>... arguments);

	/**
	 * @see #percentRank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> percentRank(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<?>... arguments);

	/**
	 * @see #percentRank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> percentRank(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<?>... arguments);

	/**
	 * Create a {@code percent_rank} ordered set-aggregate function expression.
	 *
	 * @param order The order-by clause used in the within group
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param arguments The arguments to the function
	 *
	 * @return the ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Double> percentRank(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... arguments);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Array functions for array types

	/**
	 * @see #arrayAgg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nonnull Expression<? extends T> argument);

	/**
	 * @see #arrayAgg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<? extends T> argument);

	/**
	 * @see #arrayAgg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<? extends T> argument);

	/**
	 * Create a {@code array_agg} ordered set-aggregate function expression.
	 *
	 * @param order The order-by clause used in the within group
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param argument The values to aggregate
	 *
	 * @return the ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayAgg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<? extends T> argument);

	/**
	 * Creates an array literal with the {@code array} constructor function.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayLiteral(@Nullable T... elements);

	/**
	 * Determines the length of an array.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<Integer> arrayLength(@Nonnull Expression<T[]> arrayExpression);

	/**
	 * Determines the 1-based position of an element in an array.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<Integer> arrayPosition(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	/**
	 * Determines the 1-based position of an element in an array.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<Integer> arrayPosition(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	/**
	 * Determines all 1-based positions of an element in an array.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<int[]> arrayPositions(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	/**
	 * Determines all 1-based positions of an element in an array.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<int[]> arrayPositions(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	/**
	 * Determines all 1-based positions of an element in an array.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<List<Integer>> arrayPositionsList(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	/**
	 * Determines all 1-based positions of an element in an array.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<List<Integer>> arrayPositionsList(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	/**
	 * Concatenates arrays with each other in order.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayConcat(@Nonnull Expression<T[]> arrayExpression1, @Nonnull Expression<T[]> arrayExpression2);

	/**
	 * Concatenates arrays with each other in order.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayConcat(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2);

	/**
	 * Concatenates arrays with each other in order.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayConcat(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2);

	/**
	 * Appends element to array.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayAppend(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	/**
	 * Appends element to array.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayAppend(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	/**
	 * Prepends element to array.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayPrepend(@Nonnull Expression<T> elementExpression, @Nonnull Expression<T[]> arrayExpression);

	/**
	 * Prepends element to array.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayPrepend(@Nullable T element, @Nonnull Expression<T[]> arrayExpression);

	/**
	 * Accesses the element of an array by 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> arrayGet(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> indexExpression);

	/**
	 * Accesses the element of an array by 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> arrayGet(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySet(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> indexExpression, @Nonnull Expression<T> elementExpression);
	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySet(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> indexExpression, @Nullable T element);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySet(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index, @Nonnull Expression<T> elementExpression);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySet(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index, @Nullable T element);

	/**
	 * Creates array copy with given element removed.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayRemove(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	/**
	 * Creates array copy with given element removed.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayRemove(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	/**
	 * Creates array copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayRemoveIndex(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> indexExpression);

	/**
	 * Creates array copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayRemoveIndex(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySlice(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> lowerIndexExpression, @Nonnull Expression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySlice(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> lowerIndexExpression, @Nullable Integer upperIndex);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySlice(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer lowerIndex, @Nonnull Expression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySlice(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer lowerIndex, @Nullable Integer upperIndex);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayReplace(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> oldElementExpression, @Nonnull Expression<T> newElementExpression);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayReplace(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> oldElementExpression, @Nullable T newElement);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayReplace(@Nonnull Expression<T[]> arrayExpression, @Nullable T oldElement, @Nonnull Expression<T> newElementExpression);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayReplace(@Nonnull Expression<T[]> arrayExpression, @Nullable T oldElement, @Nullable T newElement);

	/**
	 * Creates array copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayTrim(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> elementCountExpression);

	/**
	 * Creates array copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayTrim(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer elementCount);

	/**
	 * Reverses the order of elements in an array.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayReverse(@Nonnull Expression<T[]> arrayExpression);

	/**
	 * Sorts the elements of an array in ascending order.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression);

	/**
	 * Sorts the elements of an array in the specified order.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, boolean descending);

	/**
	 * Sorts the elements of an array in the specified order.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Boolean> descendingExpression);

	/**
	 * Create an expression that sorts the given array with explicit null ordering.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, boolean descending, boolean nullsFirst);

	/**
	 * Create an expression that sorts the given array with explicit null ordering.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Boolean> descendingExpression, @Nonnull Expression<Boolean> nullsFirstExpression);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayFill(@Nonnull Expression<T> elementExpression, @Nonnull Expression<Integer> elementCountExpression);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayFill(@Nonnull Expression<T> elementExpression, @Nullable Integer elementCount);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayFill(@Nullable T element, @Nonnull Expression<Integer> elementCountExpression);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T[]> arrayFill(@Nullable T element, @Nullable Integer elementCount);

	/**
	 * Concatenates the non-null array elements with a separator, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nonnull Expression<String> separatorExpression);

	/**
	 * Concatenates the non-null array elements with a separator, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nullable String separator);

	/**
	 * Concatenates the array elements with a separator, as specified by the arguments. Null array elements are replaced
	 * with the given default element.
	 *
	 * @since 7.1
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nonnull Expression<String> separatorExpression, @Nonnull Expression<String> defaultExpression);

	/**
	 * Concatenates the array elements with a separator, as specified by the arguments. Null array elements are replaced
	 * with the given default element.
	 *
	 * @since 7.1
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nonnull Expression<String> separatorExpression, @Nullable String defaultValue);

	/**
	 * Concatenates the array elements with a separator, as specified by the arguments. Null array elements are replaced
	 * with the given default element.
	 *
	 * @since 7.1
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nullable String separator, @Nonnull Expression<String> defaultExpression);

	/**
	 * Concatenates the array elements with a separator, as specified by the arguments. Null array elements are replaced
	 * with the given default element.
	 *
	 * @since 7.1
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nullable String separator, @Nullable String defaultValue);

	/**
	 * Whether an array contains an element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayContains(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	/**
	 * Whether an array contains an element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayContains(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	/**
	 * Whether an array contains an element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayContains(@Nullable T[] array, @Nonnull Expression<T> elementExpression);

	/**
	 * Whether an array contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayContainsNullable(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression);

	/**
	 * Whether an array contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayContainsNullable(@Nonnull Expression<T[]> arrayExpression, @Nullable T element);

	/**
	 * Whether an array contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayContainsNullable(@Nullable T[] array, @Nonnull Expression<T> elementExpression);

	/**
	 * Whether an array is a subset of another array.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIncludes(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T[]> subArrayExpression);

	/**
	 * Whether an array is a subset of another array.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIncludes(@Nonnull Expression<T[]> arrayExpression, @Nullable T[] subArray);

	/**
	 * Whether an array is a subset of another array.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIncludes(@Nullable T[] array, @Nonnull Expression<T[]> subArrayExpression);

	/**
	 * Whether an array is a subset of another array with nullable elements.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIncludesNullable(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T[]> subArrayExpression);

	/**
	 * Whether an array is a subset of another array with nullable elements.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIncludesNullable(@Nonnull Expression<T[]> arrayExpression, @Nullable T[] subArray);

	/**
	 * Whether an array is a subset of another array with nullable elements.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIncludesNullable(@Nullable T[] array, @Nonnull Expression<T[]> subArrayExpression);

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIntersects(@Nonnull Expression<T[]> arrayExpression1, @Nonnull Expression<T[]> arrayExpression2);

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIntersects(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2);

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIntersects(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2);

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIntersectsNullable(@Nonnull Expression<T[]> arrayExpression1, @Nonnull Expression<T[]> arrayExpression2);

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIntersectsNullable(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2);

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaPredicate arrayIntersectsNullable(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Array functions for collection types

	/**
	 * Creates a basic collection literal with the {@code array} constructor function.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<E>> JpaExpression<C> collectionLiteral(@Nullable E... elements);

	/**
	 * Determines the length of a basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Integer> collectionLength(@Nonnull Expression<? extends Collection<?>> collectionExpression);

	/**
	 * Determines the 1-based position of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaExpression<Integer> collectionPosition(@Nonnull Expression<? extends Collection<? extends E>> collectionExpression, @Nullable E element);

	/**
	 * Determines the 1-based position of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaExpression<Integer> collectionPosition(@Nonnull Expression<? extends Collection<? extends E>> collectionExpression, @Nonnull Expression<E> elementExpression);

	/**
	 * Determines all 1-based positions of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<int[]> collectionPositions(@Nonnull Expression<? extends Collection<? super T>> collectionExpression, @Nonnull Expression<T> elementExpression);

	/**
	 * Determines all 1-based positions of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<int[]> collectionPositions(@Nonnull Expression<? extends Collection<? super T>> collectionExpression, @Nullable T element);

	/**
	 * Determines all 1-based positions of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<List<Integer>> collectionPositionsList(@Nonnull Expression<? extends Collection<? super T>> collectionExpression, @Nonnull Expression<T> elementExpression);

	/**
	 * Determines all 1-based positions of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<List<Integer>> collectionPositionsList(@Nonnull Expression<? extends Collection<? super T>> collectionExpression, @Nullable T element);

	/**
	 * Concatenates basic collections with each other in order.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(@Nonnull Expression<C> collectionExpression1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Concatenates basic collections with each other in order.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(@Nonnull Expression<C> collectionExpression1, @Nullable Collection<? extends E> collection2);

	/**
	 * Concatenates basic collections with each other in order.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(@Nullable C collection1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Appends element to basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionAppend(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<? extends E> elementExpression);

	/**
	 * Appends element to basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionAppend(@Nonnull Expression<C> collectionExpression, @Nullable E element);

	/**
	 * Prepends element to basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionPrepend(@Nonnull Expression<? extends E> elementExpression, @Nonnull Expression<C> collectionExpression);

	/**
	 * Prepends element to basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionPrepend(@Nullable E element, @Nonnull Expression<C> collectionExpression);

	/**
	 * Accesses the element of the basic collection by 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaExpression<E> collectionGet(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nonnull Expression<Integer> indexExpression);

	/**
	 * Accesses the element of the basic collection by 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaExpression<E> collectionGet(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable Integer index);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionSet(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<Integer> indexExpression, @Nonnull Expression<? extends E> elementExpression);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionSet(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<Integer> indexExpression, @Nullable E element);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionSet(@Nonnull Expression<C> collectionExpression, @Nullable Integer index, @Nonnull Expression<? extends E> elementExpression);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionSet(@Nonnull Expression<C> collectionExpression, @Nullable Integer index, @Nullable E element);

	/**
	 * Creates basic collection copy with given element removed.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionRemove(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<? extends E> elementExpression);

	/**
	 * Creates basic collection copy with given element removed.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionRemove(@Nonnull Expression<C> collectionExpression, @Nullable E element);

	/**
	 * Creates basic collection copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionRemoveIndex(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<Integer> indexExpression);

	/**
	 * Creates basic collection copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionRemoveIndex(@Nonnull Expression<C> collectionExpression, @Nullable Integer index);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionSlice(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<Integer> lowerIndexExpression, @Nonnull Expression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionSlice(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<Integer> lowerIndexExpression, @Nullable Integer upperIndex);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionSlice(@Nonnull Expression<C> collectionExpression, @Nullable Integer lowerIndex, @Nonnull Expression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionSlice(@Nonnull Expression<C> collectionExpression, @Nullable Integer lowerIndex, @Nullable Integer upperIndex);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<? extends E> oldElementExpression, @Nonnull Expression<? extends E> newElementExpression);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(@Nonnull Expression<C> collectionExpression, @Nonnull Expression<? extends E> oldElementExpression, @Nullable E newElement);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(@Nonnull Expression<C> collectionExpression, @Nullable E oldElement, @Nonnull Expression<? extends E> newElementExpression);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(@Nonnull Expression<C> collectionExpression, @Nullable E oldElement, @Nullable E newElement);

	/**
	 * Creates basic collection copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionTrim(@Nonnull Expression<C> arrayExpression, @Nonnull Expression<Integer> elementCountExpression);

	/**
	 * Creates basic collection copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionTrim(@Nonnull Expression<C> arrayExpression, @Nullable Integer elementCount);

	/**
	 * Create an expression that reverses the order of the elements of a collection.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionReverse(@Nonnull Expression<C> collectionExpression);

	/**
	 * Create an expression that sorts the elements of a collection.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionSort(@Nonnull Expression<C> collectionExpression);

	/**
	 * Create an expression that sorts the given collection in specified order.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionSort(@Nonnull Expression<C> collectionExpression, boolean descending);

	/**
	 * Create an expression that sorts the given collection in specified order.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Boolean> descendingExpression);

	/**
	 * Create an expression that sorts the given collection with explicit null ordering.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			boolean descending,
			boolean nullsFirst);

	/**
	 * Create an expression that sorts the given collection with explicit null ordering.
	 *
	 * @since 7.2
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<C extends Collection<?>> JpaExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Boolean> descendingExpression,
			@Nonnull Expression<Boolean> nullsFirstExpression);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<Collection<T>> collectionFill(@Nonnull Expression<T> elementExpression, @Nonnull Expression<Integer> elementCountExpression);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<Collection<T>> collectionFill(@Nonnull Expression<T> elementExpression, @Nullable Integer elementCount);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<Collection<T>> collectionFill(@Nullable T element, @Nonnull Expression<Integer> elementCountExpression);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<Collection<T>> collectionFill(@Nullable T element, @Nullable Integer elementCount);

	/**
	 * Concatenates the non-null basic collection elements with a separator, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nonnull Expression<String> separatorExpression);

	/**
	 * Concatenates the non-null basic collection elements with a separator, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nullable String separator);

	/**
	 * Concatenates the collection elements with a separator, as specified by the arguments. Null collection elements
	 * are replaced with the given default element.
	 *
	 * @since 7.1
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nonnull Expression<String> separatorExpression, @Nonnull Expression<String> defaultExpression);

	/**
	 * Concatenates the collection elements with a separator, as specified by the arguments. Null collection elements
	 * are replaced with the given default element.
	 *
	 * @since 7.1
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nonnull Expression<String> separatorExpression, @Nullable String defaultValue);

	/**
	 * Concatenates the collection elements with a separator, as specified by the arguments. Null collection elements
	 * are replaced with the given default element.
	 *
	 * @since 7.1
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nullable String separator, @Nonnull Expression<String> defaultExpression);

	/**
	 * Concatenates the collection elements with a separator, as specified by the arguments. Null collection elements
	 * are replaced with the given default element.
	 *
	 * @since 7.1
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nullable String separator, @Nullable String defaultValue);

	/**
	 * Whether a basic collection contains an element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionContains(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nonnull Expression<? extends E> elementExpression);

	/**
	 * Whether a basic collection contains an element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionContains(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable E element);

	/**
	 * Whether a basic collection contains an element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionContains(@Nonnull Collection<E> collection, @Nonnull Expression<E> elementExpression);

	/**
	 * Whether a basic collection contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionContainsNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nonnull Expression<? extends E> elementExpression);

	/**
	 * Whether a basic collection contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionContainsNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable E element);

	/**
	 * Whether a basic collection contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionContainsNullable(@Nonnull Collection<E> collection, @Nonnull Expression<E> elementExpression);

	/**
	 * Whether a basic collection is a subset of another basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIncludes(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression);

	/**
	 * Whether a basic collection is a subset of another basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIncludes(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable Collection<? extends E> subCollection);

	/**
	 * Whether a basic collection is a subset of another basic collection.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIncludes(@Nullable Collection<E> collection, @Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression);

	/**
	 * Whether a basic collection is a subset of another basic collection with nullable elements.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIncludesNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression);

	/**
	 * Whether a basic collection is a subset of another basic collection with nullable elements.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIncludesNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable Collection<? extends E> subCollection);

	/**
	 * Whether a basic collection is a subset of another basic collection with nullable elements.
	 *
	 * @since 6.4
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIncludesNullable(@Nullable Collection<E> collection, @Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression);

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIntersects(@Nonnull Expression<? extends Collection<E>> collectionExpression1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIntersects(@Nonnull Expression<? extends Collection<E>> collectionExpression1, @Nullable Collection<? extends E> collection2);

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIntersects(@Nullable Collection<E> collection1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIntersectsNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIntersectsNullable(@Nonnull Expression<? extends Collection<E>> collectionExpression1, @Nullable Collection<? extends E> collection2);

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaPredicate collectionIntersectsNullable(@Nullable Collection<E> collection1, @Nonnull Expression<? extends Collection<? extends E>> collectionExpression2);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JSON functions

	/**
	 * @see #jsonValue(Expression, String, Class)
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaJsonValueExpression<String> jsonValue(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath);

	/**
	 * Extracts a value by JSON path from a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaJsonValueExpression<T> jsonValue(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Class<T> returningType);

	/**
	 * @see #jsonValue(Expression, Expression, Class)
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaJsonValueExpression<String> jsonValue(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath);

	/**
	 * Extracts a value by JSON path from a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaJsonValueExpression<T> jsonValue(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Class<T> returningType);

	/**
	 * @see #jsonQuery(Expression, Expression)
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaJsonQueryExpression jsonQuery(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath);

	/**
	 * Queries values by JSON path from a JSON document.
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaJsonQueryExpression jsonQuery(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath);

	/**
	 * Checks if a JSON document contains a node for the given JSON path.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaJsonExistsExpression jsonExists(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath);

	/**
	 * Checks if a JSON document contains a node for the given JSON path.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaJsonExistsExpression jsonExists(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath);

	/**
	 * Create a JSON object from the given map of key values.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonObject(@Nonnull Map<?, ? extends Expression<?>> keyValues);

	/**
	 * Create a JSON object from the given map of key values, retaining {@code null} values in the JSON.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonObjectWithNulls(@Nonnull Map<?, ? extends Expression<?>> keyValues);

	/**
	 * Create a JSON array from the array of values.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonArray(@Nonnull Expression<?>... values);

	/**
	 * Create a JSON object from the given array of values, retaining {@code null} values in the JSON array.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonArrayWithNulls(@Nonnull Expression<?>... values);

	/**
	 * Aggregates the given value into a JSON array.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonArrayAgg(@Nonnull Expression<?> value);

	/**
	 * Aggregates the given value into a JSON array.
	 * Ordering values based on the given order by items.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nonnull JpaOrder... orderBy);

	/**
	 * Aggregates the given value into a JSON array.
	 * Filtering rows that don't match the given filter predicate.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nullable Predicate filter);

	/**
	 * Aggregates the given value into a JSON array.
	 * Filtering rows that don't match the given filter predicate.
	 * Ordering values based on the given order by items.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nullable Predicate filter, @Nonnull JpaOrder... orderBy);

	/**
	 * Aggregates the given value into a JSON array, retaining {@code null} values in the JSON array.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value);

	/**
	 * Aggregates the given value into a JSON array, retaining {@code null} values in the JSON array.
	 * Ordering values based on the given order by items.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nonnull JpaOrder... orderBy);

	/**
	 * Aggregates the given value into a JSON array, retaining {@code null} values in the JSON array.
	 * Filtering rows that don't match the given filter predicate.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nullable Predicate filter);

	/**
	 * Aggregates the given value into a JSON array, retaining {@code null} values in the JSON array.
	 * Filtering rows that don't match the given filter predicate.
	 * Ordering values based on the given order by items.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nullable Predicate filter, @Nonnull JpaOrder... orderBy);

	/**
	 * Aggregates the given value under the given key into a JSON object.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonObjectAgg(@Nonnull Expression<?> key, @Nonnull Expression<?> value);

	/**
	 * Aggregates the given value under the given key into a JSON object, retaining {@code null} values in the JSON object.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonObjectAggWithNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value);

	/**
	 * Aggregates the given value under the given key into a JSON object.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonObjectAggWithUniqueKeys(@Nonnull Expression<?> key, @Nonnull Expression<?> value);

	/**
	 * Aggregates the given value under the given key into a JSON object, retaining {@code null} values in the JSON object.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonObjectAggWithUniqueKeysAndNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value);

	/**
	 * Aggregates the given value under the given key into a JSON object.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonObjectAgg(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter);

	/**
	 * Aggregates the given value under the given key into a JSON object, retaining {@code null} values in the JSON object.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonObjectAggWithNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter);

	/**
	 * Aggregates the given value under the given key into a JSON object.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonObjectAggWithUniqueKeys(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter);

	/**
	 * Aggregates the given value under the given key into a JSON object, retaining {@code null} values in the JSON object.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonObjectAggWithUniqueKeysAndNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter);

	/**
	 * Inserts/Replaces a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value);

	/**
	 * Inserts/Replaces a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nonnull Expression<?> value);

	/**
	 * Inserts/Replaces a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value);

	/**
	 * Inserts/Replaces a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value);

	/**
	 * Removes a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonRemove(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath);

	/**
	 * Removes a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonRemove(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath);

	/**
	 * Inserts a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value);

	/**
	 * Inserts a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nonnull Expression<?> value);

	/**
	 * Inserts a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value);

	/**
	 * Inserts a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value);

	/**
	 * Replaces a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value);

	/**
	 * Replaces a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nonnull Expression<?> value);

	/**
	 * Replaces a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value);

	/**
	 * Replaces a value by JSON path within a JSON document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value);

	/**
	 * Applies the patch JSON document onto the other JSON document and returns that.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonMergepatch(@Nonnull Expression<?> document, @Nonnull Expression<?> patch);

	/**
	 * Applies the patch JSON document onto the other JSON document and returns that.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonMergepatch(@Nonnull Expression<?> document, @Nullable String patch);

	/**
	 * Applies the patch JSON document onto the other JSON document and returns that.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> jsonMergepatch(@Nullable String document, @Nonnull Expression<?> patch);

	/**
	 * Creates an XML element with the given element name.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaXmlElementExpression xmlelement(@Nonnull String elementName);

	/**
	 * Creates an XML comment with the given argument as content.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlcomment(@Nullable String comment);

	/**
	 * Creates an XML forest from the given XML element expressions.
	 *
	 * @since 7.0
	 * @see #named(Expression, String)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlforest(@Nonnull Expression<?>... elements);

	/**
	 * Creates an XML forest from the given XML element expressions.
	 *
	 * @since 7.0
	 * @see #named(Expression, String)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlforest(@Nonnull List<? extends Expression<?>> elements);

	/**
	 * Concatenates the given XML element expressions.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlconcat(@Nonnull Expression<?>... elements);

	/**
	 * Concatenates the given XML element expressions.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlconcat(@Nonnull List<? extends Expression<?>> elements);

	/**
	 * Creates an XML processing with the given name.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlpi(@Nonnull String elementName);

	/**
	 * Creates an XML processing with the given name and content.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlpi(@Nonnull String elementName, @Nonnull Expression<String> content);

	/**
	 * Queries the given XML document with the given XPath or XQuery query.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlquery(@Nullable String query, @Nonnull Expression<?> xmlDocument);

	/**
	 * Queries the given XML document with the given XPath or XQuery query.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlquery(@Nonnull Expression<String> query, @Nonnull Expression<?> xmlDocument);

	/**
	 * Checks if the given XPath or XQuery query exists in the given XML document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Boolean> xmlexists(@Nullable String query, @Nonnull Expression<?> xmlDocument);

	/**
	 * Checks if the given XPath or XQuery query exists in the given XML document.
	 *
	 * @since 7.0
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<Boolean> xmlexists(@Nonnull Expression<String> query, @Nonnull Expression<?> xmlDocument);

	/**
	 * @see #xmlagg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlagg(@Nullable JpaOrder order, @Nonnull Expression<?> argument);

	/**
	 * @see #xmlagg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlagg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<?> argument);

	/**
	 * @see #xmlagg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlagg(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<?> argument);

	/**
	 * Create a {@code xmlagg} ordered set-aggregate function expression.
	 *
	 * @param order The order-by clause used in the within group
	 * @param filter The optional filter clause
	 * @param window The optional window over which to apply the function
	 * @param argument The values to join
	 *
	 * @return the ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaExpression<String> xmlagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<?> argument);

	/**
	 * Creates a named expression. The name is important for the result of the expression,
	 * e.g. when building an {@code xmlforest}, the name acts as the XML element name.
	 *
	 * @since 7.0
	 * @see #xmlforest(Expression[])
	 * @see #xmlforest(List)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<T> JpaExpression<T> named(@Nonnull Expression<T> expression, @Nonnull String name);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Set-Returning functions

	/**
	 * Create a new set-returning function expression.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaSetReturningFunction<E> setReturningFunction(@Nonnull String name, @Nonnull Expression<?>... args);

	/**
	 * Creates an unnest function expression to turn an array into a set of rows.
	 *
	 * @since 7.0
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaSetReturningFunction<E> unnestArray(@Nonnull Expression<E[]> array);

	/**
	 * Creates an unnest function expression to turn an array into a set of rows.
	 *
	 * @since 7.0
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E> JpaSetReturningFunction<E> unnestCollection(@Nonnull Expression<? extends Collection<E>> collection);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nonnull Expression<E> step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nonnull Expression<E> step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nullable E step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nullable E step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nullable E step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop, @Nonnull Expression<E> step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop, @Nullable E step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nonnull Expression<E> step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nonnull Expression<? extends TemporalAmount> step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nonnull Expression<? extends TemporalAmount> step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nullable E stop, @Nonnull Expression<? extends TemporalAmount> step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nullable TemporalAmount step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nullable TemporalAmount step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nullable TemporalAmount step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nullable E stop, @Nullable TemporalAmount step);

	/**
	 * Creates a {@code generate_series} function expression to generate a set of values as rows.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	<E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nonnull Expression<? extends TemporalAmount> step);

	/**
	 * Creates a {@code json_table} function expression to generate rows from JSON array elements.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaJsonTableFunction jsonTable(@Nonnull Expression<?> jsonDocument);

	/**
	 * Creates a {@code json_table} function expression to generate rows from JSON array elements.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaJsonTableFunction jsonTable(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath);

	/**
	 * Creates a {@code json_table} function expression to generate rows from JSON array elements.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaJsonTableFunction jsonTable(@Nonnull Expression<?> jsonDocument, @Nullable Expression<String> jsonPath);

	/**
	 * Creates a {@code xmltable} function expression to generate rows from XML elements.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaXmlTableFunction xmlTable(@Nullable String xpath, @Nonnull Expression<?> xmlDocument);

	/**
	 * Creates a {@code xmltable} function expression to generate rows from XML elements.
	 *
	 * @since 7.0
	 * @see JpaSelectCriteria#from(JpaSetReturningFunction)
	 * @see JpaFrom#join(JpaSetReturningFunction)
	 */
	@Nonnull
	@Incubating(since = "6.3")
	JpaXmlTableFunction xmlTable(@Nonnull Expression<String> xpath, @Nonnull Expression<?> xmlDocument);

	/**
	 * Create a string concatenation expression from a list of expressions.
	 */
	@Nonnull
	@Override
	JpaExpression<String> concat(@Nonnull List<Expression<String>> expressions);

	/**
	 * Create an expression extracting the given temporal field from a temporal expression.
	 */
	@Nonnull
	@Override
	<N, T extends Temporal> JpaExpression<N> extract(@Nonnull TemporalField<N, T> field, @Nonnull Expression<T> temporal);
}
