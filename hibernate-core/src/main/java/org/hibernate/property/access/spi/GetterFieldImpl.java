/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.property.access.internal.AbstractFieldSerialForm;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.ReflectHelper.findGetterMethodForFieldAccess;

/**
 * Field-based implementation of Getter
 *
 * @author Steve Ebersole
 */
@Internal
public class GetterFieldImpl implements Getter {
	private final Class<?> containerClass;
	private final String propertyName;
	private final Field field;
	private final @Nullable Method getterMethod;

	public GetterFieldImpl(Class<?> containerClass, String propertyName, Field field) {
		this ( containerClass, propertyName, field, findGetterMethodForFieldAccess( field, propertyName ) );
	}

	GetterFieldImpl(Class<?> containerClass, String propertyName, Field field, Method getterMethod) {
		this.containerClass = containerClass;
		this.propertyName = propertyName;
		this.field = field;
		this.getterMethod = getterMethod;
	}

	@Override
	public @Nullable Object get(Object owner) {
		try {
			return field.get( owner );
		}
		catch (Exception e) {
			throw new PropertyAccessException(
					String.format(
							Locale.ROOT,
							"Error accessing field [%s] by reflection for persistent property [%s#%s] : %s",
							field.toGenericString(),
							containerClass.getName(),
							propertyName,
							owner
					),
					e
			);
		}
	}

	@Override
	public @Nullable Object getForInsert(Object owner, Map<Object, Object> mergeMap, SharedSessionContractImplementor session) {
		return get( owner );
	}

	@Override
	public Class<?> getReturnTypeClass() {
		return field.getType();
	}

	@Override
	public Type getReturnType() {
		return field.getGenericType();
	}

	public Field getField() {
		return field;
	}

	@Override
	public Member getMember() {
		return getField();
	}

	@Override
	public @Nullable String getMethodName() {
		return getterMethod != null ? getterMethod.getName() : null;
	}

	@Override
	public @Nullable Method getMethod() {
		return getterMethod;
	}

	@Serial
	private Object writeReplace() throws ObjectStreamException {
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
			return new GetterFieldImpl( containerClass, propertyName, resolveField() );
		}

	}
}
