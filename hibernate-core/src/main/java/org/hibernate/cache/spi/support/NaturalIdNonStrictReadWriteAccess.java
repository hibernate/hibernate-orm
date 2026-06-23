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

/**
 * Standard support for {@link org.hibernate.cache.spi.access.NaturalIdDataAccess}
 * using the {@link AccessType#NONSTRICT_READ_WRITE} access type.
 *
 * @author Steve Ebersole
 */
public class NaturalIdNonStrictReadWriteAccess extends AbstractNaturalIdDataAccess {
	public NaturalIdNonStrictReadWriteAccess(
			@Nonnull DomainDataRegion region,
			@Nonnull CacheKeysFactory keysFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nonnull NaturalIdDataCachingConfig config) {
		super( region, keysFactory, storageAccess, config );
	}

	@Override
	@Nonnull
	public AccessType getAccessType() {
		return AccessType.NONSTRICT_READ_WRITE;
	}

	@Override
	public void unlockItem(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nullable SoftLock lock) {
		getStorageAccess().removeFromCache( key, session );
	}

	@Override
	public boolean insert(
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
		getStorageAccess().removeFromCache( key, session );
		return false;
	}
}
