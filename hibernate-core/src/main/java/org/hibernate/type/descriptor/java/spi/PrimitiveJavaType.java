/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;

import org.hibernate.SPI;
import org.hibernate.type.descriptor.java.BasicJavaType;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Additional descriptor contract for primitive and primitive-wrapper Java
/// types.
///
/// @param <J> the wrapper value type
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT })
public interface PrimitiveJavaType<J extends Serializable> extends BasicJavaType<J> {
	/**
	 * Retrieve the primitive counterpart to the wrapper type identified by
	 * this descriptor
	 *
	 * @return The primitive Java type.
	 */
	Class<?> getPrimitiveClass();

	/**
	 * Get the Java type that describes an array of this type.
	 */
	Class<J[]> getArrayClass();

	/**
	 * Get the Java type that describes an array of this type's primitive variant.
	 */
	Class<?> getPrimitiveArrayClass();
}
