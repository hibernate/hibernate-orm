/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntityDataAccess
		extends AbstractCachedDomainDataAccess
		implements EntityDataAccess {

	private final CacheKeysFactory cacheKeysFactory;

	public AbstractEntityDataAccess(
			@Nonnull DomainDataRegion region,
			@Nonnull CacheKeysFactory cacheKeysFactory,
			@Nonnull DomainDataStorageAccess storageAccess) {
		super( region, storageAccess );
		this.cacheKeysFactory = cacheKeysFactory;
	}

	@Override
	@Nonnull
	public Object generateCacheKey(
			@Nonnull Object id,
			@Nonnull EntityPersister rootEntityDescriptor,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier) {
		return cacheKeysFactory.createEntityKey(
				id,
				rootEntityDescriptor,
				factory,
				tenantIdentifier
		);
	}

	@Override
	@Nonnull
	public Object getCacheKeyId(@Nonnull Object cacheKey) {
		return cacheKeysFactory.getEntityId( cacheKey );
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
}
