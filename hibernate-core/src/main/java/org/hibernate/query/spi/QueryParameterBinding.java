/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.Collection;

import jakarta.persistence.TemporalType;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.QueryArgumentException;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.BindableType;

/**
 * The value and type binding information for a particular query parameter.
 * Can represent both single-valued and multivalued bindings of arguments
 * to a parameter.
 * <p>
 * The operations of this interface attempt to assign argument values to
 * the parameter of type {@code T}. If the argument cannot be coerced to
 * {@code T}, the operation fails and throws {@link QueryArgumentException}.
 *
 * @param <T> The type of the query parameter
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameterBinding<T> {
	/**
	 * The query parameter associated with this binding.
	 */
	QueryParameter<T> getQueryParameter();

	/**
	 * Is any argument (even a {@code null} argument) currently
	 * bound to the parameter? That is, was one of the
	 * {@link #setBindValue} or {@link #setBindValues} methods
	 * execute successfully?
	 */
	boolean isBound();

	/**
	 * Is the binding multivalued?
	 */
	boolean isMultiValued();

	/**
	 * Get the type currently associated with this binding.
	 * By default, this is the type inferred from the parameter.
	 * It may be modified via a call to {@link #setBindValue} or
	 * {@link #setBindValues}.
	 *
	 * @return The currently associated {@link BindableType}
	 */
	@Nullable BindableType<T> getBindType();

	/**
	 * If the parameter is of a temporal type, return the explicitly
	 * specified precision, if any.
	 */
	@Nullable @SuppressWarnings("deprecation") TemporalType getExplicitTemporalPrecision();

	/**
	 * Set argument. If the given value is a {@link Collection},
	 * it might be interpreted as multiple arguments. Use the
	 * inherent type of the parameter.
	 *
	 * @throws QueryArgumentException
	 *        if the value cannot be bound to the parameter
	 */
	default void setBindValue(Object value) {
		setBindValue( value, false );
	}

	/**
	 * Set argument. If the given value is a {@link Collection},
	 * it might be interpreted as multiple arguments. Use the
	 * inherent type of the parameter.
	 *
	 * @param resolveJdbcTypeIfNecessary
	 *        Controls whether the parameter type should be
	 *        resolved if necessary.
	 * @throws QueryArgumentException
	 *        if the value cannot be bound to the parameter
	 */
	void setBindValue(Object value, boolean resolveJdbcTypeIfNecessary);

	/**
	 * Set the argument, specifying an explicit {@link BindableType}.
	 *
	 * @param value The argument
	 * @param clarifiedType The explicit type
	 */
	<A> void setBindValue(A value, @Nullable BindableType<A> clarifiedType);

	/**
	 * Set the argument, specifying an explicit {@link TemporalType}.
	 * If the given value is a {@link Collection}, it might be interpreted
	 * as multiple arguments.
	 *
	 * @param value The argument
	 * @param temporalTypePrecision The explicit temporal type
	 * @throws QueryArgumentException
	 *        if the value cannot be bound to the parameter
	 */
	void setBindValue(Object value, @SuppressWarnings("deprecation") TemporalType temporalTypePrecision);

	/**
	 * Get the argument currently bound to the parameter.
	 *
	 * @return The argument currently bound
	 * @throws IllegalStateException
	 *         if the parameter is multivalued
	 */
	T getBindValue();

	/**
	 * Attempt to set multiple arguments to the parameter. Use the
	 * inherent type of the parameter.
	 *
	 * @param values The arguments
	 * @throws IllegalArgumentException if the parameter is not multivalued
	 * @throws QueryArgumentException
	 *        if one of the values cannot be bound to the parameter
	 */
	void setBindValues(Collection<?> values);

	/**
	 * Attempt to set multiple arguments to the parameter, specifying
	 * an explicit {@link BindableType}.
	 *
	 * @param values The arguments
	 * @param clarifiedType The explicit type
	 * @throws IllegalArgumentException
	 *         if the parameter is not multivalued
	 * @throws QueryArgumentException
	 *        if one of the values cannot be bound to the parameter
	 */
	<A> void setBindValues(Collection<? extends A> values, BindableType<A> clarifiedType);

	/**
	 * Attempt to set multiple arguments to the parameter, specifying
	 * an explicit {@link TemporalType}.
	 *
	 * @param values The arguments
	 * @param temporalTypePrecision The explicit temporal type
	 * @throws IllegalArgumentException
	 *        if the parameter is not multivalued
	 * @throws QueryArgumentException
	 *        if one of the values cannot be bound to the parameter
	 */
	void setBindValues(
			Collection<?> values,
			@SuppressWarnings("deprecation")
			TemporalType temporalTypePrecision);

	/**
	 * Get the arguments currently bound to the parameter.
	 *
	 * @return The arguments currently bound
	 * @throws IllegalArgumentException if the parameter is not multivalued
	 */
	Collection<? extends T> getBindValues();

	/**
	 * Returns the inferred mapping model expressible, i.e.,
	 * the model reference against which this parameter is compared.
	 *
	 * @return the inferred mapping model expressible or {@code null}
	 */
	@Nullable MappingModelExpressible<T> getType();

	/**
	 * Sets the mapping model expressible for this parameter.
	 *
	 * @param type The mapping model expressible
	 * @return Whether the binding type was actually changed
	 */
	boolean setType(@Nullable MappingModelExpressible<T> type);
}
