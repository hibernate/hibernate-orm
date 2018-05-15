/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.function.BiConsumer;

import org.hibernate.Incubating;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.cache.spi.QueryKey;

/**
 * Manages all the parameter bindings for a particular query.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameterBindings<B extends QueryParameterBinding<?>> {
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
	boolean isBound(QueryParameterImplementor parameter);

	/**
	 * Access to the binding via QueryParameter reference
	 *
	 * @param parameter The QueryParameter reference
	 *
	 * @return The binding, or {@code null} if not yet bound
	 */
	B getBinding(QueryParameterImplementor parameter);

	/**
	 * Access to the binding via name
	 *
	 * @param name The parameter name
	 *
	 * @return The binding, or {@code null} if not yet bound
	 */
	B getBinding(String name);

	/**
	 * Access to the binding via position
	 *
	 * @param position The parameter position
	 *
	 * @return The binding, or {@code null} if not yet bound
	 */
	B getBinding(int position);

	/**
	 * Validate the bindings.  Called just before execution
	 */
	void validate();

	/**
	 * Generate a "memento" for these parameter bindings that can be used
	 * in creating a {@link org.hibernate.cache.spi.QueryKey}
	 */
	default QueryKey.ParameterBindingsMemento generateQueryKeyMemento() {
		throw new NotYetImplementedFor6Exception(  );
	}

	default void visitBindings(BiConsumer<QueryParameterImplementor<?>, QueryParameterBinding<?>> action) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@SuppressWarnings("unchecked")
	QueryParameterBindings NO_PARAM_BINDINGS = new QueryParameterBindings() {
		@Override
		public boolean isBound(QueryParameterImplementor parameter) {
			return false;
		}

		@Override
		public QueryParameterBinding<?> getBinding(QueryParameterImplementor parameter) {
			return null;
		}

		@Override
		public QueryParameterBinding<?> getBinding(String name) {
			return null;
		}

		@Override
		public QueryParameterBinding<?> getBinding(int position) {
			return null;
		}

		@Override
		public void visitBindings(BiConsumer action) {
		}

		@Override
		public void validate() {
		}
	};

}
