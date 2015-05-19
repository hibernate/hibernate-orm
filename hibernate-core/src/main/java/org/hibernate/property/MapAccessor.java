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
public class MapAccessor implements PropertyAccessor {
	@Override
	public Getter getGetter(Class theClass, String propertyName)
		throws PropertyNotFoundException {
		return new MapGetter(propertyName);
	}

	@Override
	public Setter getSetter(Class theClass, String propertyName)
		throws PropertyNotFoundException {
		return new MapSetter(propertyName);
	}

	public static final class MapSetter implements Setter {
		private String name;

		MapSetter(String name) {
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
		@SuppressWarnings("unchecked")
		public void set(Object target, Object value, SessionFactoryImplementor factory)
			throws HibernateException {
			( (Map) target ).put( name, value );
		}

	}

	public static final class MapGetter implements Getter {
		private String name;

		MapGetter(String name) {
			this.name = name;
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
		public Object get(Object target) throws HibernateException {
			return ( (Map) target ).get(name);
		}

		@Override
		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session) {
			return get( target );
		}

		@Override
		public Class getReturnType() {
			return Object.class;
		}
	}

}
