/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.access;

import jakarta.persistence.Cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Base contract for accessing the cached data for a particular element of
 * the domain model in a transactionally ACID manner.
 *
 * @apiNote Note that the following methods are not considered "transactional"
 * in this sense: {@link #contains}, {@link #lockRegion}, {@link #unlockRegion},
 * {@link #evict}, {@link #evictAll}. The semantics of these methods come from
 * JPA's {@link Cache} contract.
 *
 * @implSpec The "non-transactional" methods noted in the {@code @apiNote}
 * should be implemented to ignore any locking. That is, when {@link #evict}
 * is called, the item should be forcibly removed from the cache regardless
 * of whether anything has locked it.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public interface CachedDomainDataAccess {
	/**
	 * The region containing the data being accessed
	 */
	DomainDataRegion getRegion();

	/**
	 * The type of access implemented
	 */
	AccessType getAccessType();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Transactional

	/**
	 * Attempt to retrieve an object from the cache. Usually used when
	 * attempting to resolve an entity or collection from the second-level
	 * cache.
	 *
	 * @param session Current session.
	 * @param key The key of the item to be retrieved.
	 *
	 * @return the cached data or {@code null}
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	Object get(SharedSessionContractImplementor session, Object key);

	/**
	 * Attempt to cache an object, after loading it from the database.
	 *
	 * @param session Current session.
	 * @param key The item key
	 * @param value The item
	 * @param version the item version number
	 *
	 * @return {@code true} if the object was successfully cached
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version);

	/**
	 * Attempt to cache an object, after loading from the database,
	 * explicitly specifying the {@code minimalPut} behavior.
	 *
	 * @param session Current session.
	 * @param key The item key
	 * @param value The item
	 * @param version the item version number
	 * @param minimalPutOverride Explicit minimalPut flag
	 *
	 * @return {@code true} if the object was successfully cached
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version,
			boolean minimalPutOverride);

	/**
	 * Notify before an attempt to update or delete the keyed object.
	 * This operation is used by "asynchronous" concurrency strategies.
	 * <p>
	 * The returned object must be passed back to {@link #unlockItem},
	 * to release the lock. Concurrency strategies which do not support
	 * client-visible locks may silently return null.
	 *
	 * @param session Current session.
	 * @param key The key of the item to lock
	 * @param version The item's current version value
	 *
	 * @return A representation of our lock on the item; or {@code null}.
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version);

	/**
	 * Notify that an attempt to update or delete the keyed object has
	 * completed, with or without success, after transaction completion.
	 * This operation is used by "asynchronous" concurrency strategies.
	 *
	 * @param session Current session.
	 * @param key The item key
	 * @param lock The lock previously obtained from {@link #lockItem}
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock);

	/**
	 * Notify that an item has become stale, before completion of the
	 * transaction. This operation is used by "synchronous" concurrency
	 * strategies.
	 *
	 * @param session Current session.
	 * @param key The key of the item to remove
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	void remove(SharedSessionContractImplementor session, Object key);

	/**
	 * Remove all data for this accessed type.
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	void removeAll(SharedSessionContractImplementor session);



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Non-transactional

	/**
	 * Determine whether this region contains data for the given key.
	 * <p>
	 * The semantic here is whether the cache contains data visible for the
	 * current call context.  This should be viewed as a "best effort", meaning
	 * blocking should be avoided if possible.
	 *
	 * @param key The cache key
	 *
	 * @return True if the underlying cache contains corresponding data; false
	 * otherwise.
	 */
	boolean contains(Object key);

	/**
	 * Lock the entire region
	 *
	 * @return A representation of our lock on the item; or {@code null}.
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	SoftLock lockRegion();

	/**
	 * Called after we have finished the attempted invalidation of the entire
	 * region
	 *
	 * @param lock The lock previously obtained from {@link #lockRegion}
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	void unlockRegion(SoftLock lock);

	/**
	 * Forcibly evict an item from the cache immediately without regard for transaction
	 * isolation and/or locking.  This behavior is exactly Hibernate legacy behavior, but
	 * it is also required by JPA - so we cannot remove it.
	 * <p>
	 * Used from JPA's {@link jakarta.persistence.Cache#evict(Class, Object)}, as well as the
	 * Hibernate extension {@link org.hibernate.Cache#evictEntityData(Class, Object)}
	 * and {@link org.hibernate.Cache#evictEntityData(String, Object)}
	 *
	 * @param key The key of the item to remove
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	void evict(Object key);

	/**
	 * Forcibly evict all items from the cache immediately without regard for transaction
	 * isolation.  This behavior is exactly Hibernate legacy behavior, but it is also required
	 * by JPA - so we cannot remove it.
	 * <p>
	 * Used from our JPA impl of {@link Cache#evictAll()} as well as the Hibernate
	 * extensions {@link org.hibernate.Cache#evictEntityData(Class)},
	 * {@link org.hibernate.Cache#evictEntityData(String)} and
	 * {@link org.hibernate.Cache#evictEntityData()}
	 *
	 * @throws CacheException Propagated from underlying cache provider
	 */
	void evictAll();
}
