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
import org.hibernate.sqm.parser.criteria.tree.from.JpaCollectionJoin;

/**
 * Hibernate ORM specialization of the JPA {@link javax.persistence.criteria.CollectionJoin}
 * contract.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaCollectionJoinImplementor<Z,X> extends JpaAttributeJoinImplementor<Z,X>, JpaCollectionJoin<Z,X> {
	@Override
	JpaCollectionJoinImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);

	@Override
	JpaCollectionJoinImplementor<Z, X> on(Expression<Boolean> restriction);

	@Override
	JpaCollectionJoinImplementor<Z, X> on(Predicate... restrictions);

	@Override
	<T extends X> JpaCollectionJoinImplementor<Z, T> treatAs(Class<T> treatAsType);
}
