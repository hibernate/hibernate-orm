/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.hibernate.property.access.spi.EnhancedSetterImpl;
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
public class PropertyAccessEnhancedImpl extends PropertyAccessMixedImpl {

	public PropertyAccessEnhancedImpl(
			PropertyAccessStrategy strategy,
			Class containerJavaType,
			String propertyName) {
		super( strategy, containerJavaType, propertyName );
	}

	@Override
	protected Setter fieldSetter(Class<?> containerJavaType, String propertyName, Field field) {
		return resolveEnhancedSetterForField( containerJavaType, propertyName, field );
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

}
