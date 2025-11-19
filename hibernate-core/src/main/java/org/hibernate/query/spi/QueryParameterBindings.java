/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.function.BiConsumer;

import org.hibernate.Incubating;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.internal.QueryParameterBindingsImpl;

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
	boolean isBound(QueryParameterImplementor<?> parameter);

	/**
	 * Access to the binding via QueryParameter reference
	 *
	 * @param parameter The QueryParameter reference
	 *
	 * @return The binding, or {@code null} if not yet bound
	 */
	default <P> QueryParameterBinding<P> getBinding(QueryParameter<P> parameter) {
		return getBinding( (QueryParameterImplementor<P>) parameter );
	}

	/**
	 * Access to the binding via QueryParameter reference
	 *
	 * @param parameter The QueryParameter reference
	 *
	 * @return The binding, or {@code null} if not yet bound
	 */
	<P> QueryParameterBinding<P> getBinding(QueryParameterImplementor<P> parameter);

	/**
	 * Access to the binding via name
	 *
	 * @param name The parameter name
	 *
	 * @return The binding, or {@code null} if not yet bound
	 */
	<P> QueryParameterBinding<P> getBinding(String name);

	/**
	 * Access to the binding via position
	 *
	 * @param position The parameter position
	 *
	 * @return The binding, or {@code null} if not yet bound
	 */
	<P> QueryParameterBinding<P> getBinding(int position);

	/**
	 * Validate the bindings.  Called just before execution
	 */
	void validate();

	boolean hasAnyMultiValuedBindings();

	boolean hasAnyTransientEntityBindings(SharedSessionContractImplementor session);

	/**
	 * Generate a "memento" for these parameter bindings that can be used
	 * in creating a {@link QueryKey}
	 */
	QueryKey.ParameterBindingsMemento generateQueryKeyMemento(SharedSessionContractImplementor session);

	void visitBindings(BiConsumer<? super QueryParameter<?>, ? super QueryParameterBinding<?>> action);

	QueryKey.ParameterBindingsMemento NO_PARAMETER_BINDING_MEMENTO = new QueryKey.ParameterBindingsMemento(){
	};

	/**
	 * @deprecated Use {@link #empty()} instead.
	 *             Currently unused and can be safely removed.
	 */
	@Deprecated(forRemoval = true, since = "6.6")
	@SuppressWarnings({"rawtypes", "unchecked"})
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

		@Override
		public boolean hasAnyMultiValuedBindings() {
			return false;
		}

		@Override
		public boolean hasAnyTransientEntityBindings(SharedSessionContractImplementor session) {
			return false;
		}

		@Override
		public QueryKey.ParameterBindingsMemento generateQueryKeyMemento(SharedSessionContractImplementor session) {
			return NO_PARAMETER_BINDING_MEMENTO;
		}
	};

	static QueryParameterBindings empty() {
		return QueryParameterBindingsImpl.EMPTY;
	}

}
