/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;


import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractReadWriteAccess extends AbstractCachedDomainDataAccess {

	private final UUID uuid = UUID.randomUUID();
	private final AtomicLong nextLockId = new AtomicLong();
	private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
	private final Lock readLock = reentrantReadWriteLock.readLock();
	private final Lock writeLock = reentrantReadWriteLock.writeLock();

	protected AbstractReadWriteAccess(
			DomainDataRegion domainDataRegion,
			DomainDataStorageAccess storageAccess) {
		super( domainDataRegion, storageAccess );
	}

	protected abstract Comparator<?> getVersionComparator();

	protected UUID uuid() {
		return uuid;
	}

	protected long nextLockId() {
		return nextLockId.getAndIncrement();
	}

	protected Lock readLock() {
		return readLock;
	}

	protected Lock writeLock() {
		return writeLock;
	}

	/**
	 * Returns <code>null</code> if the item is not readable.  Locked items are not readable, nor are items created
	 * afterQuery the start of this transaction.
	 */
	@Override
	public Object get(SharedSessionContractImplementor session, Object key) {
		final boolean traceEnabled = L2CACHE_LOGGER.isTraceEnabled();
		if ( traceEnabled ) {
			L2CACHE_LOGGER.tracef( "Getting cached data from region ['%s' (%s)] by key [%s]",
					getRegion().getName(), getAccessType(), key );
		}
		try {
			readLock.lock();
			final var item = (Lockable) getStorageAccess().getFromCache( key, session );
			if ( item == null ) {
				if ( traceEnabled ) {
					L2CACHE_LOGGER.tracef( "Cache miss: region = '%s', key = '%s'",
							getRegion().getName(), key );
				}
				return null;
			}

			if ( isReadable( session, item ) ) {
				if ( traceEnabled ) {
					L2CACHE_LOGGER.tracef( "Cache hit: region = '%s', key = '%s'",
							getRegion().getName(), key );
				}
				return item.getValue();
			}
			else {
				if ( traceEnabled ) {
					L2CACHE_LOGGER.tracef( "Cache hit, but item is unreadable/invalid: region = '%s', key = '%s'",
							getRegion().getName(), key );
				}
				return null;
			}
		}
		finally {
			readLock.unlock();
		}
	}

	private static boolean isReadable(SharedSessionContractImplementor session, Lockable item) {
		return item.isReadable( session.getCacheTransactionSynchronization().getCachingTimestamp() );
	}

	@Override
	public boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version) {
		try {
			final boolean traceEnabled = L2CACHE_LOGGER.isTraceEnabled();
			if ( traceEnabled ) {
				L2CACHE_LOGGER.tracef( "Caching data from load [region='%s' (%s)] : key[%s] -> value[%s]",
						getRegion().getName(), getAccessType(), key, value );
			}
			writeLock.lock();
			final var item = (Lockable) getStorageAccess().getFromCache( key, session );
			if ( isWritable( session, version, item ) ) {
				getStorageAccess().putIntoCache(
						key,
						new Item( value, version, session.getCacheTransactionSynchronization().getCachingTimestamp() ),
						session
				);
				return true;
			}
			else {
				if ( traceEnabled ) {
					L2CACHE_LOGGER.tracef(
							"Cache put-from-load [region='%s' (%s), key='%s', value='%s'] failed due to being non-writable",
							getAccessType(),
							getRegion().getName(),
							key,
							value
					);
				}
				return false;
			}
		}
		finally {
			writeLock.unlock();
		}
	}

	private boolean isWritable(SharedSessionContractImplementor session, Object version, Lockable item) {
		return item == null
			|| item.isWriteable( session.getCacheTransactionSynchronization().getCachingTimestamp(),
									version, getVersionComparator() );
	}

	protected abstract AccessedDataClassification getAccessedDataClassification();

	@Override
	public final boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version,
			boolean minimalPutOverride) {
		return putFromLoad( session, key, value, version );
	}

	@Override
	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) {
		try {
			writeLock.lock();
			final long timeout = nextTimestamp() + getTimeout();
			if ( L2CACHE_LOGGER.isTraceEnabled() ) {
				L2CACHE_LOGGER.tracef( "Locking cache item [region='%s' (%s)] : '%s' (timeout=%s, version=%s)",
						getRegion().getName(), getAccessType(), key, timeout, version );
			}
			final var item = (Lockable) getStorageAccess().getFromCache( key, session );
			final var lock = lock( item, version, timeout );
			getStorageAccess().putIntoCache( key, lock, session );
			return lock;
		}
		finally {
			writeLock.unlock();
		}
	}

	private SoftLockImpl lock(Lockable item, Object version, long timeout) {
		return item == null
				? new SoftLockImpl( timeout, uuid, nextLockId(), version )
				: item.lock( timeout, uuid, nextLockId() );
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {
		try {
			if ( L2CACHE_LOGGER.isTraceEnabled() ) {
				L2CACHE_LOGGER.tracef( "Unlocking cache item [region='%s' (%s)] : %s",
						getRegion().getName(), getAccessType(), key );
			}
			writeLock.lock();
			final var item = (Lockable) getStorageAccess().getFromCache( key, session );
			if ( item != null && item.isUnlockable( lock ) ) {
				decrementLock( session, key, (SoftLockImpl) item );
			}
			else {
				handleLockExpiry( session, key );
			}
		}
		finally {
			writeLock.unlock();
		}
	}

	long getTimeout() {
		return getRegion().getRegionFactory().getTimeout();
	}

	long nextTimestamp() {
		return getRegion().getRegionFactory().nextTimestamp();
	}

	protected void decrementLock(SharedSessionContractImplementor session, Object key, SoftLockImpl lock) {
		lock.unlock( nextTimestamp() );
		getStorageAccess().putIntoCache( key, lock, session );
	}

	protected void handleLockExpiry(SharedSessionContractImplementor session, Object key) {
		L2CACHE_LOGGER.softLockedCacheExpired( getRegion().getName(), key );
		L2CACHE_LOGGER.tracef( "Cached entry expired: %s", key );
		final var regionFactory = getRegion().getRegionFactory();
		// create a new lock that times out immediately
		long timestamp = regionFactory.nextTimestamp() + regionFactory.getTimeout();
		final var newLock = new SoftLockImpl( timestamp, uuid, nextLockId.getAndIncrement(), null );
		//newLock.unlock( timestamp );
		newLock.unlock( timestamp - regionFactory.getTimeout() );
		getStorageAccess().putIntoCache( key, newLock, session );
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) {
		if ( getStorageAccess().getFromCache( key, session ) instanceof SoftLock ) {
			if ( L2CACHE_LOGGER.isDebugEnabled() ) {
				L2CACHE_LOGGER.debugf( "Skipping remove call in read-write access to maintain SoftLock: ", key );
			}
			// don't do anything... we want the SoftLock to remain in place
		}
		else {
			super.remove( session, key );
		}
	}

	@Override
	public void removeAll(SharedSessionContractImplementor session) {
		// A no-op
	}

	/**
	 * Interface type implemented by all wrapper objects in the cache.
	 */
	public interface Lockable {

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
		 * Locks this entry, stamping it with the UUID and lockId given, with the lock timeout occurring at the specified
		 * time.  The returned Lock object can be used to unlock the entry in the future.
		 */
		SoftLockImpl lock(long timeout, UUID uuid, long lockId);
	}

	/**
	 * Wrapper type representing unlocked items.
	 */
	public final static class Item implements Serializable, Lockable {
		@Serial
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
			if ( L2CACHE_LOGGER.isTraceEnabled() ) {
				L2CACHE_LOGGER.tracef(
						"Checking readability of read-write cache item [timestamp='%s', version='%s'] : txTimestamp='%s'",
						(Object) timestamp,
						version,
						txTimestamp
				);
			}

			return txTimestamp > timestamp;
		}

		@Override
		public boolean isWriteable(long txTimestamp, Object newVersion, Comparator versionComparator) {
			if ( L2CACHE_LOGGER.isTraceEnabled() ) {
				L2CACHE_LOGGER.tracef(
						"Checking writeability of read-write cache item [timestamp='%s', version='%s'] : txTimestamp='%s', newVersion='%s'",
						timestamp,
						version,
						txTimestamp,
						newVersion
				);
			}

			//noinspection unchecked
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
		public SoftLockImpl lock(long timeout, UUID uuid, long lockId) {
			return new SoftLockImpl( timeout, uuid, lockId, version );
		}

		@Override
		public String toString() {
			return String.format(
					Locale.ROOT,
					"read-write Item(%s)",
					getValue()
			);
		}
	}

	/**
	 * Wrapper type representing locked items.
	 */
	public static class SoftLockImpl implements Serializable, Lockable, SoftLock {

		@Serial
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
		SoftLockImpl(long timeout, UUID sourceUuid, long lockId, Object version) {
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
			if ( L2CACHE_LOGGER.isTraceEnabled() ) {
				L2CACHE_LOGGER.tracef(
						"Checking writeability of read-write cache lock [timeout='%s', lockId='%s', version='%s', sourceUuid=%s, multiplicity='%s', unlockTimestamp='%s'] : txTimestamp='%s', newVersion='%s'",
						timeout,
						lockId,
						version,
						sourceUuid,
						multiplicity,
						unlockTimestamp,
						txTimestamp,
						newVersion
				);
			}

			if ( txTimestamp > timeout ) {
				// if timed-out - allow write
				return true;
			}
			if ( multiplicity > 0 ) {
				// if still locked - disallow write
				return false;
			}

			//noinspection unchecked
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
			return equals( lock );
		}

		@Override
		public boolean equals(Object other) {
			if ( other == this ) {
				return true;
			}
			else if ( other instanceof SoftLockImpl that ) {
				return this.lockId == that.lockId
					&& this.sourceUuid.equals( that.sourceUuid );
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
		public SoftLockImpl lock(long timeout, UUID uuid, long lockId) {
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
