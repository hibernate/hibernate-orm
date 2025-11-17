/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.PropertyAccessSerializationException;

/**
 * Base Serializable form for setter methods
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSetterMethodSerialForm implements Serializable {
	private final Class<?> containerClass;
	private final String propertyName;

	private final Class<?> declaringClass;
	private final String methodName;
	private final Class<?> argumentType;

	public AbstractSetterMethodSerialForm(Class<?> containerClass, String propertyName, Method method) {
		this.containerClass = containerClass;
		this.propertyName = propertyName;
		this.declaringClass = method.getDeclaringClass();
		this.methodName = method.getName();
		this.argumentType = method.getParameterTypes()[0];
	}

	public Class<?> getContainerClass() {
		return containerClass;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public Class<?> getDeclaringClass() {
		return declaringClass;
	}

	public String getMethodName() {
		return methodName;
	}

	public Class<?> getArgumentType() {
		return argumentType;
	}

	protected Method resolveMethod() {
		try {
			final var method = declaringClass.getDeclaredMethod( methodName, argumentType );
			ReflectHelper.ensureAccessibility( method );
			return method;
		}
		catch (NoSuchMethodException e) {
			throw new PropertyAccessSerializationException(
					"Unable to resolve setter method on deserialization : " + declaringClass.getName() + "#"
							+ methodName + "(" + argumentType.getName() + ")"
			);
		}
	}
}
