/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.hibernate.PropertyNotFoundException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessBuildingException;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterFieldImpl;
import org.hibernate.property.access.spi.SetterMethodImpl;

/**
 * A PropertyAccess based on mix of getter/setter method and/or field.
 *
 * @author Steve Ebersole
 */
public class PropertyAccessMixedImpl implements PropertyAccess {
	private final PropertyAccessStrategyMixedImpl strategy;

	private final Getter getter;
	private final Setter setter;

	public PropertyAccessMixedImpl(
			PropertyAccessStrategyMixedImpl strategy,
			Class containerJavaType,
			String propertyName) {
		this.strategy = strategy;

		final Field field = fieldOrNull( containerJavaType, propertyName );
		final Method getterMethod = getterMethodOrNull( containerJavaType, propertyName );

		final Class propertyJavaType;

		// need one of field or getterMethod to be non-null
		if ( field == null && getterMethod == null ) {
			throw new PropertyAccessBuildingException(
					"Could not locate field nor getter method for property named [" + containerJavaType.getName() +
							"#" + propertyName + "]"
			);
		}
		else if ( field != null ) {
			propertyJavaType = field.getType();
			this.getter = new GetterFieldImpl( containerJavaType, propertyName, field );
		}
		else {
			propertyJavaType = getterMethod.getReturnType();
			this.getter = new GetterMethodImpl( containerJavaType, propertyName, getterMethod );
		}

		final Method setterMethod = setterMethodOrNull( containerJavaType, propertyName, propertyJavaType );

		// need one of field or setterMethod to be non-null
		if ( field == null && setterMethod == null ) {
			throw new PropertyAccessBuildingException(
					"Could not locate field nor setter method for property named [" + containerJavaType.getName() +
							"#" + propertyName + "]"
			);
		}
		else if ( field != null ) {
			this.setter = new SetterFieldImpl( containerJavaType, propertyName, field );
		}
		else {
			this.setter = new SetterMethodImpl( containerJavaType, propertyName, setterMethod );
		}
	}

	private static Field fieldOrNull(Class containerJavaType, String propertyName) {
		try {
			return ReflectHelper.findField( containerJavaType, propertyName );
		}
		catch (PropertyNotFoundException e) {
			return null;
		}
	}

	private static Method getterMethodOrNull(Class containerJavaType, String propertyName) {
		try {
			return ReflectHelper.findGetterMethod( containerJavaType, propertyName );
		}
		catch (PropertyNotFoundException e) {
			return null;
		}
	}

	private static Method setterMethodOrNull(Class containerJavaType, String propertyName, Class propertyJavaType) {
		try {
			return ReflectHelper.findSetterMethod( containerJavaType, propertyName, propertyJavaType );
		}
		catch (PropertyNotFoundException e) {
			return null;
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
}
