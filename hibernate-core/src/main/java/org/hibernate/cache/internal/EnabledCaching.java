/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.cache.spi.SecondLevelCacheLogger;

import static org.hibernate.cache.spi.RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME;
import static org.hibernate.cache.spi.RegionFactory.DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME;
import static org.hibernate.internal.util.StringHelper.qualifyConditionally;
import static org.hibernate.pretty.MessageHelper.collectionInfoString;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * A {@link CacheImplementor} service used when the second-level cache is enabled.
 *
 * @author Steve Ebersole
 * @author Strong Liu
 * @author Gail Badner
 */
public class EnabledCaching implements CacheImplementor, DomainDataRegionBuildingContext {
	private static final SecondLevelCacheLogger LOG = SecondLevelCacheLogger.L2CACHE_LOGGER;

	private final SessionFactoryImplementor sessionFactory;
	private final RegionFactory regionFactory;

	private final Map<String,Region> regionsByName = new ConcurrentHashMap<>();

	// A map by name for QueryResultsRegion instances that have the same name as a Region
	// in #regionsByName.
	private final Map<String, QueryResultsRegion> queryResultsRegionsByDuplicateName = new ConcurrentHashMap<>();

	private final Map<NavigableRole,EntityDataAccess> entityAccessMap = new ConcurrentHashMap<>();
	private final Map<NavigableRole,NaturalIdDataAccess> naturalIdAccessMap = new ConcurrentHashMap<>();
	private final Map<NavigableRole,CollectionDataAccess> collectionAccessMap = new ConcurrentHashMap<>();

	private final TimestampsCache timestampsCache;

	private final QueryResultsCache defaultQueryResultsCache;
	private final Map<String, QueryResultsCache> namedQueryResultsCacheMap = new ConcurrentHashMap<>();


	private final Set<String> legacySecondLevelCacheNames = new LinkedHashSet<>();
	private final Map<String,Set<NaturalIdDataAccess>> legacyNaturalIdAccessesForRegion = new ConcurrentHashMap<>();

	public EnabledCaching(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		final var options = sessionFactory.getSessionFactoryOptions();
		regionFactory = options.getServiceRegistry().requireService( RegionFactory.class );
		regionFactory.start( options, sessionFactory.getProperties() );
		if ( options.isQueryCacheEnabled() ) {
			timestampsCache = buildTimestampsCache( sessionFactory );
			defaultQueryResultsCache = buildQueryResultsCache( sessionFactory );
		}
		else {
			timestampsCache = new TimestampsCacheDisabledImpl();
			defaultQueryResultsCache = null;
		}
	}

	private QueryResultsCache buildQueryResultsCache(SessionFactoryImplementor sessionFactory) {
		final var queryResultsRegion =
				regionFactory.buildQueryResultsRegion(
						DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME,
						sessionFactory
				);
		regionsByName.put( queryResultsRegion.getName(), queryResultsRegion );
		return new QueryResultsCacheImpl( queryResultsRegion, timestampsCache );
	}

	private TimestampsCache buildTimestampsCache(SessionFactoryImplementor sessionFactory) {
		final var timestampsRegion =
				regionFactory.buildTimestampsRegion(
						DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME,
						sessionFactory
				);
		legacySecondLevelCacheNames.add( timestampsRegion.getName() );
		return sessionFactory.getSessionFactoryOptions().getTimestampsCacheFactory()
				.buildTimestampsCache( this, timestampsRegion );
	}

