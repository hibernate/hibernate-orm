//$Id: AbstractComponentType.java 7670 2005-07-29 05:36:14Z oneovthafew $
package org.hibernate.type;

import java.lang.reflect.Method;

import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.SessionImplementor;

/**
 * Enables other Component-like types to hold collections and have cascades, etc.
 *
 * @see ComponentType
 * @see AnyType
 * @author Gavin King
 */
public interface AbstractComponentType extends Type {
	/**
	 * Get the types of the component properties
	 */
	public Type[] getSubtypes();
	/**
	 * Get the names of the component properties
	 */
	public String[] getPropertyNames();
	/**
	 * Optional operation
	 * @return nullability of component properties
	 */
	public boolean[] getPropertyNullability();
	/**
	 * Get the values of the component properties of 
	 * a component instance
	 */
	public Object[] getPropertyValues(Object component, SessionImplementor session) throws HibernateException;
	/**
	 * Optional operation
	 */
	public Object[] getPropertyValues(Object component, EntityMode entityMode) throws HibernateException;
	/**
	 * Optional operation
	 */
	public void setPropertyValues(Object component, Object[] values, EntityMode entityMode) throws HibernateException;
	public Object getPropertyValue(Object component, int i, SessionImplementor session) throws HibernateException;
	//public Object instantiate(Object parent, SessionImplementor session) throws HibernateException;
	public CascadeStyle getCascadeStyle(int i);
	public FetchMode getFetchMode(int i);
	public boolean isMethodOf(Method method);
	public boolean isEmbedded();
}
