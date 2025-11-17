/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.PropertyAccessSerializationException;

/**
 * Base Serializable form for field (used as Getter or Setter)
 *
 * @author Steve Ebersole
 */
public abstract class AbstractFieldSerialForm implements Serializable {
	private final Class<?> declaringClass;
	private final String fieldName;

	protected AbstractFieldSerialForm(Field field) {
		this( field.getDeclaringClass(), field.getName() );
	}

	protected AbstractFieldSerialForm(Class<?> declaringClass, String fieldName) {
		this.declaringClass = declaringClass;
		this.fieldName = fieldName;
	}

	protected Field resolveField() {
		try {
			final var field = declaringClass.getDeclaredField( fieldName );
			ReflectHelper.ensureAccessibility( field );
			return field;
		}
		catch (NoSuchFieldException e) {
			throw new PropertyAccessSerializationException(
					"Unable to resolve field on deserialization : " + declaringClass.getName() + "#" + fieldName
			);
		}
	}
}
