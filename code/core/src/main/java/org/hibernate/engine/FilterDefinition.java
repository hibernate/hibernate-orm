// $Id: FilterDefinition.java 9765 2006-04-19 01:45:07Z max.andersen@jboss.com $
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
