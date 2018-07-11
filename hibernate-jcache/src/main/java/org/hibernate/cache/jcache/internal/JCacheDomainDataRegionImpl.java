/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache.internal;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;

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
		SecondLevelCacheLogger.INSTANCE.nonStandardSupportForAccessType(
				getName(),
				AccessType.TRANSACTIONAL.getExternalName(),
				getRegionFactory().getClass().getSimpleName()
		);
		return super.generateTransactionalEntityDataAccess( entityAccessConfig );
	}

	@Override
	protected NaturalIdDataAccess generateTransactionalNaturalIdDataAccess(NaturalIdDataCachingConfig accessConfig) {
		SecondLevelCacheLogger.INSTANCE.nonStandardSupportForAccessType(
				getName(),
				AccessType.TRANSACTIONAL.getExternalName(),
				getRegionFactory().getClass().getSimpleName()
		);
		return super.generateTransactionalNaturalIdDataAccess( accessConfig );
	}

	@Override
	protected CollectionDataAccess generateTransactionalCollectionDataAccess(CollectionDataCachingConfig accessConfig) {
		SecondLevelCacheLogger.INSTANCE.nonStandardSupportForAccessType(
				getName(),
				AccessType.TRANSACTIONAL.getExternalName(),
				getRegionFactory().getClass().getSimpleName()
		);
		return super.generateTransactionalCollectionDataAccess( accessConfig );
	}
}
