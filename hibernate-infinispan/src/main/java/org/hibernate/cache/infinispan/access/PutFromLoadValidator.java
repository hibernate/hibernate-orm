/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.util.CacheCommandInitializer;
import org.hibernate.cache.infinispan.util.EndInvalidationCommand;
import org.hibernate.cache.spi.RegionFactory;
import org.infinispan.AdvancedCache;
import org.infinispan.commands.tx.CommitCommand;
import org.infinispan.commands.tx.PrepareCommand;
import org.infinispan.commands.write.InvalidateCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.interceptors.EntryWrappingInterceptor;
import org.infinispan.interceptors.base.BaseRpcInterceptor;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.inboundhandler.DeliverOrder;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.ConcurrentHashSet;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

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
 * <li> Call {@link #registerPendingPut(Object, long)}</li>
 * <li> Read the database</li>
 * <li> Call {@link #acquirePutFromLoadLock(Object, long)}
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
 * <li>or {@link #beginInvalidatingRegion()} followed by {@link #endInvalidatingRegion()}
 *     (for a general invalidation all pending puts)</li>
 * </ul>
 * After transaction commit (when the DB is updated) {@link #endInvalidatingKey(Object)} should
 * be called in order to allow further attempts to cache entry.
 * </p>
 * <p/>
 * <p>
 * This class also supports the concept of "naked puts", which are calls to
 * {@link #acquirePutFromLoadLock(Object, long)} without a preceding {@link #registerPendingPut(Object, long)}.
 * Besides not acquiring lock in {@link #registerPendingPut(Object, long)} this can happen when collection
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
	private static final Log log = LogFactory.getLog(PutFromLoadValidator.class);
	private static final boolean trace = log.isTraceEnabled();

	/**
	 * Used to determine whether the owner of a pending put is a thread or a transaction
	 */
	private final TransactionManager transactionManager;

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
	 * Transactions that invalidate the region. Entries are removed during next invalidation based on transaction status.
	 */
	private final ConcurrentHashSet<Transaction> regionInvalidators = new ConcurrentHashSet<Transaction>();


	/**
	 * Creates a new put from load validator instance.
    *
    * @param cache Cache instance on which to store pending put information.
	 * @param transactionManager Transaction manager
	 */
	public PutFromLoadValidator(AdvancedCache cache, TransactionManager transactionManager) {
		this( cache, cache.getCacheManager(), transactionManager);
	}

   /**
    * Creates a new put from load validator instance.
	*
	* @param cache Cache instance on which to store pending put information.
	* @param cacheManager where to find a cache to store pending put information
	* @param tm transaction manager
	*/
	public PutFromLoadValidator(AdvancedCache cache,
			EmbeddedCacheManager cacheManager, TransactionManager tm) {

		Configuration cacheConfiguration = cache.getCacheConfiguration();
		Configuration pendingPutsConfiguration = cacheManager.getCacheConfiguration(InfinispanRegionFactory.PENDING_PUTS_CACHE_NAME);
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
		configurationBuilder.read(pendingPutsConfiguration);
		configurationBuilder.dataContainer().keyEquivalence(cacheConfiguration.dataContainer().keyEquivalence());
		String pendingPutsName = cache.getName() + "-" + InfinispanRegionFactory.PENDING_PUTS_CACHE_NAME;
		cacheManager.defineConfiguration(pendingPutsName, configurationBuilder.build());

		if (pendingPutsConfiguration.expiration() != null && pendingPutsConfiguration.expiration().maxIdle() > 0) {
			this.expirationPeriod = pendingPutsConfiguration.expiration().maxIdle();
		}
		else {
			throw new IllegalArgumentException("Pending puts cache needs to have maxIdle expiration set!");
		}

		// Since we need to intercept both invalidations of entries that are in the cache and those
		// that are not, we need to use custom interceptor, not listeners (which fire only for present entries).
		if (cacheConfiguration.clustering().cacheMode().isClustered()) {
			RpcManager rpcManager = cache.getComponentRegistry().getComponent(RpcManager.class);
			CacheCommandInitializer cacheCommandInitializer = cache.getComponentRegistry().getComponent(CacheCommandInitializer.class);
			// Note that invalidation does *NOT* acquire locks; therefore, we have to start invalidating before
			// wrapping the entry, since if putFromLoad was invoked between wrap and beginInvalidatingKey, the invalidation
			// would not commit the entry removal (as during wrap the entry was not in cache)
			cache.addInterceptorBefore(new PutFromLoadInterceptor(cache.getName(), rpcManager, cacheCommandInitializer), EntryWrappingInterceptor.class);
			cacheCommandInitializer.addPutFromLoadValidator(cache.getName(), this);
		}

		this.cache = cache;
		this.pendingPuts = cacheManager.getCache(pendingPutsName);
		this.transactionManager = tm;
	}

	/**
	 * This methods should be called only from tests; it removes existing validator from the cache structures
	 * in order to replace it with new one.
	 *
	 * @param cache
    */
	public static void removeFromCache(AdvancedCache cache) {
		List<CommandInterceptor> interceptorChain = cache.getInterceptorChain();
		int index = 0;
		for (; index < interceptorChain.size(); ++index) {
			if (interceptorChain.get(index).getClass().getName().startsWith(PutFromLoadValidator.class.getName())) {
				cache.removeInterceptor(index);
				break;
			}
		}
		CacheCommandInitializer cci = cache.getComponentRegistry().getComponent(CacheCommandInitializer.class);
		cci.removePutFromLoadValidator(cache.getName());
	}

	// ----------------------------------------------------------------- Public

	/**
	 * Marker for lock acquired in {@link #acquirePutFromLoadLock(Object, long)}
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
	 * @param key the key
	 *
	 * @param txTimestamp
	 * @return <code>AcquiredLock</code> if the lock is acquired and the cache put
	 *         can proceed; <code>null</code> if the data should not be cached
	 */
	public Lock acquirePutFromLoadLock(Object key, long txTimestamp) {
		if (trace) {
			log.tracef("acquirePutFromLoadLock(%s#%s, %d)", cache.getName(), key, txTimestamp);
		}
		boolean valid = false;
		boolean locked = false;

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
							}
							else {
								// this is a naked put
								if (pending.hasInvalidator()) {
									valid = false;
								}
								else {
									// if this transaction started after last invalidation we can continue
									valid = txTimestamp > pending.lastInvalidationEnd;
								}
							}
							return valid ? pending : null;
						}
						finally {
							if (!valid) {
								pending.releaseLock();
								locked = false;
							}
							if (trace) {
								log.tracef("acquirePutFromLoadLock(%s#%s, %d) ended with %s", cache.getName(), key, txTimestamp, pending);
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
					if (txTimestamp <= regionInvalidationTimestamp) {
						if (trace) {
							log.tracef("acquirePutFromLoadLock(%s#%s, %d) failed due to invalidated region", cache.getName(), key, txTimestamp);
						}
						return null;
					}

					PendingPut pendingPut = new PendingPut(getOwnerForPut());
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
	 * {@link #acquirePutFromLoadLock(Object, long)}.
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
				pendingPuts.remove( key, pending );
			}
			pending.releaseLock();
		}
	}

	/**
	 * Invalidates all {@link #registerPendingPut(Object, long) previously registered pending puts} ensuring a subsequent call to
	 * {@link #acquirePutFromLoadLock(Object, long)} will return <code>false</code>. <p> This method will block until any
	 * concurrent thread that has {@link #acquirePutFromLoadLock(Object, long) acquired the putFromLoad lock} for the any key has
	 * released the lock. This allows the caller to be certain the putFromLoad will not execute after this method returns,
	 * possibly caching stale data. </p>
	 *
	 * @return <code>true</code> if the invalidation was successful; <code>false</code> if a problem occured (which the
	 *         caller should treat as an exception condition)
	 */
	public boolean beginInvalidatingRegion() {
		if (trace) {
			log.trace("Started invalidating region " + cache.getName());
		}
		boolean ok = true;
		long now = System.currentTimeMillis();
		// deny all puts until endInvalidatingRegion is called; at that time the region should be already
		// in INVALID state, therefore all new requests should be blocked and ongoing should fail by timestamp
		synchronized (this) {
			regionInvalidationTimestamp = Long.MAX_VALUE;
			regionInvalidations++;
		}
		if (transactionManager != null) {
			// cleanup old transactions
			for (Iterator<Transaction> it = regionInvalidators.iterator(); it.hasNext(); ) {
				Transaction tx = it.next();
				try {
					switch (tx.getStatus()) {
						case Status.STATUS_COMMITTED:
						case Status.STATUS_ROLLEDBACK:
						case Status.STATUS_UNKNOWN:
						case Status.STATUS_NO_TRANSACTION:
							it.remove();
					}
				}
				catch (SystemException e) {
					log.error("Cannot retrieve transaction status", e);
				}
			}
			// add this transaction
			try {
				Transaction tx = transactionManager.getTransaction();
				if (tx != null) {
					regionInvalidators.add(tx);
				}
			}
			catch (SystemException e) {
				log.error("TransactionManager failed to provide transaction", e);
				return false;
			}
		}

		try {
			// Acquire the lock for each entry to ensure any ongoing
			// work associated with it is completed before we return
			for (Iterator<PendingPutMap> it = pendingPuts.values().iterator(); it.hasNext(); ) {
				PendingPutMap entry = it.next();
				if (entry.acquireLock(60, TimeUnit.SECONDS)) {
					try {
						entry.invalidate(now, expirationPeriod);
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
				regionInvalidationTimestamp = System.currentTimeMillis();
			}
		}
		if (trace) {
			log.trace("Finished invalidating region " + cache.getName());
		}
	}

	/**
	 * Notifies this validator that it is expected that a database read followed by a subsequent {@link
	 * #acquirePutFromLoadLock(Object, long)} call will occur. The intent is this method would be called following a cache miss
	 * wherein it is expected that a database read plus cache put will occur. Calling this method allows the validator to
	 * treat the subsequent <code>acquirePutFromLoadLock</code> as if the database read occurred when this method was
	 * invoked. This allows the validator to compare the timestamp of this call against the timestamp of subsequent removal
	 * notifications.
	 *
	 * @param key key that will be used for subsequent cache put
	 * @param txTimestamp
	 */
	public void registerPendingPut(Object key, long txTimestamp) {
		long invalidationTimestamp = this.regionInvalidationTimestamp;
		if (txTimestamp <= invalidationTimestamp) {
			boolean skip;
			if (invalidationTimestamp == Long.MAX_VALUE) {
				// there is ongoing invalidation of pending puts
				skip = true;
			}
			else {
				Transaction tx = null;
				if (transactionManager != null) {
					try {
						tx = transactionManager.getTransaction();
					}
					catch (SystemException e) {
						log.error("TransactionManager failed to provide transaction", e);
					}
				}
				skip = tx == null || !regionInvalidators.contains(tx);
			}
			if (skip) {
				if (trace) {
					log.tracef("registerPendingPut(%s#%s, %d) skipped due to region invalidation (%d)", cache.getName(), key, txTimestamp, invalidationTimestamp);
				}
				return;
			}
		}

		final PendingPut pendingPut = new PendingPut( getOwnerForPut() );
		final PendingPutMap pendingForKey = new PendingPutMap( pendingPut );

		final PendingPutMap existing = pendingPuts.putIfAbsent( key, pendingForKey );
		if ( existing != null ) {
			if ( existing.acquireLock( 10, TimeUnit.SECONDS ) ) {
				try {
					if ( !existing.hasInvalidator() ) {
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
	}

	/**
	 * Calls {@link #beginInvalidatingKey(Object, Object)} with current transaction or thread.
	 * @param key
	 * @return
    */
	public boolean beginInvalidatingKey(Object key) {
		return beginInvalidatingKey(key, getOwnerForPut());
	}

	/**
	 * Invalidates any {@link #registerPendingPut(Object, long) previously registered pending puts}
	 * and disables further registrations ensuring a subsequent call to {@link #acquirePutFromLoadLock(Object, long)}
	 * will return <code>false</code>. <p> This method will block until any concurrent thread that has
	 * {@link #acquirePutFromLoadLock(Object, long) acquired the putFromLoad lock} for the given key
	 * has released the lock. This allows the caller to be certain the putFromLoad will not execute after this method
	 * returns, possibly caching stale data. </p>
	 * After this transaction completes, {@link #endInvalidatingKey(Object)} needs to be called }
	 *
	 * @param key key identifying data whose pending puts should be invalidated
	 *
	 * @return <code>true</code> if the invalidation was successful; <code>false</code> if a problem occured (which the
	 *         caller should treat as an exception condition)
	 */
	public boolean beginInvalidatingKey(Object key, Object lockOwner) {
		PendingPutMap pending = new PendingPutMap(null);
		PendingPutMap prev = pendingPuts.putIfAbsent(key, pending);
		if (prev != null) {
			pending = prev;
		}
		if (pending.acquireLock(60, TimeUnit.SECONDS)) {
			try {
				long now = System.currentTimeMillis();
				pending.invalidate(now, expirationPeriod);
				pending.addInvalidator(lockOwner, now, expirationPeriod);
			}
			finally {
				pending.releaseLock();
			}
			if (trace) {
				log.tracef("beginInvalidatingKey(%s#%s, %s) ends with %s", cache.getName(), key, lockOwner, pending);
			}
			return true;
		}
		else {
			log.tracef("beginInvalidatingKey(%s#%s, %s) failed to acquire lock", cache.getName(), key);
			return false;
		}
	}

	/**
	 * Calls {@link #endInvalidatingKey(Object, Object)} with current transaction or thread.
	 * @param key
	 * @return
    */
	public boolean endInvalidatingKey(Object key) {
		return endInvalidatingKey(key, getOwnerForPut());
	}

	/**
	 * Called after the transaction completes, allowing caching of entries. It is possible that this method
	 * is called without previous invocation of {@link #beginInvalidatingKey(Object)}, then it should be a no-op.
	 *
	 * @param key
	 * @param lockOwner owner of the invalidation - transaction or thread
	 * @return
    */
	public boolean endInvalidatingKey(Object key, Object lockOwner) {
		PendingPutMap pending = pendingPuts.get(key);
		if (pending == null) {
			if (trace) {
				log.tracef("endInvalidatingKey(%s#%s, %s) could not find pending puts", cache.getName(), key, lockOwner);
			}
			return true;
		}
		if (pending.acquireLock(60, TimeUnit.SECONDS)) {
			try {
				long now = System.currentTimeMillis();
				pending.removeInvalidator(lockOwner, now);
				// we can't remove the pending put yet because we wait for naked puts
				// pendingPuts should be configured with maxIdle time so won't have memory leak
				return true;
			}
			finally {
				pending.releaseLock();
				if (trace) {
					log.tracef("endInvalidatingKey(%s#%s, %s) ends with %s", cache.getName(), key, lockOwner, pending);
				}
			}
		}
		else {
			if (trace) {
				log.tracef("endInvalidatingKey(%s#%s, %s) failed to acquire lock", cache.getName(), key, lockOwner);
			}
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
		private Invalidator singleInvalidator;
		private Map<Object, Invalidator> invalidators;
		private long lastInvalidationEnd = Long.MIN_VALUE;

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
							sb.append(invalidators);
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
					return sb.append("}").toString();
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

		public void invalidate(long now, long expirationPeriod) {
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

		public void addInvalidator(Object owner, long now, long invalidatorTimeout) {
			assert owner != null;
			if (invalidators == null) {
				if (singleInvalidator == null) {
					singleInvalidator = new Invalidator(owner, now);
				}
				else {
					if (singleInvalidator.registeredTimestamp + invalidatorTimeout < now) {
						// remove leaked invalidator
						singleInvalidator = new Invalidator(owner, now);
					}
					invalidators = new HashMap<Object, Invalidator>();
					invalidators.put(singleInvalidator.owner, singleInvalidator);
					invalidators.put(owner, new Invalidator(owner, now));
					singleInvalidator = null;
				}
			}
			else {
				long allowedRegistration = now - invalidatorTimeout;
				// remove leaked invalidators
				for (Iterator<Invalidator> it = invalidators.values().iterator(); it.hasNext(); ) {
					if (it.next().registeredTimestamp < allowedRegistration) {
						it.remove();
					}
				}
				invalidators.put(owner, new Invalidator(owner, now));
			}
		}

		public boolean hasInvalidator() {
			return singleInvalidator != null || (invalidators != null && !invalidators.isEmpty());
		}

		public void removeInvalidator(Object owner, long now) {
			if (invalidators == null) {
				if (singleInvalidator != null && singleInvalidator.owner.equals(owner)) {
					singleInvalidator = null;
				}
			}
			else {
				invalidators.remove(owner);
			}
			lastInvalidationEnd = Math.max(lastInvalidationEnd, now);
		}

		public boolean canRemove() {
			return size() == 0 && !hasInvalidator() && lastInvalidationEnd == Long.MIN_VALUE;
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
			return (completed ? "C@" : "R@") + owner;
		}

		public boolean invalidate(long now, long expirationPeriod) {
			completed = true;
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

		private Invalidator(Object owner, long registeredTimestamp) {
			this.owner = owner;
			this.registeredTimestamp = registeredTimestamp;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{");
			sb.append("Owner=").append(owner);
			sb.append(", Timestamp=").append(registeredTimestamp);
			sb.append('}');
			return sb.toString();
		}
	}

	private class PutFromLoadInterceptor extends BaseRpcInterceptor {
		private final String cacheName;
		private final RpcManager rpcManager;
		private final CacheCommandInitializer cacheCommandInitializer;

		public PutFromLoadInterceptor(String cacheName, RpcManager rpcManager, CacheCommandInitializer cacheCommandInitializer) {
			this.cacheName = cacheName;
			this.rpcManager = rpcManager;
			this.cacheCommandInitializer = cacheCommandInitializer;
		}

		// We need to intercept PrepareCommand, not InvalidateCommand since the interception takes
		// place before EntryWrappingInterceptor and the PrepareCommand is multiplexed into InvalidateCommands
		// as part of EntryWrappingInterceptor
		@Override
		public Object visitPrepareCommand(TxInvocationContext ctx, PrepareCommand command) throws Throwable {
			if (!ctx.isOriginLocal()) {
				for (WriteCommand wc : command.getModifications()) {
					if (wc instanceof InvalidateCommand) {
						// InvalidateCommand does not correctly implement getAffectedKeys()
						for (Object key : ((InvalidateCommand) wc).getKeys()) {
							beginInvalidatingKey(key, ctx.getLockOwner());
						}
					}
					else {
						for (Object key : wc.getAffectedKeys()) {
							beginInvalidatingKey(key, ctx.getLockOwner());
						}
					}
				}
			}
			return invokeNextInterceptor(ctx, command);
		}

		@Override
		public Object visitCommitCommand(TxInvocationContext ctx, CommitCommand command) throws Throwable {
			try {
				if (ctx.isOriginLocal()) {
					// send async Commit
					Set<Object> affectedKeys = ctx.getAffectedKeys();
					if (!affectedKeys.isEmpty()) {
						EndInvalidationCommand commitCommand = cacheCommandInitializer.buildEndInvalidationCommand(
								cacheName, affectedKeys.toArray(), ctx.getGlobalTransaction());
						rpcManager.invokeRemotely(null, commitCommand, rpcManager.getDefaultRpcOptions(false, DeliverOrder.NONE));
					}
				}
			}
			finally {
				return invokeNextInterceptor(ctx, command);
			}
		}
	}
}
