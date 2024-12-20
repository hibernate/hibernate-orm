/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import org.hibernate.property.access.spi.EnhancedSetterImpl;
import org.hibernate.property.access.spi.EnhancedSetterMethodImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessBuildingException;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jakarta.persistence.AccessType;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.ReflectHelper.findSetterMethod;
import static org.hibernate.internal.util.ReflectHelper.getterMethodOrNull;
import static org.hibernate.property.access.internal.AccessStrategyHelper.fieldOrNull;

/**
 * A {@link PropertyAccess} for byte code enhanced entities. Enhanced setter methods ( if available ) are used for
 * property writes. Regular getter methods/fields are used for property access. Based upon PropertyAccessMixedImpl.
 *
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
public class PropertyAccessEnhancedImpl implements PropertyAccess {
	private final PropertyAccessStrategy strategy;

	private final Getter getter;
	private final Setter setter;

	public PropertyAccessEnhancedImpl(
			PropertyAccessStrategy strategy,
			Class<?> containerJavaType,
			String propertyName,
			@Nullable AccessType getterAccessType) {
		this.strategy = strategy;

		final AccessType propertyAccessType = resolveAccessType( getterAccessType, containerJavaType, propertyName );

		switch ( propertyAccessType ) {
			case FIELD: {
				final Field field = fieldOrNull( containerJavaType, propertyName );
				if ( field == null ) {
					throw new PropertyAccessBuildingException(
							"Could not locate field for property named [" + containerJavaType.getName() + "#" + propertyName + "]"
					);
				}
				this.getter = new GetterFieldImpl( containerJavaType, propertyName, field );
				this.setter = new EnhancedSetterImpl( containerJavaType, propertyName, field );
				break;
			}
			case PROPERTY: {
				final Method getterMethod = getterMethodOrNull( containerJavaType, propertyName );
				if ( getterMethod == null ) {
					throw new PropertyAccessBuildingException(
							"Could not locate getter for property named [" + containerJavaType.getName() + "#" + propertyName + "]"
					);
				}
				final Method setterMethod = findSetterMethod( containerJavaType, propertyName, getterMethod.getReturnType() );

				this.getter = new GetterMethodImpl( containerJavaType, propertyName, getterMethod );
				this.setter = new EnhancedSetterMethodImpl( containerJavaType, propertyName, setterMethod );
				break;
			}
			default: {
				throw new PropertyAccessBuildingException(
						"Invalid access type " + propertyAccessType + " for property named [" + containerJavaType.getName() + "#" + propertyName + "]"
				);
			}
		}
	}

	private static AccessType resolveAccessType(@Nullable AccessType getterAccessType, Class<?> containerJavaType, String propertyName) {
		if ( getterAccessType != null ) {
			// this should indicate FIELD access
			return getterAccessType;
		}
		return AccessStrategyHelper.getAccessType( containerJavaType, propertyName );
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
	public Setter getSetter() {
		return setter;
	}
}
