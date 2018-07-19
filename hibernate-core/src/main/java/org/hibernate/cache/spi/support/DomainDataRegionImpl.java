/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

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
	@SuppressWarnings("WeakerAccess")
	public DomainDataRegionImpl(
			DomainDataRegionConfig regionConfig,
			RegionFactoryTemplate regionFactory,
			DomainDataStorageAccess domainDataStorageAccess,
			CacheKeysFactory defaultKeysFactory,
			DomainDataRegionBuildingContext buildingContext) {
		super(
				regionConfig,
				regionFactory,
				domainDataStorageAccess,
				defaultKeysFactory,
				buildingContext
		);
	}

	@Override
	protected EntityDataAccess generateTransactionalEntityDataAccess(EntityDataCachingConfig entityAccessConfig) {
		return new EntityTransactionalAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				entityAccessConfig
		);
	}

	@Override
	protected NaturalIdDataAccess generateTransactionalNaturalIdDataAccess(NaturalIdDataCachingConfig accessConfig) {
		return new NaturalIdTransactionalAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	@Override
	protected CollectionDataAccess generateTransactionalCollectionDataAccess(CollectionDataCachingConfig accessConfig) {
		return new CollectionTransactionAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}
}
