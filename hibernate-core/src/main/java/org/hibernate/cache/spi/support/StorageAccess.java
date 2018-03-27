/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * A general read/write abstraction over the specific "cache"
 * object from the caching provider
 *
 * @author Steve Ebersole
 */
public interface StorageAccess {
	/**
	 * Does the cache contain this key?
	 */
	boolean contains(Object key);

	/**
	 * Get an item from the cache.
	 */
	Object getFromCache(Object key, SharedSessionContractImplementor session);

	/**
	 * Put an item into the cache
	 */
	void putIntoCache(Object key, Object value, SharedSessionContractImplementor session);

	/**
	 * Remove an item from the cache by key
	 */
	default void removeFromCache(Object key, SharedSessionContractImplementor session) {
		evictData( key );
	}

	/**
	 * Clear data from the cache
	 */
	default void clearCache(SharedSessionContractImplementor session) {
		evictData();
	}

	/**
	 * Clear all data regardless of transaction/locking
	 */
	void evictData();

	/**
	 * Remove the entry regardless of transaction/locking
	 */
	void evictData(Object key);

	/***
	 * Release any resources.  Called during cache shutdown
	 */
	void release();

}
