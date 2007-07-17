package org.hibernate.cache.impl.bridge;

import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * Adapter specifically bridging {@link EntityRegionAccessStrategy} to {@link CacheConcurrencyStrategy}.
 *
 * @author Steve Ebersole
 */
public class EntityAccessStrategyAdapter implements EntityRegionAccessStrategy {
	private final EntityRegion region;
	private final CacheConcurrencyStrategy ccs;
	private final Settings settings;

	public EntityAccessStrategyAdapter(EntityRegion region, CacheConcurrencyStrategy ccs, Settings settings) {
		this.region = region;
		this.ccs = ccs;
		this.settings = settings;
	}

	public EntityRegion getRegion() {
		return region;
	}

	public Object get(Object key, long txTimestamp) throws CacheException {
		return ccs.get( key, txTimestamp );
	}

	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
		return putFromLoad( key, value, txTimestamp, version, settings.isMinimalPutsEnabled() );
	}

	public boolean putFromLoad(
			Object key,
			Object value,
			long txTimestamp,
			Object version,
			boolean minimalPutOverride) throws CacheException {
		return ccs.put( key, value, txTimestamp, version, region.getCacheDataDescription().getVersionComparator(), minimalPutOverride );
	}

	public SoftLock lockItem(Object key, Object version) throws CacheException {
		return ccs.lock( key, version );
	}

	public SoftLock lockRegion() throws CacheException {
		// no-op; CCS did not have such a concept
		return null;
	}

	public void unlockItem(Object key, SoftLock lock) throws CacheException {
		ccs.release( key, lock );
	}

	public void unlockRegion(SoftLock lock) throws CacheException {
		// again, CCS did not have such a concept; but a reasonable
		// proximity is to clear the cache after transaction *as long as*
		// the underlying cache is not JTA aware.
		if ( !region.isTransactionAware() ) {
			ccs.clear();
		}
	}

	public boolean insert(Object key, Object value, Object version) throws CacheException {
		return ccs.insert( key, value, version );
	}

	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
		return ccs.afterInsert( key, value, version );
	}

	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		return ccs.update( key, value, currentVersion, previousVersion );
	}

	public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		return ccs.afterUpdate( key, value, currentVersion, lock );
	}

	public void remove(Object key) throws CacheException {
		ccs.evict( key );
	}

	public void removeAll() throws CacheException {
		// again, CCS did not have such a concept; however a reasonable
		// proximity is to clear the cache.  For non-transaction aware
		// caches, we will also do a clear at the end of the transaction
		ccs.clear();
	}

	public void evict(Object key) throws CacheException {
		ccs.remove( key );
	}

	public void evictAll() throws CacheException {
		ccs.clear();
	}

	public void destroy() {
		ccs.destroy();
	}
}
