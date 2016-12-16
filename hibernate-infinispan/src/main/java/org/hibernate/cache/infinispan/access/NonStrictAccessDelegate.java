/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.access;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.cache.infinispan.util.VersionedEntry;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;

/**
 * Access delegate that relaxes the consistency a bit: stale reads are prohibited only after the transaction
 * commits. This should also be able to work with async caches, and that would allow the replication delay
 * even after the commit.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class NonStrictAccessDelegate implements AccessDelegate {
	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog( NonStrictAccessDelegate.class );
	private static final boolean trace = log.isTraceEnabled();

	private final BaseTransactionalDataRegion region;
	private final AdvancedCache cache;
	private final AdvancedCache writeCache;
	private final AdvancedCache putFromLoadCache;
	private final Comparator versionComparator;


	public NonStrictAccessDelegate(BaseTransactionalDataRegion region) {
		this.region = region;
		this.cache = region.getCache();
		this.writeCache = Caches.ignoreReturnValuesCache(cache);
		// Note that correct behaviour of local and async writes depends on LockingInterceptor (see there for details)
		this.putFromLoadCache = writeCache.withFlags( Flag.ZERO_LOCK_ACQUISITION_TIMEOUT, Flag.FAIL_SILENTLY, Flag.FORCE_ASYNCHRONOUS );
		Configuration configuration = cache.getCacheConfiguration();
		if (configuration.clustering().cacheMode().isInvalidation()) {
			throw new IllegalArgumentException("Nonstrict-read-write mode cannot use invalidation.");
		}
		if (configuration.transaction().transactionMode().isTransactional()) {
			throw new IllegalArgumentException("Currently transactional caches are not supported.");
		}
		this.versionComparator = region.getCacheDataDescription().getVersionComparator();
		if (versionComparator == null) {
			throw new IllegalArgumentException("This strategy requires versioned entities/collections but region " + region.getName() + " contains non-versioned data!");
		}
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		if (txTimestamp < region.getLastRegionInvalidation() ) {
			return null;
		}
		Object value = cache.get(key);
		if (value instanceof VersionedEntry) {
			return ((VersionedEntry) value).getValue();
		}
		return value;
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version) {
		return putFromLoad(session, key, value, txTimestamp, version, false);
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride) throws CacheException {
		long lastRegionInvalidation = region.getLastRegionInvalidation();
		if (txTimestamp < lastRegionInvalidation) {
			log.tracef("putFromLoad not executed since tx started at %d, before last region invalidation finished = %d", txTimestamp, lastRegionInvalidation);
			return false;
		}
		assert version != null;

		if (minimalPutOverride) {
			Object prev = cache.get(key);
			if (prev != null) {
				Object oldVersion = getVersion(prev);
				if (oldVersion != null) {
					if (versionComparator.compare(version, oldVersion) <= 0) {
						if (trace) {
							log.tracef("putFromLoad not executed since version(%s) <= oldVersion(%s)", version, oldVersion);
						}
						return false;
					}
				}
				else if (prev instanceof VersionedEntry && txTimestamp <= ((VersionedEntry) prev).getTimestamp()) {
					if (trace) {
						log.tracef("putFromLoad not executed since tx started at %d and entry was invalidated at %d",
								txTimestamp, ((VersionedEntry) prev).getTimestamp());
					}
					return false;
				}
			}
		}
		// we can't use putForExternalRead since the PFER flag means that entry is not wrapped into context
		// when it is present in the container. TombstoneCallInterceptor will deal with this.
		if (!(value instanceof CacheEntry)) {
			value = new VersionedEntry(value, version, txTimestamp);
		}
		// Apply the update locally first - if we're the backup owner, async propagation wouldn't change the value
		// for the subsequent operation soon enough as it goes through primary owner
		putFromLoadCache.put(key, value);
		return true;
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return false;
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
		return false;
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		// there's no 'afterRemove', so we have to use our own synchronization
		// the API does not provide version of removed item but we can't load it from the cache
		// as that would be prone to race conditions - if the entry was updated in the meantime
		// the remove could be discarded and we would end up with stale record
		// See VersionedTest#testCollectionUpdate for such situation
		TransactionCoordinator transactionCoordinator = session.getTransactionCoordinator();
		RemovalSynchronization sync = new RemovalSynchronization(transactionCoordinator, writeCache, false, region, key);
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
		writeCache.put(key, new VersionedEntry(null, null, region.nextTimestamp()));
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
		writeCache.put(key, getVersioned(value, version, session.getTimestamp()));
		return true;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) {
		writeCache.put(key, getVersioned(value, currentVersion, session.getTimestamp()));
		return true;
	}

	protected Object getVersion(Object value) {
		if (value instanceof CacheEntry) {
			return ((CacheEntry) value).getVersion();
		}
		else if (value instanceof VersionedEntry) {
			return ((VersionedEntry) value).getVersion();
		}
		return null;
	}

	protected Object getVersioned(Object value, Object version, long timestamp) {
		assert value != null;
		assert version != null;
		return new VersionedEntry(value, version, timestamp);
	}
}
