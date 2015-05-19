/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.PropertyAccessException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * Accesses fields directly.
 * @author Gavin King
 */
public class DirectPropertyAccessor implements PropertyAccessor {

	public static final class DirectGetter implements Getter {
		private final transient Field field;
		private final Class clazz;
		private final String name;

		DirectGetter(Field field, Class clazz, String name) {
			this.field = field;
			this.clazz = clazz;
			this.name = name;
		}

		@Override
		public Object get(Object target) throws HibernateException {
			try {
				return field.get(target);
			}
			catch (Exception e) {
				throw new PropertyAccessException(e, "could not get a field value by reflection", false, clazz, name);
			}
		}

		@Override
		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session) {
			return get( target );
		}

		@Override
		public Member getMember() {
			return field;
		}

		@Override
		public Method getMethod() {
			return null;
		}

		@Override
		public String getMethodName() {
			return null;
		}

		@Override
		public Class getReturnType() {
			return field.getType();
		}

		Object readResolve() {
			return new DirectGetter( getField(clazz, name), clazz, name );
		}

		@Override
		public String toString() {
			return "DirectGetter(" + clazz.getName() + '.' + name + ')';
		}
	}

	public static final class DirectSetter implements Setter {
		private final transient Field field;
		private final Class clazz;
		private final String name;
		DirectSetter(Field field, Class clazz, String name) {
			this.field = field;
			this.clazz = clazz;
			this.name = name;
		}

		@Override
		public Method getMethod() {
			return null;
		}

		@Override
		public String getMethodName() {
			return null;
		}

		@Override
		public void set(Object target, Object value, SessionFactoryImplementor factory) throws HibernateException {
			try {
				field.set(target, value);
			}
			catch (Exception e) {
				if(value == null && field.getType().isPrimitive()) {
					throw new PropertyAccessException(
							e, 
							"Null value was assigned to a property of primitive type", 
							true, 
							clazz, 
							name
						);					
				}
				else {
					throw new PropertyAccessException(e, "could not set a field value by reflection", true, clazz, name);
				}
			}
		}

		@Override
		public String toString() {
			return "DirectSetter(" + clazz.getName() + '.' + name + ')';
		}
		
		Object readResolve() {
			return new DirectSetter( getField(clazz, name), clazz, name );
		}
	}

	private static Field getField(Class clazz, String name) throws PropertyNotFoundException {
		if ( clazz==null || clazz==Object.class ) {
			throw new PropertyNotFoundException("field not found: " + name); 
		}
		Field field;
		try {
			field = clazz.getDeclaredField(name);
		}
		catch (NoSuchFieldException nsfe) {
			field = getField( clazz, clazz.getSuperclass(), name );
		}
		field.setAccessible(true);
		return field;
	}

	private static Field getField(Class root, Class clazz, String name) throws PropertyNotFoundException {
		if ( clazz==null || clazz==Object.class ) {
			throw new PropertyNotFoundException("field [" + name + "] not found on " + root.getName()); 
		}
		Field field;
		try {
			field = clazz.getDeclaredField(name);
		}
		catch (NoSuchFieldException nsfe) {
			field = getField( root, clazz.getSuperclass(), name );
		}
		field.setAccessible(true);
		return field;
	}

	@Override
	public Getter getGetter(Class theClass, String propertyName) throws PropertyNotFoundException {
		return new DirectGetter( getField(theClass, propertyName), theClass, propertyName );
	}

	@Override
	public Setter getSetter(Class theClass, String propertyName) throws PropertyNotFoundException {
		return new DirectSetter( getField(theClass, propertyName), theClass, propertyName );
	}

}
