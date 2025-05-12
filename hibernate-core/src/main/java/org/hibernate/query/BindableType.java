/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.persistence.metamodel.Type;
import org.hibernate.Incubating;

/**
 * Represents a type which can be bound to a {@linkplain CommonQueryContract query}
 * parameter. An instance of {@code BindableType} may be passed to operations like
 * {@link CommonQueryContract#setParameter(int, Object, BindableType)} and
 * {@link CommonQueryContract#setParameter(String, Object, BindableType)}.
 *
 * @implNote Every implementation of this interface must also implement
 *           the SPI {@link org.hibernate.query.spi.BindableTypeImplementor}.
 *
 * @see org.hibernate.type.BasicTypeReference
 * @see org.hibernate.type.StandardBasicTypes
 *
 * @author Steve Ebersole
 */
@Incubating
public interface BindableType<J> extends Type<J> {
	/**
	 * The expected Java type of the argument to the query parameter.
	 */
	Class<J> getBindableJavaType();

	@Override
	default Class<J> getJavaType() {
		return getBindableJavaType();
	}
}
