/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

	@Nonnull
	@Override
	public <X, T> JpaExpression<X> cast(@Nonnull JpaExpression<T> expression, @Nonnull Class<X> castTargetJavaType) {
		return criteriaBuilder.cast( expression, castTargetJavaType );
	}

	@Nonnull
	@Override
	public <X, T> JpaExpression<X> cast(@Nonnull JpaExpression<T> expression, @Nonnull JpaCastTarget<X> castTarget) {
		return criteriaBuilder.cast( expression, castTarget );
	}

	@Nonnull
	@Override
	public <X> JpaCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType) {
		return criteriaBuilder.castTarget( castTargetJavaType );
	}

	@Nonnull
	@Override
	public <X> JpaCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType, long length) {
		return criteriaBuilder.castTarget( castTargetJavaType, length );
	}

	@Nonnull
	@Override
	public <X> JpaCastTarget<X> castTarget(@Nonnull Class<X> castTargetJavaType, int precision, int scale) {
		return criteriaBuilder.castTarget( castTargetJavaType, precision, scale );
	}

	@Nonnull
	@Override
	public JpaPredicate wrap(@Nonnull Expression<Boolean> expression) {
		return criteriaBuilder.wrap( expression );
	}

	@Nonnull
	@Override @SafeVarargs
	public final JpaPredicate wrap(@Nonnull Expression<Boolean>... expressions) {
		return criteriaBuilder.wrap( expressions );
	}

	@Nonnull
	@Override
	public JpaPredicate wrap(@Nonnull BooleanExpression... expressions) {
		return criteriaBuilder.wrap( expressions );
	}

	@Nonnull
	@Override
	public <T extends HibernateCriteriaBuilder> T unwrap(@Nonnull Class<T> clazz) {
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

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> createQuery(@Nonnull String hql, @Nonnull Class<T> resultClass) {
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

	@Override
	public @Nonnull <T> TypedQueryReference<T> augment(
			@Nonnull TypedQueryReference<?> reference,
			@Nonnull Class<T> augmentedResultType,
			@Nonnull Consumer<CriteriaQuery<T>> augmentation) {
		return criteriaBuilder.augment( reference, augmentedResultType, augmentation );
	}

	@Nonnull
	@Override
	public StatementReference augment(
			@Nonnull StatementReference reference,
			@Nonnull Consumer<CriteriaStatement<?>> augmentation) {
		return criteriaBuilder.augment( reference, augmentation );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaInsertValues<T> createCriteriaInsertValues(@Nonnull Class<T> targetEntity) {
		return criteriaBuilder.createCriteriaInsertValues( targetEntity );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaInsertSelect<T> createCriteriaInsertSelect(@Nonnull Class<T> targetEntity) {
		return criteriaBuilder.createCriteriaInsertSelect( targetEntity );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaValues values(@Nonnull Expression<?>... expressions) {
		return criteriaBuilder.values( expressions );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaValues values(@Nonnull List<? extends Expression<?>> expressions) {
		return criteriaBuilder.values( expressions );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> unionAll(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return criteriaBuilder.unionAll( query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> union(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return criteriaBuilder.union( query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> union(boolean all, @Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return criteriaBuilder.union( all, query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> intersectAll(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return criteriaBuilder.intersectAll( query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> intersect(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return criteriaBuilder.intersect( query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> intersect(
			boolean all,
			@Nonnull CriteriaQuery<? extends T> query1,
			@Nonnull CriteriaQuery<?>... queries) {
		return criteriaBuilder.intersect( all, query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> exceptAll(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return criteriaBuilder.exceptAll( query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> except(@Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return criteriaBuilder.except( query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaCriteriaQuery<T> except(boolean all, @Nonnull CriteriaQuery<? extends T> query1, @Nonnull CriteriaQuery<?>... queries) {
		return criteriaBuilder.except( all, query1, queries );
	}

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> union(@Nonnull CriteriaSelect<? extends T> left, @Nonnull CriteriaSelect<? extends T> right) {
		return criteriaBuilder.union( left, right );
	}

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> unionAll(@Nonnull JpaSubQuery<? extends T> query1, @Nonnull JpaSubQuery<? extends T> query2) {
		return criteriaBuilder.unionAll( query1, query2 );
	}

	@Nonnull
	@Override
	public <T> CriteriaSelect<T> unionAll(@Nonnull CriteriaSelect<? extends T> left, @Nonnull CriteriaSelect<? extends T> right) {
		return criteriaBuilder.unionAll( left, right );
	}

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> union(@Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return criteriaBuilder.union( query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> union(boolean all, @Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return criteriaBuilder.union( all, query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> intersectAll(@Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return criteriaBuilder.intersectAll( query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> intersect(@Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return criteriaBuilder.intersect( query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> intersect(boolean all, @Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
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

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> exceptAll(@Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return criteriaBuilder.exceptAll( query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> except(@Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
		return criteriaBuilder.except( query1, queries );
	}

	@Nonnull
	@Override
	public <T> JpaSubQuery<T> except(boolean all, @Nonnull Subquery<? extends T> query1, @Nonnull Subquery<?>... queries) {
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
	public JpaExpression<Double> power(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
		return criteriaBuilder.power( x, y );
	}

	@Nonnull
	@Override
	public <T extends Number> JpaExpression<T> round(@Nonnull Expression<T> x, @Nonnull Integer n) {
		return criteriaBuilder.round( x, n );
	}

	@Nonnull
	@Override
	public <T extends Number> JpaExpression<T> truncate(@Nonnull Expression<T> x, @Nullable Integer n) {
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

	@Nonnull
	@Override
	public JpaExpression<?> fk(@Nonnull Path<?> path) {
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

	@Nonnull
	@Override
	public <Y> JpaCompoundSelection<Y> construct(@Nonnull Class<Y> resultClass, @Nonnull List<? extends Selection<?>> arguments) {
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

	@Nonnull
	@Override
	public <Y> JpaCompoundSelection<Y> array(@Nonnull Class<Y> resultClass, @Nonnull Selection<?>... selections) {
		return criteriaBuilder.array( resultClass, selections );
	}

	@Nonnull
	@Override
	public <Y> JpaCompoundSelection<Y> array(@Nonnull Class<Y> resultClass, @Nonnull List<? extends Selection<?>> selections) {
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

	@Nonnull
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
	public <N extends Number> JpaExpression<N> sum(@Nonnull Expression<? extends N> x, @Nullable N y) {
		return criteriaBuilder.sum( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> sum(@Nullable N x, @Nonnull Expression<? extends N> y) {
		return criteriaBuilder.sum( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> prod(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y) {
		return criteriaBuilder.prod( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> prod(@Nonnull Expression<? extends N> x, @Nullable N y) {
		return criteriaBuilder.prod( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> prod(@Nullable N x, @Nonnull Expression<? extends N> y) {
		return criteriaBuilder.prod( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> diff(@Nonnull Expression<? extends N> x, @Nonnull Expression<? extends N> y) {
		return criteriaBuilder.diff( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> diff(@Nonnull Expression<? extends N> x, @Nullable N y) {
		return criteriaBuilder.diff( x, y );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<N> diff(@Nullable N x, @Nonnull Expression<? extends N> y) {
		return criteriaBuilder.diff( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Number> quot(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.quot( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Number> quot(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
		return criteriaBuilder.quot( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Number> quot(@Nullable Number x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.quot( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Integer> mod(@Nonnull Expression<Integer> x, @Nonnull Expression<Integer> y) {
		return criteriaBuilder.mod( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Integer> mod(@Nonnull Expression<Integer> x, @Nullable Integer y) {
		return criteriaBuilder.mod( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Integer> mod(@Nullable Integer x, @Nonnull Expression<Integer> y) {
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

	@Nonnull
	@Override @SafeVarargs
	public final <T> List<? extends JpaExpression<T>> literals(@Nonnull T... values) {
		return criteriaBuilder.literals( values );
	}

	@Nonnull
	@Override
	public <T> List<? extends JpaExpression<T>> literals(@Nonnull List<T> values) {
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

	@Nonnull
	@Override
	public <T> JpaParameterExpression<List<T>> listParameter(@Nonnull Class<T> paramClass) {
		return criteriaBuilder.listParameter( paramClass );
	}

	@Nonnull
	@Override
	public <T> JpaParameterExpression<List<T>> listParameter(@Nonnull Class<T> paramClass, @Nullable String name) {
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

	@Nonnull
	@Override
	public JpaExpression<String> concat(@Nullable String x, @Nullable String y) {
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

	@Nonnull
	@Override
	public JpaFunction<Instant> currentInstant() {
		return criteriaBuilder.currentInstant();
	}

	@Nonnull
	@Override
	public JpaExpression<?> id(@Nonnull Path<?> path) {
		return criteriaBuilder.id( path );
	}

	@Nonnull
	@Override
	public JpaExpression<?> version(@Nonnull Path<?> path) {
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

	@Nonnull
	@Override
	public <K, L extends List<?>> JpaExpression<Set<K>> indexes(@Nonnull L list) {
		return criteriaBuilder.indexes( list );
	}

	@Nonnull
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
	public <Y> JpaCoalesce<Y> coalesce(@Nonnull Expression<? extends Y> x, @Nullable Y y) {
		return criteriaBuilder.coalesce( x, y );
	}

	@Nonnull
	@Override
	public <Y> JpaExpression<Y> nullif(@Nonnull Expression<Y> x, @Nonnull Expression<?> y) {
		return criteriaBuilder.nullif( x, y );
	}

	@Nonnull
	@Override
	public <Y> JpaExpression<Y> nullif(@Nonnull Expression<Y> x, @Nullable Y y) {
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
	public JpaPredicate equal(@Nonnull Expression<?> x, @Nullable Object y) {
		return criteriaBuilder.equal( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate notEqual(@Nonnull Expression<?> x, @Nonnull Expression<?> y) {
		return criteriaBuilder.notEqual( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate notEqual(@Nonnull Expression<?> x, @Nullable Object y) {
		return criteriaBuilder.notEqual( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate distinctFrom(@Nonnull Expression<?> x, @Nonnull Expression<?> y) {
		return criteriaBuilder.distinctFrom( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate distinctFrom(@Nonnull Expression<?> x, @Nullable Object y) {
		return criteriaBuilder.distinctFrom( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate notDistinctFrom(@Nonnull Expression<?> x, @Nonnull Expression<?> y) {
		return criteriaBuilder.notDistinctFrom( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate notDistinctFrom(@Nonnull Expression<?> x, @Nullable Object y) {
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
	public <Y extends Comparable<? super Y>> JpaPredicate greaterThan(@Nonnull Expression<? extends Y> x, @Nullable Y y) {
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
	public <Y extends Comparable<? super Y>> JpaPredicate greaterThanOrEqualTo(@Nonnull Expression<? extends Y> x, @Nullable Y y) {
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
	public <Y extends Comparable<? super Y>> JpaPredicate lessThan(@Nonnull Expression<? extends Y> x, @Nullable Y y) {
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
	public <Y extends Comparable<? super Y>> JpaPredicate lessThanOrEqualTo(@Nonnull Expression<? extends Y> x, @Nullable Y y) {
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
	public <Y extends Comparable<? super Y>> JpaPredicate between(@Nonnull Expression<? extends Y> value, @Nullable Y lower, @Nullable Y upper) {
		return criteriaBuilder.between( value, lower, upper );
	}

	@Nonnull
	@Override
	public <Y extends Comparable<? super Y>> JpaPredicate between(
			@Nullable Y value,
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
	public JpaPredicate gt(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
		return criteriaBuilder.gt( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate ge(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.ge( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate ge(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
		return criteriaBuilder.ge( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate lt(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.lt( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate lt(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
		return criteriaBuilder.lt( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate le(@Nonnull Expression<? extends Number> x, @Nonnull Expression<? extends Number> y) {
		return criteriaBuilder.le( x, y );
	}

	@Nonnull
	@Override
	public JpaPredicate le(@Nonnull Expression<? extends Number> x, @Nullable Number y) {
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
	public <E, C extends Collection<E>> JpaPredicate isMember(@Nullable E elem, @Nonnull Expression<C> collection) {
		return criteriaBuilder.isMember( elem, collection );
	}

	@Nonnull
	@Override
	public <E, C extends Collection<E>> JpaPredicate isNotMember(@Nonnull Expression<E> elem, @Nonnull Expression<C> collection) {
		return criteriaBuilder.isNotMember( elem, collection );
	}

	@Nonnull
	@Override
	public <E, C extends Collection<E>> JpaPredicate isNotMember(@Nullable E elem, @Nonnull Expression<C> collection) {
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

	@Nonnull
	@Override
	public JpaPredicate ilike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern) {
		return criteriaBuilder.ilike( x, pattern );
	}

	@Nonnull
	@Override
	public JpaPredicate ilike(@Nonnull Expression<String> x, @Nullable String pattern) {
		return criteriaBuilder.ilike( x, pattern );
	}

	@Nonnull
	@Override
	public JpaPredicate ilike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar) {
		return criteriaBuilder.ilike( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate ilike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar) {
		return criteriaBuilder.ilike( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate ilike(@Nonnull Expression<String> x, @Nullable String pattern, @Nonnull Expression<Character> escapeChar) {
		return criteriaBuilder.ilike( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate ilike(@Nonnull Expression<String> x, @Nullable String pattern, char escapeChar) {
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

	@Nonnull
	@Override
	public JpaPredicate notIlike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern) {
		return criteriaBuilder.notIlike( x, pattern );
	}

	@Nonnull
	@Override
	public JpaPredicate notIlike(@Nonnull Expression<String> x, @Nullable String pattern) {
		return criteriaBuilder.notIlike( x, pattern );
	}

	@Nonnull
	@Override
	public JpaPredicate notIlike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, @Nonnull Expression<Character> escapeChar) {
		return criteriaBuilder.notIlike( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate notIlike(@Nonnull Expression<String> x, @Nonnull Expression<String> pattern, char escapeChar) {
		return criteriaBuilder.notIlike( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate notIlike(@Nonnull Expression<String> x, @Nullable String pattern, @Nonnull Expression<Character> escapeChar) {
		return criteriaBuilder.notIlike( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate notIlike(@Nonnull Expression<String> x, @Nullable String pattern, char escapeChar) {
		return criteriaBuilder.notIlike( x, pattern, escapeChar );
	}

	@Nonnull
	@Override
	public JpaPredicate likeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return criteriaBuilder.likeRegexp( x, pattern );
	}

	@Nonnull
	@Override
	public JpaPredicate ilikeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return criteriaBuilder.ilikeRegexp( x, pattern );
	}

	@Nonnull
	@Override
	public JpaPredicate notLikeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return criteriaBuilder.notLikeRegexp( x, pattern );
	}

	@Nonnull
	@Override
	public JpaPredicate notIlikeRegexp(@Nonnull Expression<String> x, @Nonnull String pattern) {
		return criteriaBuilder.notIlikeRegexp( x, pattern );
	}

	@Nonnull
	@Override
	public <T> JpaInPredicate<T> in(@Nonnull Expression<? extends T> expression) {
		return criteriaBuilder.in( expression );
	}

	@Nonnull
	@Override @SafeVarargs
	public final <T> JpaInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull Expression<? extends T>... values) {
		return criteriaBuilder.in( expression, values );
	}

	@Nonnull
	@Override @SafeVarargs
	public final <T> JpaInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull T... values) {
		return criteriaBuilder.in( expression, values );
	}

	@Nonnull
	@Override
	public <T> JpaInPredicate<T> in(@Nonnull Expression<? extends T> expression, @Nonnull Collection<T> values) {
		return criteriaBuilder.in( expression, values );
	}

	@Nonnull
	@Override
	public JpaPredicate exists(@Nonnull Subquery<?> subquery) {
		return criteriaBuilder.exists( subquery );
	}

	@Nonnull
	@Override
	public <M extends Map<?, ?>> JpaPredicate isMapEmpty(@Nonnull JpaExpression<M> mapExpression) {
		return criteriaBuilder.isMapEmpty( mapExpression );
	}

	@Nonnull
	@Override
	public <M extends Map<?, ?>> JpaPredicate isMapNotEmpty(@Nonnull JpaExpression<M> mapExpression) {
		return criteriaBuilder.isMapNotEmpty( mapExpression );
	}

	@Nonnull
	@Override
	public <M extends Map<?, ?>> JpaExpression<Integer> mapSize(@Nonnull JpaExpression<M> mapExpression) {
		return criteriaBuilder.mapSize( mapExpression );
	}

	@Nonnull
	@Override
	public <M extends Map<?, ?>> JpaExpression<Integer> mapSize(@Nonnull M map) {
		return criteriaBuilder.mapSize( map );
	}

	@Nonnull
	@Override
	public JpaOrder sort(@Nonnull JpaExpression<?> sortExpression, @Nonnull SortDirection sortOrder) {
		return criteriaBuilder.sort( sortExpression, sortOrder );
	}

	@Nonnull
	@Override
	public JpaOrder sort(@Nonnull JpaExpression<?> sortExpression, @Nonnull SortDirection sortOrder, @Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.sort( sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public JpaOrder sort(
			@Nonnull JpaExpression<?> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence,
			boolean ignoreCase) {
		return criteriaBuilder.sort( sortExpression, sortOrder, nullPrecedence, ignoreCase );
	}

	@Nonnull
	@Override
	public JpaOrder sort(@Nonnull JpaExpression<?> sortExpression) {
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

	@Nonnull
	@Override
	public JpaOrder asc(@Nonnull Expression<?> x, boolean nullsFirst) {
		return criteriaBuilder.asc( x, nullsFirst );
	}

	@Nonnull
	@Override
	public JpaOrder desc(@Nonnull Expression<?> x, boolean nullsFirst) {
		return criteriaBuilder.desc( x, nullsFirst );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaSearchOrder search(
			@Nonnull JpaCteCriteriaAttribute cteAttribute,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.search( cteAttribute, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaSearchOrder search(@Nonnull JpaCteCriteriaAttribute cteAttribute, @Nonnull SortDirection sortOrder) {
		return criteriaBuilder.search( cteAttribute, sortOrder );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaSearchOrder search(@Nonnull JpaCteCriteriaAttribute cteAttribute) {
		return criteriaBuilder.search( cteAttribute );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaSearchOrder asc(@Nonnull JpaCteCriteriaAttribute x) {
		return criteriaBuilder.asc( x );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaSearchOrder desc(@Nonnull JpaCteCriteriaAttribute x) {
		return criteriaBuilder.desc( x );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaSearchOrder asc(@Nonnull JpaCteCriteriaAttribute x, boolean nullsFirst) {
		return criteriaBuilder.asc( x, nullsFirst );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaSearchOrder desc(@Nonnull JpaCteCriteriaAttribute x, boolean nullsFirst) {
		return criteriaBuilder.desc( x, nullsFirst );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> sql(@Nonnull String pattern, @Nonnull Class<T> type, @Nonnull Expression<?>... arguments) {
		return criteriaBuilder.sql( pattern, type, arguments );
	}

	@Nonnull
	@Override
	public JpaFunction<String> format(@Nonnull Expression<? extends TemporalAccessor> datetime, @Nonnull String pattern) {
		return criteriaBuilder.format( datetime, pattern );
	}

	@Nonnull
	@Override
	public JpaFunction<Integer> year(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.year( datetime );
	}

	@Nonnull
	@Override
	public JpaFunction<Integer> month(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.month( datetime );
	}

	@Nonnull
	@Override
	public JpaFunction<Integer> day(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.day( datetime );
	}

	@Nonnull
	@Override
	public JpaFunction<Integer> hour(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.hour( datetime );
	}

	@Nonnull
	@Override
	public JpaFunction<Integer> minute(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.minute( datetime );
	}

	@Nonnull
	@Override
	public JpaFunction<Float> second(@Nonnull Expression<? extends TemporalAccessor> datetime) {
		return criteriaBuilder.second( datetime );
	}

	@Nonnull
	@Override
	public <T extends TemporalAccessor> JpaFunction<T> truncate(@Nonnull Expression<T> datetime, @Nonnull TemporalUnit temporalUnit) {
		return criteriaBuilder.truncate( datetime, temporalUnit );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nullable String replacement, int start) {
		return criteriaBuilder.overlay( string, replacement, start );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nonnull Expression<String> replacement, int start) {
		return criteriaBuilder.overlay( string, replacement, start );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nullable String replacement, @Nonnull Expression<Integer> start) {
		return criteriaBuilder.overlay( string, replacement, start );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			@Nonnull Expression<Integer> start) {
		return criteriaBuilder.overlay( string, replacement, start );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(@Nonnull Expression<String> string, @Nullable String replacement, int start, int length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			int start,
			int length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nullable String replacement,
			@Nonnull Expression<Integer> start,
			int length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			@Nonnull Expression<Integer> start,
			int length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nullable String replacement,
			int start,
			@Nullable Expression<Integer> length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			int start,
			@Nullable Expression<Integer> length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nullable String replacement,
			@Nonnull Expression<Integer> start,
			@Nullable Expression<Integer> length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> overlay(
			@Nonnull Expression<String> string,
			@Nonnull Expression<String> replacement,
			@Nonnull Expression<Integer> start,
			@Nullable Expression<Integer> length) {
		return criteriaBuilder.overlay( string, replacement, start, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(@Nonnull Expression<String> x, int length) {
		return criteriaBuilder.pad( x, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, int length) {
		return criteriaBuilder.pad( ts, x, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length) {
		return criteriaBuilder.pad( x, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, @Nonnull Expression<Integer> length) {
		return criteriaBuilder.pad( ts, x, length );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(@Nonnull Expression<String> x, int length, char padChar) {
		return criteriaBuilder.pad( x, length, padChar );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, int length, char padChar) {
		return criteriaBuilder.pad( ts, x, length, padChar );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length, char padChar) {
		return criteriaBuilder.pad( x, length, padChar );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, @Nonnull Expression<Integer> length, char padChar) {
		return criteriaBuilder.pad( ts, x, length, padChar );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(@Nonnull Expression<String> x, int length, @Nullable Expression<Character> padChar) {
		return criteriaBuilder.pad( x, length, padChar );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(@Nullable Trimspec ts, @Nonnull Expression<String> x, int length, @Nullable Expression<Character> padChar) {
		return criteriaBuilder.pad( ts, x, length, padChar );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(@Nonnull Expression<String> x, @Nonnull Expression<Integer> length, @Nullable Expression<Character> padChar) {
		return criteriaBuilder.pad( x, length, padChar );
	}

	@Nonnull
	@Override
	public JpaFunction<String> pad(
			@Nullable Trimspec ts,
			@Nonnull Expression<String> x,
			@Nonnull Expression<Integer> length,
			@Nullable Expression<Character> padChar) {
		return criteriaBuilder.pad( ts, x, length, padChar );
	}

	@Nonnull
	@Override
	public JpaFunction<String> repeat(@Nonnull Expression<String> x, @Nonnull Expression<Integer> times) {
		return criteriaBuilder.repeat( x, times );
	}

	@Nonnull
	@Override
	public JpaFunction<String> repeat(@Nonnull Expression<String> x, int times) {
		return criteriaBuilder.repeat( x, times );
	}

	@Nonnull
	@Override
	public JpaFunction<String> repeat(@Nullable String x, @Nonnull Expression<Integer> times) {
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

	@Nonnull
	@Override
	public JpaFunction<String> collate(@Nonnull Expression<String> x, @Nonnull String collation) {
		return criteriaBuilder.collate( x, collation );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> log10(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.log10( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> log(@Nullable Number b, @Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.log( b, x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> log(@Nonnull Expression<? extends Number> b, @Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.log( b, x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> pi() {
		return criteriaBuilder.pi();
	}

	@Nonnull
	@Override
	public JpaExpression<Double> sin(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.sin( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> cos(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.cos( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> tan(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.tan( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> asin(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.asin( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> acos(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.acos( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> atan(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.atan( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> atan2(@Nullable Number y, @Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.atan2( y, x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> atan2(@Nonnull Expression<? extends Number> y, @Nullable Number x) {
		return criteriaBuilder.atan2( y, x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> atan2(@Nonnull Expression<? extends Number> y, @Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.atan2( y, x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> sinh(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.sinh( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> cosh(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.cosh( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> tanh(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.tanh( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> degrees(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.degrees( x );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> radians(@Nonnull Expression<? extends Number> x) {
		return criteriaBuilder.radians( x );
	}

	@Nonnull
	@Override
	public JpaWindow createWindow() {
		return criteriaBuilder.createWindow();
	}

	@Nonnull
	@Override
	public JpaWindowFrame frameUnboundedPreceding() {
		return criteriaBuilder.frameUnboundedPreceding();
	}

	@Nonnull
	@Override
	public JpaWindowFrame frameBetweenPreceding(int offset) {
		return criteriaBuilder.frameBetweenPreceding( offset );
	}

	@Nonnull
	@Override
	public JpaWindowFrame frameBetweenPreceding(@Nonnull Expression<?> offset) {
		return criteriaBuilder.frameBetweenPreceding( offset );
	}

	@Nonnull
	@Override
	public JpaWindowFrame frameCurrentRow() {
		return criteriaBuilder.frameCurrentRow();
	}

	@Nonnull
	@Override
	public JpaWindowFrame frameBetweenFollowing(int offset) {
		return criteriaBuilder.frameBetweenFollowing( offset );
	}

	@Nonnull
	@Override
	public JpaWindowFrame frameBetweenFollowing(@Nonnull Expression<?> offset) {
		return criteriaBuilder.frameBetweenFollowing( offset );
	}

	@Nonnull
	@Override
	public JpaWindowFrame frameUnboundedFollowing() {
		return criteriaBuilder.frameUnboundedFollowing();
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> windowFunction(@Nonnull String name, @Nullable Class<T> type, @Nonnull JpaWindow window, @Nonnull Expression<?>... args) {
		return criteriaBuilder.windowFunction( name, type, window, args );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> rowNumber(@Nonnull JpaWindow window) {
		return criteriaBuilder.rowNumber( window );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> firstValue(@Nonnull Expression<T> argument, @Nonnull JpaWindow window) {
		return criteriaBuilder.firstValue( argument, window );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> lastValue(@Nonnull Expression<T> argument, @Nonnull JpaWindow window) {
		return criteriaBuilder.lastValue( argument, window );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> nthValue(@Nonnull Expression<T> argument, int n, @Nonnull JpaWindow window) {
		return criteriaBuilder.nthValue( argument, n, window );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> nthValue(@Nonnull Expression<T> argument, @Nonnull Expression<Integer> n, @Nonnull JpaWindow window) {
		return criteriaBuilder.nthValue( argument, n, window );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> rank(@Nonnull JpaWindow window) {
		return criteriaBuilder.rank( window );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> denseRank(@Nonnull JpaWindow window) {
		return criteriaBuilder.denseRank( window );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> percentRank(@Nonnull JpaWindow window) {
		return criteriaBuilder.percentRank( window );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> cumeDist(@Nonnull JpaWindow window) {
		return criteriaBuilder.cumeDist( window );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> functionAggregate(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<?>... args) {
		return criteriaBuilder.functionAggregate( name, type, filter, args );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> functionAggregate(@Nonnull String name, @Nullable Class<T> type, @Nullable JpaWindow window, @Nonnull Expression<?>... args) {
		return criteriaBuilder.functionAggregate( name, type, window, args );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> functionAggregate(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... args) {
		return criteriaBuilder.functionAggregate( name, type, filter, window, args );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<Number> sum(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter) {
		return criteriaBuilder.sum( argument, filter );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<Number> sum(@Nonnull Expression<N> argument, @Nullable JpaWindow window) {
		return criteriaBuilder.sum( argument, window );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<Number> sum(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter, @Nullable JpaWindow window) {
		return criteriaBuilder.sum( argument, filter, window );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<Double> avg(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter) {
		return criteriaBuilder.avg( argument, filter );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<Double> avg(@Nonnull Expression<N> argument, @Nullable JpaWindow window) {
		return criteriaBuilder.avg( argument, window );
	}

	@Nonnull
	@Override
	public <N extends Number> JpaExpression<Double> avg(@Nonnull Expression<N> argument, @Nullable JpaPredicate filter, @Nullable JpaWindow window) {
		return criteriaBuilder.avg( argument, filter, window );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> count(@Nonnull Expression<?> argument, @Nullable JpaPredicate filter) {
		return criteriaBuilder.count( argument, filter );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> count(@Nonnull Expression<?> argument, @Nullable JpaWindow window) {
		return criteriaBuilder.count( argument, window );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> count(@Nonnull Expression<?> argument, @Nullable JpaPredicate filter, @Nullable JpaWindow window) {
		return criteriaBuilder.count( argument, filter, window );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> functionWithinGroup(@Nonnull String name, @Nullable Class<T> type, @Nullable JpaOrder order, @Nonnull Expression<?>... args) {
		return criteriaBuilder.functionWithinGroup( name, type, order, args );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> functionWithinGroup(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<?>... args) {
		return criteriaBuilder.functionWithinGroup( name, type, order, filter, args );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> functionWithinGroup(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaOrder order,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... args) {
		return criteriaBuilder.functionWithinGroup( name, type, order, window, args );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> functionWithinGroup(
			@Nonnull String name,
			@Nullable Class<T> type,
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... args) {
		return criteriaBuilder.functionWithinGroup( name, type, order, filter, window, args );
	}

	@Nonnull
	@Override
	public JpaExpression<String> listagg(@Nullable JpaOrder order, @Nonnull Expression<String> argument, @Nonnull String separator) {
		return criteriaBuilder.listagg( order, argument, separator );
	}

	@Nonnull
	@Override
	public JpaExpression<String> listagg(@Nullable JpaOrder order, @Nonnull Expression<String> argument, @Nonnull Expression<String> separator) {
		return criteriaBuilder.listagg( order, argument, separator );
	}

	@Nonnull
	@Override
	public JpaExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<String> argument,
			@Nonnull String separator) {
		return criteriaBuilder.listagg( order, filter, argument, separator );
	}

	@Nonnull
	@Override
	public JpaExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<String> argument,
			@Nonnull Expression<String> separator) {
		return criteriaBuilder.listagg( order, filter, argument, separator );
	}

	@Nonnull
	@Override
	public JpaExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaWindow window,
			@Nonnull Expression<String> argument,
			@Nonnull String separator) {
		return criteriaBuilder.listagg( order, window, argument, separator );
	}

	@Nonnull
	@Override
	public JpaExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaWindow window,
			@Nonnull Expression<String> argument,
			@Nonnull Expression<String> separator) {
		return criteriaBuilder.listagg( order, window, argument, separator );
	}

	@Nonnull
	@Override
	public JpaExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<String> argument,
			@Nonnull String separator) {
		return criteriaBuilder.listagg( order, filter, window, argument, separator );
	}

	@Nonnull
	@Override
	public JpaExpression<String> listagg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<String> argument,
			@Nonnull Expression<String> separator) {
		return criteriaBuilder.listagg( order, filter, window, argument, separator );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> mode(@Nonnull Expression<T> sortExpression, @Nonnull SortDirection sortOrder, @Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.mode( sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> mode(
			@Nullable JpaPredicate filter,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.mode( filter, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> mode(
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.mode( window, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> mode(
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.mode( filter, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, filter, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> percentileCont(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.percentileCont( argument, filter, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.percentileDisc( argument, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.percentileDisc( argument, filter, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.percentileDisc( argument, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public <T> JpaExpression<T> percentileDisc(
			@Nonnull Expression<? extends Number> argument,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<T> sortExpression,
			@Nonnull SortDirection sortOrder,
			@Nonnull Nulls nullPrecedence) {
		return criteriaBuilder.percentileDisc( argument, filter, window, sortExpression, sortOrder, nullPrecedence );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> rank(@Nullable JpaOrder order, @Nonnull Expression<?>... arguments) {
		return criteriaBuilder.rank( order, arguments );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> rank(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<?>... arguments) {
		return criteriaBuilder.rank( order, filter, arguments );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> rank(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<?>... arguments) {
		return criteriaBuilder.rank( order, window, arguments );
	}

	@Nonnull
	@Override
	public JpaExpression<Long> rank(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nullable JpaWindow window, @Nonnull Expression<?>... arguments) {
		return criteriaBuilder.rank( order, filter, window, arguments );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> percentRank(@Nullable JpaOrder order, @Nonnull Expression<?>... arguments) {
		return criteriaBuilder.percentRank( order, arguments );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> percentRank(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<?>... arguments) {
		return criteriaBuilder.percentRank( order, filter, arguments );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> percentRank(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<?>... arguments) {
		return criteriaBuilder.percentRank( order, window, arguments );
	}

	@Nonnull
	@Override
	public JpaExpression<Double> percentRank(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<?>... arguments) {
		return criteriaBuilder.percentRank( order, filter, window, arguments );
	}

	@Nonnull
	@Override
	public JpaExpression<Duration> durationSum(@Nonnull Expression<Duration> x, @Nonnull Expression<Duration> y) {
		return criteriaBuilder.durationSum( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Duration> durationSum(@Nonnull Expression<Duration> x, @Nullable Duration y) {
		return criteriaBuilder.durationSum( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Duration> durationDiff(@Nonnull Expression<Duration> x, @Nonnull Expression<Duration> y) {
		return criteriaBuilder.durationDiff( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Duration> durationDiff(@Nonnull Expression<Duration> x, @Nullable Duration y) {
		return criteriaBuilder.durationDiff( x, y );
	}

	@Nonnull
	@Override
	public JpaExpression<Duration> durationScaled(@Nonnull Expression<? extends Number> number, @Nonnull Expression<Duration> duration) {
		return criteriaBuilder.durationScaled( number, duration );
	}

	@Nonnull
	@Override
	public JpaExpression<Duration> durationScaled(@Nullable Number number, @Nonnull Expression<Duration> duration) {
		return criteriaBuilder.durationScaled( number, duration );
	}

	@Nonnull
	@Override
	public JpaExpression<Duration> durationScaled(@Nonnull Expression<? extends Number> number, @Nullable Duration duration) {
		return criteriaBuilder.durationScaled( number, duration );
	}

	@Nonnull
	@Override
	public <T extends Temporal> JpaExpression<Duration> durationBetween(@Nonnull Expression<T> x, @Nonnull Expression<T> y) {
		return criteriaBuilder.durationBetween( x, y );
	}

	@Nonnull
	@Override
	public <T extends Temporal> JpaExpression<Duration> durationBetween(@Nonnull Expression<T> x, @Nullable T y) {
		return criteriaBuilder.durationBetween( x, y );
	}

	@Nonnull
	@Override
	public <T extends Temporal> JpaExpression<T> addDuration(@Nonnull Expression<T> datetime, @Nonnull Expression<Duration> duration) {
		return criteriaBuilder.addDuration( datetime, duration );
	}

	@Nonnull
	@Override
	public <T extends Temporal> JpaExpression<T> addDuration(@Nonnull Expression<T> datetime, @Nullable Duration duration) {
		return criteriaBuilder.addDuration( datetime, duration );
	}

	@Nonnull
	@Override
	public <T extends Temporal> JpaExpression<T> addDuration(@Nullable T datetime, @Nonnull Expression<Duration> duration) {
		return criteriaBuilder.addDuration(datetime, duration);
	}

	@Nonnull
	@Override
	public <T extends Temporal> JpaExpression<T> subtractDuration(@Nonnull Expression<T> datetime, @Nonnull Expression<Duration> duration) {
		return criteriaBuilder.subtractDuration( datetime, duration );
	}

	@Nonnull
	@Override
	public <T extends Temporal> JpaExpression<T> subtractDuration(@Nonnull Expression<T> datetime, @Nullable Duration duration) {
		return criteriaBuilder.subtractDuration( datetime, duration );
	}

	@Nonnull
	@Override
	public <T extends Temporal> JpaExpression<T> subtractDuration(@Nullable T datetime, @Nonnull Expression<Duration> duration) {
		return criteriaBuilder.subtractDuration(datetime, duration);
	}

	@Nonnull
	@Override
	public JpaExpression<Long> durationByUnit(@Nonnull TemporalUnit unit, @Nonnull Expression<Duration> duration) {
		return criteriaBuilder.durationByUnit(unit, duration);
	}

	@Nonnull
	@Override
	public JpaExpression<Duration> duration(long magnitude, @Nonnull TemporalUnit unit) {
		return criteriaBuilder.duration( magnitude, unit );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nonnull Expression<? extends T> argument) {
		return criteriaBuilder.arrayAgg( order, argument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<? extends T> argument) {
		return criteriaBuilder.arrayAgg( order, filter, argument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayAgg(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<? extends T> argument) {
		return criteriaBuilder.arrayAgg( order, window, argument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayAgg(
			@Nullable JpaOrder order,
			@Nullable JpaPredicate filter,
			@Nullable JpaWindow window,
			@Nonnull Expression<? extends T> argument) {
		return criteriaBuilder.arrayAgg( order, filter, window, argument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayLiteral(@Nullable T... elements) {
		return criteriaBuilder.arrayLiteral( elements );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<Integer> arrayLength(@Nonnull Expression<T[]> arrayExpression) {
		return criteriaBuilder.arrayLength( arrayExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<Integer> arrayPosition(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return criteriaBuilder.arrayPosition( arrayExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<Integer> arrayPosition(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.arrayPosition( arrayExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<int[]> arrayPositions(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.arrayPositions( arrayExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<int[]> arrayPositions(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return criteriaBuilder.arrayPositions( arrayExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<List<Integer>> arrayPositionsList(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.arrayPositionsList( arrayExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<List<Integer>> arrayPositionsList(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return criteriaBuilder.arrayPositionsList( arrayExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayConcat(@Nonnull Expression<T[]> arrayExpression1, @Nonnull Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayConcat( arrayExpression1, arrayExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayConcat(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2) {
		return criteriaBuilder.arrayConcat( arrayExpression1, array2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayConcat(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayConcat( array1, arrayExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayAppend(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.arrayAppend( arrayExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayAppend(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return criteriaBuilder.arrayAppend( arrayExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayPrepend(@Nonnull Expression<T> elementExpression, @Nonnull Expression<T[]> arrayExpression) {
		return criteriaBuilder.arrayPrepend( elementExpression, arrayExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayPrepend(@Nullable T element, @Nonnull Expression<T[]> arrayExpression) {
		return criteriaBuilder.arrayPrepend( element, arrayExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T> arrayGet(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Integer> indexExpression) {
		return criteriaBuilder.arrayGet( arrayExpression, indexExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T> arrayGet(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index) {
		return criteriaBuilder.arrayGet( arrayExpression, index );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySet(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> indexExpression,
			@Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.arraySet( arrayExpression, indexExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySet(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> indexExpression,
			@Nullable T element) {
		return criteriaBuilder.arraySet( arrayExpression, indexExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySet(
			@Nonnull Expression<T[]> arrayExpression,
			@Nullable Integer index,
			@Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.arraySet( arrayExpression, index, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySet(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index, @Nullable T element) {
		return criteriaBuilder.arraySet( arrayExpression, index, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayRemove(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.arrayRemove( arrayExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayRemove(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return criteriaBuilder.arrayRemove( arrayExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayRemoveIndex(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> indexExpression) {
		return criteriaBuilder.arrayRemoveIndex( arrayExpression, indexExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayRemoveIndex(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer index) {
		return criteriaBuilder.arrayRemoveIndex( arrayExpression, index );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySlice(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> lowerIndexExpression,
			@Nonnull Expression<Integer> upperIndexExpression) {
		return criteriaBuilder.arraySlice( arrayExpression, lowerIndexExpression, upperIndexExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySlice(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> lowerIndexExpression,
			@Nullable Integer upperIndex) {
		return criteriaBuilder.arraySlice( arrayExpression, lowerIndexExpression, upperIndex );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySlice(
			@Nonnull Expression<T[]> arrayExpression,
			@Nullable Integer lowerIndex,
			@Nonnull Expression<Integer> upperIndexExpression) {
		return criteriaBuilder.arraySlice( arrayExpression, lowerIndex, upperIndexExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySlice(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer lowerIndex, @Nullable Integer upperIndex) {
		return criteriaBuilder.arraySlice( arrayExpression, lowerIndex, upperIndex );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayReplace(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T> oldElementExpression,
			@Nonnull Expression<T> newElementExpression) {
		return criteriaBuilder.arrayReplace( arrayExpression, oldElementExpression, newElementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayReplace(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T> oldElementExpression,
			@Nullable T newElement) {
		return criteriaBuilder.arrayReplace( arrayExpression, oldElementExpression, newElement );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayReplace(
			@Nonnull Expression<T[]> arrayExpression,
			@Nullable T oldElement,
			@Nonnull Expression<T> newElementExpression) {
		return criteriaBuilder.arrayReplace( arrayExpression, oldElement, newElementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayReplace(@Nonnull Expression<T[]> arrayExpression, @Nullable T oldElement, @Nullable T newElement) {
		return criteriaBuilder.arrayReplace( arrayExpression, oldElement, newElement );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayTrim(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Integer> elementCountExpression) {
		return criteriaBuilder.arrayTrim( arrayExpression, elementCountExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayTrim(@Nonnull Expression<T[]> arrayExpression, @Nullable Integer elementCount) {
		return criteriaBuilder.arrayTrim( arrayExpression, elementCount );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayReverse(@Nonnull Expression<T[]> arrayExpression) {
		return criteriaBuilder.arrayReverse( arrayExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression) {
		return criteriaBuilder.arraySort( arrayExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, boolean descending) {
		return criteriaBuilder.arraySort( arrayExpression, descending );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<Boolean> descendingExpression) {
		return criteriaBuilder.arraySort( arrayExpression, descendingExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySort(@Nonnull Expression<T[]> arrayExpression, boolean descending, boolean nullsFirst) {
		return criteriaBuilder.arraySort( arrayExpression, descending, nullsFirst );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arraySort(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<Boolean> descendingExpression,
			@Nonnull Expression<Boolean> nullsFirstExpression) {
		return criteriaBuilder.arraySort( arrayExpression, descendingExpression, nullsFirstExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayFill(
			@Nonnull Expression<T> elementExpression,
			@Nonnull Expression<Integer> elementCountExpression) {
		return criteriaBuilder.arrayFill( elementExpression, elementCountExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayFill(@Nonnull Expression<T> elementExpression, @Nullable Integer elementCount) {
		return criteriaBuilder.arrayFill( elementExpression, elementCount );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayFill(@Nullable T element, @Nonnull Expression<Integer> elementCountExpression) {
		return criteriaBuilder.arrayFill( element, elementCountExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T[]> arrayFill(@Nullable T element, @Nullable Integer elementCount) {
		return criteriaBuilder.arrayFill( element, elementCount );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> arrayToString(
			@Nonnull Expression<? extends Object[]> arrayExpression,
			@Nonnull Expression<String> separatorExpression) {
		return criteriaBuilder.arrayToString( arrayExpression, separatorExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nullable String separator) {
		return criteriaBuilder.arrayToString( arrayExpression, separator );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nonnull Expression<String> separatorExpression, @Nonnull Expression<String> defaultExpression) {
		return criteriaBuilder.arrayToString( arrayExpression, separatorExpression, defaultExpression );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nonnull Expression<String> separatorExpression, @Nullable String defaultValue) {
		return criteriaBuilder.arrayToString( arrayExpression, separatorExpression, defaultValue );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nullable String separator, @Nonnull Expression<String> defaultExpression) {
		return criteriaBuilder.arrayToString( arrayExpression, separator, defaultExpression );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaExpression<String> arrayToString(@Nonnull Expression<? extends Object[]> arrayExpression, @Nullable String separator, @Nullable String defaultValue) {
		return criteriaBuilder.arrayToString( arrayExpression, separator, defaultValue );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayContains(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.arrayContains( arrayExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayContains(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return criteriaBuilder.arrayContains( arrayExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayContains(@Nullable T[] array, @Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.arrayContains( array, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayContainsNullable(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.arrayContainsNullable( arrayExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayContainsNullable(@Nonnull Expression<T[]> arrayExpression, @Nullable T element) {
		return criteriaBuilder.arrayContainsNullable( arrayExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayContainsNullable(@Nullable T[] array, @Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.arrayContainsNullable( array, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIncludes(@Nonnull Expression<T[]> arrayExpression, @Nonnull Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayIncludes( arrayExpression, subArrayExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIncludes(@Nonnull Expression<T[]> arrayExpression, @Nullable T[] subArray) {
		return criteriaBuilder.arrayIncludes( arrayExpression, subArray );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIncludes(@Nullable T[] array, @Nonnull Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayIncludes( array, subArrayExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIncludesNullable(
			@Nonnull Expression<T[]> arrayExpression,
			@Nonnull Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayIncludesNullable( arrayExpression, subArrayExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIncludesNullable(@Nonnull Expression<T[]> arrayExpression, @Nullable T[] subArray) {
		return criteriaBuilder.arrayIncludesNullable( arrayExpression, subArray );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIncludesNullable(@Nullable T[] array, @Nonnull Expression<T[]> subArrayExpression) {
		return criteriaBuilder.arrayIncludesNullable( array, subArrayExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIntersects(@Nonnull Expression<T[]> arrayExpression1, @Nonnull Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayIntersects( arrayExpression1, arrayExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIntersects(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2) {
		return criteriaBuilder.arrayIntersects( arrayExpression1, array2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIntersects(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayIntersects( array1, arrayExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIntersectsNullable(@Nonnull Expression<T[]> arrayExpression1, @Nonnull Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayIntersectsNullable( arrayExpression1, arrayExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIntersectsNullable(@Nonnull Expression<T[]> arrayExpression1, @Nullable T[] array2) {
		return criteriaBuilder.arrayIntersectsNullable( arrayExpression1, array2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaPredicate arrayIntersectsNullable(@Nullable T[] array1, @Nonnull Expression<T[]> arrayExpression2) {
		return criteriaBuilder.arrayIntersectsNullable( array1, arrayExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<E>> JpaExpression<C> collectionLiteral(@Nullable E... elements) {
		return criteriaBuilder.collectionLiteral( elements );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<Integer> collectionLength(@Nonnull Expression<? extends Collection<?>> collectionExpression) {
		return criteriaBuilder.collectionLength( collectionExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaExpression<Integer> collectionPosition(
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression,
			@Nullable E element) {
		return criteriaBuilder.collectionPosition( collectionExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaExpression<Integer> collectionPosition(
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression,
			@Nonnull Expression<E> elementExpression) {
		return criteriaBuilder.collectionPosition( collectionExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<int[]> collectionPositions(
			@Nonnull Expression<? extends Collection<? super T>> collectionExpression,
			@Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.collectionPositions( collectionExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<int[]> collectionPositions(
			@Nonnull Expression<? extends Collection<? super T>> collectionExpression,
			@Nullable T element) {
		return criteriaBuilder.collectionPositions( collectionExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<List<Integer>> collectionPositionsList(
			@Nonnull Expression<? extends Collection<? super T>> collectionExpression,
			@Nonnull Expression<T> elementExpression) {
		return criteriaBuilder.collectionPositionsList( collectionExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<List<Integer>> collectionPositionsList(
			@Nonnull Expression<? extends Collection<? super T>> collectionExpression,
			@Nullable T element) {
		return criteriaBuilder.collectionPositionsList( collectionExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(
			@Nonnull Expression<C> collectionExpression1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionConcat( collectionExpression1, collectionExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(
			@Nonnull Expression<C> collectionExpression1,
			@Nullable Collection<? extends E> collection2) {
		return criteriaBuilder.collectionConcat( collectionExpression1, collection2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionConcat(
			@Nullable C collection1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionConcat( collection1, collectionExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionAppend(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionAppend( collectionExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionAppend(
			@Nonnull Expression<C> collectionExpression,
			@Nullable E element) {
		return criteriaBuilder.collectionAppend( collectionExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionPrepend(
			@Nonnull Expression<? extends E> elementExpression,
			@Nonnull Expression<C> collectionExpression) {
		return criteriaBuilder.collectionPrepend( elementExpression, collectionExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionPrepend(
			@Nullable E element,
			@Nonnull Expression<C> collectionExpression) {
		return criteriaBuilder.collectionPrepend( element, collectionExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaExpression<E> collectionGet(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nonnull Expression<Integer> indexExpression) {
		return criteriaBuilder.collectionGet( collectionExpression, indexExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaExpression<E> collectionGet(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable Integer index) {
		return criteriaBuilder.collectionGet( collectionExpression, index );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionSet(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Integer> indexExpression,
			@Nonnull Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionSet( collectionExpression, indexExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionSet(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Integer> indexExpression,
			@Nullable E element) {
		return criteriaBuilder.collectionSet( collectionExpression, indexExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionSet(
			@Nonnull Expression<C> collectionExpression,
			@Nullable Integer index,
			@Nonnull Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionSet( collectionExpression, index, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionSet(
			@Nonnull Expression<C> collectionExpression,
			@Nullable Integer index,
			@Nullable E element) {
		return criteriaBuilder.collectionSet( collectionExpression, index, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionRemove(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionRemove( collectionExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionRemove(
			@Nonnull Expression<C> collectionExpression,
			@Nullable E element) {
		return criteriaBuilder.collectionRemove( collectionExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionRemoveIndex(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Integer> indexExpression) {
		return criteriaBuilder.collectionRemoveIndex( collectionExpression, indexExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionRemoveIndex(
			@Nonnull Expression<C> collectionExpression,
			@Nullable Integer index) {
		return criteriaBuilder.collectionRemoveIndex( collectionExpression, index );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionSlice(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Integer> lowerIndexExpression,
			@Nonnull Expression<Integer> upperIndexExpression) {
		return criteriaBuilder.collectionSlice( collectionExpression, lowerIndexExpression, upperIndexExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionSlice(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Integer> lowerIndexExpression,
			@Nullable Integer upperIndex) {
		return criteriaBuilder.collectionSlice( collectionExpression, lowerIndexExpression, upperIndex );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionSlice(
			@Nonnull Expression<C> collectionExpression,
			@Nullable Integer lowerIndex,
			@Nonnull Expression<Integer> upperIndexExpression) {
		return criteriaBuilder.collectionSlice( collectionExpression, lowerIndex, upperIndexExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionSlice(
			@Nonnull Expression<C> collectionExpression,
			@Nullable Integer lowerIndex,
			@Nullable Integer upperIndex) {
		return criteriaBuilder.collectionSlice( collectionExpression, lowerIndex, upperIndex );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<? extends E> oldElementExpression,
			@Nonnull Expression<? extends E> newElementExpression) {
		return criteriaBuilder.collectionReplace( collectionExpression, oldElementExpression, newElementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<? extends E> oldElementExpression,
			@Nullable E newElement) {
		return criteriaBuilder.collectionReplace( collectionExpression, oldElementExpression, newElement );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(
			@Nonnull Expression<C> collectionExpression,
			@Nullable E oldElement,
			@Nonnull Expression<? extends E> newElementExpression) {
		return criteriaBuilder.collectionReplace( collectionExpression, oldElement, newElementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E, C extends Collection<? super E>> JpaExpression<C> collectionReplace(
			@Nonnull Expression<C> collectionExpression,
			@Nullable E oldElement,
			@Nullable E newElement) {
		return criteriaBuilder.collectionReplace( collectionExpression, oldElement, newElement );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionTrim(
			@Nonnull Expression<C> arrayExpression,
			@Nonnull Expression<Integer> elementCountExpression) {
		return criteriaBuilder.collectionTrim( arrayExpression, elementCountExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionTrim(
			@Nonnull Expression<C> arrayExpression,
			@Nullable Integer elementCount) {
		return criteriaBuilder.collectionTrim( arrayExpression, elementCount );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionReverse(@Nonnull Expression<C> collectionExpression) {
		return criteriaBuilder.collectionReverse( collectionExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionSort(@Nonnull Expression<C> collectionExpression) {
		return criteriaBuilder.collectionSort( collectionExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			boolean descending) {
		return criteriaBuilder.collectionSort( collectionExpression, descending );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Boolean> descendingExpression) {
		return criteriaBuilder.collectionSort( collectionExpression, descendingExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			boolean descending,
			boolean nullsFirst) {
		return criteriaBuilder.collectionSort( collectionExpression, descending, nullsFirst );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <C extends Collection<?>> JpaExpression<C> collectionSort(
			@Nonnull Expression<C> collectionExpression,
			@Nonnull Expression<Boolean> descendingExpression,
			@Nonnull Expression<Boolean> nullsFirstExpression) {
		return criteriaBuilder.collectionSort( collectionExpression, descendingExpression, nullsFirstExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<Collection<T>> collectionFill(
			@Nonnull Expression<T> elementExpression,
			@Nonnull Expression<Integer> elementCountExpression) {
		return criteriaBuilder.collectionFill( elementExpression, elementCountExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<Collection<T>> collectionFill(@Nonnull Expression<T> elementExpression, @Nullable Integer elementCount) {
		return criteriaBuilder.collectionFill( elementExpression, elementCount );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<Collection<T>> collectionFill(@Nullable T element, @Nonnull Expression<Integer> elementCountExpression) {
		return criteriaBuilder.collectionFill( element, elementCountExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<Collection<T>> collectionFill(@Nullable T element, @Nullable Integer elementCount) {
		return criteriaBuilder.collectionFill( element, elementCount );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> collectionToString(
			@Nonnull Expression<? extends Collection<?>> collectionExpression,
			@Nonnull Expression<String> separatorExpression) {
		return criteriaBuilder.collectionToString( collectionExpression, separatorExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> collectionToString(
			@Nonnull Expression<? extends Collection<?>> collectionExpression,
			@Nullable String separator) {
		return criteriaBuilder.collectionToString( collectionExpression, separator );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nonnull Expression<String> separatorExpression, @Nonnull Expression<String> defaultExpression) {
		return criteriaBuilder.collectionToString( collectionExpression, separatorExpression, defaultExpression );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nonnull Expression<String> separatorExpression, @Nullable String defaultValue) {
		return criteriaBuilder.collectionToString( collectionExpression, separatorExpression, defaultValue );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nullable String separator, @Nonnull Expression<String> defaultExpression) {
		return criteriaBuilder.collectionToString( collectionExpression, separator, defaultExpression );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaExpression<String> collectionToString(@Nonnull Expression<? extends Collection<?>> collectionExpression, @Nullable String separator, @Nullable String defaultValue) {
		return criteriaBuilder.collectionToString( collectionExpression, separator, defaultValue );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionContains(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nonnull Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionContains( collectionExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionContains(@Nonnull Expression<? extends Collection<E>> collectionExpression, @Nullable E element) {
		return criteriaBuilder.collectionContains( collectionExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionContains(@Nonnull Collection<E> collection, @Nonnull Expression<E> elementExpression) {
		return criteriaBuilder.collectionContains( collection, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionContainsNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nonnull Expression<? extends E> elementExpression) {
		return criteriaBuilder.collectionContainsNullable( collectionExpression, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionContainsNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nullable E element) {
		return criteriaBuilder.collectionContainsNullable( collectionExpression, element );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionContainsNullable(@Nonnull Collection<E> collection, @Nonnull Expression<E> elementExpression) {
		return criteriaBuilder.collectionContainsNullable( collection, elementExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIncludes(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return criteriaBuilder.collectionIncludes( collectionExpression, subCollectionExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIncludes(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nullable Collection<? extends E> subCollection) {
		return criteriaBuilder.collectionIncludes( collectionExpression, subCollection );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIncludes(
			@Nullable Collection<E> collection,
			@Nonnull Expression<? extends Collection<? extends E>> subArrayExpression) {
		return criteriaBuilder.collectionIncludes( collection, subArrayExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIncludesNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return criteriaBuilder.collectionIncludesNullable( collectionExpression, subCollectionExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIncludesNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression,
			@Nullable Collection<? extends E> subCollection) {
		return criteriaBuilder.collectionIncludesNullable( collectionExpression, subCollection );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIncludesNullable(
			@Nullable Collection<E> collection,
			@Nonnull Expression<? extends Collection<? extends E>> subCollectionExpression) {
		return criteriaBuilder.collectionIncludesNullable( collection, subCollectionExpression );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIntersects(
			@Nonnull Expression<? extends Collection<E>> collectionExpression1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionIntersects( collectionExpression1, collectionExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIntersects(
			@Nonnull Expression<? extends Collection<E>> collectionExpression1,
			@Nullable Collection<? extends E> collection2) {
		return criteriaBuilder.collectionIntersects( collectionExpression1, collection2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIntersects(
			@Nullable Collection<E> collection1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionIntersects( collection1, collectionExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIntersectsNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionIntersectsNullable( collectionExpression1, collectionExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIntersectsNullable(
			@Nonnull Expression<? extends Collection<E>> collectionExpression1,
			@Nullable Collection<? extends E> collection2) {
		return criteriaBuilder.collectionIntersectsNullable( collectionExpression1, collection2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaPredicate collectionIntersectsNullable(
			@Nullable Collection<E> collection1,
			@Nonnull Expression<? extends Collection<? extends E>> collectionExpression2) {
		return criteriaBuilder.collectionIntersectsNullable( collection1, collectionExpression2 );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaJsonValueExpression<String> jsonValue(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath) {
		return criteriaBuilder.jsonValue( jsonDocument, jsonPath );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaJsonValueExpression<T> jsonValue(
			@Nonnull Expression<?> jsonDocument,
			@Nullable String jsonPath,
			@Nullable Class<T> returningType) {
		return criteriaBuilder.jsonValue( jsonDocument, jsonPath, returningType );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaJsonValueExpression<String> jsonValue(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath) {
		return criteriaBuilder.jsonValue( jsonDocument, jsonPath );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaJsonValueExpression<T> jsonValue(
			@Nonnull Expression<?> jsonDocument,
			@Nonnull Expression<String> jsonPath,
			@Nullable Class<T> returningType) {
		return criteriaBuilder.jsonValue( jsonDocument, jsonPath, returningType );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaJsonQueryExpression jsonQuery(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath) {
		return criteriaBuilder.jsonQuery( jsonDocument, jsonPath );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaJsonQueryExpression jsonQuery(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath) {
		return criteriaBuilder.jsonQuery( jsonDocument, jsonPath );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaJsonExistsExpression jsonExists(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath) {
		return criteriaBuilder.jsonExists( jsonDocument, jsonPath );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaJsonExistsExpression jsonExists(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath) {
		return criteriaBuilder.jsonExists( jsonDocument, jsonPath );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonObject(@Nonnull Map<?, ? extends Expression<?>> keyValues) {
		return criteriaBuilder.jsonObject( keyValues );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonObjectWithNulls(@Nonnull Map<?, ? extends Expression<?>> keyValues) {
		return criteriaBuilder.jsonObjectWithNulls( keyValues );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonArray(@Nonnull Expression<?>... values) {
		return criteriaBuilder.jsonArray( values );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonArrayWithNulls(@Nonnull Expression<?>... values) {
		return criteriaBuilder.jsonArrayWithNulls( values );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonArrayAgg(@Nonnull Expression<?> value) {
		return criteriaBuilder.jsonArrayAgg( value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value) {
		return criteriaBuilder.jsonArrayAggWithNulls( value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nonnull JpaOrder... orderBy) {
		return criteriaBuilder.jsonArrayAgg( value, orderBy );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nullable Predicate filter) {
		return criteriaBuilder.jsonArrayAgg( value, filter );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonArrayAgg(@Nonnull Expression<?> value, @Nullable Predicate filter, @Nonnull JpaOrder... orderBy) {
		return criteriaBuilder.jsonArrayAgg( value, filter, orderBy );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nonnull JpaOrder... orderBy) {
		return criteriaBuilder.jsonArrayAggWithNulls( value, orderBy );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nullable Predicate filter) {
		return criteriaBuilder.jsonArrayAggWithNulls( value, filter );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonArrayAggWithNulls(@Nonnull Expression<?> value, @Nullable Predicate filter, @Nonnull JpaOrder... orderBy) {
		return criteriaBuilder.jsonArrayAggWithNulls( value, filter, orderBy );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonObjectAgg(@Nonnull Expression<?> key, @Nonnull Expression<?> value) {
		return criteriaBuilder.jsonObjectAgg( key, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonObjectAggWithNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value) {
		return criteriaBuilder.jsonObjectAggWithNulls( key, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonObjectAggWithUniqueKeys(@Nonnull Expression<?> key, @Nonnull Expression<?> value) {
		return criteriaBuilder.jsonObjectAggWithUniqueKeys( key, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonObjectAggWithUniqueKeysAndNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value) {
		return criteriaBuilder.jsonObjectAggWithUniqueKeysAndNulls( key, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonObjectAgg(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter) {
		return criteriaBuilder.jsonObjectAgg( key, value, filter );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonObjectAggWithNulls(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter) {
		return criteriaBuilder.jsonObjectAggWithNulls( key, value, filter );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonObjectAggWithUniqueKeys(@Nonnull Expression<?> key, @Nonnull Expression<?> value, @Nullable Predicate filter) {
		return criteriaBuilder.jsonObjectAggWithUniqueKeys( key, value, filter );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonObjectAggWithUniqueKeysAndNulls(
			@Nonnull Expression<?> key,
			@Nonnull Expression<?> value,
			@Nullable Predicate filter) {
		return criteriaBuilder.jsonObjectAggWithUniqueKeysAndNulls( key, value, filter );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value) {
		return criteriaBuilder.jsonSet( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nonnull Expression<?> value) {
		return criteriaBuilder.jsonSet( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value) {
		return criteriaBuilder.jsonSet( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonSet(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value) {
		return criteriaBuilder.jsonSet( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonRemove(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath) {
		return criteriaBuilder.jsonRemove( jsonDocument, jsonPath );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonRemove(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath) {
		return criteriaBuilder.jsonRemove( jsonDocument, jsonPath );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value) {
		return criteriaBuilder.jsonInsert( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonInsert(
			@Nonnull Expression<?> jsonDocument,
			@Nonnull Expression<String> jsonPath,
			@Nonnull Expression<?> value) {
		return criteriaBuilder.jsonInsert( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value) {
		return criteriaBuilder.jsonInsert( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonInsert(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value) {
		return criteriaBuilder.jsonInsert( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nonnull Expression<?> value) {
		return criteriaBuilder.jsonReplace( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonReplace(
			@Nonnull Expression<?> jsonDocument,
			@Nonnull Expression<String> jsonPath,
			@Nonnull Expression<?> value) {
		return criteriaBuilder.jsonReplace( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath, @Nullable Object value) {
		return criteriaBuilder.jsonReplace( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonReplace(@Nonnull Expression<?> jsonDocument, @Nonnull Expression<String> jsonPath, @Nullable Object value) {
		return criteriaBuilder.jsonReplace( jsonDocument, jsonPath, value );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonMergepatch(@Nonnull Expression<?> document, @Nonnull Expression<?> patch) {
		return criteriaBuilder.jsonMergepatch( document, patch );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonMergepatch(@Nonnull Expression<?> document, @Nullable String patch) {
		return criteriaBuilder.jsonMergepatch( document, patch );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> jsonMergepatch(@Nullable String document, @Nonnull Expression<?> patch) {
		return criteriaBuilder.jsonMergepatch( document, patch );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaXmlElementExpression xmlelement(@Nonnull String elementName) {
		return criteriaBuilder.xmlelement( elementName );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlcomment(@Nullable String comment) {
		return criteriaBuilder.xmlcomment( comment );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlforest(@Nonnull Expression<?>... elements) {
		return criteriaBuilder.xmlforest( elements );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlforest(@Nonnull List<? extends Expression<?>> elements) {
		return criteriaBuilder.xmlforest( elements );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlconcat(@Nonnull Expression<?>... elements) {
		return criteriaBuilder.xmlconcat( elements );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlconcat(@Nonnull List<? extends Expression<?>> elements) {
		return criteriaBuilder.xmlconcat( elements );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlpi(@Nonnull String elementName) {
		return criteriaBuilder.xmlpi( elementName );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlpi(@Nonnull String elementName, @Nonnull Expression<String> content) {
		return criteriaBuilder.xmlpi( elementName, content );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlquery(@Nullable String query, @Nonnull Expression<?> xmlDocument) {
		return criteriaBuilder.xmlquery( query, xmlDocument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlquery(@Nonnull Expression<String> query, @Nonnull Expression<?> xmlDocument) {
		return criteriaBuilder.xmlquery( query, xmlDocument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<Boolean> xmlexists(@Nullable String query, @Nonnull Expression<?> xmlDocument) {
		return criteriaBuilder.xmlexists( query, xmlDocument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<Boolean> xmlexists(@Nonnull Expression<String> query, @Nonnull Expression<?> xmlDocument) {
		return criteriaBuilder.xmlexists( query, xmlDocument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlagg(@Nullable JpaOrder order, @Nonnull Expression<?> argument) {
		return criteriaBuilder.xmlagg( order, argument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlagg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nonnull Expression<?> argument) {
		return criteriaBuilder.xmlagg( order, filter, argument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlagg(@Nullable JpaOrder order, @Nullable JpaWindow window, @Nonnull Expression<?> argument) {
		return criteriaBuilder.xmlagg( order, window, argument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public JpaExpression<String> xmlagg(@Nullable JpaOrder order, @Nullable JpaPredicate filter, @Nullable JpaWindow window, @Nonnull Expression<?> argument) {
		return criteriaBuilder.xmlagg( order, filter, window, argument );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <T> JpaExpression<T> named(@Nonnull Expression<T> expression, @Nonnull String name) {
		return criteriaBuilder.named( expression, name );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E> JpaSetReturningFunction<E> setReturningFunction(@Nonnull String name, @Nonnull Expression<?>... args) {
		return criteriaBuilder.setReturningFunction( name, args );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaSetReturningFunction<E> unnestArray(@Nonnull Expression<E[]> array) {
		return criteriaBuilder.unnestArray( array );
	}

	@Nonnull
	@Override
	@Incubating(since = "6.3")
	public <E> JpaSetReturningFunction<E> unnestCollection(@Nonnull Expression<? extends Collection<E>> collection) {
		return criteriaBuilder.unnestCollection( collection );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop) {
		return criteriaBuilder.generateSeries( start, stop );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop) {
		return criteriaBuilder.generateSeries( start, stop );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop) {
		return criteriaBuilder.generateSeries( start, stop );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop) {
		return criteriaBuilder.generateSeries( start, stop );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nonnull Expression<E> step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nonnull Expression<E> step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nullable E step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nullable E step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nullable E step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop, @Nonnull Expression<E> step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nullable E start, @Nullable E stop, @Nullable E step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Number> JpaSetReturningFunction<E> generateSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nonnull Expression<E> step) {
		return criteriaBuilder.generateSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nonnull Expression<? extends TemporalAmount> step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nonnull Expression<? extends TemporalAmount> step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nullable E stop, @Nonnull Expression<? extends TemporalAmount> step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nullable TemporalAmount step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nullable E stop, @Nullable TemporalAmount step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nonnull Expression<E> stop, @Nullable TemporalAmount step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nullable E start, @Nullable E stop, @Nullable TemporalAmount step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public <E extends Temporal> JpaSetReturningFunction<E> generateTimeSeries(@Nonnull Expression<E> start, @Nonnull Expression<E> stop, @Nonnull Expression<? extends TemporalAmount> step) {
		return criteriaBuilder.generateTimeSeries( start, stop, step );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaJsonTableFunction jsonTable(@Nonnull Expression<?> jsonDocument) {
		return criteriaBuilder.jsonTable( jsonDocument );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaJsonTableFunction jsonTable(@Nonnull Expression<?> jsonDocument, @Nullable String jsonPath) {
		return criteriaBuilder.jsonTable( jsonDocument, jsonPath );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaJsonTableFunction jsonTable(@Nonnull Expression<?> jsonDocument, @Nullable Expression<String> jsonPath) {
		return criteriaBuilder.jsonTable( jsonDocument, jsonPath );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaXmlTableFunction xmlTable(@Nullable String xpath, @Nonnull Expression<?> xmlDocument) {
		return criteriaBuilder.xmlTable( xpath, xmlDocument );
	}

	@Nonnull
	@Incubating(since = "6.3")
	@Override
	public JpaXmlTableFunction xmlTable(@Nonnull Expression<String> xpath, @Nonnull Expression<?> xmlDocument) {
		return criteriaBuilder.xmlTable( xpath, xmlDocument );
	}

	@Override
	public <C extends Comparable<? super C>> Expression<C> least(C x, Expression<C> y) {
		return criteriaBuilder.least( x, y );
	}

	@Override
	public <C extends Comparable<? super C>> Expression<C> least(Expression<C> x, Expression<C> y) {
		return criteriaBuilder.least( x, y );
	}

	@Override
	public <C extends Comparable<? super C>> Expression<C> greatest(C x, Expression<C> y) {
		return criteriaBuilder.greatest( x, y );
	}

	@Override
	public <C extends Comparable<? super C>> Expression<C> greatest(Expression<C> x, Expression<C> y) {
		return criteriaBuilder.greatest( x, y );
	}
}
