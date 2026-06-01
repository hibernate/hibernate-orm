/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria.spi;

import jakarta.annotation.Nullable;
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
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.StatementReference;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaStatement;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.NumericExpression;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.TemporalExpression;
import jakarta.persistence.criteria.TextExpression;
import org.hibernate.Incubating;
import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.*;
import org.hibernate.query.common.TemporalUnit;

import jakarta.persistence.Tuple;
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
	public <X, T> JpaExpression<X> cast(JpaExpression<T> expression, JpaCastTarget<X> castTarget) {
		return criteriaBuilder.cast( expression, castTarget );
	}

	@Override
	public <X> JpaCastTarget<X> castTarget(Class<X> castTargetJavaType) {
		return criteriaBuilder.castTarget( castTargetJavaType );
	}

	@Override
	public <X> JpaCastTarget<X> castTarget(Class<X> castTargetJavaType, long length) {
		return criteriaBuilder.castTarget( castTargetJavaType, length );
	}

	@Override
	public <X> JpaCastTarget<X> castTarget(Class<X> castTargetJavaType, int precision, int scale) {
		return criteriaBuilder.castTarget( castTargetJavaType, precision, scale );
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
	public JpaPredicate wrap(BooleanExpression... expressions) {
		return criteriaBuilder.wrap( expressions );
	}

	@Override
	public <T extends HibernateCriteriaBuilder> T unwrap(Class<T> clazz) {
		return criteriaBuilder.unwrap( clazz );
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<Object> createQuery() {
		return criteriaBuilder.createQuery();
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> createQuery(@Nonnull Class<T> resultClass) {
		return criteriaBuilder.createQuery( resultClass );
	}

	@Override
	public <T> JpaCriteriaQuery<T> createQuery(String hql, Class<T> resultClass) {
		return criteriaBuilder.createQuery( hql, resultClass );
	}

	@Nonnull
	@Override
	public <T> CriteriaQuery<T> createQuery(@Nonnull Class<T> resultClass, @Nonnull String jpql) {
		return criteriaBuilder.createQuery( resultClass, jpql );
	}

	@Nonnull
	@Override
	public CriteriaQuery<?> createQuery(@Nonnull String jpql) {
		return criteriaBuilder.createQuery( jpql );
	}

	@Nonnull
	@Override
	public JpaCriteriaQuery<Tuple> createTupleQuery() {
		return criteriaBuilder.createTupleQuery();
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaUpdate<T> createCriteriaUpdate(@Nonnull Class<T> targetEntity) {
		return criteriaBuilder.createCriteriaUpdate( targetEntity );
	}

	@Nonnull
	@Override
	public <T> CriteriaUpdate<T> createCriteriaUpdate(@Nonnull Class<T> targetEntity, @Nonnull String jpql) {
		return criteriaBuilder.createCriteriaUpdate( targetEntity, jpql );
	}

	@Nonnull
	@Override
	public CriteriaUpdate<?> createCriteriaUpdate(@Nonnull String jpql) {
		return criteriaBuilder.createCriteriaUpdate( jpql );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaDelete<T> createCriteriaDelete(@Nonnull Class<T> targetEntity) {
		return criteriaBuilder.createCriteriaDelete( targetEntity );
	}

	@Nonnull
	@Override
	public <T> CriteriaDelete<T> createCriteriaDelete(@Nonnull Class<T> targetEntity, @Nonnull String jpql) {
		return criteriaBuilder.createCriteriaDelete( targetEntity, jpql );
	}

	@Nonnull
	@Override
	public CriteriaDelete<?> createCriteriaDelete(@Nonnull String jpql) {
		return criteriaBuilder.createCriteriaDelete( jpql );
	}

	@Nonnull
	@Override
	public <T> TypedQueryReference<T> augment(
			@Nonnull TypedQueryReference<T> reference,
			@Nonnull Consumer<CriteriaQuery<T>> augmentation) {
		return criteriaBuilder.augment( reference, augmentation );
	}

	@Nonnull
	@Override
	public StatementReference augment(
			@Nonnull StatementReference reference,
			@Nonnull Consumer<CriteriaStatement<?>> augmentation) {
		return criteriaBuilder.augment( reference, augmentation );
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

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> union(@Nonnull CriteriaSelect<? extends T> left, @Nonnull CriteriaSelect<? extends T> right) {
		return criteriaBuilder.union( left, right );
	}

	@Override
	public <T> JpaSubQuery<T> unionAll(JpaSubQuery<? extends T> query1, JpaSubQuery<? extends T> query2) {
		return criteriaBuilder.unionAll( query1, query2 );
	}

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> unionAll(@Nonnull CriteriaSelect<? extends T> left, @Nonnull CriteriaSelect<? extends T> right) {
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

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> except(@Nonnull CriteriaSelect<T> left, @Nonnull CriteriaSelect<?> right) {
		return criteriaBuilder.except( left, right );
	}

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> exceptAll(@Nonnull CriteriaSelect<T> left, @Nonnull CriteriaSelect<?> right) {
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



	@Nonnull
	@Override
	public JpaExpression<Integer> sign(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.sign( x );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> ceiling(@Nonnull Expression<N> x) {
		return criteriaBuilder.ceiling( x );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> floor(@Nonnull Expression<N> x) {
		return criteriaBuilder.floor( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> exp(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.exp( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> ln(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.ln( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> power(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.power( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> power(@Nonnull Expression<? extends Number> x, Number y) {
		return criteriaBuilder.power( x, y );
	}

	@Nonnull
	@Override
	public <T extends Number> JpaExpression<T> round(@Nonnull Expression<T> x, @Nonnull Integer n) {
		return criteriaBuilder.round( x, n );
	}

	@Override
	public <T extends Number> JpaExpression<T> truncate(Expression<T> x, Integer n) {
		return criteriaBuilder.truncate( x, n );
	}

	@Nonnull
	@Override
	public JpaExpression<LocalDate> localDate() {
		return criteriaBuilder.localDate();
	}

	@Nonnull
	@Override
	public JpaExpression<LocalDateTime> localDateTime() {
		return criteriaBuilder.localDateTime();
	}

	@Nonnull
	@Override
	public JpaExpression<LocalTime> localTime() {
		return criteriaBuilder.localTime();
	}

	@Nonnull
	@Override
	public <N, T extends Temporal> JpaExpression<N> extract(@Nonnull TemporalField<N, T> field, @Nonnull Expression<T> temporal) {
		return criteriaBuilder.extract( field, temporal );
	}

	@Override
	public JpaExpression<?> fk(Path<?> path) {
		return criteriaBuilder.fk( path );
	}

	@Nonnull
	@Override
	public <X, T extends X> JpaPath<T> treat(@Nonnull Path<X> path, @Nonnull Class<T> type) {
		return criteriaBuilder.treat( path, type );
	}

	@Nonnull
	@Override
	public <X, T extends X> JpaRoot<T> treat(@Nonnull Root<X> root, @Nonnull Class<T> type) {
		return criteriaBuilder.treat( root, type );
	}

	@Nonnull
	@Override
	public <X, Y, T extends Y> JpaFrom<X, T> treat(@Nonnull From<X, Y> from, @Nonnull Class<T> type) {
		return criteriaBuilder.treat( from, type );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> union(@Nonnull CriteriaQuery<? extends T> left, @Nonnull CriteriaQuery<? extends T> right) {
		return criteriaBuilder.union( left, right );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> unionAll(@Nonnull CriteriaQuery<? extends T> left, @Nonnull CriteriaQuery<? extends T> right) {
		return criteriaBuilder.unionAll( left, right );
	}

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> intersect(@Nonnull CriteriaSelect<? super T> left, @Nonnull CriteriaSelect<? super T> right) {
		return criteriaBuilder.intersect( left, right );
	}

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> intersectAll(@Nonnull CriteriaSelect<? super T> left, @Nonnull CriteriaSelect<? super T> right) {
		return criteriaBuilder.intersectAll( left, right );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> intersect(@Nonnull CriteriaQuery<? super T> left, @Nonnull CriteriaQuery<? super T> right) {
		return criteriaBuilder.intersect( left, right );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> intersectAll(@Nonnull CriteriaQuery<? super T> left, @Nonnull CriteriaQuery<? super T> right) {
		return criteriaBuilder.intersectAll( left, right );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> except(@Nonnull CriteriaQuery<T> left, @Nonnull CriteriaQuery<?> right) {
		return criteriaBuilder.except( left, right );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> exceptAll(@Nonnull CriteriaQuery<T> left, @Nonnull CriteriaQuery<?> right) {
		return criteriaBuilder.exceptAll( left, right );
	}

	@Nonnull
	@Override
	public <X, T, V extends T> JpaJoin<X, V> treat(@Nonnull Join<X, T> join, @Nonnull Class<V> type) {
		return criteriaBuilder.treat( join, type );
	}

	@Nonnull
	@Override
	public <X, T, E extends T> JpaCollectionJoin<X, E> treat(@Nonnull CollectionJoin<X, T> join, @Nonnull Class<E> type) {
		return criteriaBuilder.treat( join, type );
	}

	@Nonnull
	@Override
	public <X, T, E extends T> JpaSetJoin<X, E> treat(@Nonnull SetJoin<X, T> join, @Nonnull Class<E> type) {
		return criteriaBuilder.treat( join, type );
	}

	@Nonnull
	@Override
	public <X, T, E extends T> JpaListJoin<X, E> treat(@Nonnull ListJoin<X, T> join, @Nonnull Class<E> type) {
		return criteriaBuilder.treat( join, type );
	}

	@Nonnull
	@Override
	public <X, K, T, V extends T> JpaMapJoin<X, K, V> treat(@Nonnull MapJoin<X, K, T> join, @Nonnull Class<V> type) {
		return criteriaBuilder.treat( join, type );
	}

	@Nonnull
	@Override
	public <Y> JpaCompoundSelection<Y> construct(@Nonnull Class<Y> resultClass, @Nonnull Selection<?>... selections) {
		return criteriaBuilder.construct( resultClass, selections );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> construct(Class<Y> resultClass, List<? extends Selection<?>> arguments) {
		return criteriaBuilder.construct( resultClass, arguments );
	}

	@Nonnull
	@Override
	public JpaCompoundSelection<Tuple> tuple(@Nonnull Selection<?>... selections) {
		return criteriaBuilder.tuple( selections );
	}

	@Nonnull
	@Override
	public JpaCompoundSelection<Tuple> tuple(@Nonnull List<Selection<?>> selections) {
		return criteriaBuilder.tuple( selections );
	}

	@Nonnull
	@Override
	public JpaCompoundSelection<Object[]> array(@Nonnull Selection<?>... selections) {
		return criteriaBuilder.array( selections );
	}

	@Nonnull
	@Override
	public JpaCompoundSelection<Object[]> array(@Nonnull List<Selection<?>> selections) {
		return criteriaBuilder.array( selections );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> array(Class<Y> resultClass, Selection<?>... selections) {
		return criteriaBuilder.array( resultClass, selections );
	}

	@Override
	public <Y> JpaCompoundSelection<Y> array(Class<Y> resultClass, List<? extends Selection<?>> selections) {
		return criteriaBuilder.array( resultClass, selections );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<Double> avg(@Nonnull Expression<N> argument) {
		return criteriaBuilder.avg( argument );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> sum(@Nonnull Expression<N> argument) {
		return criteriaBuilder.sum( argument );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> sumAsLong(@Nonnull Expression<Integer> argument) {
		return criteriaBuilder.sumAsLong( argument );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> sumAsDouble(@Nonnull Expression<Float> argument) {
		return criteriaBuilder.sumAsDouble( argument );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> max(@Nonnull Expression<N> argument) {
		return criteriaBuilder.max( argument );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> min(@Nonnull Expression<N> argument) {
		return criteriaBuilder.min( argument );
	}

	@Nonnull
	@Override
	public <X extends Comparable<? super X>> JpaExpression<X> greatest(@Nonnull Expression<X> argument) {
		return criteriaBuilder.greatest( argument );
	}

	@Nonnull
	@Override
	public <X extends Comparable<? super X>> JpaExpression<X> least(@Nonnull Expression<X> argument) {
		return criteriaBuilder.least( argument );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> count(@Nonnull Expression<?> argument) {
		return criteriaBuilder.count( argument );
	}

	@Override
	public JpaExpression<Long> count() {
		return criteriaBuilder.count();
	}

	@Nonnull
	@Override
	public JpaExpression<Long> countDistinct(@Nonnull Expression<?> x) {
		return criteriaBuilder.countDistinct( x );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> neg(@Nonnull Expression<N> x) {
		return criteriaBuilder.neg( x );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> abs(@Nonnull Expression<N> x) {
		return criteriaBuilder.abs( x );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> sum(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y) {
		return criteriaBuilder.sum( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> sum(@Nonnull Expression<? extends N> x, N y) {
		return criteriaBuilder.sum( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> sum(N x, @Nonnull Expression<? extends N> y) {
		return criteriaBuilder.sum( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> prod(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y) {
		return criteriaBuilder.prod( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> prod(@Nonnull Expression<? extends N> x, N y) {
		return criteriaBuilder.prod( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> prod(N x, @Nonnull Expression<? extends N> y) {
		return criteriaBuilder.prod( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> diff(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y) {
		return criteriaBuilder.diff( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> diff(@Nonnull Expression<? extends N> x, N y) {
		return criteriaBuilder.diff( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> diff(N x, @Nonnull Expression<? extends N> y) {
		return criteriaBuilder.diff( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Number> quot(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.quot( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Number> quot(@Nonnull Expression<? extends Number> x, Number y) {
		return criteriaBuilder.quot( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Number> quot(Number x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.quot( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Integer> mod(@Nonnull Expression<Integer> x, @Nonnull Expression<Integer> y) {
		return criteriaBuilder.mod( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Integer> mod(@Nonnull Expression<Integer> x, Integer y) {
		return criteriaBuilder.mod( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Integer> mod(Integer x, @Nonnull Expression<Integer> y) {
		return criteriaBuilder.mod( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> sqrt(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.sqrt( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> toLong(@Nonnull Expression<? extends Number> number) {
		return criteriaBuilder.toLong( number );
	}

	@Nonnull
	@Override
	public JpaExpression<Integer> toInteger(@Nonnull Expression<? extends Number> number) {
		return criteriaBuilder.toInteger( number );
	}

	@Nonnull
	@Override
	public JpaExpression<Float> toFloat(@Nonnull Expression<? extends Number> number) {
		return criteriaBuilder.toFloat( number );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> toDouble(@Nonnull Expression<? extends Number> number) {
		return criteriaBuilder.toDouble( number );
	}

	@Nonnull
	@Override
	public JpaExpression<BigDecimal> toBigDecimal(@Nonnull Expression<? extends Number> number) {
		return criteriaBuilder.toBigDecimal( number );
	}

	@Nonnull
	@Override
	public JpaExpression<BigInteger> toBigInteger(@Nonnull Expression<? extends Number> number) {
		return criteriaBuilder.toBigInteger( number );
	}

	@Nonnull
	@Override
	public JpaExpression<String> toString(@Nonnull Expression<Character> character) {
		return criteriaBuilder.toString( character );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> literal(@Nonnull T value) {
		return criteriaBuilder.literal( value );
	}

	@Nonnull
	@Override
	public <N extends Number & Comparable<N>> NumericExpression<N> numericLiteral(@Nonnull N value) {
		return criteriaBuilder.numericLiteral( value );
	}

	@Nonnull
	@Override
	public TextExpression stringLiteral(@Nonnull String value) {
		return criteriaBuilder.stringLiteral( value );
	}

	@Nonnull
	@Override
	public <T extends Temporal & Comparable<? super T>> TemporalExpression<T> temporalLiteral(@Nonnull T value) {
		return criteriaBuilder.temporalLiteral( value );
	}

	@Nonnull
	@Override
	public BooleanExpression booleanLiteral(boolean value) {
		return criteriaBuilder.booleanLiteral( value );
	}

	@Override @SafeVarargs
	public final <T> List<? extends JpaExpression<T>> literals(T... values) {
		return criteriaBuilder.literals( values );
	}

	@Override
	public <T> List<? extends JpaExpression<T>> literals(List<T> values) {
		return criteriaBuilder.literals( values );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> nullLiteral(@Nonnull Class<T> resultClass) {
		return criteriaBuilder.nullLiteral( resultClass );
	}

	@Nonnull
	@Override
	public <T> JpaParameterExpression<T> parameter(@Nonnull Class<T> paramClass) {
		return criteriaBuilder.parameter( paramClass );
	}

	@Nonnull
	@Override
	public <T> JpaParameterExpression<T> parameter(@Nonnull Class<T> paramClass, @Nonnull String name) {
		return criteriaBuilder.parameter( paramClass, name );
	}

	@Nonnull
	@Override
	public <T> ParameterExpression<T> convertedParameter(@Nonnull Class<? extends AttributeConverter<T, ?>> converter) {
		return criteriaBuilder.convertedParameter( converter );
	}

	@Nonnull
	@Override
	public JpaExpression<String> concat(@Nonnull Expression<String> x, @Nonnull Expression<String> y) {
		return criteriaBuilder.concat( x, y );
	}

	@Override
	public <T> JpaParameterExpression<List<T>> listParameter(Class<T> paramClass) {
		return criteriaBuilder.listParameter( paramClass );
	}

	@Override
	public <T> JpaParameterExpression<List<T>> listParameter(Class<T> paramClass, String name) {
		return criteriaBuilder.listParameter( paramClass, name );
	}

	@Nonnull
	@Override
	public JpaExpression<String> concat(@Nonnull Expression<String> x, @Nonnull String y) {
		return criteriaBuilder.concat( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<String> concat(@Nonnull String x, @Nonnull Expression<String> y) {
		return criteriaBuilder.concat( x, y );
	}

	@Override
	public JpaExpression<String> concat(String x, String y) {
		return criteriaBuilder.concat( x, y );
	}

	@Nonnull
	@Override
	public JpaFunction<String> substring(@Nonnull Expression<String> x, @Nonnull Expression<Integer> from) {
		return criteriaBuilder.substring( x, from );
	}

	@Nonnull
	@Override
	public JpaFunction<String> substring(@Nonnull Expression<String> x, int from) {
		return criteriaBuilder.substring( x, from );
	}

	@Nonnull
	@Override
	public JpaFunction<String> substring(@Nonnull Expression<String> x, @Nonnull Expression<Integer> from, @Nonnull Expression<Integer> len) {
		return criteriaBuilder.substring( x, from, len );
	}

	@Nonnull
	@Override
	public JpaFunction<String> substring(@Nonnull Expression<String> x, int from, int len) {
		return criteriaBuilder.substring( x, from, len );
	}

	@Nonnull
	@Override
	public JpaFunction<String> trim(@Nonnull Expression<String> x) {
		return criteriaBuilder.trim( x );
	}

	@Nonnull
	@Override
	public JpaFunction<String> trim(@Nonnull Trimspec ts, @Nonnull Expression<String> x) {
		return criteriaBuilder.trim( ts, x );
	}

	@Nonnull
	@Override
	public JpaFunction<String> trim(@Nonnull Expression<Character> t, @Nonnull Expression<String> x) {
		return criteriaBuilder.trim( t, x );
	}

	@Nonnull
	@Override
	public JpaFunction<String> trim(@Nonnull Trimspec ts, @Nonnull Expression<Character> t, @Nonnull Expression<String> x) {
		return criteriaBuilder.trim( ts, t, x );
	}

	@Nonnull
	@Override
	public JpaFunction<String> trim(char t, @Nonnull Expression<String> x) {
		return criteriaBuilder.trim( t, x );
	}

	@Nonnull
	@Override
	public JpaFunction<String> trim(@Nonnull Trimspec ts, char t, @Nonnull Expression<String> x) {
		return criteriaBuilder.trim( ts, t, x );
	}

	@Nonnull
	@Override
	public JpaFunction<String> lower(@Nonnull Expression<String> x) {
		return criteriaBuilder.lower( x );
	}

	@Nonnull
	@Override
	public JpaFunction<String> upper(@Nonnull Expression<String> x) {
		return criteriaBuilder.upper( x );
	}

	@Nonnull
	@Override
	public JpaFunction<Integer> length(@Nonnull Expression<String> x) {
		return criteriaBuilder.length( x );
	}

	@Nonnull
	@Override
	public JpaFunction<Integer> locate(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern) {
		return criteriaBuilder.locate( x, pattern );
	}

	@Nonnull
	@Override
	public JpaFunction<Integer> locate(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return criteriaBuilder.locate( x, pattern );
	}

	@Nonnull
	@Override
	public JpaFunction<Integer> locate(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Integer> from) {
		return criteriaBuilder.locate( x, pattern, from );
	}

	@Nonnull
	@Override
	public JpaFunction<Integer> locate(@Nonnull Expression<String> x, @Nonnull String pattern, int from) {
		return criteriaBuilder.locate( x, pattern, from );
	}

	@Nonnull
	@Override
	public JpaFunction<Date> currentDate() {
		return criteriaBuilder.currentDate();
	}

	@Nonnull
	@Override
	public JpaFunction<Time> currentTime() {
		return criteriaBuilder.currentTime();
	}

	@Nonnull
	@Override
	public JpaFunction<Timestamp> currentTimestamp() {
		return criteriaBuilder.currentTimestamp();
	}

	@Override
	public JpaFunction<Instant> currentInstant() {
		return criteriaBuilder.currentInstant();
	}

	@Override
	public JpaExpression<?> id(Path<?> path) {
		return criteriaBuilder.id( path );
	}

	@Override
	public JpaExpression<?> version(Path<?> path) {
		return criteriaBuilder.version( path );
	}

	@Nonnull
	@Override
	public <T> JpaFunction<T> function(@Nonnull String name, @Nonnull Class<T> type, @Nonnull Expression<?>... args) {
		return criteriaBuilder.function( name, type, args );
	}

	@Nonnull
	@Override
	public <Y> JpaExpression<Y> all(@Nonnull Subquery<Y> subquery) {
		return criteriaBuilder.all( subquery );
	}

	@Nonnull
	@Override
	public <Y> JpaExpression<Y> some(@Nonnull Subquery<Y> subquery) {
		return criteriaBuilder.some( subquery );
	}

	@Nonnull
	@Override
	public <Y> JpaExpression<Y> any(@Nonnull Subquery<Y> subquery) {
		return criteriaBuilder.any( subquery );
	}

	@Override
	public <K, L extends List<?>> JpaExpression<Set<K>> indexes(L list) {
		return criteriaBuilder.indexes( list );
	}

	@Override
	public <T> JpaExpression<T> value(@Nullable T value) {
		return criteriaBuilder.value( value );
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> JpaExpression<Integer> size(@Nonnull Expression<C> collection) {
		return criteriaBuilder.size( collection );
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> JpaExpression<Integer> size(@Nonnull C collection) {
		return criteriaBuilder.size( collection );
	}

	@Nonnull
	@Override
	public <T> JpaCoalesce<T> coalesce() {
		return criteriaBuilder.coalesce();
	}

	@Nonnull
	@Override
	public <Y> JpaCoalesce<Y> coalesce(@Nonnull Expression<? extends Y> x, @Nonnull Expression<? extends Y> y) {
		return criteriaBuilder.coalesce( x, y );
	}

	@Nonnull
	@Override
	public <Y> JpaCoalesce<Y> coalesce(@Nonnull Expression<? extends Y> x, Y y) {
		return criteriaBuilder.coalesce( x, y );
	}

	@Nonnull
	@Override
	public <Y> JpaExpression<Y> nullif(@Nonnull Expression<Y> x, @Nonnull Expression<?> y) {
		return criteriaBuilder.nullif( x, y );
	}

	@Nonnull
	@Override
	public <Y> JpaExpression<Y> nullif(@Nonnull Expression<Y> x, Y y) {
		return criteriaBuilder.nullif( x, y );
	}

	@Nonnull
	@Override
	public <C, R> JpaSimpleCase<C, R> selectCase(@Nonnull Expression<? extends C> expression) {
		return criteriaBuilder.selectCase( expression );
	}

	@Nonnull
	@Override
	public <C, R> JpaSimpleCase<C, R> selectCase(@Nonnull Expression<? extends C> expression, @Nonnull Class<R> resultType) {
		return criteriaBuilder.selectCase( expression, resultType );
	}

	@Nonnull
	@Override
	public <R> JpaSearchedCase<R> selectCase() {
		return criteriaBuilder.selectCase();
	}

	@Nonnull
	@Override
	public <R> JpaSearchedCase<R> selectCase(@Nonnull Class<R> resultType) {
		return criteriaBuilder.selectCase( resultType );
	}

	@Nonnull
	@Override
	public JpaPredicate and(@Nonnull Expression<Boolean> x, @Nonnull Expression<Boolean> y) {
		return criteriaBuilder.and( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate and(@Nonnull Predicate... restrictions) {
		return criteriaBuilder.and( restrictions );
	}

	@Nonnull
	@Override
	public JpaPredicate and(@Nonnull BooleanExpression... restrictions) {
		return criteriaBuilder.and( restrictions );
	}

	@Nonnull
	@Override
	public JpaPredicate and(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		return criteriaBuilder.and( restrictions );
	}

	@Nonnull
	@Override
	public JpaPredicate or(@Nonnull Expression<Boolean> x, @Nonnull Expression<Boolean> y) {
		return criteriaBuilder.or( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate or(@Nonnull Predicate... restrictions) {
		return criteriaBuilder.or( restrictions );
	}

	@Nonnull
	@Override
	public JpaPredicate or(@Nonnull BooleanExpression... restrictions) {
		return criteriaBuilder.or( restrictions );
	}

	@Nonnull
	@Override
	public JpaPredicate or(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		return criteriaBuilder.or( restrictions );
	}

	@Nonnull
	@Override
	public JpaPredicate not(@Nonnull Expression<Boolean> restriction) {
		return criteriaBuilder.not( restriction );
	}

	@Nonnull
	@Override
	public JpaPredicate conjunction() {
		return criteriaBuilder.conjunction();
	}

	@Nonnull
	@Override
	public JpaPredicate disjunction() {
		return criteriaBuilder.disjunction();
	}

	@Nonnull
	@Override
	public JpaPredicate isTrue(@Nonnull Expression<Boolean> x) {
		return criteriaBuilder.isTrue( x );
	}

	@Nonnull
	@Override
	public JpaPredicate isFalse(@Nonnull Expression<Boolean> x) {
		return criteriaBuilder.isFalse( x );
	}

	@Nonnull
	@Override
	public JpaPredicate isNull(@Nonnull Expression<?> x) {
		return criteriaBuilder.isNull( x );
	}

	@Nonnull
	@Override
	public JpaPredicate isNotNull(@Nonnull Expression<?> x) {
		return criteriaBuilder.isNotNull( x );
	}

	@Nonnull
	@Override
	public JpaPredicate equal(@Nonnull Expression<?> x, @Nonnull Expression<?> y) {
		return criteriaBuilder.equal( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate equal(@Nonnull Expression<?> x, Object y) {
		return criteriaBuilder.equal( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate notEqual(@Nonnull Expression<?> x, @Nonnull Expression<?> y) {
		return criteriaBuilder.notEqual( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate notEqual(@Nonnull Expression<?> x, Object y) {
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

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate greaterThan(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y) {
		return criteriaBuilder.greaterThan( x, y );
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate greaterThan(@Nonnull Expression<? extends Y> x, Y y) {
		return criteriaBuilder.greaterThan( x, y );
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate greaterThanOrEqualTo(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y) {
		return criteriaBuilder.greaterThanOrEqualTo( x, y );
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate greaterThanOrEqualTo(@Nonnull Expression<? extends Y> x, Y y) {
		return criteriaBuilder.greaterThanOrEqualTo( x, y );
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate lessThan(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y) {
		return criteriaBuilder.lessThan( x, y );
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate lessThan(@Nonnull Expression<? extends Y> x, Y y) {
		return criteriaBuilder.lessThan( x, y );
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate lessThanOrEqualTo(
			@Nonnull Expression<? extends Y> x,
			@Nonnull Expression<? extends Y> y) {
		return criteriaBuilder.lessThanOrEqualTo( x, y );
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate lessThanOrEqualTo(@Nonnull Expression<? extends Y> x, Y y) {
		return criteriaBuilder.lessThanOrEqualTo( x, y );
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate between(
			@Nonnull Expression<? extends Y> value,
			@Nonnull Expression<? extends Y> lower,
			@Nonnull Expression<? extends Y> upper) {
		return criteriaBuilder.between( value, lower, upper );
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate between(@Nonnull Expression<? extends Y> value, Y lower, Y upper) {
		return criteriaBuilder.between( value, lower, upper );
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate between(
			Y value,
			@Nonnull Expression<? extends Y> lower,
			@Nonnull Expression<? extends Y> upper) {
		return criteriaBuilder.between( value, lower, upper );
	}

	@Nonnull
	@Override
	public JpaPredicate gt(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.gt( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate gt(@Nonnull Expression<? extends Number> x, Number y) {
		return criteriaBuilder.gt( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate ge(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.ge( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate ge(@Nonnull Expression<? extends Number> x, Number y) {
		return criteriaBuilder.ge( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate lt(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.lt( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate lt(@Nonnull Expression<? extends Number> x, Number y) {
		return criteriaBuilder.lt( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate le(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.le( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate le(@Nonnull Expression<? extends Number> x, Number y) {
		return criteriaBuilder.le( x, y );
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> JpaPredicate isEmpty(@Nonnull Expression<C> collection) {
		return criteriaBuilder.isEmpty( collection );
	}

	@Nonnull
	@Override
	public <C extends Collection<?>> JpaPredicate isNotEmpty(@Nonnull Expression<C> collection) {
		return criteriaBuilder.isNotEmpty( collection );
	}

	@Nonnull
	@Override
	public <E, C extends Collection<E>> JpaPredicate isMember(@Nonnull Expression<E> elem, @Nonnull Expression<C> collection) {
		return criteriaBuilder.isMember( elem, collection );
	}

	@Nonnull
	@Override
	public <E, C extends Collection<E>> JpaPredicate isMember(E elem, @Nonnull Expression<C> collection) {
		return criteriaBuilder.isMember( elem, collection );
	}

	@Nonnull
	@Override
	public <E, C extends Collection<E>> JpaPredicate isNotMember(@Nonnull Expression<E> elem, @Nonnull Expression<C> collection) {
		return criteriaBuilder.isNotMember( elem, collection );
	}

	@Nonnull
	@Override
	public <E, C extends Collection<E>> JpaPredicate isNotMember(E elem, @Nonnull Expression<C> collection) {
		return criteriaBuilder.isNotMember( elem, collection );
	}

	@Nonnull
	@Override
	public JpaPredicate like(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern) {
		return criteriaBuilder.like( x, pattern );
	}

	@Nonnull
	@Override
	public JpaPredicate like(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return criteriaBuilder.like( x, pattern );
	}

	@Nonnull
	@Override
	public JpaPredicate like(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar) {
		return criteriaBuilder.like( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate like(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar) {
		return criteriaBuilder.like( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate like(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull Expression<Character> escapeChar) {
		return criteriaBuilder.like( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate like(@Nonnull Expression<String> x, @Nonnull String pattern, char escapeChar) {
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

	@Nonnull
	@Override
	public JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern) {
		return criteriaBuilder.notLike( x, pattern );
	}

	@Nonnull
	@Override
	public JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return criteriaBuilder.notLike( x, pattern );
	}

	@Nonnull
	@Override
	public JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar) {
		return criteriaBuilder.notLike( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar) {
		return criteriaBuilder.notLike( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull Expression<Character> escapeChar) {
		return criteriaBuilder.notLike( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate notLike(@Nonnull Expression<String> x, @Nonnull String pattern, char escapeChar) {
		return criteriaBuilder.notLike( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaExpression<String> concat(@Nonnull List<Expression<String>> expressions) {
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
	public JpaPredicate likeRegexp(Expression<String> x, String pattern) {
		return criteriaBuilder.likeRegexp( x, pattern );
	}

	@Override
	public JpaPredicate ilikeRegexp(Expression<String> x, String pattern) {
		return criteriaBuilder.ilikeRegexp( x, pattern );
	}

	@Override
	public JpaPredicate notLikeRegexp(Expression<String> x, String pattern) {
		return criteriaBuilder.notLikeRegexp( x, pattern );
	}

	@Override
	public JpaPredicate notIlikeRegexp(Expression<String> x, String pattern) {
		return criteriaBuilder.notIlikeRegexp( x, pattern );
	}

	@Nonnull
	@Override
	public <T> JpaInPredicate<T> in(@Nonnull Expression<? extends T> expression) {
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

	@Nonnull
	@Override
	public JpaPredicate exists(@Nonnull Subquery<?> subquery) {
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

	@Nonnull
	@Override
	public JpaOrder asc(@Nonnull Expression<?> x) {
		return criteriaBuilder.asc( x );
	}

	@Nonnull
	@Override
	public JpaOrder desc(@Nonnull Expression<?> x) {
		return criteriaBuilder.desc( x );
	}

	@Nonnull
	@Override
	public Order asc(@Nonnull Expression<?> expression, @Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.asc( expression, nullPrecedence );
	}

	@Nonnull
	@Override
	public Order desc(@Nonnull Expression<?> expression, @Nonnull Nulls nullPrecedence) {
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
			Nulls nullPrecedence) {
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

	@Nonnull
	@Override
	public JpaFunction<String> left(@Nonnull Expression<String> x, int length) {
		return criteriaBuilder.left( x, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> left(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length) {
		return criteriaBuilder.left( x, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> right(@Nonnull Expression<String> x, int length) {
		return criteriaBuilder.right( x, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> right(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length) {
		return criteriaBuilder.right( x, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> replace(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull String replacement) {
		return criteriaBuilder.replace( x, pattern, replacement );
	}

	@Nonnull
	@Override
	public JpaFunction<String> replace(@Nonnull Expression<String> x, @Nonnull String pattern, @Nonnull Expression<String> replacement) {
		return criteriaBuilder.replace( x, pattern, replacement );
	}

	@Nonnull
	@Override
	public JpaFunction<String> replace(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull String replacement) {
		return criteriaBuilder.replace( x, pattern, replacement );
	}

	@Nonnull
	@Override
	public JpaFunction<String> replace(
			@Nonnull Expression<String> x,
			@Nonnull Expression<String> pattern,
			@Nonnull Expression<String> replacement) {
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
	public <T> JpaExpression<T> mode(Expression<T> sortExpression, SortDirection sortOrder, Nulls nullPrecedence) {
		return criteriaBuilder.mode( sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> mode(
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
		return criteriaBuilder.mode( filter, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> mode(
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
		return criteriaBuilder.mode( window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> mode(
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
		return criteriaBuilder.mode( filter, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, filter, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileCont(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, filter, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
		return criteriaBuilder.percentileDisc( argument, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
		return criteriaBuilder.percentileDisc( argument, filter, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
		return criteriaBuilder.percentileDisc( argument, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Override
	public <T> JpaExpression<T> percentileDisc(
			Expression<? extends Number> argument,
			JpaPredicate filter,
			JpaWindow window,
			Expression<T> sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
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
	public <T> JpaExpression<T[]> arrayReverse(Expression<T[]> arrayExpression) {
		return criteriaBuilder.arrayReverse( arrayExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySort(Expression<T[]> arrayExpression) {
		return criteriaBuilder.arraySort( arrayExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySort(Expression<T[]> arrayExpression, boolean descending) {
		return criteriaBuilder.arraySort( arrayExpression, descending );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySort(Expression<T[]> arrayExpression, Expression<Boolean> descendingExpression) {
		return criteriaBuilder.arraySort( arrayExpression, descendingExpression );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySort(Expression<T[]> arrayExpression, boolean descending, boolean nullsFirst) {
		return criteriaBuilder.arraySort( arrayExpression, descending, nullsFirst );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T[]> arraySort(
			Expression<T[]> arrayExpression,
			Expression<Boolean> descendingExpression,
			Expression<Boolean> nullsFirstExpression) {
		return criteriaBuilder.arraySort( arrayExpression, descendingExpression, nullsFirstExpression );
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

	@Incubating
	@Override
	public JpaExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, Expression<String> separatorExpression, Expression<String> defaultExpression) {
		return criteriaBuilder.arrayToString( arrayExpression, separatorExpression, defaultExpression );
	}

	@Incubating
	@Override
	public JpaExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, Expression<String> separatorExpression, String defaultValue) {
		return criteriaBuilder.arrayToString( arrayExpression, separatorExpression, defaultValue );
	}

	@Incubating
	@Override
	public JpaExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, String separator, Expression<String> defaultExpression) {
		return criteriaBuilder.arrayToString( arrayExpression, separator, defaultExpression );
	}

	@Incubating
	@Override
	public JpaExpression<String> arrayToString(Expression<? extends Object[]> arrayExpression, String separator, String defaultValue) {
		return criteriaBuilder.arrayToString( arrayExpression, separator, defaultValue );
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
	public <C extends Collection<?>> JpaExpression<C> collectionReverse(Expression<C> collectionExpression) {
		return criteriaBuilder.collectionReverse( collectionExpression );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionSort(Expression<C> collectionExpression) {
		return criteriaBuilder.collectionSort( collectionExpression );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionSort(
			Expression<C> collectionExpression,
			boolean descending) {
		return criteriaBuilder.collectionSort( collectionExpression, descending );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionSort(
			Expression<C> collectionExpression,
			Expression<Boolean> descendingExpression) {
		return criteriaBuilder.collectionSort( collectionExpression, descendingExpression );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionSort(
			Expression<C> collectionExpression,
			boolean descending,
			boolean nullsFirst) {
		return criteriaBuilder.collectionSort( collectionExpression, descending, nullsFirst );
	}

	@Override
	@Incubating
	public <C extends Collection<?>> JpaExpression<C> collectionSort(
			Expression<C> collectionExpression,
			Expression<Boolean> descendingExpression,
			Expression<Boolean> nullsFirstExpression) {
		return criteriaBuilder.collectionSort( collectionExpression, descendingExpression, nullsFirstExpression );
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
	public JpaExpression<String> collectionToString(
			Expression<? extends Collection<?>> collectionExpression,
			Expression<String> separatorExpression) {
		return criteriaBuilder.collectionToString( collectionExpression, separatorExpression );
	}

	@Override
	@Incubating
	public JpaExpression<String> collectionToString(
			Expression<? extends Collection<?>> collectionExpression,
			String separator) {
		return criteriaBuilder.collectionToString( collectionExpression, separator );
	}

	@Incubating
	@Override
	public JpaExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, Expression<String> separatorExpression, Expression<String> defaultExpression) {
		return criteriaBuilder.collectionToString( collectionExpression, separatorExpression, defaultExpression );
	}

	@Incubating
	@Override
	public JpaExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, Expression<String> separatorExpression, String defaultValue) {
		return criteriaBuilder.collectionToString( collectionExpression, separatorExpression, defaultValue );
	}

	@Incubating
	@Override
	public JpaExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, String separator, Expression<String> defaultExpression) {
		return criteriaBuilder.collectionToString( collectionExpression, separator, defaultExpression );
	}

	@Incubating
	@Override
	public JpaExpression<String> collectionToString(Expression<? extends Collection<?>> collectionExpression, String separator, String defaultValue) {
		return criteriaBuilder.collectionToString( collectionExpression, separator, defaultValue );
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

	@Override
	@Incubating
	public JpaJsonValueExpression<String> jsonValue(Expression<?> jsonDocument, String jsonPath) {
		return criteriaBuilder.jsonValue( jsonDocument, jsonPath );
	}

	@Override
	@Incubating
	public <T> JpaJsonValueExpression<T> jsonValue(
			Expression<?> jsonDocument,
			String jsonPath,
			Class<T> returningType) {
		return criteriaBuilder.jsonValue( jsonDocument, jsonPath, returningType );
	}

	@Override
	@Incubating
	public JpaJsonValueExpression<String> jsonValue(Expression<?> jsonDocument, Expression<String> jsonPath) {
		return criteriaBuilder.jsonValue( jsonDocument, jsonPath );
	}

	@Override
	@Incubating
	public <T> JpaJsonValueExpression<T> jsonValue(
			Expression<?> jsonDocument,
			Expression<String> jsonPath,
			Class<T> returningType) {
		return criteriaBuilder.jsonValue( jsonDocument, jsonPath, returningType );
	}

	@Override
	@Incubating
	public JpaJsonQueryExpression jsonQuery(Expression<?> jsonDocument, String jsonPath) {
		return criteriaBuilder.jsonQuery( jsonDocument, jsonPath );
	}

	@Override
	@Incubating
	public JpaJsonQueryExpression jsonQuery(Expression<?> jsonDocument, Expression<String> jsonPath) {
		return criteriaBuilder.jsonQuery( jsonDocument, jsonPath );
	}

	@Override
	@Incubating
	public JpaJsonExistsExpression jsonExists(Expression<?> jsonDocument, String jsonPath) {
		return criteriaBuilder.jsonExists( jsonDocument, jsonPath );
	}

	@Override
	@Incubating
	public JpaJsonExistsExpression jsonExists(Expression<?> jsonDocument, Expression<String> jsonPath) {
		return criteriaBuilder.jsonExists( jsonDocument, jsonPath );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonObject(Map<?, ? extends Expression<?>> keyValues) {
		return criteriaBuilder.jsonObject( keyValues );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonObjectWithNulls(Map<?, ? extends Expression<?>> keyValues) {
		return criteriaBuilder.jsonObjectWithNulls( keyValues );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonArray(Expression<?>... values) {
		return criteriaBuilder.jsonArray( values );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonArrayWithNulls(Expression<?>... values) {
		return criteriaBuilder.jsonArrayWithNulls( values );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonArrayAgg(Expression<?> value) {
		return criteriaBuilder.jsonArrayAgg( value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonArrayAggWithNulls(Expression<?> value) {
		return criteriaBuilder.jsonArrayAggWithNulls( value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonArrayAgg(Expression<?> value, JpaOrder... orderBy) {
		return criteriaBuilder.jsonArrayAgg( value, orderBy );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonArrayAgg(Expression<?> value, Predicate filter) {
		return criteriaBuilder.jsonArrayAgg( value, filter );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonArrayAgg(Expression<?> value, Predicate filter, JpaOrder... orderBy) {
		return criteriaBuilder.jsonArrayAgg( value, filter, orderBy );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonArrayAggWithNulls(Expression<?> value, JpaOrder... orderBy) {
		return criteriaBuilder.jsonArrayAggWithNulls( value, orderBy );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonArrayAggWithNulls(Expression<?> value, Predicate filter) {
		return criteriaBuilder.jsonArrayAggWithNulls( value, filter );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonArrayAggWithNulls(Expression<?> value, Predicate filter, JpaOrder... orderBy) {
		return criteriaBuilder.jsonArrayAggWithNulls( value, filter, orderBy );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonObjectAgg(Expression<?> key, Expression<?> value) {
		return criteriaBuilder.jsonObjectAgg( key, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonObjectAggWithNulls(Expression<?> key, Expression<?> value) {
		return criteriaBuilder.jsonObjectAggWithNulls( key, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonObjectAggWithUniqueKeys(Expression<?> key, Expression<?> value) {
		return criteriaBuilder.jsonObjectAggWithUniqueKeys( key, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonObjectAggWithUniqueKeysAndNulls(Expression<?> key, Expression<?> value) {
		return criteriaBuilder.jsonObjectAggWithUniqueKeysAndNulls( key, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonObjectAgg(Expression<?> key, Expression<?> value, Predicate filter) {
		return criteriaBuilder.jsonObjectAgg( key, value, filter );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonObjectAggWithNulls(Expression<?> key, Expression<?> value, Predicate filter) {
		return criteriaBuilder.jsonObjectAggWithNulls( key, value, filter );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonObjectAggWithUniqueKeys(Expression<?> key, Expression<?> value, Predicate filter) {
		return criteriaBuilder.jsonObjectAggWithUniqueKeys( key, value, filter );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonObjectAggWithUniqueKeysAndNulls(
			Expression<?> key,
			Expression<?> value,
			Predicate filter) {
		return criteriaBuilder.jsonObjectAggWithUniqueKeysAndNulls( key, value, filter );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonSet(Expression<?> jsonDocument, String jsonPath, Expression<?> value) {
		return criteriaBuilder.jsonSet( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonSet(Expression<?> jsonDocument, Expression<String> jsonPath, Expression<?> value) {
		return criteriaBuilder.jsonSet( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonSet(Expression<?> jsonDocument, String jsonPath, Object value) {
		return criteriaBuilder.jsonSet( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonSet(Expression<?> jsonDocument, Expression<String> jsonPath, Object value) {
		return criteriaBuilder.jsonSet( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonRemove(Expression<?> jsonDocument, String jsonPath) {
		return criteriaBuilder.jsonRemove( jsonDocument, jsonPath );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonRemove(Expression<?> jsonDocument, Expression<String> jsonPath) {
		return criteriaBuilder.jsonRemove( jsonDocument, jsonPath );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonInsert(Expression<?> jsonDocument, String jsonPath, Expression<?> value) {
		return criteriaBuilder.jsonInsert( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonInsert(
			Expression<?> jsonDocument,
			Expression<String> jsonPath,
			Expression<?> value) {
		return criteriaBuilder.jsonInsert( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonInsert(Expression<?> jsonDocument, String jsonPath, Object value) {
		return criteriaBuilder.jsonInsert( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonInsert(Expression<?> jsonDocument, Expression<String> jsonPath, Object value) {
		return criteriaBuilder.jsonInsert( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonReplace(Expression<?> jsonDocument, String jsonPath, Expression<?> value) {
		return criteriaBuilder.jsonReplace( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonReplace(
			Expression<?> jsonDocument,
			Expression<String> jsonPath,
			Expression<?> value) {
		return criteriaBuilder.jsonReplace( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonReplace(Expression<?> jsonDocument, String jsonPath, Object value) {
		return criteriaBuilder.jsonReplace( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonReplace(Expression<?> jsonDocument, Expression<String> jsonPath, Object value) {
		return criteriaBuilder.jsonReplace( jsonDocument, jsonPath, value );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonMergepatch(Expression<?> document, Expression<?> patch) {
		return criteriaBuilder.jsonMergepatch( document, patch );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonMergepatch(Expression<?> document, String patch) {
		return criteriaBuilder.jsonMergepatch( document, patch );
	}

	@Override
	@Incubating
	public JpaExpression<String> jsonMergepatch(String document, Expression<?> patch) {
		return criteriaBuilder.jsonMergepatch( document, patch );
	}

	@Override
	@Incubating
	public JpaXmlElementExpression xmlelement(String elementName) {
		return criteriaBuilder.xmlelement( elementName );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlcomment(String comment) {
		return criteriaBuilder.xmlcomment( comment );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlforest(Expression<?>... elements) {
		return criteriaBuilder.xmlforest( elements );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlforest(List<? extends Expression<?>> elements) {
		return criteriaBuilder.xmlforest( elements );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlconcat(Expression<?>... elements) {
		return criteriaBuilder.xmlconcat( elements );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlconcat(List<? extends Expression<?>> elements) {
		return criteriaBuilder.xmlconcat( elements );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlpi(String elementName) {
		return criteriaBuilder.xmlpi( elementName );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlpi(String elementName, Expression<String> content) {
		return criteriaBuilder.xmlpi( elementName, content );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlquery(String query, Expression<?> xmlDocument) {
		return criteriaBuilder.xmlquery( query, xmlDocument );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlquery(Expression<String> query, Expression<?> xmlDocument) {
		return criteriaBuilder.xmlquery( query, xmlDocument );
	}

	@Override
	@Incubating
	public JpaExpression<Boolean> xmlexists(String query, Expression<?> xmlDocument) {
		return criteriaBuilder.xmlexists( query, xmlDocument );
	}

	@Override
	@Incubating
	public JpaExpression<Boolean> xmlexists(Expression<String> query, Expression<?> xmlDocument) {
		return criteriaBuilder.xmlexists( query, xmlDocument );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlagg(JpaOrder order, Expression<?> argument) {
		return criteriaBuilder.xmlagg( order, argument );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlagg(JpaOrder order, JpaPredicate filter, Expression<?> argument) {
		return criteriaBuilder.xmlagg( order, filter, argument );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlagg(JpaOrder order, JpaWindow window, Expression<?> argument) {
		return criteriaBuilder.xmlagg( order, window, argument );
	}

	@Override
	@Incubating
	public JpaExpression<String> xmlagg(JpaOrder order, JpaPredicate filter, JpaWindow window, Expression<?> argument) {
		return criteriaBuilder.xmlagg( order, filter, window, argument );
	}

	@Override
	@Incubating
	public <T> JpaExpression<T> named(Expression<T> expression, String name) {
		return criteriaBuilder.named( expression, name );
	}

	@Incubating
	@Override
	public <E> JpaSetReturningFunction<E> setReturningFunction(String name, Expression<?>... args) {
		return criteriaBuilder.setReturningFunction( name, args );
	}

	@Override
	@Incubating
	public <E> JpaSetReturningFunction<E> unnestArray(Expression<E[]> array) {
		return criteriaBuilder.unnestArray( array );
	}

	@Override
	@Incubating
	public <E> JpaSetReturningFunction<E> unnestCollection(Expression<? extends Collection<E>> collection) {
		return criteriaBuilder.unnestCollection( collection );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(E start, E stop) {
		return criteriaBuilder.generateSeries( start, stop );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(E start, Expression<E> stop) {
		return criteriaBuilder.generateSeries( start, stop );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(Expression<E> start, E stop) {
		return criteriaBuilder.generateSeries( start, stop );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(Expression<E> start, Expression<E> stop) {
		return criteriaBuilder.generateSeries( start, stop );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(E start, Expression<E> stop, Expression<E> step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(Expression<E> start, E stop, Expression<E> step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(Expression<E> start, Expression<E> stop, E step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(E start, Expression<E> stop, E step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(Expression<E> start, E stop, E step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(E start, E stop, Expression<E> step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(E start, E stop, E step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(Expression<E> start, Expression<E> stop, Expression<E> step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(E start, Expression<E> stop, Expression<? extends TemporalAmount> step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(Expression<E> start, E stop, Expression<? extends TemporalAmount> step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(E start, E stop, Expression<? extends TemporalAmount> step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(Expression<E> start, Expression<E> stop, TemporalAmount step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(Expression<E> start, E stop, TemporalAmount step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(E start, Expression<E> stop, TemporalAmount step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(E start, E stop, TemporalAmount step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Incubating
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(Expression<E> start, Expression<E> stop, Expression<? extends TemporalAmount> step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Incubating
	@Override
	public JpaJsonTableFunction jsonTable(Expression<?> jsonDocument) {
		return criteriaBuilder.jsonTable( jsonDocument );
	}

	@Incubating
	@Override
	public JpaJsonTableFunction jsonTable(Expression<?> jsonDocument, String jsonPath) {
		return criteriaBuilder.jsonTable( jsonDocument, jsonPath );
	}

	@Incubating
	@Override
	public JpaJsonTableFunction jsonTable(Expression<?> jsonDocument, Expression<String> jsonPath) {
		return criteriaBuilder.jsonTable( jsonDocument, jsonPath );
	}

	@Incubating
	@Override
	public JpaXmlTableFunction xmlTable(String xpath, Expression<?> xmlDocument) {
		return criteriaBuilder.xmlTable( xpath, xmlDocument );
	}

	@Incubating
	@Override
	public JpaXmlTableFunction xmlTable(Expression<String> xpath, Expression<?> xmlDocument) {
		return criteriaBuilder.xmlTable( xpath, xmlDocument );
	}
}
