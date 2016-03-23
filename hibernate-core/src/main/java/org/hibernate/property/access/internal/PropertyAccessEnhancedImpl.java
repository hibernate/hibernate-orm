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
import org.hibernate.property.access.spi.EnhancedSetterImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessBuildingException;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterFieldImpl;

/**
 * A PropertyAccess for byte code enhanced entities. Enhanced setter methods ( if available ) are used for
 * property writes. Regular getter methods/fields are used for property access. Based upon PropertyAccessMixedImpl.
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

		// need one of field or getterMethod to be non-null
		if ( field == null && getterMethod == null ) {
			throw new PropertyAccessBuildingException(
					String.format(
							"Could not locate field for property [%s] on bytecode-enhanced Class [%s]",
							propertyName,
							containerJavaType.getName()
					)
			);
		}
		else if ( field != null ) {
			this.getter = new GetterFieldImpl( containerJavaType, propertyName, field );
		}
		else {
			this.getter = new GetterMethodImpl( containerJavaType, propertyName, getterMethod );
		}

		this.setter = resolveEnhancedSetterForField( containerJavaType, propertyName, field );
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

	private static Setter resolveEnhancedSetterForField(Class<?> containerClass, String propertyName, Field field) {
		try {
			String enhancedSetterName = EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + propertyName;
			Method enhancedSetter = containerClass.getDeclaredMethod( enhancedSetterName, field.getType() );
			enhancedSetter.setAccessible( true );
			return new EnhancedSetterImpl( containerClass, propertyName, enhancedSetter );
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
