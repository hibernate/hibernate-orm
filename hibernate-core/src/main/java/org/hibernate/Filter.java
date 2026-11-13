package org.hibernate;

import java.util.Collection;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.annotations.FilterDef;
import org.hibernate.engine.FilterDefinition;

/**
 * Allows control over an enabled {@linkplain FilterDef filter} at runtime.
 * In particular, allows {@linkplain #setParameter(String, Object) arguments}
 * to be assigned to parameters declared by the filter.
 * <p>
 * A filter may be defined using the annotations {@link FilterDef @FilterDef}
 * and {@link org.hibernate.annotations.Filter @Filter}, but must be explicitly
 * enabled at runtime by calling {@link Session#enableFilter(String)}, unless
 * the filter is declared as {@linkplain FilterDef#autoEnabled auto-enabled}.
 * If, in a given session, a filter not declared {@code autoEnabled = true} is
 * not explicitly enabled by calling {@code enableFilter()}, the filter will
 * have no effect in that session.
 * <p>
 * Every {@linkplain FilterDef#parameters parameter} of the filter must be
 * supplied an argument by calling {@code setParameter()} immediately after
 * {@code enableFilter()} is called, and before any other operation of the
 * session is invoked.
 *
 * @see org.hibernate.annotations.FilterDef
 * @see Session#enableFilter(String)
 * @see FilterDefinition
 *
 * @author Steve Ebersole
 */
public interface Filter {

	/**
	 * Get the name of this filter.
	 *
	 * @return This filter's name.
	 */
	@Nonnull
	String getName();

	/**
	 * Get the associated {@link FilterDefinition definition} of this
	 * named filter.
	 *
	 * @return The filter definition
	 */
	@Nonnull
	FilterDefinition getFilterDefinition();

	/**
	 * Set the named parameter's value for this filter.
	 *
	 * @param name The parameter's name.
	 * @param value The value to be applied, which may be null.
	 * @return This FilterImpl instance (for method chaining).
	 */
	@Nonnull
	Filter setParameter(@Nonnull String name, @Nullable Object value);

	/**
	 * Set the named parameter's value list for this filter.  Used
	 * in conjunction with IN-style filter criteria.
	 *
	 * @param name The parameter's name.
	 * @param values The values to be expanded into an SQL IN list.
	 * @return This FilterImpl instance (for method chaining).
	 */
	@Nonnull
	Filter setParameterList(@Nonnull String name, @Nonnull Collection<?> values);

	/**
	 * Set the named parameter's value list for this filter.  Used
	 * in conjunction with IN-style filter criteria.
	 *
	 * @param name The parameter's name.
	 * @param values The values to be expanded into an SQL IN list.
	 * @return This FilterImpl instance (for method chaining).
	 */
	@Nonnull
	Filter setParameterList(@Nonnull String name, @Nonnull Object[] values);

	/**
	 * Perform validation of the filter state.  This is used to verify
	 * the state of the filter after its enablement and before its use.
	 *
	 * @throws HibernateException If the state is not currently valid.
	 */
	void validate() throws HibernateException;

	/**
	 * Get the associated {@link FilterDefinition autoEnabled} of this
	 * named filter.
	 *
	 * @return The flag value
	 */
	boolean isAutoEnabled();

	/**
	 * Get the associated {@link FilterDefinition applyToLoadByKey} of this
	 * named filter.
	 *
	 * @return The flag value
	 */
	boolean isAppliedToLoadByKey();

	/**
	 * Obtain the argument currently bound to the filter parameter
	 * with the given name.
	 *
	 * @param name the name of the filter parameter
	 * @return the bound value, or the value supplied by the parameter resolver
	 * if the bound value is null. Returns null if neither supplies a value.
	 *
	 * @since 7
	 */
	@Incubating(since = "7.0")
	@Nullable
	Object getParameterValue(@Nonnull String name);
}
