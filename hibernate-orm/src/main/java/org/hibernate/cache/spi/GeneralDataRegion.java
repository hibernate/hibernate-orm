/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Contract for general-purpose cache regions.
 *
 * @author Steve Ebersole
 */
public interface GeneralDataRegion extends Region {

	/**
	 * Get an item from the cache.
	 *
	 * @param session
	 * @param key The key of the item to be retrieved.
	 *
	 * @return the cached object or <tt>null</tt>
	 *
	 * @throws org.hibernate.cache.CacheException Indicates a problem accessing the item or region.
	 */
	Object get(SharedSessionContractImplementor session, Object key) throws CacheException;

	/**
	 * Put an item into the cache.
	 *
	 * @param session
	 * @param key The key under which to cache the item.
	 * @param value The item to cache.
	 *
	 * @throws CacheException Indicates a problem accessing the region.
	 */
	void put(SharedSessionContractImplementor session, Object key, Object value) throws CacheException;

	/**
	 * Evict an item from the cache immediately (without regard for transaction
	 * isolation).
	 *
	 * @param key The key of the item to remove
	 * @throws CacheException Indicates a problem accessing the item or region.
	 */
	void evict(Object key) throws CacheException;

	/**
	 * Evict all contents of this particular cache region (without regard for transaction
	 * isolation).
	 *
	 * @throws CacheException Indicates problem accessing the region.
	 */
	void evictAll() throws CacheException;
}
