/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;
import java.util.Set;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * Commonality between a JPA {@link JpaCriteriaQuery} and {@link JpaSubQuery}
 *
 * @author Steve Ebersole
 */
public interface JpaQuerySpecification<T> extends AbstractQuery<T>, JpaCriteriaNode {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Roots

	Set<? extends JpaRoot> getJpaRoots();

	@Override
	@SuppressWarnings("unchecked")
	default Set<Root<?>> getRoots() {
		return (Set) getJpaRoots();
	}

	@Override
	<X> JpaRoot<X> from(Class<X> entityClass);

	@Override
	<X> JpaRoot<X> from(EntityType<X> entity);

	<X> JpaRoot<X> from(EntityDomainType<X> entity);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Selections

	@Override
	JpaQuerySpecification<T> distinct(boolean distinct);

	@Override
	JpaSelection<T> getSelection();

	JpaQuerySpecification<T> setSelection(JpaSelection<T> selection);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Restrictions


	@Override
	JpaPredicate getRestriction();

	JpaQuerySpecification<T> setWhere(JpaPredicate restriction);

	@Override
	JpaQuerySpecification<T> where(Expression<Boolean> restriction);

	@Override
	JpaQuerySpecification<T> where(Predicate... restrictions);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grouping

	@Override
	List<Expression<?>> getGroupList();
	List<? extends JpaExpression<?>> getGroupByList();
	JpaQuerySpecification<T> setGroupBy(List<? extends JpaExpression<?>> grouping);

	@Override
	JpaQuerySpecification<T> groupBy(Expression<?>... grouping);

	@Override
	JpaQuerySpecification<T> groupBy(List<Expression<?>> grouping);

	@Override
	JpaPredicate getGroupRestriction();
	JpaQuerySpecification<T> setGroupRestriction(JpaPredicate restrictions);

	@Override
	JpaQuerySpecification<T> having(Expression<Boolean> restriction);

	@Override
	JpaQuerySpecification<T> having(Predicate... restrictions);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Ordering

	List<? extends JpaOrder> getSortSpecifications();

	JpaQuerySpecification<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Limit

	<X> JpaExpression<X> getLimit();

	JpaQuerySpecification<T> setLimit(JpaExpression<?> limit);

	<X> JpaExpression<X> getOffset();

	JpaQuerySpecification<T> setOffset(JpaExpression offset);

}
