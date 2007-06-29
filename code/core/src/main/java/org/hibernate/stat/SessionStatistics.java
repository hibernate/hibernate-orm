//$Id: SessionStatistics.java 6750 2005-05-11 15:26:04Z oneovthafew $
package org.hibernate.stat;

import java.util.Set;

/**
 * Information about the first-level (session) cache
 * for a particular session instance
 * @author Gavin King
 */
public interface SessionStatistics {

	/**
	 * Get the number of entity instances associated with the session
	 */
	public int getEntityCount();
	/**
	 * Get the number of collection instances associated with the session
	 */
	public int getCollectionCount();

	/**
	 * Get the set of all <tt>EntityKey</tt>s
	 * @see org.hibernate.engine.EntityKey
	 */
	public Set getEntityKeys();
	/**
	 * Get the set of all <tt>CollectionKey</tt>s
	 * @see org.hibernate.engine.CollectionKey
	 */
	public Set getCollectionKeys();
	
}
