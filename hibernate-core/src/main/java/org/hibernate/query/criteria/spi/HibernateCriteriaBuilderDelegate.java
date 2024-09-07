/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.spi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaCollectionJoin;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaCriteriaDelete;
import org.hibernate.query.criteria.JpaCriteriaInsertSelect;
import org.hibernate.query.criteria.JpaCriteriaInsertValues;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCriteriaUpdate;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaFunction;
import org.hibernate.query.criteria.JpaInPredicate;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaListJoin;
import org.hibernate.query.criteria.JpaMapJoin;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSearchOrder;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.criteria.JpaSetJoin;
import org.hibernate.query.criteria.JpaSimpleCase;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.criteria.JpaValues;
import org.hibernate.query.criteria.JpaWindow;
import org.hibernate.query.criteria.JpaWindowFrame;
import org.hibernate.query.common.TemporalUnit;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.TemporalField;

public class HibernateCriteriaBuilderDelegate implements HibernateCriteriaBuilder {
	private final HibernateCriteriaBuilder criteriaBuilder;

	public HibernateCriteriaBuilderDelegate(HibernateCriteriaBuilder criteriaBuilder) {
		this.criteriaBuilder = criteriaBuilder;
	}

	public HibernateCriteriaBuilderDelegate(CriteriaBuilder criteriaBuilder) {
		this.criteriaBuilder = (HibernateCriteriaBuilder) criteriaBuilder;
	}

	protected HibernateCriteriaBuilder getCriteriaBuilder() {
		return criteriaBuilder;
	}

	@Override
	public <X, T> JpaExpression<X> cast(JpaExpression<T> expression, Class<X> castTargetJavaType) {
		return criteriaBuilder.cast( expression, castTargetJavaType );
	}

	@Override
	public JpaPredicate wrap(Expression<Boolean> expression) {
		return criteriaBuilder.wrap( expression );
	}

	@Override @SafeVarargs
	public final JpaPredicate wrap(Expression<Boolean>... expressions) {
		return criteriaBuilder.wrap( expressions );
	}

	@Override
	public <T extends HibernateCriteriaBuilder> T unwrap(Class<T> clazz) {
		return criteriaBuilder.unwrap( clazz );
	}

	@Override
	public JpaCriteriaQuery<Object> createQuery() {
		return criteriaBuilder.createQuery();
	}

	@Override
	public <T> JpaCriteriaQuery<T> createQuery(Class<T> resultClass) {
		return criteriaBuilder.createQuery( resultClass );
	}

	@Override
	public <T> JpaCriteriaQuery<T> createQuery(String hql, Class<T> resultClass) {
		return criteriaBuilder.createQuery( hql, resultClass );
	}

	@Override
	public JpaCriteriaQuery<Tuple> createTupleQuery() {
		return criteriaBuilder.createTupleQuery();
	}

	@Override
	public <T> JpaCriteriaUpdate<T> createCriteriaUpdate(Class<T> targetEntity) {
		return criteriaBuilder.createCriteriaUpdate( targetEntity );
	}

	@Override
	public <T> JpaCriteriaDelete<T> createCriteriaDelete(Class<T> targetEntity) {
		return criteriaBuilder.createCriteriaDelete( targetEntity );
	}

	@Override
	public <T> JpaCriteriaInsertValues<T> createCriteriaInsertValues(Class<T> targetEntity) {
		return criteriaBuilder.createCriteriaInsertValues( targetEntity );
	}

	@Override
	public <T> JpaCriteriaInsertSelect<T> createCriteriaInsertSelect(Class<T> targetEntity) {
		return criteriaBuilder.createCriteriaInsertSelect( targetEntity );
	}

	@Override
	@Incubating
	public JpaValues values(Expression<?>... expressions) {
		return criteriaBuilder.values( expressions );
	}

	@Override
	@Incubating
	public JpaValues values(List<? extends Expression<?>> expressions) {
		return criteriaBuilder.values( expressions );
	}

	@Override
	public <T> JpaCriteriaQuery<T> unionAll(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return criteriaBuilder.unionAll( query1, queries );
	}

	@Override
	public <T> JpaCriteriaQuery<T> union(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return criteriaBuilder.union( query1, queries );
	}

	@Override
	public <T> JpaCriteriaQuery<T> union(boolean all, CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return criteriaBuilder.union( all, query1, queries );
	}

	@Override
	public <T> JpaCriteriaQuery<T> intersectAll(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return criteriaBuilder.intersectAll( query1, queries );
	}

	@Override
	public <T> JpaCriteriaQuery<T> intersect(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return criteriaBuilder.intersect( query1, queries );
	}

	@Override
	public <T> JpaCriteriaQuery<T> intersect(
			boolean all,
			CriteriaQuery<? extends T> query1,
			CriteriaQuery<?>... queries) {
		return criteriaBuilder.intersect( all, query1, queries );
	}

	@Override
	public <T> JpaCriteriaQuery<T> exceptAll(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return criteriaBuilder.exceptAll( query1, queries );
	}

	@Override
	public <T> JpaCriteriaQuery<T> except(CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return criteriaBuilder.except( query1, queries );
	}

	@Override
	public <T> JpaCriteriaQuery<T> except(boolean all, CriteriaQuery<? extends T> query1, CriteriaQuery<?>... queries) {
		return criteriaBuilder.except( all, query1, queries );
	}

	@Override
	public <T> CriteriaSelect<T> union(CriteriaSelect<? extends T> left, CriteriaSelect<? extends T> right) {
		return criteriaBuilder.union( left, right );
	}

	@Override
	public <T> JpaSubQuery<T> unionAll(JpaSubQuery<? extends T> query1, JpaSubQuery<? extends T> query2) {
		return criteriaBuilder.unionAll( query1, query2 );
	}

	@Override
	public <T> CriteriaSelect<T> unionAll(CriteriaSelect<? extends T> left, CriteriaSelect<? extends T> right) {
		return criteriaBuilder.unionAll( left, right );
	}

	@Override
	public <T> JpaSubQuery<T> union(Subquery<? extends T> query1, Subquery<?>... queries) {
		return criteriaBuilder.union( query1, queries );
	}

	@Override
	public <T> JpaSubQuery<T> union(boolean all, Subquery<? extends T> query1, Subquery<?>... queries) {
		return criteriaBuilder.union( all, query1, queries );
	}

	@Override
	public <T> JpaSubQuery<T> intersectAll(Subquery<? extends T> query1, Subquery<?>... queries) {
		return criteriaBuilder.intersectAll( query1, queries );
	}

	@Override
	public <T> JpaSubQuery<T> intersect(Subquery<? extends T> query1, Subquery<?>... queries) {
		return criteriaBuilder.intersect( query1, queries );
	}

	@Override
	public <T> JpaSubQuery<T> intersect(boolean all, Subquery<? extends T> query1, Subquery<?>... queries) {
		return criteriaBuilder.intersect( all, query1, queries );
	}

	@Override
	public <T> CriteriaSelect<T> except(CriteriaSelect<T> left, CriteriaSelect<?> right) {
		return criteriaBuilder.except( left, right );
	}

	@Override
	public <T> CriteriaSelect<T> exceptAll(CriteriaSelect<T> left, CriteriaSelect<?> right) {
		return criteriaBuilder.exceptAll( left, right );
	}

	@Override
	public <T> JpaSubQuery<T> exceptAll(Subquery<? extends T> query1, Subquery<?>... queries) {
		return criteriaBuilder.exceptAll( query1, queries );
	}

	@Override
	public <T> JpaSubQuery<T> except(Subquery<? extends T> query1, Subquery<?>... queries) {
		return criteriaBuilder.except( query1, queries );
	}

	@Override
	public <T> JpaSubQuery<T> except(boolean all, Subquery<? extends T> query1, Subquery<?>... queries) {
		return criteriaBuilder.except( all, query1, queries );
	}



	@Override
	public JpaExpression<Integer> sign(Expression<? extends Number> x) {
		return criteriaBuilder.sign( x );
	}

	@Override
	public <N extends Number> JpaExpression<N> ceiling(Expression<N> x) {
		return criteriaBuilder.ceiling( x );
	}

	@Override
	public <N extends Number> JpaExpression<N> floor(Expression<N> x) {
		return criteriaBuilder.floor( x );
	}

	@Override
	public JpaExpression<Double> exp(Expression<? extends Number> x) {
		return criteriaBuilder.exp( x );
	}

	@Override
	public JpaExpression<Double> ln(Expression<? extends Number> x) {
		return criteriaBuilder.ln( x );
	}

	@Override
	public JpaExpression<Double> power(Expression<? extends Number> x, Expression<? extends Number> y) {
		return criteriaBuilder.power( x, y );
	}

	@Override
	public JpaExpression<Double> power(Expression<? extends Number> x, Number y) {
		return criteriaBuilder.power( x, y );
	}

	@Override
	public <T extends Number> JpaExpression<T> round(Expression<T> x, Integer n) {
		return criteriaBuilder.round( x, n );
	}

	@Override
	public <T extends Number> JpaExpression<T> truncate(Expression<T> x, Integer n) {
		return criteriaBuilder.truncate( x, n );
	}

	@Override
	public JpaExpression<LocalDate> localDate() {
		return criteriaBuilder.localDate();
	}

	@Override
	public JpaExpression<LocalDateTime> localDateTime() {
		return criteriaBuilder.localDateTime();
	}

	@Override
	public JpaExpression<LocalTime> localTime() {
		return criteriaBuilder.localTime();
	}

	@Override
	public <N, T extends Temporal> JpaExpression<N> extract(TemporalField<N, T> field, Expression<T> temporal) {
		return null;
	}

	@Override
	public <P, F> JpaExpression<F> fk(Path<P> path) {
		return criteriaBuilder.fk( path );
	}

	@Override
	public <X, T extends X> JpaPath<T> treat(Path<X> path, Class<T> type) {
		return criteriaBuilder.treat( path, type );
	}

	@Override
	public <X, T extends X> JpaRoot<T> treat(Root<X> root, Class<T> type) {
		return criteriaBuilder.treat( root, type );
	}

	@Override
	public <T> JpaCriteriaQuery<T> union(CriteriaQuery<? extends T> left, CriteriaQuery<? extends T> right) {
		return criteriaBuilder.union( left, right );
	}

	@Override
	public <T> JpaCriteriaQuery<T> unionAll(CriteriaQuery<? extends T> left, CriteriaQuery<? extends T> right) {
		return criteriaBuilder.unionAll( left, right );
	}

	@Override
	public <T> CriteriaSelect<T> intersect(CriteriaSelect<? super T> left, CriteriaSelect<? super T> right) {
		return criteriaBuilder.intersect( left, right );
	}

