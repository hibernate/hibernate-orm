/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.cache.CacheException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Tracks the timestamps of the most recent updates to particular tables. It is
 * important that the cache timeout of the underlying cache implementation be set
 * to a higher value than the timeouts of any of the query caches. In fact, we
 * recommend that the the underlying cache not be configured for expiry at all.
 * Note, in particular, that an LRU cache expiry policy is never appropriate.
 *
 * @author Gavin King
 * @author Mikheil Kapanadze
 *
 * @deprecated Use {@link TimestampsCache} instead
 */
@SuppressWarnings("unused")
@Deprecated
public interface UpdateTimestampsCache {
	/**
	 * Get the underlying cache region where data is stored..
	 *
	 * @return The underlying region.
	 */
	TimestampsRegion getRegion();

	/**
	 * Perform pre-invalidation.
	 *
	 * @param spaces The spaces to pre-invalidate
	 *
	 * @throws CacheException Indicated problem delegating to underlying region.
	 */
	void preInvalidate(Serializable[] spaces, SharedSessionContractImplementor session) throws CacheException;

	/**
	 * Perform invalidation.
	 *
	 *
	 * @param spaces The spaces to invalidate.
	 * @param session
	 *
	 * @throws CacheException Indicated problem delegating to underlying region.
	 */
	void invalidate(Serializable[] spaces, SharedSessionContractImplementor session) throws CacheException;

	/**
	 * Perform an up-to-date check for the given set of query spaces.
	 *
	 *
	 * @param spaces The spaces to check
	 * @param timestamp The timestamp against which to check.
	 *
	 * @throws CacheException Indicated problem delegating to underlying region.
	 */
	boolean isUpToDate(Set<Serializable> spaces, Long timestamp, SharedSessionContractImplementor session) throws CacheException;

	/**
	 * Clear the update-timestamps data.
	 *
	 * @throws CacheException Indicates problem delegating call to underlying region.
	 */
	void clear() throws CacheException;

	/**
	 * Destroys the cache.
	 *
	 * @throws CacheException Indicates problem delegating call to underlying region.
	 */
	void destroy();
}
