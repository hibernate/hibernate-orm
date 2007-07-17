//$Id: StructuredMapCacheEntry.java 5707 2005-02-13 12:47:01Z oneovthafew $
package org.hibernate.cache.entry;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.engine.SessionFactoryImplementor;

/**
 * @author Gavin King
 */
public class StructuredMapCacheEntry implements CacheEntryStructure {

	public Object structure(Object item) {
		CollectionCacheEntry entry = (CollectionCacheEntry) item;
		Serializable[] state = entry.getState();
		Map map = new HashMap(state.length);
		for ( int i=0; i<state.length; ) {
			map.put( state[i++], state[i++] );
		}
		return map;
	}
	
	public Object destructure(Object item, SessionFactoryImplementor factory) {
		Map map = (Map) item;
		Serializable[] state = new Serializable[ map.size()*2 ];
		int i=0;
		Iterator iter = map.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			state[i++] = (Serializable) me.getKey();
			state[i++] = (Serializable) me.getValue();
		}
		return new CollectionCacheEntry(state);
	}

}
