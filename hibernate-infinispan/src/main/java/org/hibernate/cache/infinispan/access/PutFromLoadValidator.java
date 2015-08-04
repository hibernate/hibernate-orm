/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Encapsulates logic to allow a {@link TransactionalAccessDelegate} to determine
 * whether a {@link TransactionalAccessDelegate#putFromLoad(Object, Object, long, Object, boolean)}
 * call should be allowed to update the cache. A <code>putFromLoad</code> has
 * the potential to store stale data, since the data may have been removed from the
 * database and the cache between the time when the data was read from the database
 * and the actual call to <code>putFromLoad</code>.
 * <p>
 * The expected usage of this class by a thread that read the cache and did
 * not find data is:
 * <p/>
 * <ol>
 * <li> Call {@link #registerPendingPut(Object)}</li>
 * <li> Read the database</li>
 * <li> Call {@link #acquirePutFromLoadLock(Object)}
 * <li> if above returns <code>null</code>, the thread should not cache the data;
 * only if above returns instance of <code>AcquiredLock</code>, put data in the cache and...</li>
 * <li> then call {@link #releasePutFromLoadLock(Object, Lock)}</li>
 * </ol>
 * </p>
 * <p/>
 * <p>
 * The expected usage by a thread that is taking an action such that any pending
 * <code>putFromLoad</code> may have stale data and should not cache it is to either
 * call
 * <p/>
 * <ul>
 * <li> {@link #beginInvalidatingKey(Object)} (for a single key invalidation)</li>
 * <li>or {@link #invalidateRegion()} (for a general invalidation all pending puts)</li>
 * </ul>
 * After transaction commit (when the DB is updated) {@link #endInvalidatingKey(Object)} should
 * be called in order to allow further attempts to cache entry.
 * </p>
 * <p/>
 * <p>
 * This class also supports the concept of "naked puts", which are calls to
 * {@link #acquirePutFromLoadLock(Object)} without a preceding {@link #registerPendingPut(Object)}.
 * Besides not acquiring lock in {@link #registerPendingPut(Object)} this can happen when collection
 * elements are loaded after the collection has not been found in the cache, where the elements
 * don't have their own table but can be listed as 'select ... from Element where collection_id = ...'.
 * </p>
 *
 * @author Brian Stansberry
 * @version $Revision: $
 */
public class PutFromLoadValidator {
	/**
	 * Period (in ms) after a removal during which a call to
	 * {@link #acquirePutFromLoadLock(Object)} that hasn't been
	 * {@link #registerPendingPut(Object) pre-registered} (aka a "naked put")
	 * will return false.
	 */
	public static final long NAKED_PUT_INVALIDATION_PERIOD = TimeUnit.SECONDS.toMillis( 20 );

	/**
	 * Used to determine whether the owner of a pending put is a thread or a transaction
	 */
	private final TransactionManager transactionManager;

	private final long nakedPutInvalidationPeriod;

	/**
	 * Registry of expected, future, isPutValid calls. If a key+owner is registered in this map, it
	 * is not a "naked put" and is allowed to proceed.
	 */
	private final ConcurrentMap<Object, PendingPutMap> pendingPuts;

	/**
	 * The time of the last call to {@link #invalidateRegion()}, plus NAKED_PUT_INVALIDATION_PERIOD. All naked
	 * puts will be rejected until the current time is greater than this value.
	 * NOTE: update only through {@link #invalidationUpdater}!
	 */
	private volatile long invalidationTimestamp = Long.MIN_VALUE;

	private static final AtomicLongFieldUpdater<PutFromLoadValidator> invalidationUpdater
			= AtomicLongFieldUpdater.newUpdater(PutFromLoadValidator.class, "invalidationTimestamp");

	/**
	 * Creates a new put from load validator instance.
    *
    * @param cache Cache instance on which to store pending put information.
	 * @param transactionManager Transaction manager
	 */
	public PutFromLoadValidator(AdvancedCache cache, TransactionManager transactionManager) {
		this( cache, transactionManager, NAKED_PUT_INVALIDATION_PERIOD );
	}

   /**
    * Constructor variant for use by unit tests; allows control of various timeouts by the test.
    *
    * @param cache Cache instance on which to store pending put information.
	 * @param transactionManager Transaction manager
    * @param nakedPutInvalidationPeriod Period (in ms) after a removal during which a call to
    *                                   {@link #acquirePutFromLoadLock(Object)} that hasn't been
    *                                   {@link #registerPendingPut(Object) pre-registered} (aka a "naked put")
    *                                   will return false.
    */
	public PutFromLoadValidator(
			AdvancedCache cache, TransactionManager transactionManager,
			long nakedPutInvalidationPeriod) {
		this(cache, cache.getCacheManager(), transactionManager, nakedPutInvalidationPeriod);
	}

   /**
    * Creates a new put from load validator instance.
	*
	* @param cache Cache instance on which to store pending put information.
	* @param cacheManager where to find a cache to store pending put information
	* @param tm transaction manager
	* @param nakedPutInvalidationPeriod Period (in ms) after a removal during which a call to
	*                                   {@link #acquirePutFromLoadLock(Object)} that hasn't been
	*                                   {@link #registerPendingPut(Object) pre-registered} (aka a "naked put")
	*                                   will return false.
	*/
	public PutFromLoadValidator(AdvancedCache cache,
			EmbeddedCacheManager cacheManager,
			TransactionManager tm, long nakedPutInvalidationPeriod) {

		Configuration cacheConfiguration = cache.getCacheConfiguration();
		Configuration pendingPutsConfiguration = cacheManager.getCacheConfiguration(InfinispanRegionFactory.PENDING_PUTS_CACHE_NAME);
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		if (pendingPutsConfiguration != null) {
			configurationBuilder.read(pendingPutsConfiguration);
		}
		configurationBuilder.dataContainer().keyEquivalence(cacheConfiguration.dataContainer().keyEquivalence());
		String pendingPutsName = cache.getName() + "-" + InfinispanRegionFactory.PENDING_PUTS_CACHE_NAME;
		cacheManager.defineConfiguration(pendingPutsName, configurationBuilder.build());

		this.pendingPuts = cacheManager.getCache(pendingPutsName);
		this.transactionManager = tm;
		this.nakedPutInvalidationPeriod = nakedPutInvalidationPeriod;
	}

	// ----------------------------------------------------------------- Public

	/**
	 * Marker for lock acquired in {@link #acquirePutFromLoadLock(Object)}
	 */
	public static class Lock {
		protected Lock() {}
	}

	/**
	 * Acquire a lock giving the calling thread the right to put data in the
	 * cache for the given key.
	 * <p>
	 * <strong>NOTE:</strong> A call to this method that returns <code>true</code>
	 * should always be matched with a call to {@link #releasePutFromLoadLock(Object, Lock)}.
	 * </p>
	 *
	 * @param key the key
	 *
	 * @return <code>AcquiredLock</code> if the lock is acquired and the cache put
	 *         can proceed; <code>null</code> if the data should not be cached
	 */
	public Lock acquirePutFromLoadLock(Object key) {
		boolean valid = false;
		boolean locked = false;
		long now = Long.MIN_VALUE;

		PendingPutMap pending = pendingPuts.get( key );
		for (;;) {
			try {
				if (pending != null) {
					locked = pending.acquireLock(100, TimeUnit.MILLISECONDS);
					if (locked) {
						try {
							final PendingPut toCancel = pending.remove(getOwnerForPut());
							if (toCancel != null) {
								valid = !toCancel.completed;
								toCancel.completed = true;
							} else {
								// this is a naked put
								if (pending.hasInvalidator()) {
									valid = false;
								} else {
									if (now == Long.MIN_VALUE) {
										now = System.currentTimeMillis();
									}
									valid = now > pending.nakedPutsDeadline;
								}
							}
							return valid ? pending : null;
						} finally {
							if (!valid) {
								pending.releaseLock();
								locked = false;
							}
						}
					} else {
						// oops, we have leaked record for this owner, but we don't want to wait here
						return null;
					}
				} else {
					// Key wasn't in pendingPuts, so either this is a "naked put"
					// or regionRemoved has been called. Check if we can proceed
					long invalidationTimestamp = this.invalidationTimestamp;
					if (invalidationTimestamp != Long.MIN_VALUE) {
						now = System.currentTimeMillis();
						if (now > invalidationTimestamp) {
							// time is +- monotonic se don't let other threads do the expensive currentTimeMillis()
							invalidationUpdater.compareAndSet(this, invalidationTimestamp, Long.MIN_VALUE);
						} else {
							return null;
						}
					}

					PendingPut pendingPut = new PendingPut(getOwnerForPut());
					pending = new PendingPutMap(pendingPut);
					PendingPutMap existing = pendingPuts.putIfAbsent(key, pending);
					if (existing != null) {
						pending = existing;
					}
					// continue in next loop with lock acquisition
				}
			} catch (Throwable t) {
				if (locked) {
					pending.releaseLock();
				}

				if (t instanceof RuntimeException) {
					throw (RuntimeException) t;
				} else if (t instanceof Error) {
					throw (Error) t;
				} else {
					throw new RuntimeException(t);
				}
			}
		}
	}

	/**
	 * Releases the lock previously obtained by a call to
	 * {@link #acquirePutFromLoadLock(Object)} that returned <code>true</code>.
	 *
	 * @param key the key
	 */
	public void releasePutFromLoadLock(Object key, Lock lock) {
		final PendingPutMap pending = (PendingPutMap) lock;
		if ( pending != null ) {
			if ( pending.canRemove() ) {
				pendingPuts.remove( key, pending );
			}
			pending.releaseLock();
		}
	}

	/**
	 * Invalidates all {@link #registerPendingPut(Object) previously registered pending puts} ensuring a subsequent call to
	 * {@link #acquirePutFromLoadLock(Object)} will return <code>false</code>. <p> This method will block until any
	 * concurrent thread that has {@link #acquirePutFromLoadLock(Object) acquired the putFromLoad lock} for the any key has
	 * released the lock. This allows the caller to be certain the putFromLoad will not execute after this method returns,
	 * possibly caching stale data. </p>
	 *
	 * @return <code>true</code> if the invalidation was successful; <code>false</code> if a problem occured (which the
	 *         caller should treat as an exception condition)
	 */
	public boolean invalidateRegion() {
		// TODO: not sure what happens with locks acquired *after* calling this method but before
		// the actual invalidation
		boolean ok = true;
		invalidationUpdater.set(this, System.currentTimeMillis() + nakedPutInvalidationPeriod);
		try {

			// Acquire the lock for each entry to ensure any ongoing
			// work associated with it is completed before we return
			for ( Iterator<PendingPutMap> it = pendingPuts.values().iterator(); it.hasNext(); ) {
				PendingPutMap entry = it.next();
				if ( entry.acquireLock( 60, TimeUnit.SECONDS ) ) {
					try {
						entry.invalidate();
					}
					finally {
						entry.releaseLock();
					}
					it.remove();
				}
				else {
					ok = false;
				}
			}
		} catch (Exception e) {
			ok = false;
		}

		return ok;
	}

	/**
	 * Notifies this validator that it is expected that a database read followed by a subsequent {@link
	 * #acquirePutFromLoadLock(Object)} call will occur. The intent is this method would be called following a cache miss
	 * wherein it is expected that a database read plus cache put will occur. Calling this method allows the validator to
	 * treat the subsequent <code>acquirePutFromLoadLock</code> as if the database read occurred when this method was
	 * invoked. This allows the validator to compare the timestamp of this call against the timestamp of subsequent removal
	 * notifications.
	 *
	 * @param key key that will be used for subsequent cache put
	 */
	public void registerPendingPut(Object key) {
		final PendingPut pendingPut = new PendingPut( getOwnerForPut() );
		final PendingPutMap pendingForKey = new PendingPutMap( pendingPut );

		final PendingPutMap existing = pendingPuts.putIfAbsent( key, pendingForKey );
		if ( existing != null ) {
			if ( existing.acquireLock( 10, TimeUnit.SECONDS ) ) {
				try {
					if ( !existing.hasInvalidator() ) {
						existing.put(pendingPut);
					}
				} finally {
					existing.releaseLock();
				}
			}
			else {
				// Can't get the lock; when we come back we'll be a "naked put"
			}
		}
	}

	/**
	 * Invalidates any {@link #registerPendingPut(Object) previously registered pending puts}
	 * and disables further registrations ensuring a subsequent call to {@link #acquirePutFromLoadLock(Object)}
	 * will return <code>false</code>. <p> This method will block until any concurrent thread that has
	 * {@link #acquirePutFromLoadLock(Object) acquired the putFromLoad lock} for the given key
	 * has released the lock. This allows the caller to be certain the putFromLoad will not execute after this method
	 * returns, possibly caching stale data. </p>
	 * After this transaction completes, {@link #endInvalidatingKey(Object)} needs to be called }
	 *
	 * @param key key identifying data whose pending puts should be invalidated
	 *
	 * @return <code>true</code> if the invalidation was successful; <code>false</code> if a problem occured (which the
	 *         caller should treat as an exception condition)
	 */
	public boolean beginInvalidatingKey(Object key) {
		PendingPutMap pending = new PendingPutMap(null);
		PendingPutMap prev = pendingPuts.putIfAbsent(key, pending);
		if (prev != null) {
			pending = prev;
		}
		if (pending.acquireLock(60, TimeUnit.SECONDS)) {
			try {
				pending.invalidate();
				pending.addInvalidator(getOwnerForPut(), System.currentTimeMillis() + nakedPutInvalidationPeriod);
			} finally {
				pending.releaseLock();
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Called after the transaction completes, allowing caching of entries. It is possible that this method
	 * is called without previous invocation of {@link #beginInvalidatingKey(Object)}, then it should be noop.
	 *
	 * @param key
	 * @return
    */
	public boolean endInvalidatingKey(Object key) {
		PendingPutMap pending = pendingPuts.get(key);
		if (pending == null) {
			return true;
		}
		if (pending.acquireLock(60, TimeUnit.SECONDS)) {
			try {
				pending.removeInvalidator(getOwnerForPut());
				// we can't remove the pending put yet because we wait for naked puts
				// pendingPuts should be configured with maxIdle time so won't have memory leak
				return true;
			} finally {
				pending.releaseLock();
			}
		} else {
			return false;
		}
	}


	// ---------------------------------------------------------------- Private

	private Object getOwnerForPut() {
		Transaction tx = null;
		try {
			if ( transactionManager != null ) {
				tx = transactionManager.getTransaction();
			}
		}
		catch (SystemException se) {
			throw new CacheException( "Could not obtain transaction", se );
		}
		return tx == null ? Thread.currentThread() : tx;

	}

	/**
	 * Lazy-initialization map for PendingPut. Optimized for the expected usual case where only a
	 * single put is pending for a given key.
	 * <p/>
	 * This class is NOT THREAD SAFE. All operations on it must be performed with the lock held.
	 */
	private static class PendingPutMap extends Lock {
		private PendingPut singlePendingPut;
		private Map<Object, PendingPut> fullMap;
		private final java.util.concurrent.locks.Lock lock = new ReentrantLock();
		private Object singleInvalidator;
		private Set<Object> invalidators;
		private long nakedPutsDeadline = Long.MIN_VALUE;

		PendingPutMap(PendingPut singleItem) {
			this.singlePendingPut = singleItem;
		}

		public void put(PendingPut pendingPut) {
			if ( singlePendingPut == null ) {
				if ( fullMap == null ) {
					// initial put
					singlePendingPut = pendingPut;
				}
				else {
					fullMap.put( pendingPut.owner, pendingPut );
				}
			}
			else {
				// 2nd put; need a map
				fullMap = new HashMap<Object, PendingPut>( 4 );
				fullMap.put( singlePendingPut.owner, singlePendingPut );
				singlePendingPut = null;
				fullMap.put( pendingPut.owner, pendingPut );
			}
		}

		public PendingPut remove(Object ownerForPut) {
			PendingPut removed = null;
			if ( fullMap == null ) {
				if ( singlePendingPut != null
						&& singlePendingPut.owner.equals( ownerForPut ) ) {
					removed = singlePendingPut;
					singlePendingPut = null;
				}
			}
			else {
				removed = fullMap.remove( ownerForPut );
			}
			return removed;
		}

		public int size() {
			return fullMap == null ? (singlePendingPut == null ? 0 : 1)
					: fullMap.size();
		}

		public boolean acquireLock(long time, TimeUnit unit) {
			try {
				return lock.tryLock( time, unit );
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}

		public void releaseLock() {
			lock.unlock();
		}

		public void invalidate() {
			if ( singlePendingPut != null ) {
				singlePendingPut.completed = true;
				// Nullify to avoid leaking completed pending puts
				singlePendingPut = null;
			}
			else if ( fullMap != null ) {
				for ( PendingPut pp : fullMap.values() ) {
					pp.completed = true;
				}
				// Nullify to avoid leaking completed pending puts
				fullMap = null;
			}
		}

		public void addInvalidator(Object invalidator, long deadline) {
			if (invalidators == null) {
				if (singleInvalidator == null) {
					singleInvalidator = invalidator;
				} else {
					invalidators = new HashSet<Object>();
					invalidators.add(singleInvalidator);
					invalidators.add(invalidator);
					singleInvalidator = null;
				}
			} else {
				invalidators.add(invalidator);
			}
			nakedPutsDeadline = Math.max(nakedPutsDeadline, deadline);
		}

		public boolean hasInvalidator() {
			return singleInvalidator != null || (invalidators != null && !invalidators.isEmpty());
		}

		public void removeInvalidator(Object invalidator) {
			if (invalidators == null) {
				if (singleInvalidator != null && singleInvalidator.equals(invalidator)) {
					singleInvalidator = null;
				}
			} else {
				invalidators.remove(invalidator);
			}
		}

		public boolean canRemove() {
			return size() == 0 && !hasInvalidator() &&
					(nakedPutsDeadline == Long.MIN_VALUE || nakedPutsDeadline < System.currentTimeMillis());
		}
	}

	private static class PendingPut {
		private final Object owner;
		private volatile boolean completed;

		private PendingPut(Object owner) {
			this.owner = owner;
		}
	}
}
