/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
