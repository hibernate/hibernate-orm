/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * @author Gavin King
 */
public class EmbeddedPropertyAccessor implements PropertyAccessor {

	public static final class EmbeddedGetter implements Getter {
		private final Class clazz;
		
		EmbeddedGetter(Class clazz) {
			this.clazz = clazz;
		}

		@Override
		public Object get(Object target) throws HibernateException {
			return target;
		}

		@Override
		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session) {
			return get( target );
		}

		@Override
		public Member getMember() {
			return null;
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
			return clazz;
		}

		@Override
		public String toString() {
			return "EmbeddedGetter(" + clazz.getName() + ')';
		}
	}

	public static final class EmbeddedSetter implements Setter {
		private final Class clazz;
		
		EmbeddedSetter(Class clazz) {
			this.clazz = clazz;
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
		public void set(Object target, Object value, SessionFactoryImplementor factory) {
		}

		@Override
		public String toString() {
			return "EmbeddedSetter(" + clazz.getName() + ')';
		}
	}

	@Override
	public Getter getGetter(Class theClass, String propertyName) throws PropertyNotFoundException {
		return new EmbeddedGetter(theClass);
	}

	@Override
	public Setter getSetter(Class theClass, String propertyName) throws PropertyNotFoundException {
		return new EmbeddedSetter(theClass);
	}

}
