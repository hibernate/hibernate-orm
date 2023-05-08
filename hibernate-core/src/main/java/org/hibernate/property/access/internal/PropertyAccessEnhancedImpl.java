/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.EnhancedSetterImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jakarta.persistence.AccessType;

/**
 * A {@link PropertyAccess} for byte code enhanced entities. Enhanced setter methods ( if available ) are used for
 * property writes. Regular getter methods/fields are used for property access. Based upon PropertyAccessMixedImpl.
 *
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
public class PropertyAccessEnhancedImpl extends PropertyAccessMixedImpl {

	public PropertyAccessEnhancedImpl(
			PropertyAccessStrategy strategy,
			Class<?> containerJavaType,
			String propertyName) {
		super( strategy, containerJavaType, propertyName );
	}

	@Override
	protected Method getterOrNull(Class<?> containerJavaType, String propertyName) {
		// expand catch from PropertyNotFoundException to the broader MappingException

		try {
			return ReflectHelper.getterMethodOrNull( containerJavaType, propertyName );
		}
		catch (MappingException e) {
			return null;
		}
	}

	@Override
	protected Setter fieldSetter(Class<?> containerJavaType, String propertyName, Field field) {
		return new EnhancedSetterImpl( containerJavaType, propertyName, field );
	}
}
