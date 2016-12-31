/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.Incubating;
import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;
import org.hibernate.sqm.parser.criteria.tree.from.JpaMapJoin;

/**
 * Hibernate ORM specialization of the JPA {@link javax.persistence.criteria.MapJoin}
 * contract.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaMapJoinImplementor<Z,K,V> extends JpaAttributeJoinImplementor<Z,V>, JpaMapJoin<Z,K,V> {
	@Override
	JpaMapJoinImplementor<Z,K,V> correlateTo(CriteriaSubqueryImpl subquery);

	@Override
	JpaMapJoinImplementor<Z, K, V> on(Expression<Boolean> restriction);

	@Override
	JpaMapJoinImplementor<Z, K, V> on(Predicate... restrictions);

	@Override
	<T extends V> JpaMapJoinImplementor<Z, K, T> treatAs(Class<T> treatAsType);
}
