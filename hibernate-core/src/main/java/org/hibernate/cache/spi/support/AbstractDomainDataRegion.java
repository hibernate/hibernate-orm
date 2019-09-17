/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDomainDataRegion extends AbstractRegion implements DomainDataRegion {
	private static final Logger log = Logger.getLogger( AbstractDomainDataRegion.class );

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

		this.sessionFactory = buildingContext.getSessionFactory();

		if ( defaultKeysFactory == null ) {
			defaultKeysFactory = DefaultCacheKeysFactory.INSTANCE;
		}
		this.effectiveKeysFactory = buildingContext.getEnforcedCacheKeysFactory() != null
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
		log.tracef( "DomainDataRegion created [%s]; key-factory = %s", regionConfig.getRegionName(), effectiveKeysFactory );

		this.entityDataAccessMap = generateEntityDataAccessMap( regionConfig );
		this.naturalIdDataAccessMap = generateNaturalIdDataAccessMap( regionConfig );
		this.collectionDataAccessMap = generateCollectionDataAccessMap( regionConfig );

	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public CacheKeysFactory getEffectiveKeysFactory() {
		return effectiveKeysFactory;
	}

	@Override
	public EntityDataAccess getEntityDataAccess(NavigableRole rootEntityRole) {
		final EntityDataAccess access = entityDataAccessMap.get( rootEntityRole );
		if ( access == null ) {
			throw new IllegalArgumentException( "Caching was not configured for entity : " + rootEntityRole.getFullPath() );
		}
		return access;
	}


	@Override
	public NaturalIdDataAccess getNaturalIdDataAccess(NavigableRole rootEntityRole) {
		final NaturalIdDataAccess access = naturalIdDataAccessMap.get( rootEntityRole );
		if ( access == null ) {
			throw new IllegalArgumentException( "Caching was not configured for entity natural-id : " + rootEntityRole.getFullPath() );
		}
		return access;
	}

	@Override
	public CollectionDataAccess getCollectionDataAccess(NavigableRole collectionRole) {
		final CollectionDataAccess access = collectionDataAccessMap.get( collectionRole );
		if ( access == null ) {
			throw new IllegalArgumentException( "Caching was not configured for collection : " + collectionRole.getFullPath() );
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
		final List<EntityDataCachingConfig> entityCaching = regionConfig.getEntityCaching();
		if ( entityCaching.isEmpty() ) {
			return Collections.emptyMap();
		}

		final Map<NavigableRole, EntityDataAccess> accessMap = new HashMap<>( entityCaching.size() );
		for ( EntityDataCachingConfig entityAccessConfig : entityCaching ) {
			accessMap.put(
					entityAccessConfig.getNavigableRole(),
					generateEntityAccess( entityAccessConfig )
			);
		}

		return Collections.unmodifiableMap( accessMap );
	}

	private Map<NavigableRole, NaturalIdDataAccess> generateNaturalIdDataAccessMap(DomainDataRegionConfig regionConfig) {
		final List<NaturalIdDataCachingConfig> naturalIdCaching = regionConfig.getNaturalIdCaching();
		if ( naturalIdCaching.isEmpty() ) {
			return Collections.emptyMap();
		}

		final Map<NavigableRole, NaturalIdDataAccess> accessMap = new HashMap<>( naturalIdCaching.size() );
		for ( NaturalIdDataCachingConfig naturalIdAccessConfig : naturalIdCaching ) {
			accessMap.put(
					naturalIdAccessConfig.getNavigableRole(),
					generateNaturalIdAccess( naturalIdAccessConfig )
			);
		}

		return Collections.unmodifiableMap( accessMap );
	}

	private Map<NavigableRole, CollectionDataAccess> generateCollectionDataAccessMap(
			DomainDataRegionConfig regionConfig) {
		final List<CollectionDataCachingConfig> collectionCaching = regionConfig.getCollectionCaching();
		if ( collectionCaching.isEmpty() ) {
			return Collections.emptyMap();
		}

		final Map<NavigableRole, CollectionDataAccess> accessMap = new HashMap<>( collectionCaching.size() );
		for ( CollectionDataCachingConfig cachingConfig : collectionCaching ) {
			accessMap.put(
					cachingConfig.getNavigableRole(),
					generateCollectionAccess( cachingConfig )
			);
		}

		return Collections.unmodifiableMap( accessMap );
	}

	@Override
	public void clear() {
		for ( EntityDataAccess cacheAccess : entityDataAccessMap.values() ) {
			cacheAccess.evictAll();
		}

		for ( NaturalIdDataAccess cacheAccess : naturalIdDataAccessMap.values() ) {
			cacheAccess.evictAll();
		}

		for ( CollectionDataAccess cacheAccess : collectionDataAccessMap.values() ) {
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
		if ( Destructible.class.isInstance( cacheAccess ) ) {
			( (Destructible) cacheAccess ).destroy();
		}
	}

	protected void releaseDataAccess(NaturalIdDataAccess cacheAccess) {
		if ( Destructible.class.isInstance( cacheAccess ) ) {
			( (Destructible) cacheAccess ).destroy();
		}
	}

	protected void releaseDataAccess(CollectionDataAccess cacheAccess) {
		if ( Destructible.class.isInstance( cacheAccess ) ) {
			( (Destructible) cacheAccess ).destroy();
		}
	}

	@Override
	public void destroy() throws CacheException {
		for ( EntityDataAccess cacheAccess : entityDataAccessMap.values() ) {
			releaseDataAccess( cacheAccess );
		}

		for ( NaturalIdDataAccess cacheAccess : naturalIdDataAccessMap.values() ) {
			releaseDataAccess( cacheAccess );
		}

		for ( CollectionDataAccess cacheAccess : collectionDataAccessMap.values() ) {
			releaseDataAccess( cacheAccess );
		}
	}

}
