/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.util.Set;
import java.util.function.Consumer;
import jakarta.persistence.Parameter;

import org.hibernate.Incubating;


/**
 * Access to known information about the parameters for a query.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ParameterMetadata {
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
	// General purpose

	/**
	 * The total number of registered parameters.
	 */
	int getParameterCount();

	/**
	 * Find the QueryParameter registered under the given name, if one.
	 *
	 * @return The registered match, or {@code null} is there is no match
	 *
	 * @see #getQueryParameter(String)
	 */
	QueryParameter<?> findQueryParameter(String name);

	/**
	 * Get the QueryParameter reference registered here under the given name.
	 *
	 * @return The registered match.  Never {@code null}
	 *
	 * @throws IllegalArgumentException if no parameter is registered under that name
	 */
	QueryParameter<?> getQueryParameter(String name);

	/**
	 * Find the QueryParameter registered under the given position-label, if one.
	 *
	 * @return The registered match, or {@code null} is there is no match
	 *
	 * @see #getQueryParameter(int)
	 */
	QueryParameter<?> findQueryParameter(int positionLabel);

	/**
	 * Get the QueryParameter reference registered here under the given position-label.
	 *
	 * @return The registered match.  Never {@code null}
	 *
	 * @throws IllegalArgumentException if no parameter is registered under that position-label
	 */
	QueryParameter<?> getQueryParameter(int positionLabel);

	/**
	 * A deeper resolution attempt from a JPA parameter reference to Hibernate's
	 * contract.  Generally should return the same param reference.
	 *
	 * According to the spec, only Parameter references obtained from the provider
	 * are valid.
	 */
	<P> QueryParameter<P> resolve(Parameter<P> param);

	default <T> BindableType<T> getInferredParameterType(QueryParameter<T> parameter) {
		return null;
	}

	/**
	 * Is this parameter reference registered in this collection?
	 */
	boolean containsReference(QueryParameter<?> parameter);

	Set<? extends QueryParameter<?>> getRegistrations();

	/**
	 * General purpose visitation using functional
	 */
	void visitRegistrations(Consumer<? extends QueryParameter<?>> action);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
	// Named parameters

	/**
	 * Does this parameter set contain any named parameters?
	 *
	 * @return {@code true} if there are named parameters; {@code false} otherwise.
	 */
	boolean hasNamedParameters();

	/**
	 * Return the names of all named parameters of the query.
	 *
	 * @return the parameter names
	 */
	Set<String> getNamedParameterNames();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
	// "positional" parameters

	/**
	 * Does this parameter set contain any positional parameters?
	 *
	 * @return {@code true} if there are positional parameters; {@code false} otherwise.
	 */
	boolean hasPositionalParameters();

	Set<Integer> getOrdinalParameterLabels();
}
