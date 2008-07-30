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

import java.io.Serializable;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cache.access.SoftLock;

/**
 * Caches data that is sometimes updated while maintaining the semantics of
 * "read committed" isolation level. If the database is set to "repeatable
 * read", this concurrency strategy <em>almost</em> maintains the semantics.
 * Repeatable read isolation is compromised in the case of concurrent writes.
 * This is an "asynchronous" concurrency strategy.<br>
 * <br>
 * If this strategy is used in a cluster, the underlying cache implementation
 * must support distributed hard locks (which are held only momentarily). This
 * strategy also assumes that the underlying cache implementation does not do
 * asynchronous replication and that state has been fully replicated as soon
 * as the lock is released.
 *
 * @see NonstrictReadWriteCache for a faster algorithm
 * @see CacheConcurrencyStrategy
 */
public class ReadWriteCache implements CacheConcurrencyStrategy {

	private static final Logger log = LoggerFactory.getLogger(ReadWriteCache.class);

	private Cache cache;
	private int nextLockId;

	public ReadWriteCache() {}

	public void setCache(Cache cache) {
		this.cache=cache;
	}

	public Cache getCache() {
		return cache;
	}

	public String getRegionName() {
		return cache.getRegionName();
	}
	
	/**
	 * Generate an id for a new lock. Uniqueness per cache instance is very
	 * desirable but not absolutely critical. Must be called from one of the
	 * synchronized methods of this class.
	 */
	private int nextLockId() {
		if (nextLockId==Integer.MAX_VALUE) nextLockId = Integer.MIN_VALUE;
		return nextLockId++;
	}

	/**
	 * Do not return an item whose timestamp is later than the current
	 * transaction timestamp. (Otherwise we might compromise repeatable
	 * read unnecessarily.) Do not return an item which is soft-locked.
	 * Always go straight to the database instead.<br>
	 * <br>
	 * Note that since reading an item from that cache does not actually
	 * go to the database, it is possible to see a kind of phantom read
	 * due to the underlying row being updated after we have read it
	 * from the cache. This would not be possible in a lock-based
	 * implementation of repeatable read isolation. It is also possible
	 * to overwrite changes made and committed by another transaction
	 * after the current transaction read the item from the cache. This
	 * problem would be caught by the update-time version-checking, if
	 * the data is versioned or timestamped.
	 */
	public synchronized Object get(Object key, long txTimestamp) throws CacheException {

		if ( log.isTraceEnabled() ) log.trace("Cache lookup: " + key);

		/*try {
			cache.lock(key);*/

			Lockable lockable = (Lockable) cache.get(key);

			boolean gettable = lockable!=null && lockable.isGettable(txTimestamp);

			if (gettable) {
				if ( log.isTraceEnabled() ) log.trace("Cache hit: " + key);
				return ( (Item) lockable ).getValue();
			}
			else {
				if ( log.isTraceEnabled() ) {
					if (lockable==null) {
						log.trace("Cache miss: " + key);
					}
					else {
						log.trace("Cached item was locked: " + key);
					}
				}
				return null;
			}
		/*}
		finally {
			cache.unlock(key);
		}*/
	}

	/**
	 * Stop any other transactions reading or writing this item to/from
	 * the cache. Send them straight to the database instead. (The lock
	 * does time out eventually.) This implementation tracks concurrent
	 * locks of transactions which simultaneously attempt to write to an
	 * item.
	 */
	public synchronized SoftLock lock(Object key, Object version) throws CacheException {
		if ( log.isTraceEnabled() ) log.trace("Invalidating: " + key);

		try {
			cache.lock(key);

			Lockable lockable = (Lockable) cache.get(key);
			long timeout = cache.nextTimestamp() + cache.getTimeout();
			final Lock lock = (lockable==null) ?
				new Lock( timeout, nextLockId(), version ) :
				lockable.lock( timeout, nextLockId() );
			cache.update(key, lock);
			return lock;
		}
		finally {
			cache.unlock(key);
		}

	}

