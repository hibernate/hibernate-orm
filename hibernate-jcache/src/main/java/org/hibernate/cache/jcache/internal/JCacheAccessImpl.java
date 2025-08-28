/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.jcache.internal;

import javax.cache.Cache;

import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * StorageAccess implementation wrapping a JCache {@link Cache} reference.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public class JCacheAccessImpl implements DomainDataStorageAccess {
	private final Cache underlyingCache;

	public JCacheAccessImpl(Cache underlyingCache) {
		this.underlyingCache = underlyingCache;
	}

	public Cache getUnderlyingCache() {
		return underlyingCache;
	}

	@Override
	public boolean contains(Object key) {
		return underlyingCache.containsKey( key );
	}

	@Override
	public Object getFromCache(Object key, SharedSessionContractImplementor session) {
		return underlyingCache.get( key );
	}

	@Override
	public void putIntoCache(Object key, Object value, SharedSessionContractImplementor session) {
		underlyingCache.put( key, value );
	}

	@Override
	public void removeFromCache(Object key, SharedSessionContractImplementor session) {
		underlyingCache.remove( key );
	}

	@Override
	public void evictData(Object key) {
		underlyingCache.remove( key );
	}

	@Override
	public void clearCache(SharedSessionContractImplementor session) {
		underlyingCache.clear();
	}

	@Override
	public void evictData() {
		underlyingCache.clear();
	}

	@Override
	public void release() {
		underlyingCache.close();
	}
}
