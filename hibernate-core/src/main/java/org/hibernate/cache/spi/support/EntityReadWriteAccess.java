/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import java.util.Comparator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Standard support for {@link EntityDataAccess}
 * using the {@link AccessType#READ_WRITE} access type.
 *
 * @author Steve Ebersole
 */
public class EntityReadWriteAccess extends AbstractReadWriteAccess implements EntityDataAccess {
	private final CacheKeysFactory keysFactory;
	private final Comparator<Object> versionComparator;

	public EntityReadWriteAccess(
			@Nonnull DomainDataRegion domainDataRegion,
			@Nonnull CacheKeysFactory keysFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nonnull EntityDataCachingConfig entityAccessConfig) {
		super( domainDataRegion, storageAccess );
		this.keysFactory = keysFactory;
		final var versionComparatorAccess =
				entityAccessConfig.getVersionComparatorAccess();
		this.versionComparator =
				versionComparatorAccess == null
						? null
						: versionComparatorAccess.get();
	}

	@Override
	@Nonnull
	public AccessType getAccessType() {
		return AccessType.READ_WRITE;
	}

	@Deprecated
	@Override
	@Nonnull
	protected AccessedDataClassification getAccessedDataClassification() {
		return AccessedDataClassification.ENTITY;
	}

	@Override
	@Nullable
	protected Comparator<Object> getVersionComparator() {
		return versionComparator;
	}

	@Override
	@Nonnull
	public Object generateCacheKey(
			@Nonnull Object id,
			@Nonnull EntityPersister rootEntityDescriptor,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier) {
		return keysFactory.createEntityKey( id, rootEntityDescriptor, factory, tenantIdentifier );
	}

	@Override
	@Nonnull
	public Object getCacheKeyId(@Nonnull Object cacheKey) {
		return keysFactory.getEntityId( cacheKey );
	}

	private void put(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable Object version) {
		getStorageAccess().putIntoCache( key, new Item( value, version, nextTimestamp() ), session );
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
		try {
			writeLock().lock();
			final var item = (Lockable) getStorageAccess().getFromCache( key, session );
			if ( item == null ) {
				put( session, key, value, version );
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
			@Nonnull Object value,
			@Nullable Object currentVersion,
			@Nullable Object previousVersion) {
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
		try {
			writeLock().lock();
			Lockable item = (Lockable) getStorageAccess().getFromCache( key, session );

			if ( item != null && item.isUnlockable( lock ) ) {
				final var lockItem = (SoftLockImpl) item;
				if ( lockItem.wasLockedConcurrently() ) {
					decrementLock( session, key, lockItem );
					return false;
				}
				else {
					put( session, key, value, currentVersion );
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

	@Override
	@Nullable
	public SoftLock lockRegion() {
		return null;
	}
}
