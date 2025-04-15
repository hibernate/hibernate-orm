/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.SharedSessionContract;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.common.FetchClauseType;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.query.restriction.Restriction;

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
	JpaCriteriaQuery<Long> createCountQuery();

	/**
	 * If the result type of this query is an entity class, add one or more
	 * {@linkplain org.hibernate.query.Order rules} for ordering the query results.
	 *
	 * @param orderList one or more instances of {@link org.hibernate.query.Order}
	 *
	 * @see org.hibernate.query.Order
	 *
	 * @since 7.0
	 */
	@Incubating
	JpaCriteriaQuery<T> setOrder(List<? extends org.hibernate.query.Order<? super T>> orderList);

	/**
	 * If the result type of this query is an entity class, add a
	 * {@linkplain org.hibernate.query.Order rule} for ordering the query results.
	 *
	 * @param order an instance of {@link org.hibernate.query.Order}
	 *
	 * @see org.hibernate.query.Order
	 *
	 * @since 7.0
	 */
	@Incubating
	JpaCriteriaQuery<T> setOrder(org.hibernate.query.Order<? super T> order);

	/**
	 * If the result type of this query is an entity class, add a
	 * {@linkplain Restriction rule} for restricting the query results.
	 *
	 * @param restriction an instance of {@link Restriction}
	 *
	 * @see Restriction
	 *
	 * @since 7.0
	 */
	@Incubating
	JpaCriteriaQuery<T> addRestriction(Restriction<? super T> restriction);

	/**
	 * Creates a query for this criteria query.
	 *
	 * @param session the Hibernate session
	 *
	 * @see SharedSessionContract#createQuery(CriteriaQuery)
	 *
	 * @since 7.0
	 */
	@Incubating
	default SelectionQuery<T> toQuery(SharedSessionContract session) {
		return session.createQuery( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Limit/Offset/Fetch clause

	JpaExpression<Number> getOffset();

	JpaCriteriaQuery<T> offset(JpaExpression<? extends Number> offset);

	JpaCriteriaQuery<T> offset(Number offset);

	JpaExpression<Number> getFetch();

	JpaCriteriaQuery<T> fetch(JpaExpression<? extends Number> fetch);

	JpaCriteriaQuery<T> fetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);

	JpaCriteriaQuery<T> fetch(Number fetch);

	JpaCriteriaQuery<T> fetch(Number fetch, FetchClauseType fetchClauseType);

	FetchClauseType getFetchClauseType();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Accessors

	/**
	 * Return the {@linkplain #getRoots() roots} as a list.
	 */
	List<? extends JpaRoot<?>> getRootList();

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
	<E> JpaRoot<? extends E> getRoot(String alias, Class<E> type);

	/**
	 * {@inheritDoc}
	 *
	 * @apiNote Warning!  This actually walks the criteria tree looking
	 * for parameters nodes.
	 */
	@Override
	Set<ParameterExpression<?>> getParameters();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mutators

	@Override
	<X> JpaRoot<X> from(Class<X> entityClass);

	@Override
	<X> JpaRoot<X> from(EntityType<X> entity);

	@Override
	JpaCriteriaQuery<T> distinct(boolean distinct);

	@Override
	JpaCriteriaQuery<T> select(Selection<? extends T> selection);

	@Override @Deprecated
	JpaCriteriaQuery<T> multiselect(Selection<?>... selections);

	@Override @Deprecated
	JpaCriteriaQuery<T> multiselect(List<Selection<?>> selectionList);

	@Override
	JpaCriteriaQuery<T> where(Expression<Boolean> restriction);

	@Override
	JpaCriteriaQuery<T> where(Predicate... restrictions);

	@Override
	JpaCriteriaQuery<T> where(List<Predicate> restrictions);

	@Override
	JpaCriteriaQuery<T> groupBy(Expression<?>... grouping);

	@Override
	JpaCriteriaQuery<T> groupBy(List<Expression<?>> grouping);

	@Override
	JpaCriteriaQuery<T> having(Expression<Boolean> restriction);

	@Override
	JpaCriteriaQuery<T> having(Predicate... restrictions);

	@Override
	JpaCriteriaQuery<T> having(List<Predicate> restrictions);

	@Override
	JpaCriteriaQuery<T> orderBy(Order... o);

	@Override
	JpaCriteriaQuery<T> orderBy(List<Order> o);

	@Override
	<U> JpaSubQuery<U> subquery(EntityType<U> type);

	HibernateCriteriaBuilder getCriteriaBuilder();
}
