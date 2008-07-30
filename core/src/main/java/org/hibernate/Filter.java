/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate;

import org.hibernate.engine.FilterDefinition;

import java.util.Collection;

/**
 * Type definition of Filter.  Filter defines the user's view into enabled dynamic filters,
 * allowing them to set filter parameter values.
 *
 * @author Steve Ebersole
 */
public interface Filter {

	/**
	 * Get the name of this filter.
	 *
	 * @return This filter's name.
	 */
	public String getName();

	/**
	 * Get the filter definition containing additional information about the
	 * filter (such as default-condition and expected parameter names/types).
	 *
	 * @return The filter definition
	 */
	public FilterDefinition getFilterDefinition();


	/**
	 * Set the named parameter's value for this filter.
	 *
	 * @param name The parameter's name.
	 * @param value The value to be applied.
	 * @return This FilterImpl instance (for method chaining).
	 */
	public Filter setParameter(String name, Object value);

	/**
	 * Set the named parameter's value list for this filter.  Used
	 * in conjunction with IN-style filter criteria.
	 *
	 * @param name The parameter's name.
	 * @param values The values to be expanded into an SQL IN list.
	 * @return This FilterImpl instance (for method chaining).
	 */
	public Filter setParameterList(String name, Collection values);

	/**
	 * Set the named parameter's value list for this filter.  Used
	 * in conjunction with IN-style filter criteria.
	 *
	 * @param name The parameter's name.
	 * @param values The values to be expanded into an SQL IN list.
	 * @return This FilterImpl instance (for method chaining).
	 */
	public Filter setParameterList(String name, Object[] values);

	/**
	 * Perform validation of the filter state.  This is used to verify the
	 * state of the filter after its enablement and before its use.
	 *
	 * @throws HibernateException If the state is not currently valid.
	 */
	public void validate() throws HibernateException;
}
