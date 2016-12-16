/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.impl;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.access.AccessDelegate;
import org.hibernate.cache.infinispan.access.LockingInterceptor;
import org.hibernate.cache.infinispan.access.NonStrictAccessDelegate;
import org.hibernate.cache.infinispan.access.NonTxInvalidationCacheAccessDelegate;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.access.TombstoneAccessDelegate;
import org.hibernate.cache.infinispan.access.TombstoneCallInterceptor;
import org.hibernate.cache.infinispan.access.TxInvalidationCacheAccessDelegate;
import org.hibernate.cache.infinispan.access.UnorderedDistributionInterceptor;
import org.hibernate.cache.infinispan.access.VersionedCallInterceptor;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.FutureUpdate;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.cache.infinispan.util.Tombstone;
import org.hibernate.cache.infinispan.util.VersionedEntry;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.TransactionalDataRegion;

import org.hibernate.cache.spi.access.AccessType;
import org.infinispan.AdvancedCache;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.filter.KeyValueFilter;
import org.infinispan.interceptors.CallInterceptor;
import org.infinispan.interceptors.EntryWrappingInterceptor;
import org.infinispan.interceptors.base.CommandInterceptor;
import org.infinispan.interceptors.distribution.NonTxDistributionInterceptor;
import org.infinispan.interceptors.locking.NonTransactionalLockingInterceptor;

