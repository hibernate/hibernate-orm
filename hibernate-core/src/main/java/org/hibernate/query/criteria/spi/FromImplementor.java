/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.function.Consumer;

import org.hibernate.query.criteria.JpaFrom;
import org.hibernate.query.criteria.JpaSubQuery;

/**
 * SPI-level contract for {@link org.hibernate.query.criteria.JpaFrom}
 * implementors
 *
 * @author Steve Ebersole
 */
public interface FromImplementor<O,T> extends PathImplementor<T>, JpaFrom<O,T>, PathSourceImplementor<T> {
	/**
	 * Because we unify the JPA Join and Fetch hierarchies a JoinImplementor will
	 * always be a FetchImplementor and any FetchImplementor will also always be a
	 * JoinImplementor... So we need this method to distinguish whether a join is
	 * fetched or not.  This has no real meaning for {@link RootImplementor} nodes
	 */
	boolean isFetched();

	@Override
	FromImplementor<O, T> getCorrelationParent();

	@Override
	FromImplementor<O, T> correlateTo(JpaSubQuery<T> subquery);

	void visitJoins(Consumer<JoinImplementor<T,?>> consumer);
	void visitFetches(Consumer<FetchImplementor<T,?>> consumer);
}
