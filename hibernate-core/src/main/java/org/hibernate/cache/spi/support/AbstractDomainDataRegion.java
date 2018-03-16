/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDomainDataRegion extends AbstractRegion implements DomainDataRegion {
	private final SessionFactoryImplementor sessionFactory;

	private final Map<NavigableRole,EntityDataAccess> entityDataAccessMap;
	private final Map<NavigableRole,NaturalIdDataAccess> naturalIdDataAccessMap;
	private final Map<NavigableRole,CollectionDataAccess> collectionDataAccessMap;

	public AbstractDomainDataRegion(
			DomainDataRegionConfig regionConfig,
			RegionFactory regionFactory,
			DomainDataStorageAccess storageAccess,
			DomainDataRegionBuildingContext buildingContext) {
		super( regionFactory.qualify( regionConfig.getRegionName() ), regionFactory, storageAccess );
		this.sessionFactory = buildingContext.getSessionFactory();

		this.entityDataAccessMap = generateEntityDataAccessMap( regionConfig );
		this.naturalIdDataAccessMap = generateNaturalIdDataAccessMap( regionConfig );
		this.collectionDataAccessMap = generateCollectionDataAccessMap( regionConfig );
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public DomainDataStorageAccess getStorageAccess() {
		return (DomainDataStorageAccess) super.getStorageAccess();
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
		if ( regionConfig.getEntityCaching().isEmpty() ) {
			return Collections.emptyMap();
		}

		final Map<NavigableRole, EntityDataAccess> accessMap = new ConcurrentHashMap<>();
		for ( EntityDataCachingConfig entityAccessConfig : regionConfig.getEntityCaching() ) {
			accessMap.computeIfAbsent(
					entityAccessConfig.getNavigableRole(),
					hierarchy -> generateEntityAccess( entityAccessConfig )
			);
		}

		return Collections.unmodifiableMap( accessMap );
	}

	private Map<NavigableRole, NaturalIdDataAccess> generateNaturalIdDataAccessMap(DomainDataRegionConfig regionConfig) {
		if ( regionConfig.getNaturalIdCaching().isEmpty() ) {
			return Collections.emptyMap();
		}

		final Map<NavigableRole, NaturalIdDataAccess> accessMap = new ConcurrentHashMap<>();
		for ( NaturalIdDataCachingConfig naturalIdAccessConfig : regionConfig.getNaturalIdCaching() ) {
			accessMap.computeIfAbsent(
					naturalIdAccessConfig.getNavigableRole(),
					hierarchy -> generateNaturalIdAccess( naturalIdAccessConfig )
			);
		}

		return Collections.unmodifiableMap( accessMap );
	}

	private Map<NavigableRole, CollectionDataAccess> generateCollectionDataAccessMap(
			DomainDataRegionConfig regionConfig) {
		if ( regionConfig.getCollectionCaching().isEmpty() ) {
			return Collections.emptyMap();
		}

		final Map<NavigableRole, CollectionDataAccess> accessMap = new ConcurrentHashMap<>();
		for ( CollectionDataCachingConfig cachingConfig : regionConfig.getCollectionCaching() ) {
			accessMap.computeIfAbsent(
					cachingConfig.getNavigableRole(),
					hierarchy -> generateCollectionAccess( cachingConfig )
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

		getStorageAccess().release();
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
