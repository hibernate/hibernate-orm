/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util.beans;

import java.lang.reflect.Method;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Describes a single property of a JavaBean.
 * This is a reimplementation to avoid dependency on the {@code java.desktop} module.
 */
public final class PropertyDescriptor {
	private final String name;
	private final @Nullable Method readMethod;
	private final @Nullable Method writeMethod;

	public PropertyDescriptor(final String name, final @Nullable Method readMethod, final @Nullable Method writeMethod) {
		this.name = name;
		this.readMethod = readMethod;
		this.writeMethod = writeMethod;
	}

	/**
	 * Gets the programmatic name of this property.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the method used to read the property value.
	 *
	 * @return The getter method, or null if the property is write-only.
	 */
	public @Nullable Method getReadMethod() {
		return readMethod;
	}

	/**
	 * Gets the method used to write the property value.
	 *
	 * @return The setter method, or null if the property is read-only.
	 */
	public @Nullable Method getWriteMethod() {
		return writeMethod;
	}
}
