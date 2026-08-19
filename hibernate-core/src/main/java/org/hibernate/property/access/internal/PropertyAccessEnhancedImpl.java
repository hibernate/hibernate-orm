/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import jakarta.persistence.AccessType;
import jakarta.annotation.Nullable;
import org.hibernate.accessor.HibernateAccessorValueReader;
import org.hibernate.accessor.HibernateAccessorValueWriter;
import org.hibernate.property.access.spi.EnhancedGetterFieldImpl;
import org.hibernate.property.access.spi.EnhancedSetterImpl;
import org.hibernate.property.access.spi.EnhancedSetterMethodImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessBuildingException;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.PropertyAccessorService;
import org.hibernate.property.access.spi.PropertyValueAccessor;
import org.hibernate.property.access.spi.Setter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.hibernate.internal.util.ReflectHelper.findField;
import static org.hibernate.internal.util.ReflectHelper.findSetterMethod;
import static org.hibernate.internal.util.ReflectHelper.getterMethodOrNull;
import static org.hibernate.property.access.internal.AccessStrategyHelper.determineEnhancementState;
import static org.hibernate.property.access.internal.AccessStrategyHelper.fieldOrNull;
import static org.hibernate.property.access.internal.AccessStrategyHelper.getAccessType;

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
	private final PropertyValueAccessor propertyValueAccessor;

	public PropertyAccessEnhancedImpl(
			PropertyAccessorService propertyAccessorService,
			PropertyAccessStrategy strategy,
			Class<?> containerJavaType,
			String propertyName,
			@Nullable AccessType classAccessType) {
		this.strategy = strategy;

		final var propertyAccessType =
				classAccessType == null
						? getAccessType( containerJavaType, propertyName )
						: classAccessType;

		switch ( propertyAccessType ) {
			case FIELD: {
				final var field = fieldOrNull( containerJavaType, propertyName );
				if ( field == null ) {
					throw new PropertyAccessBuildingException(
							"Could not locate field for property named [" + containerJavaType.getName() + "#" + propertyName + "]"
					);
				}
				getter = new GetterFieldImpl( containerJavaType, propertyName, field );
				setter = new EnhancedSetterImpl( containerJavaType, propertyName, field );
				propertyValueAccessor = PropertyValueAccessor.enhanced(
						propertyAccessorService.hibernateAccessorFactory().valueReader( field ),
						propertyAccessorService.hibernateAccessorFactory().valueWriter( field ),
						determineEnhancementState( containerJavaType, field.getType() ),
						propertyName
				);
				break;
			}
			case PROPERTY: {
				final var getterMethod = getterMethodOrNull( containerJavaType, propertyName );
				if ( getterMethod == null ) {
					throw new PropertyAccessBuildingException(
							"Could not locate getter for property named [" + containerJavaType.getName() + "#" + propertyName + "]"
					);
				}

				final HibernateAccessorValueReader<?> reader;
				final HibernateAccessorValueWriter writer;
				final int enhancementState;

				if ( classAccessType != null && getAccessType( containerJavaType, propertyName ) == AccessType.FIELD ) {
					// We need to default to FIELD unless we have an explicit AccessType
					// to avoid unnecessary initializations
					final Field field = findField( containerJavaType, propertyName );
					getter = new EnhancedGetterFieldImpl( containerJavaType, propertyName, field, getterMethod );
					enhancementState = determineEnhancementState( containerJavaType, field.getType() );
					reader = propertyAccessorService.hibernateAccessorFactory().valueReader( field );

					setter = new EnhancedSetterImpl( containerJavaType, propertyName, field );
					writer = propertyAccessorService.hibernateAccessorFactory().valueWriter( field );
				}
				else {
					// when classAccessType is null, know PROPERTY is the explicit access type
					getter = new GetterMethodImpl( containerJavaType, propertyName, getterMethod );
					enhancementState = determineEnhancementState( containerJavaType, getterMethod.getReturnType() );
					reader = propertyAccessorService.hibernateAccessorFactory().valueReader( getterMethod );

					Method setterMethod = findSetterMethod( containerJavaType, propertyName, getterMethod.getReturnType() );
					setter = new EnhancedSetterMethodImpl( containerJavaType, propertyName, setterMethod );
					writer = propertyAccessorService.hibernateAccessorFactory().valueWriter( setterMethod );
				}

				propertyValueAccessor = PropertyValueAccessor.enhanced(
						reader,
						writer,
						enhancementState,
						propertyName
				);
				break;
			}
			default: {
				throw new PropertyAccessBuildingException(
						"Invalid access type " + propertyAccessType + " for property named [" + containerJavaType.getName() + "#" + propertyName + "]"
				);
			}
		}
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

	@Override
	public PropertyValueAccessor getPropertyValueAccessor() {
		return propertyValueAccessor;
	}
}
