/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.spi;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

import org.hibernate.PropertyAccessException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.property.access.internal.AbstractFieldSerialForm;

/**
 * Field-based implementation of Setter
 *
 * @author Steve Ebersole
 */
public class SetterFieldImpl implements Setter {
	private final Class containerClass;
	private final String propertyName;
	private final Field field;
	private final Method setterMethod;

	public SetterFieldImpl(Class containerClass, String propertyName, Field field, Method method) {
		this.containerClass = containerClass;
		this.propertyName = propertyName;
		this.field = field;
		this.setterMethod = method;
	}

	@Override
	public void set(Object target, Object value, SessionFactoryImplementor factory) {
		try {
			field.set( target, value );
		}
		catch (Exception e) {
			if (value == null && field.getType().isPrimitive()) {
				throw new PropertyAccessException(
						e,
						String.format(
								Locale.ROOT,
								"Null value was assigned to a property [%s.%s] of primitive type",
								containerClass,
								propertyName
						),
						true,
						containerClass,
						propertyName
				);
			}
			else {
				throw new PropertyAccessException(
						e,
						String.format(
								Locale.ROOT,
								"Could not set field value [%s] value by reflection : [%s.%s]",
								value,
								containerClass,
								propertyName
						),
						true,
						containerClass,
						propertyName
				);
			}
		}
	}

	@Override
	public String getMethodName() {
		return setterMethod != null ? setterMethod.getName() : null;
	}

	@Override
	public Method getMethod() {
		return setterMethod;
	}

	private Object writeReplace() throws ObjectStreamException {
		return new SerialForm( containerClass, propertyName, field, setterMethod );
	}

	private static class SerialForm extends AbstractFieldSerialForm implements Serializable {
		private final Class containerClass;
		private final String propertyName;
		private final String methodName;
		private final Class argumentType;

		private SerialForm(Class containerClass, String propertyName, Field field, Method method) {
			super( field );
			this.containerClass = containerClass;
			this.propertyName = propertyName;
			this.methodName = method != null ? method.getName() : null;
			this.argumentType = method != null ? method.getParameterTypes()[0] : null;
		}

		private Object readResolve() {
			return new SetterFieldImpl( containerClass, propertyName, resolveField(), resolveMethod() );
		}

		@SuppressWarnings("unchecked")
		private Method resolveMethod() {
			if (methodName == null && argumentType == null) {
				return null;
			}
			try {
				final Method method = declaringClass.getDeclaredMethod( methodName, argumentType );
				method.setAccessible( true );
				return method;
			}
			catch (NoSuchMethodException e) {
				throw new PropertyAccessSerializationException(
						"Unable to resolve setter method on deserialization : " + declaringClass.getName() + "#"
								+ methodName + "(" + argumentType.getName() + ")"
				);
			}
		}

	}
}
