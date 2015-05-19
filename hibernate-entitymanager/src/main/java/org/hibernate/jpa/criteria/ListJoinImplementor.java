/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;

/**
 * Specialization of {@link JoinImplementor} for {@link java.util.List} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface ListJoinImplementor<Z,X> extends JoinImplementor<Z,X>, ListJoin<Z,X> {
	@Override
	public ListJoinImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);

	@Override
	public ListJoinImplementor<Z, X> on(Expression<Boolean> restriction);

	@Override
	public ListJoinImplementor<Z, X> on(Predicate... restrictions);

	@Override
	public <T extends X> ListJoinImplementor<Z, T> treatAs(Class<T> treatAsType);
}
