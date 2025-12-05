/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import org.hibernate.Internal;
import org.hibernate.PropertyAccessException;
import org.hibernate.property.access.internal.AbstractFieldSerialForm;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.ReflectHelper.setterMethodOrNull;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Field-based implementation of Setter
 *
 * @author Steve Ebersole
 */
@Internal
public class SetterFieldImpl implements Setter {
	private final Class<?> containerClass;
	private final String propertyName;
	private final Field field;
	private final @Nullable Method setterMethod;

	public SetterFieldImpl(Class<?> containerClass, String propertyName, Field field) {
		this.containerClass = containerClass;
		this.propertyName = propertyName;
		this.field = field;
		this.setterMethod = setterMethodOrNull( containerClass, propertyName, field.getType() );
	}

	public Class<?> getContainerClass() {
		return containerClass;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public Field getField() {
		return field;
	}

	@Override
	public void set(Object target, @Nullable Object value) {
		try {
			field.set( target, value );
		}
		catch (Exception e) {
			if ( value == null && field.getType().isPrimitive() ) {
				throw new PropertyAccessException(
						e,
						String.format(
								Locale.ROOT,
								"Null value was assigned to a property [%s.%s] of primitive type",
								containerClass,
								propertyName
						),
						true,
						containerClass,
						propertyName
				);
			}
			else {
				throw new PropertyAccessException(
						e,
						String.format(
								Locale.ROOT,
								"Could not set value of type [%s]",
								typeName( value )
						),
						true,
						containerClass,
						propertyName
				);
			}
		}
	}

	private static String typeName(@Nullable Object value) {
		final var lazyInitializer = extractLazyInitializer( value );
		if ( lazyInitializer != null ) {
			return lazyInitializer.getEntityName();
		}
		else if ( value != null ) {
			return value.getClass().getTypeName();
		}
		else {
			return "<unknown>";
		}
	}

	@Override
	public @Nullable String getMethodName() {
		return setterMethod != null ? setterMethod.getName() : null;
	}

	@Override
	public @Nullable Method getMethod() {
		return setterMethod;
	}

	@Serial
	private Object writeReplace() {
		return new SerialForm( containerClass, propertyName, field );
	}

	private static class SerialForm extends AbstractFieldSerialForm implements Serializable {
		private final Class<?> containerClass;
		private final String propertyName;


		private SerialForm(Class<?> containerClass, String propertyName, Field field) {
			super( field );
			this.containerClass = containerClass;
			this.propertyName = propertyName;
		}

		@Serial
		private Object readResolve() {
			return new SetterFieldImpl( containerClass, propertyName, resolveField() );
		}

	}
}
