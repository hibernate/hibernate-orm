/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.impl;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.access.AccessDelegate;
import org.hibernate.cache.infinispan.access.NonTxInvalidationCacheAccessDelegate;
import org.hibernate.cache.infinispan.access.TombstoneAccessDelegate;
import org.hibernate.cache.infinispan.access.TombstoneCallInterceptor;
import org.hibernate.cache.infinispan.access.TxInvalidationCacheAccessDelegate;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.FutureUpdate;
import org.hibernate.cache.infinispan.util.Tombstone;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TransactionalDataRegion;

import org.infinispan.AdvancedCache;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.interceptors.CallInterceptor;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import javax.transaction.TransactionManager;

import java.util.List;
import java.util.Map;

/**
 * Support for Inifinispan {@link org.hibernate.cache.spi.TransactionalDataRegion} implementors.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class BaseTransactionalDataRegion
		extends BaseRegion implements TransactionalDataRegion {
	private static final Log log = LogFactory.getLog( BaseTransactionalDataRegion.class );
	private final CacheDataDescription metadata;
	private final CacheKeysFactory cacheKeysFactory;

	protected final boolean useTombstones;
	protected final long tombstoneExpiration;
	protected final boolean requiresTransaction;

	/**
	 * Base transactional region constructor
	 *
	 * @param cache instance to store transactional data
	 * @param name of the transactional region
	 * @param transactionManager
	 * @param metadata for the transactional region
	 * @param factory for the transactional region
	 * @param cacheKeysFactory factory for cache keys
	 */
	public BaseTransactionalDataRegion(
			AdvancedCache cache, String name, TransactionManager transactionManager,
			CacheDataDescription metadata, RegionFactory factory, CacheKeysFactory cacheKeysFactory) {
		super( cache, name, transactionManager, factory);
		this.metadata = metadata;
		this.cacheKeysFactory = cacheKeysFactory;

		Configuration configuration = cache.getCacheConfiguration();
		CacheMode cacheMode = configuration.clustering().cacheMode();

		useTombstones = cacheMode.isDistributed() || cacheMode.isReplicated();
		// TODO: make these timeouts configurable
		tombstoneExpiration = InfinispanRegionFactory.PENDING_PUTS_CACHE_CONFIGURATION.expiration().maxIdle();
		requiresTransaction = configuration.transaction().transactionMode().isTransactional()
				&& !configuration.transaction().autoCommit();

		if (useTombstones) {
			if (configuration.eviction().maxEntries() >= 0) {
				log.warn("Setting eviction on cache using tombstones can introduce inconsistencies!");
			}

			cache.removeInterceptor(CallInterceptor.class);
			TombstoneCallInterceptor tombstoneCallInterceptor = new TombstoneCallInterceptor(tombstoneExpiration);
			cache.getComponentRegistry().registerComponent(tombstoneCallInterceptor, TombstoneCallInterceptor.class);
			List<CommandInterceptor> interceptorChain = cache.getInterceptorChain();
			cache.addInterceptor(tombstoneCallInterceptor, interceptorChain.size());
		}
	}

	@Override
	public CacheDataDescription getCacheDataDescription() {
		return metadata;
	}

	public CacheKeysFactory getCacheKeysFactory() {
		return cacheKeysFactory;
	}

	protected AccessDelegate createAccessDelegate() {
		CacheMode cacheMode = cache.getCacheConfiguration().clustering().cacheMode();
		if (cacheMode.isDistributed() || cacheMode.isReplicated()) {
			return new TombstoneAccessDelegate(this);
		}
		else {
			if (cache.getCacheConfiguration().transaction().transactionMode().isTransactional()) {
				return new TxInvalidationCacheAccessDelegate(this, getValidator());
			}
			else {
				return new NonTxInvalidationCacheAccessDelegate(this, getValidator());
			}
		}
	}

	public long getTombstoneExpiration() {
		return tombstoneExpiration;
	}

	public long getLastRegionInvalidation() {
		return lastRegionInvalidation;
	}

	@Override
	protected void runInvalidation(boolean inTransaction) {
		if (!useTombstones) {
			super.runInvalidation(inTransaction);
			return;
		}
		// If the transaction is required, we simply need it -> will create our own
		boolean startedTx = false;
		if ( !inTransaction && requiresTransaction) {
			try {
				tm.begin();
				startedTx = true;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		// We can never use cache.clear() since tombstones must be kept.
		try {
			AdvancedCache localCache = Caches.localCache(cache);
			CloseableIterator<CacheEntry> it = Caches.entrySet(localCache, Tombstone.EXCLUDE_TOMBSTONES).iterator();
			try {
				while (it.hasNext()) {
					// Cannot use it.next(); it.remove() due to ISPN-5653
					CacheEntry entry = it.next();
					localCache.remove(entry.getKey(), entry.getValue());
				}
			}
			finally {
				it.close();
			}
		}
		finally {
			if (startedTx) {
				try {
					tm.commit();
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public Map toMap() {
		if (useTombstones) {
			AdvancedCache localCache = Caches.localCache(cache);
			return Caches.entrySet(localCache, Tombstone.EXCLUDE_TOMBSTONES, FutureUpdate.VALUE_EXTRACTOR).toMap();
		}
		else {
			return super.toMap();
		}
	}
}
