/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * A general read/write abstraction over the specific "cache" object from the caching provider.
 *
 * @apiNote Similar to {@link org.hibernate.cache.spi.access.CachedDomainDataAccess},
 *          some methods handle "transactional" access (access in the scope of a session),
 *          and some are non-"transactional" (for cache management outside a session).
 *
 * @author Steve Ebersole
 */
public interface StorageAccess {
	/**
	 * Get an item from the cache.
	 */
	@Nullable
	Object getFromCache(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Put an item into the cache
	 */
	void putIntoCache(
			@Nonnull Object key,
			@Nonnull Object value,
			@Nonnull SharedSessionContractImplementor session);

	/**
	 * Remove an item from the cache by key
	 */
	default void removeFromCache(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		evictData( key );
	}

	/**
	 * Clear data from the cache
	 */
	default void clearCache(@Nonnull SharedSessionContractImplementor session) {
		evictData();
	}

	/**
	 * Does the cache contain this key?
	 */
	boolean contains(@Nonnull Object key);

	/**
	 * Clear all data regardless of transaction/locking
	 */
	void evictData();

	/**
	 * Remove the entry regardless of transaction/locking
	 */
	void evictData(@Nonnull Object key);

	/**
	 * Release any resources.  Called during cache shutdown
	 */
	void release();
}
