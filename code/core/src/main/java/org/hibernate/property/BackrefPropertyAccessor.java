//$Id: BackrefPropertyAccessor.java 7516 2005-07-16 22:20:48Z oneovthafew $
package org.hibernate.property;

import java.lang.reflect.Method;
import java.util.Map;
import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Represents a "back-reference" to the id of a collection owner.
 *
 * @author Gavin King
 */
public class BackrefPropertyAccessor implements PropertyAccessor {

	private final String propertyName;
	private final String entityName;

	/**
	 * A placeholder for a property value, indicating that
	 * we don't know the value of the back reference
	 */
	public static final Serializable UNKNOWN = new Serializable() {
		public String toString() { return "<unknown>"; }
		public Object readResolve() {
			return UNKNOWN;
		}
	};
	
	/**
	 * Constructs a new instance of BackrefPropertyAccessor.
	 *
	 * @param collectionRole The collection role which this back ref references.
	 */
	public BackrefPropertyAccessor(String collectionRole, String entityName) {
		this.propertyName = collectionRole.substring( entityName.length() + 1 );
		this.entityName = entityName;
	}

	public Setter getSetter(Class theClass, String propertyName) {
		return new BackrefSetter();
	}

	public Getter getGetter(Class theClass, String propertyName) {
		return new BackrefGetter();
	}


	/**
	 * The Setter implementation for id backrefs.
	 */
	public static final class BackrefSetter implements Setter {

		public Method getMethod() {
			return null;
		}

		public String getMethodName() {
			return null;
		}

		public void set(Object target, Object value, SessionFactoryImplementor factory) {
			// this page intentionally left blank :)
		}

	}


	/**
	 * The Getter implementation for id backrefs.
	 */
	public class BackrefGetter implements Getter {
		
		public Object getForInsert(Object target, Map mergeMap, SessionImplementor session)
		throws HibernateException {
			if (session==null) {
				return UNKNOWN;
			}
			else {
				return session.getPersistenceContext()
						.getOwnerId( entityName, propertyName, target, mergeMap );
			}
		}

		public Object get(Object target)  {
			return UNKNOWN;
		}

		public Method getMethod() {
			return null;
		}

		public String getMethodName() {
			return null;
		}

		public Class getReturnType() {
			return Object.class;
		}
	}
}

