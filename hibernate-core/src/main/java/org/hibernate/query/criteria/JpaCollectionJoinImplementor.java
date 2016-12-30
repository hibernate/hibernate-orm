/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;

/**
 * Specialization of {@link JpaAttributeJoinImplementor} for {@link java.util.Collection} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaCollectionJoinImplementor<Z,X> extends JpaAttributeJoinImplementor<Z,X>, CollectionJoin<Z,X> {
	@Override
	JpaCollectionJoinImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);

	@Override
	JpaCollectionJoinImplementor<Z, X> on(Expression<Boolean> restriction);

	@Override
	JpaCollectionJoinImplementor<Z, X> on(Predicate... restrictions);

	@Override
	<T extends X> JpaCollectionJoinImplementor<Z, T> treatAs(Class<T> treatAsType);
}
