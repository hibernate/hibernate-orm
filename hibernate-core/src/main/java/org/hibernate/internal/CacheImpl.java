/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.HibernateException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.engine.spi.CacheImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class CacheImpl implements CacheImplementor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( CacheImpl.class );

	private final SessionFactoryImplementor sessionFactory;
	private final SessionFactoryOptions settings;
	private final transient QueryCache queryCache;
	private final transient RegionFactory regionFactory;
	private final transient UpdateTimestampsCache updateTimestampsCache;
	private final transient ConcurrentMap<String, QueryCache> queryCaches;
	private final transient ConcurrentMap<String, Region> allCacheRegions = new ConcurrentHashMap<String, Region>();

	public CacheImpl(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.settings = sessionFactory.getSessionFactoryOptions();
		//todo should get this from service registry
		this.regionFactory = settings.getServiceRegistry().getService( RegionFactory.class );
		regionFactory.start( settings, sessionFactory.getProperties() );
		if ( settings.isQueryCacheEnabled() ) {
			updateTimestampsCache = new UpdateTimestampsCache(
					settings,
					sessionFactory.getProperties(),
					sessionFactory
			);
			queryCache = settings.getQueryCacheFactory()
					.getQueryCache( null, updateTimestampsCache, settings, sessionFactory.getProperties() );
			queryCaches = new ConcurrentHashMap<String, QueryCache>();
			allCacheRegions.put( updateTimestampsCache.getRegion().getName(), updateTimestampsCache.getRegion() );
			allCacheRegions.put( queryCache.getRegion().getName(), queryCache.getRegion() );
		}
		else {
			updateTimestampsCache = null;
			queryCache = null;
			queryCaches = null;
		}
	}

	@Override
	public boolean containsEntity(Class entityClass, Serializable identifier) {
		return containsEntity( entityClass.getName(), identifier );
	}

	@Override
	public boolean containsEntity(String entityName, Serializable identifier) {
		EntityPersister p = sessionFactory.getEntityPersister( entityName );
		if ( p.hasCache() ) {
			EntityRegionAccessStrategy cache = p.getCacheAccessStrategy();
			Object key = cache.generateCacheKey( identifier, p, sessionFactory, null ); // have to assume non tenancy
			return cache.getRegion().contains( key );
		}
		else {
			return false;
		}
	}

	@Override
	public void evictEntity(Class entityClass, Serializable identifier) {
		evictEntity( entityClass.getName(), identifier );
	}

	@Override
	public void evictEntity(String entityName, Serializable identifier) {
		EntityPersister p = sessionFactory.getEntityPersister( entityName );
		if ( p.hasCache() ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Evicting second-level cache: %s",
						MessageHelper.infoString( p, identifier, sessionFactory )
				);
			}
			EntityRegionAccessStrategy cache = p.getCacheAccessStrategy();
			Object key = cache.generateCacheKey( identifier, p, sessionFactory, null ); // have to assume non tenancy
			cache.evict( key );
		}
	}

	@Override
	public void evictEntityRegion(Class entityClass) {
		evictEntityRegion( entityClass.getName() );
	}

	@Override
	public void evictEntityRegion(String entityName) {
		EntityPersister p = sessionFactory.getEntityPersister( entityName );
		if ( p.hasCache() ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Evicting second-level cache: %s", p.getEntityName() );
			}
			p.getCacheAccessStrategy().evictAll();
		}
	}

	@Override
	public void evictEntityRegions() {
		for ( String s : sessionFactory.getEntityPersisters().keySet() ) {
			evictEntityRegion( s );
		}
	}

	@Override
	public void evictNaturalIdRegion(Class entityClass) {
		evictNaturalIdRegion( entityClass.getName() );
	}

	@Override
	public void evictNaturalIdRegion(String entityName) {
		EntityPersister p = sessionFactory.getEntityPersister( entityName );
		if ( p.hasNaturalIdCache() ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Evicting natural-id cache: %s", p.getEntityName() );
			}
			p.getNaturalIdCacheAccessStrategy().evictAll();
		}
	}

	@Override
	public void evictNaturalIdRegions() {
		for ( String s : sessionFactory.getEntityPersisters().keySet() ) {
			evictNaturalIdRegion( s );
		}
	}

	@Override
	public boolean containsCollection(String role, Serializable ownerIdentifier) {
		CollectionPersister p = sessionFactory.getCollectionPersister( role );
		if ( p.hasCache() ) {
			CollectionRegionAccessStrategy cache = p.getCacheAccessStrategy();
			Object key = cache.generateCacheKey( ownerIdentifier, p, sessionFactory, null ); // have to assume non tenancy
			return cache.getRegion().contains( key );
		}
		else {
			return false;
		}
	}

	@Override
	public void evictCollection(String role, Serializable ownerIdentifier) {
		CollectionPersister p = sessionFactory.getCollectionPersister( role );
		if ( p.hasCache() ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"Evicting second-level cache: %s",
						MessageHelper.collectionInfoString( p, ownerIdentifier, sessionFactory )
				);
			}
			CollectionRegionAccessStrategy cache = p.getCacheAccessStrategy();
			Object key = cache.generateCacheKey( ownerIdentifier, p, sessionFactory, null ); // have to assume non tenancy
			cache.evict( key );
		}
	}

	@Override
	public void evictCollectionRegion(String role) {
		CollectionPersister p = sessionFactory.getCollectionPersister( role );
		if ( p.hasCache() ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Evicting second-level cache: %s", p.getRole() );
			}
			p.getCacheAccessStrategy().evictAll();
		}
	}

	@Override
	public void evictCollectionRegions() {
		for ( String s : sessionFactory.getCollectionPersisters().keySet() ) {
			evictCollectionRegion( s );
		}
	}

	@Override
	public boolean containsQuery(String regionName) {
		return queryCaches.containsKey( regionName );
	}

	@Override
	public void evictDefaultQueryRegion() {
		if ( sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled() ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debug( "Evicting default query region cache." );
			}
			sessionFactory.getQueryCache().clear();
		}
	}

	@Override
	public void evictQueryRegion(String regionName) {
		if ( regionName == null ) {
			throw new NullPointerException(
					"Region-name cannot be null (use Cache#evictDefaultQueryRegion to evict the default query cache)"
			);
		}
		if ( sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled() ) {
			QueryCache namedQueryCache = queryCaches.get( regionName );
			// TODO : cleanup entries in queryCaches + allCacheRegions ?
			if ( namedQueryCache != null ) {
				if ( LOG.isDebugEnabled() ) {
					LOG.debugf( "Evicting query cache, region: %s", regionName );
				}
				namedQueryCache.clear();
			}
		}
	}

	@Override
	public void evictQueryRegions() {
		evictDefaultQueryRegion();

		if ( CollectionHelper.isEmpty( queryCaches ) ) {
			return;
		}
		if ( LOG.isDebugEnabled() ) {
			LOG.debug( "Evicting cache of all query regions." );
		}
		for ( QueryCache queryCache : queryCaches.values() ) {
			queryCache.clear();
		}
	}

	@Override
	public void close() {
		if ( settings.isQueryCacheEnabled() ) {
			queryCache.destroy();

			for ( QueryCache cache : queryCaches.values() ) {
				cache.destroy();
			}
			updateTimestampsCache.destroy();
		}

		regionFactory.stop();
	}

	@Override
	public QueryCache getQueryCache() {
		return queryCache;
	}

	@Override
	public QueryCache getQueryCache(String regionName) throws HibernateException {
		if ( regionName == null ) {
			return getQueryCache();
		}

		if ( !settings.isQueryCacheEnabled() ) {
			return null;
		}

		QueryCache currentQueryCache = queryCaches.get( regionName );
		if ( currentQueryCache == null ) {
			synchronized (allCacheRegions) {
				currentQueryCache = queryCaches.get( regionName );
				if ( currentQueryCache == null ) {
					currentQueryCache = settings.getQueryCacheFactory()
							.getQueryCache(
									regionName,
									updateTimestampsCache,
									settings,
									sessionFactory.getProperties()
							);
					queryCaches.put( regionName, currentQueryCache );
					allCacheRegions.put( currentQueryCache.getRegion().getName(), currentQueryCache.getRegion() );
				}
				else {
					return currentQueryCache;
				}
			}
		}
		return currentQueryCache;
	}

	@Override
	public void addCacheRegion(String name, Region region) {
		allCacheRegions.put( name, region );
	}

	@Override
	public UpdateTimestampsCache getUpdateTimestampsCache() {
		return updateTimestampsCache;
	}

	@Override
	public void evictQueries() throws HibernateException {
		if ( settings.isQueryCacheEnabled() ) {
			queryCache.clear();
		}
	}

	@Override
	public Region getSecondLevelCacheRegion(String regionName) {
		return allCacheRegions.get( regionName );
	}

	@Override
	public Region getNaturalIdCacheRegion(String regionName) {
		return allCacheRegions.get( regionName );
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public Map<String, Region> getAllSecondLevelCacheRegions() {
		return new HashMap<String, Region>( allCacheRegions );
	}

	@Override
	public RegionFactory getRegionFactory() {
		return regionFactory;
	}

	@Override
	public void evictAllRegions() {
		evictCollectionRegions();
		evictDefaultQueryRegion();
		evictEntityRegions();
		evictQueryRegions();
		evictNaturalIdRegions();
	}
}
