/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.compile;

import javax.persistence.TypedQuery;

/**
 * Used to describe implicit (not defined in criteria query) parameters.
 *
 * @author Steve Ebersole
 */
public interface ImplicitParameterBinding {
	/**
	 * Retrieve the generated name of the implicit parameter.
	 *
	 * @return The parameter name.
	 */
	public String getParameterName();

	/**
	 * Get the java type of the "thing" that led to the implicit parameter.  Used from
	 * {@link org.hibernate.jpa.spi.HibernateEntityManagerImplementor.QueryOptions#getNamedParameterExplicitTypes()}
	 * in determining "guessed type" overriding.
	 *
	 * @return The java type
	 */
	public Class getJavaType();

	/**
	 * Bind the implicit parameter's value to the JPA query.
	 *
	 * @param typedQuery The JPA query.
	 */
	public void bind(TypedQuery typedQuery);
}
