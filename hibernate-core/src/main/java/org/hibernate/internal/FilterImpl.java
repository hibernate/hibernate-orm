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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

	private transient @Nonnull FilterDefinition definition;
	private final @Nonnull String filterName;
	//Lazily initialized!
	//Note that ordering is important for cache keys
	private @Nullable TreeMap<String,Object> parameters;
	private final boolean autoEnabled;
	private final boolean applyToLoadByKey;
	private transient boolean validated;

	void afterDeserialize(@Nonnull SessionFactoryImplementor factory) {
		definition = factory.getFilterDefinition( filterName );
		validated = false;
		validate();
	}

	/**
	 * Constructs a new FilterImpl.
	 *
	 * @param configuration The filter's global configuration.
	 */
	public FilterImpl(@Nonnull FilterDefinition configuration) {
		this.definition = configuration;
		filterName = definition.getFilterName();
		this.autoEnabled = definition.isAutoEnabled();
		this.applyToLoadByKey = definition.isAppliedToLoadByKey();
	}

	@Override
	@Nonnull
	public FilterDefinition getFilterDefinition() {
		return definition;
	}

	/**
	 * Get the name of this filter.
	 *
	 * @return This filter's name.
	 */
	@Override
	@Nonnull
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

	@Nonnull
	public Map<String,?> getParameters() {
		return parameters == null
				? emptyMap()
				: unmodifiableMap( parameters );
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
	@Override
	@Nonnull
	public Filter setParameter(@Nonnull String name, @Nullable Object value) {
		final Object argument = definition.processArgument( value );

		// Make sure this is a defined parameter and check the incoming value type
		final var type = definition.getParameterJdbcMapping( name );
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
		validated = false;
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
	@Override
	@Nonnull
	public Filter setParameterList(@Nonnull String name, @Nonnull Collection<?> values) throws HibernateException {
		// Make sure this is a defined parameter and check the incoming value type
		if ( values == null ) {
			throw new IllegalArgumentException( "Collection must be not null" );
		}
		final var type = definition.getParameterJdbcMapping( name );
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
		validated = false;
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
	@Override
	@Nonnull
	public Filter setParameterList(@Nonnull String name, @Nonnull Object[] values) throws IllegalArgumentException {
		return setParameterList( name, Arrays.asList( values ) );
	}

	/**
	 * Get the value of the named parameter for the current filter.
	 *
	 * @param name The name of the parameter for which to return the value.
	 * @return The value of the named parameter.
	 */
	@Nullable
	public Object getParameter(@Nonnull String name) {
		return parameters == null ? null : parameters.get( name );
	}

	@Nullable
	public Supplier<?> getParameterResolver(@Nonnull String name) {
		return definition.getParameterResolver(name);
	}

	/**
	 * Perform validation of the filter state.  This is used to verify the
	 * state of the filter after its enablement and before its use.
	 *
	 * @throws HibernateException If the state is not currently valid.
	 */
	public void validate() throws HibernateException {
		if ( validated ) {
			return;
		}
		// for each of the defined parameters, make sure its argument
		// has been set or a resolver has been implemented and specified
		for ( final String parameterName : definition.getParameterNames() ) {
			if ( !hasArgument(parameterName) && !hasResolver(parameterName)) {
				throw new HibernateException( "Filter parameter '" + getName()
						+ "' has neither an argument nor a resolver" );
			}
		}
		validated = true;
	}

	private boolean hasResolver(@Nonnull String parameterName) {
		final Supplier<?> resolver = getParameterResolver(parameterName);
		return resolver != null
			&& !resolver.getClass().isInterface();
	}

	private boolean hasArgument(@Nonnull String parameterName) {
		return parameters != null && parameters.containsKey(parameterName);
	}

	@Override
	@Nullable
	public Object getParameterValue(@Nonnull String paramName) {
		final Object value = getParameter( paramName );
		if ( value != null ) {
			return value;
		}
		else {
			final var filterParamResolver = getParameterResolver( paramName );
			return filterParamResolver == null ? null : filterParamResolver.get();
		}
	}
}