	@Override
	public void prime(Set<DomainDataRegionConfig> cacheRegionConfigs) {
		for ( var regionConfig : cacheRegionConfigs ) {
			final var region = buildRegion( regionConfig );
			regionsByName.put( region.getName(), region );
			if ( !Objects.equals( region.getName(), regionConfig.getRegionName() ) ) {
				throw new HibernateException(
						String.format(
								Locale.ROOT,
								"Region [%s] returned from RegionFactory [%s] was named differently than requested name.  Expecting `%s`, but found `%s`",
								region,
								getRegionFactory().getClass().getName(),
								regionConfig.getRegionName(),
								region.getName()
						)
				);
			}

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Entity caching

			for ( var entityAccessConfig : regionConfig.getEntityCaching() ) {
				final var navigableRole = entityAccessConfig.getNavigableRole();
				entityAccessMap.put( navigableRole, region.getEntityDataAccess( navigableRole ) );
				legacySecondLevelCacheNames.add( qualifiedRegionName( region ) );
			}


			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Natural-id caching

			final var naturalIdCaching = regionConfig.getNaturalIdCaching();
			if ( naturalIdCaching.isEmpty() ) {
				legacyNaturalIdAccessesForRegion.put( region.getName(), Collections.emptySet() );
			}
			else {
				final Set<NaturalIdDataAccess> accesses = new HashSet<>();
				for ( var naturalIdAccessConfig : naturalIdCaching ) {
					final var navigableRole = naturalIdAccessConfig.getNavigableRole();
					final var naturalIdDataAccess =
							naturalIdAccessMap.put( navigableRole,
									region.getNaturalIdDataAccess( navigableRole ) );
					accesses.add( naturalIdDataAccess );
				}
				legacyNaturalIdAccessesForRegion.put( region.getName(), accesses );
			}


			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// Collection caching

			for ( var collectionAccessConfig : regionConfig.getCollectionCaching() ) {
				final var navigableRole = collectionAccessConfig.getNavigableRole();
				collectionAccessMap.put( navigableRole,
						region.getCollectionDataAccess( navigableRole ) );
				legacySecondLevelCacheNames.add( qualifiedRegionName( region ) );
			}
		}

	}

	private String qualifiedRegionName(DomainDataRegion region) {
		return qualifyConditionally( getRegionPrefix(), region.getName() );
	}

	private String getRegionPrefix() {
		return sessionFactory.getSessionFactoryOptions().getCacheRegionPrefix();
	}

	private DomainDataRegion buildRegion(DomainDataRegionConfig regionConfig) {
		return regionFactory.buildDomainDataRegion( regionConfig, this );
	}

