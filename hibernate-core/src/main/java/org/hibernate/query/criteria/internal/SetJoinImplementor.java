/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.SetJoin;

/**
 * Specialization of {@link JoinImplementor} for {@link java.util.Set} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface SetJoinImplementor<Z,X> extends JoinImplementor<Z,X>, SetJoin<Z,X> {
	@Override
	public SetJoinImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);

	@Override
	public SetJoinImplementor<Z,X> on(Expression<Boolean> restriction);

	@Override
	public SetJoinImplementor<Z, X> on(Predicate... restrictions);

	@Override
	public <T extends X> SetJoinImplementor<Z, T> treatAs(Class<T> treatAsType);
}
