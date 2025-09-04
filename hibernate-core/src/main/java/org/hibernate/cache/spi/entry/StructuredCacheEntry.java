/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

	private final EntityPersister persister;

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
		final var map = (Map<?,?>) structured;
		final String subclass = (String) map.get( SUBCLASS_KEY );
		final Object version = map.get( VERSION_KEY );
		final String[] names =
				factory.getMappingMetamodel()
						.getEntityDescriptor( subclass )
						.getPropertyNames();
		final Serializable[] disassembledState = new Serializable[names.length];
		for ( int i = 0; i < names.length; i++ ) {
			disassembledState[i] = (Serializable) map.get( names[i] );
		}
		return new StandardCacheEntryImpl( disassembledState, subclass, version );
	}

	@Override
	public Object structure(Object item) {
		final var entry = (CacheEntry) item;
		final String[] names = persister.getPropertyNames();
		final Map<String,Object> map = new HashMap<>( names.length + 3, 1f );
		map.put( SUBCLASS_KEY, entry.getSubclass() );
		map.put( VERSION_KEY, entry.getVersion() );
		for ( int i=0; i<names.length; i++ ) {
			map.put( names[i], entry.getDisassembledState()[i] );
		}
		return map;
	}
}
