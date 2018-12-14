/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.query.criteria.JpaQuerySpecification;

/**
 * @author Steve Ebersole
 */
public interface QuerySpecificationImplementor<T> extends JpaQuerySpecification<T>, CriteriaNode {
	@Override
	QuerySpecificationImplementor<T> distinct(boolean distinct);

	@Override
	SelectionImplementor<T> getSelection();

	@Override
	Set<? extends RootImplementor> getJpaRoots();

	@Override
	PredicateImplementor getRestriction();

	@Override
	List<? extends SortSpecification> getSortSpecifications();

	@Override
	List<? extends ExpressionImplementor<?>> getGroupByList();

	@Override
	PredicateImplementor getGroupRestriction();

	@Override
	<X> ExpressionImplementor<X> getLimit();

	@Override
	<X> ExpressionImplementor<X> getOffset();
}
