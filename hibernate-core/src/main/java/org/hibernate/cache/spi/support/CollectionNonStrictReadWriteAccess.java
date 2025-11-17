/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Standard support for {@link org.hibernate.cache.spi.access.CollectionDataAccess}
 * using the {@link AccessType#NONSTRICT_READ_WRITE} access type.
 *
 * @author Steve Ebersole
 */
public class CollectionNonStrictReadWriteAccess extends AbstractCollectionDataAccess {
	public CollectionNonStrictReadWriteAccess(
			DomainDataRegion region,
			CacheKeysFactory keysFactory,
			DomainDataStorageAccess storageAccess,
			CollectionDataCachingConfig config) {
		super( region, keysFactory, storageAccess, config );
	}

	@Override
	public AccessType getAccessType() {
		return AccessType.READ_WRITE;
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) {
		getStorageAccess().removeFromCache( key, session );
	}
}
