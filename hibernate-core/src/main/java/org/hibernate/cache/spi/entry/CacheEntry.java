/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
