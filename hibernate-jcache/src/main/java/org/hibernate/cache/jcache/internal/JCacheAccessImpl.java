/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.jcache.internal;

import javax.cache.Cache;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * StorageAccess implementation wrapping a JCache {@link Cache} reference.
 *
 * @author Steve Ebersole
 */
public class JCacheAccessImpl implements DomainDataStorageAccess {
	private final Cache<Object,Object> underlyingCache;

	public JCacheAccessImpl(@Nonnull Cache<Object,Object> underlyingCache) {
		this.underlyingCache = underlyingCache;
	}

	@Nonnull
	public Cache<Object,Object> getUnderlyingCache() {
		return underlyingCache;
	}

	@Override
	public boolean contains(@Nonnull Object key) {
		return underlyingCache.containsKey( key );
	}

	@Override
	@Nullable
	public Object getFromCache(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		return underlyingCache.get( key );
	}

	@Override
	public void putIntoCache(
			@Nonnull Object key,
			@Nonnull Object value,
			@Nonnull SharedSessionContractImplementor session) {
		underlyingCache.put( key, value );
	}

	@Override
	public void removeFromCache(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		underlyingCache.remove( key );
	}

	@Override
	public void evictData(@Nonnull Object key) {
		underlyingCache.remove( key );
	}

	@Override
	public void clearCache(@Nonnull SharedSessionContractImplementor session) {
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
