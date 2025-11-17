/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import java.util.Map;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;


import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDomainDataRegion extends AbstractRegion implements DomainDataRegion {

	private final SessionFactoryImplementor sessionFactory;
	private final CacheKeysFactory effectiveKeysFactory;

	private Map<NavigableRole,EntityDataAccess> entityDataAccessMap;
	private Map<NavigableRole,NaturalIdDataAccess> naturalIdDataAccessMap;
	private Map<NavigableRole,CollectionDataAccess> collectionDataAccessMap;

	public AbstractDomainDataRegion(
			DomainDataRegionConfig regionConfig,
			RegionFactory regionFactory,
			CacheKeysFactory defaultKeysFactory,
			DomainDataRegionBuildingContext buildingContext) {
//		super( regionFactory.qualify( regionConfig.getRegionName() ), regionFactory );
		super( regionConfig.getRegionName(), regionFactory );

		sessionFactory = buildingContext.getSessionFactory();

		if ( defaultKeysFactory == null ) {
			defaultKeysFactory = DefaultCacheKeysFactory.INSTANCE;
		}
		effectiveKeysFactory = buildingContext.getEnforcedCacheKeysFactory() != null
				? buildingContext.getEnforcedCacheKeysFactory()
				: defaultKeysFactory;
	}

	/**
	 * Should be called at the end of the subtype's constructor, or at least after the
	 * `#super(...)` (aka, this type's constructor) call.  It's a timing issue - we need access
	 * to the DomainDataStorageAccess in DomainDataRegionTemplate but in methods initiated
	 * (atm) from AbstractDomainDataRegion's constructor
	 */
	protected void completeInstantiation(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext) {
		L2CACHE_LOGGER.tracef( "DomainDataRegion created [%s]; key-factory = %s",
				regionConfig.getRegionName(), effectiveKeysFactory );

		entityDataAccessMap = generateEntityDataAccessMap( regionConfig );
		naturalIdDataAccessMap = generateNaturalIdDataAccessMap( regionConfig );
		collectionDataAccessMap = generateCollectionDataAccessMap( regionConfig );

	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public CacheKeysFactory getEffectiveKeysFactory() {
		return effectiveKeysFactory;
	}

	@Override
	public EntityDataAccess getEntityDataAccess(NavigableRole rootEntityRole) {
		final var access = entityDataAccessMap.get( rootEntityRole );
		if ( access == null ) {
			throw new IllegalArgumentException( "Caching was not configured for entity: " + rootEntityRole.getFullPath() );
		}
		return access;
	}


	@Override
	public NaturalIdDataAccess getNaturalIdDataAccess(NavigableRole rootEntityRole) {
		final var access = naturalIdDataAccessMap.get( rootEntityRole );
		if ( access == null ) {
			throw new IllegalArgumentException( "Caching was not configured for entity natural id: " + rootEntityRole.getFullPath() );
		}
		return access;
	}

	@Override
	public CollectionDataAccess getCollectionDataAccess(NavigableRole collectionRole) {
		final var access = collectionDataAccessMap.get( collectionRole );
		if ( access == null ) {
			throw new IllegalArgumentException( "Caching was not configured for collection: " + collectionRole.getFullPath() );
		}
		return access;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// creation

	protected abstract EntityDataAccess generateEntityAccess(EntityDataCachingConfig entityAccessConfig);
	protected abstract CollectionDataAccess generateCollectionAccess(CollectionDataCachingConfig cachingConfig);
	protected abstract NaturalIdDataAccess generateNaturalIdAccess(NaturalIdDataCachingConfig naturalIdAccessConfig);

	private Map<NavigableRole, EntityDataAccess> generateEntityDataAccessMap(
			DomainDataRegionConfig regionConfig) {
		final var entityCaching = regionConfig.getEntityCaching();
		if ( entityCaching.isEmpty() ) {
			return emptyMap();
		}

		final Map<NavigableRole, EntityDataAccess> accessMap = mapOfSize( entityCaching.size() );
		for ( var entityAccessConfig : entityCaching ) {
			accessMap.put( entityAccessConfig.getNavigableRole(),
					generateEntityAccess( entityAccessConfig ) );
		}
		return unmodifiableMap( accessMap );
	}

	private Map<NavigableRole, NaturalIdDataAccess> generateNaturalIdDataAccessMap(DomainDataRegionConfig regionConfig) {
		final var naturalIdCaching = regionConfig.getNaturalIdCaching();
		if ( naturalIdCaching.isEmpty() ) {
			return emptyMap();
		}

		final Map<NavigableRole, NaturalIdDataAccess> accessMap = mapOfSize( naturalIdCaching.size() );
		for ( var naturalIdAccessConfig : naturalIdCaching ) {
			accessMap.put( naturalIdAccessConfig.getNavigableRole(),
					generateNaturalIdAccess( naturalIdAccessConfig ) );
		}
		return unmodifiableMap( accessMap );
	}

	private Map<NavigableRole, CollectionDataAccess> generateCollectionDataAccessMap(
			DomainDataRegionConfig regionConfig) {
		final var collectionCaching = regionConfig.getCollectionCaching();
		if ( collectionCaching.isEmpty() ) {
			return emptyMap();
		}

		final Map<NavigableRole, CollectionDataAccess> accessMap = mapOfSize( collectionCaching.size() );
		for ( var cachingConfig : collectionCaching ) {
			accessMap.put( cachingConfig.getNavigableRole(),
					generateCollectionAccess( cachingConfig ) );
		}
		return unmodifiableMap( accessMap );
	}

	@Override
	public void clear() {
		for ( var cacheAccess : entityDataAccessMap.values() ) {
			cacheAccess.evictAll();
		}
		for ( var cacheAccess : naturalIdDataAccessMap.values() ) {
			cacheAccess.evictAll();
		}
		for ( var cacheAccess : collectionDataAccessMap.values() ) {
			cacheAccess.evictAll();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// destruction

	/**
	 * Optional interface caching implementors can implement in their
	 * CachedDomainDataAccess impls to automatically have them destroyed
	 * when this region is destroyed
	 */
	public interface Destructible {
		void destroy();
	}

	protected void releaseDataAccess(EntityDataAccess cacheAccess) {
		if ( cacheAccess instanceof Destructible destructible ) {
			destructible.destroy();
		}
	}

	protected void releaseDataAccess(NaturalIdDataAccess cacheAccess) {
		if ( cacheAccess instanceof Destructible destructible ) {
			destructible.destroy();
		}
	}

	protected void releaseDataAccess(CollectionDataAccess cacheAccess) {
		if ( cacheAccess instanceof Destructible destructible ) {
			destructible.destroy();
		}
	}

	@Override
	public void destroy() throws CacheException {
		for ( var cacheAccess : entityDataAccessMap.values() ) {
			releaseDataAccess( cacheAccess );
		}
		for ( var cacheAccess : naturalIdDataAccessMap.values() ) {
			releaseDataAccess( cacheAccess );
		}
		for ( var cacheAccess : collectionDataAccessMap.values() ) {
			releaseDataAccess( cacheAccess );
		}
	}

}
