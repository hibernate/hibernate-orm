package org.hibernate.cache.infinispan.entity;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * A specialization of {@link TransactionalAccess} that ensures we never update data. Infinispan
 * access is always transactional.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
class ReadOnlyAccess extends TransactionalAccess {

	ReadOnlyAccess(EntityRegionImpl region) {
		super(region);
	}

	@Override
	public boolean update(Object key, Object value, Object currentVersion,
						  Object previousVersion) throws CacheException {
		throw new UnsupportedOperationException("Illegal attempt to edit read only item");
	}

	@Override
	public boolean afterUpdate(Object key, Object value, Object currentVersion,
							   Object previousVersion, SoftLock lock) throws CacheException {
		throw new UnsupportedOperationException("Illegal attempt to edit read only item");
	}

}