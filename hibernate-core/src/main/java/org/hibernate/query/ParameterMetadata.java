/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

/**
 * Access to known information about the parameters for a query.
 *
 * @author Steve Ebersole
 */
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
	 * Return the names of all named parameters of the query.
	 *
	 * @return the parameter names, in no particular order
	 */
	String[] getNamedParameterNames();

	/**
	 * Returns the number of positional parameters.
	 *
	 * @return The number of positional parameters.
	 */
	int getPositionalParameterCount();

	<T> QueryParameter<T> getQueryParameter(String name);

	<T> QueryParameter<T> getQueryParameter(Integer position);
}
