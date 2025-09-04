/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;


import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;

/**
 * Abstract implementation of {@link  org.hibernate.cache.spi.DomainDataRegion} based
 * on implementations just needing to provide a {@link DomainDataStorageAccess} reference
 * for basic caching support - DomainDataStorageAccess acts as a simple wrapper around
 * some generalized cache actions such as put or get.  Most implementations (our own
 * JCache-based one included) can likely be as simple as:
 *
 * 		* Custom DomainDataStorageAccess implementation, bridging calls back
 * 			to the specific cache provider's APIs
 * 		* Custom DomainDataRegionTemplate implementation that creates its custom
 * 			DomainDataStorageAccess reference
 * 		* Custom RegionFactory implementation that creates its custom DomainDataRegionTemplate
 *
 * todo (5.3) : move this javadoc into DomainDataRegion and/or package javadoc
 *
 * @author Steve Ebersole
 */
public class DomainDataRegionTemplate extends AbstractDomainDataRegion {

	private final DomainDataStorageAccess storageAccess;

	public DomainDataRegionTemplate(
			DomainDataRegionConfig regionConfig,
			RegionFactory regionFactory,
			DomainDataStorageAccess storageAccess,
			CacheKeysFactory defaultKeysFactory,
			DomainDataRegionBuildingContext buildingContext) {
		super( regionConfig, regionFactory, defaultKeysFactory, buildingContext );
		this.storageAccess = storageAccess;

		// now the super-type calls will have access to the `DomainDataStorageAccess` reference
		completeInstantiation( regionConfig, buildingContext );
	}

	/**
	 * Public for testing purposes
	 */
	public DomainDataStorageAccess getCacheStorageAccess() {
		return storageAccess;
	}


	@Override
	public EntityDataAccess generateEntityAccess(EntityDataCachingConfig entityAccessConfig) {
		final var namedEntityRole = entityAccessConfig.getNavigableRole();
		final var accessType = entityAccessConfig.getAccessType();

		L2CACHE_LOGGER.tracef( "Generating entity cache access [%s] : %s",
				accessType.getExternalName(), namedEntityRole );

		return switch ( accessType ) {
			case READ_ONLY -> generateReadOnlyEntityAccess( entityAccessConfig );
			case READ_WRITE -> generateReadWriteEntityAccess( entityAccessConfig );
			case NONSTRICT_READ_WRITE -> generateNonStrictReadWriteEntityAccess( entityAccessConfig );
			case TRANSACTIONAL -> generateTransactionalEntityDataAccess( entityAccessConfig );
		};
	}

	protected EntityDataAccess generateReadOnlyEntityAccess(EntityDataCachingConfig accessConfig) {
		return new EntityReadOnlyAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	protected EntityDataAccess generateReadWriteEntityAccess(EntityDataCachingConfig accessConfig) {
		return new EntityReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	protected EntityDataAccess generateNonStrictReadWriteEntityAccess(EntityDataCachingConfig accessConfig) {
		return new EntityNonStrictReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	protected EntityDataAccess generateTransactionalEntityDataAccess(EntityDataCachingConfig entityAccessConfig) {
		throw generateTransactionalNotSupportedException();
	}

	private UnsupportedOperationException generateTransactionalNotSupportedException() {
		return new UnsupportedOperationException( "Cache provider [" + getRegionFactory() + "] does not support `"
													+ AccessType.TRANSACTIONAL.getExternalName() + "` access" );
	}

	@Override
	public NaturalIdDataAccess generateNaturalIdAccess(NaturalIdDataCachingConfig accessConfig) {
		final var namedEntityRole = accessConfig.getNavigableRole();
		final var accessType = accessConfig.getAccessType();

		L2CACHE_LOGGER.tracef( "Generating entity natural-id access [%s] : %s",
				accessType.getExternalName(), namedEntityRole );

		return switch ( accessType ) {
			case READ_ONLY -> generateReadOnlyNaturalIdAccess( accessConfig );
			case READ_WRITE -> generateReadWriteNaturalIdAccess( accessConfig );
			case NONSTRICT_READ_WRITE -> generateNonStrictReadWriteNaturalIdAccess( accessConfig );
			case TRANSACTIONAL -> generateTransactionalNaturalIdDataAccess( accessConfig );
		};
	}

	protected NaturalIdDataAccess generateReadOnlyNaturalIdAccess(NaturalIdDataCachingConfig accessConfig) {
		return new NaturalIdReadOnlyAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	protected NaturalIdDataAccess generateReadWriteNaturalIdAccess(NaturalIdDataCachingConfig accessConfig) {
		return new NaturalIdReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	protected NaturalIdDataAccess generateNonStrictReadWriteNaturalIdAccess(NaturalIdDataCachingConfig accessConfig) {
		return new NaturalIdNonStrictReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	protected NaturalIdDataAccess generateTransactionalNaturalIdDataAccess(NaturalIdDataCachingConfig accessConfig) {
		throw generateTransactionalNotSupportedException();
	}

	@Override
	public CollectionDataAccess generateCollectionAccess(CollectionDataCachingConfig accessConfig) {
		final var namedCollectionRole = accessConfig.getNavigableRole();

		if ( L2CACHE_LOGGER.isTraceEnabled() ) {
			L2CACHE_LOGGER.trace( "Generating collection cache access: " + namedCollectionRole );
		}

		return switch ( accessConfig.getAccessType() ) {
			case READ_ONLY -> generateReadOnlyCollectionAccess( accessConfig );
			case READ_WRITE -> generateReadWriteCollectionAccess( accessConfig );
			case NONSTRICT_READ_WRITE -> generateNonStrictReadWriteCollectionAccess( accessConfig );
			case TRANSACTIONAL -> generateTransactionalCollectionDataAccess( accessConfig );
		};
	}

	private CollectionDataAccess generateReadOnlyCollectionAccess(CollectionDataCachingConfig accessConfig) {
		return new CollectionReadOnlyAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	private CollectionDataAccess generateReadWriteCollectionAccess(CollectionDataCachingConfig accessConfig) {
		return new CollectionReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	private CollectionDataAccess generateNonStrictReadWriteCollectionAccess(CollectionDataCachingConfig accessConfig) {
		return new CollectionNonStrictReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	protected CollectionDataAccess generateTransactionalCollectionDataAccess(CollectionDataCachingConfig accessConfig) {
		throw generateTransactionalNotSupportedException();
	}
}
