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
import org.hibernate.persister.entity.EntityPersister;

/**
 * Structured CacheEntry format for entities.  Used to store the entry into the second-level cache
 * as a Map so that users can more easily see the cached state.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StructuredCacheEntry implements CacheEntryStructure {
	public static final String SUBCLASS_KEY = "_subclass";
	public static final String VERSION_KEY = "_version";

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
	@SuppressWarnings("unchecked")
	public Object destructure(Object structured, SessionFactoryImplementor factory) {
		final Map map = (Map) structured;
		final String subclass = (String) map.get( SUBCLASS_KEY );
		final Object version = map.get( VERSION_KEY );
		final EntityPersister subclassPersister = factory.getEntityPersister( subclass );
		final String[] names = subclassPersister.getPropertyNames();
		final Serializable[] disassembledState = new Serializable[names.length];
		for ( int i = 0; i < names.length; i++ ) {
			disassembledState[i] = (Serializable) map.get( names[i] );
		}
		return new StandardCacheEntryImpl(
			disassembledState,
			subclass,
			version
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object structure(Object item) {
		final CacheEntry entry = (CacheEntry) item;
		final String[] names = persister.getPropertyNames();
		final Map map = new HashMap( names.length + 3, 1f );
		map.put( SUBCLASS_KEY, entry.getSubclass() );
		map.put( VERSION_KEY, entry.getVersion() );
		for ( int i=0; i<names.length; i++ ) {
			map.put( names[i], entry.getDisassembledState()[i] );
		}
		return map;
	}
}
