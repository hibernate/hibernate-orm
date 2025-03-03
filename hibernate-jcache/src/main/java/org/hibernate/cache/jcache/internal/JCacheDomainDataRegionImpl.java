/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.jcache.internal;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;

import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;

/**
 * @author Vlad Mihalcea
 */
public class JCacheDomainDataRegionImpl extends DomainDataRegionImpl {

	public JCacheDomainDataRegionImpl(
			DomainDataRegionConfig regionConfig,
			RegionFactoryTemplate regionFactory,
			DomainDataStorageAccess domainDataStorageAccess,
			CacheKeysFactory defaultKeysFactory,
			DomainDataRegionBuildingContext buildingContext) {
		super( regionConfig, regionFactory, domainDataStorageAccess, defaultKeysFactory, buildingContext );
	}

	@Override
	protected EntityDataAccess generateTransactionalEntityDataAccess(EntityDataCachingConfig entityAccessConfig) {
		L2CACHE_LOGGER.nonStandardSupportForAccessType(
				getName(),
				AccessType.TRANSACTIONAL.getExternalName(),
				getRegionFactory().getClass().getSimpleName()
		);
		return super.generateTransactionalEntityDataAccess( entityAccessConfig );
	}

	@Override
	protected NaturalIdDataAccess generateTransactionalNaturalIdDataAccess(NaturalIdDataCachingConfig accessConfig) {
		L2CACHE_LOGGER.nonStandardSupportForAccessType(
				getName(),
				AccessType.TRANSACTIONAL.getExternalName(),
				getRegionFactory().getClass().getSimpleName()
		);
		return super.generateTransactionalNaturalIdDataAccess( accessConfig );
	}

	@Override
	protected CollectionDataAccess generateTransactionalCollectionDataAccess(CollectionDataCachingConfig accessConfig) {
		L2CACHE_LOGGER.nonStandardSupportForAccessType(
				getName(),
				AccessType.TRANSACTIONAL.getExternalName(),
				getRegionFactory().getClass().getSimpleName()
		);
		return super.generateTransactionalCollectionDataAccess( accessConfig );
	}
}
