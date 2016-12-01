/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.query;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.cache.infinispan.util.InvocationAfterCompletion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.context.Flag;
import org.infinispan.transaction.TransactionMode;

/**
 * Region for caching query results.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class QueryResultsRegionImpl extends BaseTransactionalDataRegion implements QueryResultsRegion {
	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( QueryResultsRegionImpl.class );

	private final AdvancedCache evictCache;
	private final AdvancedCache putCache;
	private final AdvancedCache getCache;
	private final ConcurrentMap<SharedSessionContractImplementor, Map> transactionContext = new ConcurrentHashMap<SharedSessionContractImplementor, Map>();
	private final boolean putCacheRequiresTransaction;

	/**
	 * Query region constructor
	 *  @param cache instance to store queries
	 * @param name of the query region
	 * @param factory for the query region
	 */
	public QueryResultsRegionImpl(AdvancedCache cache, String name, TransactionManager transactionManager, InfinispanRegionFactory factory) {
		super( cache, name, transactionManager, null, factory, null );
		// If Infinispan is using INVALIDATION for query cache, we don't want to propagate changes.
		// We use the Timestamps cache to manage invalidation
		final boolean localOnly = Caches.isInvalidationCache( cache );

		this.evictCache = localOnly ? Caches.localCache( cache ) : cache;

		this.putCache = localOnly ?
				Caches.failSilentWriteCache( cache, Flag.CACHE_MODE_LOCAL ) :
				Caches.failSilentWriteCache( cache );

		this.getCache = Caches.failSilentReadCache( cache );

		TransactionConfiguration transactionConfiguration = putCache.getCacheConfiguration().transaction();
		boolean transactional = transactionConfiguration.transactionMode() != TransactionMode.NON_TRANSACTIONAL;
		this.putCacheRequiresTransaction = transactional && !transactionConfiguration.autoCommit();
		// Since we execute the query update explicitly form transaction synchronization, the putCache does not need
		// to be transactional anymore (it had to be in the past to prevent revealing uncommitted changes).
		if (transactional) {
			log.useNonTransactionalQueryCache();
		}

	}

	@Override
	protected boolean isRegionAccessStrategyEnabled() {
		return false;
	}

	@Override
	public void evict(Object key) throws CacheException {
		for (Map map : transactionContext.values()) {
			map.remove(key);
		}
		evictCache.remove( key );
	}

	@Override
	public void evictAll() throws CacheException {
		transactionContext.clear();
		final Transaction tx = suspend();
		try {
			// Invalidate the local region and then go remote
			invalidateRegion();
			Caches.broadcastEvictAll( cache );
		}
		finally {
			resume( tx );
		}
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key) throws CacheException {
		if ( !checkValid() ) {
			return null;
		}

		// In Infinispan get doesn't acquire any locks, so no need to suspend the tx.
		// In the past, when get operations acquired locks, suspending the tx was a way
		// to avoid holding locks that would prevent updates.
		// Add a zero (or low) timeout option so we don't block
		// waiting for tx's that did a put to commit
		Object result = null;
		Map map = transactionContext.get(session);
		if (map != null) {
			result = map.get(key);
		}
		if (result == null) {
			result = getCache.get( key );
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void put(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		if ( checkValid() ) {
			// See HHH-7898: Even with FAIL_SILENTLY flag, failure to write in transaction
			// fails the whole transaction. It is an Infinispan quirk that cannot be fixed
			// ISPN-5356 tracks that. This is because if the transaction continued the
			// value could be committed on backup owners, including the failed operation,
			// and the result would not be consistent.
			TransactionCoordinator tc = session.getTransactionCoordinator();
			if (tc != null && tc.isJoined()) {
				tc.getLocalSynchronizations().registerSynchronization(new PostTransactionQueryUpdate(tc, session, key, value));
				// no need to synchronize as the transaction will be accessed by only one thread
				Map map = transactionContext.get(session);
				if (map == null) {
					transactionContext.put(session, map = new HashMap());
				}
				map.put(key, value);
				return;
			}
			// Here we don't want to suspend the tx. If we do:
			// 1) We might be caching query results that reflect uncommitted
			// changes. No tx == no WL on cache node, so other threads
			// can prematurely see those query results
			// 2) No tx == immediate replication. More overhead, plus we
			// spread issue #1 above around the cluster

			// Add a zero (or quite low) timeout option so we don't block.
			// Ignore any TimeoutException. Basically we forego caching the
			// query result in order to avoid blocking.
			// Reads are done with suspended tx, so they should not hold the
			// lock for long.  Not caching the query result is OK, since
			// any subsequent read will just see the old result with its
			// out-of-date timestamp; that result will be discarded and the
			// db query performed again.
			putCache.put( key, value );
		}
	}

	private class PostTransactionQueryUpdate extends InvocationAfterCompletion {
		private final SharedSessionContractImplementor session;
		private final Object key;
		private final Object value;

		public PostTransactionQueryUpdate(TransactionCoordinator tc, SharedSessionContractImplementor session, Object key, Object value) {
			super(tc, putCacheRequiresTransaction);
			this.session = session;
			this.key = key;
			this.value = value;
		}

		@Override
		public void afterCompletion(int status) {
			transactionContext.remove(session);
			super.afterCompletion(status);
		}

		@Override
		protected void invoke(boolean success) {
			if (success) {
				putCache.put(key, value);
			}
		}
	}
}
