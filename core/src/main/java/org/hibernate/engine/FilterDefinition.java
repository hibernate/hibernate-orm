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
package org.hibernate.engine;

import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * A FilterDefinition defines the global attributes of a dynamic filter.  This
 * information includes its name as well as its defined parameters (name and type).
 * 
 * @author Steve Ebersole
 */
public class FilterDefinition implements Serializable {
	private final String filterName;
	private final String defaultFilterCondition;
	private final Map parameterTypes = new HashMap();

	/**
	 * Construct a new FilterDefinition instance.
	 *
	 * @param name The name of the filter for which this configuration is in effect.
	 */
	public FilterDefinition(String name, String defaultCondition, Map parameterTypes) {
		this.filterName = name;
		this.defaultFilterCondition = defaultCondition;
		this.parameterTypes.putAll( parameterTypes );
	}

	/**
	 * Get the name of the filter this configuration defines.
	 *
	 * @return The filter name for this configuration.
	 */
	public String getFilterName() {
		return filterName;
	}

	/**
	 * Get a set of the parameters defined by this configuration.
	 *
	 * @return The parameters named by this configuration.
	 */
	public Set getParameterNames() {
		return parameterTypes.keySet();
	}

	/**
	 * Retreive the type of the named parameter defined for this filter.
	 *
	 * @param parameterName The name of the filter parameter for which to return the type.
	 * @return The type of the named parameter.
	 */
    public Type getParameterType(String parameterName) {
	    return (Type) parameterTypes.get(parameterName);
    }

	public String getDefaultFilterCondition() {
		return defaultFilterCondition;
	}

	public Map getParameterTypes() {
		return parameterTypes;
	}

}
