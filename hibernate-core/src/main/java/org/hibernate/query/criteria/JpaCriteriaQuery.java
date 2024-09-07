/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;
import java.util.Set;

import org.hibernate.query.common.FetchClauseType;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.EntityType;

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
	List<Root<?>> getRootList();

	@Override
	@SuppressWarnings("unchecked")
	default List<Order> getOrderList() {
		return (List) getQueryPart().getSortSpecifications();
	}

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

	@Override
	JpaCriteriaQuery<T> multiselect(Selection<?>... selections);

	@Override
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
}