	/**
	 * Do not add an item to the cache unless the current transaction
	 * timestamp is later than the timestamp at which the item was
	 * invalidated. (Otherwise, a stale item might be re-added if the
	 * database is operating in repeatable read isolation mode.)
	 * For versioned data, don't add the item unless it is the later
	 * version.
	 */
	public synchronized boolean put(
			Object key, 
			Object value, 
			long txTimestamp, 
			Object version, 
			Comparator versionComparator,
			boolean minimalPut) 
	throws CacheException {
		if ( log.isTraceEnabled() ) log.trace("Caching: " + key);

		try {
			cache.lock(key);

			Lockable lockable = (Lockable) cache.get(key);

			boolean puttable = lockable==null || 
				lockable.isPuttable(txTimestamp, version, versionComparator);

			if (puttable) {
				cache.put( key, new Item( value, version, cache.nextTimestamp() ) );
				if ( log.isTraceEnabled() ) log.trace("Cached: " + key);
				return true;
			}
			else {
				if ( log.isTraceEnabled() ) {
					if ( lockable.isLock() ) {
						log.trace("Item was locked: " + key);
					}
					else {
						log.trace("Item was already cached: " + key);
					}
				}
				return false;
			}
		}
		finally {
			cache.unlock(key);
		}
	}

	/**
	 * decrement a lock and put it back in the cache
	 */
	private void decrementLock(Object key, Lock lock) throws CacheException {
		//decrement the lock
		lock.unlock( cache.nextTimestamp() );
		cache.update(key, lock);
	}

	/**
	 * Release the soft lock on the item. Other transactions may now
	 * re-cache the item (assuming that no other transaction holds a
	 * simultaneous lock).
	 */
	public synchronized void release(Object key, SoftLock clientLock) throws CacheException {
		if ( log.isTraceEnabled() ) log.trace("Releasing: " + key);

		try {
			cache.lock(key);

			Lockable lockable = (Lockable) cache.get(key);
			if ( isUnlockable(clientLock, lockable) ) {
				decrementLock(key, (Lock) lockable);
			}
			else {
				handleLockExpiry(key);
			}
		}
		finally {
			cache.unlock(key);
		}
	}

	void handleLockExpiry(Object key) throws CacheException {
		log.warn("An item was expired by the cache while it was locked (increase your cache timeout): " + key);
		long ts = cache.nextTimestamp() + cache.getTimeout();
		// create new lock that times out immediately
		Lock lock = new Lock( ts, nextLockId(), null );
		lock.unlock(ts);
		cache.update(key, lock);
	}

	public void clear() throws CacheException {
		cache.clear();
	}

	public void remove(Object key) throws CacheException {
		cache.remove(key);
	}

	public void destroy() {
		try {
			cache.destroy();
		}
		catch (Exception e) {
			log.warn("could not destroy cache", e);
		}
	}

	/**
	 * Re-cache the updated state, if and only if there there are
	 * no other concurrent soft locks. Release our lock.
	 */
	public synchronized boolean afterUpdate(Object key, Object value, Object version, SoftLock clientLock) 
	throws CacheException {
		
		if ( log.isTraceEnabled() ) log.trace("Updating: " + key);

		try {
			cache.lock(key);

			Lockable lockable = (Lockable) cache.get(key);
			if ( isUnlockable(clientLock, lockable) ) {
				Lock lock = (Lock) lockable;
				if ( lock.wasLockedConcurrently() ) {
					// just decrement the lock, don't recache
					// (we don't know which transaction won)
					decrementLock(key, lock);
					return false;
				}
				else {
					//recache the updated state
					cache.update( key, new Item( value, version, cache.nextTimestamp() ) );
					if ( log.isTraceEnabled() ) log.trace("Updated: " + key);
					return true;
				}
			}
			else {
				handleLockExpiry(key);
				return false;
			}

		}
		finally {
			cache.unlock(key);
		}
	}

	/**
	 * Add the new item to the cache, checking that no other transaction has
	 * accessed the item.
	 */
	public synchronized boolean afterInsert(Object key, Object value, Object version) 
	throws CacheException {
	
		if ( log.isTraceEnabled() ) log.trace("Inserting: " + key);
		try {
			cache.lock(key);

			Lockable lockable = (Lockable) cache.get(key);
			if (lockable==null) {
				cache.update( key, new Item( value, version, cache.nextTimestamp() ) );
				if ( log.isTraceEnabled() ) log.trace("Inserted: " + key);
				return true;
			}
			else {
				return false;
			}
		}
		finally {
			cache.unlock(key);
		}
	}

	/**
	 * Do nothing.
	 */
	public void evict(Object key) throws CacheException {
		// noop
	}

	/**
	 * Do nothing.
	 */
	public boolean insert(Object key, Object value, Object currentVersion) {
		return false;
	}

