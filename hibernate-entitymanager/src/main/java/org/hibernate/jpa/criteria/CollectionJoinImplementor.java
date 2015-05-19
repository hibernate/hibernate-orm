/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria;

import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

/**
 * Specialization of {@link JoinImplementor} for {@link java.util.Collection} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface CollectionJoinImplementor<Z,X> extends JoinImplementor<Z,X>, CollectionJoin<Z,X> {
	@Override
	public CollectionJoinImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);

	@Override
	public CollectionJoinImplementor<Z, X> on(Expression<Boolean> restriction);

	@Override
	public CollectionJoinImplementor<Z, X> on(Predicate... restrictions);

	@Override
	public <T extends X> CollectionJoinImplementor<Z, T> treatAs(Class<T> treatAsType);
}
