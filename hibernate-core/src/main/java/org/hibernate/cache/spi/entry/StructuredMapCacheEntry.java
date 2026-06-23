/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.entry;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;

import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

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
	@Nonnull
	public Object structure(@Nonnull Object item) {
		final var entry = (CollectionCacheEntry) item;
		final Serializable[] state = entry.getState();
		final Map<Serializable,Serializable> map = mapOfSize( state.length );
		for ( int i = 0; i < state.length; ) {
			map.put( state[i++], state[i++] );
		}
		return map;
	}

	@Override
	@Nonnull
	public Object destructure(@Nonnull Object structured, @Nonnull SessionFactoryImplementor factory) {
		final var map = (Map<?,?>) structured;
		final Serializable[] state = new Serializable[ map.size()*2 ];
		int i = 0;
		for ( var me : map.entrySet() ) {
			state[i++] = (Serializable) me.getKey();
			state[i++] = (Serializable) me.getValue();
		}
		return new CollectionCacheEntry(state);
	}

	private StructuredMapCacheEntry() {
	}
}
