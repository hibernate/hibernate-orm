/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.PropertyAccessSerializationException;

/**
 * Abstract serialization replacement for field based Getter and Setter impls.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractFieldSerialForm implements Serializable {
	private final Class declaringClass;
	private final String fieldName;

	protected AbstractFieldSerialForm(Field field) {
		this( field.getDeclaringClass(), field.getName() );
	}

	protected AbstractFieldSerialForm(Class declaringClass, String fieldName) {
		this.declaringClass = declaringClass;
		this.fieldName = fieldName;
	}

	protected Field resolveField() {
		try {
			final Field field = declaringClass.getDeclaredField( fieldName );
			ReflectHelper.ensureAccessibility( field );
			return field;
		}
		catch (NoSuchFieldException e) {
			throw new PropertyAccessSerializationException(
					"Unable to resolve field on deserialization : " + declaringClass.getName() + "#" + fieldName
			);
		}
	}
}
