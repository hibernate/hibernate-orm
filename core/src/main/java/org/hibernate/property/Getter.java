//$Id: Getter.java 7516 2005-07-16 22:20:48Z oneovthafew $
package org.hibernate.property;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

/**
 * Gets values of a particular property
 *
 * @author Gavin King
 */
public interface Getter extends Serializable {
	/**
	 * Get the property value from the given instance .
	 * @param owner The instance containing the value to be retreived.
	 * @return The extracted value.
	 * @throws HibernateException
	 */
	public Object get(Object owner) throws HibernateException;

	/**
	 * Get the property value from the given owner instance.
	 *
	 * @param owner The instance containing the value to be retreived.
	 * @param mergeMap a map of merged persistent instances to detached instances
	 * @param session The session from which this request originated.
	 * @return The extracted value.
	 * @throws HibernateException
	 */
	public Object getForInsert(Object owner, Map mergeMap, SessionImplementor session) 
	throws HibernateException;

	/**
	 * Get the declared Java type
	 */
	public Class getReturnType();

	/**
	 * Optional operation (return null)
	 */
	public String getMethodName();

	/**
	 * Optional operation (return null)
	 */
	public Method getMethod();
}
