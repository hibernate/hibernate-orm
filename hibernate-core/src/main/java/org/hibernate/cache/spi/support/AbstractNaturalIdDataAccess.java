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
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNaturalIdDataAccess extends AbstractCachedDomainDataAccess implements NaturalIdDataAccess {
	private final CacheKeysFactory keysFactory;

	public AbstractNaturalIdDataAccess(
			@Nonnull DomainDataRegion region,
			@Nonnull CacheKeysFactory keysFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nonnull NaturalIdDataCachingConfig config) {
		super( region, storageAccess );
		this.keysFactory = keysFactory;
	}

	@Override
	@Nonnull
	public Object generateCacheKey(
			@Nonnull Object naturalIdValues,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor session) {
		return keysFactory.createNaturalIdKey( naturalIdValues, persister, session );
	}

	@Override
	@Nonnull
	public Object getNaturalIdValues(@Nonnull Object cacheKey) {
		return keysFactory.getNaturalIdValues( cacheKey );
	}


	@Override
	public boolean insert(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value) {
		getStorageAccess().putIntoCache( key, value, session );
		return true;
	}

	@Override
	public boolean afterInsert(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value) {
		return false;
	}

	@Override
	public boolean update(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value) {
		getStorageAccess().putIntoCache( key, value, session );
		return true;
	}

	@Override
	public boolean afterUpdate(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable SoftLock lock) {
		return false;
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
