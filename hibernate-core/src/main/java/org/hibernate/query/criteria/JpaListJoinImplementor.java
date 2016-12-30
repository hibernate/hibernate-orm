/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;

/**
 * Specialization of {@link JpaAttributeJoinImplementor} for {@link java.util.List} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaListJoinImplementor<Z,X> extends JpaAttributeJoinImplementor<Z,X>, ListJoin<Z,X> {
	@Override
	JpaListJoinImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);

	@Override
	JpaListJoinImplementor<Z, X> on(Expression<Boolean> restriction);

	@Override
	JpaListJoinImplementor<Z, X> on(Predicate... restrictions);

	@Override
	<T extends X> JpaListJoinImplementor<Z, T> treatAs(Class<T> treatAsType);
}
