/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import java.util.Map;

import org.hibernate.cache.CacheException;

/**
 * Defines a contract for accessing a particular named region within the 
 * underlying cache implementation.
 *
 * @author Steve Ebersole
 */
public interface Region {
	/**
	 * Retrieve the name of this region.
	 *
	 * @return The region name
	 */
	String getName();

	/**
	 * The "end state" contract of the region's lifecycle.  Called
	 * during {@link org.hibernate.SessionFactory#close()} to give
	 * the region a chance to cleanup.
	 *
	 * @throws org.hibernate.cache.CacheException Indicates problem shutting down
	 */
	void destroy() throws CacheException;

	/**
	 * Determine whether this region contains data for the given key.
	 * <p/>
	 * The semantic here is whether the cache contains data visible for the
	 * current call context.  This should be viewed as a "best effort", meaning
	 * blocking should be avoid if possible.
	 *
	 * @param key The cache key
	 *
	 * @return True if the underlying cache contains corresponding data; false
	 * otherwise.
	 */
	boolean contains(Object key);

	/**
	 * The number of bytes is this cache region currently consuming in memory.
	 *
	 * @return The number of bytes consumed by this region; -1 if unknown or
	 * unsupported.
	 */
	long getSizeInMemory();

	/**
	 * The count of entries currently contained in the regions in-memory store.
	 *
	 * @return The count of entries in memory; -1 if unknown or unsupported.
	 */
	long getElementCountInMemory();

	/**
	 * The count of entries currently contained in the regions disk store.
	 *
	 * @return The count of entries on disk; -1 if unknown or unsupported.
	 */
	long getElementCountOnDisk();

	/**
	 * Get the contents of this region as a map.
	 * <p/>
	 * Implementors which do not support this notion
	 * should simply return an empty map.
	 *
	 * @return The content map.
	 */
	Map toMap();

	/**
	 * Get the next timestamp according to the underlying cache implementor.
	 *
	 * @todo Document the usages of this method so providers know exactly what is expected.
	 *
	 * @return The next timestamp
	 */
	long nextTimestamp();

	/**
	 * Get a timeout value.
	 *
	 * @todo Again, document the usages of this method so providers know exactly what is expected.
	 *
	 * @return The time out value
	 */
	int getTimeout();
}
