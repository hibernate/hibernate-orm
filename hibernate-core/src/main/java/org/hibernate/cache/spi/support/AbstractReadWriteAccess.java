/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractReadWriteAccess extends AbstractCachedDomainDataAccess {
	private static final Logger log = Logger.getLogger( AbstractReadWriteAccess.class );

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

	protected abstract Comparator getVersionComparator();

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
		log.debugf( "Getting cached data from region [`%s` (%s)] by key [%s]", getRegion().getName(), getAccessType(), key );
		try {
			readLock.lock();
			Lockable item = (Lockable) getStorageAccess().getFromCache( key, session );

			if ( item == null ) {
				log.debugf( "Cache miss : region = `%s`, key = `%s`", getRegion().getName(), key );
				return null;
			}

			boolean readable = item.isReadable( session.getTransactionStartTimestamp() );
			if ( readable ) {
				log.debugf( "Cache hit : region = `%s`, key = `%s`", getRegion().getName(), key );
				return item.getValue();
			}
			else {
				log.debugf( "Cache hit, but item is unreadable/invalid : region = `%s`, key = `%s`", getRegion().getName(), key );
				return null;
			}
		}
		finally {
			readLock.unlock();
		}
	}

	@Override
	public boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version) {
		try {
			log.debugf( "Caching data from load [region=`%s` (%s)] : key[%s] -> value[%s]", getRegion().getName(), getAccessType(), key, value );
			writeLock.lock();
			Lockable item = (Lockable) getStorageAccess().getFromCache( key, session );

			boolean writable = item == null || item.isWriteable( session.getTransactionStartTimestamp(), version, getVersionComparator() );
			if ( writable ) {
				getStorageAccess().putIntoCache(
						key,
						new Item( value, version, session.getTransactionStartTimestamp() ),
						session
				);
				return true;
			}
			else {
				log.debugf(
						"Cache put-from-load [region=`%s` (%s), key=`%s`, value=`%s`] failed due to being non-writable",
						getAccessType(),
						getRegion().getName(),
						key,
						value
				);
				return false;
			}
		}
		finally {
			writeLock.unlock();
		}
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

			long timeout = getRegion().getRegionFactory().nextTimestamp() + getRegion().getRegionFactory().getTimeout();
			log.debugf( "Locking cache item [region=`%s` (%s)] : `%s` (timeout=%s, version=%s)", getRegion().getName(), getAccessType(), key, timeout, version );

			Lockable item = (Lockable) getStorageAccess().getFromCache( key, session );
			final SoftLockImpl lock = ( item == null )
					? new SoftLockImpl( timeout, uuid, nextLockId(), version )
					: item.lock( timeout, uuid, nextLockId() );
			getStorageAccess().putIntoCache( key, lock, session );
			return lock;
		}
		finally {
			writeLock.unlock();
		}
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {
		try {
			log.debugf( "Unlocking cache item [region=`%s` (%s)] : %s", getRegion().getName(), getAccessType(), key );
			writeLock.lock();
			Lockable item = (Lockable) getStorageAccess().getFromCache( key, session );

			if ( ( item != null ) && item.isUnlockable( lock ) ) {
				decrementLock( session, key, (SoftLockImpl) item );
			}
			else {
				handleLockExpiry( session, key, item );
			}
		}
		finally {
			writeLock.unlock();
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected void decrementLock(SharedSessionContractImplementor session, Object key, SoftLockImpl lock) {
		lock.unlock( getRegion().getRegionFactory().nextTimestamp() );
		getStorageAccess().putIntoCache( key, lock, session );
	}

	@SuppressWarnings("WeakerAccess")
	protected void handleLockExpiry(SharedSessionContractImplementor session, Object key, Lockable lock) {
		SecondLevelCacheLogger.INSTANCE.softLockedCacheExpired( getRegion().getName(), key );
		log.info( "Cached entry expired : " + key );

		// create new lock that times out immediately
		long ts = getRegion().getRegionFactory().nextTimestamp() + getRegion().getRegionFactory().getTimeout();
		SoftLockImpl newLock = new SoftLockImpl( ts, uuid, nextLockId.getAndIncrement(), null );
		//newLock.unlock( ts );
		newLock.unlock( ts - getRegion().getRegionFactory().getTimeout() );
		getStorageAccess().putIntoCache( key, newLock, session );
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) {
		if ( getStorageAccess().getFromCache( key, session ) instanceof SoftLock ) {
			log.debugf( "Skipping #remove call in read-write access to maintain SoftLock : %s", key );
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
			if ( log.isDebugEnabled() ) {
				log.debugf(
						"Checking readability of read-write cache item [timestamp=`%s`, version=`%s`] : txTimestamp=`%s`",
						(Object) timestamp,
						version,
						txTimestamp
				);
			}

			return txTimestamp > timestamp;
		}

		@Override
		public boolean isWriteable(long txTimestamp, Object newVersion, Comparator versionComparator) {
			if ( log.isDebugEnabled() ) {
				log.debugf(
						"Checking writeability of read-write cache item [timestamp=`%s`, version=`%s`] : txTimestamp=`%s`, newVersion=`%s`",
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
			if ( log.isDebugEnabled() ) {
				log.debugf(
						"Checking writeability of read-write cache lock [timeout=`%s`, lockId=`%s`, version=`%s`, sourceUuid=%s, multiplicity=`%s`, unlockTimestamp=`%s`] : txTimestamp=`%s`, newVersion=`%s`",
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
		public boolean equals(Object o) {
			if ( o == this ) {
				return true;
			}
			else if ( o instanceof SoftLockImpl ) {
				return ( lockId == ( (SoftLockImpl) o ).lockId ) && sourceUuid.equals( ( (SoftLockImpl) o ).sourceUuid );
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
