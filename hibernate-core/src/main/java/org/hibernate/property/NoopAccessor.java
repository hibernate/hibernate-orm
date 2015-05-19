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
 * Used to declare properties not represented at the pojo level
 * 
 * @author Michael Bartmann
 */
public class NoopAccessor implements PropertyAccessor {
	@Override
	public Getter getGetter(Class arg0, String arg1) throws PropertyNotFoundException {
		return new NoopGetter();
	}

	@Override
	public Setter getSetter(Class arg0, String arg1) throws PropertyNotFoundException {
		return new NoopSetter();
	}

	/**
	 * A Getter which will always return null. It should not be called anyway.
	 */
	private static class NoopGetter implements Getter {
		/**
		 * {@inheritDoc}
		 * <p/>
		 * Here we always return <tt>null</tt>
		 */
		@Override
		public Object get(Object target) throws HibernateException {
			return null;
		}

		@Override
		public Object getForInsert(Object target, Map map, SessionImplementor arg1)
				throws HibernateException {
			return null;
		}

		@Override
		public Class getReturnType() {
			return Object.class;
		}

		@Override
		public Member getMember() {
			return null;
		}

		@Override
		public String getMethodName() {
			return null;
		}

		@Override
		public Method getMethod() {
			return null;
		}
	}

	/**
	 * A Setter which will just do nothing.
	 */
	private static class NoopSetter implements Setter {
		@Override
		public void set(Object target, Object value, SessionFactoryImplementor arg2) {
			// nothing to do
		}

		@Override
		public String getMethodName() {
			return null;
		}

		@Override
		public Method getMethod() {
			return null;
		}

	}
}
