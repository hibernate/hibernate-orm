package org.hibernate.cache.impl.bridge;

import org.hibernate.cache.GeneralDataRegion;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Settings;

/**
 * {@inheritDoc}
*
* @author Steve Ebersole
*/
public abstract class BaseGeneralDataRegionAdapter extends BaseRegionAdapter implements GeneralDataRegion {

	protected BaseGeneralDataRegionAdapter(Cache underlyingCache, Settings settings) {
		super( underlyingCache, settings );
	}

	public Object get(Object key) throws CacheException {
		return underlyingCache.get( key );
	}

	public void put(Object key, Object value) throws CacheException {
		underlyingCache.put( key, value );
	}

	public void evict(Object key) throws CacheException {
		underlyingCache.remove( key );
	}

	public void evictAll() throws CacheException {
		underlyingCache.clear();
	}
}
