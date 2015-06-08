/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 */
abstract class AbstractReadWriteAccessStrategy extends BaseRegionAccessStrategy {
	private static final Logger LOG = Logger.getLogger( AbstractReadWriteAccessStrategy.class.getName() );

	private final UUID uuid = UUID.randomUUID();
	private final AtomicLong nextLockId = new AtomicLong();
	private ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
	protected java.util.concurrent.locks.Lock readLock = reentrantReadWriteLock.readLock();
	protected java.util.concurrent.locks.Lock writeLock = reentrantReadWriteLock.writeLock();

	/**
	 * Returns <code>null</code> if the item is not readable.  Locked items are not readable, nor are items created
	 * after the start of this transaction.
	 */
	@Override
	public final Object get(Object key, long txTimestamp) throws CacheException {
		LOG.debugf( "getting key[%s] from region[%s]", key, getInternalRegion().getName() );
		try {
			readLock.lock();
			Lockable item = (Lockable) getInternalRegion().get( key );

			boolean readable = item != null && item.isReadable( txTimestamp );
			if ( readable ) {
				LOG.debugf( "hit key[%s] in region[%s]", key, getInternalRegion().getName() );
				return item.getValue();
			}
			else {
				if ( item == null ) {
					LOG.debugf( "miss key[%s] in region[%s]", key, getInternalRegion().getName() );
				}
				else {
					LOG.debugf( "hit key[%s] in region[%s], but it is unreadable", key, getInternalRegion().getName() );
				}
				return null;
			}
		}
		finally {
			readLock.unlock();
		}
	}

	abstract Comparator getVersionComparator();

	/**
	 * Returns <code>false</code> and fails to put the value if there is an existing un-writeable item mapped to this
	 * key.
	 */
	@Override
	public final boolean putFromLoad(
			Object key,
			Object value,
			long txTimestamp,
			Object version,
			boolean minimalPutOverride)
			throws CacheException {
		try {
			LOG.debugf( "putting key[%s] -> value[%s] into region[%s]", key, value, getInternalRegion().getName() );
			writeLock.lock();
			Lockable item = (Lockable) getInternalRegion().get( key );
			boolean writeable = item == null || item.isWriteable( txTimestamp, version, getVersionComparator() );
			if ( writeable ) {
				LOG.debugf(
						"putting key[%s] -> value[%s] into region[%s] success",
						key,
						value,
						getInternalRegion().getName()
				);
				getInternalRegion().put( key, new Item( value, version, getInternalRegion().nextTimestamp() ) );
				return true;
			}
			else {
				LOG.debugf(
						"putting key[%s] -> value[%s] into region[%s] fail due to it is unwriteable",
						key,
						value,
						getInternalRegion().getName()
				);
				return false;
			}
		}
		finally {
			writeLock.unlock();
		}
	}

	/**
	 * Soft-lock a cache item.
	 */
	@Override
	public final SoftLock lockItem(Object key, Object version) throws CacheException {

		try {
			LOG.debugf( "locking key[%s] in region[%s]", key, getInternalRegion().getName() );
			writeLock.lock();
			Lockable item = (Lockable) getInternalRegion().get( key );
			long timeout = getInternalRegion().nextTimestamp() + getInternalRegion().getTimeout();
			final Lock lock = ( item == null ) ? new Lock( timeout, uuid, nextLockId(), version ) : item.lock(
					timeout,
					uuid,
					nextLockId()
			);
			getInternalRegion().put( key, lock );
			return lock;
		}
		finally {
			writeLock.unlock();
		}
	}

	/**
	 * Soft-unlock a cache item.
	 */
	@Override
	public final void unlockItem(Object key, SoftLock lock) throws CacheException {

		try {
			LOG.debugf( "unlocking key[%s] in region[%s]", key, getInternalRegion().getName() );
			writeLock.lock();
			Lockable item = (Lockable) getInternalRegion().get( key );

			if ( ( item != null ) && item.isUnlockable( lock ) ) {
				decrementLock( key, (Lock) item );
			}
			else {
				handleLockExpiry( key, item );
			}
		}
		finally {
			writeLock.unlock();
		}
	}

	private long nextLockId() {
		return nextLockId.getAndIncrement();
	}

	/**
	 * Unlock and re-put the given key, lock combination.
	 */
	protected void decrementLock(Object key, Lock lock) {
		lock.unlock( getInternalRegion().nextTimestamp() );
		getInternalRegion().put( key, lock );
	}