	/**
	 * Do nothing.
	 */
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) {
		return false;
	}

	/**
	 * Is the client's lock commensurate with the item in the cache?
	 * If it is not, we know that the cache expired the original
	 * lock.
	 */
	private boolean isUnlockable(SoftLock clientLock, Lockable myLock)
	throws CacheException {
		//null clientLock is remotely possible but will never happen in practice
		return myLock!=null &&
			myLock.isLock() &&
			clientLock!=null &&
			( (Lock) clientLock ).getId()==( (Lock) myLock ).getId();
	}

	public static interface Lockable {
		public Lock lock(long timeout, int id);
		public boolean isLock();
		public boolean isGettable(long txTimestamp);
		public boolean isPuttable(long txTimestamp, Object newVersion, Comparator comparator);
	}

	/**
	 * An item of cached data, timestamped with the time it was cached,.
	 * @see ReadWriteCache
	 */
	public static final class Item implements Serializable, Lockable {

		private final long freshTimestamp;
		private final Object value;
		private final Object version;

		public Item(Object value, Object version, long currentTimestamp) {
			this.value = value;
			this.version = version;
			freshTimestamp = currentTimestamp;
		}
		/**
		 * The timestamp on the cached data
		 */
		public long getFreshTimestamp() {
			return freshTimestamp;
		}
		/**
		 * The actual cached data
		 */
		public Object getValue() {
			return value;
		}

		/**
		 * Lock the item
		 */
		public Lock lock(long timeout, int id) {
			return new Lock(timeout, id, version);
		}
		/**
		 * Not a lock!
		 */
		public boolean isLock() {
			return false;
		}
		/**
		 * Is this item visible to the timestamped
		 * transaction?
		 */
		public boolean isGettable(long txTimestamp) {
			return freshTimestamp < txTimestamp;
		}

		/**
		 * Don't overwite already cached items
		 */
		public boolean isPuttable(long txTimestamp, Object newVersion, Comparator comparator) {
			// we really could refresh the item if it
			// is not a lock, but it might be slower
			//return freshTimestamp < txTimestamp
			return version!=null && comparator.compare(version, newVersion) < 0;
		}

		public String toString() {
			return "Item{version=" + version +
				",freshTimestamp=" + freshTimestamp;
		}
	}

	/**
	 * A soft lock which supports concurrent locking,
	 * timestamped with the time it was released
	 * @author Gavin King
	 */
	public static final class Lock implements Serializable, Lockable, SoftLock {
		private long unlockTimestamp = -1;
		private int multiplicity = 1;
		private boolean concurrentLock = false;
		private long timeout;
		private final int id;
		private final Object version;

		public Lock(long timeout, int id, Object version) {
			this.timeout = timeout;
			this.id = id;
			this.version = version;
		}

		public long getUnlockTimestamp() {
			return unlockTimestamp;
		}
		/**
		 * Increment the lock, setting the
		 * new lock timeout
		 */
		public Lock lock(long timeout, int id) {
			concurrentLock = true;
			multiplicity++;
			this.timeout = timeout;
			return this;
		}
		/**
		 * Decrement the lock, setting the unlock
		 * timestamp if now unlocked
		 * @param currentTimestamp
		 */
		public void unlock(long currentTimestamp) {
			if ( --multiplicity == 0 ) {
				unlockTimestamp = currentTimestamp;
			}
		}

		/**
		 * Can the timestamped transaction re-cache this
		 * locked item now?
		 */
		public boolean isPuttable(long txTimestamp, Object newVersion, Comparator comparator) {
			if (timeout < txTimestamp) return true;
			if (multiplicity>0) return false;
			return version==null ? 
				unlockTimestamp < txTimestamp :
				comparator.compare(version, newVersion) < 0; //by requiring <, we rely on lock timeout in the case of an unsuccessful update!
		}

		/**
		 * Was this lock held concurrently by multiple
		 * transactions?
		 */
		public boolean wasLockedConcurrently() {
			return concurrentLock;
		}
		/**
		 * Yes, this is a lock
		 */
		public boolean isLock() {
			return true;
		}
		/**
		 * locks are not returned to the client!
		 */
		public boolean isGettable(long txTimestamp) {
			return false;
		}

		public int getId() { return id; }

		public String toString() {
			return "Lock{id=" + id +
				",version=" + version +
				",multiplicity=" + multiplicity +
				",unlockTimestamp=" + unlockTimestamp;
		}

	}

	public String toString() {
		return cache + "(read-write)";
	}

}






