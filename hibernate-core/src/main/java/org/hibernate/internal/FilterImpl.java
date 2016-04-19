/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Implementation of FilterImpl.  FilterImpl implements the user's
 * view into enabled dynamic filters, allowing them to set filter parameter values.
 *
 * @author Steve Ebersole
 */
public class FilterImpl implements Filter, Serializable {
	public static final String MARKER = "$FILTER_PLACEHOLDER$";

	private transient FilterDefinition definition;
	private String filterName;
	private Map<String,Object> parameters = new HashMap<>();
	
	void afterDeserialize(SessionFactoryImplementor factory) {
		definition = factory.getFilterDefinition( filterName );
		validate();
	}

	/**
	 * Constructs a new FilterImpl.
	 *
	 * @param configuration The filter's global configuration.
	 */
	public FilterImpl(FilterDefinition configuration) {
		this.definition = configuration;
		filterName = definition.getFilterName();
	}

	public FilterDefinition getFilterDefinition() {
		return definition;
	}

	/**
	 * Get the name of this filter.
	 *
	 * @return This filter's name.
	 */
	public String getName() {
		return definition.getFilterName();
	}
	
	public Map<String,?> getParameters() {
		return parameters;
	}

	/**
	 * Set the named parameter's value for this filter.
	 *
	 * @param name The parameter's name.
	 * @param value The value to be applied.
	 * @return This FilterImpl instance (for method chaining).
	 * @throws IllegalArgumentException Indicates that either the parameter was undefined or that the type
	 * of the passed value did not match the configured type.
	 */
	public Filter setParameter(String name, Object value) throws IllegalArgumentException {
		// Make sure this is a defined parameter and check the incoming value type
		// TODO: what should be the actual exception type here?
		Type type = definition.getParameterType( name );
		if ( type == null ) {
			throw new IllegalArgumentException( "Undefined filter parameter [" + name + "]" );
		}
		if ( value != null && !type.getReturnedClass().isAssignableFrom( value.getClass() ) ) {
			throw new IllegalArgumentException( "Incorrect type for parameter [" + name + "]" );
		}
		parameters.put( name, value );
		return this;
	}

	/**
	 * Set the named parameter's value list for this filter.  Used
	 * in conjunction with IN-style filter criteria.
	 *
	 * @param name   The parameter's name.
	 * @param values The values to be expanded into an SQL IN list.
	 * @return This FilterImpl instance (for method chaining).
	 */
	public Filter setParameterList(String name, Collection values) throws HibernateException  {
		// Make sure this is a defined parameter and check the incoming value type
		if ( values == null ) {
			throw new IllegalArgumentException( "Collection must be not null!" );
		}
		Type type = definition.getParameterType( name );
		if ( type == null ) {
			throw new HibernateException( "Undefined filter parameter [" + name + "]" );
		}
		if ( !values.isEmpty() ) {
			Class elementClass = values.iterator().next().getClass();
			if ( !type.getReturnedClass().isAssignableFrom( elementClass ) ) {
				throw new HibernateException( "Incorrect type for parameter [" + name + "]" );
			}
		}
		parameters.put( name, values );
		return this;
	}

	/**
	 * Set the named parameter's value list for this filter.  Used
	 * in conjunction with IN-style filter criteria.
	 *
	 * @param name The parameter's name.
	 * @param values The values to be expanded into an SQL IN list.
	 * @return This FilterImpl instance (for method chaining).
	 */
	public Filter setParameterList(String name, Object[] values) throws IllegalArgumentException {
		return setParameterList( name, Arrays.asList( values ) );
	}

	/**
	 * Get the value of the named parameter for the current filter.
	 *
	 * @param name The name of the parameter for which to return the value.
	 * @return The value of the named parameter.
	 */
	public Object getParameter(String name) {
		return parameters.get( name );
	}

	/**
	 * Perform validation of the filter state.  This is used to verify the
	 * state of the filter afterQuery its enablement and beforeQuery its use.
	 *
	 * @throws HibernateException If the state is not currently valid.
	 */
	public void validate() throws HibernateException {
		// for each of the defined parameters, make sure its value
		// has been set

		for ( final String parameterName : definition.getParameterNames() ) {
			if ( parameters.get( parameterName ) == null ) {
				throw new HibernateException(
						"Filter [" + getName() + "] parameter [" + parameterName + "] value not set"
				);
			}
		}
	}
}
