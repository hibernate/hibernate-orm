/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.query.NullPrecedence;
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
 * needed express any query written in standard JPQL. This interface extends
 * {@code CriteriaBuilder}, adding operations needed to express features of
 * HQL which are not available in standard JPQL. For example:
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
 */
@Incubating
public interface HibernateCriteriaBuilder extends CriteriaBuilder {

	<X, T> JpaExpression<X> cast(JpaExpression<T> expression, Class<X> castTargetJavaType);

	JpaPredicate wrap(Expression<Boolean> expression);

	@SuppressWarnings("unchecked")
	JpaPredicate wrap(Expression<Boolean>... expressions);

	<T extends HibernateCriteriaBuilder> T unwrap(Class<T> clazz);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Criteria creation

	@Override
	JpaCriteriaQuery<Object> createQuery();

	@Override
	<T> JpaCriteriaQuery<T> createQuery(Class<T> resultClass);

	@Override
	JpaCriteriaQuery<Tuple> createTupleQuery();

	@Override
	<T> JpaCriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity);

	@Override
	<T> JpaCriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity);

	<T> JpaCriteriaInsertValues<T> createCriteriaInsertValues(Class<T> targetEntity);

	<T> JpaCriteriaInsertSelect<T> createCriteriaInsertSelect(Class<T> targetEntity);

	@Incubating
	JpaValues values(Expression<?>... expressions);

	@Incubating
	JpaValues values(List<? extends Expression<?>> expressions);

	/**
	 * Transform the given HQL {@code select} query to an equivalent criteria query.
	 *
	 * @param hql The HQL {@code select} query
	 * @param resultClass The result type of the query
	 *
	 * @return The equivalent criteria query
	 *
	 * @since 6.3
	 */
	<T> JpaCriteriaQuery<T> createQuery(String hql, Class<T> resultClass);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Set operation

	default <T> JpaCriteriaQuery<T> unionAll(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return union( true, query1, queries );
	}

	default <T> JpaCriteriaQuery<T> union(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return union( false, query1, queries );
	}

	<T> JpaCriteriaQuery<T> union(boolean all, CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries);

	default <T> JpaCriteriaQuery<T> intersectAll(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return intersect( true, query1, queries );
	}

	default <T> JpaCriteriaQuery<T> intersect(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return intersect( false, query1, queries );
	}

	<T> JpaCriteriaQuery<T> intersect(boolean all, CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries);

	default <T> JpaCriteriaQuery<T> exceptAll(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return except( true, query1, queries );
	}

	default <T> JpaCriteriaQuery<T> except(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return except( false, query1, queries );
	}

	<T> JpaCriteriaQuery<T> except(boolean all, CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries);

	@Override
	<T> CriteriaSelect<T> union(CriteriaSelect<? extends T> left, CriteriaSelect<? extends T> right);

	<T> JpaCriteriaQuery<T> union(CriteriaQuery<? extends T> left, CriteriaQuery<? extends T> right);

	default <T> JpaSubQuery<T> union(Subquery<? extends T> query1, Subquery<?>... queries) {
		return union( false, query1, queries );
	}

	<T> JpaSubQuery<T> union(boolean all, Subquery<? extends T> query1, Subquery<?>... queries);

	default <T> JpaSubQuery<T> unionAll(JpaSubQuery<? extends T> query1, JpaSubQuery<? extends T> query2) {
		return union( true, query1, query2 );
	}

	@Override
	<T> CriteriaSelect<T> unionAll(CriteriaSelect<? extends T> left, CriteriaSelect<? extends T> right);

	<T> JpaCriteriaQuery<T> unionAll(CriteriaQuery<? extends T> left, CriteriaQuery<? extends T> right);

	@Override
	<T> CriteriaSelect<T> intersect(CriteriaSelect<? super T> left, CriteriaSelect<? super T> right);

	@Override
	<T> CriteriaSelect<T> intersectAll(CriteriaSelect<? super T> left, CriteriaSelect<? super T> right);

	<T> JpaCriteriaQuery<T> intersect(CriteriaQuery<? super T> left, CriteriaQuery<? super T> right);

	<T> JpaCriteriaQuery<T> intersectAll(CriteriaQuery<? super T> left, CriteriaQuery<? super T> right);

	default <T> JpaSubQuery<T> intersectAll(Subquery<? extends T> query1, Subquery<?>... queries) {
		return intersect( true, query1, queries );
	}

	default <T> JpaSubQuery<T> intersect(Subquery<? extends T> query1, Subquery<?>... queries) {
		return intersect( false, query1, queries );
	}

	<T> JpaSubQuery<T> intersect(boolean all, Subquery<? extends T> query1, Subquery<?>... queries);

	@Override
	<T> CriteriaSelect<T> except(CriteriaSelect<T> left, CriteriaSelect<?> right);

	@Override
	<T> CriteriaSelect<T> exceptAll(CriteriaSelect<T> left, CriteriaSelect<?> right);

	<T> JpaCriteriaQuery<T> except(CriteriaQuery<T> left, CriteriaQuery<?> right);

	<T> JpaCriteriaQuery<T> exceptAll(CriteriaQuery<T> left, CriteriaQuery<?> right);

	default <T> JpaSubQuery<T> exceptAll(Subquery<? extends T> query1, Subquery<?>... queries) {
		return except( true, query1, queries );
	}

	default <T> JpaSubQuery<T> except(Subquery<? extends T> query1, Subquery<?>... queries) {
		return except( false, query1, queries );
	}

	<T> JpaSubQuery<T> except(boolean all, Subquery<? extends T> query1, Subquery<?>... queries);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA 3.1

	/**
	 * Create an expression that returns the sign of its
	 * argument, that is, {@code 1} if its argument is
	 * positive, {@code -1} if its argument is negative,
	 * or {@code 0} if its argument is exactly zero.
	 * @param x expression
	 * @return sign
	 */
	JpaExpression<Integer> sign(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the ceiling of its
	 * argument, that is, the smallest integer greater than
	 * or equal to its argument.
	 * @param x expression
	 * @return ceiling
	 */
	<N extends Number> JpaExpression<N> ceiling(Expression<N> x);

	/**
	 * Create an expression that returns the floor of its
	 * argument, that is, the largest integer smaller than
	 * or equal to its argument.
	 * @param x expression
	 * @return floor
	 */
	<N extends Number> JpaExpression<N> floor(Expression<N> x);

	/**
	 * Create an expression that returns the exponential
	 * of its argument, that is, Euler's number <i>e</i>
	 * raised to the power of its argument.
	 * @param x expression
	 * @return exponential
	 */
	JpaExpression<Double> exp(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the natural logarithm
	 * of its argument.
	 * @param x expression
	 * @return natural logarithm
	 */
	JpaExpression<Double> ln(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the first argument
	 * raised to the power of its second argument.
	 * @param x base
	 * @param y exponent
	 * @return the base raised to the power of the exponent
	 */
	JpaExpression<Double> power(Expression<? extends Number> x, Expression<? extends Number> y);

	/**
	 * Create an expression that returns the first argument
	 * raised to the power of its second argument.
	 * @param x base
	 * @param y exponent
	 * @return the base raised to the power of the exponent
	 */
	JpaExpression<Double> power(Expression<? extends Number> x, Number y);

	/**
	 * Create an expression that returns the first argument
	 * rounded to the number of decimal places given by the
	 * second argument.
	 * @param x base
	 * @param n number of decimal places
	 * @return the rounded value
	 */
	<T extends Number> JpaExpression<T> round(Expression<T> x, Integer n);

	/**
	 * Create an expression that returns the first argument
	 * truncated to the number of decimal places given by the
	 * second argument.
	 * @param x base
	 * @param n number of decimal places
	 * @return the truncated value
	 */
	<T extends Number> JpaExpression<T> truncate(Expression<T> x, Integer n);

	/**
	 *  Create expression to return current local date.
	 *  @return expression for current date
	 */
	JpaExpression<java.time.LocalDate> localDate();

	/**
	 *  Create expression to return current local datetime.
	 *  @return expression for current timestamp
	 */
	JpaExpression<java.time.LocalDateTime> localDateTime();

	/**
	 *  Create expression to return current local time.
	 *  @return expression for current time
	 */
	JpaExpression<java.time.LocalTime> localTime();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Paths

	<P, F> JpaExpression<F> fk(Path<P> path);

	@Override
	<X, T extends X> JpaPath<T> treat(Path<X> path, Class<T> type);

	@Override
	<X, T extends X> JpaRoot<T> treat(Root<X> root, Class<T> type);

	@Override
	<X, T, V extends T> JpaJoin<X, V> treat(Join<X, T> join, Class<V> type);

	@Override
	<X, T, E extends T> JpaCollectionJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type);

	@Override
	<X, T, E extends T> JpaSetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type);

	@Override
	<X, T, E extends T> JpaListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type);

	@Override
	<X, K, T, V extends T> JpaMapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Selections

	@Override
	<Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, Selection<?>... selections);
	<Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, List<? extends JpaSelection<?>> arguments);

	@Override
	JpaCompoundSelection<Tuple> tuple(Selection<?>... selections);
	JpaCompoundSelection<Tuple> tuple(List<Selection<?>> selections);

	@Override
	JpaCompoundSelection<Object[]> array(Selection<?>... selections);
	JpaCompoundSelection<Object[]> array(List<Selection<?>> selections);

	<Y> JpaCompoundSelection<Y> array(Class<Y> resultClass, Selection<?>... selections);
	<Y> JpaCompoundSelection<Y> array(Class<Y> resultClass, List<? extends JpaSelection<?>> selections);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	@Override
	<N extends Number> JpaExpression<Double> avg(Expression<N> argument);

	@Override
	<N extends Number> JpaExpression<N> sum(Expression<N> argument);

	@Override
	JpaExpression<Long> sumAsLong(Expression<Integer> argument);

	@Override
	JpaExpression<Double> sumAsDouble(Expression<Float> argument);

	@Override
	<N extends Number> JpaExpression<N> max(Expression<N> argument);

	@Override
	<N extends Number> JpaExpression<N> min(Expression<N> argument);

	@Override
	<X extends Comparable<? super X>> JpaExpression<X> greatest(Expression<X> argument);

	@Override
	<X extends Comparable<? super X>> JpaExpression<X> least(Expression<X> argument);

	@Override
	JpaExpression<Long> count(Expression<?> argument);

	@Override
	JpaExpression<Long> countDistinct(Expression<?> x);

	/**
	 * Equivalent to HQL {@code count(*)}.
	 */
	JpaExpression<Long> count();

	@Override
	<N extends Number> JpaExpression<N> neg(Expression<N> x);

	@Override
	<N extends Number> JpaExpression<N> abs(Expression<N> x);

	@Override
	<N extends Number> JpaExpression<N> sum(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpression<N> sum(Expression<? extends N> x, N y);

	@Override
	<N extends Number> JpaExpression<N> sum(N x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpression<N> prod(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpression<N> prod(Expression<? extends N> x, N y);

	@Override
	<N extends Number> JpaExpression<N> prod(N x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpression<N> diff(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpression<N> diff(Expression<? extends N> x, N y);

	@Override
	<N extends Number> JpaExpression<N> diff(N x, Expression<? extends N> y);

	@Override
	JpaExpression<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaExpression<Number> quot(Expression<? extends Number> x, Number y);

	@Override
	JpaExpression<Number> quot(Number x, Expression<? extends Number> y);

	@Override
	JpaExpression<Integer> mod(Expression<Integer> x, Expression<Integer> y);

	@Override
	JpaExpression<Integer> mod(Expression<Integer> x, Integer y);

	@Override
	JpaExpression<Integer> mod(Integer x, Expression<Integer> y);

	@Override
	JpaExpression<Double> sqrt(Expression<? extends Number> x);

	/**
	 * Add two {@linkplain Duration durations}.
	 * @since 6.3
	 */
	JpaExpression<Duration> durationSum(Expression<Duration> x, Expression<Duration> y);

	/**
	 * Add two {@linkplain Duration durations}.
	 * @since 6.3
	 */
	JpaExpression<Duration> durationSum(Expression<Duration> x, Duration y);

	/**
	 * Subtract one {@linkplain Duration duration} from another.
	 * @since 6.3
	 */
	JpaExpression<Duration> durationDiff(Expression<Duration> x, Expression<Duration> y);

	/**
	 * Subtract one {@linkplain Duration duration} from another.
	 * @since 6.3
	 */
	JpaExpression<Duration> durationDiff(Expression<Duration> x, Duration y);

	/**
	 * Scale a {@linkplain Duration duration} by a number.
	 * @since 6.3
	 */
	JpaExpression<Duration> durationScaled(Expression<? extends Number> number, Expression<Duration> duration);

	/**
	 * Scale a {@linkplain Duration duration} by a number.
	 * @since 6.3
	 */
	JpaExpression<Duration> durationScaled(Number number, Expression<Duration> duration);

	/**
	 * Scale a {@linkplain Duration duration} by a number.
	 * @since 6.3
	 */
	JpaExpression<Duration> durationScaled(Expression<? extends Number> number, Duration duration);

	/**
	 * A literal {@link Duration}, for example, "five days" or "30 minutes".
	 * @since 6.3
	 */
	@Incubating
	JpaExpression<Duration> duration(long magnitude, TemporalUnit unit);

	/**
	 * Convert a {@link Duration} to a numeric magnitude in the given units.
	 * @param unit a choice of temporal granularity
	 * @param duration the duration in a "unit-free" form
	 * @return the magnitude of the duration measured in the given units
	 * @since 6.3
	 */
	@Incubating
	JpaExpression<Long> durationByUnit(TemporalUnit unit, Expression<Duration> duration);

	/**
	 * Subtract two dates or two datetimes, returning the duration between the
	 * two dates or between two datetimes.
	 * @since 6.3
	 */
	<T extends Temporal> JpaExpression<Duration> durationBetween(Expression<T> x, Expression<T> y);

	/**
	 * Subtract two dates or two datetimes, returning the duration between the
	 * two dates or between two datetimes.
	 * @since 6.3
	 */
	<T extends Temporal> JpaExpression<Duration> durationBetween(Expression<T> x, T y);

	/**
	 * Add a duration to a date or datetime, that is, return a later date or
	 * datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	<T extends Temporal> JpaExpression<T> addDuration(Expression<T> datetime, Expression<Duration> duration);

	/**
	 * Add a duration to a date or datetime, that is, return a later date or
	 * datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	<T extends Temporal> JpaExpression<T> addDuration(Expression<T> datetime, Duration duration);

	/**
	 * Add a duration to a date or datetime, that is, return a later date or
	 * datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	<T extends Temporal> JpaExpression<T> addDuration(T datetime, Expression<Duration> duration);

	/**
	 * Subtract a duration to a date or datetime, that is, return an earlier date
	 * or datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	<T extends Temporal> JpaExpression<T> subtractDuration(Expression<T> datetime, Expression<Duration> duration);

	/**
	 * Subtract a duration to a date or datetime, that is, return an earlier date
	 * or datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	<T extends Temporal> JpaExpression<T> subtractDuration(Expression<T> datetime, Duration duration);

	/**
	 * Subtract a duration to a date or datetime, that is, return an earlier date
	 * or datetime which is separated from the given date or datetime by the given
	 * duration.
	 * @since 6.3
	 */
	<T extends Temporal> JpaExpression<T> subtractDuration(T datetime, Expression<Duration> duration);

	@Override
	JpaExpression<Long> toLong(Expression<? extends Number> number);

	@Override
	JpaExpression<Integer> toInteger(Expression<? extends Number> number);

	@Override
	JpaExpression<Float> toFloat(Expression<? extends Number> number);

	@Override
	JpaExpression<Double> toDouble(Expression<? extends Number> number);

	@Override
	JpaExpression<BigDecimal> toBigDecimal(Expression<? extends Number> number);

	@Override
	JpaExpression<BigInteger> toBigInteger(Expression<? extends Number> number);

	@Override
	JpaExpression<String> toString(Expression<Character> character);

	@Override
	<T> JpaExpression<T> literal(T value);

	<T> List<? extends JpaExpression<T>> literals(T... values);

	<T> List<? extends JpaExpression<T>> literals(List<T> values);

	@Override
	<T> JpaExpression<T> nullLiteral(Class<T> resultClass);

	@Override
	<T> JpaParameterExpression<T> parameter(Class<T> paramClass);

	@Override
	<T> JpaParameterExpression<T> parameter(Class<T> paramClass, String name);

	/**
	 * Create a multivalued parameter accepting multiple arguments
	 * packaged together as a {@link List}.
	 * @param paramClass the type of each argument to the parameter
	 * @param <T> the type of each argument to the parameter
	 * @since 7.0
	 */
	<T> JpaParameterExpression<List<T>> parameterList(Class<T> paramClass);

	/**
	 * Create a multivalued parameter accepting multiple arguments
	 * packaged together as a {@link List}.
	 * @param paramClass the type of each argument to the parameter
	 * @param name the parameter name
	 * @param <T> the type of each argument to the parameter
	 * @since 7.0
	 */
	<T> JpaParameterExpression<List<T>> parameterList(Class<T> paramClass, String name);

	@Override
	JpaExpression<String> concat(Expression<String> x, Expression<String> y);

	@Override
	JpaExpression<String> concat(Expression<String> x, String y);

	@Override
	JpaExpression<String> concat(String x, Expression<String> y);

	JpaExpression<String> concat(String x, String y);

	@Override
	JpaFunction<String> substring(Expression<String> x, Expression<Integer> from);

	@Override
	JpaFunction<String> substring(Expression<String> x, int from);

	@Override
	JpaFunction<String> substring(
			Expression<String> x,
			Expression<Integer> from,
			Expression<Integer> len);

	@Override
	JpaFunction<String> substring(Expression<String> x, int from, int len);

	@Override
	JpaFunction<String> trim(Expression<String> x);

	@Override
	JpaFunction<String> trim(Trimspec ts, Expression<String> x);

	@Override
	JpaFunction<String> trim(Expression<Character> t, Expression<String> x);

	@Override
	JpaFunction<String> trim(Trimspec ts, Expression<Character> t, Expression<String> x);

	@Override
	JpaFunction<String> trim(char t, Expression<String> x);

	@Override
	JpaFunction<String> trim(Trimspec ts, char t, Expression<String> x);

	@Override
	JpaFunction<String> lower(Expression<String> x);

	@Override
	JpaFunction<String> upper(Expression<String> x);

	@Override
	JpaFunction<Integer> length(Expression<String> x);

	@Override
	JpaFunction<Integer> locate(Expression<String> x, Expression<String> pattern);

	@Override
	JpaFunction<Integer> locate(Expression<String> x, String pattern);

	@Override
	JpaFunction<Integer> locate(
			Expression<String> x,
			Expression<String> pattern,
			Expression<Integer> from);

	@Override
	JpaFunction<Integer> locate(Expression<String> x, String pattern, int from);

	@Override
	JpaFunction<Date> currentDate();

	@Override
	JpaFunction<Time> currentTime();

	@Override
	JpaFunction<Timestamp> currentTimestamp();

	JpaFunction<Instant> currentInstant();

	@Override
	<T> JpaFunction<T> function(String name, Class<T> type, Expression<?>... args);

	@Override
	<Y> JpaExpression<Y> all(Subquery<Y> subquery);

	@Override
	<Y> JpaExpression<Y> some(Subquery<Y> subquery);

	@Override
	<Y> JpaExpression<Y> any(Subquery<Y> subquery);

	@Override
	<K, M extends Map<K, ?>> JpaExpression<Set<K>> keys(M map);

	<K, L extends List<?>> JpaExpression<Set<K>> indexes(L list);

	<T> JpaExpression<T> value(T value);

	@Override
	<V, M extends Map<?, V>> Expression<Collection<V>> values(M map);

	@Override
	<C extends Collection<?>> JpaExpression<Integer> size(Expression<C> collection);

	@Override
	<C extends Collection<?>> JpaExpression<Integer> size(C collection);

	@Override
	<T> JpaCoalesce<T> coalesce();

	@Override
	<Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Y y);

	@Override
	<Y> JpaExpression<Y> nullif(Expression<Y> x, Expression<?> y);

	@Override
	<Y> JpaExpression<Y> nullif(Expression<Y> x, Y y);

	@Override
	<C, R> JpaSimpleCase<C, R> selectCase(Expression<? extends C> expression);

	@Override
	<R> JpaSearchedCase<R> selectCase();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates


	@Override
	JpaPredicate and(Expression<Boolean> x, Expression<Boolean> y);

	@Override
	JpaPredicate and(Predicate... restrictions);

	@Override
	JpaPredicate or(Expression<Boolean> x, Expression<Boolean> y);

	@Override
	JpaPredicate or(Predicate... restrictions);

	@Override
	JpaPredicate not(Expression<Boolean> restriction);

	@Override
	JpaPredicate conjunction();

	@Override
	JpaPredicate disjunction();

	@Override
	JpaPredicate isTrue(Expression<Boolean> x);

	@Override
	JpaPredicate isFalse(Expression<Boolean> x);

	@Override
	JpaPredicate isNull(Expression<?> x);

	@Override
	JpaPredicate isNotNull(Expression<?> x);

	@Override
	JpaPredicate equal(Expression<?> x, Expression<?> y);

	@Override
	JpaPredicate equal(Expression<?> x, Object y);

	@Override
	JpaPredicate notEqual(Expression<?> x, Expression<?> y);

	@Override
	JpaPredicate notEqual(Expression<?> x, Object y);

	JpaPredicate distinctFrom(Expression<?> x, Expression<?> y);

	JpaPredicate distinctFrom(Expression<?> x, Object y);

	JpaPredicate notDistinctFrom(Expression<?> x, Expression<?> y);

	JpaPredicate notDistinctFrom(Expression<?> x, Object y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThan(
			Expression<? extends Y> x,
			Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThan(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate greaterThanOrEqualTo(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThan(
			Expression<? extends Y> x,
			Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThan(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate lessThanOrEqualTo(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate between(
			Expression<? extends Y> value,
			Expression<? extends Y> lower,
			Expression<? extends Y> upper);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicate between(Expression<? extends Y> value, Y lower, Y upper);

	@Override
	JpaPredicate gt(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicate gt(Expression<? extends Number> x, Number y);

	@Override
	JpaPredicate ge(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicate ge(Expression<? extends Number> x, Number y);

	@Override
	JpaPredicate lt(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicate lt(Expression<? extends Number> x, Number y);

	@Override
	JpaPredicate le(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicate le(Expression<? extends Number> x, Number y);

	@Override
	<C extends Collection<?>> JpaPredicate isEmpty(Expression<C> collection);

	@Override
	<C extends Collection<?>> JpaPredicate isNotEmpty(Expression<C> collection);

	@Override
	<E, C extends Collection<E>> JpaPredicate isMember(Expression<E> elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> JpaPredicate isMember(E elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> JpaPredicate isNotMember(Expression<E> elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> JpaPredicate isNotMember(E elem, Expression<C> collection);

	@Override
	JpaPredicate like(Expression<String> x, Expression<String> pattern);

	@Override
	JpaPredicate like(Expression<String> x, String pattern);

	@Override
	JpaPredicate like(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicate like(Expression<String> x, Expression<String> pattern, char escapeChar);

	@Override
	JpaPredicate like(Expression<String> x, String pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicate like(Expression<String> x, String pattern, char escapeChar);

	JpaPredicate ilike(Expression<String> x, Expression<String> pattern);

	JpaPredicate ilike(Expression<String> x, String pattern);

	JpaPredicate ilike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	JpaPredicate ilike(Expression<String> x, Expression<String> pattern, char escapeChar);

	JpaPredicate ilike(Expression<String> x, String pattern, Expression<Character> escapeChar);

	JpaPredicate ilike(Expression<String> x, String pattern, char escapeChar);

	@Override
	JpaPredicate notLike(Expression<String> x, Expression<String> pattern);

	@Override
	JpaPredicate notLike(Expression<String> x, String pattern);

	@Override
	JpaPredicate notLike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicate notLike(Expression<String> x, Expression<String> pattern, char escapeChar);

	@Override
	JpaPredicate notLike(Expression<String> x, String pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicate notLike(Expression<String> x, String pattern, char escapeChar);

	JpaPredicate notIlike(Expression<String> x, Expression<String> pattern);

	JpaPredicate notIlike(Expression<String> x, String pattern);

	JpaPredicate notIlike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	JpaPredicate notIlike(Expression<String> x, Expression<String> pattern, char escapeChar);

	JpaPredicate notIlike(Expression<String> x, String pattern, Expression<Character> escapeChar);

	JpaPredicate notIlike(Expression<String> x, String pattern, char escapeChar);

	@Override
	<T> JpaInPredicate<T> in(Expression<? extends T> expression);

	@SuppressWarnings("unchecked")
	<T> JpaInPredicate<T> in(Expression<? extends T> expression, Expression<? extends T>... values);

	@SuppressWarnings("unchecked")
	<T> JpaInPredicate<T> in(Expression<? extends T> expression, T... values);

	<T> JpaInPredicate<T> in(Expression<? extends T> expression, Collection<T> values);

	@Override
	JpaPredicate exists(Subquery<?> subquery);

	/**
	 * Create a predicate that tests whether a Map is empty.
	 *
	 * @apiNote Due to type-erasure we cannot name this the same as
	 *          {@link CriteriaBuilder#isEmpty}.
	 *
	 * @param mapExpression The expression resolving to a Map which we
	 * want to check for emptiness
	 *
	 * @return is-empty predicate
	 */
	<M extends Map<?,?>> JpaPredicate isMapEmpty(JpaExpression<M> mapExpression);

	/**
	 * Create a predicate that tests whether a Map is not empty.
	 *
	 * @apiNote Due to type-erasure we cannot name this the same as
	 *          {@link CriteriaBuilder#isNotEmpty}
	 *
	 * @param mapExpression The expression resolving to a Map which we
	 * want to check for non-emptiness
	 *
	 * @return is-not-empty predicate
	 */
	<M extends Map<?,?>> JpaPredicate isMapNotEmpty(JpaExpression<M> mapExpression);

	/**
	 * Create an expression that tests the size of a map.
	 *
	 * @apiNote Due to type-erasure we cannot name this the same as
	 *          {@link CriteriaBuilder#size}
	 *
	 * @param mapExpression The expression resolving to a Map for which we
	 * want to know the size
	 *
	 * @return size expression
	 */
	<M extends Map<?,?>> JpaExpression<Integer> mapSize(JpaExpression<M> mapExpression);

	/**
	 * Create an expression that tests the size of a map.
	 *
	 * @param map The Map for which we want to know the size
	 *
	 * @return size expression
	 */
	<M extends Map<?, ?>> JpaExpression<Integer> mapSize(M map);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordering


	JpaOrder sort(JpaExpression<?> sortExpression);

	JpaOrder sort(JpaExpression<?> sortExpression, SortDirection sortOrder);

	JpaOrder sort(JpaExpression<?> sortExpression, SortDirection sortOrder, Nulls nullPrecedence);

	JpaOrder sort(JpaExpression<?> sortExpression, SortDirection sortOrder, Nulls nullPrecedence, boolean ignoreCase);

	/**
	 * @deprecated Use {@linkplain #sort(JpaExpression, SortDirection, Nulls)} instead
	 */
	@Deprecated
	default JpaOrder sort(JpaExpression<?> sortExpression, SortDirection sortOrder, NullPrecedence nullPrecedence) {
		return sort( sortExpression, sortOrder, nullPrecedence.getJpaValue() );
	}

	/**
	 * @deprecated Use {@linkplain #sort(JpaExpression, SortDirection, Nulls, boolean)} instead
	 */
	@Deprecated
	default JpaOrder sort(
			JpaExpression<?> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence,
			boolean ignoreCase) {
		return sort( sortExpression, sortOrder, nullPrecedence.getJpaValue(), ignoreCase );
	}

	@Override
	JpaOrder asc(Expression<?> x);

	@Override
	JpaOrder desc(Expression<?> x);

	/**
	 * Create an ordering by the ascending value of the expression.
	 * @param x  expression used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return ascending ordering corresponding to the expression
	 */
	JpaOrder asc(Expression<?> x, boolean nullsFirst);

	/**
	 * Create an ordering by the descending value of the expression.
	 * @param x  expression used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return descending ordering corresponding to the expression
	 */
	JpaOrder desc(Expression<?> x, boolean nullsFirst);

	/**
	 * Create a search ordering based on the sort order and null precedence of the value of the CTE attribute.
	 * @param cteAttribute CTE attribute used to define the ordering
	 * @param sortOrder The sort order
	 * @param nullPrecedence The null precedence
	 * @return ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder search(JpaCteCriteriaAttribute cteAttribute, SortDirection sortOrder, NullPrecedence nullPrecedence);

	/**
	 * Create a search ordering based on the sort order of the value of the CTE attribute.
	 * @param cteAttribute CTE attribute used to define the ordering
	 * @param sortOrder The sort order
	 * @return ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder search(JpaCteCriteriaAttribute cteAttribute, SortDirection sortOrder);

	/**
	 * Create a search ordering based on the ascending value of the CTE attribute.
	 * @param cteAttribute CTE attribute used to define the ordering
	 * @return ascending ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder search(JpaCteCriteriaAttribute cteAttribute);

	/**
	 * Create a search ordering by the ascending value of the CTE attribute.
	 * @param x  CTE attribute used to define the ordering
	 * @return ascending ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder asc(JpaCteCriteriaAttribute x);

	/**
	 * Create a search ordering by the descending value of the CTE attribute.
	 * @param x CTE attribute used to define the ordering
	 * @return descending ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder desc(JpaCteCriteriaAttribute x);

	/**
	 * Create a search ordering by the ascending value of the CTE attribute.
	 * @param x  CTE attribute used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return ascending ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder asc(JpaCteCriteriaAttribute x, boolean nullsFirst);

	/**
	 * Create a search ordering by the descending value of the CTE attribute.
	 * @param x CTE attribute used to define the ordering
	 * @param nullsFirst Whether <code>null</code> should be sorted first
	 * @return descending ordering corresponding to the CTE attribute
	 */
	@Incubating
	JpaSearchOrder desc(JpaCteCriteriaAttribute x, boolean nullsFirst);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Non-standard HQL functions

	/**
	 * Embed native {@code pattern} that will be unquoted and embedded in the generated SQL.
	 * Occurrences of {@code ?} in the pattern are replaced with the remaining {@code arguments}
	 * of the function.
	 *
	 * @param pattern native SQL pattern
	 * @param type type of this expression
	 * @param arguments optional arguments to the SQL pattern
	 * @param <T> type of this expression
	 *
	 * @return native SQL expression
	 */
	@Incubating
	<T> JpaExpression<T> sql(String pattern, Class<T> type, Expression<?>... arguments);

	/**
	 * Format a date, time, or datetime according to a pattern.
	 * The pattern must be written in a subset of the pattern language defined by
	 * Java’s {@link java.time.format.DateTimeFormatter}.
	 * <p>
	 * See {@link org.hibernate.dialect.Dialect#appendDatetimeFormat}
	 * for a full list of pattern elements.
	 *
	 * @param datetime the datetime expression to format
	 * @param pattern the pattern to use for formatting
	 *
	 * @return format expression
	 */
	@Incubating
	JpaFunction<String> format(Expression<? extends TemporalAccessor> datetime, String pattern);

	/**
	 * Extracts the {@link TemporalUnit#YEAR} of a date, time, or datetime expression.
	 *
	 * @param datetime the date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Incubating
	JpaFunction<Integer> year(Expression<? extends TemporalAccessor> datetime);

	/**
	 * Extracts the {@link TemporalUnit#MONTH} of a date, time, or datetime expression.
	 *
	 * @param datetime the date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Incubating
	JpaFunction<Integer> month(Expression<? extends TemporalAccessor> datetime);

	/**
	 * Extracts the {@link TemporalUnit#DAY} of a date, time, or datetime expression.
	 *
	 * @param datetime the date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Incubating
	JpaFunction<Integer> day(Expression<? extends TemporalAccessor> datetime);

	/**
	 * Extracts the {@link TemporalUnit#HOUR} of a date, time, or datetime expression.
	 *
	 * @param datetime the date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Incubating
	JpaFunction<Integer> hour(Expression<? extends TemporalAccessor> datetime);

	/**
	 * Extracts the {@link TemporalUnit#MINUTE} of a date, time, or datetime expression.
	 *
	 * @param datetime the date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Incubating
	JpaFunction<Integer> minute(Expression<? extends TemporalAccessor> datetime);

	/**
	 * Extracts the {@link TemporalUnit#SECOND} of a date, time, or datetime expression.
	 *
	 * @param datetime the date, time, or datetime to extract the value from
	 *
	 * @return the extracted value
	 */
	@Incubating
	JpaFunction<Float> second(Expression<? extends TemporalAccessor> datetime);

	/**
	 * Truncates a date, time or datetime expression to the given {@link TemporalUnit}.
	 * Supported units are: {@code YEAR}, {@code MONTH}, {@code DAY},  {@code HOUR}, {@code MINUTE}, {@code SECOND}.
	 * <p>
	 * Truncating translates to obtaining a value of the same type in which all temporal units smaller than {@code field} have been pruned.
	 * For hours, minutes and second this means setting them to {@code 00}. For months and days, this means setting them to {@code 01}.
	 *
	 * @param datetime the date, time or datetime expression to be truncated
	 * @param temporalUnit the temporal unit for truncation
	 *
	 * @return the truncated value
	 */
	@Incubating
	<T extends TemporalAccessor> JpaFunction<T> truncate(Expression<T> datetime, TemporalUnit temporalUnit);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> overlay(Expression<String> string, String replacement, int start);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> overlay(Expression<String> string, Expression<String> replacement, int start);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> overlay(Expression<String> string, String replacement, Expression<Integer> start);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> overlay(Expression<String> string, Expression<String> replacement, Expression<Integer> start);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> overlay(Expression<String> string, String replacement, int start, int length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> overlay(Expression<String> string, Expression<String> replacement, int start, int length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> overlay(Expression<String> string, String replacement, Expression<Integer> start, int length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			Expression<Integer> start,
			int length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> overlay(Expression<String> string, String replacement, int start, Expression<Integer> length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			int start,
			Expression<Integer> length);

	/**
	 * @see #overlay(Expression, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> overlay(
			Expression<String> string,
			String replacement,
			Expression<Integer> start,
			Expression<Integer> length);

	/**
	 * Overlay the {@code string} expression with the {@code replacement} expression,
	 * starting from index {@code start} and substituting a number of characters
	 * corresponding to the length of the {@code replacement} expression or the
	 * {@code length} parameter if specified.
	 *
	 * @param string string expression to be manipulated
	 * @param replacement string expression to replace in original
	 * @param start start position
	 * @param length optional, number of characters to substitute
	 *
	 * @return overlay expression
	 */
	@Incubating
	JpaFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			Expression<Integer> start,
			Expression<Integer> length);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> pad(Expression<String> x, int length);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> pad(Trimspec ts, Expression<String> x, int length);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> pad(Expression<String> x, Expression<Integer> length);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> pad(Trimspec ts, Expression<String> x, Expression<Integer> length);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> pad(Expression<String> x, int length, char padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> pad(Trimspec ts, Expression<String> x, int length, char padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> pad(Expression<String> x, Expression<Integer> length, char padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> pad(Trimspec ts, Expression<String> x, Expression<Integer> length, char padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> pad(Expression<String> x, int length, Expression<Character> padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> pad(Trimspec ts, Expression<String> x, int length, Expression<Character> padChar);

	/**
	 * @see #pad(Trimspec, Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> pad(Expression<String> x, Expression<Integer> length, Expression<Character> padChar);

	/**
	 * Pad the specified string expression with whitespace or with the {@code padChar} character if specified.
	 * Optionally pass a {@link jakarta.persistence.criteria.CriteriaBuilder.Trimspec} to pad the
	 * string expression with {@code LEADING} or {@code TRAILING} (default) characters.
	 *
	 * @param ts optional {@link jakarta.persistence.criteria.CriteriaBuilder.Trimspec}
	 * @param x string expression to pad
	 * @param length length of the result string after padding
	 * @param padChar optional pad character
	 *
	 * @return pad expression
	 */
	@Incubating
	JpaFunction<String> pad(
			Trimspec ts,
			Expression<String> x,
			Expression<Integer> length,
			Expression<Character> padChar);

	/**
	 * Concatenate the given string expression with itself the given number of times.
	 *
	 * @param x the string expression to concatenate
	 * @param times the number of times it should be repeated
	 *
	 * @return repeat expression
	 */
	JpaFunction<String> repeat(Expression<String> x, Expression<Integer> times);

	/**
	 * Concatenate the given string expression with itself the given number of times.
	 *
	 * @param x the string expression to concatenate
	 * @param times the number of times it should be repeated
	 *
	 * @return repeat expression
	 */
	JpaFunction<String> repeat(Expression<String> x, int times);

	/**
	 * Concatenate the given string expression with itself the given number of times.
	 *
	 * @param x the string expression to concatenate
	 * @param times the number of times it should be repeated
	 *
	 * @return repeat expression
	 */
	JpaFunction<String> repeat(String x, Expression<Integer> times);

	/**
	 * @see #left(Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> left(Expression<String> x, int length);

	/**
	 * Extract the {@code length} leftmost characters of a string.
	 *
	 * @param x original string
	 * @param length number of characters
	 *
	 * @return left expression
	 */
	@Incubating
	JpaFunction<String> left(Expression<String> x, Expression<Integer> length);

	/**
	 * @see #right(Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> right(Expression<String> x, int length);

	/**
	 * Extract the {@code length} rightmost characters of a string.
	 *
	 * @param x original string
	 * @param length number of characters
	 *
	 * @return left expression
	 */
	@Incubating
	JpaFunction<String> right(Expression<String> x, Expression<Integer> length);

	/**
	 * @see #replace(Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> replace(Expression<String> x, String pattern, String replacement);

	/**
	 * @see #replace(Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> replace(Expression<String> x, String pattern, Expression<String> replacement);

	/**
	 * @see #replace(Expression, Expression, Expression)
	 */
	@Incubating
	JpaFunction<String> replace(Expression<String> x, Expression<String> pattern, String replacement);

	/**
	 * Replace all occurrences of {@code pattern} within the original string with {@code replacement}.
	 *
	 * @param x original string
	 * @param pattern the string to be replaced
	 * @param replacement the new replacement string
	 *
	 * @return replace expression
	 */
	@Incubating
	JpaFunction<String> replace(Expression<String> x, Expression<String> pattern, Expression<String> replacement);

	@Incubating
	JpaFunction<String> collate(Expression<String> x, String collation);

	/**
	 * Create an expression that returns the base-10 logarithm
	 * of its argument.
	 *
	 * @param x expression
	 *
	 * @return base-10 logarithm
	 */
	@Incubating
	JpaExpression<Double> log10(Expression<? extends Number> x);

	/**
	 * @see #log(Expression, Expression)
	 */
	@Incubating
	JpaExpression<Double> log(Number b, Expression<? extends Number> x);

	/**
	 * Create an expression that returns the logarithm of {@code x} to the base {@code b}.
	 *
	 * @param b base
	 * @param x expression
	 *
	 * @return arbitrary-base logarithm
	 */
	@Incubating
	JpaExpression<Double> log(Expression<? extends Number> b, Expression<? extends Number> x);

	/**
	 * Literal expression corresponding to the value of pi.
	 *
	 * @return pi expression
	 */
	@Incubating
	JpaExpression<Double> pi();

	/**
	 * Create an expression that returns the sine of its argument.
	 *
	 * @param x expression
	 *
	 * @return sine
	 */
	@Incubating
	JpaExpression<Double> sin(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the cosine of its argument.
	 *
	 * @param x expression
	 *
	 * @return cosine
	 */
	@Incubating
	JpaExpression<Double> cos(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the tangent of its argument.
	 *
	 * @param x expression
	 *
	 * @return tangent
	 */
	@Incubating
	JpaExpression<Double> tan(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the inverse sine of its argument.
	 *
	 * @param x expression
	 *
	 * @return inverse sine
	 */
	@Incubating
	JpaExpression<Double> asin(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the inverse cosine of its argument.
	 *
	 * @param x expression
	 *
	 * @return inverse cosine
	 */
	@Incubating
	JpaExpression<Double> acos(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the inverse tangent of its argument.
	 *
	 * @param x expression
	 *
	 * @return inverse tangent
	 */
	@Incubating
	JpaExpression<Double> atan(Expression<? extends Number> x);

	/**
	 * @see #atan2(Expression, Expression)
	 */
	@Incubating
	JpaExpression<Double> atan2(Number y, Expression<? extends Number> x);

	/**
	 * @see #atan2(Expression, Expression)
	 */
	@Incubating
	JpaExpression<Double> atan2(Expression<? extends Number> y, Number x);

	/**
	 * Create an expression that returns the inverse tangent of {@code y} over {@code x}.
	 *
	 * @param y y coordinate
	 * @param x x coordinate
	 *
	 * @return 2-argument inverse tangent
	 */
	@Incubating
	JpaExpression<Double> atan2(Expression<? extends Number> y, Expression<? extends Number> x);

	/**
	 * Create an expression that returns the hyperbolic sine of its argument.
	 *
	 * @param x expression
	 *
	 * @return hyperbolic sine
	 */
	@Incubating
	JpaExpression<Double> sinh(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the hyperbolic cosine of its argument.
	 *
	 * @param x expression
	 *
	 * @return hyperbolic cosine
	 */
	@Incubating
	JpaExpression<Double> cosh(Expression<? extends Number> x);

	/**
	 * Create an expression that returns the hyperbolic tangent of its argument.
	 *
	 * @param x expression
	 *
	 * @return hyperbolic tangent
	 */
	@Incubating
	JpaExpression<Double> tanh(Expression<? extends Number> x);

	/**
	 * Create an expression that converts an angle measured in radians
	 * to an approximately equivalent angle measured in degrees.
	 *
	 * @param x expression
	 *
	 * @return degrees
	 */
	@Incubating
	JpaExpression<Double> degrees(Expression<? extends Number> x);

	/**
	 * Create an expression that converts an angle measured in degrees
	 * to an approximately equivalent angle measured in radians.
	 *
	 * @param x expression
	 *
	 * @return radians
	 */
	@Incubating
	JpaExpression<Double> radians(Expression<? extends Number> x);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Window functions

	/**
	 * Create an empty {@link JpaWindow} to use with window and aggregate functions.
	 *
	 * @return the empty window
	 */
	@Incubating
	JpaWindow createWindow();

	/**
	 * Create a window frame of type {@link FrameKind#UNBOUNDED_PRECEDING} to use with {@link JpaWindow}s.
	 *
	 * @return the window frame
	 */
	@Incubating
	JpaWindowFrame frameUnboundedPreceding();

	/**
	 * @see #frameBetweenPreceding(Expression)
	 */
	@Incubating
	JpaWindowFrame frameBetweenPreceding(int offset);

	/**
	 * Create window frame of type {@link FrameKind#OFFSET_PRECEDING} to use with {@link JpaWindow}s.
	 *
	 * @param offset the {@code offset} expression
	 *
	 * @return the window frame
	 */
	@Incubating
	JpaWindowFrame frameBetweenPreceding(Expression<?> offset);

	/**
	 * Create a window frame of type {@link FrameKind#CURRENT_ROW} to use with {@link JpaWindow}s.
	 *
	 * @return the window frame
	 */
	@Incubating
	JpaWindowFrame frameCurrentRow();

	/**
	 * @see #frameBetweenFollowing(Expression)
	 */
	@Incubating
	JpaWindowFrame frameBetweenFollowing(int offset);

	/**
	 * Create a window frame of type {@link FrameKind#OFFSET_FOLLOWING} to use with {@link JpaWindow}s.
	 *
	 * @param offset the {@code offset} expression
	 *
	 * @return the window frame
	 */
	@Incubating
	JpaWindowFrame frameBetweenFollowing(Expression<?> offset);

	/**
	 * Create a window frame of type {@link FrameKind#UNBOUNDED_FOLLOWING} to use with {@link JpaWindow}s.
	 *
	 * @return the window frame
	 */
	@Incubating
	JpaWindowFrame frameUnboundedFollowing();

	/**
	 * Create a generic window function expression that will be applied
	 * over the specified {@link JpaWindow window}.
	 *
	 * @param name name of the window function
	 * @param type type of this expression
	 * @param window window over which the function will be applied
	 * @param args arguments to the function
	 * @param <T> type of this expression
	 *
	 * @return window function expression
	 */
	@Incubating
	<T> JpaExpression<T> windowFunction(String name, Class<T> type, JpaWindow window, Expression<?>... args);

	/**
	 * Create a {@code row_number} window function expression.
	 *
	 * @param window window over which the function will be applied
	 *
	 * @return window function expression
	 *
	 * @see #windowFunction
	 */
	@Incubating
	JpaExpression<Long> rowNumber(JpaWindow window);

	/**
	 * Create a {@code first_value} window function expression.
	 *
	 * @param argument argument expression to pass to {@code first_value}
	 * @param window window over which the function will be applied
	 * @param <T> type of the expression
	 *
	 * @return window function expression
	 *
	 * @see #windowFunction
	 */
	@Incubating
	<T> JpaExpression<T> firstValue(Expression<T> argument, JpaWindow window);

	/**
	 * Create a {@code last_value} window function expression.
	 *
	 * @param argument argument expression to pass to {@code last_value}
	 * @param window window over which the function will be applied
	 * @param <T> type of the expression
	 *
	 * @return window function expression
	 *
	 * @see #windowFunction
	 */
	@Incubating
	<T> JpaExpression<T> lastValue(Expression<T> argument, JpaWindow window);

	/**
	 * @see #nthValue(Expression, Expression, JpaWindow) nthValue
	 */
	@Incubating
	<T> JpaExpression<T> nthValue(Expression<T> argument, int n, JpaWindow window);

	/**
	 * Create a {@code nth_value} window function expression.
	 *
	 * @param argument argument expression to pass to {@code nth_value}
	 * @param n the {@code N} argument for the function
	 * @param window window over which the function will be applied
	 * @param <T> type of the expression
	 *
	 * @return window function expression
	 *
	 * @see #windowFunction
	 */
	@Incubating
	<T> JpaExpression<T> nthValue(Expression<T> argument, Expression<Integer> n, JpaWindow window);

	/**
	 * Create a {@code rank} window function expression.
	 *
	 * @param window window over which the function will be applied
	 *
	 * @return window function expression
	 *
	 * @see #windowFunction
	 */
	@Incubating
	JpaExpression<Long> rank(JpaWindow window);

	/**
	 * Create a {@code dense_rank} window function expression.
	 *
	 * @param window window over which the function will be applied
	 *
	 * @return window function expression
	 *
	 * @see #windowFunction
	 */
	@Incubating
	JpaExpression<Long> denseRank(JpaWindow window);

	/**
	 * Create a {@code percent_rank} window function expression.
	 *
	 * @param window window over which the function will be applied
	 *
	 * @return window function expression
	 *
	 * @see #windowFunction
	 */
	@Incubating
	JpaExpression<Double> percentRank(JpaWindow window);

	/**
	 * Create a {@code cume_dist} window function expression.
	 *
	 * @param window window over which the function will be applied
	 *
	 * @return window function expression
	 *
	 * @see #windowFunction
	 */
	@Incubating
	JpaExpression<Double> cumeDist(JpaWindow window);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Aggregate functions

	/**
	 * @see #functionAggregate(String, Class, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	<T> JpaExpression<T> functionAggregate(
			String name,
			Class<T> type,
			JpaPredicate filter,
			Expression<?>... args);

	/**
	 * @see #functionAggregate(String, Class, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	<T> JpaExpression<T> functionAggregate(
			String name,
			Class<T> type,
			JpaWindow window,
			Expression<?>... args);

	/**
	 * Create a generic aggregate function expression.
	 *
	 * @param name name of the ordered set-aggregate function
	 * @param type type of this expression
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 * @param args optional arguments to the function
	 * @param <T> type of this expression
	 *
	 * @return aggregate function expression
	 */
	@Incubating
	<T> JpaExpression<T> functionAggregate(
			String name,
			Class<T> type,
			JpaPredicate filter,
			JpaWindow window,
			Expression<?>... args);

	/**
	 * @see #sum(Expression, JpaPredicate, JpaWindow)
	 */
	@Incubating
	<N extends Number> JpaExpression<Number> sum(Expression<N> argument, JpaPredicate filter);

	/**
	 * @see #sum(Expression, JpaPredicate, JpaWindow)
	 */
	@Incubating
	<N extends Number> JpaExpression<Number> sum(Expression<N> argument, JpaWindow window);

	/**
	 * Create a {@code sum} aggregate function expression.
	 *
	 * @param argument argument to the function
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 * @param <N> type of the input expression
	 *
	 * @return aggregate function expression
	 *
	 * @see #functionAggregate(String, Class, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	<N extends Number> JpaExpression<Number> sum(Expression<N> argument, JpaPredicate filter, JpaWindow window);

	/**
	 * @see #avg(Expression, JpaPredicate, JpaWindow)
	 */
	@Incubating
	<N extends Number> JpaExpression<Double> avg(Expression<N> argument, JpaPredicate filter);

	/**
	 * @see #avg(Expression, JpaPredicate, JpaWindow)
	 */
	@Incubating
	<N extends Number> JpaExpression<Double> avg(Expression<N> argument, JpaWindow window);

	/**
	 * Create an {@code avg} aggregate function expression.
	 *
	 * @param argument argument to the function
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 * @param <N> type of the input expression
	 *
	 * @return aggregate function expression
	 *
	 * @see #functionAggregate(String, Class, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	<N extends Number> JpaExpression<Double> avg(Expression<N> argument, JpaPredicate filter, JpaWindow window);

	/**
	 * @see #count(Expression, JpaPredicate, JpaWindow)
	 */
	@Incubating
	JpaExpression<Long> count(Expression<?> argument, JpaPredicate filter);

	/**
	 * @see #count(Expression, JpaPredicate, JpaWindow)
	 */
	@Incubating
	JpaExpression<Long> count(Expression<?> argument, JpaWindow window);

	/**
	 * Create a {@code count} aggregate function expression.
	 *
	 * @param argument argument to the function
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 *
	 * @return aggregate function expression
	 *
	 * @see #functionAggregate(String, Class, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	JpaExpression<Long> count(Expression<?> argument, JpaPredicate filter, JpaWindow window);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordered-Set Aggregate functions

	/**
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	<T> JpaExpression<T> functionWithinGroup(String name, Class<T> type, JpaOrder order, Expression<?>... args);

	/**
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	<T> JpaExpression<T> functionWithinGroup(
			String name,
			Class<T> type,
			JpaOrder order,
			JpaPredicate filter,
			Expression<?>... args);

	/**
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	<T> JpaExpression<T> functionWithinGroup(
			String name,
			Class<T> type,
			JpaOrder order,
			JpaWindow window,
			Expression<?>... args);

	/**
	 * Create a generic ordered set-aggregate function expression.
	 *
	 * @param name name of the ordered set-aggregate function
	 * @param type type of this expression
	 * @param order order by clause used in within group
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 * @param args optional arguments to the function
	 * @param <T> type of this expression
	 *
	 * @return ordered set-aggregate function expression
	 */
	@Incubating
	<T> JpaExpression<T> functionWithinGroup(
			String name,
			Class<T> type,
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<?>... args);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Incubating
	JpaExpression<String> listagg(JpaOrder order, Expression<String> argument, String separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Incubating
	JpaExpression<String> listagg(JpaOrder order, Expression<String> argument, Expression<String> separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Incubating
	JpaExpression<String> listagg(JpaOrder order, JpaPredicate filter, Expression<String> argument, String separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Incubating
	JpaExpression<String> listagg(
			JpaOrder order,
			JpaPredicate filter,
			Expression<String> argument,
			Expression<String> separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Incubating
	JpaExpression<String> listagg(JpaOrder order, JpaWindow window, Expression<String> argument, String separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Incubating
	JpaExpression<String> listagg(
			JpaOrder order,
			JpaWindow window,
			Expression<String> argument,
			Expression<String> separator);

	/**
	 * @see #listagg(JpaOrder, JpaPredicate, JpaWindow, Expression, Expression)
	 */
	@Incubating
	JpaExpression<String> listagg(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<String> argument,
			String separator);

	/**
	 * Create a {@code listagg} ordered set-aggregate function expression.
	 *
	 * @param order order by clause used in within group
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 * @param argument values to join
	 * @param separator the separator used to join the values
	 *
	 * @return ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	JpaExpression<String> listagg(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<String> argument,
			Expression<String> separator);

	/**
	 * @see #mode(JpaPredicate, JpaWindow, Expression, SortDirection, NullPrecedence)
	 */
	@Incubating
	<T> JpaExpression<T> mode(Expression<T> sortExpression, SortDirection sortOrder, NullPrecedence nullPrecedence);

	/**
	 * @see #mode(JpaPredicate, JpaWindow, Expression, SortDirection, NullPrecedence)
	 */
	@Incubating
	<T> JpaExpression<T> mode(
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	/**
	 * @see #mode(JpaPredicate, JpaWindow, Expression, SortDirection, NullPrecedence)
	 */
	@Incubating
	<T> JpaExpression<T> mode(
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	/**
	 * Create a {@code mode} ordered set-aggregate function expression.
	 *
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 * @param sortExpression the sort expression
	 * @param sortOrder the sort order
	 * @param nullPrecedence the null precedence
	 * @param <T> type of this expression
	 *
	 * @return ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	<T> JpaExpression<T> mode(
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	/**
	 * @see #percentileCont(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, NullPrecedence)
	 */
	@Incubating
	<T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	/**
	 * @see #percentileCont(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, NullPrecedence)
	 */
	@Incubating
	<T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	/**
	 * @see #percentileCont(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, NullPrecedence)
	 */
	@Incubating
	<T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	/**
	 * Create a {@code percentile_cont} ordered set-aggregate function expression.
	 *
	 * @param argument argument to the function
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 * @param sortExpression the sort expression
	 * @param sortOrder the sort order
	 * @param nullPrecedence the null precedence
	 *
	 * @return ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	<T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	/**
	 * @see #percentileDisc(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, NullPrecedence)
	 */
	@Incubating
	<T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	/**
	 * @see #percentileDisc(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, NullPrecedence)
	 */
	@Incubating
	<T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	/**
	 * @see #percentileDisc(Expression, JpaPredicate, JpaWindow, Expression, SortDirection, NullPrecedence)
	 */
	@Incubating
	<T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	/**
	 * Create a {@code percentile_disc} ordered set-aggregate function expression.
	 *
	 * @param argument argument to the function
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 * @param sortExpression the sort expression
	 * @param sortOrder the sort order
	 * @param nullPrecedence the null precedence
	 *
	 * @return ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	<T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence);

	/**
	 * @see #rank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	JpaExpression<Long> rank(JpaOrder order, Expression<?>... arguments);

	/**
	 * @see #rank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	JpaExpression<Long> rank(JpaOrder order, JpaPredicate filter, Expression<?>... arguments);

	/**
	 * @see #rank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	JpaExpression<Long> rank(JpaOrder order, JpaWindow window, Expression<?>... arguments);

	/**
	 * Create a {@code rank} ordered set-aggregate function expression.
	 *
	 * @param order order by clause used in within group
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 * @param arguments arguments to the function
	 *
	 * @return ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	JpaExpression<Long> rank(JpaOrder order, JpaPredicate filter, JpaWindow window, Expression<?>... arguments);

	/**
	 * @see #percentRank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	JpaExpression<Double> percentRank(JpaOrder order, Expression<?>... arguments);

	/**
	 * @see #percentRank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	JpaExpression<Double> percentRank(JpaOrder order, JpaPredicate filter, Expression<?>... arguments);

	/**
	 * @see #percentRank(JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	JpaExpression<Double> percentRank(JpaOrder order, JpaWindow window, Expression<?>... arguments);

	/**
	 * Create a {@code percent_rank} ordered set-aggregate function expression.
	 *
	 * @param order order by clause used in within group
	 * @param filter optional filter clause
	 * @param window optional window over which to apply the function
	 * @param arguments arguments to the function
	 *
	 * @return ordered set-aggregate expression
	 *
	 * @see #functionWithinGroup(String, Class, JpaOrder, JpaPredicate, JpaWindow, Expression...)
	 */
	@Incubating
	JpaExpression<Double> percentRank(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<?>... arguments);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Array functions for array types

	/**
	 * @see #arrayAgg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayAgg(JpaOrder order, Expression<? extends T> argument);

	/**
	 * @see #arrayAgg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayAgg(JpaOrder order, JpaPredicate filter, Expression<? extends T> argument);

	/**
	 * @see #arrayAgg(JpaOrder, JpaPredicate, JpaWindow, Expression)
	 * @since 6.4
	 */
	@Incubating
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
	@Incubating
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
	@Incubating
	<T> JpaExpression<T[]> arrayLiteral(T... elements);

	/**
	 * Determines the length of an array.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<Integer> arrayLength(Expression<T[]> arrayExpression);

	/**
	 * Determines the 1-based position of an element in an array.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<Integer> arrayPosition(Expression<T[]> arrayExpression, T element);

	/**
	 * Determines the 1-based position of an element in an array.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<Integer> arrayPosition(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	/**
	 * Determines all 1-based positions of an element in an array.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<int[]> arrayPositions(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	/**
	 * Determines all 1-based positions of an element in an array.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<int[]> arrayPositions(Expression<T[]> arrayExpression, T element);

	/**
	 * Determines all 1-based positions of an element in an array.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<List<Integer>> arrayPositionsList(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	/**
	 * Determines all 1-based positions of an element in an array.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<List<Integer>> arrayPositionsList(Expression<T[]> arrayExpression, T element);

	/**
	 * Concatenates arrays with each other in order.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayConcat(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2);

	/**
	 * Concatenates arrays with each other in order.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayConcat(Expression<T[]> arrayExpression1, T[] array2);

	/**
	 * Concatenates arrays with each other in order.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayConcat(T[] array1, Expression<T[]> arrayExpression2);

	/**
	 * Appends element to array.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayAppend(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	/**
	 * Appends element to array.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayAppend(Expression<T[]> arrayExpression, T element);

	/**
	 * Prepends element to array.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayPrepend(Expression<T> elementExpression, Expression<T[]> arrayExpression);

	/**
	 * Prepends element to array.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayPrepend(T element, Expression<T[]> arrayExpression);

	/**
	 * Accesses the element of an array by 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T> arrayGet(Expression<T[]> arrayExpression, Expression<Integer> indexExpression);

	/**
	 * Accesses the element of an array by 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T> arrayGet(Expression<T[]> arrayExpression, Integer index);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arraySet(Expression<T[]> arrayExpression, Expression<Integer> indexExpression, Expression<T> elementExpression);
	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arraySet(Expression<T[]> arrayExpression, Expression<Integer> indexExpression, T element);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arraySet(Expression<T[]> arrayExpression, Integer index, Expression<T> elementExpression);

	/**
	 * Creates array copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arraySet(Expression<T[]> arrayExpression, Integer index, T element);

	/**
	 * Creates array copy with given element removed.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayRemove(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	/**
	 * Creates array copy with given element removed.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayRemove(Expression<T[]> arrayExpression, T element);

	/**
	 * Creates array copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayRemoveIndex(Expression<T[]> arrayExpression, Expression<Integer> indexExpression);

	/**
	 * Creates array copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayRemoveIndex(Expression<T[]> arrayExpression, Integer index);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arraySlice(Expression<T[]> arrayExpression, Expression<Integer> lowerIndexExpression, Expression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arraySlice(Expression<T[]> arrayExpression, Expression<Integer> lowerIndexExpression, Integer upperIndex);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arraySlice(Expression<T[]> arrayExpression, Integer lowerIndex, Expression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-array of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arraySlice(Expression<T[]> arrayExpression, Integer lowerIndex, Integer upperIndex);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayReplace(Expression<T[]> arrayExpression, Expression<T> oldElementExpression, Expression<T> newElementExpression);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayReplace(Expression<T[]> arrayExpression, Expression<T> oldElementExpression, T newElement);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayReplace(Expression<T[]> arrayExpression, T oldElement, Expression<T> newElementExpression);

	/**
	 * Creates array copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayReplace(Expression<T[]> arrayExpression, T oldElement, T newElement);

	/**
	 * Creates array copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayTrim(Expression<T[]> arrayExpression, Expression<Integer> elementCountExpression);

	/**
	 * Creates array copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayTrim(Expression<T[]> arrayExpression, Integer elementCount);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayFill(Expression<T> elementExpression, Expression<Integer> elementCountExpression);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayFill(Expression<T> elementExpression, Integer elementCount);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayFill(T element, Expression<Integer> elementCountExpression);

	/**
	 * Creates array with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<T[]> arrayFill(T element, Integer elementCount);

	/**
	 * Concatenates the non-null array elements with a separator, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	JpaExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, Expression<String> separatorExpression);

	/**
	 * Concatenates the non-null array elements with a separator, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	JpaExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, String separator);
	/**
	 * Whether an array contains an element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaPredicate arrayContains(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	/**
	 * Whether an array contains an element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaPredicate arrayContains(Expression<T[]> arrayExpression, T element);

	/**
	 * Whether an array contains an element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaPredicate arrayContains(T[] array, Expression<T> elementExpression);

	/**
	 * Whether an array contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaPredicate arrayContainsNullable(Expression<T[]> arrayExpression, Expression<T> elementExpression);

	/**
	 * Whether an array contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaPredicate arrayContainsNullable(Expression<T[]> arrayExpression, T element);

	/**
	 * Whether an array contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaPredicate arrayContainsNullable(T[] array, Expression<T> elementExpression);

	/**
	 * Whether an array is a subset of another array.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIncludes(Expression, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayContainsAll(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression) {
		return arrayIncludes( arrayExpression, subArrayExpression );
	}

	/**
	 * Whether an array is a subset of another array.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIncludes(Expression, Object[])}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayContainsAll(Expression<T[]> arrayExpression, T[] subArray) {
		return arrayIncludes( arrayExpression, subArray );
	}

	/**
	 * Whether an array is a subset of another array.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIncludes(Object[], Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayContainsAll(T[] array, Expression<T[]> subArrayExpression) {
		return arrayIncludes( array, subArrayExpression );
	}

	/**
	 * Whether an array is a subset of another array with nullable elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIncludesNullable(Expression, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayContainsAllNullable(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression) {
		return arrayIncludesNullable( arrayExpression, subArrayExpression );
	}

	/**
	 * Whether an array is a subset of another array with nullable elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIncludesNullable(Expression, Object[])}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayContainsAllNullable(Expression<T[]> arrayExpression, T[] subArray) {
		return arrayIncludesNullable( arrayExpression, subArray );
	}

	/**
	 * Whether an array is a subset of another array with nullable elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIncludesNullable(Object[], Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayContainsAllNullable(T[] array, Expression<T[]> subArrayExpression) {
		return arrayIncludesNullable( array, subArrayExpression );
	}

	/**
	 * Whether an array is a subset of another array.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIncludes(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression);

	/**
	 * Whether an array is a subset of another array.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIncludes(Expression<T[]> arrayExpression, T[] subArray);

	/**
	 * Whether an array is a subset of another array.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIncludes(T[] array, Expression<T[]> subArrayExpression);

	/**
	 * Whether an array is a subset of another array with nullable elements.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIncludesNullable(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression);

	/**
	 * Whether an array is a subset of another array with nullable elements.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIncludesNullable(Expression<T[]> arrayExpression, T[] subArray);

	/**
	 * Whether an array is a subset of another array with nullable elements.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIncludesNullable(T[] array, Expression<T[]> subArrayExpression);

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIntersects(Expression, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayOverlaps(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return arrayIntersects( arrayExpression1, arrayExpression2 );
	}

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIntersects(Expression, Object[])}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayOverlaps(Expression<T[]> arrayExpression1, T[] array2) {
		return arrayIntersects( arrayExpression1, array2 );
	}

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIntersects(Object[], Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayOverlaps(T[] array1, Expression<T[]> arrayExpression2) {
		return arrayIntersects( array1, arrayExpression2 );
	}

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIntersectsNullable(Expression, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayOverlapsNullable(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return arrayIntersectsNullable( arrayExpression1, arrayExpression2 );
	}

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIntersectsNullable(Expression, Object[])}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayOverlapsNullable(Expression<T[]> arrayExpression1, T[] array2) {
		return arrayIntersectsNullable( arrayExpression1, array2 );
	}

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #arrayIntersectsNullable(Object[], jakarta.persistence.criteria.Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <T> JpaPredicate arrayOverlapsNullable(T[] array1, Expression<T[]> arrayExpression2) {
		return arrayIntersectsNullable( array1, arrayExpression2 );
	}

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIntersects(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2);

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIntersects(Expression<T[]> arrayExpression1, T[] array2);

	/**
	 * Whether one array has any elements common with another array.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIntersects(T[] array1, Expression<T[]> arrayExpression2);

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIntersectsNullable(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2);

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIntersectsNullable(Expression<T[]> arrayExpression1, T[] array2);

	/**
	 * Whether one array has any elements common with another array, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Incubating
	<T> JpaPredicate arrayIntersectsNullable(T[] array1, Expression<T[]> arrayExpression2);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Array functions for collection types

	/**
	 * Creates a basic collection literal with the {@code array} constructor function.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<E>> JpaExpression<C> collectionLiteral(E... elements);

	/**
	 * Determines the length of a basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	JpaExpression<Integer> collectionLength(Expression<? extends Collection<?>> collectionExpression);

	/**
	 * Determines the 1-based position of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaExpression<Integer> collectionPosition(Expression<? extends Collection<? extends E>> collectionExpression, E element);

	/**
	 * Determines the 1-based position of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaExpression<Integer> collectionPosition(Expression<? extends Collection<? extends E>> collectionExpression, Expression<E> elementExpression);

	/**
	 * Determines all 1-based positions of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<int[]> collectionPositions(Expression<? extends Collection<? super T>> collectionExpression, Expression<T> elementExpression);

	/**
	 * Determines all 1-based positions of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<int[]> collectionPositions(Expression<? extends Collection<? super T>> collectionExpression, T element);

	/**
	 * Determines all 1-based positions of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<List<Integer>> collectionPositionsList(Expression<? extends Collection<? super T>> collectionExpression, Expression<T> elementExpression);

	/**
	 * Determines all 1-based positions of an element in a basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<List<Integer>> collectionPositionsList(Expression<? extends Collection<? super T>> collectionExpression, T element);

	/**
	 * Concatenates basic collections with each other in order.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(Expression<C> collectionExpression1, Expression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Concatenates basic collections with each other in order.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(Expression<C> collectionExpression1, Collection<? extends E> collection2);

	/**
	 * Concatenates basic collections with each other in order.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(C collection1, Expression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Appends element to basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionAppend(Expression<C> collectionExpression, Expression<? extends E> elementExpression);

	/**
	 * Appends element to basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionAppend(Expression<C> collectionExpression, E element);

	/**
	 * Prepends element to basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionPrepend(Expression<? extends E> elementExpression, Expression<C> collectionExpression);

	/**
	 * Prepends element to basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionPrepend(E element, Expression<C> collectionExpression);

	/**
	 * Accesses the element of the basic collection by 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaExpression<E> collectionGet(Expression<? extends Collection<E>> collectionExpression, Expression<Integer> indexExpression);

	/**
	 * Accesses the element of the basic collection by 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaExpression<E> collectionGet(Expression<? extends Collection<E>> collectionExpression, Integer index);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionSet(Expression<C> collectionExpression, Expression<Integer> indexExpression, Expression<? extends E> elementExpression);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionSet(Expression<C> collectionExpression, Expression<Integer> indexExpression, E element);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionSet(Expression<C> collectionExpression, Integer index, Expression<? extends E> elementExpression);

	/**
	 * Creates basic collection copy with given element at given 1-based index.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionSet(Expression<C> collectionExpression, Integer index, E element);

	/**
	 * Creates basic collection copy with given element removed.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionRemove(Expression<C> collectionExpression, Expression<? extends E> elementExpression);

	/**
	 * Creates basic collection copy with given element removed.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionRemove(Expression<C> collectionExpression, E element);

	/**
	 * Creates basic collection copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	@Incubating
	<C extends Collection<?>> JpaExpression<C> collectionRemoveIndex(Expression<C> collectionExpression, Expression<Integer> indexExpression);

	/**
	 * Creates basic collection copy with the element at the given 1-based index removed.
	 *
	 * @since 6.4
	 */
	@Incubating
	<C extends Collection<?>> JpaExpression<C> collectionRemoveIndex(Expression<C> collectionExpression, Integer index);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Incubating
	<C extends Collection<?>> JpaExpression<C> collectionSlice(Expression<C> collectionExpression, Expression<Integer> lowerIndexExpression, Expression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Incubating
	<C extends Collection<?>> JpaExpression<C> collectionSlice(Expression<C> collectionExpression, Expression<Integer> lowerIndexExpression, Integer upperIndex);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Incubating
	<C extends Collection<?>> JpaExpression<C> collectionSlice(Expression<C> collectionExpression, Integer lowerIndex, Expression<Integer> upperIndexExpression);

	/**
	 * Creates a sub-collection of the based on 1-based lower and upper index.
	 * Both indexes are inclusive.
	 *
	 * @since 6.4
	 */
	@Incubating
	<C extends Collection<?>> JpaExpression<C> collectionSlice(Expression<C> collectionExpression, Integer lowerIndex, Integer upperIndex);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(Expression<C> collectionExpression, Expression<? extends E> oldElementExpression, Expression<? extends E> newElementExpression);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(Expression<C> collectionExpression, Expression<? extends E> oldElementExpression, E newElement);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(Expression<C> collectionExpression, E oldElement, Expression<? extends E> newElementExpression);

	/**
	 * Creates basic collection copy replacing a given element with another.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(Expression<C> collectionExpression, E oldElement, E newElement);

	/**
	 * Creates basic collection copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	@Incubating
	<C extends Collection<?>> JpaExpression<C> collectionTrim(Expression<C> arrayExpression, Expression<Integer> elementCountExpression);

	/**
	 * Creates basic collection copy without the last N elements, specified by the second argument.
	 *
	 * @since 6.4
	 */
	@Incubating
	<C extends Collection<?>> JpaExpression<C> collectionTrim(Expression<C> arrayExpression, Integer elementCount);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<Collection<T>> collectionFill(Expression<T> elementExpression, Expression<Integer> elementCountExpression);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<Collection<T>> collectionFill(Expression<T> elementExpression, Integer elementCount);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<Collection<T>> collectionFill(T element, Expression<Integer> elementCountExpression);

	/**
	 * Creates basic collection with the same element N times, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<Collection<T>> collectionFill(T element, Integer elementCount);

	/**
	 * Concatenates the non-null basic collection elements with a separator, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, Expression<String> separatorExpression);

	/**
	 * Concatenates the non-null basic collection elements with a separator, as specified by the arguments.
	 *
	 * @since 6.4
	 */
	@Incubating
	<T> JpaExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, String separator);

	/**
	 * Whether a basic collection contains an element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionContains(Expression<? extends Collection<E>> collectionExpression, Expression<? extends E> elementExpression);

	/**
	 * Whether a basic collection contains an element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionContains(Expression<? extends Collection<E>> collectionExpression, E element);

	/**
	 * Whether a basic collection contains an element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionContains(Collection<E> collection, Expression<E> elementExpression);

	/**
	 * Whether a basic collection contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionContainsNullable(Expression<? extends Collection<E>> collectionExpression, Expression<? extends E> elementExpression);

	/**
	 * Whether a basic collection contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionContainsNullable(Expression<? extends Collection<E>> collectionExpression, E element);

	/**
	 * Whether a basic collection contains a nullable element.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionContainsNullable(Collection<E> collection, Expression<E> elementExpression);

	/**
	 * Whether a basic collection is a subset of another basic collection.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIncludes(Expression, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionContainsAll(Expression<? extends Collection<E>> collectionExpression, Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return collectionIncludes( collectionExpression, subCollectionExpression );
	}

	/**
	 * Whether a basic collection is a subset of another basic collection.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIncludes(Expression, Collection)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionContainsAll(Expression<? extends Collection<E>> collectionExpression, Collection<? extends E> subCollection) {
		return collectionIncludes( collectionExpression, subCollection );
	}

	/**
	 * Whether a basic collection is a subset of another basic collection.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIncludes(Collection, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionContainsAll(Collection<E> collection, Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return collectionIncludes( collection, subCollectionExpression );
	}

	/**
	 * Whether a basic collection is a subset of another basic collection with nullable elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIncludesNullable(Expression, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionContainsAllNullable(Expression<? extends Collection<E>> collectionExpression, Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return collectionIncludesNullable( collectionExpression, subCollectionExpression );
	}

	/**
	 * Whether a basic collection is a subset of another basic collection with nullable elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIncludesNullable(Expression, Collection)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionContainsAllNullable(Expression<? extends Collection<E>> collectionExpression, Collection<? extends E> subCollection) {
		return collectionIncludesNullable( collectionExpression, subCollection );
	}

	/**
	 * Whether a basic collection is a subset of another basic collection with nullable elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIncludesNullable(Collection, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionContainsAllNullable(Collection<E> collection, Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return collectionIncludesNullable( collection, subCollectionExpression );
	}

	/**
	 * Whether a basic collection is a subset of another basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionIncludes(Expression<? extends Collection<E>> collectionExpression, Expression<? extends Collection<? extends E>> subCollectionExpression);

	/**
	 * Whether a basic collection is a subset of another basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionIncludes(Expression<? extends Collection<E>> collectionExpression, Collection<? extends E> subCollection);

	/**
	 * Whether a basic collection is a subset of another basic collection.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionIncludes(Collection<E> collection, Expression<? extends Collection<? extends E>> subCollectionExpression);

	/**
	 * Whether a basic collection is a subset of another basic collection with nullable elements.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionIncludesNullable(Expression<? extends Collection<E>> collectionExpression, Expression<? extends Collection<? extends E>> subCollectionExpression);

	/**
	 * Whether a basic collection is a subset of another basic collection with nullable elements.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionIncludesNullable(Expression<? extends Collection<E>> collectionExpression, Collection<? extends E> subCollection);

	/**
	 * Whether a basic collection is a subset of another basic collection with nullable elements.
	 *
	 * @since 6.4
	 */
	@Incubating
	<E> JpaPredicate collectionIncludesNullable(Collection<E> collection, Expression<? extends Collection<? extends E>> subCollectionExpression);

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIntersects(Expression, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionOverlaps(Expression<? extends Collection<E>> collectionExpression1, Expression<? extends Collection<? extends E>> collectionExpression2) {
		return collectionIntersects( collectionExpression1, collectionExpression2 );
	}

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIntersects(Expression, Collection)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionOverlaps(Expression<? extends Collection<E>> collectionExpression1, Collection<? extends E> collection2) {
		return collectionIntersects( collectionExpression1, collection2 );
	}

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIntersects(Collection, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionOverlaps(Collection<E> collection1, Expression<? extends Collection<? extends E>> collectionExpression2) {
		return collectionIntersects( collection1, collectionExpression2 );
	}

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIntersectsNullable(Expression, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionOverlapsNullable(Expression<? extends Collection<E>> collectionExpression1, Expression<? extends Collection<? extends E>> collectionExpression2) {
		return collectionIntersectsNullable( collectionExpression1, collectionExpression2 );
	}

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIntersectsNullable(Expression, Collection)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionOverlapsNullable(Expression<? extends Collection<E>> collectionExpression1, Collection<? extends E> collection2) {
		return collectionIntersectsNullable( collectionExpression1, collection2 );
	}

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.4
	 * @deprecated Replaced with {@link #collectionIntersectsNullable(Collection, Expression)}
	 */
	@Incubating
	@Deprecated(forRemoval = true)
	default <E> JpaPredicate collectionOverlapsNullable(Collection<E> collection1, Expression<? extends Collection<? extends E>> collectionExpression2) {
		return collectionIntersectsNullable( collection1, collectionExpression2 );
	}

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.6
	 */
	@Incubating
	<E> JpaPredicate collectionIntersects(Expression<? extends Collection<E>> collectionExpression1, Expression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.6
	 */
	@Incubating
	<E> JpaPredicate collectionIntersects(Expression<? extends Collection<E>> collectionExpression1, Collection<? extends E> collection2);

	/**
	 * Whether one basic collection has any elements common with another basic collection.
	 *
	 * @since 6.6
	 */
	@Incubating
	<E> JpaPredicate collectionIntersects(Collection<E> collection1, Expression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Incubating
	<E> JpaPredicate collectionIntersectsNullable(Expression<? extends Collection<E>> collectionExpression1, Expression<? extends Collection<? extends E>> collectionExpression2);

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Incubating
	<E> JpaPredicate collectionIntersectsNullable(Expression<? extends Collection<E>> collectionExpression1, Collection<? extends E> collection2);

	/**
	 * Whether one basic collection has any elements common with another basic collection, supporting {@code null} elements.
	 *
	 * @since 6.6
	 */
	@Incubating
	<E> JpaPredicate collectionIntersectsNullable(Collection<E> collection1, Expression<? extends Collection<? extends E>> collectionExpression2);


	@Override
	JpaPredicate and(List<Predicate> restrictions);

	@Override
	JpaPredicate or(List<Predicate> restrictions);

	@Override
	JpaExpression<String> concat(List<Expression<String>> expressions);

	@Override
	<N, T extends Temporal> JpaExpression<N> extract(TemporalField<N, T> field, Expression<T> temporal);
}
