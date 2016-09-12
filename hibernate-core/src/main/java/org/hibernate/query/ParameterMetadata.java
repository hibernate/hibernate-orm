/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.util.Set;
import javax.persistence.Parameter;

import org.hibernate.Incubating;

/**
 * Access to known information about the parameters for a query.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ParameterMetadata {

	/**
	 * Does this parameter set contain any named parameters?
	 *
	 * @return {@code true} if there are named parameters; {@code false} otherwise.
	 */
	boolean hasNamedParameters();

	/**
	 * Does this parameter set contain any positional parameters?
	 *
	 * @return {@code true} if there are positional parameters; {@code false} otherwise.
	 */
	boolean hasPositionalParameters();

	/**
	 * Returns the number of named parameters.
	 *
	 * @return The number of named parameters.  Will return zero if
	 * {@link #hasNamedParameters()} ()} is {@code false}
	 */
	int getNamedParameterCount();

	/**
	 * Returns the number of positional parameters.
	 *
	 * @return The number of positional parameters.  Will return zero if
	 * {@link #hasPositionalParameters()} is {@code false}
	 */
	int getPositionalParameterCount();

	/**
	 * Return the names of all named parameters of the query.
	 *
	 * @return the parameter names
	 */
	Set<String> getNamedParameterNames();

	Set<QueryParameter<?>> collectAllParameters();

	Set<Parameter<?>> collectAllParametersJpa();

	<T> QueryParameter<T> getQueryParameter(String name);

	<T> QueryParameter<T> getQueryParameter(int position);

	<T> QueryParameter<T> resolve(Parameter<T> param);

	default boolean isOrdinalParametersZeroBased() {
		return true;
	}

	default void setOrdinalParametersZeroBased(boolean isZeroBased) {
	}
}
