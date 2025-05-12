/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.metamodel.Type;
import org.hibernate.type.BindableType;
import org.hibernate.type.BasicTypeReference;

import java.util.Objects;

/**
 * Represents a typed argument to a query parameter.
 * <p>
 * Usually, the {@linkplain org.hibernate.type.Type Hibernate type} of
 * an argument to a query parameter may be inferred, and so it's rarely
 * necessary to explicitly pass a type when binding the argument.
 * Occasionally, and especially when the argument is null, the type
 * cannot be inferred and must be explicitly specified. In such cases,
 * an instance of {@code TypedParameterValue} may be passed to
 * {@link jakarta.persistence.Query#setParameter setParameter()}.
 * <p>
 * For example:
 * <pre>
 * query.setParameter("stringNamedParam",
 *         TypedParameterValue.ofNull(StandardBasicTypes.STRING))
 * </pre>
 * <pre>
 * query.setParameter("address",
 *         TypedParameterValue.of(Address_.class_, address))
 * </pre>
 * <p>
 * Here, a "null string" argument was bound to the named parameter
 * {@code :stringNamedParam}.
 *
 * @author Steve Ebersole
 *
 * @since 6
 *
 * @see jakarta.persistence.Query#setParameter(int, Object)
 * @see jakarta.persistence.Query#setParameter(String, Object)
 * @see CommonQueryContract#setParameter(int, Object)
 * @see CommonQueryContract#setParameter(String, Object)
 *
 * @see org.hibernate.type.StandardBasicTypes
 */
public record TypedParameterValue<J>(BindableType<J> type, J value) {

	public TypedParameterValue {
		Objects.requireNonNull( type, "type must not be null" );
	}

	/**
	 * Obtain an instance with the given type and given value.
	 *
	 * @since 7.0
	 */
	public static <J> TypedParameterValue<J> of(Type<J> type, J value) {
		return new TypedParameterValue<>( (BindableType<J>) type, value );
	}

	/**
	 * Obtain an instance with the given type and a null value.
	 *
	 * @since 7.0
	 */
	public static <J> TypedParameterValue<J> ofNull(Type<J> type) {
		return new TypedParameterValue<>( (BindableType<J>) type, null );
	}

	/**
	 * The value to bind
	 *
	 * @return The value to be bound
	 *
	 * @deprecated use {@link #value}
	 */
	@Deprecated(since = "7")
	public J getValue() {
		return value;
	}

	/**
	 * The specific Hibernate type to use to bind the value.
	 *
	 * @return The Hibernate type to use.
	 *
	 * @deprecated use {@link #type}
	 */
	@Deprecated(since = "7")
	public BindableType<J> getType() {
		return type;
	}

	/**
	 * The specific Hibernate type reference to use to bind the value.
	 *
	 * @return The Hibernate type reference to use.
	 *
	 * @deprecated use {@link #type}
	 */
	@Deprecated(since = "7")
	public BasicTypeReference<J> getTypeReference() {
		return type instanceof BasicTypeReference<J> reference ? reference : null;
	}
}
