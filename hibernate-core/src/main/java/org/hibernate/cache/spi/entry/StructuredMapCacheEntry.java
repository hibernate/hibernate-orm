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
import java.util.Iterator;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * (De)structure the map type collection attribute that is being cached into 2LC.
 *
 * @author Gavin King
 */
public class StructuredMapCacheEntry implements CacheEntryStructure<CollectionCacheEntry, Map<Serializable,Serializable>> {
	@Override
	public Map<Serializable, Serializable> structure(CollectionCacheEntry entry) {
		final Serializable[] states = entry.getState();
		final Map<Serializable, Serializable> map = new HashMap<Serializable, Serializable>( states.length );
		for ( final Serializable state : states ) {
			map.put( state, state );
		}
		return map;
	}

	@Override
	public CollectionCacheEntry destructure(Map<Serializable, Serializable> map, SessionFactoryImplementor factory) {
		Serializable[] states = new Serializable[map.size() * 2];
		int i = 0;
		for ( final Serializable key : map.keySet() ) {
			states[i++] = key;
			states[i++] = map.get( key );
		}
		return new CollectionCacheEntry( states );
	}

}
