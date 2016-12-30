/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import javax.persistence.criteria.From;

import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;
import org.hibernate.sqm.parser.criteria.tree.from.JpaFrom;

/**
 * Implementation contract for the JPA {@link From} interface.
 *
 * @author Steve Ebersole
 */
public interface JpaFromImplementor<Z,X> extends JpaPathImplementor<X>, JpaFrom<Z,X> {
	JpaFromImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);
	void prepareCorrelationDelegate(JpaFromImplementor<Z,X> parent);
	JpaFromImplementor<Z, X> getCorrelationParent();

	@Override
	<T extends X> JpaPathImplementor<T> treatAs(Class<T> treatAsType);
}