	/**
	 * Handle the timeout of a previous lock mapped to this key
	 */
	protected void handleLockExpiry(Object key, Lockable lock) {
		LOG.info( "Cached entry expired : " + key );

		long ts = getInternalRegion().nextTimestamp() + getInternalRegion().getTimeout();
		// create new lock that times out immediately
		Lock newLock = new Lock( ts, uuid, nextLockId.getAndIncrement(), null );
		newLock.unlock( ts );
		getInternalRegion().put( key, newLock );
	}

	/**
	 * Interface type implemented by all wrapper objects in the cache.
	 */
	protected interface Lockable {

		/**
		 * Returns <code>true</code> if the enclosed value can be read by a transaction started at the given time.
		 */
		boolean isReadable(long txTimestamp);

		/**
		 * Returns <code>true</code> if the enclosed value can be replaced with one of the given version by a
		 * transaction started at the given time.
		 */
		boolean isWriteable(long txTimestamp, Object version, Comparator versionComparator);

		/**
		 * Returns the enclosed value.
		 */
		Object getValue();

		/**
		 * Returns <code>true</code> if the given lock can be unlocked using the given SoftLock instance as a handle.
		 */
		boolean isUnlockable(SoftLock lock);

		/**
		 * Locks this entry, stamping it with the UUID and lockId given, with the lock timeout occuring at the specified
		 * time.  The returned Lock object can be used to unlock the entry in the future.
		 */
		Lock lock(long timeout, UUID uuid, long lockId);
	}

	/**
	 * Wrapper type representing unlocked items.
	 */
	protected final static class Item implements Serializable, Lockable {

		private static final long serialVersionUID = 1L;
		private final Object value;
		private final Object version;
		private final long timestamp;

		/**
		 * Creates an unlocked item wrapping the given value with a version and creation timestamp.
		 */
		Item(Object value, Object version, long timestamp) {
			this.value = value;
			this.version = version;
			this.timestamp = timestamp;
		}

		@Override
		public boolean isReadable(long txTimestamp) {
			return txTimestamp > timestamp;
		}

		@Override
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
	}

	/**
	 * Wrapper type representing locked items.
	 */
	protected final static class Lock implements Serializable, Lockable, SoftLock {

		private static final long serialVersionUID = 2L;

		private final UUID sourceUuid;
		private final long lockId;
		private final Object version;

		private long timeout;
		private boolean concurrent;
		private int multiplicity = 1;
		private long unlockTimestamp;

		/**
		 * Creates a locked item with the given identifiers and object version.
		 */
		Lock(long timeout, UUID sourceUuid, long lockId, Object version) {
			this.timeout = timeout;
			this.lockId = lockId;
			this.version = version;
			this.sourceUuid = sourceUuid;
		}

		@Override
		public boolean isReadable(long txTimestamp) {
			return false;
		}

		@Override
		public boolean isWriteable(long txTimestamp, Object newVersion, Comparator versionComparator) {
			if ( txTimestamp > timeout ) {
				// if timedout then allow write
				return true;
			}
			if ( multiplicity > 0 ) {
				// if still locked then disallow write
				return false;
			}
			return version == null ? txTimestamp > unlockTimestamp : versionComparator.compare(
					version,
					newVersion
			) < 0;
		}

		@Override
		public Object getValue() {
			return null;
		}

		@Override
		public boolean isUnlockable(SoftLock lock) {
			return equals( lock );
		}

		@Override
		public boolean equals(Object o) {
			if ( o == this ) {
				return true;
			}
			else if ( o instanceof Lock ) {
				return ( lockId == ( (Lock) o ).lockId ) && sourceUuid.equals( ( (Lock) o ).sourceUuid );
			}
			else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			int hash = ( sourceUuid != null ? sourceUuid.hashCode() : 0 );
			int temp = (int) lockId;
			for ( int i = 1; i < Long.SIZE / Integer.SIZE; i++ ) {
				temp ^= ( lockId >>> ( i * Integer.SIZE ) );
			}
			return hash + temp;
		}

		/**
		 * Returns true if this Lock has been concurrently locked by more than one transaction.
		 */
		public boolean wasLockedConcurrently() {
			return concurrent;
		}

		@Override
		public Lock lock(long timeout, UUID uuid, long lockId) {
			concurrent = true;
			multiplicity++;
			this.timeout = timeout;
			return this;
		}

		/**
		 * Unlocks this Lock, and timestamps the unlock event.
		 */
		public void unlock(long timestamp) {
			if ( --multiplicity == 0 ) {
				unlockTimestamp = timestamp;
			}
		}

		@Override
		public String toString() {
			return "Lock Source-UUID:" + sourceUuid + " Lock-ID:" + lockId;
		}
	}
}
