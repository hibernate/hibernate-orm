/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property;
import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;

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
	 * Retrieve the member to which this property maps.  This might be the
	 * field or it might be the getter method.
	 *
	 * @return The mapped member.
	 */
	public Member getMember();

	/**
	 * Retrieve the declared Java type
	 *
	 * @return The declared java type.
	 */
	public Class getReturnType();

	/**
	 * Retrieve the getter-method name.
	 * <p/>
	 * Optional operation (return null)
	 *
	 * @return The name of the getter method, or null.
	 */
	public String getMethodName();

	/**
	 * Retrieve the getter-method.
	 * <p/>
	 * Optional operation (return null)
	 *
	 * @return The getter method, or null.
	 */
	public Method getMethod();
}
