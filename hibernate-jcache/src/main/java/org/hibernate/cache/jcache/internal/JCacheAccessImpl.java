/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache.internal;

import javax.cache.Cache;

import org.hibernate.cache.spi.support.StorageAccess;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public class JCacheAccessImpl implements StorageAccess {
	// todo (5.3) : consider RegionFactory support in hibernate-core based on StorageAccess
	//		plus the DomainDataStorageAccess defined here

	private final Cache underlyingCache;

	public JCacheAccessImpl(Cache underlyingCache) {
		this.underlyingCache = underlyingCache;
	}

	public Cache getUnderlyingCache() {
		return underlyingCache;
	}

	@Override
	public Object getFromCache(Object key) {
		return underlyingCache.get( key );
	}

	@Override
	public void putIntoCache(Object key, Object value) {
		underlyingCache.put( key, value );
	}

	@Override
	public void removeFromCache(Object key) {
		underlyingCache.remove( key );
	}

	@Override
	public void clearCache() {
		underlyingCache.clear();
	}

	@Override
	public void release() {
		underlyingCache.close();
	}
}
