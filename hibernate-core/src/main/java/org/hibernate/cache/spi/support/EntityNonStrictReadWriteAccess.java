/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Standard support for {@link org.hibernate.cache.spi.access.EntityDataAccess}
 * using the {@link AccessType#NONSTRICT_READ_WRITE} access type.
 *
 * @author Steve Ebersole
 */
public class EntityNonStrictReadWriteAccess extends AbstractEntityDataAccess {
	public EntityNonStrictReadWriteAccess(
			@Nonnull DomainDataRegion domainDataRegion,
			@Nonnull CacheKeysFactory keysFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nonnull EntityDataCachingConfig entityAccessConfig) {
		super( domainDataRegion, keysFactory, storageAccess );
	}

	@Override
	@Nonnull
	public AccessType getAccessType() {
		return AccessType.NONSTRICT_READ_WRITE;
	}

	@Override
	public boolean insert(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable Object version) {
		return false;
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
		getStorageAccess().removeFromCache( key, session );
		return false;
	}

	@Override
	public boolean afterUpdate(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable Object currentVersion,
			@Nullable Object previousVersion,
			@Nullable SoftLock lock) {
		unlockItem( session, key, lock );
		return false;
	}

	/**
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public void unlockItem(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nullable SoftLock lock) throws CacheException {
		getStorageAccess().removeFromCache( key, session );
	}
}
