package org.hibernate.property;

import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;

/**
 * Used to declare properties not represented at the pojo level
 * 
 * @author Michael Bartmann
 */
public class NoopAccessor implements PropertyAccessor {

	public Getter getGetter(Class arg0, String arg1) throws PropertyNotFoundException {
		return new NoopGetter();
	}

	public Setter getSetter(Class arg0, String arg1) throws PropertyNotFoundException {
		return new NoopSetter();
	}

	/**
	 * A Getter which will always return null. It should not be called anyway.
	 */
	private static class NoopGetter implements Getter {

		/**
		 * @return always null
		 */
		public Object get(Object target) throws HibernateException {
			return null;
		}

		public Object getForInsert(Object target, Map map, SessionImplementor arg1)
				throws HibernateException {
			return null;
		}

		public Class getReturnType() {
			return Object.class;
		}

		public String getMethodName() {
			return null;
		}

		public Method getMethod() {
			return null;
		}

	}

	/**
	 * A Setter which will just do nothing.
	 */
	private static class NoopSetter implements Setter {

		public void set(Object target, Object value, SessionFactoryImplementor arg2)
				throws HibernateException {
			// do not do anything
		}

		public String getMethodName() {
			return null;
		}

		public Method getMethod() {
			return null;
		}

	}
}
