/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.cache;

import java.util.Comparator;

import org.hibernate.cache.access.SoftLock;

/**
 * Implementors manage transactional access to cached data. Transactions
 * pass in a timestamp indicating transaction start time. Two different
 * implementation patterns are provided for.<ul>
 * <li>A transaction-aware cache implementation might be wrapped by a
 * "synchronous" concurrency strategy, where updates to the cache are written
 * to the cache inside the transaction.</li>
 * <li>A non transaction-aware cache would be wrapped by an "asynchronous"
 * concurrency strategy, where items are merely "soft locked" during the 
 * transaction and then updated during the "after transaction completion"
 * phase; the soft lock is not an actual lock on the database row -
 * only upon the cached representation of the item.</li>
 * </ul>
 * <p/>
 * In terms of entity caches, the expected call sequences are: <ul>
 * <li><b>DELETES</b> : {@link #lock} -> {@link #evict} -> {@link #release}</li>
 * <li><b>UPDATES</b> : {@link #lock} -> {@link #update} -> {@link #afterUpdate}</li>
 * <li><b>INSERTS</b> : {@link #insert} -> {@link #afterInsert}</li>
 * </ul>
 * <p/>
 * In terms of collection caches, all modification actions actually just
 * invalidate the entry(s).  The call sequence here is:
 * {@link #lock} -> {@link #evict} -> {@link #release}
 * <p/>
 * Note that, for an asynchronous cache, cache invalidation must be a two 
 * step process (lock->release, or lock-afterUpdate), since this is the only 
 * way to guarantee consistency with the database for a nontransactional cache
 * implementation. For a synchronous cache, cache invalidation is a single 
 * step process (evict, or update). Hence, this interface defines a three
 * step process, to cater for both models.
 * <p/>
 * Note that query result caching does not go through a concurrency strategy; they
 * are managed directly against the underlying {@link Cache cache regions}.
 *
 * @deprecated As of 3.3; see <a href="package.html"/> for details.
 */
public interface CacheConcurrencyStrategy {

	/**
	 * Attempt to retrieve an object from the cache. Mainly used in attempting
	 * to resolve entities/collections from the second level cache.
	 *
	 * @param key
	 * @param txTimestamp a timestamp prior to the transaction start time
	 * @return the cached object or <tt>null</tt>
	 * @throws CacheException
	 */
	public Object get(Object key, long txTimestamp) throws CacheException;

	/**
	 * Attempt to cache an object, after loading from the database.
	 *
	 * @param key
	 * @param value
	 * @param txTimestamp a timestamp prior to the transaction start time
	 * @param version the item version number
	 * @param versionComparator a comparator used to compare version numbers
	 * @param minimalPut indicates that the cache should avoid a put is the item is already cached
	 * @return <tt>true</tt> if the object was successfully cached
	 * @throws CacheException
	 */
	public boolean put(
			Object key, 
			Object value, 
			long txTimestamp, 
			Object version, 
			Comparator versionComparator,
			boolean minimalPut) 
	throws CacheException;

	/**
	 * We are going to attempt to update/delete the keyed object. This
	 * method is used by "asynchronous" concurrency strategies.
	 * <p/>
	 * The returned object must be passed back to release(), to release the
	 * lock. Concurrency strategies which do not support client-visible
	 * locks may silently return null.
	 * 
	 * @param key
	 * @param version 
	 * @throws CacheException
	 */
	public SoftLock lock(Object key, Object version) throws CacheException;

	/**
	 * Called after an item has become stale (before the transaction completes).
	 * This method is used by "synchronous" concurrency strategies.
	 */
	public void evict(Object key) throws CacheException;

	/**
	 * Called after an item has been updated (before the transaction completes),
	 * instead of calling evict().
	 * This method is used by "synchronous" concurrency strategies.
	 */
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException;

	/**
	 * Called after an item has been inserted (before the transaction completes),
	 * instead of calling evict().
	 * This method is used by "synchronous" concurrency strategies.
	 */
	public boolean insert(Object key, Object value, Object currentVersion) throws CacheException;
	
	
	/**
	 * Called when we have finished the attempted update/delete (which may or 
	 * may not have been successful), after transaction completion.
	 * This method is used by "asynchronous" concurrency strategies.
	 * @param key
	 * @throws CacheException
	 */
	public void release(Object key, SoftLock lock) throws CacheException;
	/**
	 * Called after an item has been updated (after the transaction completes),
	 * instead of calling release().
	 * This method is used by "asynchronous" concurrency strategies.
	 */
	public boolean afterUpdate(Object key, Object value, Object version, SoftLock lock)
	throws CacheException;
	/**
	 * Called after an item has been inserted (after the transaction completes),
	 * instead of calling release().
	 * This method is used by "asynchronous" concurrency strategies.
	 */
	public boolean afterInsert(Object key, Object value, Object version) 
	throws CacheException;
	
	
	/**
	 * Evict an item from the cache immediately (without regard for transaction
	 * isolation).
	 * @param key
	 * @throws CacheException
	 */
	public void remove(Object key) throws CacheException;
	/**
	 * Evict all items from the cache immediately.
	 * @throws CacheException
	 */
	public void clear() throws CacheException;
	/**
	 * Clean up all resources.
	 */
	public void destroy();
	/**
	 * Set the underlying cache implementation.
	 * @param cache
	 */
	public void setCache(Cache cache);
		
	/**
	 * Get the cache region name
	 */
	public String getRegionName();
	
	/**
	 * Get the wrapped cache implementation
	 */
	public Cache getCache();
}






