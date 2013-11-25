package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
class TransactionalEntityRegionAccessStrategy extends BaseEntityRegionAccessStrategy {
	TransactionalEntityRegionAccessStrategy(EntityRegionImpl region) {
		super( region );
	}

	@Override
	public boolean afterInsert(Object key, Object value, Object version) {
		return false;
	}

	@Override
	public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) {
		return false;
	}

	@Override
	public void remove(Object key) throws CacheException {
		evict( key );
	}

	@Override
	public boolean update(Object key, Object value, Object currentVersion,
						  Object previousVersion) throws CacheException {
		return insert( key, value, currentVersion );
	}
}
