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

	/**
	 * Set the compound subquery selection.
	 */
	JpaSubQuery<T> multiselect(Selection<?>... selections);

	/**
	 * Set the compound subquery selection.
	 */
	JpaSubQuery<T> multiselect(List<Selection<?>> selectionList);

	/**
	 * Correlate the given parent query element into this subquery.
	 */
	<X, Y> JpaCrossJoin<X, Y> correlate(JpaCrossJoin<X, Y> parentCrossJoin);

	/**
	 * Correlate the given parent query element into this subquery.
	 */
	<X> JpaEntityJoin<T,X> correlate(JpaEntityJoin<T,X> parentEntityJoin);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Limit/Offset/Fetch clause

	/**
	 * Set the subquery offset.
	 */
	@Nullable JpaExpression<Number> getOffset();

	JpaSubQuery<T> offset(@Nullable JpaExpression<? extends Number> offset);

	/**
	 * Set the subquery offset.
	 */
	JpaSubQuery<T> offset(@Nullable Number offset);

	/**
	 * Set the subquery fetch limit.
	 */
	@Nullable JpaExpression<Number> getFetch();

	JpaSubQuery<T> fetch(@Nullable JpaExpression<? extends Number> fetch);

	/**
	 * Set the subquery fetch limit.
	 */
	JpaSubQuery<T> fetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);

	/**
	 * Set the subquery fetch limit.
	 */
	JpaSubQuery<T> fetch(@Nullable Number fetch);

	/**
	 * Set the subquery fetch limit.
	 */
	JpaSubQuery<T> fetch(Number fetch, FetchClauseType fetchClauseType);

	/**
	 * Return the fetch clause type.
	 */
	FetchClauseType getFetchClauseType();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Order by clause

	/**
	 * Return the ordering expressions.
	 */
	List<JpaOrder> getOrderList();

	/**
	 * Set the ordering expressions.
	 */
	JpaSubQuery<T> orderBy(Order... o);

	/**
	 * Set the ordering expressions.
	 */
	JpaSubQuery<T> orderBy(List<Order> o);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	/**
	 * Set whether duplicate subquery results are eliminated.
	 */
	@Nonnull
	@Override
	JpaSubQuery<T> distinct(boolean distinct);

	/**
	 * Return the subquery selection.
	 */
	@Override
	@Nullable
	JpaExpression<T> getSelection();

	/**
	 * Set the subquery selection.
	 */
	@Nonnull
	@Override
	JpaSubQuery<T> select(@Nonnull Expression<T> expression);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	@Override
	JpaSubQuery<T> where(@Nonnull Expression<Boolean> restriction);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	@Override
	JpaSubQuery<T> where(@Nonnull BooleanExpression... restrictions);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	@Override
	JpaSubQuery<T> where(@Nonnull List<? extends Expression<Boolean>> restrictions);

	/**
	 * Set the grouping expressions.
	 */
	@Nonnull
	@Override
	JpaSubQuery<T> groupBy(@Nonnull Expression<?>... grouping);

	/**
	 * Set the grouping expressions.
	 */
	@Nonnull
	@Override
	JpaSubQuery<T> groupBy(@Nonnull List<Expression<?>> grouping);

	/**
	 * Set the group restriction.
	 */
	@Nonnull
	@Override
	JpaSubQuery<T> having(@Nonnull Expression<Boolean> restriction);

	/**
	 * Set the group restriction.
	 */
	@Nonnull
	@Override
	JpaSubQuery<T> having(@Nonnull BooleanExpression... restrictions);

	/**
	 * Set the group restriction.
	 */
	@Nonnull
	@Override
	JpaSubQuery<T> having(@Nonnull List<? extends Expression<Boolean>> restrictions);

	/**
	 * Correlate the given parent query element into this subquery.
	 */
	@Nonnull
	@Override
	<Y> JpaRoot<Y> correlate(@Nonnull Root<Y> parentRoot);

	/**
	 * Correlate the given parent query element into this subquery.
	 */
	@Nonnull
	@Override
	<X, Y> JpaJoin<X, Y> correlate(@Nonnull Join<X, Y> parentJoin);

	/**
	 * Correlate the given parent query element into this subquery.
	 */
	@Nonnull
	@Override
	<X, Y> JpaCollectionJoin<X, Y> correlate(@Nonnull CollectionJoin<X, Y> parentCollection);

	/**
	 * Correlate the given parent query element into this subquery.
	 */
	@Nonnull
	@Override
	<X, Y> JpaSetJoin<X, Y> correlate(@Nonnull SetJoin<X, Y> parentSet);

	/**
	 * Correlate the given parent query element into this subquery.
	 */
	@Nonnull
	@Override
	<X, Y> JpaListJoin<X, Y> correlate(@Nonnull ListJoin<X, Y> parentList);

	/**
	 * Correlate the given parent query element into this subquery.
	 */
	@Nonnull
	@Override
	<X, K, V> JpaMapJoin<X, K, V> correlate(@Nonnull MapJoin<X, K, V> parentMap);
}
