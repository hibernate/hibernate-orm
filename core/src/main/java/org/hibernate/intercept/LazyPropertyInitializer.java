//$Id: LazyPropertyInitializer.java 9210 2006-02-03 22:15:19Z steveebersole $
package org.hibernate.intercept;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

/**
 * Contract for controlling how lazy properties get initialized.
 * 
 * @author Gavin King
 */
public interface LazyPropertyInitializer {

	/**
	 * Marker value for uninitialized properties
	 */
	public static final Serializable UNFETCHED_PROPERTY = new Serializable() {
		public String toString() {
			return "<lazy>";
		}
		public Object readResolve() {
			return UNFETCHED_PROPERTY;
		}
	};

	/**
	 * Initialize the property, and return its new value
	 */
	public Object initializeLazyProperty(String fieldName, Object entity, SessionImplementor session)
	throws HibernateException;

}
