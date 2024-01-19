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

import org.hibernate.metamodel.mapping.JdbcMapping;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the definition of a {@link org.hibernate.Filter filter}.
 * This information includes the {@linkplain #filterName name} of the
 * filter, along with the {@linkplain #getParameterJdbcMapping(String)
 * names} and {@linkplain #getParameterJdbcMapping(String) types} of
 * every parameter of the filter. A filter may optionally have a
 * {@linkplain #defaultFilterCondition default condition}.
 *
 * @see org.hibernate.annotations.FilterDef
 * @see org.hibernate.Filter
 *
 * @author Steve Ebersole
 */
public class FilterDefinition implements Serializable {
	private final String filterName;
	private final String defaultFilterCondition;
	private final Map<String, JdbcMapping> explicitParamJaMappings = new HashMap<>();
	private final boolean appliedOnFind;

	/**
	 * Construct a new FilterDefinition instance.
	 *
	 * @param name The name of the filter for which this configuration is in effect.
	 */
	public FilterDefinition(String name, String defaultCondition, @Nullable Map<String, JdbcMapping> explicitParamJaMappings, boolean appliedOnFind) {
		this.filterName = name;
		this.defaultFilterCondition = defaultCondition;
		if ( explicitParamJaMappings != null ) {
			this.explicitParamJaMappings.putAll( explicitParamJaMappings );
		}
		this.appliedOnFind = appliedOnFind;
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
		// local variable instantiated for checkerframework nullness inference reasons
		Set<String> keys = explicitParamJaMappings.keySet();
		return keys;
	}

	/**
	 * Retrieve the type of the named parameter defined for this filter.
	 *
	 * @param parameterName The name of the filter parameter for which to return the type.
	 *
	 * @return The type of the named parameter.
	 */
	public @Nullable JdbcMapping getParameterJdbcMapping(String parameterName) {
		return explicitParamJaMappings.get( parameterName );
	}

	public String getDefaultFilterCondition() {
		return defaultFilterCondition;
	}

	/**
	 * Get a flag that defines if the filter should be applied
	 * on direct fetches or not.
	 *
	 * @return The flag value.
	 */
	public boolean isAppliedOnFind() {
		return appliedOnFind;
	}

	/**
	 * Called before binding a JDBC parameter
	 *
	 * @param value the argument to the parameter, as set via {@link org.hibernate.Filter#setParameter(String, Object)}
	 * @return the argument that will actually be bound to the parameter
	 */
	public Object processArgument(Object value) {
		return value;
	}

}