import javax.transaction.TransactionManager;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Support for Inifinispan {@link org.hibernate.cache.spi.TransactionalDataRegion} implementors.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class BaseTransactionalDataRegion
		extends BaseRegion implements TransactionalDataRegion {
	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( BaseTransactionalDataRegion.class );
	private final CacheDataDescription metadata;
	private final CacheKeysFactory cacheKeysFactory;
	private final boolean requiresTransaction;

	private long tombstoneExpiration;
	private PutFromLoadValidator validator;

	private AccessType accessType;
	private Strategy strategy;

	protected enum Strategy {
		NONE, VALIDATION, TOMBSTONES, VERSIONED_ENTRIES
	}

	/**
	 * Base transactional region constructor
	 *  @param cache instance to store transactional data
	 * @param name of the transactional region
	 * @param transactionManager
	 * @param metadata for the transactional region
	 * @param factory for the transactional region
	 * @param cacheKeysFactory factory for cache keys
	 */
	public BaseTransactionalDataRegion(
			AdvancedCache cache, String name, TransactionManager transactionManager,
			CacheDataDescription metadata, InfinispanRegionFactory factory, CacheKeysFactory cacheKeysFactory) {
		super( cache, name, transactionManager, factory);
		this.metadata = metadata;
		this.cacheKeysFactory = cacheKeysFactory;

		Configuration configuration = cache.getCacheConfiguration();
		requiresTransaction = configuration.transaction().transactionMode().isTransactional()
				&& !configuration.transaction().autoCommit();
		tombstoneExpiration = factory.getPendingPutsCacheConfiguration().expiration().maxIdle();
		if (!isRegionAccessStrategyEnabled()) {
			strategy = Strategy.NONE;
		}
	}

	/**
	 * @return True if this region is accessed through RegionAccessStrategy, false if it is accessed directly.
    */
	protected boolean isRegionAccessStrategyEnabled() {
		return true;
	}

	@Override
	public CacheDataDescription getCacheDataDescription() {
		return metadata;
	}

	public CacheKeysFactory getCacheKeysFactory() {
		return cacheKeysFactory;
	}

	protected synchronized AccessDelegate createAccessDelegate(AccessType accessType) {
		if (accessType == null) {
			throw new IllegalArgumentException();
		}
		if (this.accessType != null && !this.accessType.equals(accessType)) {
			throw new IllegalStateException("This region was already set up for " + this.accessType + ", cannot use using " + accessType);
		}
		this.accessType = accessType;

		CacheMode cacheMode = cache.getCacheConfiguration().clustering().cacheMode();
		if (accessType == AccessType.NONSTRICT_READ_WRITE) {
			prepareForVersionedEntries();
			return new NonStrictAccessDelegate(this);
		}
		if (cacheMode.isDistributed() || cacheMode.isReplicated()) {
			prepareForTombstones();
			return new TombstoneAccessDelegate(this);
		}
		else {
			prepareForValidation();
			if (cache.getCacheConfiguration().transaction().transactionMode().isTransactional()) {
				return new TxInvalidationCacheAccessDelegate(this, validator);
			}
			else {
				return new NonTxInvalidationCacheAccessDelegate(this, validator);
			}
		}
	}

	protected void prepareForValidation() {
		if (strategy != null) {
			assert strategy == Strategy.VALIDATION;
			return;
		}
		validator = new PutFromLoadValidator(cache, factory);
		strategy = Strategy.VALIDATION;
	}

	protected void prepareForVersionedEntries() {
		if (strategy != null) {
			assert strategy == Strategy.VERSIONED_ENTRIES;
			return;
		}

		replaceCommonInterceptors();

		cache.removeInterceptor(CallInterceptor.class);
		VersionedCallInterceptor tombstoneCallInterceptor = new VersionedCallInterceptor(this, metadata.getVersionComparator());
		cache.getComponentRegistry().registerComponent(tombstoneCallInterceptor, VersionedCallInterceptor.class);
		List<CommandInterceptor> interceptorChain = cache.getInterceptorChain();
		cache.addInterceptor(tombstoneCallInterceptor, interceptorChain.size());

		strategy = Strategy.VERSIONED_ENTRIES;
	}

	private void prepareForTombstones() {
		if (strategy != null) {
			assert strategy == Strategy.TOMBSTONES;
			return;
		}
		Configuration configuration = cache.getCacheConfiguration();
		if (configuration.eviction().maxEntries() >= 0) {
			log.evictionWithTombstones();
		}

		replaceCommonInterceptors();

		cache.removeInterceptor(CallInterceptor.class);
		TombstoneCallInterceptor tombstoneCallInterceptor = new TombstoneCallInterceptor(this);
		cache.getComponentRegistry().registerComponent(tombstoneCallInterceptor, TombstoneCallInterceptor.class);
		List<CommandInterceptor> interceptorChain = cache.getInterceptorChain();
		cache.addInterceptor(tombstoneCallInterceptor, interceptorChain.size());

		strategy = Strategy.TOMBSTONES;
	}

	private void replaceCommonInterceptors() {
		CacheMode cacheMode = cache.getCacheConfiguration().clustering().cacheMode();
		if (!cacheMode.isReplicated() && !cacheMode.isDistributed()) {
			return;
		}

		LockingInterceptor lockingInterceptor = new LockingInterceptor();
		cache.getComponentRegistry().registerComponent(lockingInterceptor, LockingInterceptor.class);
		if (!cache.addInterceptorBefore(lockingInterceptor, NonTransactionalLockingInterceptor.class)) {
			throw new IllegalStateException("Misconfigured cache, interceptor chain is " + cache.getInterceptorChain());
		}
		cache.removeInterceptor(NonTransactionalLockingInterceptor.class);

		UnorderedDistributionInterceptor distributionInterceptor = new UnorderedDistributionInterceptor();
		cache.getComponentRegistry().registerComponent(distributionInterceptor, UnorderedDistributionInterceptor.class);
		if (!cache.addInterceptorBefore(distributionInterceptor, NonTxDistributionInterceptor.class)) {
			throw new IllegalStateException("Misconfigured cache, interceptor chain is " + cache.getInterceptorChain());
		}
		cache.removeInterceptor(NonTxDistributionInterceptor.class);

		EntryWrappingInterceptor ewi = cache.getComponentRegistry().getComponent(EntryWrappingInterceptor.class);
		try {
			Field isUsingLockDelegation = EntryWrappingInterceptor.class.getDeclaredField("isUsingLockDelegation");
			isUsingLockDelegation.setAccessible(true);
			isUsingLockDelegation.set(ewi, false);
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			throw new IllegalStateException(e);
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
		if (strategy == null) {
			throw new IllegalStateException("Strategy was not set");
		}
		switch (strategy) {
			case NONE:
			case VALIDATION:
				super.runInvalidation(inTransaction);
				return;
			case TOMBSTONES:
				removeEntries(inTransaction, Tombstone.EXCLUDE_TOMBSTONES);
				return;
			case VERSIONED_ENTRIES:
				removeEntries(inTransaction, VersionedEntry.EXCLUDE_EMPTY_EXTRACT_VALUE);
				return;
		}
	}

	private void removeEntries(boolean inTransaction, KeyValueFilter filter) {
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
			long now = nextTimestamp();
			try {
				while (it.hasNext()) {
					// Cannot use it.next(); it.remove() due to ISPN-5653
					CacheEntry entry = it.next();
					switch (strategy) {
						case TOMBSTONES:
							localCache.remove(entry.getKey(), entry.getValue());
							break;
						case VERSIONED_ENTRIES:
							localCache.put(entry.getKey(), new VersionedEntry(null, null, now), tombstoneExpiration, TimeUnit.MILLISECONDS);
							break;
					}
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
		if (strategy == null) {
			throw new IllegalStateException("Strategy was not set");
		}
		switch (strategy) {
			case NONE:
			case VALIDATION:
				return super.toMap();
			case TOMBSTONES:
				return Caches.entrySet(Caches.localCache(cache), Tombstone.EXCLUDE_TOMBSTONES).toMap();
			case VERSIONED_ENTRIES:
				return Caches.entrySet(Caches.localCache(cache), VersionedEntry.EXCLUDE_EMPTY_EXTRACT_VALUE, VersionedEntry.EXCLUDE_EMPTY_EXTRACT_VALUE).toMap();
			default:
				throw new IllegalStateException(strategy.toString());
		}
	}

	@Override
	public boolean contains(Object key) {
		if (!checkValid()) {
			return false;
		}
		Object value = cache.get(key);
		if (value instanceof Tombstone) {
			return false;
		}
		if (value instanceof FutureUpdate) {
			return ((FutureUpdate) value).getValue() != null;
		}
		if (value instanceof VersionedEntry) {
			return ((VersionedEntry) value).getValue() != null;
		}
		return value != null;
	}
}
