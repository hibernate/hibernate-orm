/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper;

/**
 * Contract for {@link PropertyMapper} implementations to expose whether they should be included
 * as a wrapper for a {@code <dynamic-component/>} mapping.
 *
 * In this mapping, values are actually stored as a key-value pair in a HashMap rather than
 * them being treated as java-bean values using a setter method.
 *
 * @author Chris Cranford
 */
public interface DynamicComponentMapperSupport {
	/**
	 * Mark the property mapper that it wraps a dynamic-component.
	 */
	void markAsDynamicComponentMap();

	/**
	 * Returns whether the property mapper wraps a dynamic-component.
	 */
	boolean isDynamicComponentMap();
}
