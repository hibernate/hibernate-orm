/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;
import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;
import org.hibernate.sqm.parser.criteria.tree.from.JpaFrom;

/**
 * Hibernate ORM specialization of the JPA {@link javax.persistence.criteria.From}
 * contract.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating
public interface JpaFromImplementor<Z,X> extends JpaPathImplementor<X>, JpaFrom<Z,X> {
	JpaFromImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);
	void prepareCorrelationDelegate(JpaFromImplementor<Z,X> parent);
	JpaFromImplementor<Z, X> getCorrelationParent();

	@Override
	<T extends X> JpaPathImplementor<T> treatAs(Class<T> treatAsType);
}