	@Override
	public CacheKeysFactory getEnforcedCacheKeysFactory() {
		// todo (6.0) : allow configuration of this
		return null;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public RegionFactory getRegionFactory() {
		return regionFactory;
	}

	@Override
	public TimestampsCache getTimestampsCache() {
		return timestampsCache;
	}


	@Override
	public Region getRegion(String regionName) {
		// The Region in regionsByName has precedence over the
		// QueryResultsRegion in #queryResultsRegionsByDuplicateName
		return regionsByName.get( regionName );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity data

	@Override
	public boolean containsEntity(Class<?> entityClass, Object identifier) {
		return containsEntity( entityClass.getName(), identifier );
	}

	@Override
	public boolean containsEntity(String entityName, Object identifier) {
		final var persister = getEntityDescriptor( entityName );
		final var cacheAccess = persister.getCacheAccessStrategy();
		if ( cacheAccess != null ) {
			final Object idValue =
					persister.getIdentifierMapping().getJavaType()
							.coerce( identifier, sessionFactory::getTypeConfiguration );
			final Object key = cacheAccess.generateCacheKey(
					idValue,
					persister.getRootEntityDescriptor().getEntityPersister(),
					sessionFactory,
					null
			);
			return cacheAccess.contains( key );
		}
		else {
			return false;
		}
	}

	@Override
	public void evictEntityData(Class<?> entityClass, Object identifier) {
		evictEntityData( entityClass.getName(), identifier );
	}

	@Override
	public void evictEntityData(String entityName, Object identifier) {
		final var persister = getEntityDescriptor( entityName );
		final var cacheAccess = persister.getCacheAccessStrategy();
		if ( cacheAccess != null ) {
			LOG.evictingEntityCache( infoString( persister, identifier, sessionFactory ) );
			final Object cacheKey =
					cacheAccess.generateCacheKey( identifier, persister, sessionFactory, null );
			cacheAccess.evict( cacheKey );
		}
	}

	@Override
	public void evictEntityData(Class<?> entityClass) {
		evictEntityData( entityClass.getName() );
	}

	@Override
	public void evictEntityData(String entityName) {
		evictEntityData( getEntityDescriptor( entityName ) );
	}

	private MappingMetamodelImplementor getMappingMetamodel() {
		return sessionFactory.getMappingMetamodel();
	}

	private EntityPersister getEntityDescriptor(String entityName) {
		return getMappingMetamodel().getEntityDescriptor( entityName );
	}

	private CollectionPersister getCollectionDescriptor(String role) {
		return getMappingMetamodel().getCollectionDescriptor( role );
	}

	protected void evictEntityData(EntityPersister entityDescriptor) {
		final var rootEntityDescriptor = rootEntityPersister( entityDescriptor );
		evictEntityData( rootEntityDescriptor.getNavigableRole(),
				rootEntityDescriptor.getCacheAccessStrategy() );
	}

	private static EntityPersister rootEntityPersister(EntityPersister entityDescriptor) {
		if ( entityDescriptor.isInherited()
				&& !entityDescriptor.getEntityName().equals( entityDescriptor.getRootEntityName() ) ) {
			return entityDescriptor.getRootEntityDescriptor().getEntityPersister();
		}
		else {
			return entityDescriptor;
		}
	}

	private void evictEntityData(NavigableRole navigableRole, EntityDataAccess cacheAccess) {
		if ( cacheAccess != null ) {
			LOG.evictingEntityCacheByRole( navigableRole.getFullPath() );
			cacheAccess.evictAll();
		}
	}

	@Override
	public void evictEntityData() {
		getMappingMetamodel().forEachEntityDescriptor( this::evictEntityData );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Natural-id data

	@Override
	public void evictNaturalIdData(Class<?> entityClass) {
		evictNaturalIdData( entityClass.getName() );
	}

	@Override
	public void evictNaturalIdData(String entityName) {
		evictNaturalIdData( getEntityDescriptor( entityName ) );
	}

	private void evictNaturalIdData(EntityPersister rootEntityDescriptor) {
		evictNaturalIdData( rootEntityDescriptor.getNavigableRole(),
				rootEntityDescriptor.getNaturalIdCacheAccessStrategy() );
	}

	@Override
	public void evictNaturalIdData() {
		naturalIdAccessMap.forEach( this::evictNaturalIdData );
	}

	private void evictNaturalIdData(NavigableRole rootEntityRole, NaturalIdDataAccess cacheAccess) {
		if ( cacheAccess != null ) {
			LOG.evictingNaturalIdCache( rootEntityRole.getFullPath() );
			cacheAccess.evictAll();
		}
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection data

	@Override
	public boolean containsCollection(String role, Object ownerIdentifier) {
		final var persister = getCollectionDescriptor( role );
		final var cacheAccess = persister.getCacheAccessStrategy();
		if ( cacheAccess != null ) {
			final Object cacheKey =
					cacheAccess.generateCacheKey( ownerIdentifier, persister, sessionFactory, null );
			return cacheAccess.contains( cacheKey );
		}
		else {
			return false;
		}
	}

	@Override
	public void evictCollectionData(String role, Object ownerIdentifier) {
		final var persister = getCollectionDescriptor( role );
		final var cacheAccess = persister.getCacheAccessStrategy();
		if ( cacheAccess != null ) {
			LOG.evictingCollectionCache( collectionInfoString( persister, ownerIdentifier, sessionFactory ) );
			final Object cacheKey =
					cacheAccess.generateCacheKey( ownerIdentifier, persister, sessionFactory, null );
			cacheAccess.evict( cacheKey );
		}
	}

	@Override
	public void evictCollectionData(String role) {
		evictCollectionData( getCollectionDescriptor( role ) );
	}

	private void evictCollectionData(CollectionPersister collectionDescriptor) {
		evictCollectionData( collectionDescriptor.getNavigableRole(),
				collectionDescriptor.getCacheAccessStrategy() );
	}

	private void evictCollectionData(NavigableRole navigableRole, CollectionDataAccess cacheAccess) {
		if ( cacheAccess != null ) {
			LOG.evictingCollectionCacheByRole( navigableRole.getFullPath() );
			cacheAccess.evictAll();
		}
	}

	@Override
	public void evictCollectionData() {
		collectionAccessMap.forEach( this::evictCollectionData );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query-results data

	@Override
	public boolean containsQuery(String regionName) {
		return getQueryResultsCacheStrictly( regionName ) != null;
	}

	@Override
	public void evictDefaultQueryRegion() {
		evictQueryResultRegion( defaultQueryResultsCache );
	}

	@Override
	public void evictQueryRegion(String regionName) {
		final var cache = getQueryResultsCache( regionName );
		if ( cache != null ) {
			evictQueryResultRegion( cache );
		}
	}

	private void evictQueryResultRegion(QueryResultsCache cache) {
		if ( cache != null ) {
			LOG.evictingQueryCacheRegion( cache.getRegion().getName() );
			cache.clear();
		}
	}

	@Override
	public void evictQueryRegions() {
		LOG.evictingAllQueryRegions();
		evictQueryResultRegion( defaultQueryResultsCache );
		for ( QueryResultsCache cache : namedQueryResultsCacheMap.values() ) {
			evictQueryResultRegion( cache );
		}
	}

	@Override
	public void evictAll() {
		// Evict only the "JPA cache", which is purely defined as the entity regions.
		evictEntityData();
	}

	@Override
	public void evictAllRegions() {
		evictEntityData();
		evictNaturalIdData();
		evictCollectionData();
		evictDefaultQueryRegion();
		evictQueryRegions();
	}

	@Override
	public QueryResultsCache getDefaultQueryResultsCache() {
		return defaultQueryResultsCache;
	}

	private String getDefaultResultCacheName() {
		return defaultQueryResultsCache.getRegion().getName();
	}

	private boolean isQueryCacheEnabled() {
		return sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled();
	}

	@Override
	public QueryResultsCache getQueryResultsCache(String regionName) throws HibernateException {
		if ( !isQueryCacheEnabled() ) {
			return null;
		}
		else if ( regionName == null || regionName.equals( getDefaultResultCacheName() ) ) {
			return getDefaultQueryResultsCache();
		}
		else {
			final var existing = namedQueryResultsCacheMap.get( regionName );
			return existing != null ? existing : makeQueryResultsRegionAccess( regionName );
		}
	}

	@Override
	public QueryResultsCache getQueryResultsCacheStrictly(String regionName) {
		if ( !isQueryCacheEnabled() ) {
			return null;
		}
		else if ( regionName == null || regionName.equals( getDefaultResultCacheName() ) ) {
			return getDefaultQueryResultsCache();
		}
		else {
			return namedQueryResultsCacheMap.get( regionName );
		}
	}

	protected QueryResultsCache makeQueryResultsRegionAccess(String regionName) {
		final var regionAccess = new QueryResultsCacheImpl( getQueryResultsRegion( regionName ), timestampsCache );
		namedQueryResultsCacheMap.put( regionName, regionAccess );
		legacySecondLevelCacheNames.add( regionName );
		return regionAccess;
	}

	private QueryResultsRegion getQueryResultsRegion(String regionName) {
		final Region region = regionsByName.computeIfAbsent( regionName, this::makeQueryResultsRegion );
		return region instanceof QueryResultsRegion queryResultsRegion
				? queryResultsRegion // There was already a different type of Region with the same name.
				: queryResultsRegionsByDuplicateName.computeIfAbsent( regionName, this::makeQueryResultsRegion );
	}

	protected QueryResultsRegion makeQueryResultsRegion(String regionName) {
		return regionFactory.buildQueryResultsRegion( regionName, getSessionFactory() );
	}

	@Override
	public Set<String> getCacheRegionNames() {
		return regionsByName.keySet();
	}

	@Override
	public void evictRegion(String regionName) {
		getRegion( regionName ).clear();
		final var queryResultsRegionWithDuplicateName =
				queryResultsRegionsByDuplicateName.get( regionName );
		if ( queryResultsRegionWithDuplicateName != null ) {
			queryResultsRegionWithDuplicateName.clear();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> type) {
		if ( type.isAssignableFrom( EnabledCaching.class ) ) {
			return (T) this;
		}
		else if ( type.isAssignableFrom( RegionFactory.class ) ) {
			return (T) regionFactory;
		}
		else {
			throw new PersistenceException( "Hibernate cannot unwrap Cache as '" + type.getName() + "'" );
		}
	}

	@Override
	public void close() {
		for ( var region : regionsByName.values() ) {
			region.destroy();
		}
		for ( var region : queryResultsRegionsByDuplicateName.values() ) {
			region.destroy();
		}
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA-defined methods

	@Override
	public boolean contains(Class cls, Object primaryKey) {
		// JPA
		return containsEntity( cls, primaryKey );
	}

	@Override
	public void evict(Class cls, Object primaryKey) {
		// JPA call
		evictEntityData( cls, primaryKey );
	}

	@Override
	public void evict(Class cls) {
		// JPA
		evictEntityData( cls );
	}




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	@Override @Deprecated
	public EntityDataAccess getEntityRegionAccess(NavigableRole rootEntityName) {
		return entityAccessMap.get( rootEntityName );
	}

	@Override @Deprecated
	public NaturalIdDataAccess getNaturalIdCacheRegionAccessStrategy(NavigableRole rootEntityName) {
		return naturalIdAccessMap.get( rootEntityName );
	}

	@Override @Deprecated
	public CollectionDataAccess getCollectionRegionAccess(NavigableRole collectionRole) {
		return collectionAccessMap.get( collectionRole );
	}
}
