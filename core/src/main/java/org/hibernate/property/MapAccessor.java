//$Id: MapAccessor.java 7516 2005-07-16 22:20:48Z oneovthafew $
package org.hibernate.property;

import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Gavin King
 */
public class MapAccessor implements PropertyAccessor {

	public Getter getGetter(Class theClass, String propertyName)
		throws PropertyNotFoundException {
		return new MapGetter(propertyName);
	}

	public Setter getSetter(Class theClass, String propertyName)
		throws PropertyNotFoundException {
		return new MapSetter(propertyName);
	}

	public static final class MapSetter implements Setter {

		private String name;

		MapSetter(String name) {
			this.name = name;
		}

		public Method getMethod() {
			return null;
		}

		public String getMethodName() {
			return null;
		}

		public void set(Object target, Object value, SessionFactoryImplementor factory)
			throws HibernateException {
			( (Map) target ).put(name, value);
		}

	}

	public static final class MapGetter implements Getter {

		private String name;

		MapGetter(String name) {
			this.name = name;
		}

		public Method getMethod() {
			return null;
		}

		public String getMethodName() {
			return null;
		}

		public Object get(Object target) throws HibernateException {
			return ( (Map) target ).get(name);
		}

		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session) {
			return get( target );
		}

		public Class getReturnType() {
			return Object.class;
		}

	}

}
