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

import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
			// This is needed because until JDK 9 the Reflection API
			// does not use the same caching as used for auto-boxing.
			// See https://bugs.openjdk.java.net/browse/JDK-5043030 for details.
			// The code below can be removed when we move to JDK 9.
			// double and float are intentionally not handled here because
			// the JLS § 5.1.7 does not define caching for boxed values of
			// this types.
			Class<?> type = field.getType();
			if ( type.isPrimitive() ) {
				if ( type == Boolean.TYPE ) {
					return Boolean.valueOf( field.getBoolean( owner ) );
				}
				else if ( type == Byte.TYPE ) {
					return Byte.valueOf( field.getByte( owner ) );
				}
				else if ( type == Character.TYPE ) {
					return Character.valueOf( field.getChar( owner ) );
				}
				else if ( type == Integer.TYPE ) {
					return Integer.valueOf( field.getInt( owner ) );
				}
				else if ( type == Long.TYPE ) {
					return Long.valueOf( field.getLong( owner ) );
				}
				else if ( type == Short.TYPE ) {
					return Short.valueOf( field.getShort( owner ) );
				}
			}
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
	public Object getForInsert(Object owner, Map mergeMap, SharedSessionContractImplementor session) {
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
