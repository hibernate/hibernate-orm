/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;

/**
 * Standard support for {@link org.hibernate.cache.spi.access.NaturalIdDataAccess}
 * using the {@link AccessType#READ_ONLY} access type.
 *
 * @author Steve Ebersole
 */
public class NaturalIdReadOnlyAccess extends AbstractNaturalIdDataAccess {
	public NaturalIdReadOnlyAccess(
			@Nonnull DomainDataRegion region,
			@Nonnull CacheKeysFactory keysFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nonnull NaturalIdDataCachingConfig config) {
		super( region, keysFactory, storageAccess, config );
		if ( config.isMutable() ) {
			L2CACHE_LOGGER.readOnlyCachingMutableNaturalId( config.getNavigableRole().getFullPath() );
		}
	}

	@Override
	@Nonnull
	public AccessType getAccessType() {
		return AccessType.READ_ONLY;
	}

	@Override
	public void unlockItem(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nullable SoftLock lock) {
		evict( key );
	}
}
