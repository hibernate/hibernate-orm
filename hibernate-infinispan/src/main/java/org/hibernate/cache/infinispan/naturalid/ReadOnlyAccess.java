package org.hibernate.cache.infinispan.naturalid;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
class ReadOnlyAccess extends TransactionalAccess {

	ReadOnlyAccess(NaturalIdRegionImpl naturalIdRegion) {
		super( naturalIdRegion );
	}


	@Override
	public boolean update(Object key, Object value) throws CacheException {
		throw new UnsupportedOperationException( "Illegal attempt to edit read only item" );
	}

	@Override
	public boolean afterUpdate(Object key, Object value, SoftLock lock) throws CacheException {
		throw new UnsupportedOperationException( "Illegal attempt to edit read only item" );
	}
}
