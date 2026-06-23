/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class EntityTransactionalAccess extends AbstractEntityDataAccess {
	public EntityTransactionalAccess(
			@Nonnull DomainDataRegion region,
			@Nonnull CacheKeysFactory keysFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nonnull EntityDataCachingConfig accessConfig) {
		super( region, keysFactory, storageAccess );
	}

	@Override
	public boolean insert(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable Object version) {
		getStorageAccess().putIntoCache( key, value, session );
		return true;
	}

	@Override
	public boolean afterInsert(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable Object version) {
		return false;
	}

	@Override
	public boolean update(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable Object currentVersion,
			@Nullable Object previousVersion) {
		getStorageAccess().putIntoCache( key, value, session );
		return true;
	}

	@Override
	public boolean afterUpdate(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable Object currentVersion,
			@Nullable Object previousVersion,
			@Nullable SoftLock lock) {
		return false;
	}

	@Override
	@Nonnull
	public AccessType getAccessType() {
		return AccessType.TRANSACTIONAL;
	}
}
