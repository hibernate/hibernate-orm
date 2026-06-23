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
			@Nonnull DomainDataRegionConfig regionConfig,
			@Nonnull RegionFactory regionFactory,
			@Nonnull DomainDataStorageAccess storageAccess,
			@Nullable CacheKeysFactory defaultKeysFactory,
			@Nonnull DomainDataRegionBuildingContext buildingContext) {
		super( regionConfig, regionFactory, defaultKeysFactory, buildingContext );
		this.storageAccess = storageAccess;
		// now the super-type calls will have access to the `DomainDataStorageAccess` reference
		completeInstantiation( regionConfig, buildingContext );
	}

	/**
	 * Public for testing purposes
	 */
	@Nonnull
	public DomainDataStorageAccess getCacheStorageAccess() {
		return storageAccess;
	}


	@Override
	@Nonnull
	public EntityDataAccess generateEntityAccess(@Nonnull EntityDataCachingConfig entityAccessConfig) {
		final var accessType = entityAccessConfig.getAccessType();
		if ( L2CACHE_LOGGER.isTraceEnabled() ) {
			L2CACHE_LOGGER.generatingEntityAccess(
					entityAccessConfig.getNavigableRole().toString(),
					accessType.getExternalName()
			);
		}
		return switch ( accessType ) {
			case READ_ONLY -> generateReadOnlyEntityAccess( entityAccessConfig );
			case READ_WRITE -> generateReadWriteEntityAccess( entityAccessConfig );
			case NONSTRICT_READ_WRITE -> generateNonStrictReadWriteEntityAccess( entityAccessConfig );
			case TRANSACTIONAL -> generateTransactionalEntityDataAccess( entityAccessConfig );
		};
	}

	@Nonnull
	protected EntityDataAccess generateReadOnlyEntityAccess(@Nonnull EntityDataCachingConfig accessConfig) {
		return new EntityReadOnlyAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	@Nonnull
	protected EntityDataAccess generateReadWriteEntityAccess(@Nonnull EntityDataCachingConfig accessConfig) {
		return new EntityReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	@Nonnull
	protected EntityDataAccess generateNonStrictReadWriteEntityAccess(@Nonnull EntityDataCachingConfig accessConfig) {
		return new EntityNonStrictReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	@Nonnull
	protected EntityDataAccess generateTransactionalEntityDataAccess(@Nonnull EntityDataCachingConfig entityAccessConfig) {
		throw generateTransactionalNotSupportedException();
	}

	private UnsupportedOperationException generateTransactionalNotSupportedException() {
		return new UnsupportedOperationException( "Cache provider [" + getRegionFactory() + "] does not support `"
													+ AccessType.TRANSACTIONAL.getExternalName() + "` access" );
	}

	@Override
	@Nonnull
	public NaturalIdDataAccess generateNaturalIdAccess(@Nonnull NaturalIdDataCachingConfig accessConfig) {
		final var accessType = accessConfig.getAccessType();
		if ( L2CACHE_LOGGER.isTraceEnabled() ) {
			L2CACHE_LOGGER.generatingNaturalIdAccess(
					accessConfig.getNavigableRole().toString(),
					accessType.getExternalName()
			);
		}
		return switch ( accessType ) {
			case READ_ONLY -> generateReadOnlyNaturalIdAccess( accessConfig );
			case READ_WRITE -> generateReadWriteNaturalIdAccess( accessConfig );
			case NONSTRICT_READ_WRITE -> generateNonStrictReadWriteNaturalIdAccess( accessConfig );
			case TRANSACTIONAL -> generateTransactionalNaturalIdDataAccess( accessConfig );
		};
	}

	@Nonnull
	protected NaturalIdDataAccess generateReadOnlyNaturalIdAccess(@Nonnull NaturalIdDataCachingConfig accessConfig) {
		return new NaturalIdReadOnlyAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	@Nonnull
	protected NaturalIdDataAccess generateReadWriteNaturalIdAccess(@Nonnull NaturalIdDataCachingConfig accessConfig) {
		return new NaturalIdReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	@Nonnull
	protected NaturalIdDataAccess generateNonStrictReadWriteNaturalIdAccess(@Nonnull NaturalIdDataCachingConfig accessConfig) {
		return new NaturalIdNonStrictReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	@Nonnull
	protected NaturalIdDataAccess generateTransactionalNaturalIdDataAccess(@Nonnull NaturalIdDataCachingConfig accessConfig) {
		throw generateTransactionalNotSupportedException();
	}

	@Override
	@Nonnull
	public CollectionDataAccess generateCollectionAccess(@Nonnull CollectionDataCachingConfig accessConfig) {
		final var accessType = accessConfig.getAccessType();
		if ( L2CACHE_LOGGER.isTraceEnabled() ) {
			L2CACHE_LOGGER.generatingCollectionAccess(
					accessConfig.getNavigableRole().toString(),
					accessType.getExternalName()
			);
		}
		return switch ( accessType ) {
			case READ_ONLY -> generateReadOnlyCollectionAccess( accessConfig );
			case READ_WRITE -> generateReadWriteCollectionAccess( accessConfig );
			case NONSTRICT_READ_WRITE -> generateNonStrictReadWriteCollectionAccess( accessConfig );
			case TRANSACTIONAL -> generateTransactionalCollectionDataAccess( accessConfig );
		};
	}

	@Nonnull
	private CollectionDataAccess generateReadOnlyCollectionAccess(@Nonnull CollectionDataCachingConfig accessConfig) {
		return new CollectionReadOnlyAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	@Nonnull
	private CollectionDataAccess generateReadWriteCollectionAccess(@Nonnull CollectionDataCachingConfig accessConfig) {
		return new CollectionReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	@Nonnull
	private CollectionDataAccess generateNonStrictReadWriteCollectionAccess(@Nonnull CollectionDataCachingConfig accessConfig) {
		return new CollectionNonStrictReadWriteAccess(
				this,
				getEffectiveKeysFactory(),
				getCacheStorageAccess(),
				accessConfig
		);
	}

	@Nonnull
	protected CollectionDataAccess generateTransactionalCollectionDataAccess(@Nonnull CollectionDataCachingConfig accessConfig) {
		throw generateTransactionalNotSupportedException();
	}
}
