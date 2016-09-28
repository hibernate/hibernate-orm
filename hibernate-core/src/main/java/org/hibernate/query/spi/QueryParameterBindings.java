/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.query.QueryParameter;

/**
 * Manages all the parameter bindings for a particular query.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameterBindings {
	/**
	 * Has binding been done for the given parameter.  Handles
	 * cases where we do not (yet) have a binding object as well
	 * by simply returning false.
	 *
	 * @param parameter The parameter to check for a binding
	 *
	 * @return {@code true} if its value has been bound; {@code false}
	 * otherwise.
	 */
	boolean isBound(QueryParameter parameter);

	/**
	 * Access to the binding via QueryParameter reference
	 *
	 * @param parameter The QueryParameter reference
	 *
	 * @return The binding, or {@code null} if not yet bound
	 */
	<T> QueryParameterBinding<T> getBinding(QueryParameter<T> parameter);

	/**
	 * Access to the binding via name
	 *
	 * @param name The parameter name
	 *
	 * @return The binding, or {@code null} if not yet bound
	 */
	<T> QueryParameterBinding<T> getBinding(String name);

	/**
	 * Access to the binding via position
	 *
	 * @param position The parameter position
	 *
	 * @return The binding, or {@code null} if not yet bound
	 */
	<T> QueryParameterBinding getBinding(int position);

	/**
	 * Validate the bindings.  Called just before execution
	 */
	void validate();
}
