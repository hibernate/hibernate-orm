/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.hibernate.property.access.spi.PropertyAccessSerializationException;

/**
 * Abstract serialization replacement for field based Getter and Setter impls.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractFieldSerialForm implements Serializable {
	private final Class declaringClass;
	private final String fieldName;
	private final String methodName;

	protected AbstractFieldSerialForm(Field field, String methodName) {
		this( field.getDeclaringClass(), field.getName(), methodName );
	}

	protected AbstractFieldSerialForm(Class declaringClass, String fieldName, String methodName ) {
		this.declaringClass = declaringClass;
		this.fieldName = fieldName;
		this.methodName = methodName;
	}

	protected Field resolveField() {
		try {
			final Field field = declaringClass.getDeclaredField( fieldName );
			field.setAccessible( true );
			return field;
		}
		catch (NoSuchFieldException e) {
			throw new PropertyAccessSerializationException(
					"Unable to resolve field on deserialization : " + declaringClass.getName() + "#" + fieldName
			);
		}
	}

	@SuppressWarnings("unchecked")
	protected Method resolveMethod() {
		try {
			final Method method = declaringClass.getDeclaredMethod( methodName );
			method.setAccessible( true );
			return method;
		}
		catch (NoSuchMethodException e) {
			throw new PropertyAccessSerializationException(
					"Unable to resolve method on deserialization : " + declaringClass.getName() + "#" + methodName
			);
		}
	}
}
