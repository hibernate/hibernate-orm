/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;

/**
 * @author Steve Ebersole
 */
public class DomainDataRegionImpl extends DomainDataRegionTemplate {

	public DomainDataRegionImpl(
			@Nonnull DomainDataRegionConfig regionConfig,
			@Nonnull RegionFactoryTemplate regionFactory,
			@Nonnull DomainDataStorageAccess domainDataStorageAccess,
			@Nullable CacheKeysFactory defaultKeysFactory,
			@Nonnull DomainDataRegionBuildingContext buildingContext) {
		super(
				regionConfig,
				regionFactory,
				domainDataStorageAccess,
				defaultKeysFactory,
				buildingContext
		);
	}

	@Override
	@Nonnull
	protected EntityDataAccess generateTransactionalEntityDataAccess(@Nonnull EntityDataCachingConfig entityAccessConfig) {
		return new EntityTransactionalAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				entityAccessConfig
		);
	}

	@Override
	@Nonnull
	protected NaturalIdDataAccess generateTransactionalNaturalIdDataAccess(@Nonnull NaturalIdDataCachingConfig accessConfig) {
		return new NaturalIdTransactionalAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	@Override
	@Nonnull
	protected CollectionDataAccess generateTransactionalCollectionDataAccess(@Nonnull CollectionDataCachingConfig accessConfig) {
		return new CollectionTransactionAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}
}
