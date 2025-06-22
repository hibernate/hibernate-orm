/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import jakarta.persistence.metamodel.Type;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.SqmBindableType;

/**
 * Represents a type of argument which can be bound to a positional or named
 * {@linkplain org.hibernate.query.CommonQueryContract query} parameter.
 * <p>
 * An instance of {@code BindableType} may be passed to operations like
 * {@link org.hibernate.query.CommonQueryContract#setParameter(int, Object, Type)} and
 * {@link org.hibernate.query.CommonQueryContract#setParameter(String, Object, Type)}
 * to disambiguate the interpretation of the argument.
 *
 * @implNote Every implementation of {@link Type} must also implement this SPI.
 *
 * @param <J> the type of the parameter
 *
 * @see org.hibernate.type.BasicTypeReference
 * @see org.hibernate.type.StandardBasicTypes
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
@Incubating
public interface BindableType<J> extends Type<J> {
	/**
	 * Resolve this parameter type to the corresponding {@link SqmBindableType}.
	 */
	SqmBindableType<J> resolveExpressible(BindingContext bindingContext);
}
