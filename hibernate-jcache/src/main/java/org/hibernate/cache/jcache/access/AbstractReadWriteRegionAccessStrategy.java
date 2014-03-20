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
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.jcache.JCacheTransactionalDataRegion;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Alex Snaps
 */
public class AbstractReadWriteRegionAccessStrategy<R extends JCacheTransactionalDataRegion> {
	protected final R region;
	protected final Comparator versionComparator;
	private final UUID uuid = UUID.randomUUID();
	private final AtomicLong nextLockId = new AtomicLong();

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
		return region.invoke(
				key, new EntryProcessor<Object, Object, Boolean>() {
					@Override
					public Boolean process(MutableEntry<Object, Object> entry, Object... args)
							throws EntryProcessorException {
						final Lockable item = (Lockable) entry.getValue();
						final boolean writeable = item == null || item.isWriteable(
								(Long) args[1],
								args[2],
								versionComparator
						);
						if ( writeable ) {
							entry.setValue( new Item( args[0], args[2], region.nextTimestamp() ) );
							return true;
						}
						else {
							return false;
						}
					}
				}, value, txTimestamp, version);
	}

	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		return putFromLoad( session, key, value, txTimestamp, version );
	}

	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) throws CacheException {
		return region.invoke(
				key, new EntryProcessor<Object, Object, SoftLock>() {
					@Override
					public SoftLock process(MutableEntry<Object, Object> entry, Object... args)
							throws EntryProcessorException {
						final Lockable item = (Lockable) entry.getValue();
						final long timeout = region.nextTimestamp() + region.getTimeout();
						final Lock lock = ( item == null ) ? new Lock(
								timeout,
								(UUID) args[0],
								(Long) args[1],
								args[2]
						)
								: item.lock( timeout, (UUID) args[0], (Long) args[1] );
						entry.setValue( lock );
						return lock;
					}
				}, uuid, nextLockId(), version
		);
	}

	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		region.invoke(
				key, new EntryProcessor<Object, Object, Void>() {
					@Override
					public Void process(MutableEntry<Object, Object> entry, Object... args)
							throws EntryProcessorException {
						final Lockable item = (Lockable) entry.getValue();

						if ( (item != null) && item.isUnlockable( (SoftLock) args[0] ) ) {
							( (Lock) item ).unlock( region.nextTimestamp() );
							entry.setValue( item );
						}
						else {
							entry.setValue( null );
						}
						return null;
					}
				}, lock);
	}

	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		region.remove( key );
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
		throw new UnsupportedOperationException( "JCache doesn't support region locking" );
	}

	private long nextLockId() {
		return nextLockId.getAndIncrement();
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
	}

	/**
	 * Wrapper type representing locked items.
	 */
	public static final class Lock implements Serializable, Lockable, SoftLock {
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
		public Lock(long timeout, UUID sourceUuid, long lockId, Object version) {
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
			return equals( lock );
		}

		@Override
		@SuppressWarnings("SimplifiableIfStatement")
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
			final int hash = ( sourceUuid != null ? sourceUuid.hashCode() : 0 );
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
