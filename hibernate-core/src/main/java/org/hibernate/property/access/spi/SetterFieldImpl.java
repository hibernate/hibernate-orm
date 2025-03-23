/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import org.hibernate.Internal;
import org.hibernate.PropertyAccessException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.internal.AbstractFieldSerialForm;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

import org.checkerframework.checker.nullness.qual.Nullable;

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
		this.setterMethod = ReflectHelper.setterMethodOrNull( containerClass, propertyName, field.getType() );
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
			if (value == null && field.getType().isPrimitive()) {
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
				final String valueType;
				final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( value );
				if ( lazyInitializer != null ) {
					valueType = lazyInitializer.getEntityName();
				}
				else if ( value != null ) {
					valueType = value.getClass().getTypeName();
				}
				else {
					valueType = "<unknown>";
				}
				throw new PropertyAccessException(
						e,
						String.format(
								Locale.ROOT,
								"Could not set value of type [%s]",
								valueType
						),
						true,
						containerClass,
						propertyName
				);
			}
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

		private Object readResolve() {
			return new SetterFieldImpl( containerClass, propertyName, resolveField() );
		}

	}
}
