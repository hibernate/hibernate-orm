package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
class TransactionalCollectionRegionAccessStrategy extends BaseCollectionRegionAccessStrategy {
	TransactionalCollectionRegionAccessStrategy(CollectionRegionImpl region) {
		super( region );
	}




	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(Object key) throws CacheException {
		evict( key );
	}


}
