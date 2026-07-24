/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import jakarta.annotation.Nullable;

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
	private final Map<String, String> parameterTypeClassNames = new HashMap<>();
	private final Map<String, String> parameterResolverClassNames = new HashMap<>();
	private transient Map<String, ManagedBean<? extends Supplier<?>>> parameterResolverMap = new HashMap<>();
	private final boolean autoEnabled;
	private final boolean applyToLoadByKey;

	/**
	 * Construct a new FilterDefinition instance.
	 *
	 * @param name The name of the filter for which this configuration is in effect.
	 */
	public FilterDefinition(String name, String defaultCondition, @Nullable Map<String, JdbcMapping> explicitParamJaMappings) {
		this( name, defaultCondition, false, false, explicitParamJaMappings, Collections.emptyMap() );
	}

	public FilterDefinition(
			String name,
			String defaultCondition,
			boolean autoEnabled,
			boolean applyToLoadByKey,
			@Nullable Map<String, JdbcMapping> explicitParamJaMappings,
			@Nullable Map<String, ManagedBean<? extends  Supplier<?>>> parameterResolverMap) {
		this(
				name,
				defaultCondition,
				autoEnabled,
				applyToLoadByKey,
				explicitParamJaMappings,
				inferParameterTypeClassNames( explicitParamJaMappings ),
				parameterResolverMap
		);
	}

	@Internal
	public FilterDefinition(
			String name,
			String defaultCondition,
			boolean autoEnabled,
			boolean applyToLoadByKey,
			@Nullable Map<String, JdbcMapping> explicitParamJaMappings,
			@Nullable Map<String, String> parameterTypeClassNames,
			@Nullable Map<String, ManagedBean<? extends Supplier<?>>> parameterResolverMap) {
		this.filterName = name;
		this.defaultFilterCondition = defaultCondition;
		if ( explicitParamJaMappings != null ) {
			this.explicitParamJaMappings.putAll( explicitParamJaMappings );
		}
		if ( parameterTypeClassNames != null ) {
			this.parameterTypeClassNames.putAll( parameterTypeClassNames );
		}
		if ( parameterResolverMap != null ) {
			this.parameterResolverMap.putAll( parameterResolverMap );
			parameterResolverMap.forEach( (parameterName, bean) ->
					parameterResolverClassNames.put( parameterName, bean.getBeanClass().getName() ) );
		}
		this.autoEnabled = autoEnabled;
		this.applyToLoadByKey = applyToLoadByKey;
	}

	private static Map<String, String> inferParameterTypeClassNames(
			@Nullable Map<String, JdbcMapping> parameterMappings) {
		if ( parameterMappings == null || parameterMappings.isEmpty() ) {
			return Collections.emptyMap();
		}
		final Map<String, String> result = new HashMap<>();
		parameterMappings.forEach( (name, mapping) -> {
			if ( mapping != null ) {
				result.put( name, mapping.getJavaTypeDescriptor().getJavaTypeClass().getName() );
			}
		} );
		return result;
	}

	@Internal
	public Map<String, String> getParameterTypeClassNames() {
		return Collections.unmodifiableMap( parameterTypeClassNames );
	}

	@Internal
	public Map<String, String> getParameterResolverClassNames() {
		return Collections.unmodifiableMap( parameterResolverClassNames );
	}

	@Internal
	public static FilterDefinition restored(
			String name,
			String defaultCondition,
			boolean autoEnabled,
			boolean applyToLoadByKey,
			Map<String, JdbcMapping> parameterMappings,
			Map<String, String> parameterTypeClassNames,
			Map<String, String> parameterResolverClassNames) {
		final var definition = new FilterDefinition(
				name,
				defaultCondition,
				autoEnabled,
				applyToLoadByKey,
				parameterMappings,
				parameterTypeClassNames,
				Collections.emptyMap()
		);
		definition.parameterResolverClassNames.putAll( parameterResolverClassNames );
		return definition;
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
		// Local variable helps static nullness inference.
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

	public @Nullable Supplier<?> getParameterResolver(String parameterName) {
		final var resolver = parameterResolverMap == null ? null : parameterResolverMap.get( parameterName );
		return resolver == null ? null : resolver.getBeanInstance();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void reattachParameterResolvers(
			ClassLoaderAccess classLoaderAccess,
			ManagedBeanRegistry managedBeanRegistry) {
		parameterResolverMap = new HashMap<>();
		parameterResolverClassNames.forEach( (name, className) -> {
			final Class<?> resolvedClass;
			try {
				resolvedClass = classLoaderAccess.classForName( className );
			}
			catch (RuntimeException e) {
				throw new IllegalStateException(
						"Could not resolve archived parameter resolver class '" + className
								+ "' for filter '" + filterName + "' parameter '" + name + "'",
						e
				);
			}
			if ( !Supplier.class.isAssignableFrom( resolvedClass ) ) {
				throw new IllegalArgumentException(
						"Archived parameter resolver class '" + className
								+ "' for filter '" + filterName + "' parameter '" + name
								+ "' does not implement " + Supplier.class.getName()
				);
			}
			final Class<? extends Supplier<?>> resolverClass =
					(Class<? extends Supplier<?>>) (Class) resolvedClass;
			try {
				parameterResolverMap.put( name, managedBeanRegistry.getBean( resolverClass ) );
			}
			catch (RuntimeException e) {
				throw new IllegalStateException(
						"Could not instantiate archived parameter resolver class '" + className
								+ "' for filter '" + filterName + "' parameter '" + name + "'",
						e
				);
			}
		} );
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
	public boolean isAppliedToLoadByKey() {
		return applyToLoadByKey;
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

	/**
	 * Get a flag that defines if the filter should be enabled by default.
	 *
	 * @return The flag value.
	 */
	public boolean isAutoEnabled() {
		return autoEnabled;
	}

}
