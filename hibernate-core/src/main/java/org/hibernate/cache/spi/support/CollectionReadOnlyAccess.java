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
 * Standard support for {@link org.hibernate.cache.spi.access.CollectionDataAccess}
 * using the {@link AccessType#READ_ONLY} access type.
 *
 * @author Steve Ebersole
 */
public class CollectionReadOnlyAccess extends AbstractCollectionDataAccess {
	public CollectionReadOnlyAccess(
			@Nonnull DomainDataRegion region,
			@Nonnull CacheKeysFactory keysFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nonnull CollectionDataCachingConfig config) {
		super( region, keysFactory, storageAccess, config );
	}

	@Override
	@Nonnull
	public AccessType getAccessType() {
		return AccessType.READ_ONLY;
	}
}