	@Override
	public <T> CriteriaSelect<T> intersectAll(CriteriaSelect<? super T> left, CriteriaSelect<? super T> right) {
		return criteriaBuilder.intersectAll( left, right );
	}

	@Override
	public <T> JpaCriteriaQuery<T> intersect(CriteriaQuery<? super T> left, CriteriaQuery<? super T> right) {
		return criteriaBuilder.intersect( left, right );
	}

	@Override
	public <T> JpaCriteriaQuery<T> intersectAll(CriteriaQuery<? super T> left, CriteriaQuery<? super T> right) {
		return criteriaBuilder.intersectAll( left, right );
	}

	@Override
	public <T> JpaCriteriaQuery<T> except(CriteriaQuery<T> left, CriteriaQuery<?> right) {
		return criteriaBuilder.except( left, right );
	}

	@Override
	public <T> JpaCriteriaQuery<T> exceptAll(CriteriaQuery<T> left, CriteriaQuery<?> right) {
		return criteriaBuilder.exceptAll( left, right );
	}

	@Override
	public <X, T, V extends T> JpaJoin<X, V> treat(Join<X, T> join, Class<V> type) {
		return criteriaBuilder.treat( join, type );
	}

	@Override
	public <X, T, E extends T> JpaCollectionJoin<X, E> treat(CollectionJoin<X, T> join, Class<E> type) {
		return criteriaBuilder.treat( join, type );
	}

	@Override
	public <X, T, E extends T> JpaSetJoin<X, E> treat(SetJoin<X, T> join, Class<E> type) {
		return criteriaBuilder.treat( join, type );
	}

	@Override
	public <X, T, E extends T> JpaListJoin<X, E> treat(ListJoin<X, T> join, Class<E> type) {
		return criteriaBuilder.treat( join, type );
	}

	@Override
	public <X, K, T, V extends T> JpaMapJoin<X, K, V> treat(MapJoin<X, K, T> join, Class<V> type) {
		return criteriaBuilder.treat( join, type );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, Selection<?>... selections) {
		return criteriaBuilder.construct( resultClass, selections );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, List<? extends JpaSelection<?>> arguments) {
		return criteriaBuilder.construct( resultClass, arguments );
	}

	@Override
	public JpaCompoundSelection<Tuple> tuple(Selection<?>... selections) {
		return criteriaBuilder.tuple( selections );
	}

	@Override
	public JpaCompoundSelection<Tuple> tuple(List<Selection<?>> selections) {
		return criteriaBuilder.tuple( selections );
	}

	@Override
	public JpaCompoundSelection<Object[]> array(Selection<?>... selections) {
		return criteriaBuilder.array( selections );
	}

