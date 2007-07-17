//$Id: UniqueKeyLoadable.java 5732 2005-02-14 15:53:24Z oneovthafew $
package org.hibernate.persister.entity;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Gavin King
 */
public interface UniqueKeyLoadable extends Loadable {
	/**
	 * Load an instance of the persistent class, by a unique key other
	 * than the primary key.
	 */
	public Object loadByUniqueKey(String propertyName, Object uniqueKey, SessionImplementor session) 
	throws HibernateException;
	/**
	 * Get the property number of the unique key property
	 */
	public int getPropertyIndex(String propertyName);

}
