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
import javax.persistence.Tuple;
import javax.persistence.criteria.CollectionJoin;
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

import org.hibernate.NullPrecedence;
import org.hibernate.SortOrder;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmRestrictedSubQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.SqmInPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Adapts the JPA CriteriaBuilder to generate SQM nodes.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public interface NodeBuilder extends HibernateCriteriaBuilder {
	MetamodelImplementor getDomainModel();

	default TypeConfiguration getTypeConfiguration() {
		return getDomainModel().getTypeConfiguration();
	}

	ServiceRegistry getServiceRegistry();

	QueryEngine getQueryEngine();

	@Override
	SqmSelectStatement<Object> createQuery();

	@Override
	<T> SqmSelectStatement<T> createQuery(Class<T> resultClass);

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
	<T> SqmInsertSelectStatement<T> createCriteriaInsertSelect(Class<T> targetEntity);

	@Override
	<N extends Number> SqmExpression abs(Expression<N> x);

	@Override
	<X, T> SqmExpression<X> cast(JpaExpression<T> expression, Class<X> castTargetJavaType);

	@Override
	SqmPredicate wrap(Expression<Boolean> expression);

	@Override
	SqmPredicate wrap(Expression<Boolean>... expressions);

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

	@Override
	<T> SqmParameter<T> parameter(Class<T> paramClass);

	@Override
	<T> SqmParameter<T> parameter(Class<T> paramClass, String name);

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
	<T> SqmFunction<T> function(
			String name, Class<T> type, Expression<?>[] args);

	@Override
	<Y> SqmRestrictedSubQueryExpression<Y> all(Subquery<Y> subquery);

	@Override
	<Y> SqmRestrictedSubQueryExpression<Y> some(Subquery<Y> subquery);

	@Override
	<Y> SqmRestrictedSubQueryExpression<Y> any(Subquery<Y> subquery);

	@Override
	<K, M extends Map<K, ?>> JpaExpression<Set<K>> keys(M map);

	@Override
	<K, L extends List<?>> JpaExpression<Set<K>> indexes(L list);

	@Override
	<V, C extends Collection<V>> JpaExpression<Collection<V>> values(C collection);

	@Override
	<V, M extends Map<?, V>> Expression<Collection<V>> values(M map);

	@Override
	<C extends Collection<?>> JpaExpression<Integer> size(Expression<C> collection);

	@Override
	<C extends Collection<?>> JpaExpression<Integer> size(C collection);

	@Override
	<T> JpaCoalesce<T> coalesce();

	@Override
	<Y> JpaCoalesce<Y> coalesce(
			Expression<? extends Y> x, Expression<? extends Y> y);

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

	@Override
	<R> SqmTuple<R> tuple(
			Class<R> tupleType,
			JpaExpression<?>... expressions);

	@Override
	<R> SqmTuple<R> tuple(
			Class<R> tupleType,
			List<JpaExpression<?>> expressions);

	@Override
	<R> SqmTuple<R> tuple(
			DomainType<R> tupleType,
			JpaExpression<?>... expressions);

	@Override
	<R> SqmTuple<R> tuple(
			DomainType<R> tupleType,
			List<JpaExpression<?>> expressions);

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
	<Y extends Comparable<? super Y>> SqmPredicate greaterThan(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThan(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate greaterThanOrEqualTo(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThan(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThan(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(Expression<? extends Y> x, Expression<? extends Y> y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate lessThanOrEqualTo(Expression<? extends Y> x, Y y);

	@Override
	<Y extends Comparable<? super Y>> SqmPredicate between(Expression<? extends Y> value, Expression<? extends Y> lower, Expression<? extends Y> upper);

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
	<T> SqmInPredicate in(Expression<? extends T> expression);

	@Override
	<T> SqmInPredicate in(Expression<? extends T> expression, Expression<? extends T>... values);

	@Override
	<T> SqmInPredicate in(Expression<? extends T> expression, T... values);

	@Override
	<T> SqmInPredicate in(Expression<? extends T> expression, List<T> values);

	<T> SqmInPredicate in(Expression<? extends T> expression, SqmSubQuery<T> subQuery);

	@Override
	SqmPredicate exists(Subquery<?> subquery);

	@Override
	<M extends Map<?, ?>> SqmPredicate isMapEmpty(JpaExpression<M> mapExpression);

	@Override
	<M extends Map<?, ?>> SqmPredicate isMapNotEmpty(JpaExpression<M> mapExpression);

	@Override
	<M extends Map<?,?>> SqmExpression<Integer> mapSize(JpaExpression<M> mapExpression);

	@Override
	SqmExpression<Integer> mapSize(Map map);

	@Override
	SqmSortSpecification sort(
			JpaExpression<?> sortExpression,
			SortOrder sortOrder,
			NullPrecedence nullPrecedence);

	@Override
	SqmSortSpecification sort(JpaExpression<?> sortExpression, SortOrder sortOrder);

	@Override
	SqmSortSpecification sort(JpaExpression<?> sortExpression);

	@Override
	SqmSortSpecification asc(Expression<?> x);

	@Override
	SqmSortSpecification desc(Expression<?> x);
}
