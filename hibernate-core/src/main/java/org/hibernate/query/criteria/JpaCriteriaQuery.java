/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.EntityType;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.query.common.FetchClauseType;

import java.util.List;
import java.util.Set;

/**
 * Extension of the JPA {@link CriteriaQuery}
 *
 * @author Steve Ebersole
 */
public interface JpaCriteriaQuery<T> extends CriteriaQuery<T>, JpaQueryableCriteria<T>, JpaSelectCriteria<T>, JpaCriteriaSelect<T> {

	/**
	 * A query that returns the number of results of this query.
	 *
	 * @since 6.4
	 *
	 * @see org.hibernate.query.SelectionQuery#getResultCount()
	 */
	@Nonnull
	JpaCriteriaQuery<Long> createCountQuery();

	/**
	 * A query that returns {@code true} if this query has any results.
	 *
	 * @since 7.1
	 */
	@Incubating
	@Nonnull
	JpaCriteriaQuery<Boolean> createExistsQuery();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Limit/Offset/Fetch clause

	/**
	 * Return the query offset expression.
	 */
	@Nullable
	JpaExpression<Number> getOffset();

	/**
	 * Set the query offset.
	 */
	@Nonnull
	JpaCriteriaQuery<T> offset(@Nullable JpaExpression<? extends Number> offset);

	/**
	 * Set the query offset.
	 */
	@Nonnull
	JpaCriteriaQuery<T> offset(@Nullable Number offset);

	/**
	 * Return the query fetch expression.
	 */
	@Nullable
	JpaExpression<Number> getFetch();

	/**
	 * Set the query fetch limit.
	 */
	@Nonnull
	JpaCriteriaQuery<T> fetch(@Nullable JpaExpression<? extends Number> fetch);

	/**
	 * Set the query fetch limit.
	 */
	@Nonnull
	JpaCriteriaQuery<T> fetch(@Nullable JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);

	/**
	 * Set the query fetch limit.
	 */
	@Nonnull
	JpaCriteriaQuery<T> fetch(@Nullable Number fetch);

	/**
	 * Set the query fetch limit.
	 */
	@Nonnull
	JpaCriteriaQuery<T> fetch(@Nullable Number fetch, FetchClauseType fetchClauseType);

	/**
	 * Return the fetch clause type.
	 */
	FetchClauseType getFetchClauseType();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Accessors

	/**
	 * Return the {@linkplain #getRoots() roots} as a list.
	 */
	@Nonnull
	List<Root<?>> getRootList();

	/**
	 * Get a {@linkplain Root query root} element at the given position
	 * with the given type.
	 *
	 * @param position the position of this root element
	 * @param type the type of the root entity
	 *
	 * @throws IllegalArgumentException if the root entity at the given
	 *         position is not of the given type, or if there are not
	 *         enough root entities in the query
	 */
	@Nonnull
	<E> JpaRoot<? extends E> getRoot(int position, Class<E> type);

	/**
	 * Get a {@linkplain Root query root} element with the given alias
	 * and the given type.
	 *
	 * @param alias the identification variable of the root element
	 * @param type the type of the root entity
	 *
	 * @throws IllegalArgumentException if the root entity with the
	 *         given alias is not of the given type, or if there is
	 *         no root entities with the given alias
	 */
	@Nonnull
	<E> JpaRoot<? extends E> getRoot(String alias, Class<E> type);

	/**
	 * {@inheritDoc}
	 *
	 * @apiNote Warning!  This actually walks the criteria tree looking
	 * for parameters nodes.
	 */
	@Nonnull
	@Override
	Set<ParameterExpression<?>> getParameters();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mutators

	/**
	 * Create a query root for the given entity type.
	 */
	@Nonnull
	@Override
	<X> JpaRoot<X> from(@Nonnull Class<X> entityClass);

	/**
	 * Create a query root for the given entity type.
	 */
	@Nonnull
	@Override
	<X> JpaRoot<X> from(@Nonnull EntityType<X> entity);

	/**
	 * Set whether duplicate query results are eliminated.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> distinct(boolean distinct);

	/**
	 * Set the query selection.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> select(@Nonnull Selection<? extends T> selection);

	/**
	 * Set the compound query selection.
	 */
	@Nonnull
	@Override @Deprecated
	JpaCriteriaQuery<T> multiselect(@Nonnull Selection<?>... selections);

	/**
	 * Set the compound query selection.
	 */
	@Nonnull
	@Override @Deprecated
	JpaCriteriaQuery<T> multiselect(@Nonnull List<Selection<?>> selectionList);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> where(@Nonnull Expression<Boolean> restriction);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> where(@Nonnull BooleanExpression... restrictions);

	/**
	 * Set the restriction.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> where(@Nonnull List<? extends Expression<Boolean>> restrictions);

	/**
	 * Set the grouping expressions.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> groupBy(@Nonnull Expression<?>... grouping);

	/**
	 * Set the grouping expressions.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> groupBy(@Nonnull List<Expression<?>> grouping);

	/**
	 * Set the group restriction.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> having(@Nonnull Expression<Boolean> restriction);

	/**
	 * Set the group restriction.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> having(@Nonnull BooleanExpression... restrictions);

	/**
	 * Set the group restriction.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> having(@Nonnull List<? extends Expression<Boolean>> restrictions);

	/**
	 * Set the ordering expressions.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> orderBy(@Nonnull Order... o);

	/**
	 * Set the ordering expressions.
	 */
	@Nonnull
	@Override
	JpaCriteriaQuery<T> orderBy(@Nonnull List<Order> o);

	/**
	 * Create a subquery for the given entity type.
	 */
	@Nonnull
	@Override
	<U> JpaSubQuery<U> subquery(@Nonnull EntityType<U> type);

	/**
	 * Return the criteria builder that created this query.
	 */
	@Nonnull
	HibernateCriteriaBuilder getCriteriaBuilder();
}
