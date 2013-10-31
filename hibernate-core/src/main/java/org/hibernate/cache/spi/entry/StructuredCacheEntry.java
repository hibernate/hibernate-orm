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
import org.hibernate.persister.entity.EntityPersister;

/**
 * Structured CacheEntry format for entities.  Used to store the entry into the second-level cache
 * as a Map so that users can more easily see the cached state.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StructuredCacheEntry implements CacheEntryStructure {
	private EntityPersister persister;

	/**
	 * Constructs a StructuredCacheEntry strategy
	 *
	 * @param persister The persister whose data needs to be structured.
	 */
	public StructuredCacheEntry(EntityPersister persister) {
		this.persister = persister;
	}

	@Override
	public Object destructure(Object structured, SessionFactoryImplementor factory) {
		final Map map = (Map) structured;
		final boolean lazyPropertiesUnfetched = (Boolean) map.get( "_lazyPropertiesUnfetched" );
		final String subclass = (String) map.get( "_subclass" );
		final Object version = map.get( "_version" );
		final EntityPersister subclassPersister = factory.getEntityPersister( subclass );
		final String[] names = subclassPersister.getPropertyNames();
		final Serializable[] state = new Serializable[names.length];
		for ( int i = 0; i < names.length; i++ ) {
			state[i] = (Serializable) map.get( names[i] );
		}
		return new StandardCacheEntryImpl( state, subclass, lazyPropertiesUnfetched, version );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object structure(Object item) {
		final CacheEntry entry = (CacheEntry) item;
		final String[] names = persister.getPropertyNames();
		final Map map = new HashMap( names.length + 3, 1f );
		map.put( "_subclass", entry.getSubclass() );
		map.put( "_version", entry.getVersion() );
		map.put( "_lazyPropertiesUnfetched", entry.areLazyPropertiesUnfetched() );
		for ( int i=0; i<names.length; i++ ) {
			map.put( names[i], entry.getDisassembledState()[i] );
		}
		return map;
	}
}
