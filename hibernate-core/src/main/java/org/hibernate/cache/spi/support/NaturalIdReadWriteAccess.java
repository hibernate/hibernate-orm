/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Comparator;

import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Standard support for {@link NaturalIdDataAccess}
 * using the {@link AccessType#READ_WRITE} access type.
 *
 * @author Steve Ebersole
 */
public class NaturalIdReadWriteAccess extends AbstractReadWriteAccess implements NaturalIdDataAccess {
	private final CacheKeysFactory keysFactory;

	public NaturalIdReadWriteAccess(
			@Nonnull DomainDataRegion region,
			@Nonnull CacheKeysFactory keysFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nonnull NaturalIdDataCachingConfig naturalIdDataCachingConfig) {
		super( region, storageAccess );
		this.keysFactory = keysFactory;
	}

	@Deprecated
	@Override
	@Nonnull
	protected AccessedDataClassification getAccessedDataClassification() {
		return AccessedDataClassification.NATURAL_ID;
	}

	@Override
	@Nonnull
	public AccessType getAccessType() {
		return AccessType.READ_WRITE;
	}

	@Override
	@Nullable
	protected Comparator<Object> getVersionComparator() {
		// natural id has no comparator
		return null;
	}

	@Override
	@Nonnull
	public Object generateCacheKey(
			@Nonnull Object naturalIdValues,
			@Nonnull EntityPersister rootEntityDescriptor,
			@Nonnull SharedSessionContractImplementor session) {
		return keysFactory.createNaturalIdKey( naturalIdValues, rootEntityDescriptor, session );
	}

	@Override
	@Nonnull
	public Object getNaturalIdValues(@Nonnull Object cacheKey) {
		return keysFactory.getNaturalIdValues( cacheKey );
	}

	private void put(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value) {
		getStorageAccess().putIntoCache( key, new Item( value, null, nextTimestamp() ), session );
	}

	@Override
	public boolean insert(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value) {
		return false;
	}

	@Override
	public boolean afterInsert(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value) {
		try {
			writeLock().lock();
			final var item = (Lockable) getStorageAccess().getFromCache( key, session );
			if ( item == null ) {
				put( session, key, value );
				return true;
			}
			else {
				return false;
			}
		}
		finally {
			writeLock().unlock();
		}
	}

	@Override
	public boolean update(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value) {
		return false;
	}

	@Override
	public boolean afterUpdate(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable SoftLock lock) {
		try {
			writeLock().lock();
			final var item = (Lockable) getStorageAccess().getFromCache( key, session );
			if ( item != null && item.isUnlockable( lock ) ) {
				final var lockItem = (SoftLockImpl) item;
				if ( lockItem.wasLockedConcurrently() ) {
					decrementLock( session, key, lockItem );
					return false;
				}
				else {
					put( session, key, value );
					return true;
				}
			}
			else {
				handleLockExpiry( session, key );
				return false;
			}
		}
		finally {
			writeLock().unlock();
		}
	}
}
