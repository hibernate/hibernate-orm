/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import jakarta.persistence.AccessType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessBuildingException;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.hibernate.internal.util.ReflectHelper.getterMethodOrNull;

/**
 * A {@link PropertyAccess} based on mix of getter method or field.
 *
 * @author Gavin King
 */
public class PropertyAccessGetterImpl implements PropertyAccess {
	private final PropertyAccessStrategy strategy;

	private final Getter getter;

	public PropertyAccessGetterImpl(PropertyAccessStrategy strategy, Class<?> containerJavaType, String propertyName) {
		this.strategy = strategy;

		final AccessType propertyAccessType = AccessStrategyHelper.getAccessType( containerJavaType, propertyName );
		switch ( propertyAccessType ) {
			case FIELD: {
				final Field field = AccessStrategyHelper.fieldOrNull( containerJavaType, propertyName );
				if ( field == null ) {
					throw new PropertyAccessBuildingException(
							"Could not locate field for property named [" + containerJavaType.getName() + "#" + propertyName + "]"
					);
				}
				getter = fieldGetter( containerJavaType, propertyName, field );
				break;
			}
			case PROPERTY: {
				final Method getterMethod = getterMethodOrNull( containerJavaType, propertyName );
				if ( getterMethod == null ) {
					throw new PropertyAccessBuildingException(
							"Could not locate getter for property named [" + containerJavaType.getName() + "#" + propertyName + "]"
					);
				}
				getter = propertyGetter( containerJavaType, propertyName, getterMethod );
				break;
			}
			default: {
				throw new PropertyAccessBuildingException(
						"Invalid access type " + propertyAccessType + " for property named [" + containerJavaType.getName() + "#" + propertyName + "]"
				);
			}
		}
	}

	// --- //

	private static Getter fieldGetter(Class<?> containerJavaType, String propertyName, Field field) {
		return new GetterFieldImpl( containerJavaType, propertyName, field );
	}

	private static Getter propertyGetter(Class<?> containerJavaType, String propertyName, Method method) {
		return new GetterMethodImpl( containerJavaType, propertyName, method );
	}

	@Override
	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return strategy;
	}

	@Override
	public Getter getGetter() {
		return getter;
	}

	@Override
	public @Nullable Setter getSetter() {
		return null;
	}
}
