/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
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

	protected AbstractReadWriteAccess(DomainDataRegionImpl region) {
		super( region );
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.READ_WRITE;
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
	public final Object get(SharedSessionContractImplementor session, Object key) {
		log.debugf( "Getting cached data from region[%s] by key[%s]", getRegion().getName(), key );
		try {
			readLock.lock();
			Lockable item = (Lockable) getFromCache( key );

			// todo (6.0) : how to determine "txn start timestamp" if no transaction?
			// 		-when Session's current CacheTransactionContext is null
			boolean readable = item != null && item.isReadable( session.getTransactionStartTimestamp() );
			if ( readable ) {
				log.debugf( "Cache hit : region = `%s`, key = `%s`", getRegion().getName(), key );
				return item.getValue();
			}
			else {
				if ( item == null ) {
					log.debugf( "Cache miss : region = `%s`, key = `%s`", getRegion().getName(), key );
				}
				else {
					log.debugf( "Cache hit, but item is unreadable/invalid : region = `%s`, key = `%s`", getRegion().getName(), key );
				}
				return null;
			}
		}
		finally {
			readLock.unlock();
		}
	}
	@Override
	public final boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version,
			boolean minimalPutOverride) {
		try {
			log.debugf( "Caching data from load (region=`%s`] : key[%s] -> value[%s]", getRegion().getName(), key, value );
			writeLock.lock();
			Lockable item = (Lockable) getFromCache( key );
			// todo (6.0) : again, how to determine "txn start timestamp" if no transaction?
			// 		- when Session's current CacheTransactionContext is null
			boolean writable = item == null || item.isWriteable( session.getTransactionStartTimestamp(), version, getVersionComparator() );
			if ( writable ) {
				addToCache( key, new Item( value, version, getRegion().getRegionFactory().nextTimestamp() ) );
				return true;
			}
			else {
				log.debugf(
						"Cache put-from-load [region=`%s`, key=`%s`, value=`%s`] failed due to being non-writable",
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
	@Override
	public final SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) {
		try {
			log.debugf( "Locking cache item [region=`%s`] : `%s`", getRegion().getName(), key );
			writeLock.lock();
			Lockable item = (Lockable) getFromCache( key );
			long timeout = getRegion().getRegionFactory().nextTimestamp() + getRegion().getRegionFactory().getTimeout();
			final SoftLockImpl lock = ( item == null )
					? new SoftLockImpl( timeout, uuid, nextLockId(), version )
					: item.lock( timeout, uuid, nextLockId() );
			addToCache( key, lock );
			return lock;
		}
		finally {
			writeLock.unlock();
		}
	}

	public final void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {
		try {
			log.debugf( "Unlocking cache item [region=`%s`] : %s", getRegion().getName(), key );
			writeLock.lock();
			Lockable item = (Lockable) getFromCache( key );

			if ( ( item != null ) && item.isUnlockable( lock ) ) {
				decrementLock(session, key, (SoftLockImpl) item );
			}
			else {
				handleLockExpiry(session, key, item );
			}
		}
		finally {
			writeLock.unlock();
		}
	}

	@Override
	public SoftLock lockRegion() {
		return null;
	}

	@Override
	public void unlockRegion(SoftLock lock) {

	}

	@SuppressWarnings("WeakerAccess")
	protected void decrementLock(SharedSessionContractImplementor session, Object key, SoftLockImpl lock) {
		lock.unlock( getRegion().getRegionFactory().nextTimestamp() );
		addToCache( key, lock );
	}

	@SuppressWarnings("WeakerAccess")
	protected void handleLockExpiry(SharedSessionContractImplementor session, Object key, Lockable lock) {
		log.info( "Cached entry expired : " + key );

		long ts = getRegion().getRegionFactory().nextTimestamp() + getRegion().getRegionFactory().getTimeout();
		// create new lock that times out immediately
		SoftLockImpl newLock = new SoftLockImpl( ts, uuid, nextLockId.getAndIncrement(), null );
		newLock.unlock( ts );
		addToCache( key, newLock );
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
		 * Locks this entry, stamping it with the UUID and lockId given, with the lock timeout occuring at the specified
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
		public SoftLockImpl lock(long timeout, UUID uuid, long lockId) {
			return new SoftLockImpl( timeout, uuid, lockId, version );
		}
	}

	/**
	 * Wrapper type representing locked items.
	 */
	public final static class SoftLockImpl implements Serializable, Lockable, SoftLock {

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
