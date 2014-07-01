/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.hibernate.persister.entity.EntityPersister;

/**
 * Specialized CacheEntry for storing direct references to entity instances.
 *
 * @author Steve Ebersole
 */
public class ReferenceCacheEntryImpl implements CacheEntry {
	private final Object reference;
	// passing the persister avoids a costly persister lookup by class name at cache retrieval time
	private final EntityPersister subclassPersister;

	/**
	 * Constructs a ReferenceCacheEntryImpl
	 *
	 * @param reference The reference entity instance
	 * @param subclassPersister The specific subclass persister
	 */
	public ReferenceCacheEntryImpl(Object reference, EntityPersister subclassPersister) {
		this.reference = reference;
		this.subclassPersister = subclassPersister;
	}

	/**
	 * Provides access to the stored reference.
	 *
	 * @return The stored reference
	 */
	public Object getReference() {
		return reference;
	}

	@Override
	public boolean isReferenceEntry() {
		return true;
	}

	@Override
	public String getSubclass() {
		return subclassPersister.getEntityName();
	}

	public EntityPersister getSubclassPersister() {
		return subclassPersister;
	}

	@Override
	public Object getVersion() {
		// reference data cannot be versioned
		return null;
	}

	@Override
	public boolean areLazyPropertiesUnfetched() {
		// reference data cannot define lazy attributes
		return false;
	}

	@Override
	public Serializable[] getDisassembledState() {
		// reference data is not disassembled into the cache
		return null;
	}
}
