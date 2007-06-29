//$Id$
package org.hibernate.cache.entry;

import org.hibernate.engine.SessionFactoryImplementor;



/**
 * @author Gavin King
 */
public interface CacheEntryStructure {
	public Object structure(Object item);
	public Object destructure(Object map, SessionFactoryImplementor factory);
}