	@Override
	public JpaCompoundSelection<Object[]> array(List<Selection<?>> selections) {
		return criteriaBuilder.array( selections );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> array(Class<Y> resultClass, Selection<?>... selections) {
		return criteriaBuilder.array( resultClass, selections );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> array(Class<Y> resultClass, List<? extends JpaSelection<?>> selections) {
		return criteriaBuilder.array( resultClass, selections );
	}

	@Override
	public <N extends Number> JpaExpression<Double> avg(Expression<N> argument) {
		return criteriaBuilder.avg( argument );
	}

	@Override
	public <N extends Number> JpaExpression<N> sum(Expression<N> argument) {
		return criteriaBuilder.sum( argument );
	}

	@Override
	public JpaExpression<Long> sumAsLong(Expression<Integer> argument) {
		return criteriaBuilder.sumAsLong( argument );
	}

	@Override
	public JpaExpression<Double> sumAsDouble(Expression<Float> argument) {
		return criteriaBuilder.sumAsDouble( argument );
	}

	@Override
	public <N extends Number> JpaExpression<N> max(Expression<N> argument) {
		return criteriaBuilder.max( argument );
	}

	@Override
	public <N extends Number> JpaExpression<N> min(Expression<N> argument) {
		return criteriaBuilder.min( argument );
	}

	@Override
	public <X extends Comparable<? super X>> JpaExpression<X> greatest(Expression<X> argument) {
		return criteriaBuilder.greatest( argument );
	}

	@Override
	public <X extends Comparable<? super X>> JpaExpression<X> least(Expression<X> argument) {
		return criteriaBuilder.least( argument );
	}

	@Override
	public JpaExpression<Long> count(Expression<?> argument) {
		return criteriaBuilder.count( argument );
	}

	@Override
	public JpaExpression<Long> count() {
		return criteriaBuilder.count();
	}

	@Override
	public JpaExpression<Long> countDistinct(Expression<?> x) {
		return criteriaBuilder.countDistinct( x );
	}

	@Override
	public <N extends Number> JpaExpression<N> neg(Expression<N> x) {
		return criteriaBuilder.neg( x );
	}

	@Override
	public <N extends Number> JpaExpression<N> abs(Expression<N> x) {
		return criteriaBuilder.abs( x );
	}

	@Override
	public <N extends Number> JpaExpression<N> sum(Expression<? extends N> x, Expression<? extends N> y) {
		return criteriaBuilder.sum( x, y );
	}

	@Override
	public <N extends Number> JpaExpression<N> sum(Expression<? extends N> x, N y) {
		return criteriaBuilder.sum( x, y );
	}

	@Override
	public <N extends Number> JpaExpression<N> sum(N x, Expression<? extends N> y) {
		return criteriaBuilder.sum( x, y );
	}

	@Override
	public <N extends Number> JpaExpression<N> prod(Expression<? extends N> x, Expression<? extends N> y) {
		return criteriaBuilder.prod( x, y );
	}

	@Override
	public <N extends Number> JpaExpression<N> prod(Expression<? extends N> x, N y) {
		return criteriaBuilder.prod( x, y );
	}

	@Override
	public <N extends Number> JpaExpression<N> prod(N x, Expression<? extends N> y) {
		return criteriaBuilder.prod( x, y );
	}

	@Override
	public <N extends Number> JpaExpression<N> diff(Expression<? extends N> x, Expression<? extends N> y) {
		return criteriaBuilder.diff( x, y );
	}

	@Override
	public <N extends Number> JpaExpression<N> diff(Expression<? extends N> x, N y) {
		return criteriaBuilder.diff( x, y );
	}

	@Override
	public <N extends Number> JpaExpression<N> diff(N x, Expression<? extends N> y) {
		return criteriaBuilder.diff( x, y );
	}

	@Override
	public JpaExpression<Number> quot(Expression<? extends Number> x, Expression<? extends Number> y) {
		return criteriaBuilder.quot( x, y );
	}

	@Override
	public JpaExpression<Number> quot(Expression<? extends Number> x, Number y) {
		return criteriaBuilder.quot( x, y );
	}

	@Override
	public JpaExpression<Number> quot(Number x, Expression<? extends Number> y) {
		return criteriaBuilder.quot( x, y );
	}

	@Override
	public JpaExpression<Integer> mod(Expression<Integer> x, Expression<Integer> y) {
		return criteriaBuilder.mod( x, y );
	}

	@Override
	public JpaExpression<Integer> mod(Expression<Integer> x, Integer y) {
		return criteriaBuilder.mod( x, y );
	}

	@Override
	public JpaExpression<Integer> mod(Integer x, Expression<Integer> y) {
		return criteriaBuilder.mod( x, y );
	}

	@Override
	public JpaExpression<Double> sqrt(Expression<? extends Number> x) {
		return criteriaBuilder.sqrt( x );
	}

	@Override
	public JpaExpression<Long> toLong(Expression<? extends Number> number) {
		return criteriaBuilder.toLong( number );
	}

	@Override
	public JpaExpression<Integer> toInteger(Expression<? extends Number> number) {
		return criteriaBuilder.toInteger( number );
	}

	@Override
	public JpaExpression<Float> toFloat(Expression<? extends Number> number) {
		return criteriaBuilder.toFloat( number );
	}

	@Override
	public JpaExpression<Double> toDouble(Expression<? extends Number> number) {
		return criteriaBuilder.toDouble( number );
	}

	@Override
	public JpaExpression<BigDecimal> toBigDecimal(Expression<? extends Number> number) {
		return criteriaBuilder.toBigDecimal( number );
	}

	@Override
	public JpaExpression<BigInteger> toBigInteger(Expression<? extends Number> number) {
		return criteriaBuilder.toBigInteger( number );
	}

	@Override
	public JpaExpression<String> toString(Expression<Character> character) {
		return criteriaBuilder.toString( character );
	}

	@Override
	public <T> JpaExpression<T> literal(T value) {
		return criteriaBuilder.literal( value );
	}

	@Override @SafeVarargs
	public final <T> List<? extends JpaExpression<T>> literals(T... values) {
		return criteriaBuilder.literals( values );
	}

	@Override
	public <T> List<? extends JpaExpression<T>> literals(List<T> values) {
		return criteriaBuilder.literals( values );
	}

	@Override
	public <T> JpaExpression<T> nullLiteral(Class<T> resultClass) {
		return criteriaBuilder.nullLiteral( resultClass );
	}

	@Override
	public <T> JpaParameterExpression<T> parameter(Class<T> paramClass) {
		return criteriaBuilder.parameter( paramClass );
	}

	@Override
	public <T> JpaParameterExpression<T> parameter(Class<T> paramClass, String name) {
		return criteriaBuilder.parameter( paramClass, name );
	}

	@Override
	public JpaExpression<String> concat(Expression<String> x, Expression<String> y) {
		return criteriaBuilder.concat( x, y );
	}

	@Override
	public <T> JpaParameterExpression<List<T>> parameterList(Class<T> paramClass) {
		return criteriaBuilder.parameterList( paramClass );
	}

	@Override
	public <T> JpaParameterExpression<List<T>> parameterList(Class<T> paramClass, String name) {
		return criteriaBuilder.parameterList( paramClass, name );
	}

	@Override
	public JpaExpression<String> concat(Expression<String> x, String y) {
		return criteriaBuilder.concat( x, y );
	}

	@Override
	public JpaExpression<String> concat(String x, Expression<String> y) {
		return criteriaBuilder.concat( x, y );
	}

	@Override
	public JpaExpression<String> concat(String x, String y) {
		return criteriaBuilder.concat( x, y );
	}

	@Override
	public JpaFunction<String> substring(Expression<String> x, Expression<Integer> from) {
		return criteriaBuilder.substring( x, from );
	}

	@Override
	public JpaFunction<String> substring(Expression<String> x, int from) {
		return criteriaBuilder.substring( x, from );
	}

	@Override
	public JpaFunction<String> substring(Expression<String> x, Expression<Integer> from, Expression<Integer> len) {
		return criteriaBuilder.substring( x, from, len );
	}

	@Override
	public JpaFunction<String> substring(Expression<String> x, int from, int len) {
		return criteriaBuilder.substring( x, from, len );
	}

	@Override
	public JpaFunction<String> trim(Expression<String> x) {
		return criteriaBuilder.trim( x );
	}

	@Override
	public JpaFunction<String> trim(Trimspec ts, Expression<String> x) {
		return criteriaBuilder.trim( ts, x );
	}

	@Override
	public JpaFunction<String> trim(Expression<Character> t, Expression<String> x) {
		return criteriaBuilder.trim( t, x );
	}

	@Override
	public JpaFunction<String> trim(Trimspec ts, Expression<Character> t, Expression<String> x) {
		return criteriaBuilder.trim( ts, t, x );
	}

	@Override
	public JpaFunction<String> trim(char t, Expression<String> x) {
		return criteriaBuilder.trim( t, x );
	}

	@Override
	public JpaFunction<String> trim(Trimspec ts, char t, Expression<String> x) {
		return criteriaBuilder.trim( ts, t, x );
	}

	@Override
	public JpaFunction<String> lower(Expression<String> x) {
		return criteriaBuilder.lower( x );
	}

	@Override
	public JpaFunction<String> upper(Expression<String> x) {
		return criteriaBuilder.upper( x );
	}

	@Override
	public JpaFunction<Integer> length(Expression<String> x) {
		return criteriaBuilder.length( x );
	}

	@Override
	public JpaFunction<Integer> locate(Expression<String> x, Expression<String> pattern) {
		return criteriaBuilder.locate( x, pattern );
	}

	@Override
	public JpaFunction<Integer> locate(Expression<String> x, String pattern) {
		return criteriaBuilder.locate( x, pattern );
	}

	@Override
	public JpaFunction<Integer> locate(Expression<String> x, Expression<String> pattern, Expression<Integer> from) {
		return criteriaBuilder.locate( x, pattern, from );
	}

	@Override
	public JpaFunction<Integer> locate(Expression<String> x, String pattern, int from) {
		return criteriaBuilder.locate( x, pattern, from );
	}

	@Override
	public JpaFunction<Date> currentDate() {
		return criteriaBuilder.currentDate();
	}

	@Override
	public JpaFunction<Time> currentTime() {
		return criteriaBuilder.currentTime();
	}

	@Override
	public JpaFunction<Timestamp> currentTimestamp() {
		return criteriaBuilder.currentTimestamp();
	}

	@Override
	public JpaFunction<Instant> currentInstant() {
		return criteriaBuilder.currentInstant();
	}

	@Override
	public <T> JpaFunction<T> function(String name, Class<T> type, Expression<?>... args) {
		return criteriaBuilder.function( name, type, args );
	}

	@Override
	public <Y> JpaExpression<Y> all(Subquery<Y> subquery) {
		return criteriaBuilder.all( subquery );
	}

	@Override
	public <Y> JpaExpression<Y> some(Subquery<Y> subquery) {
		return criteriaBuilder.some( subquery );
	}

	@Override
	public <Y> JpaExpression<Y> any(Subquery<Y> subquery) {
		return criteriaBuilder.any( subquery );
	}

	@Override
	public <K, M extends Map<K, ?>> JpaExpression<Set<K>> keys(M map) {
		return criteriaBuilder.keys( map );
	}

	@Override
	public <K, L extends List<?>> JpaExpression<Set<K>> indexes(L list) {
		return criteriaBuilder.indexes( list );
	}

	@Override
	public <T> JpaExpression<T> value(T value) {
		return criteriaBuilder.value( value );
	}

	@Override
	public <V, M extends Map<?, V>> Expression<Collection<V>> values(M map) {
		return criteriaBuilder.values( map );
	}

	@Override
	public <C extends Collection<?>> JpaExpression<Integer> size(Expression<C> collection) {
		return criteriaBuilder.size( collection );
	}

	@Override
	public <C extends Collection<?>> JpaExpression<Integer> size(C collection) {
		return criteriaBuilder.size( collection );
	}

	@Override
	public <T> JpaCoalesce<T> coalesce() {
		return criteriaBuilder.coalesce();
	}

	@Override
	public <Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Expression<? extends Y> y) {
		return criteriaBuilder.coalesce( x, y );
	}

	@Override
	public <Y> JpaCoalesce<Y> coalesce(Expression<? extends Y> x, Y y) {
		return criteriaBuilder.coalesce( x, y );
	}

	@Override
	public <Y> JpaExpression<Y> nullif(Expression<Y> x, Expression<?> y) {
		return criteriaBuilder.nullif( x, y );
	}

	@Override
	public <Y> JpaExpression<Y> nullif(Expression<Y> x, Y y) {
		return criteriaBuilder.nullif( x, y );
	}

	@Override
	public <C, R> JpaSimpleCase<C, R> selectCase(Expression<? extends C> expression) {
		return criteriaBuilder.selectCase( expression );
	}

	@Override
	public <R> JpaSearchedCase<R> selectCase() {
		return criteriaBuilder.selectCase();
	}

	@Override
	public JpaPredicate and(Expression<Boolean> x, Expression<Boolean> y) {
		return criteriaBuilder.and( x, y );
	}

	@Override
	public JpaPredicate and(Predicate... restrictions) {
		return criteriaBuilder.and( restrictions );
	}

	@Override
	public JpaPredicate and(List<Predicate> restrictions) {
		return criteriaBuilder.and( restrictions );
	}

	@Override
	public JpaPredicate or(Expression<Boolean> x, Expression<Boolean> y) {
		return criteriaBuilder.or( x, y );
	}

	@Override
	public JpaPredicate or(Predicate... restrictions) {
		return criteriaBuilder.or( restrictions );
	}

	@Override
	public JpaPredicate or(List<Predicate> restrictions) {
		return criteriaBuilder.or( restrictions );
	}

	@Override
	public JpaPredicate not(Expression<Boolean> restriction) {
		return criteriaBuilder.not( restriction );
	}

	@Override
	public JpaPredicate conjunction() {
		return criteriaBuilder.conjunction();
	}

	@Override
	public JpaPredicate disjunction() {
		return criteriaBuilder.disjunction();
	}

	@Override
	public JpaPredicate isTrue(Expression<Boolean> x) {
		return criteriaBuilder.isTrue( x );
	}

	@Override
	public JpaPredicate isFalse(Expression<Boolean> x) {
		return criteriaBuilder.isFalse( x );
	}

	@Override
	public JpaPredicate isNull(Expression<?> x) {
		return criteriaBuilder.isNull( x );
	}

	@Override
	public JpaPredicate isNotNull(Expression<?> x) {
		return criteriaBuilder.isNotNull( x );
	}

	@Override
	public JpaPredicate equal(Expression<?> x, Expression<?> y) {
		return criteriaBuilder.equal( x, y );
	}

	@Override
	public JpaPredicate equal(Expression<?> x, Object y) {
		return criteriaBuilder.equal( x, y );
	}

	@Override
	public JpaPredicate notEqual(Expression<?> x, Expression<?> y) {
		return criteriaBuilder.notEqual( x, y );
	}

	@Override
	public JpaPredicate notEqual(Expression<?> x, Object y) {
		return criteriaBuilder.notEqual( x, y );
	}

	@Override
	public JpaPredicate distinctFrom(Expression<?> x, Expression<?> y) {
		return criteriaBuilder.distinctFrom( x, y );
	}

	@Override
	public JpaPredicate distinctFrom(Expression<?> x, Object y) {
		return criteriaBuilder.distinctFrom( x, y );
	}

	@Override
	public JpaPredicate notDistinctFrom(Expression<?> x, Expression<?> y) {
		return criteriaBuilder.notDistinctFrom( x, y );
	}

	@Override
	public JpaPredicate notDistinctFrom(Expression<?> x, Object y) {
		return criteriaBuilder.notDistinctFrom( x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate greaterThan(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return criteriaBuilder.greaterThan( x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate greaterThan(Expression<? extends Y> x, Y y) {
		return criteriaBuilder.greaterThan( x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate greaterThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return criteriaBuilder.greaterThanOrEqualTo( x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate greaterThanOrEqualTo(Expression<? extends Y> x, Y y) {
		return criteriaBuilder.greaterThanOrEqualTo( x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate lessThan(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return criteriaBuilder.lessThan( x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate lessThan(Expression<? extends Y> x, Y y) {
		return criteriaBuilder.lessThan( x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate lessThanOrEqualTo(
			Expression<? extends Y> x,
			Expression<? extends Y> y) {
		return criteriaBuilder.lessThanOrEqualTo( x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate lessThanOrEqualTo(Expression<? extends Y> x, Y y) {
		return criteriaBuilder.lessThanOrEqualTo( x, y );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate between(
			Expression<? extends Y> value,
			Expression<? extends Y> lower,
			Expression<? extends Y> upper) {
		return criteriaBuilder.between( value, lower, upper );
	}

	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate between(Expression<? extends Y> value, Y lower, Y upper) {
		return criteriaBuilder.between( value, lower, upper );
	}

	@Override
	public JpaPredicate gt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return criteriaBuilder.gt( x, y );
	}

	@Override
	public JpaPredicate gt(Expression<? extends Number> x, Number y) {
		return criteriaBuilder.gt( x, y );
	}

	@Override
	public JpaPredicate ge(Expression<? extends Number> x, Expression<? extends Number> y) {
		return criteriaBuilder.ge( x, y );
	}

	@Override
	public JpaPredicate ge(Expression<? extends Number> x, Number y) {
		return criteriaBuilder.ge( x, y );
	}

	@Override
	public JpaPredicate lt(Expression<? extends Number> x, Expression<? extends Number> y) {
		return criteriaBuilder.lt( x, y );
	}

	@Override
	public JpaPredicate lt(Expression<? extends Number> x, Number y) {
		return criteriaBuilder.lt( x, y );
	}

	@Override
	public JpaPredicate le(Expression<? extends Number> x, Expression<? extends Number> y) {
		return criteriaBuilder.le( x, y );
	}

	@Override
	public JpaPredicate le(Expression<? extends Number> x, Number y) {
		return criteriaBuilder.le( x, y );
	}

	@Override
	public <C extends Collection<?>> JpaPredicate isEmpty(Expression<C> collection) {
		return criteriaBuilder.isEmpty( collection );
	}

	@Override
	public <C extends Collection<?>> JpaPredicate isNotEmpty(Expression<C> collection) {
		return criteriaBuilder.isNotEmpty( collection );
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicate isMember(Expression<E> elem, Expression<C> collection) {
		return criteriaBuilder.isMember( elem, collection );
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicate isMember(E elem, Expression<C> collection) {
		return criteriaBuilder.isMember( elem, collection );
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicate isNotMember(Expression<E> elem, Expression<C> collection) {
		return criteriaBuilder.isNotMember( elem, collection );
	}

	@Override
	public <E, C extends Collection<E>> JpaPredicate isNotMember(E elem, Expression<C> collection) {
		return criteriaBuilder.isNotMember( elem, collection );
	}

	@Override
	public JpaPredicate like(Expression<String> x, Expression<String> pattern) {
		return criteriaBuilder.like( x, pattern );
	}

	@Override
	public JpaPredicate like(Expression<String> x, String pattern) {
		return criteriaBuilder.like( x, pattern );
	}

	@Override
	public JpaPredicate like(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar) {
		return criteriaBuilder.like( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate like(Expression<String> x, Expression<String> pattern, char escapeChar) {
		return criteriaBuilder.like( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate like(Expression<String> x, String pattern, Expression<Character> escapeChar) {
		return criteriaBuilder.like( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate like(Expression<String> x, String pattern, char escapeChar) {
		return criteriaBuilder.like( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate ilike(Expression<String> x, Expression<String> pattern) {
		return criteriaBuilder.ilike( x, pattern );
	}

	@Override
	public JpaPredicate ilike(Expression<String> x, String pattern) {
		return criteriaBuilder.ilike( x, pattern );
	}

	@Override
	public JpaPredicate ilike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar) {
		return criteriaBuilder.ilike( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate ilike(Expression<String> x, Expression<String> pattern, char escapeChar) {
		return criteriaBuilder.ilike( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate ilike(Expression<String> x, String pattern, Expression<Character> escapeChar) {
		return criteriaBuilder.ilike( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate ilike(Expression<String> x, String pattern, char escapeChar) {
		return criteriaBuilder.ilike( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate notLike(Expression<String> x, Expression<String> pattern) {
		return criteriaBuilder.notLike( x, pattern );
	}

	@Override
	public JpaPredicate notLike(Expression<String> x, String pattern) {
		return criteriaBuilder.notLike( x, pattern );
	}

	@Override
	public JpaPredicate notLike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar) {
		return criteriaBuilder.notLike( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate notLike(Expression<String> x, Expression<String> pattern, char escapeChar) {
		return criteriaBuilder.notLike( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate notLike(Expression<String> x, String pattern, Expression<Character> escapeChar) {
		return criteriaBuilder.notLike( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate notLike(Expression<String> x, String pattern, char escapeChar) {
		return criteriaBuilder.notLike( x, pattern, escapeChar );
	}

	@Override
	public JpaExpression<String> concat(List<Expression<String>> expressions) {
		return criteriaBuilder.concat( expressions );
	}

	@Override
	public JpaPredicate notIlike(Expression<String> x, Expression<String> pattern) {
		return criteriaBuilder.notIlike( x, pattern );
	}

	@Override
	public JpaPredicate notIlike(Expression<String> x, String pattern) {
		return criteriaBuilder.notIlike( x, pattern );
	}

	@Override
	public JpaPredicate notIlike(Expression<String> x, Expression<String> pattern, Expression<Character> escapeChar) {
		return criteriaBuilder.notIlike( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate notIlike(Expression<String> x, Expression<String> pattern, char escapeChar) {
		return criteriaBuilder.notIlike( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate notIlike(Expression<String> x, String pattern, Expression<Character> escapeChar) {
		return criteriaBuilder.notIlike( x, pattern, escapeChar );
	}

	@Override
	public JpaPredicate notIlike(Expression<String> x, String pattern, char escapeChar) {
		return criteriaBuilder.notIlike( x, pattern, escapeChar );
	}

	@Override
	public <T> JpaInPredicate<T> in(Expression<? extends T> expression) {
		return criteriaBuilder.in( expression );
	}

	@Override @SafeVarargs
	public final <T> JpaInPredicate<T> in(Expression<? extends T> expression, Expression<? extends T>... values) {
		return criteriaBuilder.in( expression, values );
	}

	@Override @SafeVarargs
	public final <T> JpaInPredicate<T> in(Expression<? extends T> expression, T... values) {
		return criteriaBuilder.in( expression, values );
	}

	@Override
	public <T> JpaInPredicate<T> in(Expression<? extends T> expression, Collection<T> values) {
		return criteriaBuilder.in( expression, values );
	}

	@Override
	public JpaPredicate exists(Subquery<?> subquery) {
		return criteriaBuilder.exists( subquery );
	}

	@Override
	public <M extends Map<?, ?>> JpaPredicate isMapEmpty(JpaExpression<M> mapExpression) {
		return criteriaBuilder.isMapEmpty( mapExpression );
	}

	@Override
	public <M extends Map<?, ?>> JpaPredicate isMapNotEmpty(JpaExpression<M> mapExpression) {
		return criteriaBuilder.isMapNotEmpty( mapExpression );
	}

	@Override
	public <M extends Map<?, ?>> JpaExpression<Integer> mapSize(JpaExpression<M> mapExpression) {
		return criteriaBuilder.mapSize( mapExpression );
	}

	@Override
	public <M extends Map<?, ?>> JpaExpression<Integer> mapSize(M map) {
		return criteriaBuilder.mapSize( map );
	}

	@Override
	public JpaOrder sort(JpaExpression<?> sortExpression, SortDirection sortOrder, NullPrecedence nullPrecedence) {
		return criteriaBuilder.sort( sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public JpaOrder sort(JpaExpression<?> sortExpression, SortDirection sortOrder, NullPrecedence nullPrecedence, boolean ignoreCase) {
		return criteriaBuilder.sort( sortExpression, sortOrder, nullPrecedence, ignoreCase );
	}

	@Override
	public JpaOrder sort(JpaExpression<?> sortExpression, SortDirection sortOrder) {
		return criteriaBuilder.sort( sortExpression, sortOrder );
	}

	@Override
	public JpaOrder sort(JpaExpression<?> sortExpression, SortDirection sortOrder, Nulls nullPrecedence) {
		return criteriaBuilder.sort( sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public JpaOrder sort(
			JpaExpression<?> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence,
			boolean ignoreCase) {
		return criteriaBuilder.sort( sortExpression, sortOrder, nullPrecedence, ignoreCase );
	}

	@Override
	public JpaOrder sort(JpaExpression<?> sortExpression) {
		return criteriaBuilder.sort( sortExpression );
	}

	@Override
	public JpaOrder asc(Expression<?> x) {
		return criteriaBuilder.asc( x );
	}

	@Override
	public JpaOrder desc(Expression<?> x) {
		return criteriaBuilder.desc( x );
	}

	@Override
	public Order asc(Expression<?> expression, Nulls nullPrecedence) {
		return criteriaBuilder.asc( expression, nullPrecedence );
	}

	@Override
	public Order desc(Expression<?> expression, Nulls nullPrecedence) {
		return criteriaBuilder.desc( expression, nullPrecedence );
	}

	@Override
	public JpaOrder asc(Expression<?> x, boolean nullsFirst) {
		return criteriaBuilder.asc( x, nullsFirst );
	}

	@Override
	public JpaOrder desc(Expression<?> x, boolean nullsFirst) {
		return criteriaBuilder.desc( x, nullsFirst );
	}

	@Override
	@Incubating
	public JpaSearchOrder search(
			JpaCteCriteriaAttribute cteAttribute,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.search( cteAttribute, sortOrder, nullPrecedence );
	}

	@Override
	@Incubating
	public JpaSearchOrder search(JpaCteCriteriaAttribute cteAttribute, SortDirection sortOrder) {
		return criteriaBuilder.search( cteAttribute, sortOrder );
	}

	@Override
	@Incubating
	public JpaSearchOrder search(JpaCteCriteriaAttribute cteAttribute) {
		return criteriaBuilder.search( cteAttribute );
	}

	@Override
	@Incubating
	public JpaSearchOrder asc(JpaCteCriteriaAttribute x) {
		return criteriaBuilder.asc( x );
	}

	@Override
	@Incubating
	public JpaSearchOrder desc(JpaCteCriteriaAttribute x) {
		return criteriaBuilder.desc( x );
	}

	@Override
	@Incubating
	public JpaSearchOrder asc(JpaCteCriteriaAttribute x, boolean nullsFirst) {
		return criteriaBuilder.asc( x, nullsFirst );
	}

	@Override
	@Incubating
	public JpaSearchOrder desc(JpaCteCriteriaAttribute x, boolean nullsFirst) {
		return criteriaBuilder.desc( x, nullsFirst );
	}

	@Override
	public <T> JpaExpression<T> sql(String pattern, Class<T> type, Expression<?>... arguments) {
		return criteriaBuilder.sql( pattern, type, arguments );
	}

	@Override
	public JpaFunction<String> format(Expression<? extends TemporalAccessor> datetime, String pattern) {
		return criteriaBuilder.format( datetime, pattern );
	}

	@Override
	public JpaFunction<Integer> year(Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.year( datetime );
	}

	@Override
	public JpaFunction<Integer> month(Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.month( datetime );
	}

	@Override
	public JpaFunction<Integer> day(Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.day( datetime );
	}

	@Override
	public JpaFunction<Integer> hour(Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.hour( datetime );
	}

	@Override
	public JpaFunction<Integer> minute(Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.minute( datetime );
	}

	@Override
	public JpaFunction<Float> second(Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.second( datetime );
	}

	@Override
	public <T extends TemporalAccessor> JpaFunction<T> truncate(Expression<T> datetime, TemporalUnit temporalUnit) {
		return criteriaBuilder.truncate( datetime, temporalUnit );
	}

	@Override
	public JpaFunction<String> overlay(Expression<String> string, String replacement, int start) {
		return criteriaBuilder.overlay( string, replacement, start );
	}

	@Override
	public JpaFunction<String> overlay(Expression<String> string, Expression<String> replacement, int start) {
		return criteriaBuilder.overlay( string, replacement, start );
	}

	@Override
	public JpaFunction<String> overlay(Expression<String> string, String replacement, Expression<Integer> start) {
		return criteriaBuilder.overlay( string, replacement, start );
	}

	@Override
	public JpaFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			Expression<Integer> start) {
		return criteriaBuilder.overlay( string, replacement, start );
	}

	@Override
	public JpaFunction<String> overlay(Expression<String> string, String replacement, int start, int length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Override
	public JpaFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			int start,
			int length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Override
	public JpaFunction<String> overlay(
			Expression<String> string,
			String replacement,
			Expression<Integer> start,
			int length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Override
	public JpaFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			Expression<Integer> start,
			int length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Override
	public JpaFunction<String> overlay(
			Expression<String> string,
			String replacement,
			int start,
			Expression<Integer> length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Override
	public JpaFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			int start,
			Expression<Integer> length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Override
	public JpaFunction<String> overlay(
			Expression<String> string,
			String replacement,
			Expression<Integer> start,
			Expression<Integer> length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Override
	public JpaFunction<String> overlay(
			Expression<String> string,
			Expression<String> replacement,
			Expression<Integer> start,
			Expression<Integer> length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Override
	public JpaFunction<String> pad(Expression<String> x, int length) {
		return criteriaBuilder.pad( x, length );
	}

	@Override
	public JpaFunction<String> pad(Trimspec ts, Expression<String> x, int length) {
		return criteriaBuilder.pad( ts, x, length );
	}

	@Override
	public JpaFunction<String> pad(Expression<String> x, Expression<Integer> length) {
		return criteriaBuilder.pad( x, length );
	}

	@Override
	public JpaFunction<String> pad(Trimspec ts, Expression<String> x, Expression<Integer> length) {
		return criteriaBuilder.pad( ts, x, length );
	}

	@Override
	public JpaFunction<String> pad(Expression<String> x, int length, char padChar) {
		return criteriaBuilder.pad( x, length, padChar );
	}

	@Override
	public JpaFunction<String> pad(Trimspec ts, Expression<String> x, int length, char padChar) {
		return criteriaBuilder.pad( ts, x, length, padChar );
	}

	@Override
	public JpaFunction<String> pad(Expression<String> x, Expression<Integer> length, char padChar) {
		return criteriaBuilder.pad( x, length, padChar );
	}

	@Override
	public JpaFunction<String> pad(Trimspec ts, Expression<String> x, Expression<Integer> length, char padChar) {
		return criteriaBuilder.pad( ts, x, length, padChar );
	}

	@Override
	public JpaFunction<String> pad(Expression<String> x, int length, Expression<Character> padChar) {
		return criteriaBuilder.pad( x, length, padChar );
	}

	@Override
	public JpaFunction<String> pad(Trimspec ts, Expression<String> x, int length, Expression<Character> padChar) {
		return criteriaBuilder.pad( ts, x, length, padChar );
	}

	@Override
	public JpaFunction<String> pad(Expression<String> x, Expression<Integer> length, Expression<Character> padChar) {
		return criteriaBuilder.pad( x, length, padChar );
	}

	@Override
	public JpaFunction<String> pad(
			Trimspec ts,
			Expression<String> x,
			Expression<Integer> length,
			Expression<Character> padChar) {
		return criteriaBuilder.pad( ts, x, length, padChar );
	}

	@Override
	public JpaFunction<String> repeat(Expression<String> x, Expression<Integer> times) {
		return criteriaBuilder.repeat( x, times );
	}

	@Override
	public JpaFunction<String> repeat(Expression<String> x, int times) {
		return criteriaBuilder.repeat( x, times );
	}

	@Override
	public JpaFunction<String> repeat(String x, Expression<Integer> times) {
		return criteriaBuilder.repeat( x, times );
	}

	@Override
	public JpaFunction<String> left(Expression<String> x, int length) {
		return criteriaBuilder.left( x, length );
	}

	@Override
	public JpaFunction<String> left(Expression<String> x, Expression<Integer> length) {
		return criteriaBuilder.left( x, length );
	}

	@Override
	public JpaFunction<String> right(Expression<String> x, int length) {
		return criteriaBuilder.right( x, length );
	}

	@Override
	public JpaFunction<String> right(Expression<String> x, Expression<Integer> length) {
		return criteriaBuilder.right( x, length );
	}

	@Override
	public JpaFunction<String> replace(Expression<String> x, String pattern, String replacement) {
		return criteriaBuilder.replace( x, pattern, replacement );
	}

	@Override
	public JpaFunction<String> replace(Expression<String> x, String pattern, Expression<String> replacement) {
		return criteriaBuilder.replace( x, pattern, replacement );
	}

	@Override
	public JpaFunction<String> replace(Expression<String> x, Expression<String> pattern, String replacement) {
		return criteriaBuilder.replace( x, pattern, replacement );
	}

	@Override
	public JpaFunction<String> replace(
			Expression<String> x,
			Expression<String> pattern,
			Expression<String> replacement) {
		return criteriaBuilder.replace( x, pattern, replacement );
	}

	@Override
	public JpaFunction<String> collate(Expression<String> x, String collation) {
		return criteriaBuilder.collate( x, collation );
	}

	@Override
	public JpaExpression<Double> log10(Expression<? extends Number> x) {
		return criteriaBuilder.log10( x );
	}

	@Override
	public JpaExpression<Double> log(Number b, Expression<? extends Number> x) {
		return criteriaBuilder.log( b, x );
	}

	@Override
	public JpaExpression<Double> log(Expression<? extends Number> b, Expression<? extends Number> x) {
		return criteriaBuilder.log( b, x );
	}

	@Override
	public JpaExpression<Double> pi() {
		return criteriaBuilder.pi();
	}

	@Override
	public JpaExpression<Double> sin(Expression<? extends Number> x) {
		return criteriaBuilder.sin( x );
	}

	@Override
	public JpaExpression<Double> cos(Expression<? extends Number> x) {
		return criteriaBuilder.cos( x );
	}

	@Override
	public JpaExpression<Double> tan(Expression<? extends Number> x) {
		return criteriaBuilder.tan( x );
	}

	@Override
	public JpaExpression<Double> asin(Expression<? extends Number> x) {
		return criteriaBuilder.asin( x );
	}

	@Override
	public JpaExpression<Double> acos(Expression<? extends Number> x) {
		return criteriaBuilder.acos( x );
	}

	@Override
	public JpaExpression<Double> atan(Expression<? extends Number> x) {
		return criteriaBuilder.atan( x );
	}

	@Override
	public JpaExpression<Double> atan2(Number y, Expression<? extends Number> x) {
		return criteriaBuilder.atan2( y, x );
	}

	@Override
	public JpaExpression<Double> atan2(Expression<? extends Number> y, Number x) {
		return criteriaBuilder.atan2( y, x );
	}

	@Override
	public JpaExpression<Double> atan2(Expression<? extends Number> y, Expression<? extends Number> x) {
		return criteriaBuilder.atan2( y, x );
	}

	@Override
	public JpaExpression<Double> sinh(Expression<? extends Number> x) {
		return criteriaBuilder.sinh( x );
	}

	@Override
	public JpaExpression<Double> cosh(Expression<? extends Number> x) {
		return criteriaBuilder.cosh( x );
	}

	@Override
	public JpaExpression<Double> tanh(Expression<? extends Number> x) {
		return criteriaBuilder.tanh( x );
	}

	@Override
	public JpaExpression<Double> degrees(Expression<? extends Number> x) {
		return criteriaBuilder.degrees( x );
	}

	@Override
	public JpaExpression<Double> radians(Expression<? extends Number> x) {
		return criteriaBuilder.radians( x );
	}

	@Override
	public JpaWindow createWindow() {
		return criteriaBuilder.createWindow();
	}

	@Override
	public JpaWindowFrame frameUnboundedPreceding() {
		return criteriaBuilder.frameUnboundedPreceding();
	}

	@Override
	public JpaWindowFrame frameBetweenPreceding(int offset) {
		return criteriaBuilder.frameBetweenPreceding( offset );
	}

	@Override
	public JpaWindowFrame frameBetweenPreceding(Expression<?> offset) {
		return criteriaBuilder.frameBetweenPreceding( offset );
	}

	@Override
	public JpaWindowFrame frameCurrentRow() {
		return criteriaBuilder.frameCurrentRow();
	}

	@Override
	public JpaWindowFrame frameBetweenFollowing(int offset) {
		return criteriaBuilder.frameBetweenFollowing( offset );
	}

	@Override
	public JpaWindowFrame frameBetweenFollowing(Expression<?> offset) {
		return criteriaBuilder.frameBetweenFollowing( offset );
	}

	@Override
	public JpaWindowFrame frameUnboundedFollowing() {
		return criteriaBuilder.frameUnboundedFollowing();
	}

	@Override
	public <T> JpaExpression<T> windowFunction(String name, Class<T> type, JpaWindow window, Expression<?>... args) {
		return criteriaBuilder.windowFunction( name, type, window, args );
	}

	@Override
	public JpaExpression<Long> rowNumber(JpaWindow window) {
		return criteriaBuilder.rowNumber( window );
	}

	@Override
	public <T> JpaExpression<T> firstValue(Expression<T> argument, JpaWindow window) {
		return criteriaBuilder.firstValue( argument, window );
	}

	@Override
	public <T> JpaExpression<T> lastValue(Expression<T> argument, JpaWindow window) {
		return criteriaBuilder.lastValue( argument, window );
	}

	@Override
	public <T> JpaExpression<T> nthValue(Expression<T> argument, int n, JpaWindow window) {
		return criteriaBuilder.nthValue( argument, n, window );
	}

	@Override
	public <T> JpaExpression<T> nthValue(Expression<T> argument, Expression<Integer> n, JpaWindow window) {
		return criteriaBuilder.nthValue( argument, n, window );
	}

	@Override
	public JpaExpression<Long> rank(JpaWindow window) {
		return criteriaBuilder.rank( window );
	}

	@Override
	public JpaExpression<Long> denseRank(JpaWindow window) {
		return criteriaBuilder.denseRank( window );
	}

	@Override
	public JpaExpression<Double> percentRank(JpaWindow window) {
		return criteriaBuilder.percentRank( window );
	}

	@Override
	public JpaExpression<Double> cumeDist(JpaWindow window) {
		return criteriaBuilder.cumeDist( window );
	}

	@Override
	public <T> JpaExpression<T> functionAggregate(
			String name,
			Class<T> type,
			JpaPredicate filter,
			Expression<?>... args) {
		return criteriaBuilder.functionAggregate( name, type, filter, args );
	}

	@Override
	public <T> JpaExpression<T> functionAggregate(String name, Class<T> type, JpaWindow window, Expression<?>... args) {
		return criteriaBuilder.functionAggregate( name, type, window, args );
	}

	@Override
	public <T> JpaExpression<T> functionAggregate(
			String name,
			Class<T> type,
			JpaPredicate filter,
			JpaWindow window,
			Expression<?>... args) {
		return criteriaBuilder.functionAggregate( name, type, filter, window, args );
	}

	@Override
	public <N extends Number> JpaExpression<Number> sum(Expression<N> argument, JpaPredicate filter) {
		return criteriaBuilder.sum( argument, filter );
	}

	@Override
	public <N extends Number> JpaExpression<Number> sum(Expression<N> argument, JpaWindow window) {
		return criteriaBuilder.sum( argument, window );
	}

	@Override
	public <N extends Number> JpaExpression<Number> sum(Expression<N> argument, JpaPredicate filter, JpaWindow window) {
		return criteriaBuilder.sum( argument, filter, window );
	}

	@Override
	public <N extends Number> JpaExpression<Double> avg(Expression<N> argument, JpaPredicate filter) {
		return criteriaBuilder.avg( argument, filter );
	}

	@Override
	public <N extends Number> JpaExpression<Double> avg(Expression<N> argument, JpaWindow window) {
		return criteriaBuilder.avg( argument, window );
	}

	@Override
	public <N extends Number> JpaExpression<Double> avg(Expression<N> argument, JpaPredicate filter, JpaWindow window) {
		return criteriaBuilder.avg( argument, filter, window );
	}

	@Override
	public JpaExpression<Long> count(Expression<?> argument, JpaPredicate filter) {
		return criteriaBuilder.count( argument, filter );
	}

	@Override
	public JpaExpression<Long> count(Expression<?> argument, JpaWindow window) {
		return criteriaBuilder.count( argument, window );
	}

	@Override
	public JpaExpression<Long> count(Expression<?> argument, JpaPredicate filter, JpaWindow window) {
		return criteriaBuilder.count( argument, filter, window );
	}

	@Override
	public <T> JpaExpression<T> functionWithinGroup(String name, Class<T> type, JpaOrder order, Expression<?>... args) {
		return criteriaBuilder.functionWithinGroup( name, type, order, args );
	}

	@Override
	public <T> JpaExpression<T> functionWithinGroup(
			String name,
			Class<T> type,
			JpaOrder order,
			JpaPredicate filter,
			Expression<?>... args) {
		return criteriaBuilder.functionWithinGroup( name, type, order, filter, args );
	}

	@Override
	public <T> JpaExpression<T> functionWithinGroup(
			String name,
			Class<T> type,
			JpaOrder order,
			JpaWindow window,
			Expression<?>... args) {
		return criteriaBuilder.functionWithinGroup( name, type, order, window, args );
	}

	@Override
	public <T> JpaExpression<T> functionWithinGroup(
			String name,
			Class<T> type,
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<?>... args) {
		return criteriaBuilder.functionWithinGroup( name, type, order, filter, window, args );
	}

	@Override
	public JpaExpression<String> listagg(JpaOrder order, Expression<String> argument, String separator) {
		return criteriaBuilder.listagg( order, argument, separator );
	}

	@Override
	public JpaExpression<String> listagg(JpaOrder order, Expression<String> argument, Expression<String> separator) {
		return criteriaBuilder.listagg( order, argument, separator );
	}

	@Override
	public JpaExpression<String> listagg(
			JpaOrder order,
			JpaPredicate filter,
			Expression<String> argument,
			String separator) {
		return criteriaBuilder.listagg( order, filter, argument, separator );
	}

	@Override
	public JpaExpression<String> listagg(
			JpaOrder order,
			JpaPredicate filter,
			Expression<String> argument,
			Expression<String> separator) {
		return criteriaBuilder.listagg( order, filter, argument, separator );
	}

	@Override
	public JpaExpression<String> listagg(
			JpaOrder order,
			JpaWindow window,
			Expression<String> argument,
			String separator) {
		return criteriaBuilder.listagg( order, window, argument, separator );
	}

	@Override
	public JpaExpression<String> listagg(
			JpaOrder order,
			JpaWindow window,
			Expression<String> argument,
			Expression<String> separator) {
		return criteriaBuilder.listagg( order, window, argument, separator );
	}

	@Override
	public JpaExpression<String> listagg(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<String> argument,
			String separator) {
		return criteriaBuilder.listagg( order, filter, window, argument, separator );
	}

	@Override
	public JpaExpression<String> listagg(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<String> argument,
			Expression<String> separator) {
		return criteriaBuilder.listagg( order, filter, window, argument, separator );
	}

	@Override
	public <T> JpaExpression<T> mode(Expression<T> sortExpression, SortDirection sortOrder, NullPrecedence nullPrecedence) {
		return criteriaBuilder.mode( sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> mode(
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.mode( filter, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> mode(
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.mode( window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> mode(
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.mode( filter, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, filter, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, filter, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.percentileDisc( argument, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.percentileDisc( argument, filter, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.percentileDisc( argument, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		return criteriaBuilder.percentileDisc( argument, filter, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public JpaExpression<Long> rank(JpaOrder order, Expression<?>... arguments) {
		return criteriaBuilder.rank( order, arguments );
	}

	@Override
	public JpaExpression<Long> rank(JpaOrder order, JpaPredicate filter, Expression<?>... arguments) {
		return criteriaBuilder.rank( order, filter, arguments );
	}

	@Override
	public JpaExpression<Long> rank(JpaOrder order, JpaWindow window, Expression<?>... arguments) {
		return criteriaBuilder.rank( order, window, arguments );
	}

	@Override
	public JpaExpression<Long> rank(JpaOrder order, JpaPredicate filter, JpaWindow window, Expression<?>... arguments) {
		return criteriaBuilder.rank( order, filter, window, arguments );
	}

	@Override
	public JpaExpression<Double> percentRank(JpaOrder order, Expression<?>... arguments) {
		return criteriaBuilder.percentRank( order, arguments );
	}

	@Override
	public JpaExpression<Double> percentRank(JpaOrder order, JpaPredicate filter, Expression<?>... arguments) {
		return criteriaBuilder.percentRank( order, filter, arguments );
	}

	@Override
	public JpaExpression<Double> percentRank(JpaOrder order, JpaWindow window, Expression<?>... arguments) {
		return criteriaBuilder.percentRank( order, window, arguments );
	}

	@Override
	public JpaExpression<Double> percentRank(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<?>... arguments) {
		return criteriaBuilder.percentRank( order, filter, window, arguments );
	}

	@Override
	public JpaExpression<Duration> durationSum(Expression<Duration> x, Expression<Duration> y) {
		return criteriaBuilder.durationSum( x, y );
	}

	@Override
	public JpaExpression<Duration> durationSum(Expression<Duration> x, Duration y) {
		return criteriaBuilder.durationSum( x, y );
	}

	@Override
	public JpaExpression<Duration> durationDiff(Expression<Duration> x, Expression<Duration> y) {
		return criteriaBuilder.durationDiff( x, y );
	}

	@Override
	public JpaExpression<Duration> durationDiff(Expression<Duration> x, Duration y) {
		return criteriaBuilder.durationDiff( x, y );
	}

	@Override
	public JpaExpression<Duration> durationScaled(Expression<? extends Number> number, Expression<Duration> duration) {
		return criteriaBuilder.durationScaled( number, duration );
	}

	@Override
	public JpaExpression<Duration> durationScaled(Number number, Expression<Duration> duration) {
		return criteriaBuilder.durationScaled( number, duration );
	}

	@Override
	public JpaExpression<Duration> durationScaled(Expression<? extends Number> number, Duration duration) {
		return criteriaBuilder.durationScaled( number, duration );
	}

	@Override
	public <T extends Temporal> JpaExpression<Duration> durationBetween(Expression<T> x, Expression<T> y) {
		return criteriaBuilder.durationBetween( x, y );
	}

	@Override
	public <T extends Temporal> JpaExpression<Duration> durationBetween(Expression<T> x, T y) {
		return criteriaBuilder.durationBetween( x, y );
	}

	@Override
	public <T extends Temporal> JpaExpression<T> addDuration(Expression<T> datetime, Expression<Duration> duration) {
		return criteriaBuilder.addDuration( datetime, duration );
	}

	@Override
	public <T extends Temporal> JpaExpression<T> addDuration(Expression<T> datetime, Duration duration) {
		return criteriaBuilder.addDuration( datetime, duration );
	}

	@Override
	public <T extends Temporal> JpaExpression<T> addDuration(T datetime, Expression<Duration> duration) {
		return criteriaBuilder.addDuration(datetime, duration);
	}

	@Override
	public <T extends Temporal> JpaExpression<T> subtractDuration(Expression<T> datetime, Expression<Duration> duration) {
		return criteriaBuilder.subtractDuration( datetime, duration );
	}

	@Override
	public <T extends Temporal> JpaExpression<T> subtractDuration(Expression<T> datetime, Duration duration) {
		return criteriaBuilder.subtractDuration( datetime, duration );
	}

	@Override
	public <T extends Temporal> JpaExpression<T> subtractDuration(T datetime, Expression<Duration> duration) {
		return criteriaBuilder.subtractDuration(datetime, duration);
	}

	@Override
	public JpaExpression<Long> durationByUnit(TemporalUnit unit, Expression<Duration> duration) {
		return criteriaBuilder.durationByUnit(unit, duration);
	}

	@Override
	public JpaExpression<Duration> duration(long magnitude, TemporalUnit unit) {
		return criteriaBuilder.duration( magnitude, unit );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayAgg(JpaOrder order, Expression<? extends T> argument) {
		return criteriaBuilder.arrayAgg( order, argument );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayAgg(JpaOrder order, JpaPredicate filter, Expression<? extends T> argument) {
		return criteriaBuilder.arrayAgg( order, filter, argument );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayAgg(JpaOrder order, JpaWindow window, Expression<? extends T> argument) {
		return criteriaBuilder.arrayAgg( order, window, argument );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayAgg(
			JpaOrder order,
			JpaPredicate filter,
			JpaWindow window,
			Expression<? extends T> argument) {
		return criteriaBuilder.arrayAgg( order, filter, window, argument );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayLiteral(T... elements) {
		return criteriaBuilder.arrayLiteral( elements );
	}

	@Override
	@Incubating
	public <T> JpaExpression<Integer> arrayLength(Expression<T[]> arrayExpression) {
		return criteriaBuilder.arrayLength( arrayExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<Integer> arrayPosition(Expression<T[]> arrayExpression, T element) {
		return criteriaBuilder.arrayPosition( arrayExpression, element );
	}

	@Override
	@Incubating
	public <T> JpaExpression<Integer> arrayPosition(Expression<T[]> arrayExpression, Expression<T> elementExpression) {
		return criteriaBuilder.arrayPosition( arrayExpression, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<int[]> arrayPositions(Expression<T[]> arrayExpression, Expression<T> elementExpression) {
		return criteriaBuilder.arrayPositions( arrayExpression, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<int[]> arrayPositions(Expression<T[]> arrayExpression, T element) {
		return criteriaBuilder.arrayPositions( arrayExpression, element );
	}

	@Override
	@Incubating
	public <T> JpaExpression<List<Integer>> arrayPositionsList(
			Expression<T[]> arrayExpression,
			Expression<T> elementExpression) {
		return criteriaBuilder.arrayPositionsList( arrayExpression, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<List<Integer>> arrayPositionsList(Expression<T[]> arrayExpression, T element) {
		return criteriaBuilder.arrayPositionsList( arrayExpression, element );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayConcat(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayConcat( arrayExpression1, arrayExpression2 );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayConcat(Expression<T[]> arrayExpression1, T[] array2) {
		return criteriaBuilder.arrayConcat( arrayExpression1, array2 );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayConcat(T[] array1, Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayConcat( array1, arrayExpression2 );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayAppend(Expression<T[]> arrayExpression, Expression<T> elementExpression) {
		return criteriaBuilder.arrayAppend( arrayExpression, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayAppend(Expression<T[]> arrayExpression, T element) {
		return criteriaBuilder.arrayAppend( arrayExpression, element );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayPrepend(Expression<T> elementExpression, Expression<T[]> arrayExpression) {
		return criteriaBuilder.arrayPrepend( elementExpression, arrayExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayPrepend(T element, Expression<T[]> arrayExpression) {
		return criteriaBuilder.arrayPrepend( element, arrayExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T> arrayGet(Expression<T[]> arrayExpression, Expression<Integer> indexExpression) {
		return criteriaBuilder.arrayGet( arrayExpression, indexExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T> arrayGet(Expression<T[]> arrayExpression, Integer index) {
		return criteriaBuilder.arrayGet( arrayExpression, index );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySet(
			Expression<T[]> arrayExpression,
			Expression<Integer> indexExpression,
			Expression<T> elementExpression) {
		return criteriaBuilder.arraySet( arrayExpression, indexExpression, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySet(
			Expression<T[]> arrayExpression,
			Expression<Integer> indexExpression,
			T element) {
		return criteriaBuilder.arraySet( arrayExpression, indexExpression, element );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySet(
			Expression<T[]> arrayExpression,
			Integer index,
			Expression<T> elementExpression) {
		return criteriaBuilder.arraySet( arrayExpression, index, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySet(Expression<T[]> arrayExpression, Integer index, T element) {
		return criteriaBuilder.arraySet( arrayExpression, index, element );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayRemove(Expression<T[]> arrayExpression, Expression<T> elementExpression) {
		return criteriaBuilder.arrayRemove( arrayExpression, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayRemove(Expression<T[]> arrayExpression, T element) {
		return criteriaBuilder.arrayRemove( arrayExpression, element );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayRemoveIndex(
			Expression<T[]> arrayExpression,
			Expression<Integer> indexExpression) {
		return criteriaBuilder.arrayRemoveIndex( arrayExpression, indexExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayRemoveIndex(Expression<T[]> arrayExpression, Integer index) {
		return criteriaBuilder.arrayRemoveIndex( arrayExpression, index );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySlice(
			Expression<T[]> arrayExpression,
			Expression<Integer> lowerIndexExpression,
			Expression<Integer> upperIndexExpression) {
		return criteriaBuilder.arraySlice( arrayExpression, lowerIndexExpression, upperIndexExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySlice(
			Expression<T[]> arrayExpression,
			Expression<Integer> lowerIndexExpression,
			Integer upperIndex) {
		return criteriaBuilder.arraySlice( arrayExpression, lowerIndexExpression, upperIndex );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySlice(
			Expression<T[]> arrayExpression,
			Integer lowerIndex,
			Expression<Integer> upperIndexExpression) {
		return criteriaBuilder.arraySlice( arrayExpression, lowerIndex, upperIndexExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySlice(Expression<T[]> arrayExpression, Integer lowerIndex, Integer upperIndex) {
		return criteriaBuilder.arraySlice( arrayExpression, lowerIndex, upperIndex );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayReplace(
			Expression<T[]> arrayExpression,
			Expression<T> oldElementExpression,
			Expression<T> newElementExpression) {
		return criteriaBuilder.arrayReplace( arrayExpression, oldElementExpression, newElementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayReplace(
			Expression<T[]> arrayExpression,
			Expression<T> oldElementExpression,
			T newElement) {
		return criteriaBuilder.arrayReplace( arrayExpression, oldElementExpression, newElement );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayReplace(
			Expression<T[]> arrayExpression,
			T oldElement,
			Expression<T> newElementExpression) {
		return criteriaBuilder.arrayReplace( arrayExpression, oldElement, newElementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayReplace(Expression<T[]> arrayExpression, T oldElement, T newElement) {
		return criteriaBuilder.arrayReplace( arrayExpression, oldElement, newElement );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayTrim(
			Expression<T[]> arrayExpression,
			Expression<Integer> elementCountExpression) {
		return criteriaBuilder.arrayTrim( arrayExpression, elementCountExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayTrim(Expression<T[]> arrayExpression, Integer elementCount) {
		return criteriaBuilder.arrayTrim( arrayExpression, elementCount );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayFill(
			Expression<T> elementExpression,
			Expression<Integer> elementCountExpression) {
		return criteriaBuilder.arrayFill( elementExpression, elementCountExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayFill(Expression<T> elementExpression, Integer elementCount) {
		return criteriaBuilder.arrayFill( elementExpression, elementCount );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayFill(T element, Expression<Integer> elementCountExpression) {
		return criteriaBuilder.arrayFill( element, elementCountExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arrayFill(T element, Integer elementCount) {
		return criteriaBuilder.arrayFill( element, elementCount );
	}

	@Override
	@Incubating
	public JpaExpression<String> arrayToString(
			Expression<? extends Object[]> arrayExpression,
			Expression<String> separatorExpression) {
		return criteriaBuilder.arrayToString( arrayExpression, separatorExpression );
	}

	@Override
	@Incubating
	public JpaExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, String separator) {
		return criteriaBuilder.arrayToString( arrayExpression, separator );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayContains(Expression<T[]> arrayExpression, Expression<T> elementExpression) {
		return criteriaBuilder.arrayContains( arrayExpression, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayContains(Expression<T[]> arrayExpression, T element) {
		return criteriaBuilder.arrayContains( arrayExpression, element );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayContains(T[] array, Expression<T> elementExpression) {
		return criteriaBuilder.arrayContains( array, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayContainsNullable(Expression<T[]> arrayExpression, Expression<T> elementExpression) {
		return criteriaBuilder.arrayContainsNullable( arrayExpression, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayContainsNullable(Expression<T[]> arrayExpression, T element) {
		return criteriaBuilder.arrayContainsNullable( arrayExpression, element );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayContainsNullable(T[] array, Expression<T> elementExpression) {
		return criteriaBuilder.arrayContainsNullable( array, elementExpression );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayContainsAll(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayContainsAll( arrayExpression, subArrayExpression );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayContainsAll(Expression<T[]> arrayExpression, T[] subArray) {
		return criteriaBuilder.arrayContainsAll( arrayExpression, subArray );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayContainsAll(T[] array, Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayContainsAll( array, subArrayExpression );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayContainsAllNullable(
			Expression<T[]> arrayExpression,
			Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayContainsAllNullable( arrayExpression, subArrayExpression );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayContainsAllNullable(Expression<T[]> arrayExpression, T[] subArray) {
		return criteriaBuilder.arrayContainsAllNullable( arrayExpression, subArray );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayContainsAllNullable(T[] array, Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayContainsAllNullable( array, subArrayExpression );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIncludes(Expression<T[]> arrayExpression, Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayIncludes( arrayExpression, subArrayExpression );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIncludes(Expression<T[]> arrayExpression, T[] subArray) {
		return criteriaBuilder.arrayIncludes( arrayExpression, subArray );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIncludes(T[] array, Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayIncludes( array, subArrayExpression );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIncludesNullable(
			Expression<T[]> arrayExpression,
			Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayIncludesNullable( arrayExpression, subArrayExpression );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIncludesNullable(Expression<T[]> arrayExpression, T[] subArray) {
		return criteriaBuilder.arrayIncludesNullable( arrayExpression, subArray );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIncludesNullable(T[] array, Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayIncludesNullable( array, subArrayExpression );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayOverlaps(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayOverlaps( arrayExpression1, arrayExpression2 );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayOverlaps(Expression<T[]> arrayExpression1, T[] array2) {
		return criteriaBuilder.arrayOverlaps( arrayExpression1, array2 );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayOverlaps(T[] array1, Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayOverlaps( array1, arrayExpression2 );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayOverlapsNullable(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayOverlapsNullable( arrayExpression1, arrayExpression2 );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayOverlapsNullable(Expression<T[]> arrayExpression1, T[] array2) {
		return criteriaBuilder.arrayOverlapsNullable( arrayExpression1, array2 );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <T> JpaPredicate arrayOverlapsNullable(T[] array1, Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayOverlapsNullable( array1, arrayExpression2 );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIntersects(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayIntersects( arrayExpression1, arrayExpression2 );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIntersects(Expression<T[]> arrayExpression1, T[] array2) {
		return criteriaBuilder.arrayIntersects( arrayExpression1, array2 );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIntersects(T[] array1, Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayIntersects( array1, arrayExpression2 );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIntersectsNullable(Expression<T[]> arrayExpression1, Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayIntersectsNullable( arrayExpression1, arrayExpression2 );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIntersectsNullable(Expression<T[]> arrayExpression1, T[] array2) {
		return criteriaBuilder.arrayIntersectsNullable( arrayExpression1, array2 );
	}

	@Override
	@Incubating
	public <T> JpaPredicate arrayIntersectsNullable(T[] array1, Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayIntersectsNullable( array1, arrayExpression2 );
	}

	@Override
	@Incubating
	public <E, C extends Collection<E>> JpaExpression<C> collectionLiteral(E... elements) {
		return criteriaBuilder.collectionLiteral( elements );
	}

	@Override
	@Incubating
	public JpaExpression<Integer> collectionLength(Expression<? extends Collection<?>> collectionExpression) {
		return criteriaBuilder.collectionLength( collectionExpression );
	}

	@Override
	@Incubating
	public <E> JpaExpression<Integer> collectionPosition(
			Expression<? extends Collection<? extends E>> collectionExpression,
			E element) {
		return criteriaBuilder.collectionPosition( collectionExpression, element );
	}

	@Override
	@Incubating
	public <E> JpaExpression<Integer> collectionPosition(
			Expression<? extends Collection<? extends E>> collectionExpression,
			Expression<E> elementExpression) {
		return criteriaBuilder.collectionPosition( collectionExpression, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<int[]> collectionPositions(
			Expression<? extends Collection<? super T>> collectionExpression,
			Expression<T> elementExpression) {
		return criteriaBuilder.collectionPositions( collectionExpression, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<int[]> collectionPositions(
			Expression<? extends Collection<? super T>> collectionExpression,
			T element) {
		return criteriaBuilder.collectionPositions( collectionExpression, element );
	}

	@Override
	@Incubating
	public <T> JpaExpression<List<Integer>> collectionPositionsList(
			Expression<? extends Collection<? super T>> collectionExpression,
			Expression<T> elementExpression) {
		return criteriaBuilder.collectionPositionsList( collectionExpression, elementExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<List<Integer>> collectionPositionsList(
			Expression<? extends Collection<? super T>> collectionExpression,
			T element) {
		return criteriaBuilder.collectionPositionsList( collectionExpression, element );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(
			Expression<C> collectionExpression1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionConcat( collectionExpression1, collectionExpression2 );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(
			Expression<C> collectionExpression1,
			Collection<? extends E> collection2) {
		return criteriaBuilder.collectionConcat( collectionExpression1, collection2 );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(
			C collection1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionConcat( collection1, collectionExpression2 );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionAppend(
			Expression<C> collectionExpression,
			Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionAppend( collectionExpression, elementExpression );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionAppend(
			Expression<C> collectionExpression,
			E element) {
		return criteriaBuilder.collectionAppend( collectionExpression, element );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionPrepend(
			Expression<? extends E> elementExpression,
			Expression<C> collectionExpression) {
		return criteriaBuilder.collectionPrepend( elementExpression, collectionExpression );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionPrepend(
			E element,
			Expression<C> collectionExpression) {
		return criteriaBuilder.collectionPrepend( element, collectionExpression );
	}

	@Override
	@Incubating
	public <E> JpaExpression<E> collectionGet(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<Integer> indexExpression) {
		return criteriaBuilder.collectionGet( collectionExpression, indexExpression );
	}

	@Override
	@Incubating
	public <E> JpaExpression<E> collectionGet(Expression<? extends Collection<E>> collectionExpression, Integer index) {
		return criteriaBuilder.collectionGet( collectionExpression, index );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionSet(
			Expression<C> collectionExpression,
			Expression<Integer> indexExpression,
			Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionSet( collectionExpression, indexExpression, elementExpression );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionSet(
			Expression<C> collectionExpression,
			Expression<Integer> indexExpression,
			E element) {
		return criteriaBuilder.collectionSet( collectionExpression, indexExpression, element );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionSet(
			Expression<C> collectionExpression,
			Integer index,
			Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionSet( collectionExpression, index, elementExpression );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionSet(
			Expression<C> collectionExpression,
			Integer index,
			E element) {
		return criteriaBuilder.collectionSet( collectionExpression, index, element );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionRemove(
			Expression<C> collectionExpression,
			Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionRemove( collectionExpression, elementExpression );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionRemove(
			Expression<C> collectionExpression,
			E element) {
		return criteriaBuilder.collectionRemove( collectionExpression, element );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionRemoveIndex(
			Expression<C> collectionExpression,
			Expression<Integer> indexExpression) {
		return criteriaBuilder.collectionRemoveIndex( collectionExpression, indexExpression );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionRemoveIndex(
			Expression<C> collectionExpression,
			Integer index) {
		return criteriaBuilder.collectionRemoveIndex( collectionExpression, index );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionSlice(
			Expression<C> collectionExpression,
			Expression<Integer> lowerIndexExpression,
			Expression<Integer> upperIndexExpression) {
		return criteriaBuilder.collectionSlice( collectionExpression, lowerIndexExpression, upperIndexExpression );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionSlice(
			Expression<C> collectionExpression,
			Expression<Integer> lowerIndexExpression,
			Integer upperIndex) {
		return criteriaBuilder.collectionSlice( collectionExpression, lowerIndexExpression, upperIndex );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionSlice(
			Expression<C> collectionExpression,
			Integer lowerIndex,
			Expression<Integer> upperIndexExpression) {
		return criteriaBuilder.collectionSlice( collectionExpression, lowerIndex, upperIndexExpression );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionSlice(
			Expression<C> collectionExpression,
			Integer lowerIndex,
			Integer upperIndex) {
		return criteriaBuilder.collectionSlice( collectionExpression, lowerIndex, upperIndex );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(
			Expression<C> collectionExpression,
			Expression<? extends E> oldElementExpression,
			Expression<? extends E> newElementExpression) {
		return criteriaBuilder.collectionReplace( collectionExpression, oldElementExpression, newElementExpression );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(
			Expression<C> collectionExpression,
			Expression<? extends E> oldElementExpression,
			E newElement) {
		return criteriaBuilder.collectionReplace( collectionExpression, oldElementExpression, newElement );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(
			Expression<C> collectionExpression,
			E oldElement,
			Expression<? extends E> newElementExpression) {
		return criteriaBuilder.collectionReplace( collectionExpression, oldElement, newElementExpression );
	}

	@Override
	@Incubating
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(
			Expression<C> collectionExpression,
			E oldElement,
			E newElement) {
		return criteriaBuilder.collectionReplace( collectionExpression, oldElement, newElement );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionTrim(
			Expression<C> arrayExpression,
			Expression<Integer> elementCountExpression) {
		return criteriaBuilder.collectionTrim( arrayExpression, elementCountExpression );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionTrim(
			Expression<C> arrayExpression,
			Integer elementCount) {
		return criteriaBuilder.collectionTrim( arrayExpression, elementCount );
	}

	@Override
	@Incubating
	public <T> JpaExpression<Collection<T>> collectionFill(
			Expression<T> elementExpression,
			Expression<Integer> elementCountExpression) {
		return criteriaBuilder.collectionFill( elementExpression, elementCountExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<Collection<T>> collectionFill(Expression<T> elementExpression, Integer elementCount) {
		return criteriaBuilder.collectionFill( elementExpression, elementCount );
	}

	@Override
	@Incubating
	public <T> JpaExpression<Collection<T>> collectionFill(T element, Expression<Integer> elementCountExpression) {
		return criteriaBuilder.collectionFill( element, elementCountExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<Collection<T>> collectionFill(T element, Integer elementCount) {
		return criteriaBuilder.collectionFill( element, elementCount );
	}

	@Override
	@Incubating
	public <T> JpaExpression<String> collectionToString(
			Expression<? extends Collection<?>> collectionExpression,
			Expression<String> separatorExpression) {
		return criteriaBuilder.collectionToString( collectionExpression, separatorExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<String> collectionToString(
			Expression<? extends Collection<?>> collectionExpression,
			String separator) {
		return criteriaBuilder.collectionToString( collectionExpression, separator );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionContains(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionContains( collectionExpression, elementExpression );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionContains(Expression<? extends Collection<E>> collectionExpression, E element) {
		return criteriaBuilder.collectionContains( collectionExpression, element );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionContains(Collection<E> collection, Expression<E> elementExpression) {
		return criteriaBuilder.collectionContains( collection, elementExpression );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionContainsNullable(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionContainsNullable( collectionExpression, elementExpression );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionContainsNullable(
			Expression<? extends Collection<E>> collectionExpression,
			E element) {
		return criteriaBuilder.collectionContainsNullable( collectionExpression, element );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionContainsNullable(Collection<E> collection, Expression<E> elementExpression) {
		return criteriaBuilder.collectionContainsNullable( collection, elementExpression );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionContainsAll(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return criteriaBuilder.collectionContainsAll( collectionExpression, subCollectionExpression );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionContainsAll(
			Expression<? extends Collection<E>> collectionExpression,
			Collection<? extends E> subCollection) {
		return criteriaBuilder.collectionContainsAll( collectionExpression, subCollection );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionContainsAll(
			Collection<E> collection,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return criteriaBuilder.collectionContainsAll( collection, subCollectionExpression );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionContainsAllNullable(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return criteriaBuilder.collectionContainsAllNullable( collectionExpression, subCollectionExpression );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionContainsAllNullable(
			Expression<? extends Collection<E>> collectionExpression,
			Collection<? extends E> subCollection) {
		return criteriaBuilder.collectionContainsAllNullable( collectionExpression, subCollection );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionContainsAllNullable(
			Collection<E> collection,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return criteriaBuilder.collectionContainsAllNullable( collection, subCollectionExpression );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIncludes(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return criteriaBuilder.collectionIncludes( collectionExpression, subCollectionExpression );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIncludes(
			Expression<? extends Collection<E>> collectionExpression,
			Collection<? extends E> subCollection) {
		return criteriaBuilder.collectionIncludes( collectionExpression, subCollection );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIncludes(
			Collection<E> collection,
			Expression<? extends Collection<? extends E>> subArrayExpression) {
		return criteriaBuilder.collectionIncludes( collection, subArrayExpression );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIncludesNullable(
			Expression<? extends Collection<E>> collectionExpression,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return criteriaBuilder.collectionIncludesNullable( collectionExpression, subCollectionExpression );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIncludesNullable(
			Expression<? extends Collection<E>> collectionExpression,
			Collection<? extends E> subCollection) {
		return criteriaBuilder.collectionIncludesNullable( collectionExpression, subCollection );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIncludesNullable(
			Collection<E> collection,
			Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return criteriaBuilder.collectionIncludesNullable( collection, subCollectionExpression );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionOverlaps(
			Expression<? extends Collection<E>> collectionExpression1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionOverlaps( collectionExpression1, collectionExpression2 );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionOverlaps(
			Expression<? extends Collection<E>> collectionExpression1,
			Collection<? extends E> collection2) {
		return criteriaBuilder.collectionOverlaps( collectionExpression1, collection2 );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionOverlaps(
			Collection<E> collection1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionOverlaps( collection1, collectionExpression2 );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionOverlapsNullable(
			Expression<? extends Collection<E>> collectionExpression1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionOverlapsNullable( collectionExpression1, collectionExpression2 );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionOverlapsNullable(
			Expression<? extends Collection<E>> collectionExpression1,
			Collection<? extends E> collection2) {
		return criteriaBuilder.collectionOverlapsNullable( collectionExpression1, collection2 );
	}

	@Override
	@Deprecated(forRemoval = true)
	@Incubating
	public <E> JpaPredicate collectionOverlapsNullable(
			Collection<E> collection1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionOverlapsNullable( collection1, collectionExpression2 );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIntersects(
			Expression<? extends Collection<E>> collectionExpression1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionIntersects( collectionExpression1, collectionExpression2 );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIntersects(
			Expression<? extends Collection<E>> collectionExpression1,
			Collection<? extends E> collection2) {
		return criteriaBuilder.collectionIntersects( collectionExpression1, collection2 );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIntersects(
			Collection<E> collection1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionIntersects( collection1, collectionExpression2 );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIntersectsNullable(
			Expression<? extends Collection<E>> collectionExpression1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionIntersectsNullable( collectionExpression1, collectionExpression2 );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIntersectsNullable(
			Expression<? extends Collection<E>> collectionExpression1,
			Collection<? extends E> collection2) {
		return criteriaBuilder.collectionIntersectsNullable( collectionExpression1, collection2 );
	}

	@Override
	@Incubating
	public <E> JpaPredicate collectionIntersectsNullable(
			Collection<E> collection1,
			Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionIntersectsNullable( collection1, collectionExpression2 );
	}
}
