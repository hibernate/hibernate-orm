/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import java.util.Comparator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
/**
 * Standard support for {@link CollectionDataAccess}
 * using the {@link AccessType#READ_WRITE} access type.
 *
 * @author Chris Cranford
 * @author Steve Ebersole
 */
public class CollectionReadWriteAccess extends AbstractReadWriteAccess implements CollectionDataAccess {
	private final Comparator<Object> versionComparator;
	private final CacheKeysFactory keysFactory;

	public CollectionReadWriteAccess(
			@Nonnull DomainDataRegion region,
			@Nonnull CacheKeysFactory keysFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nonnull CollectionDataCachingConfig config) {
		super( region, storageAccess );
		this.keysFactory = keysFactory;
		this.versionComparator = config.getOwnerVersionComparator();
	}

	@Deprecated
	@Override
	@Nonnull
	protected AccessedDataClassification getAccessedDataClassification() {
		return AccessedDataClassification.COLLECTION;
	}

	@Override
	@Nonnull
	public AccessType getAccessType() {
		return AccessType.READ_WRITE;
	}

	@Override
	@Nonnull
	public Object generateCacheKey(
			@Nonnull Object id,
			@Nonnull CollectionPersister collectionDescriptor,
			@Nonnull SessionFactoryImplementor factory,
			@Nullable String tenantIdentifier) {
		return keysFactory.createCollectionKey( id, collectionDescriptor, factory, tenantIdentifier );
	}

	@Override
	@Nonnull
	public Object getCacheKeyId(@Nonnull Object cacheKey) {
		return keysFactory.getCollectionId( cacheKey );
	}

	@Override
	@Nullable
	protected Comparator<Object> getVersionComparator() {
		return versionComparator;
	}

	@Override
	@Nullable
	public Object get(@Nonnull SharedSessionContractImplementor session, @Nonnull Object key) {
		return super.get( session, key );
	}

	@Override
	public boolean putFromLoad(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nonnull Object value,
			@Nullable Object version) {
		return super.putFromLoad( session, key, value, version );
	}

	@Override
	@Nonnull
	public SoftLock lockItem(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nullable Object version) {
		return super.lockItem( session, key, version );
	}

	@Override
	public void unlockItem(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull Object key,
			@Nullable SoftLock lock) {
		super.unlockItem( session, key, lock );
	}
}
