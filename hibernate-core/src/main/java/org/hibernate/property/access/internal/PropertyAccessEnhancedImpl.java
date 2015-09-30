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
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.EnhancedGetterMethodImpl;
import org.hibernate.property.access.spi.EnhancedSetterMethodImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessBuildingException;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterFieldImpl;

/**
 * A PropertyAccess for byte code enhanced entities. Enhanced getter / setter methods ( if available ) are used for
 * field access. Regular getter / setter methods are used for property access. In both cases, delegates calls to
 * EnhancedMethodGetterImpl / EnhancedMethodGetterImpl. Based upon PropertyAccessMixedImpl.
 *
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
public class PropertyAccessEnhancedImpl implements PropertyAccess {
	private final PropertyAccessStrategyEnhancedImpl strategy;

	private final Getter getter;
	private final Setter setter;

	public PropertyAccessEnhancedImpl(
			PropertyAccessStrategyEnhancedImpl strategy,
			Class containerJavaType,
			String propertyName) {
		this.strategy = strategy;

		final Field field = fieldOrNull( containerJavaType, propertyName );
		final Method getterMethod = getterMethodOrNull( containerJavaType, propertyName );

		final Class propertyJavaType;

		// need one of field or getterMethod to be non-null
		if ( field == null && getterMethod == null ) {
			String msg = String.format( "Could not locate field nor getter method for property named [%s#%s]",
										containerJavaType.getName(),
										propertyName );
			throw new PropertyAccessBuildingException( msg );
		}
		else if ( field != null ) {
			propertyJavaType = field.getType();
			this.getter = resolveGetterForField( containerJavaType, propertyName, field );
		}
		else {
			propertyJavaType = getterMethod.getReturnType();
			this.getter = new EnhancedGetterMethodImpl( containerJavaType, propertyName, getterMethod );
		}

		final Method setterMethod = setterMethodOrNull( containerJavaType, propertyName, propertyJavaType );

		// need one of field or setterMethod to be non-null
		if ( field == null && setterMethod == null ) {
			String msg = String.format( "Could not locate field nor getter method for property named [%s#%s]",
										containerJavaType.getName(),
										propertyName );
			throw new PropertyAccessBuildingException( msg );
		}
		else if ( field != null ) {
			this.setter = resolveSetterForField( containerJavaType, propertyName, field );
		}
		else {
			this.setter = new EnhancedSetterMethodImpl( containerJavaType, propertyName, setterMethod );
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

	//

	private static Getter resolveGetterForField(Class<?> containerClass, String propertyName, Field field) {
		try {
			String enhancedGetterName = EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + propertyName;
			Method enhancedGetter = containerClass.getDeclaredMethod( enhancedGetterName );
			enhancedGetter.setAccessible( true );
			return new EnhancedGetterMethodImpl( containerClass, propertyName, enhancedGetter );
		}
		catch (NoSuchMethodException e) {
			// enhancedGetter = null --- field not enhanced: fallback to reflection using the field
			return new GetterFieldImpl( containerClass, propertyName, field );
		}
	}

	private static Setter resolveSetterForField(Class<?> containerClass, String propertyName, Field field) {
		try {
			String enhancedSetterName = EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + propertyName;
			Method enhancedSetter = containerClass.getDeclaredMethod( enhancedSetterName, field.getType() );
			enhancedSetter.setAccessible( true );
			return new EnhancedSetterMethodImpl( containerClass, propertyName, enhancedSetter );
		}
		catch (NoSuchMethodException e) {
			// enhancedSetter = null --- field not enhanced: fallback to reflection using the field
			return new SetterFieldImpl( containerClass, propertyName, field );
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
