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
import org.hibernate.sqm.parser.criteria.tree.from.JpaAttributeJoin;
import org.hibernate.sqm.parser.criteria.tree.from.JpaFetch;
import org.hibernate.sqm.parser.criteria.tree.from.JpaFrom;

/**
 * Hibernate ORM specialization consolidating the {@link javax.persistence.criteria.Join}
 * and {@link javax.persistence.criteria.Fetch} hierarchies (since that is how we implement
 * them - this allows us to treat them polymorphically).
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaAttributeJoinImplementor<Z,X> extends JpaAttributeJoin<Z,X>, JpaFetch<Z,X>, JpaFromImplementor<Z,X> {
	@Override
	JpaFrom<?, Z> getParent();

	@Override
	JpaAttributeJoinImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);

	@Override
	JpaAttributeJoinImplementor<Z, X> on(Expression<Boolean> restriction);

	@Override
	JpaAttributeJoinImplementor<Z, X> on(Predicate... restrictions);

	@Override
	<T extends X> JpaAttributeJoinImplementor<Z, T> treatAs(Class<T> treatAsType);
}
