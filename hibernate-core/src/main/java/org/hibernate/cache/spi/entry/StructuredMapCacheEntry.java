/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi.entry;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Structured CacheEntry format for persistent Maps.
 *
 * @author Gavin King
 */
public class StructuredMapCacheEntry implements CacheEntryStructure {
	/**
	 * Access to the singleton reference
	 */
	public static final StructuredMapCacheEntry INSTANCE = new StructuredMapCacheEntry();

	@Override
	@SuppressWarnings("unchecked")
	public Object structure(Object item) {
		final CollectionCacheEntry entry = (CollectionCacheEntry) item;
		final Serializable[] state = entry.getState();
		final Map map = new HashMap( state.length );
		int i = 0;
		while ( i < state.length ) {
			map.put( state[i++], state[i++] );
		}
		return map;
	}

	@Override
	public Object destructure(Object structured, SessionFactoryImplementor factory) {
		final Map<?,?> map = (Map<?,?>) structured;
		final Serializable[] state = new Serializable[ map.size()*2 ];
		int i = 0;
		for ( Map.Entry me : map.entrySet() ) {
			state[i++] = (Serializable) me.getKey();
			state[i++] = (Serializable) me.getValue();
		}
		return new CollectionCacheEntry(state);
	}

	private StructuredMapCacheEntry() {
	}
}
