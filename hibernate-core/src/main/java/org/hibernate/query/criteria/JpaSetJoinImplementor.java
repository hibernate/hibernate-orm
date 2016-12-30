/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.SetJoin;

import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;

/**
 * Specialization of {@link JpaAttributeJoinImplementor} for {@link java.util.Set} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaSetJoinImplementor<Z,X> extends JpaAttributeJoinImplementor<Z,X>, SetJoin<Z,X> {
	@Override
	JpaSetJoinImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);

	@Override
	JpaSetJoinImplementor<Z,X> on(Expression<Boolean> restriction);

	@Override
	JpaSetJoinImplementor<Z, X> on(Predicate... restrictions);

	@Override
	<T extends X> JpaSetJoinImplementor<Z, T> treatAs(Class<T> treatAsType);
}
