/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;


import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCachedDomainDataAccess implements CachedDomainDataAccess, AbstractDomainDataRegion.Destructible {

	private final DomainDataRegion region;
	private final DomainDataStorageAccess storageAccess;

	protected AbstractCachedDomainDataAccess(
			@Nonnull DomainDataRegion region,
			@Nonnull DomainDataStorageAccess storageAccess) {
		this.region = region;
		this.storageAccess = storageAccess;
	}

	@Override
	@Nonnull
	public DomainDataRegion getRegion() {
		return region;
	}

	@Internal
	@Nonnull
	public DomainDataStorageAccess getStorageAccess() {
		return storageAccess;
	}

	protected void clearCache() {
		L2CACHE_LOGGER.clearingCacheDataMap( region.getName() );
		getStorageAccess().evictData();
	}

	@Override
	public boolean contains(@Nonnull Object key) {
		return getStorageAccess().contains( key );
	}

	@Override
	@Nullable
	public Object get(@Nonnull SharedSessionContractImplementor session, @Nonnull Object key) {
		final boolean traceEnabled = L2CACHE_LOGGER.isTraceEnabled();
		if ( traceEnabled ) {
			L2CACHE_LOGGER.gettingCachedData( region.getName(), getAccessType(), key );
		}
		final Object item = getStorageAccess().getFromCache( key, session );
		if ( traceEnabled ) {
			if ( item == null ) {
				L2CACHE_LOGGER.cacheMiss( region.getName(), key );
			}
			else {
				L2CACHE_LOGGER.cacheHit( region.getName(), key );
			}
		}
		return item;
	}

	@Override
	public boolean putFromLoad(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable Object version) {
		if ( L2CACHE_LOGGER.isTraceEnabled() ) {
			L2CACHE_LOGGER.cachingDataFromLoad( region.getName(), getAccessType(), key, value );
		}
		getStorageAccess().putFromLoad( key, value, session );
		return true;
	}

	@Override
	public boolean putFromLoad(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable Object version,
			boolean minimalPutOverride) {
		if ( minimalPutOverride && getStorageAccess().contains( key ) ) {
			if ( L2CACHE_LOGGER.isTraceEnabled() ) {
				L2CACHE_LOGGER.cachePutFromLoadSkippedDueToMinimalPut( region.getName(), getAccessType(), key );
			}
			return false;
		}
		else {
			return putFromLoad( session, key, value, version );
		}
	}

	private static final SoftLock REGION_LOCK = new SoftLock() {
	};

	@Override
	@Nonnull
	public SoftLock lockRegion() {
		return REGION_LOCK;
	}

	@Override
	public void unlockRegion(@Nullable SoftLock lock) {
		evictAll();
	}

	@Override
	public void remove(@Nonnull SharedSessionContractImplementor session, @Nonnull Object key) {
		getStorageAccess().removeFromCache( key, session );
	}

	@Override
	public void removeAll(@Nonnull SharedSessionContractImplementor session) {
		getStorageAccess().clearCache( session );
	}

	@Override
	public void evict(@Nonnull Object key) {
		getStorageAccess().evictData( key );
	}

	@Override
	public void evictAll() {
		getStorageAccess().evictData();
	}

	@Override
	public void destroy() {
		getStorageAccess().release();
	}
}
