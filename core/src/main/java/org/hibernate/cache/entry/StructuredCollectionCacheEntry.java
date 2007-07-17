//$Id: StructuredCollectionCacheEntry.java 5707 2005-02-13 12:47:01Z oneovthafew $
package org.hibernate.cache.entry;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.hibernate.engine.SessionFactoryImplementor;

/**
 * @author Gavin King
 */
public class StructuredCollectionCacheEntry implements CacheEntryStructure {

	public Object structure(Object item) {
		CollectionCacheEntry entry = (CollectionCacheEntry) item;
		return Arrays.asList( entry.getState() );
	}
	
	public Object destructure(Object item, SessionFactoryImplementor factory) {
		List list = (List) item;
		return new CollectionCacheEntry( list.toArray( new Serializable[list.size()] ) );
	}

}
