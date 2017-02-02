/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.type.Type;

/**
 * A FilterDefinition defines the global attributes of a dynamic filter.  This
 * information includes its name as well as its defined parameters (name and type).
 *
 * @author Steve Ebersole
 */
public class FilterDefinition implements Serializable {
	private final String filterName;
	private final String defaultFilterCondition;
	private final Map<String, Type> parameterTypes = new HashMap<String, Type>();

	/**
	 * Construct a new FilterDefinition instance.
	 *
	 * @param name The name of the filter for which this configuration is in effect.
	 */
	public FilterDefinition(String name, String defaultCondition, Map<String, Type> parameterTypes) {
		this.filterName = name;
		this.defaultFilterCondition = defaultCondition;
		if ( parameterTypes != null ) {
			this.parameterTypes.putAll( parameterTypes );
		}
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
	public Set<String> getParameterNames() {
		return parameterTypes.keySet();
	}

	/**
	 * Retrieve the type of the named parameter defined for this filter.
	 *
	 * @param parameterName The name of the filter parameter for which to return the type.
	 *
	 * @return The type of the named parameter.
	 */
	public Type getParameterType(String parameterName) {
		return parameterTypes.get( parameterName );
	}

	public String getDefaultFilterCondition() {
		return defaultFilterCondition;
	}

	public Map<String, Type> getParameterTypes() {
		return parameterTypes;
	}

}
