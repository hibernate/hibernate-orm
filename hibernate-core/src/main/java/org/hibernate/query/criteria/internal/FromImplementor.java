/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;
import javax.persistence.criteria.From;

import org.hibernate.query.criteria.internal.compile.RenderingContext;

/**
 * Implementation contract for the JPA {@link From} interface.
 *
 * @author Steve Ebersole
 */
public interface FromImplementor<Z,X> extends PathImplementor<X>, From<Z,X> {
	void prepareAlias(RenderingContext renderingContext);
	String renderTableExpression(RenderingContext renderingContext);


	FromImplementor<Z,X> correlateTo(CriteriaSubqueryImpl subquery);
	void prepareCorrelationDelegate(FromImplementor<Z,X> parent);
	FromImplementor<Z, X> getCorrelationParent();
}
