/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.entry;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;

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
		final Map map = CollectionHelper.mapOfSize( state.length );
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
