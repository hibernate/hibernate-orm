/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.util.CacheCommandInitializer;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.interceptors.EntryWrappingInterceptor;
import org.infinispan.interceptors.InvalidationInterceptor;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Encapsulates logic to allow a {@link InvalidationCacheAccessDelegate} to determine
 * whether a {@link InvalidationCacheAccessDelegate#putFromLoad(org.hibernate.engine.spi.SharedSessionContractImplementor, Object, Object, long, Object, boolean)}
 * call should be allowed to update the cache. A <code>putFromLoad</code> has
 * the potential to store stale data, since the data may have been removed from the
 * database and the cache between the time when the data was read from the database
 * and the actual call to <code>putFromLoad</code>.
 * <p>
 * The expected usage of this class by a thread that read the cache and did
 * not find data is:
 * <p/>
 * <ol>
 * <li> Call {@link #registerPendingPut(SharedSessionContractImplementor, Object, long)}</li>
 * <li> Read the database</li>
 * <li> Call {@link #acquirePutFromLoadLock(SharedSessionContractImplementor, Object, long)}
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
 * <li> {@link #beginInvalidatingKey(Object, Object)} (for a single key invalidation)</li>
 * <li>or {@link #beginInvalidatingRegion()} followed by {@link #endInvalidatingRegion()}
 *     (for a general invalidation all pending puts)</li>
 * </ul>
 * After transaction commit (when the DB is updated) {@link #endInvalidatingKey(Object, Object)} should
 * be called in order to allow further attempts to cache entry.
 * </p>
 * <p/>
 * <p>
 * This class also supports the concept of "naked puts", which are calls to
 * {@link #acquirePutFromLoadLock(SharedSessionContractImplementor, Object, long)} without a preceding {@link #registerPendingPut(SharedSessionContractImplementor, Object, long)}.
 * Besides not acquiring lock in {@link #registerPendingPut(SharedSessionContractImplementor, Object, long)} this can happen when collection
 * elements are loaded after the collection has not been found in the cache, where the elements
 * don't have their own table but can be listed as 'select ... from Element where collection_id = ...'.
 * Naked puts are handled according to txTimestamp obtained by calling {@link RegionFactory#nextTimestamp()}
 * before the transaction is started. The timestamp is compared with timestamp of last invalidation end time
 * and the write to the cache is denied if it is lower or equal.
 * </p>
 *
 * @author Brian Stansberry
 * @version $Revision: $
 */
public class PutFromLoadValidator {
	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(PutFromLoadValidator.class);
	private static final boolean trace = log.isTraceEnabled();

	/**
	 * Period after which ongoing invalidation is removed. Value is retrieved from cache configuration.
	 */
	private final long expirationPeriod;

	/**
	 * Registry of expected, future, isPutValid calls. If a key+owner is registered in this map, it
	 * is not a "naked put" and is allowed to proceed.
	 */
	private final ConcurrentMap<Object, PendingPutMap> pendingPuts;

	/**
	 * Main cache where the entities/collections are stored. This is not modified from within this class.
	 */
	private final AdvancedCache cache;

	private final InfinispanRegionFactory regionFactory;

	/**
	 * Injected interceptor
	 */
	private NonTxPutFromLoadInterceptor nonTxPutFromLoadInterceptor;

	/**
	 * The time of the last call to {@link #endInvalidatingRegion()}. Puts from transactions started after
	 * this timestamp are denied.
	 */
	private volatile long regionInvalidationTimestamp = Long.MIN_VALUE;

	/**
	 * Number of ongoing concurrent invalidations.
	 */
	private int regionInvalidations = 0;

	/**
	 * Allows propagation of current Session to callbacks invoked from interceptors
	 */
	private final ThreadLocal<SharedSessionContractImplementor> currentSession = new ThreadLocal<SharedSessionContractImplementor>();

	/**
	 * Creates a new put from load validator instance.
	 *
	 * @param cache Cache instance on which to store pending put information.
	 */
	public PutFromLoadValidator(AdvancedCache cache, InfinispanRegionFactory regionFactory) {
		this( cache, regionFactory, cache.getCacheManager());
	}

	/**
	 * Creates a new put from load validator instance.
	 * @param cache Cache instance on which to store pending put information.
	 * @param regionFactory
	 * @param cacheManager where to find a cache to store pending put information
	 */
	public PutFromLoadValidator(AdvancedCache cache, InfinispanRegionFactory regionFactory, EmbeddedCacheManager cacheManager) {
		this.regionFactory = regionFactory;
		Configuration cacheConfiguration = cache.getCacheConfiguration();
		Configuration pendingPutsConfiguration = regionFactory.getPendingPutsCacheConfiguration();
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.read(pendingPutsConfiguration);
		configurationBuilder.dataContainer().keyEquivalence(cacheConfiguration.dataContainer().keyEquivalence());
		String pendingPutsName = cache.getName() + "-" + InfinispanRegionFactory.DEF_PENDING_PUTS_RESOURCE;
		cacheManager.defineConfiguration(pendingPutsName, configurationBuilder.build());

		if (pendingPutsConfiguration.expiration() != null && pendingPutsConfiguration.expiration().maxIdle() > 0) {
			this.expirationPeriod = pendingPutsConfiguration.expiration().maxIdle();
		}
		else {
			throw log.pendingPutsMustHaveMaxIdle();
		}
		CacheMode cacheMode = cache.getCacheConfiguration().clustering().cacheMode();
		// Since we need to intercept both invalidations of entries that are in the cache and those
		// that are not, we need to use custom interceptor, not listeners (which fire only for present entries).
		NonTxPutFromLoadInterceptor nonTxPutFromLoadInterceptor = null;
		if (cacheMode.isClustered()) {
			if (!cacheMode.isInvalidation()) {
				throw new IllegalArgumentException("PutFromLoadValidator in clustered caches requires invalidation mode.");
			}
			addToCache(cache, this);
		}

		this.cache = cache;
		this.pendingPuts = cacheManager.getCache(pendingPutsName);
	}

	/**
	 * Besides the call from constructor, this should be called only from tests when mocking the validator.
	 */
	public static void addToCache(AdvancedCache cache, PutFromLoadValidator validator) {
		List<CommandInterceptor> interceptorChain = cache.getInterceptorChain();
		log.debug("Interceptor chain was: " + interceptorChain);
		int position = 0;
		// add interceptor before uses exact match, not instanceof match
		int invalidationPosition = 0;
		int entryWrappingPosition = 0;
		for (CommandInterceptor ci : interceptorChain) {
			if (ci instanceof InvalidationInterceptor) {
				invalidationPosition = position;
			}
			if (ci instanceof EntryWrappingInterceptor) {
				entryWrappingPosition = position;
			}
			position++;
		}
		boolean transactional = cache.getCacheConfiguration().transaction().transactionMode().isTransactional();
		if (transactional) {
			cache.removeInterceptor(invalidationPosition);
			TxInvalidationInterceptor txInvalidationInterceptor = new TxInvalidationInterceptor();
			cache.getComponentRegistry().registerComponent(txInvalidationInterceptor, TxInvalidationInterceptor.class);
			cache.addInterceptor(txInvalidationInterceptor, invalidationPosition);

			// Note that invalidation does *NOT* acquire locks; therefore, we have to start invalidating before
			// wrapping the entry, since if putFromLoad was invoked between wrap and beginInvalidatingKey, the invalidation
			// would not commit the entry removal (as during wrap the entry was not in cache)
			TxPutFromLoadInterceptor txPutFromLoadInterceptor = new TxPutFromLoadInterceptor(validator, cache.getName());
			cache.getComponentRegistry().registerComponent(txPutFromLoadInterceptor, TxPutFromLoadInterceptor.class);
			cache.addInterceptor(txPutFromLoadInterceptor, entryWrappingPosition);
		}
		else {
			cache.removeInterceptor(invalidationPosition);
			NonTxInvalidationInterceptor nonTxInvalidationInterceptor = new NonTxInvalidationInterceptor(validator);
			cache.getComponentRegistry().registerComponent(nonTxInvalidationInterceptor, NonTxInvalidationInterceptor.class);
			cache.addInterceptor(nonTxInvalidationInterceptor, invalidationPosition);

			NonTxPutFromLoadInterceptor nonTxPutFromLoadInterceptor = new NonTxPutFromLoadInterceptor(validator, cache.getName());
			cache.getComponentRegistry().registerComponent(nonTxPutFromLoadInterceptor, NonTxPutFromLoadInterceptor.class);
			cache.addInterceptor(nonTxPutFromLoadInterceptor, entryWrappingPosition);
			validator.nonTxPutFromLoadInterceptor = nonTxPutFromLoadInterceptor;
		}
		log.debug("New interceptor chain is: " + cache.getInterceptorChain());

		CacheCommandInitializer cacheCommandInitializer = cache.getComponentRegistry().getComponent(CacheCommandInitializer.class);
		cacheCommandInitializer.addPutFromLoadValidator(cache.getName(), validator);
	}

	/**
	 * This methods should be called only from tests; it removes existing validator from the cache structures
	 * in order to replace it with new one.
	 *
	 * @param cache
	 */
	public static PutFromLoadValidator removeFromCache(AdvancedCache cache) {
		cache.removeInterceptor(TxPutFromLoadInterceptor.class);
		cache.removeInterceptor(NonTxPutFromLoadInterceptor.class);
		for (Object i : cache.getInterceptorChain()) {
			if (i instanceof NonTxInvalidationInterceptor) {
				InvalidationInterceptor invalidationInterceptor = new InvalidationInterceptor();
				cache.getComponentRegistry().registerComponent(invalidationInterceptor, InvalidationInterceptor.class);
				cache.addInterceptorBefore(invalidationInterceptor, NonTxInvalidationInterceptor.class);
				cache.removeInterceptor(NonTxInvalidationInterceptor.class);
				break;
			}
			else if (i instanceof TxInvalidationInterceptor) {
				InvalidationInterceptor invalidationInterceptor = new InvalidationInterceptor();
				cache.getComponentRegistry().registerComponent(invalidationInterceptor, InvalidationInterceptor.class);
				cache.addInterceptorBefore(invalidationInterceptor, TxInvalidationInterceptor.class);
				cache.removeInterceptor(TxInvalidationInterceptor.class);
				break;
			}
		}
		CacheCommandInitializer cci = cache.getComponentRegistry().getComponent(CacheCommandInitializer.class);
		return cci.removePutFromLoadValidator(cache.getName());
	}

	public void setCurrentSession(SharedSessionContractImplementor session) {
		currentSession.set(session);
	}

	public void resetCurrentSession() {
		currentSession.remove();
	}

	/**
	 * Marker for lock acquired in {@link #acquirePutFromLoadLock(SharedSessionContractImplementor, Object, long)}
	 */
	public static abstract class Lock {
		private Lock() {}
	}

	/**
	 * Acquire a lock giving the calling thread the right to put data in the
	 * cache for the given key.
	 * <p>
	 * <strong>NOTE:</strong> A call to this method that returns <code>true</code>
	 * should always be matched with a call to {@link #releasePutFromLoadLock(Object, Lock)}.
	 * </p>
	 *
	 * @param session
	 * @param key the key
	 *
	 * @param txTimestamp
	 * @return <code>AcquiredLock</code> if the lock is acquired and the cache put
	 *         can proceed; <code>null</code> if the data should not be cached
	 */
	public Lock acquirePutFromLoadLock(SharedSessionContractImplementor session, Object key, long txTimestamp) {
		if (trace) {
			log.tracef("acquirePutFromLoadLock(%s#%s, %d)", cache.getName(), key, txTimestamp);
		}
		boolean locked = false;

		PendingPutMap pending = pendingPuts.get( key );
		for (;;) {
			try {
				if (pending != null) {
					locked = pending.acquireLock(100, TimeUnit.MILLISECONDS);
					if (locked) {
						boolean valid = false;
						try {
							if (pending.isRemoved()) {
								// this deals with a race between retrieving the map from cache vs. removing that
								// and locking the map
								pending.releaseLock();
								locked = false;
								pending = null;
								if (trace) {
									log.tracef("Record removed when waiting for the lock.");
								}
								continue;
							}
							final PendingPut toCancel = pending.remove(session);
							if (toCancel != null) {
								valid = !toCancel.completed;
								toCancel.completed = true;
							}
							else {
								// this is a naked put
								if (pending.hasInvalidator()) {
									valid = false;
								}
								// we need this check since registerPendingPut (creating new pp) can get between invalidation
								// and naked put caused by the invalidation
								else if (pending.lastInvalidationEnd != Long.MIN_VALUE) {
									// if this transaction started after last invalidation we can continue
									valid = txTimestamp > pending.lastInvalidationEnd;
								}
								else {
									valid = txTimestamp > regionInvalidationTimestamp;
								}
							}
							return valid ? pending : null;
						}
						finally {
							if (!valid && pending != null) {
								pending.releaseLock();
								locked = false;
							}
							if (trace) {
								log.tracef("acquirePutFromLoadLock(%s#%s, %d) ended with %s, valid: %s", cache.getName(), key, txTimestamp, pending, valid);
							}
						}
					}
					else {
						if (trace) {
							log.tracef("acquirePutFromLoadLock(%s#%s, %d) failed to lock", cache.getName(), key, txTimestamp);
						}
						// oops, we have leaked record for this owner, but we don't want to wait here
						return null;
					}
				}
				else {
					long regionInvalidationTimestamp = this.regionInvalidationTimestamp;
					if (txTimestamp <= regionInvalidationTimestamp) {
						if (trace) {
							log.tracef("acquirePutFromLoadLock(%s#%s, %d) failed due to region invalidated at %d", cache.getName(), key, txTimestamp, regionInvalidationTimestamp);
						}
						return null;
					}
					else {
						if (trace) {
							log.tracef("Region invalidated at %d, this transaction started at %d", regionInvalidationTimestamp, txTimestamp);
						}
					}

					PendingPut pendingPut = new PendingPut(session);
					pending = new PendingPutMap(pendingPut);
					PendingPutMap existing = pendingPuts.putIfAbsent(key, pending);
					if (existing != null) {
						pending = existing;
					}
					// continue in next loop with lock acquisition
				}
			}
			catch (Throwable t) {
				if (locked) {
					pending.releaseLock();
				}

				if (t instanceof RuntimeException) {
					throw (RuntimeException) t;
				}
				else if (t instanceof Error) {
					throw (Error) t;
				}
				else {
					throw new RuntimeException(t);
				}
			}
		}
	}

	/**
	 * Releases the lock previously obtained by a call to
	 * {@link #acquirePutFromLoadLock(SharedSessionContractImplementor, Object, long)}.
	 *
	 * @param key the key
	 */
	public void releasePutFromLoadLock(Object key, Lock lock) {
		if (trace) {
			log.tracef("releasePutFromLoadLock(%s#%s, %s)", cache.getName(), key, lock);
		}
		final PendingPutMap pending = (PendingPutMap) lock;
		if ( pending != null ) {
			if ( pending.canRemove() ) {
				pending.setRemoved();
				pendingPuts.remove( key, pending );
			}
			pending.releaseLock();
		}
	}

	/**
	 * Invalidates all {@link #registerPendingPut(SharedSessionContractImplementor, Object, long) previously registered pending puts} ensuring a subsequent call to
	 * {@link #acquirePutFromLoadLock(SharedSessionContractImplementor, Object, long)} will return <code>false</code>. <p> This method will block until any
	 * concurrent thread that has {@link #acquirePutFromLoadLock(SharedSessionContractImplementor, Object, long) acquired the putFromLoad lock} for the any key has
	 * released the lock. This allows the caller to be certain the putFromLoad will not execute after this method returns,
	 * possibly caching stale data. </p>
	 *
	 * @return <code>true</code> if the invalidation was successful; <code>false</code> if a problem occurred (which the
	 *         caller should treat as an exception condition)
	 */
	public boolean beginInvalidatingRegion() {
		if (trace) {
			log.trace("Started invalidating region " + cache.getName());
		}
		boolean ok = true;
		long now = regionFactory.nextTimestamp();
		// deny all puts until endInvalidatingRegion is called; at that time the region should be already
		// in INVALID state, therefore all new requests should be blocked and ongoing should fail by timestamp
		synchronized (this) {
			regionInvalidationTimestamp = Long.MAX_VALUE;
			regionInvalidations++;
		}

		try {
			// Acquire the lock for each entry to ensure any ongoing
			// work associated with it is completed before we return
			// We cannot erase the map: if there was ongoing invalidation and we removed it, registerPendingPut
			// started after that would have no way of finding out that the entity *is* invalidated (it was
			// removed from the cache and now the DB is about to be updated).
			for (Iterator<PendingPutMap> it = pendingPuts.values().iterator(); it.hasNext(); ) {
				PendingPutMap entry = it.next();
				if (entry.acquireLock(60, TimeUnit.SECONDS)) {
					try {
						entry.invalidate(now);
					}
					finally {
						entry.releaseLock();
					}
				}
				else {
					ok = false;
				}
			}
		}
		catch (Exception e) {
			ok = false;
		}
		return ok;
	}

	/**
	 * Called when the region invalidation is finished.
	 */
	public void endInvalidatingRegion() {
		synchronized (this) {
			if (--regionInvalidations == 0) {
				regionInvalidationTimestamp = regionFactory.nextTimestamp();
				if (trace) {
					log.tracef("Finished invalidating region %s at %d", cache.getName(), regionInvalidationTimestamp);
				}
			}
			else {
				if (trace) {
					log.tracef("Finished invalidating region %s, but there are %d ongoing invalidations", cache.getName(), regionInvalidations);
				}
			}
		}
	}

	/**
	 * Notifies this validator that it is expected that a database read followed by a subsequent {@link
	 * #acquirePutFromLoadLock(SharedSessionContractImplementor, Object, long)} call will occur. The intent is this method would be called following a cache miss
	 * wherein it is expected that a database read plus cache put will occur. Calling this method allows the validator to
	 * treat the subsequent <code>acquirePutFromLoadLock</code> as if the database read occurred when this method was
	 * invoked. This allows the validator to compare the timestamp of this call against the timestamp of subsequent removal
	 * notifications.
	 *
	 * @param session
	 * @param key key that will be used for subsequent cache put
	 * @param txTimestamp
	 */
	public void registerPendingPut(SharedSessionContractImplementor session, Object key, long txTimestamp) {
		long invalidationTimestamp = this.regionInvalidationTimestamp;
		if (txTimestamp <= invalidationTimestamp) {
			if (trace) {
				log.tracef("registerPendingPut(%s#%s, %d) skipped due to region invalidation (%d)", cache.getName(), key, txTimestamp, invalidationTimestamp);
			}
			return;
		}

		final PendingPut pendingPut = new PendingPut( session );
		final PendingPutMap pendingForKey = new PendingPutMap( pendingPut );

		for (;;) {
			final PendingPutMap existing = pendingPuts.putIfAbsent(key, pendingForKey);
			if (existing != null) {
				if (existing.acquireLock(10, TimeUnit.SECONDS)) {
					try {
						if (existing.isRemoved()) {
							if (trace) {
								log.tracef("Record removed when waiting for the lock.");
							}
							continue;
						}
						if (!existing.hasInvalidator()) {
							existing.put(pendingPut);
						}
					}
					finally {
						existing.releaseLock();
					}
					if (trace) {
						log.tracef("registerPendingPut(%s#%s, %d) ended with %s", cache.getName(), key, txTimestamp, existing);
					}
				}
				else {
					if (trace) {
						log.tracef("registerPendingPut(%s#%s, %d) failed to acquire lock", cache.getName(), key, txTimestamp);
					}
					// Can't get the lock; when we come back we'll be a "naked put"
				}
			}
			else {
				if (trace) {
					log.tracef("registerPendingPut(%s#%s, %d) registered using putIfAbsent: %s", cache.getName(), key, txTimestamp, pendingForKey);
				}
			}
			return;
		}
	}

	/**
	 * Invalidates any {@link #registerPendingPut(SharedSessionContractImplementor, Object, long) previously registered pending puts}
	 * and disables further registrations ensuring a subsequent call to {@link #acquirePutFromLoadLock(SharedSessionContractImplementor, Object, long)}
	 * will return <code>false</code>. <p> This method will block until any concurrent thread that has
	 * {@link #acquirePutFromLoadLock(SharedSessionContractImplementor, Object, long) acquired the putFromLoad lock} for the given key
	 * has released the lock. This allows the caller to be certain the putFromLoad will not execute after this method
	 * returns, possibly caching stale data. </p>
	 * After this transaction completes, {@link #endInvalidatingKey(Object, Object)} needs to be called }
	 *
	 * @param key key identifying data whose pending puts should be invalidated
	 *
	 * @return <code>true</code> if the invalidation was successful; <code>false</code> if a problem occurred (which the
	 *         caller should treat as an exception condition)
	 */
	public boolean beginInvalidatingKey(Object lockOwner, Object key) {
		return beginInvalidatingWithPFER(lockOwner, key, null);
	}

	public boolean beginInvalidatingWithPFER(Object lockOwner, Object key, Object valueForPFER) {
		for (;;) {
			PendingPutMap pending = new PendingPutMap(null);
			PendingPutMap prev = pendingPuts.putIfAbsent(key, pending);
			if (prev != null) {
				pending = prev;
			}
			if (pending.acquireLock(60, TimeUnit.SECONDS)) {
				try {
					if (pending.isRemoved()) {
						if (trace) {
							log.tracef("Record removed when waiting for the lock.");
						}
						continue;
					}
					long now = regionFactory.nextTimestamp();
					pending.invalidate(now);
					pending.addInvalidator(lockOwner, valueForPFER, now);
				}
				finally {
					pending.releaseLock();
				}
				if (trace) {
					log.tracef("beginInvalidatingKey(%s#%s, %s) ends with %s", cache.getName(), key, lockOwnerToString(lockOwner), pending);
				}
				return true;
			}
			else {
				log.tracef("beginInvalidatingKey(%s#%s, %s) failed to acquire lock", cache.getName(), key, lockOwnerToString(lockOwner));
				return false;
			}
		}
	}

	public boolean endInvalidatingKey(Object lockOwner, Object key) {
		return endInvalidatingKey(lockOwner, key, false);
	}

	/**
	 * Called after the transaction completes, allowing caching of entries. It is possible that this method
	 * is called without previous invocation of {@link #beginInvalidatingKey(Object, Object)}, then it should be a no-op.
	 *
	 * @param lockOwner owner of the invalidation - transaction or thread
	 * @param key
	 * @return
	 */
	public boolean endInvalidatingKey(Object lockOwner, Object key, boolean doPFER) {
		PendingPutMap pending = pendingPuts.get(key);
		if (pending == null) {
			if (trace) {
				log.tracef("endInvalidatingKey(%s#%s, %s) could not find pending puts", cache.getName(), key, lockOwnerToString(lockOwner));
			}
			return true;
		}
		if (pending.acquireLock(60, TimeUnit.SECONDS)) {
			try {
				long now = regionFactory.nextTimestamp();
				pending.removeInvalidator(lockOwner, key, now, doPFER);
				// we can't remove the pending put yet because we wait for naked puts
				// pendingPuts should be configured with maxIdle time so won't have memory leak
				return true;
			}
			finally {
				pending.releaseLock();
				if (trace) {
					log.tracef("endInvalidatingKey(%s#%s, %s) ends with %s", cache.getName(), key, lockOwnerToString(lockOwner), pending);
				}
			}
		}
		else {
			if (trace) {
				log.tracef("endInvalidatingKey(%s#%s, %s) failed to acquire lock", cache.getName(), key, lockOwnerToString(lockOwner));
			}
			return false;
		}
	}

	public boolean registerRemoteInvalidation(Object key, Object lockOwner) {
		SharedSessionContractImplementor session = currentSession.get();
		TransactionCoordinator transactionCoordinator = session == null ? null : session.getTransactionCoordinator();
		if (transactionCoordinator != null) {
			if (trace) {
				log.tracef("Registering synchronization on transaction in %s, cache %s: %s", lockOwnerToString(session), cache.getName(), key);
			}
			InvalidationSynchronization sync = new InvalidationSynchronization(nonTxPutFromLoadInterceptor, key, lockOwner);
			transactionCoordinator.getLocalSynchronizations().registerSynchronization(sync);
			return true;
		}
		// evict() command is not executed in session context
		return false;
	}

	// ---------------------------------------------------------------- Private

	// we can't use SessionImpl.toString() concurrently
	private static String lockOwnerToString(Object lockOwner) {
		return lockOwner instanceof SharedSessionContractImplementor ? "Session#" + lockOwner.hashCode() : lockOwner.toString();
	}

	/**
	 * Lazy-initialization map for PendingPut. Optimized for the expected usual case where only a
	 * single put is pending for a given key.
	 * <p/>
	 * This class is NOT THREAD SAFE. All operations on it must be performed with the lock held.
	 */
	private class PendingPutMap extends Lock {
		// Number of pending puts which trigger garbage collection
		private static final int GC_THRESHOLD = 10;
		private PendingPut singlePendingPut;
		private Map<Object, PendingPut> fullMap;
		private final java.util.concurrent.locks.Lock lock = new ReentrantLock();
		private Invalidator singleInvalidator;
		private Map<Object, Invalidator> invalidators;
		private long lastInvalidationEnd = Long.MIN_VALUE;
		private boolean removed = false;

		PendingPutMap(PendingPut singleItem) {
			this.singlePendingPut = singleItem;
		}

		// toString should be called only for debugging purposes
		public String toString() {
			if (lock.tryLock()) {
				try {
					StringBuilder sb = new StringBuilder();
					sb.append("{ PendingPuts=");
					if (singlePendingPut == null) {
						if (fullMap == null) {
							sb.append("[]");
						}
						else {
							sb.append(fullMap.values());
						}
					}
					else {
						sb.append('[').append(singlePendingPut).append(']');
					}
					sb.append(", Invalidators=");
					if (singleInvalidator == null) {
						if (invalidators == null) {
							sb.append("[]");
						}
						else {
							sb.append(invalidators.values());
						}
					}
					else {
						sb.append('[').append(singleInvalidator).append(']');
					}
					sb.append(", LastInvalidationEnd=");
					if (lastInvalidationEnd == Long.MIN_VALUE) {
						sb.append("<none>");
					}
					else {
						sb.append(lastInvalidationEnd);
					}
					return sb.append(", Removed=").append(removed).append("}").toString();
				}
				finally {
					lock.unlock();
				}
			}
			else {
				return "PendingPutMap: <locked>";
			}
		}

		public void put(PendingPut pendingPut) {
			if ( singlePendingPut == null ) {
				if ( fullMap == null ) {
					// initial put
					singlePendingPut = pendingPut;
				}
				else {
					fullMap.put( pendingPut.owner, pendingPut );
					if (fullMap.size() >= GC_THRESHOLD) {
						gc();
					}
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

		public void invalidate(long now) {
			if ( singlePendingPut != null ) {
				if (singlePendingPut.invalidate(now, expirationPeriod)) {
					singlePendingPut = null;
				}
			}
			else if ( fullMap != null ) {
				for ( Iterator<PendingPut> it = fullMap.values().iterator(); it.hasNext(); ) {
					PendingPut pp = it.next();
					if (pp.invalidate(now, expirationPeriod)) {
						it.remove();
					}
				}
			}
		}

		/**
		 * Running {@link #gc()} is important when the key is regularly queried but it is not
		 * present in DB. In such case, the putFromLoad would not be called at all and we would
		 * leak pending puts. Cache expiration should handle the case when the pending puts
		 * are not accessed frequently; when these are accessed, we have to do the housekeeping
		 * internally to prevent unlimited growth of the map.
		 * The pending puts will get their timestamps when the map reaches {@link #GC_THRESHOLD}
		 * entries; after expiration period these will be removed completely either through
		 * invalidation or when we try to register next pending put.
		 */
		private void gc() {
			assert fullMap != null;
			long now = regionFactory.nextTimestamp();
			log.tracef("Contains %d, doing GC at %d, expiration %d", size(), now, expirationPeriod);
			for ( Iterator<PendingPut> it = fullMap.values().iterator(); it.hasNext(); ) {
				PendingPut pp = it.next();
				if (pp.gc(now, expirationPeriod)) {
					it.remove();
				}
			}
		}

		public void addInvalidator(Object owner, Object valueForPFER, long now) {
			assert owner != null;
			if (invalidators == null) {
				if (singleInvalidator == null) {
					singleInvalidator = new Invalidator(owner, now, valueForPFER);
					put(new PendingPut(owner));
				}
				else {
					if (singleInvalidator.registeredTimestamp + expirationPeriod < now) {
						// override leaked invalidator
						singleInvalidator = new Invalidator(owner, now, valueForPFER);
						put(new PendingPut(owner));
					}
					invalidators = new HashMap<Object, Invalidator>();
					invalidators.put(singleInvalidator.owner, singleInvalidator);
					// with multiple invalidations the PFER must not be executed
					invalidators.put(owner, new Invalidator(owner, now, null));
					singleInvalidator = null;
				}
			}
			else {
				long allowedRegistration = now - expirationPeriod;
				// remove leaked invalidators
				for (Iterator<Invalidator> it = invalidators.values().iterator(); it.hasNext(); ) {
					if (it.next().registeredTimestamp < allowedRegistration) {
						it.remove();
					}
				}
				// With multiple invalidations in parallel we don't know the order in which
				// the writes were applied into DB and therefore we can't update the cache
				// with the most recent value.
				if (invalidators.isEmpty()) {
					put(new PendingPut(owner));
				}
				else {
					valueForPFER = null;
				}
				invalidators.put(owner, new Invalidator(owner, now, valueForPFER));
			}
		}

		public boolean hasInvalidator() {
			return singleInvalidator != null || (invalidators != null && !invalidators.isEmpty());
		}

		// Debug introspection method, do not use in production code!
		public Collection<Invalidator> getInvalidators() {
			lock.lock();
			try {
				if (singleInvalidator != null) {
					return Collections.singleton(singleInvalidator);
				}
				else if (invalidators != null) {
					return new ArrayList<Invalidator>(invalidators.values());
				}
				else {
					return Collections.EMPTY_LIST;
				}
			}
			finally {
				lock.unlock();
			}
		}

		public void removeInvalidator(Object owner, Object key, long now, boolean doPFER) {
			if (invalidators == null) {
				if (singleInvalidator != null && singleInvalidator.owner.equals(owner)) {
					pferValueIfNeeded(owner, key, singleInvalidator.valueForPFER, doPFER);
					singleInvalidator = null;
				}
			}
			else {
				Invalidator invalidator = invalidators.remove(owner);
				if (invalidator != null) {
					pferValueIfNeeded(owner, key, invalidator.valueForPFER, doPFER);
				}
			}
			lastInvalidationEnd = Math.max(lastInvalidationEnd, now);
		}

		private void pferValueIfNeeded(Object owner, Object key, Object valueForPFER, boolean doPFER) {
			if (valueForPFER != null) {
				PendingPut pendingPut = remove(owner);
				if (doPFER && pendingPut != null && !pendingPut.completed) {
					cache.putForExternalRead(key, valueForPFER);
				}
			}
		}

		public boolean canRemove() {
			return size() == 0 && !hasInvalidator() && lastInvalidationEnd == Long.MIN_VALUE;
		}

		public void setRemoved() {
			removed = true;
		}

		public boolean isRemoved() {
			return removed;
		}
	}

	private static class PendingPut {
		private final Object owner;
		private boolean completed;
		// the timestamp is not filled during registration in order to avoid expensive currentTimeMillis() calls
		private long registeredTimestamp = Long.MIN_VALUE;

		private PendingPut(Object owner) {
			this.owner = owner;
		}

		public String toString() {
			// we can't use SessionImpl.toString() concurrently
			return (completed ? "C@" : "R@") + lockOwnerToString(owner);
		}

		public boolean invalidate(long now, long expirationPeriod) {
			completed = true;
			return gc(now, expirationPeriod);
		}

		public boolean gc(long now, long expirationPeriod) {
			if (registeredTimestamp == Long.MIN_VALUE) {
				registeredTimestamp = now;
			}
			else if (registeredTimestamp + expirationPeriod < now){
				return true; // this is a leaked pending put
			}
			return false;
		}
	}

	private static class Invalidator {
		private final Object owner;
		private final long registeredTimestamp;
		private final Object valueForPFER;

		private Invalidator(Object owner, long registeredTimestamp, Object valueForPFER) {
			this.owner = owner;
			this.registeredTimestamp = registeredTimestamp;
			this.valueForPFER = valueForPFER;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{");
			sb.append("Owner=").append(lockOwnerToString(owner));
			sb.append(", Timestamp=").append(registeredTimestamp);
			sb.append('}');
			return sb.toString();
		}
	}
}
