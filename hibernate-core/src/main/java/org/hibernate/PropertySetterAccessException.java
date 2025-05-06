/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.util.Collection;

import org.hibernate.proxy.HibernateProxy;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Thrown when an {@link IllegalArgumentException} occurs calling a property setter method.
 *
 * @author Steve Ebersole
 */
public class PropertySetterAccessException extends PropertyAccessException {
	/**
	 * Constructs a {@code PropertyAccessException} using the specified information.
	 *
	 * @param cause The underlying cause
	 * @param persistentClass The class which is supposed to contain the property in question
	 * @param propertyName The name of the property.
	 * @param expectedType The expected property type
	 * @param target The target, which should be of type 'persistentClass'
	 * @param value The property value we are trying to set
	 */
	public PropertySetterAccessException(
			Throwable cause,
			Class<?> persistentClass,
			String propertyName,
			Class<?> expectedType,
			Object target,
			@Nullable Object value) {
		super(
				cause,
				String.format(
						"IllegalArgumentException occurred while calling setter for property [%s.%s (expected type = %s)]; " +
								"target = [%s], property value = [%s]",
						persistentClass.getName(),
						propertyName,
						expectedType.getName(),
						target,
						loggablePropertyValueString( value )
				),
				true,
				persistentClass,
				propertyName
		);
	}

	public static String loggablePropertyValueString(Object value) {
		return value instanceof Collection || value instanceof HibernateProxy
				? value.getClass().getSimpleName()
				: value.toString();
	}

	@Override
	public String toString() {
		return super.originalMessage();
	}
}
