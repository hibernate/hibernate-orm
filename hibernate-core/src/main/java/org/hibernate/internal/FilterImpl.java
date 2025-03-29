/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * Implementation of FilterImpl.  FilterImpl implements the user's
 * view into enabled dynamic filters, allowing them to set filter parameter values.
 *
 * @author Steve Ebersole
 */
public class FilterImpl implements Filter, Serializable {
	public static final String MARKER = "$FILTER_PLACEHOLDER$";

	private transient FilterDefinition definition;
	private final String filterName;
	//Lazily initialized!
	//Note that ordering is important for cache keys
	private @Nullable TreeMap<String,Object> parameters;
	private final boolean autoEnabled;
	private final boolean applyToLoadByKey;

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
		this.autoEnabled = definition.isAutoEnabled();
		this.applyToLoadByKey = definition.isAppliedToLoadByKey();
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

	/**
	 * Get a flag that defines if the filter should be enabled by default.
	 *
	 * @return The flag value.
	 */
	public boolean isAutoEnabled() {
		return autoEnabled;
	}


	/**
	 * Get a flag that defines if the filter should be applied
	 * on direct fetches or not.
	 *
	 * @return The flag value.
	 */
	public boolean isAppliedToLoadByKey() {
		return applyToLoadByKey;
	}

	public Map<String,?> getParameters() {
		return parameters == null ? emptyMap() : unmodifiableMap( parameters );
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
		final Object argument = definition.processArgument( value );

		// Make sure this is a defined parameter and check the incoming value type
		final JdbcMapping type = definition.getParameterJdbcMapping( name );
		if ( type == null ) {
			throw new IllegalArgumentException( "Undefined filter parameter '" + name + "'" );
		}
		if ( argument != null && !type.getJavaTypeDescriptor().isInstance( argument ) ) {
			throw new IllegalArgumentException( "Argument assigned to filter parameter '" + name
					+ "' is not of type '" + type.getJavaTypeDescriptor().getTypeName() + "'" );
		}
		if ( parameters == null ) {
			parameters = new TreeMap<>();
		}
		parameters.put( name, argument );
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
	public Filter setParameterList(String name, Collection<?> values) throws HibernateException  {
		// Make sure this is a defined parameter and check the incoming value type
		if ( values == null ) {
			throw new IllegalArgumentException( "Collection must be not null" );
		}
		final JdbcMapping type = definition.getParameterJdbcMapping( name );
		if ( type == null ) {
			throw new HibernateException( "Undefined filter parameter '" + name + "'" );
		}
		if ( !values.isEmpty() ) {
			final Object element = values.iterator().next();
			if ( !type.getJavaTypeDescriptor().isInstance( element ) ) {
				throw new IllegalArgumentException( "Argument assigned to filter parameter '" + name
						+ "' is not of type '" + type.getJavaTypeDescriptor().getTypeName() + "'" );
			}
		}
		if ( parameters == null ) {
			parameters = new TreeMap<>();
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
		return parameters == null ? null : parameters.get( name );
	}

	public Supplier<?> getParameterResolver(String name) {
		return definition.getParameterResolver(name);
	}

	/**
	 * Perform validation of the filter state.  This is used to verify the
	 * state of the filter after its enablement and before its use.
	 *
	 * @throws HibernateException If the state is not currently valid.
	 */
	public void validate() throws HibernateException {
		// for each of the defined parameters, make sure its argument
		// has been set or a resolver has been implemented and specified
		for ( final String parameterName : definition.getParameterNames() ) {
			if ( !hasArgument(parameterName) && !hasResolver(parameterName)) {
				throw new HibernateException( "Filter parameter '" + getName()
						+ "' has neither an argument nor a resolver" );
			}
		}
	}

	private boolean hasResolver(String parameterName) {
		final Supplier<?> resolver = getParameterResolver(parameterName);
		return resolver != null
			&& !resolver.getClass().isInterface();
	}

	private boolean hasArgument(String parameterName) {
		return parameters != null && parameters.containsKey(parameterName);
	}

	@Override
	public Object getParameterValue(String paramName) {
		final Object value = getParameter( paramName );
		if ( value != null ) {
			return value;
		}
		else {
			final Supplier<?> filterParamResolver = getParameterResolver( paramName );
			return filterParamResolver == null ? null : filterParamResolver.get();
		}
	}
}
