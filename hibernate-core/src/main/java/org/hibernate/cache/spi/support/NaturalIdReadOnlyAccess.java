/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

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
			DomainDataRegion region,
			CacheKeysFactory keysFactory,
			DomainDataStorageAccess storageAccess,
			NaturalIdDataCachingConfig config) {
		super( region, keysFactory, storageAccess, config );
		if ( config.isMutable() ) {
			L2CACHE_LOGGER.readOnlyCachingMutableNaturalId( config.getNavigableRole().getFullPath() );
		}
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.READ_ONLY;
	}

	@Override
	public void unlockItem(
			SharedSessionContractImplementor session,
			Object key,
			SoftLock lock) {
		evict( key );
	}
}
