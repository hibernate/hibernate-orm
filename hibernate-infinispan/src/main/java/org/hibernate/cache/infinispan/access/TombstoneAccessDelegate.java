/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import java.util.concurrent.TimeUnit;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.FutureUpdate;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.cache.infinispan.util.Tombstone;
import org.hibernate.cache.infinispan.util.TombstoneUpdate;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class TombstoneAccessDelegate implements AccessDelegate {
	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( TombstoneAccessDelegate.class );

	protected final BaseTransactionalDataRegion region;
	protected final AdvancedCache cache;
	protected final AdvancedCache writeCache;
	protected final AdvancedCache asyncWriteCache;
	protected final AdvancedCache putFromLoadCache;
	protected final boolean requiresTransaction;

	public TombstoneAccessDelegate(BaseTransactionalDataRegion region) {
		this.region = region;
		this.cache = region.getCache();
		this.writeCache = Caches.ignoreReturnValuesCache(cache);
		this.asyncWriteCache = Caches.asyncWriteCache(cache, Flag.IGNORE_RETURN_VALUES);
		this.putFromLoadCache = writeCache.withFlags( Flag.ZERO_LOCK_ACQUISITION_TIMEOUT, Flag.FAIL_SILENTLY );
		Configuration configuration = cache.getCacheConfiguration();
		if (configuration.clustering().cacheMode().isInvalidation()) {
			throw new IllegalArgumentException("For tombstone-based caching, invalidation cache is not allowed.");
		}
		if (configuration.transaction().transactionMode().isTransactional()) {
			throw new IllegalArgumentException("Currently transactional caches are not supported.");
		}
		requiresTransaction = configuration.transaction().transactionMode().isTransactional()
				&& !configuration.transaction().autoCommit();
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		if (txTimestamp < region.getLastRegionInvalidation() ) {
			return null;
		}
		Object value = cache.get(key);
		if (value instanceof Tombstone) {
			return null;
		}
		else if (value instanceof FutureUpdate) {
			return ((FutureUpdate) value).getValue();
		}
		else {
			return value;
		}
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version) {
		return putFromLoad(session, key, value, txTimestamp, version, false);
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride) throws CacheException {
		long lastRegionInvalidation = region.getLastRegionInvalidation();
		if (txTimestamp < lastRegionInvalidation) {
			log.tracef("putFromLoad not executed since tx started at %d, beforeQuery last region invalidation finished = %d", txTimestamp, lastRegionInvalidation);
			return false;
		}
		if (minimalPutOverride) {
			Object prev = cache.get(key);
			if (prev instanceof Tombstone) {
				Tombstone tombstone = (Tombstone) prev;
				long lastTimestamp = tombstone.getLastTimestamp();
				if (txTimestamp <= lastTimestamp) {
					log.tracef("putFromLoad not executed since tx started at %d, beforeQuery last invalidation finished = %d", txTimestamp, lastTimestamp);
					return false;
				}
			}
			else if (prev != null) {
				log.tracef("putFromLoad not executed since cache contains %s", prev);
				return false;
			}
		}
		// we can't use putForExternalRead since the PFER flag means that entry is not wrapped into context
		// when it is present in the container. TombstoneCallInterceptor will deal with this.
		putFromLoadCache.put(key, new TombstoneUpdate(session.getTimestamp(), value));
		return true;
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		write(session, key, value);
		return true;
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
		write(session, key, value);
		return true;
	}

	protected void write(SharedSessionContractImplementor session, Object key, Object value) {
		TransactionCoordinator tc = session.getTransactionCoordinator();
		FutureUpdateSynchronization sync = new FutureUpdateSynchronization(tc, writeCache, requiresTransaction, key, value);
		// FutureUpdate is handled in TombstoneCallInterceptor
		writeCache.put(key, new FutureUpdate(sync.getUuid(), null), region.getTombstoneExpiration(), TimeUnit.MILLISECONDS);
		tc.getLocalSynchronizations().registerSynchronization(sync);
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		TransactionCoordinator transactionCoordinator = session.getTransactionCoordinator();
		TombstoneSynchronization sync = new TombstoneSynchronization(transactionCoordinator, asyncWriteCache, requiresTransaction, region, key);
		Tombstone tombstone = new Tombstone(sync.getUuid(), session.getTimestamp() + region.getTombstoneExpiration(), false);
		writeCache.put(key, tombstone, region.getTombstoneExpiration(), TimeUnit.MILLISECONDS);
		transactionCoordinator.getLocalSynchronizations().registerSynchronization(sync);
	}

	@Override
	public void removeAll() throws CacheException {
		region.beginInvalidation();
		try {
			Caches.broadcastEvictAll(cache);
		}
		finally {
			region.endInvalidation();
		}
	}

	@Override
	public void evict(Object key) throws CacheException {
		writeCache.put(key, TombstoneUpdate.EVICT);
	}

	@Override
	public void evictAll() throws CacheException {
		region.beginInvalidation();
		try {
			Caches.broadcastEvictAll(cache);
		}
		finally {
			region.endInvalidation();
		}
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key) throws CacheException {
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
		return false;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) {
		return false;
	}
}
