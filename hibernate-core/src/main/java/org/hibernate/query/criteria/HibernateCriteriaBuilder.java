/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Tuple;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;

import org.hibernate.SessionFactory;
import org.hibernate.sqm.parser.criteria.tree.JpaCriteriaQuery;
import org.hibernate.sqm.parser.criteria.tree.from.JpaRoot;
import org.hibernate.sqm.parser.criteria.tree.select.JpaCompoundSelection;

/**
 * Hibernate extensions to the JPA CriteriaBuilder.
 *
 * @author Steve Ebersole
 */
public interface HibernateCriteriaBuilder extends CriteriaBuilder {
	SessionFactory getEntityManagerFactory();

	// in-flight ideas:
	//		* operator corresponding to the new "matches" HQL operator
	//		* match for our expanded dynamic-instantiation support - actually this may already be supported,
	// 				outside of checks done in #checkMultiSelect
	//		* ?generic support for SQL restrictions? - ala Restrictions.sqlRestriction
	//		* port query-by-example support - org.hibernate.criterion.Example

	/**
	 * Centralize checking of criteria query multi-selects as defined by the
	 * {@link CriteriaQuery#multiselect(List)}  method.
	 *
	 * @param selections The selection varargs to check
	 *
	 * @throws IllegalArgumentException If the selection items are not valid per {@link CriteriaQuery#multiselect}
	 * documentation.
	 * <i>&quot;An argument to the multiselect method must not be a tuple-
	 * or array-valued compound selection item.&quot;</i>
	 */
	void checkMultiSelect(List<Selection<?>> selections);

	void checkIsJpaExpression(Selection<?> selection);

	void checkIsJpaExpression(Expression<?> expression);

	void checkIsJpaPredicate(Predicate predicate);

	JpaPredicateImplementor wrap(Expression<Boolean> expression);

	/**
	 * Create a predicate that tests whether a Map is empty.
	 * <p/>
	 * NOTE : Due to type-erasure we cannot name this the same as
	 * {@link CriteriaBuilder#isEmpty}
	 *
	 *
	 * @param mapExpression The expression resolving to a Map which we
	 * want to check for emptiness
	 *
	 * @return is-empty predicate
	 */
	<M extends Map<?,?>> JpaPredicateImplementor isMapEmpty(Expression<M> mapExpression);

	/**
	 * Create a predicate that tests whether a Map is
	 * not empty.
	 * <p/>
	 * NOTE : Due to type-erasure we cannot name this the same as
	 * {@link CriteriaBuilder#isNotEmpty}
	 *
	 * @param mapExpression The expression resolving to a Map which we
	 * want to check for non-emptiness
	 *
	 * @return is-not-empty predicate
	 */
	<M extends Map<?,?>> JpaPredicateImplementor isMapNotEmpty(Expression<M> mapExpression);

	/**
	 * Create an expression that tests the size of a map.
	 * <p/>
	 * NOTE : Due to type-erasure we cannot name this the same as
	 * {@link CriteriaBuilder#size}
	 *
	 * @param mapExpression The expression resolving to a Map for which we
	 * want to know the size
	 *
	 * @return size expression
	 */
	<M extends Map<?,?>> JpaExpressionImplementor<Integer> mapSize(Expression<M> mapExpression);

	/**
	 * Create an expression that tests the size of a map.
	 *
	 * @param map The Map for which we want to know the size
	 *
	 * @return size expression
	 */
	<M extends Map<?,?>> JpaExpressionImplementor<Integer> mapSize(M map);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Co-variant overrides

	@Override
	JpaCriteriaQuery<Object> createQuery();

	@Override
	<T> JpaCriteriaQuery<T> createQuery(Class<T> resultClass);

	@Override
	JpaCriteriaQuery<Tuple> createTupleQuery();

