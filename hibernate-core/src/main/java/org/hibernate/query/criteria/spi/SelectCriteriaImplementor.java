/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.List;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.metamodel.EntityType;

import org.hibernate.query.criteria.JpaSelectCriteria;

/**
 * @author Steve Ebersole
 */
public interface SelectCriteriaImplementor<T> extends JpaSelectCriteria<T>, CriteriaNode {
	@Override
	QueryStructure<T> getQueryStructure();

	@Override
	SelectCriteriaImplementor<T> distinct(boolean distinct);

	@Override
	SelectionImplementor<T> getSelection();

	@Override
	<X> RootImplementor<X> from(Class<X> entityClass);

	@Override
	<X> RootImplementor<X> from(EntityType<X> entity);

	@Override
	PredicateImplementor getRestriction();

	@Override
	SelectCriteriaImplementor<T> where(Expression<Boolean> restriction);

	@Override
	SelectCriteriaImplementor<T> where(Predicate... restrictions);

	@Override
	SelectCriteriaImplementor<T> groupBy(Expression<?>... grouping);

	@Override
	SelectCriteriaImplementor<T> groupBy(List<Expression<?>> grouping);

	@Override
	PredicateImplementor getGroupRestriction();

	@Override
	SelectCriteriaImplementor<T> having(Expression<Boolean> restriction);

	@Override
	SelectCriteriaImplementor<T> having(Predicate... restrictions);
}
