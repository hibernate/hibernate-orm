/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

/**
 * A cached instance of a persistent class
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface CacheEntry extends Serializable {
	/**
	 * Does this entry represent a direct entity reference (rather than disassembled state)?
	 *
	 * @return true/false
	 */
	public boolean isReferenceEntry();

	/**
	 * Hibernate stores all entries pertaining to a given entity hierarchy in a single region.  This attribute
	 * tells us the specific entity type represented by the cached data.
	 *
	 * @return The entry's exact entity type.
	 */
	public String getSubclass();

	/**
	 * Retrieves the version (optimistic locking) associated with this cache entry.
	 *
	 * @return The version of the entity represented by this entry
	 */
	public Object getVersion();

	/**
	 * Does the represented data contain any un-fetched attribute values?
	 *
	 * @return true/false
	 */
	public boolean areLazyPropertiesUnfetched();

	/**
	 * Get the underlying disassembled state
	 *
	 * todo : this was added to support initializing an entity's EntityEntry snapshot during reattach;
	 * this should be refactored to instead expose a method to assemble a EntityEntry based on this
	 * state for return.
	 *
	 * @return The disassembled state
	 */
	public Serializable[] getDisassembledState();

}