	@Override
	<T> CriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity);

	@Override
	<T> CriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity);

	@Override
	<Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, Selection<?>[] selections);

	@Override
	JpaCompoundSelection<Tuple> tuple(Selection<?>[] selections);

	@Override
	JpaCompoundSelection<Object[]> array(Selection<?>[] selections);

	@Override
	<N extends Number> JpaExpressionImplementor<Double> avg(Expression<N> x);

	@Override
	<N extends Number> JpaExpressionImplementor<N> sum(Expression<N> x);

	@Override
	JpaExpressionImplementor<Long> sumAsLong(Expression<Integer> x);

	@Override
	JpaExpressionImplementor<Double> sumAsDouble(Expression<Float> x);

	@Override
	<N extends Number> JpaExpressionImplementor<N> max(Expression<N> x);

	@Override
	<N extends Number> JpaExpressionImplementor<N> min(Expression<N> x);

	@Override
	<X extends Comparable<? super X>> JpaExpressionImplementor<X> greatest(Expression<X> x);

	@Override
	<X extends Comparable<? super X>> JpaExpressionImplementor<X> least(Expression<X> x);

	@Override
	JpaExpressionImplementor<Long> count(Expression<?> x);

	@Override
	JpaExpressionImplementor<Long> countDistinct(Expression<?> x);

	@Override
	JpaPredicateImplementor exists(Subquery<?> subquery);

	@Override
	<Y> JpaExpressionImplementor<Y> all(Subquery<Y> subquery);

	@Override
	<Y> JpaExpressionImplementor<Y> some(Subquery<Y> subquery);

	@Override
	<Y> JpaExpressionImplementor<Y> any(Subquery<Y> subquery);

	@Override
	JpaPredicateImplementor and(Expression<Boolean> x, Expression<Boolean> y);

	@Override
	JpaPredicateImplementor and(Predicate... restrictions);

	@Override
	JpaPredicateImplementor or(Expression<Boolean> x, Expression<Boolean> y);

	@Override
	JpaPredicateImplementor or(Predicate... restrictions);

	@Override
	JpaPredicateImplementor not(Expression<Boolean> restriction);

	@Override
	JpaPredicateImplementor conjunction();

	@Override
	JpaPredicateImplementor disjunction();

	@Override
	JpaPredicateImplementor isTrue(Expression<Boolean> x);

	@Override
	JpaPredicateImplementor isFalse(Expression<Boolean> x);

	@Override
	JpaPredicateImplementor isNull(Expression<?> x);

	@Override
	JpaPredicateImplementor isNotNull(Expression<?> x);

	@Override
	JpaPredicateImplementor equal(Expression<?> x, Expression<?> y);

	@Override
	JpaPredicateImplementor equal(Expression<?> x, Object y);

	@Override
	JpaPredicateImplementor notEqual(Expression<?> x, Expression<?> y);

	@Override
	JpaPredicateImplementor notEqual(Expression<?> x, Object y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicateImplementor greaterThan(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicateImplementor greaterThan(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicateImplementor greaterThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicateImplementor greaterThanOrEqualTo(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicateImplementor lessThan(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicateImplementor lessThan(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicateImplementor lessThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicateImplementor lessThanOrEqualTo(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicateImplementor between(Expression<? extends Y> v, Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> JpaPredicateImplementor between(Expression<? extends Y> v, Y x, Y y);

	@Override
	JpaPredicateImplementor gt(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicateImplementor gt(Expression<? extends Number> x, Number y);

	@Override
	JpaPredicateImplementor ge(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicateImplementor ge(Expression<? extends Number> x, Number y);

	@Override
	JpaPredicateImplementor lt(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicateImplementor lt(Expression<? extends Number> x, Number y);

	@Override
	JpaPredicateImplementor le(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaPredicateImplementor le(Expression<? extends Number> x, Number y);

	@Override
	<N extends Number> JpaExpressionImplementor<N> neg(Expression<N> x);

	@Override
	<N extends Number> JpaExpressionImplementor<N> abs(Expression<N> x);

	@Override
	<N extends Number> JpaExpressionImplementor<N> sum(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpressionImplementor<N> sum(Expression<? extends N> x, N y);

	@Override
	<N extends Number> JpaExpressionImplementor<N> sum(N x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpressionImplementor<N> prod(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpressionImplementor<N> prod(Expression<? extends N> x, N y);

	@Override
	<N extends Number> JpaExpressionImplementor<N> prod(N x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpressionImplementor<N> diff(Expression<? extends N> x, Expression<? extends N> y);

	@Override
	<N extends Number> JpaExpressionImplementor<N> diff(Expression<? extends N> x, N y);

	@Override
	<N extends Number> JpaExpressionImplementor<N> diff(N x, Expression<? extends N> y);

	@Override
	JpaExpressionImplementor<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y);

	@Override
	JpaExpressionImplementor<Number> quot(Expression<? extends Number> x, Number y);

	@Override
	JpaExpressionImplementor<Number> quot(Number x, Expression<? extends Number> y);

	@Override
	JpaExpressionImplementor<Integer> mod(Expression<Integer> x, Expression<Integer> y);

	@Override
	JpaExpressionImplementor<Integer> mod(Expression<Integer> x, Integer y);

	@Override
	JpaExpressionImplementor<Integer> mod(Integer x, Expression<Integer> y);

	@Override
	JpaExpressionImplementor<Double> sqrt(Expression<? extends Number> x);

	@Override
	JpaExpressionImplementor<Long> toLong(Expression<? extends Number> number);

	@Override
	JpaExpressionImplementor<Integer> toInteger(Expression<? extends Number> number);

	@Override
	JpaExpressionImplementor<Float> toFloat(Expression<? extends Number> number);

	@Override
	JpaExpressionImplementor<Double> toDouble(Expression<? extends Number> number);

	@Override
	JpaExpressionImplementor<BigDecimal> toBigDecimal(Expression<? extends Number> number);

	@Override
	JpaExpressionImplementor<BigInteger> toBigInteger(Expression<? extends Number> number);

	@Override
	JpaExpressionImplementor<String> toString(Expression<Character> character);

	@Override
	<T> JpaExpressionImplementor<T> literal(T value);

	@Override
	<T> JpaExpressionImplementor<T> nullLiteral(Class<T> resultClass);

	@Override
	<T> JpaParameterExpression<T> parameter(Class<T> paramClass);

	@Override
	<T> JpaParameterExpression<T> parameter(Class<T> paramClass, String name);

	@Override
	<C extends Collection<?>> JpaPredicateImplementor isEmpty(Expression<C> collection);

	@Override
	<C extends Collection<?>> JpaPredicateImplementor isNotEmpty(Expression<C> collection);

	@Override
	<C extends Collection<?>> JpaExpressionImplementor<Integer> size(Expression<C> collection);

	@Override
	<C extends Collection<?>> JpaExpressionImplementor<Integer> size(C collection);

	@Override
	<E, C extends Collection<E>> JpaPredicateImplementor isMember(Expression<E> elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> JpaPredicateImplementor isMember(E elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> JpaPredicateImplementor isNotMember(Expression<E> elem, Expression<C> collection);

	@Override
	<E, C extends Collection<E>> JpaPredicateImplementor isNotMember(E elem, Expression<C> collection);

	@Override
	<V, M extends Map<?, V>> JpaExpressionImplementor<Collection<V>> values(M map);

	@Override
	<K, M extends Map<K, ?>> JpaExpressionImplementor<Set<K>> keys(M map);

	@Override
	JpaPredicateImplementor like(Expression<String> x, Expression<String> pattern);

	@Override
	JpaPredicateImplementor like(Expression<String> x, String pattern);

	@Override
	JpaPredicateImplementor like(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicateImplementor like(Expression<String> x, Expression<String> pattern, char escapeChar);

	@Override
	JpaPredicateImplementor like(Expression<String> x, String pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicateImplementor like(Expression<String> x, String pattern, char escapeChar);

	@Override
	JpaPredicateImplementor notLike(Expression<String> x, Expression<String> pattern);

	@Override
	JpaPredicateImplementor notLike(Expression<String> x, String pattern);

	@Override
	JpaPredicateImplementor notLike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicateImplementor notLike(Expression<String> x, Expression<String> pattern, char escapeChar);

	@Override
	JpaPredicateImplementor notLike(Expression<String> x, String pattern, Expression<Character> escapeChar);

	@Override
	JpaPredicateImplementor notLike(Expression<String> x, String pattern, char escapeChar);

	@Override
	JpaExpressionImplementor<String> concat(Expression<String> x, Expression<String> y);

	@Override
	JpaExpressionImplementor<String> concat(Expression<String> x, String y);

	@Override
	JpaExpressionImplementor<String> concat(String x, Expression<String> y);

	@Override
	JpaExpressionImplementor<String> substring(Expression<String> x, Expression<Integer> from);

	@Override
	JpaExpressionImplementor<String> substring(Expression<String> x, int from);

	@Override
	JpaExpressionImplementor<String> substring(Expression<String> x, Expression<Integer> from, Expression<Integer> len);

	@Override
	JpaExpressionImplementor<String> substring(Expression<String> x, int from, int len);

	@Override
	JpaExpressionImplementor<String> trim(Expression<String> x);

	@Override
	JpaExpressionImplementor<String> trim(Trimspec ts, Expression<String> x);

	@Override
	JpaExpressionImplementor<String> trim(Expression<Character> t, Expression<String> x);

	@Override
	JpaExpressionImplementor<String> trim(Trimspec ts, Expression<Character> t, Expression<String> x);

	@Override
	JpaExpressionImplementor<String> trim(char t, Expression<String> x);

	@Override
	JpaExpressionImplementor<String> trim(Trimspec ts, char t, Expression<String> x);

	@Override
	JpaExpressionImplementor<String> lower(Expression<String> x);

	@Override
	JpaExpressionImplementor<String> upper(Expression<String> x);

	@Override
	JpaExpressionImplementor<Integer> length(Expression<String> x);

	@Override
	JpaExpressionImplementor<Integer> locate(Expression<String> x, Expression<String> pattern);

	@Override
	JpaExpressionImplementor<Integer> locate(Expression<String> x, String pattern);

	@Override
	JpaExpressionImplementor<Integer> locate(Expression<String> x, Expression<String> pattern, Expression<Integer> from);

	@Override
	JpaExpressionImplementor<Integer> locate(Expression<String> x, String pattern, int from);

	@Override
	JpaExpressionImplementor<Date> currentDate();

	@Override
	JpaExpressionImplementor<Timestamp> currentTimestamp();

	@Override
	JpaExpressionImplementor<Time> currentTime();

	@Override
	<T> JpaInImplementor<T> in(Expression<? extends T> expression);

	<T> JpaInImplementor<T> in(Expression<? extends T> expression, Expression<? extends T>... values);

	<T> JpaInImplementor<T> in(Expression<? extends T> expression, T... values);

	@Override
	<Y> JpaCoalesce <Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y> JpaExpressionImplementor<Y> coalesce(Expression<? extends Y> x, Y y);

	@Override
	<Y> JpaExpressionImplementor<Y> nullif(Expression<Y> x, Expression<?> y);

	@Override
	<Y> JpaExpressionImplementor<Y> nullif(Expression<Y> x, Y y);

	@Override
	<T> JpaCoalesce<T> coalesce();

	@Override
	<C, R> JpaSimpleCase<C, R> selectCase(Expression<? extends C> expression);

	@Override
	<R> JpaSearchedCase<R> selectCase();

	@Override
	<T> JpaExpressionImplementor<T> function(String name, Class<T> type, Expression<?>[] args);

	@Override
	<X, T, V extends T> JpaAttributeJoinImplementor<X, V> treat(Join<X, T> join, Class<V> type);

	@Override
	<X, T, E extends T> JpaCollectionJoinImplementor<X, E> treat(CollectionJoin<X, T> join, Class<E> type);

	@Override
	<X, T, E extends T> JpaSetJoinImplementor<X, E> treat(SetJoin<X, T> join, Class<E> type);

	@Override
	<X, T, E extends T> JpaListJoinImplementor<X, E> treat(ListJoin<X, T> join, Class<E> type);

	@Override
	<X, K, T, V extends T> JpaMapJoinImplementor<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type);

	@Override
	<X, T extends X> JpaPathImplementor<T> treat(Path<X> path, Class<T> type);

	@Override
	<X, T extends X> JpaRoot<T> treat(Root<X> root, Class<T> type);
}
