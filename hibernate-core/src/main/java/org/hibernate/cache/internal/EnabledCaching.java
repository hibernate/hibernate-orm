/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.pretty.MessageHelper;

/**
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class EnabledCaching implements CacheImplementor, DomainDataRegionBuildingContext {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EnabledCaching.class );

	private final SessionFactoryImplementor sessionFactory;
	private final String cacheRegionPrefix;

	private final SessionFactoryOptions settings;
	private final RegionFactory regionFactory;

	private final Map<String,Region> regionsByName = new ConcurrentHashMap<>();

	private Map<EntityHierarchy,EntityDataAccess> entityAccessByHierarchy = new ConcurrentHashMap<>();
	private Map<EntityHierarchy,NaturalIdDataAccess> naturalIdAccessByHierarchy = new ConcurrentHashMap<>();
	private Map<PersistentCollectionDescriptor,CollectionDataAccess> collectAccessByDescriptor = new ConcurrentHashMap<>();

	private final TimestampsCache timestampsRegionAccess;

	private final QueryResultsCache defaultQueryResultsRegionAccess;
	private final Map<String, QueryResultsCache> namedQueryResultsRegionAccess = new ConcurrentHashMap<>();


	public EnabledCaching(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.settings = sessionFactory.getSessionFactoryOptions();

		this.regionFactory = settings.getServiceRegistry().getService( RegionFactory.class );

		this.cacheRegionPrefix = StringHelper.isEmpty( sessionFactory.getSessionFactoryOptions().getCacheRegionPrefix() )
				? ""
				: sessionFactory.getSessionFactoryOptions().getCacheRegionPrefix() + ".";

		if ( settings.isQueryCacheEnabled() ) {
			final TimestampsRegion timestampsRegion = regionFactory.buildTimestampsRegion(
					qualifyRegionName( TimestampsRegion.class.getName() ),
					sessionFactory
			);
			timestampsRegionAccess = sessionFactory.getSessionFactoryOptions()
					.getTimestampsCacheFactory()
					.buildTimestampsCache( this, timestampsRegion );

			defaultQueryResultsRegionAccess = new QueryResultsCacheImpl(
					regionFactory.buildQueryResultsRegion(
							qualifyRegionName( QueryResultsCacheImpl.class.getName() ),
							sessionFactory
					),
					timestampsRegionAccess
			);
		}
		else {
			timestampsRegionAccess = new TimestampsCacheDisabledImpl();
			defaultQueryResultsRegionAccess = null;
		}
	}

	private String qualifyRegionName(String regionName) {
		return StringHelper.isEmpty( regionName )
				? null
				: cacheRegionPrefix + regionName;
	}

	@Override
	public void prime(Set<DomainDataRegionConfig> cacheRegionConfigs) {
		for ( DomainDataRegionConfig regionConfig : cacheRegionConfigs ) {
			final DomainDataRegion region = getRegionFactory().buildDomainDataRegion( regionConfig, this );

			for ( EntityDataCachingConfig entityAccessConfig : regionConfig.getEntityCaching() ) {
				entityAccessByHierarchy.put(
						entityAccessConfig.getEntityHierarchy(),
						region.getEntityDataAccess( entityAccessConfig.getEntityHierarchy().getRootEntityType().getNavigableRole() )
				);
			}

			for ( NaturalIdDataCachingConfig naturalIdAccessConfig : regionConfig.getNaturalIdCaching() ) {
				naturalIdAccessByHierarchy.put(
						naturalIdAccessConfig.getEntityHierarchy(),
						region.getNaturalIdDataAccess( naturalIdAccessConfig.getEntityHierarchy().getRootEntityType().getNavigableRole() )
				);
			}

			for ( CollectionDataCachingConfig colletionAccessConfig : regionConfig.getCollectionCaching() ) {
				collectAccessByDescriptor.put(
						colletionAccessConfig.getCollectionDescriptor(),
						region.getCollectionDataAccess( colletionAccessConfig.getCollectionDescriptor().getNavigableRole() )
				);
			}
		}

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
	public TimestampsCache getTimestampsRegionAccess() {
		return timestampsRegionAccess;
	}

	@Override
	public Region getRegion(String regionName) {
		return regionsByName.get( regionName );
	}

	@Override
	public EntityDataAccess getEntityRegionAccess(EntityHierarchy hierarchy) {
		return entityAccessByHierarchy.get( hierarchy );
	}

	@Override
	public NaturalIdDataAccess getNaturalIdRegionAccess(EntityHierarchy hierarchy) {
		return naturalIdAccessByHierarchy.get( hierarchy );
	}

	@Override
	public CollectionDataAccess getCollectionRegionAccess(PersistentCollectionDescriptor collectionDescriptor) {
		return collectAccessByDescriptor.get( collectionDescriptor );
	}

	@Override
	public boolean containsEntity(Class entityClass, Serializable identifier) {
		return containsEntity( entityClass.getName(), identifier );
	}

	@Override
	public boolean containsEntity(String entityName, Serializable identifier) {
		final EntityDescriptor entityDescriptor = sessionFactory.getMetamodel().getTypeConfiguration().findEntityDescriptor( entityName );
		final EntityDataAccess cacheAccess = entityDescriptor.getHierarchy().getEntityCacheAccess();
		if ( cacheAccess == null ) {
			return false;
		}

		final Object key = cacheAccess.generateCacheKey( identifier, entityDescriptor.getHierarchy(), sessionFactory, null );
		return cacheAccess.contains( key );
	}

	@Override
	public void evictEntity(Class entityClass, Serializable identifier) {
		evictEntity( entityClass.getName(), identifier );
	}

	@Override
	public void evictEntity(String entityName, Serializable identifier) {
		final EntityDescriptor entityDescriptor = sessionFactory.getMetamodel().getTypeConfiguration().findEntityDescriptor( entityName );
		final EntityDataAccess cacheAccess = entityDescriptor.getHierarchy().getEntityCacheAccess();
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Evicting second-level cache: %s",
					MessageHelper.infoString( entityDescriptor, identifier, sessionFactory )
			);
		}

		final Object key = cacheAccess.generateCacheKey( identifier, entityDescriptor.getHierarchy(), sessionFactory, null );
		cacheAccess.evict( key );
	}

	@Override
	public void evictEntityRegion(Class entityClass) {
		evictEntityRegion( entityClass.getName() );
	}

	@Override
	public void evictEntityRegion(String entityName) {
		evictEntityRegion(
				sessionFactory.getMetamodel().getTypeConfiguration().findEntityDescriptor( entityName )
		);
	}

	protected void evictEntityRegion(EntityDescriptor entityDescriptor) {
		entityDescriptor = entityDescriptor.getHierarchy().getRootEntityType();

		final EntityDataAccess cacheAccess = entityDescriptor.getHierarchy().getEntityCacheAccess();
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Evicting second-level cache: %s", entityDescriptor.getEntityName() );
		}

		cacheAccess.evictAll();
	}

	@Override
	public void evictEntityRegions() {
		sessionFactory.getMetamodel().getTypeConfiguration().getEntityPersisterMap().values().forEach( this::evictEntityRegion );
	}

	@Override
	public void evictNaturalIdRegion(Class entityClass) {
		evictNaturalIdRegion( entityClass.getName() );
	}

	@Override
	public void evictNaturalIdRegion(String entityName) {
		evictNaturalIdRegion(
				sessionFactory.getMetamodel().getTypeConfiguration().findEntityDescriptor( entityName )
		);
	}

	private void evictNaturalIdRegion(EntityDescriptor entityDescriptor) {
		final NaturalIdDataAccess cacheAccess = entityDescriptor.getHierarchy().getNaturalIdDescriptor().getCacheAccess();
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Evicting natural-id cache: %s", entityDescriptor.getEntityName() );
		}

		cacheAccess.evictAll();
	}

	@Override
	public void evictNaturalIdRegions() {
		final Set<EntityHierarchy> entityHierarchies = sessionFactory.getMetamodel()
				.getTypeConfiguration()
				.getEntityHierarchies();
		entityHierarchies.stream().map( EntityHierarchy::getRootEntityType ).forEach( this::evictNaturalIdRegion );
	}

	@Override
	public boolean containsCollection(String role, Serializable ownerIdentifier) {
		final PersistentCollectionDescriptor collectionDescriptor = sessionFactory.getMetamodel()
				.getTypeConfiguration()
				.findCollectionPersister( role );

		final CollectionDataAccess cacheAccess = collectionDescriptor.getCacheAccess();
		if ( cacheAccess == null ) {
			return false;
		}

		final Object key = cacheAccess.generateCacheKey( ownerIdentifier, collectionDescriptor, sessionFactory, null );
		return cacheAccess.contains( key );
	}

	@Override
	public void evictCollection(String role, Serializable ownerIdentifier) {
		final PersistentCollectionDescriptor collectionDescriptor = sessionFactory.getMetamodel()
				.getTypeConfiguration()
				.findCollectionPersister( role );

		final CollectionDataAccess cacheAccess = collectionDescriptor.getCacheAccess();
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Evicting second-level cache: %s",
					MessageHelper.collectionInfoString( collectionDescriptor, ownerIdentifier, sessionFactory )
			);
		}

		final Object key = cacheAccess.generateCacheKey( ownerIdentifier, collectionDescriptor, sessionFactory, null );
		cacheAccess.evict( key );
	}

	@Override
	public void evictCollectionRegion(String role) {
		final PersistentCollectionDescriptor collectionDescriptor = sessionFactory.getMetamodel()
				.getTypeConfiguration()
				.findCollectionPersister( role );

		evictCollectionRegion( collectionDescriptor );
	}

	private void evictCollectionRegion(PersistentCollectionDescriptor collectionDescriptor) {
		final CollectionDataAccess cacheAccess = collectionDescriptor.getCacheAccess();
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Evicting second-level cache: %s", collectionDescriptor.getNavigableRole().getFullPath() );
		}
		cacheAccess.evictAll();
	}

	@Override
	public void evictCollectionRegions() {
		sessionFactory.getMetamodel().getTypeConfiguration().getCollectionPersisterMap()
				.values()
				.forEach( this::evictCollectionRegion );
	}

	@Override
	public boolean containsQuery(String regionName) {
		final QueryResultsCache cacheAccess = getQueryResultsCache( regionName );
		return cacheAccess != null;
	}

	@Override
	public void evictDefaultQueryRegion() {
		evictQueryResultRegion( defaultQueryResultsRegionAccess );
	}

	@Override
	public void evictQueryRegion(String regionName) {
		final QueryResultsCache cacheAccess = getQueryResultsCache( regionName );
		if ( cacheAccess == null ) {
			return;
		}

		evictQueryResultRegion( cacheAccess );
	}

	private void evictQueryResultRegion(QueryResultsCache cacheAccess) {
		if ( cacheAccess == null ) {
			return;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Evicting query cache, region: %s", cacheAccess.getRegion().getName() );
		}

		cacheAccess.clear();
	}

	@Override
	public void evictQueryRegions() {
		if ( LOG.isDebugEnabled() ) {
			LOG.debug( "Evicting cache of all query regions." );
		}

		evictQueryResultRegion( defaultQueryResultsRegionAccess );

		for ( QueryResultsCache cacheAccess : namedQueryResultsRegionAccess.values() ) {
			evictQueryResultRegion( cacheAccess );
		}
	}

	@Override
	public void close() {
		for ( Region region : regionsByName.values() ) {
			region.destroy();
		}
	}

	@Override
	public QueryResultsCache getDefaultQueryResultsRegionAccess() {
		return defaultQueryResultsRegionAccess;
	}

	@Override
	public QueryResultsCache getQueryResultsCache(String regionName) throws HibernateException {
		if ( !settings.isQueryCacheEnabled() ) {
			return null;
		}

		if ( regionName == null || regionName.equals( getDefaultQueryResultsRegionAccess().getRegion().getName() ) ) {
			return getDefaultQueryResultsRegionAccess();
		}

		return namedQueryResultsRegionAccess.get( regionName );
	}

	@Override
	public QueryResultsCache getOrMakeQueryResultsRegionAccess(String regionName) {
		if ( !settings.isQueryCacheEnabled() ) {
			return null;
		}

		regionName = qualifyRegionName( regionName );

		if ( regionName == null || regionName.equals( getDefaultQueryResultsRegionAccess().getRegion().getName() ) ) {
			return getDefaultQueryResultsRegionAccess();
		}

		return namedQueryResultsRegionAccess.computeIfAbsent(
				regionName,
				this::makeQueryResultsRegionAccess
		);
	}

	protected QueryResultsCache makeQueryResultsRegionAccess(String regionName) {
		final QueryResultsRegion region = (QueryResultsRegion) regionsByName.computeIfAbsent(
				regionName,
				this::makeQueryResultsRegion
		);

		return new QueryResultsCacheImpl( region, timestampsRegionAccess );
	}

	protected QueryResultsRegion makeQueryResultsRegion(String regionName) {
		return regionFactory.buildQueryResultsRegion( regionName, getSessionFactory() );
	}

	@Override
	public String[] getSecondLevelCacheRegionNames() {
		final Set<String> names = new HashSet<>();
		names.addAll( regionsByName.keySet() );
		return ArrayHelper.toStringArray( names );
	}

	@Override
	public void evictAllRegions() {
		evictCollectionRegions();
		evictDefaultQueryRegion();
		evictEntityRegions();
		evictQueryRegions();
		evictNaturalIdRegions();
	}

	@Override
	public boolean contains(Class cls, Object primaryKey) {
		return containsEntity( cls, (Serializable) primaryKey );
	}

	@Override
	public void evict(Class cls, Object primaryKey) {
		evictEntity( cls, (Serializable) primaryKey );
	}

	@Override
	public void evict(Class cls) {
		evictEntityRegion( cls );
	}

	@Override
	public void evictAll() {
		// Evict only the "JPA cache", which is purely defined as the entity regions.
		evictEntityRegions();
		// TODO : if we want to allow an optional clearing of all cache data, the additional calls would be:
//			evictCollectionRegions();
//			evictQueryRegions();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> cls) {
		if ( org.hibernate.Cache.class.isAssignableFrom( cls ) ) {
			return (T) this;
		}

		if ( RegionFactory.class.isAssignableFrom( cls ) ) {
			return (T) regionFactory;
		}

		throw new PersistenceException( "Hibernate cannot unwrap Cache as " + cls.getName() );
	}
}
