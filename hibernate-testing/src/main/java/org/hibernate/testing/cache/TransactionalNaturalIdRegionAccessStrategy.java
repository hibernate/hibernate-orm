package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;

/**
 * @author Eric Dalquist
 */
class TransactionalNaturalIdRegionAccessStrategy extends BaseNaturalIdRegionAccessStrategy {
	TransactionalNaturalIdRegionAccessStrategy(NaturalIdRegionImpl region) {
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
