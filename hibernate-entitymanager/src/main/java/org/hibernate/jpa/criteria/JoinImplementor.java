/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

/**
 * Consolidates the {@link Join} and {@link Fetch} hierarchies since that is how we implement them.
 * This allows us to treat them polymorphically.
*
* @author Steve Ebersole
*/
public interface JoinImplementor<Z,X> extends Join<Z,X>, Fetch<Z,X>, FromImplementor<Z,X> {
	/**
	 * Refined return type
	 */
	@Override
	public JoinImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);

	/**
	 * Coordinate return type between {@link Join#on(Expression)} and {@link Fetch#on(Expression)}
	 */
	@Override
	public JoinImplementor<Z, X> on(Expression<Boolean> restriction);

	/**
	 * Coordinate return type between {@link Join#on(Predicate...)} and {@link Fetch#on(Predicate...)}
	 */
	@Override
	public JoinImplementor<Z, X> on(Predicate... restrictions);

	@Override
	public <T extends X> JoinImplementor<Z, T> treatAs(Class<T> treatAsType);
}
