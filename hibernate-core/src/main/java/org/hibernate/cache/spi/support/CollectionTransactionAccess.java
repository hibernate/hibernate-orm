/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;

/**
 * @author Steve Ebersole
 */
public class CollectionTransactionAccess extends AbstractCollectionDataAccess {
	public CollectionTransactionAccess(
			@Nonnull DomainDataRegion region,
			@Nonnull CacheKeysFactory keysFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nonnull CollectionDataCachingConfig config) {
		super( region, keysFactory, storageAccess, config );
	}

	@Override
	@Nonnull
	public AccessType getAccessType() {
		return AccessType.TRANSACTIONAL;
	}
}
