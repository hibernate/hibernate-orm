/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import jakarta.persistence.AccessType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.property.access.spi.EnhancedGetterFieldImpl;
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

import static org.hibernate.internal.util.ReflectHelper.findField;
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
			@Nullable AccessType classAccessType) {
		this.strategy = strategy;

		final AccessType propertyAccessType = classAccessType == null ?
				AccessStrategyHelper.getAccessType( containerJavaType, propertyName ) :
				classAccessType;

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
				this.getter = propertyGetter( classAccessType, containerJavaType, propertyName, getterMethod );
				this.setter = propertySetter( classAccessType, containerJavaType, propertyName, getterMethod.getReturnType() );
				break;
			}
			default: {
				throw new PropertyAccessBuildingException(
						"Invalid access type " + propertyAccessType + " for property named [" + containerJavaType.getName() + "#" + propertyName + "]"
				);
			}
		}
	}

	private static Getter propertyGetter(@Nullable AccessType classAccessType, Class<?> containerJavaType, String propertyName, Method getterMethod) {
		if ( classAccessType != null ) {
			final AccessType explicitAccessType = AccessStrategyHelper.getAccessType( containerJavaType, propertyName );
			if ( explicitAccessType == AccessType.FIELD ) {
				// We need to default to FIELD unless we have an explicit AccessType to avoid unnecessary initializations
				final Field field = findField( containerJavaType, propertyName );
				return new EnhancedGetterFieldImpl( containerJavaType, propertyName, field, getterMethod );
			}
		}
		// when classAccessType is null know PROPERTY is the explicit access type
		return new GetterMethodImpl( containerJavaType, propertyName, getterMethod );
	}

	private static Setter propertySetter(@Nullable AccessType classAccessType, Class<?> containerJavaType, String propertyName, Class<?> fieldType) {
		if ( classAccessType != null ) {
			final AccessType explicitAccessType = AccessStrategyHelper.getAccessType( containerJavaType, propertyName );
			if ( explicitAccessType == AccessType.FIELD ) {
				// We need to default to FIELD unless we have an explicit AccessType to avoid unnecessary initializations
				final Field field = findField( containerJavaType, propertyName );
				return new EnhancedSetterImpl( containerJavaType, propertyName, field );
			}
		}
		// when classAccessType is null know PROPERTY is the explicit access type
		final Method setterMethod = findSetterMethod( containerJavaType, propertyName, fieldType );
		return new EnhancedSetterMethodImpl( containerJavaType, propertyName, setterMethod );
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
