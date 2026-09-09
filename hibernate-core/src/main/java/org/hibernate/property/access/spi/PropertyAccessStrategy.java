/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

/**
 * Describes a strategy for accessing a persistent attribute,
 * for example: field, JavaBean-style property, or whatever.
 * <p>
 * Acts as a factory for {@link PropertyAccess} instances.
 */
public interface PropertyAccessStrategy {
	/**
	 * Build a {@link PropertyAccess} for the indicated property
	 *
	 * @param containerJavaType The Java type that contains the property; may be {@code null} for non-pojo cases.
	 * @param propertyName The property name
	 * @param setterRequired Whether it is an error if we are unable to find a corresponding setter
	 *
	 * @return The appropriate PropertyAccess
	 * @deprecated Will be removed in 8.0
	 */
	@Deprecated(since ="7.4", forRemoval = true)
	PropertyAccess buildPropertyAccess(Class<?> containerJavaType, String propertyName, boolean setterRequired);
}
