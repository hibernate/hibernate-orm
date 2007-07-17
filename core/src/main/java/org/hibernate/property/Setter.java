//$Id: Setter.java 5685 2005-02-12 07:19:50Z steveebersole $
package org.hibernate.property;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Sets values to a particular property.
 * 
 * @author Gavin King
 */
public interface Setter extends Serializable {
	/**
	 * Set the property value from the given instance
	 *
	 * @param target The instance upon which to set the given value.
	 * @param value The value to be set on the target.
	 * @param factory The session factory from which this request originated.
	 * @throws HibernateException
	 */
	public void set(Object target, Object value, SessionFactoryImplementor factory) throws HibernateException;
	/**
	 * Optional operation (return null)
	 */
	public String getMethodName();
	/**
	 * Optional operation (return null)
	 */
	public Method getMethod();
}
