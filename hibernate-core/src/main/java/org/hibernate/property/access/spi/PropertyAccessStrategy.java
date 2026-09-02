/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Creates [PropertyAccess] instances for persistent attributes using field,
/// JavaBeans-style property, or another access mechanism.
///
/// Implement a strategy to provide a custom access mechanism and select it
/// with [org.hibernate.annotations.AttributeAccessor#strategy()].
///
/// @see org.hibernate.annotations.AttributeAccessor#strategy()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface PropertyAccessStrategy {
	/**
	 * Build a {@link PropertyAccess} for the indicated property
	 *
	 * @param containerJavaType The Java type that contains the property; may be {@code null} for non-pojo cases.
	 * @param propertyName The property name
	 * @param setterRequired Whether it is an error if we are unable to find a corresponding setter
	 *
	 * @return The appropriate PropertyAccess
	 */
	PropertyAccess buildPropertyAccess(Class<?> containerJavaType, String propertyName, boolean setterRequired);
}
