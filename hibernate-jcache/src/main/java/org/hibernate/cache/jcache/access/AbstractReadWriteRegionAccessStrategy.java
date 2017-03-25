/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.cache.jcache.access;

import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.jcache.JCacheMessageLogger;
import org.hibernate.cache.jcache.JCacheTransactionalDataRegion;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.jboss.logging.Logger;

/**
 * @author Alex Snaps
 */
abstract class AbstractReadWriteRegionAccessStrategy<R extends JCacheTransactionalDataRegion> {

	private static final JCacheMessageLogger LOG = Logger.getMessageLogger(
			JCacheMessageLogger.class,
			AbstractReadWriteRegionAccessStrategy.class.getName()
	);

	protected final R region;
	protected final Comparator versionComparator;
	private final UUID uuid = UUID.randomUUID();
	private final AtomicLong nextLockId = new AtomicLong();
	private final AtomicLong nextItemId = new AtomicLong();

	public AbstractReadWriteRegionAccessStrategy(R region) {
		this.versionComparator = region.getCacheDataDescription().getVersionComparator();
		this.region = region;
	}

	public R getRegion() {
		return region;
	}

	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		final Lockable item = (Lockable) region.get( key );

		final boolean readable = item != null && item.isReadable( txTimestamp );
		if ( readable ) {
			return item.getValue();
		}
		else {
			return null;
		}
	}

	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version) throws CacheException {
		while (true) {
			Lockable item = (Lockable) region.get( key );

			if (item == null) {
				/*
				 * If the item is null due a softlock being evicted... then this
				 * is wrong, the in-doubt soft-lock could get replaced with the
				 * old value.  All that can be done from a JCache perspective is
				 * to log a warning.
				 */
				if (region.putIfAbsent( key, new Item( value, version, txTimestamp, nextItemId() ))) {
					return true;
				}
			}
			else if (item.isWriteable( txTimestamp, version, versionComparator )) {
				if (region.replace( key, item, new Item( value, version, txTimestamp, nextItemId() ))) {
					return true;
				}
			}
			else {
				return false;
			}
		}
	}

	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		return putFromLoad( session, key, value, txTimestamp, version );
	}

	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) throws CacheException {
		long timeout = region.nextTimestamp() + region.getTimeout();
		while (true) {
			Lockable item = (Lockable) region.get( key );

			if ( item == null ) {
				/*
				 * What happens here if a previous soft-lock was evicted to make
				 * this null.
				 */
				Lock lock = new Lock(timeout, uuid, nextLockId(), version);
				if (region.putIfAbsent( key, lock )) {
					return lock;
				}
			}
			else {
				Lock lock = item.lock( timeout, uuid, nextLockId() );
				if (region.replace(key, item, lock)) {
					return lock;
				}
			}
		}
	}

	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		while (true) {
			Lockable item = (Lockable) region.get( key );

			if (item != null && item.isUnlockable( lock )) {
				if (region.replace(key, item, ((Lock) item ).unlock(region.nextTimestamp()))) {
					return;
				}
			}
			else {
				handleMissingLock( key, item );
				return;
			}
		}
	}

	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		//this access strategy is asynchronous
	}

	public void removeAll() throws CacheException {
		region.clear();
	}

	public void evict(Object key) throws CacheException {
		region.remove( key );
	}

	public void evictAll() throws CacheException {
		region.clear();
	}

	public SoftLock lockRegion() throws CacheException {
		return null;
	}

	public void unlockRegion(SoftLock lock) throws CacheException {
		region.clear();
	}

	private long nextLockId() {
		return nextLockId.getAndIncrement();
	}

	protected long nextItemId() {
		return nextItemId.getAndIncrement();
	}

	protected void handleMissingLock(Object key, Lockable lock) {
		LOG.missingLock( region, key, lock );
		long ts = region.nextTimestamp() + region.getTimeout();
		// create new lock that times out immediately
		Lock newLock = new Lock( ts, uuid, nextLockId.getAndIncrement(), null ).unlock( ts );
		region.put( key, newLock );
	}

	/**
	 * Interface type implemented by all wrapper objects in the cache.
	 */
	protected static interface Lockable {

		/**
		 * Returns <code>true</code> if the enclosed value can be read by a transaction started at the given time.
		 */
		public boolean isReadable(long txTimestamp);

		/**
		 * Returns <code>true</code> if the enclosed value can be replaced with one of the given version by a
		 * transaction started at the given time.
		 */
		public boolean isWriteable(long txTimestamp, Object version, Comparator versionComparator);

		/**
		 * Returns the enclosed value.
		 */
		public Object getValue();

		/**
		 * Returns <code>true</code> if the given lock can be unlocked using the given SoftLock instance as a handle.
		 */
		public boolean isUnlockable(SoftLock lock);

		/**
		 * Locks this entry, stamping it with the UUID and lockId given, with the lock timeout occuring at the specified
		 * time.  The returned Lock object can be used to unlock the entry in the future.
		 */
		public Lock lock(long timeout, UUID uuid, long lockId);
	}

	/**
	 * Wrapper type representing unlocked items.
	 */
	protected static final class Item implements Serializable, Lockable {
		private static final long serialVersionUID = 1L;
		private final Object value;
		private final Object version;
		private final long timestamp;
		private final long itemId;

		/**
		 * Creates an unlocked item wrapping the given value with a version and creation timestamp.
		 */
		Item(Object value, Object version, long timestamp, long itemId) {
			this.value = value;
			this.version = version;
			this.timestamp = timestamp;
			this.itemId = itemId;
		}

		@Override
		public boolean isReadable(long txTimestamp) {
			return txTimestamp > timestamp;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean isWriteable(long txTimestamp, Object newVersion, Comparator versionComparator) {
			return version != null && versionComparator.compare( version, newVersion ) < 0;
		}

		@Override
		public Object getValue() {
			return value;
		}

		@Override
		public boolean isUnlockable(SoftLock lock) {
			return false;
		}

		@Override
		public Lock lock(long timeout, UUID uuid, long lockId) {
			return new Lock( timeout, uuid, lockId, version );
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			else if (obj instanceof Item) {
				return itemId == ((Item) obj).itemId;
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Long.hashCode( itemId );
		}

		@Override
		public String toString() {
			return "Item{" +
											"value=" + value +
											", version=" + version +
											", timestamp=" + timestamp +
											'}';
		}
	}

	/**
	 * Wrapper type representing locked items.
	 */
	public static final class Lock implements Serializable, Lockable, SoftLock {
		private static final long serialVersionUID = 2L;

		private final UUID sourceUuid;
		private final long lockId;
		private final Object version;

		private final long timeout;
		private final boolean concurrent;
		private final int multiplicity;
		private final long unlockTimestamp;

		/**
		 * Creates a locked item with the given identifiers and object version.
		 */
		public Lock(long timeout, UUID sourceUuid, long lockId, Object version) {
			this(timeout, sourceUuid, lockId, version, 0, 1, false);
		}

		private Lock(long timeout, UUID sourceUuid, long lockId, Object version,
				long unlockTimestamp, int multiplicity, boolean concurrent) {
			this.sourceUuid = sourceUuid;
			this.lockId = lockId;
			this.version = version;

			this.timeout = timeout;
			this.unlockTimestamp = unlockTimestamp;
			this.multiplicity = multiplicity;
			this.concurrent = concurrent;
		}

		@Override
		public boolean isReadable(long txTimestamp) {
			return false;
		}

		@Override
		@SuppressWarnings({ "SimplifiableIfStatement", "unchecked" })
		public boolean isWriteable(long txTimestamp, Object newVersion, Comparator versionComparator) {
			if ( txTimestamp > timeout ) {
				// if timedout then allow write
				return true;
			}
			if ( multiplicity > 0 ) {
				// if still locked then disallow write
				return false;
			}
			return version == null
					? txTimestamp > unlockTimestamp
					: versionComparator.compare( version, newVersion ) < 0;
		}

		@Override
		public Object getValue() {
			return null;
		}

		@Override
		public boolean isUnlockable(SoftLock lock) {
			if ( lock == this ) {
				return true;
			}
			else if ( lock instanceof Lock ) {
				return (lockId == ((Lock) lock).lockId) && sourceUuid.equals(((Lock) lock).sourceUuid);
			}
			else {
				return false;
			}
		}

		@Override
		@SuppressWarnings("SimplifiableIfStatement")
		public boolean equals(Object o) {
			if ( o == this ) {
				return true;
			}
			else if ( o instanceof Lock ) {
				return (lockId == ((Lock)o ).lockId) && sourceUuid.equals( ( (Lock) o ).sourceUuid )
						&& (multiplicity == ((Lock) o).multiplicity);
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			final int hash = ( sourceUuid != null ? sourceUuid.hashCode() : 0 );
			return hash ^ Long.hashCode( lockId );
		}

		/**
		 * Returns true if this Lock has been concurrently locked by more than one transaction.
		 */
		public boolean wasLockedConcurrently() {
			return concurrent;
		}

		@Override
		public Lock lock(long timeout, UUID uuid, long lockId) {
			return new Lock( timeout, this.sourceUuid, this.lockId, this.version,
					0, this.multiplicity + 1, true );
		}

		/**
		 * Unlocks this Lock, and timestamps the unlock event.
		 */
		public Lock unlock(long timestamp) {
			if (multiplicity == 1) {
				return new Lock(timeout, sourceUuid, lockId, version,
						timestamp, 0, concurrent );

			}
			else {
				return new Lock(timeout, sourceUuid, lockId, version,
						0, multiplicity - 1, concurrent );
			}
		}

		@Override
		public String toString() {
			return "Lock Source-UUID:" + sourceUuid + " Lock-ID:" + lockId;
		}
	}
}
