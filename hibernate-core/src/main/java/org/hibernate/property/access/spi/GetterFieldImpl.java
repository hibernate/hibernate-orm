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
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.property.access.internal.AbstractFieldSerialForm;

/**
 * Field-based implementation of Getter
 *
 * @author Steve Ebersole
 */
public class GetterFieldImpl implements Getter {
	private final Class containerClass;
	private final String propertyName;
	private final Field field;

	public GetterFieldImpl(Class containerClass, String propertyName, Field field) {
		this.containerClass = containerClass;
		this.propertyName = propertyName;
		this.field = field;
	}

	@Override
	public Object get(Object owner) {
		try {
			return field.get( owner );
		}
		catch (Exception e) {
			throw new PropertyAccessException(
					String.format(
							Locale.ROOT,
							"Error accessing field [%s] by reflection for persistent property [%s#%s] : %s",
							field.toGenericString(),
							containerClass.getName(),
							propertyName,
							owner
					),
					e
			);
		}
	}

	@Override
	public Object getForInsert(Object owner, Map mergeMap, SessionImplementor session) {
		return get( owner );
	}

	@Override
	public Class getReturnType() {
		return field.getType();
	}

	@Override
	public Member getMember() {
		return field;
	}

	@Override
	public String getMethodName() {
		return null;
	}

	@Override
	public Method getMethod() {
		return null;
	}

	private Object writeReplace() throws ObjectStreamException {
		return new SerialForm( containerClass, propertyName, field );
	}

	private static class SerialForm extends AbstractFieldSerialForm implements Serializable {
		private final Class containerClass;
		private final String propertyName;

		private SerialForm(Class containerClass, String propertyName, Field field) {
			super( field );
			this.containerClass = containerClass;
			this.propertyName = propertyName;
		}

		private Object readResolve() {
			return new GetterFieldImpl( containerClass, propertyName, resolveField() );
		}
	}
}
