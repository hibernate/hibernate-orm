/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.cache.entry;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public class StructuredCacheEntry implements CacheEntryStructure {

	private EntityPersister persister;

	public StructuredCacheEntry(EntityPersister persister) {
		this.persister = persister;
	}
	
	public Object destructure(Object item, SessionFactoryImplementor factory) {
		Map map = (Map) item;
		boolean lazyPropertiesUnfetched = ( (Boolean) map.get("_lazyPropertiesUnfetched") ).booleanValue();
		String subclass = (String) map.get("_subclass");
		Object version = map.get("_version");
		EntityPersister subclassPersister = factory.getEntityPersister(subclass);
		String[] names = subclassPersister.getPropertyNames();
		Serializable[] state = new Serializable[names.length];
		for ( int i=0; i<names.length; i++ ) {
			state[i] = (Serializable) map.get( names[i] );
		}
		return new CacheEntry(state, subclass, lazyPropertiesUnfetched, version);
	}

	public Object structure(Object item) {
		CacheEntry entry = (CacheEntry) item;
		String[] names = persister.getPropertyNames();
		Map map = new HashMap(names.length+2);
		map.put( "_subclass", entry.getSubclass() );
		map.put( "_version", entry.getVersion() );
		map.put( "_lazyPropertiesUnfetched", entry.areLazyPropertiesUnfetched() ? Boolean.TRUE : Boolean.FALSE );
		for ( int i=0; i<names.length; i++ ) {
			map.put( names[i], entry.getDisassembledState()[i] );
		}
		return map;
	}
}
