/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
