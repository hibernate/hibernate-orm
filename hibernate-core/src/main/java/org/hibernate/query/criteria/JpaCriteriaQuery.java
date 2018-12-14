/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.EntityType;

/**
 * Extension of the JPA {@link CriteriaQuery}
 *
 * @author Steve Ebersole
 */
public interface JpaCriteriaQuery<T> extends CriteriaQuery<T>, QueryableCriteria, JpaSelectCriteria<T> {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Accessors

	@Override
	@SuppressWarnings("unchecked")
	default List<Order> getOrderList() {
		return (List) getQueryStructure().getSortSpecifications();
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
	JpaCriteriaQuery<T> groupBy(Expression<?>... grouping);

	@Override
	JpaCriteriaQuery<T> groupBy(List<Expression<?>> grouping);

	@Override
	JpaCriteriaQuery<T> having(Expression<Boolean> restriction);

	@Override
	JpaCriteriaQuery<T> having(Predicate... restrictions);

	@Override
	JpaCriteriaQuery<T> orderBy(Order... o);

	@Override
	JpaCriteriaQuery<T> orderBy(List<Order> o);
}
