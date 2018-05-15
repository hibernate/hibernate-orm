/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import javax.persistence.Parameter;

import org.hibernate.Incubating;

/**
 * Access to known information about the parameters for a query.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ParameterMetadata<P extends QueryParameter<?>> {
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
	// General purpose

	/**
	 * The total number of registered parameters.
	 */
	int getParameterCount();

	/**
	 * Resolve the QueryParameter reference registered here under the
	 * given name, if one.
	 *
	 * @return The registered parameter
	 *
	 * @throws IllegalArgumentException if no parameter is registered under that name
	 */
	P getQueryParameter(String name);

	/**
	 * Resolve the QueryParameter reference registered here under the
	 * given position/ordinal label, if one.
	 *
	 * @return The registered parameter
	 *
	 * @throws IllegalArgumentException if no parameter is registered under that label
	 */
	P getQueryParameter(int positionLabel);

	/**
	 * A deeper resolution attempt from a JPA parameter reference to Hibernate's
	 * contract.  Generally should return the same param reference.
	 *
	 * According to the spec, only Parameter references obtained from the provider
	 * are valid.
	 */
	P resolve(Parameter param);

	/**
	 * Is this parameter reference registered in this collection?
	 */
	boolean containsReference(P parameter);

	Set<P> getRegistrations();

	/**
	 * General purpose visitation using functional
	 */
	void visitRegistrations(Consumer<P> action);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
	// Named parameters

	/**
	 * Does this parameter set contain any named parameters?
	 *
	 * @return {@code true} if there are named parameters; {@code false} otherwise.
	 */
	boolean hasNamedParameters();

	/**
	 * Returns the number of named parameters.
	 *
	 * @return The number of named parameters.  Will return zero if
	 * {@link #hasNamedParameters()} ()} is {@code false}
	 */
	int getNamedParameterCount();

	/**
	 * Return the names of all named parameters of the query.
	 *
	 * @return the parameter names
	 */
	Set<String> getNamedParameterNames();

	Collection<P> getNamedParameters();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
	// "positional" parameters

	/**
	 * Does this parameter set contain any positional parameters?
	 *
	 * @return {@code true} if there are positional parameters; {@code false} otherwise.
	 */
	boolean hasPositionalParameters();

	/**
	 * Returns the number of positional parameters.
	 *
	 * @return The number of positional parameters.  Will return zero if
	 * {@link #hasPositionalParameters()} is {@code false}
	 */
	int getPositionalParameterCount();

	Set<Integer> getOrdinalParameterLabels();

	Collection<P> getPositionalParameters();
}
