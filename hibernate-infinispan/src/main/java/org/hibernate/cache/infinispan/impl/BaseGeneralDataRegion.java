package org.hibernate.cache.infinispan.impl;

import org.infinispan.AdvancedCache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.spi.GeneralDataRegion;
import org.hibernate.cache.spi.RegionFactory;

/**
 * Support for Infinispan {@link GeneralDataRegion} implementors.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class BaseGeneralDataRegion extends BaseRegion implements GeneralDataRegion {
	private final AdvancedCache putCache;

	public BaseGeneralDataRegion(
			AdvancedCache cache, String name,
			RegionFactory factory) {
		super( cache, name, factory );
		this.putCache = Caches.ignoreReturnValuesCache( cache );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void evict(Object key) throws CacheException {
		cache.evict( key );
	}

	@Override
	public void evictAll() throws CacheException {
		cache.clear();
	}

	@Override
	public Object get(Object key) throws CacheException {
		return cache.get( key );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void put(Object key, Object value) throws CacheException {
		putCache.put( key, value );
	}

}