//$Id: CacheEntryStructure.java 5707 2005-02-13 12:47:01Z oneovthafew $
package org.hibernate.cache.entry;

import org.hibernate.engine.SessionFactoryImplementor;



/**
 * @author Gavin King
 */
public interface CacheEntryStructure {
	public Object structure(Object item);
	public Object destructure(Object map, SessionFactoryImplementor factory);
}
