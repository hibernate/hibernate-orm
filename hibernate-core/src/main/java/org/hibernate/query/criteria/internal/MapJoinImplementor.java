/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;

/**
 * Specialization of {@link JoinImplementor} for {@link java.util.Map} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface MapJoinImplementor<Z,K,V> extends JoinImplementor<Z,V>, MapJoin<Z,K,V> {
	@Override
	public MapJoinImplementor<Z,K,V> correlateTo(CriteriaSubqueryImpl subquery);

	@Override
	public MapJoinImplementor<Z, K, V> on(Expression<Boolean> restriction);

	@Override
	public MapJoinImplementor<Z, K, V> on(Predicate... restrictions);

	@Override
	public <T extends V> MapJoinImplementor<Z, K, T> treatAs(Class<T> treatAsType);
}
