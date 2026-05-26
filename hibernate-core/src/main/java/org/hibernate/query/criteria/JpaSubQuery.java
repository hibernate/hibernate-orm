/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;
import org.hibernate.query.common.FetchClauseType;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface JpaSubQuery<T> extends Subquery<T>, JpaSelectCriteria<T>, JpaCriteriaSelect<T>, JpaExpression<T>, JpaCteContainer {

	JpaSubQuery<T> multiselect(Selection<?>... selections);

	JpaSubQuery<T> multiselect(List<Selection<?>> selectionList);

	<X> JpaCrossJoin<X> correlate(JpaCrossJoin<X> parentCrossJoin);

	<X> JpaEntityJoin<T,X> correlate(JpaEntityJoin<T,X> parentEntityJoin);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Limit/Offset/Fetch clause

	@Nullable JpaExpression<Number> getOffset();

	JpaSubQuery<T> offset(@Nullable JpaExpression<? extends Number> offset);

	JpaSubQuery<T> offset(@Nullable Number offset);

	@Nullable JpaExpression<Number> getFetch();

	JpaSubQuery<T> fetch(@Nullable JpaExpression<? extends Number> fetch);

	JpaSubQuery<T> fetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);

	JpaSubQuery<T> fetch(@Nullable Number fetch);

	JpaSubQuery<T> fetch(Number fetch, FetchClauseType fetchClauseType);

	FetchClauseType getFetchClauseType();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Order by clause

	List<JpaOrder> getOrderList();

	JpaSubQuery<T> orderBy(Order... o);

	JpaSubQuery<T> orderBy(List<Order> o);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Nonnull
	@Override
	JpaSubQuery<T> distinct(boolean distinct);

	@Override
	@Nullable
	JpaExpression<T> getSelection();

	@Nonnull
	@Override
	JpaSubQuery<T> select(@Nonnull Expression<T> expression);

	@Nonnull
	@Override
	JpaSubQuery<T> where(@Nonnull Expression<Boolean> restriction);

	@Nonnull
	@Override
	JpaSubQuery<T> where(@Nonnull BooleanExpression... restrictions);

	@Nonnull
	@Override
	JpaSubQuery<T> where(@Nonnull List<? extends Expression<Boolean>> restrictions);

	@Nonnull
	@Override
	JpaSubQuery<T> groupBy(@Nonnull Expression<?>... grouping);

	@Nonnull
	@Override
	JpaSubQuery<T> groupBy(@Nonnull List<Expression<?>> grouping);

	@Nonnull
	@Override
	JpaSubQuery<T> having(@Nonnull Expression<Boolean> restriction);

	@Nonnull
	@Override
	JpaSubQuery<T> having(@Nonnull BooleanExpression... restrictions);

	@Nonnull
	@Override
	JpaSubQuery<T> having(@Nonnull List<? extends Expression<Boolean>> restrictions);

	@Nonnull
	@Override
	<Y> JpaRoot<Y> correlate(@Nonnull Root<Y> parentRoot);

	@Nonnull
	@Override
	<X, Y> JpaJoin<X, Y> correlate(@Nonnull Join<X, Y> parentJoin);

	@Nonnull
	@Override
	<X, Y> JpaCollectionJoin<X, Y> correlate(@Nonnull CollectionJoin<X, Y> parentCollection);

	@Nonnull
	@Override
	<X, Y> JpaSetJoin<X, Y> correlate(@Nonnull SetJoin<X, Y> parentSet);

	@Nonnull
	@Override
	<X, Y> JpaListJoin<X, Y> correlate(@Nonnull ListJoin<X, Y> parentList);

	@Nonnull
	@Override
	<X, K, V> JpaMapJoin<X, K, V> correlate(@Nonnull MapJoin<X, K, V> parentMap);
}
