/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionDataAccess
		extends AbstractCachedDomainDataAccess
		implements CollectionDataAccess {

	private final CacheKeysFactory keysFactory;

	public AbstractCollectionDataAccess(
			@Nonnull DomainDataRegion region,
			@Nonnull CacheKeysFactory keysFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nonnull CollectionDataCachingConfig config) {
		super( region, storageAccess );
		this.keysFactory = keysFactory;
	}

	@Override
	@Nonnull
	public Object generateCacheKey(
			@Nonnull Object id,
			@Nonnull CollectionPersister persister,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier) {
		return keysFactory.createCollectionKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	@Nonnull
	public Object getCacheKeyId(@Nonnull Object cacheKey) {
		return keysFactory.getCollectionId( cacheKey );
	}

	@Override
	@Nullable
	public SoftLock lockItem(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nullable Object version) {
		return null;
	}

	@Override
	public void unlockItem(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nullable SoftLock lock) {

	}

	@Override
	@Nullable
	public SoftLock lockRegion() {
		return null;
	}

	@Override
	public void unlockRegion(@Nullable SoftLock lock) {
		clearCache();
	}
}
