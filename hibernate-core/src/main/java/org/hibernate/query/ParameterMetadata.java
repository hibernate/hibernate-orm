/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.persistence.Parameter;

import org.hibernate.Incubating;
import org.hibernate.type.BindableType;


/**
 * Information about the {@linkplain QueryParameter parameters}
 * of a {@linkplain CommonQueryContract query}.
 *
 * @author Steve Ebersole
 *
 * @see CommonQueryContract#getParameterMetadata()
 */
@Incubating
public interface ParameterMetadata {
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`
	// General purpose

	/**
	 * The {@link QueryParameter}s representing the parameters of the query,
	 * in no particular well-defined order.
	 *
	 * @since 7.0
	 */
	Collection<QueryParameter<?>> getParameters();

	/**
	 * The total number of registered parameters.
	 */
	int getParameterCount();

	/**
	 * Find the {@linkplain QueryParameter parameter reference} registered
	 * under the given name, if there is one.
	 *
	 * @return The registered match, or {@code null} is there is no match
	 *
	 * @see #getQueryParameter(String)
	 */
	QueryParameter<?> findQueryParameter(String name);

	/**
	 * Get the {@linkplain QueryParameter parameter reference} registered
	 * under the given name.
	 *
	 * @return The registered match. Never {@code null}
	 *
	 * @throws IllegalArgumentException if no parameter is registered under that name
	 */
	QueryParameter<?> getQueryParameter(String name);

	/**
	 * Find the {@linkplain QueryParameter parameter reference} registered
	 * at the given position-label, if there is one.
	 *
	 * @return The registered match, or {@code null} is there is no match
	 *
	 * @see #getQueryParameter(int)
	 */
	QueryParameter<?> findQueryParameter(int positionLabel);

	/**
	 * Get the {@linkplain QueryParameter parameter reference} registered
	 * at the given position-label.
	 *
	 * @return The registered match. Never {@code null}
	 *
	 * @throws IllegalArgumentException if no parameter is registered under that position-label
	 */
	QueryParameter<?> getQueryParameter(int positionLabel);

	/**
	 * Obtain a {@link QueryParameter} representing the same parameter as the
	 * given JPA-standard {@link Parameter}.
	 *
	 * @apiNote According to the spec, only {@link Parameter} references obtained
	 *          from the provider are valid.
	 */
	<P> QueryParameter<P> resolve(Parameter<P> param);

	/**
	 * Get the type of the given parameter.
	 */
	<T> BindableType<T> getInferredParameterType(QueryParameter<T> parameter);

	/**
	 * Is this parameter reference registered in this collection?
	 */
	boolean containsReference(QueryParameter<?> parameter);

	Set<? extends QueryParameter<?>> getRegistrations();

	/**
	 * General purpose visitation using functional
	 */
	void visitRegistrations(Consumer<QueryParameter<?>> action);


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

	/**
	 * Get the position labels of all positional parameters.
	 *
	 * @return the position labels
	 */
	Set<Integer> getOrdinalParameterLabels();
}
