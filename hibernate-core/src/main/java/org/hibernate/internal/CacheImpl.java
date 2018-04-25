/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
	private final transient ConcurrentMap<String, EntityRegion> entityRegionMap = new ConcurrentHashMap<String, EntityRegion>();
	private final transient ConcurrentMap<String, CollectionRegion> collectionRegionMap = new ConcurrentHashMap<String, CollectionRegion>();
	private final transient ConcurrentMap<String, NaturalIdRegion> naturalIdRegionMap = new ConcurrentHashMap<String, NaturalIdRegion>();
	private final transient ConcurrentMap<String, Region> otherRegionMap = new ConcurrentHashMap<String, Region>();

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
		if ( sessionFactory.getSessionFactoryOptions().isQueryCacheEnabled() ) {
			return queryCaches.containsKey(regionName);
		}
		else {
			return false;
		}
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
			// TODO : cleanup entries in queryCaches ?
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
			synchronized (queryCaches) {
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
		// Add the Region to all applicable maps
		boolean isOtherRegion = true;
		if ( EntityRegion.class.isInstance( region ) ) {
			entityRegionMap.put( name, (EntityRegion) region );
			isOtherRegion = false;
		}
		if ( CollectionRegion.class.isInstance( region ) ) {
			collectionRegionMap.put( name, (CollectionRegion) region );
			isOtherRegion = false;
		}
		if ( NaturalIdRegion.class.isInstance( region ) ) {
			naturalIdRegionMap.put( name, (NaturalIdRegion) region );
			isOtherRegion = false;
		}
		// If the Region is not an EntityRegion, CollectionRegion, or NaturalIdRegion,
		// then add it to otherRegionMap; the only other type of Region that Hibernate knows about
		// is updateTimestampsCache, queryCache, and queryCaches; those regions should not
		// be added using this method.
		if ( isOtherRegion ) {
			// in case an application uses this method for some other type of Region
			otherRegionMap.put( name, region );
		}
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

		// Order is important!
		// To keep results of #getSecondLevelCacheRegion and #getAllSecondLevelCacheRegions consistent,
		// the order of lookups in getSecondLevelCacheRegion needs to be the opposite of the order in
		// which Map contents are added to getAllSecondLevelCacheRegions.
		// In addition, if there is an EntityRegion and CollectionRegion with the same name,
		// then the EntityRegion should be returned.
		Region region = entityRegionMap.get(regionName);
		if ( region != null ) {
			return region;
		}
		region = collectionRegionMap.get( regionName );
		if ( region != null ) {
			return region;
		}
		region = naturalIdRegionMap.get( regionName );
		if ( region != null ) {
			return region;
		}
		region = otherRegionMap.get( regionName );
		if ( region != null ) {
			return region;
		}
		if ( settings.isQueryCacheEnabled() ) {
			// keys in queryCaches may not be equal to the Region names
			// obtained from queryCaches values; we need to be sure we are comparing
			// the actual QueryCache region names with regionName.
			for ( QueryCache queryCacheValue : queryCaches.values() ) {
				if ( queryCacheValue.getRegion().getName().equals( regionName ) ) {
					return queryCacheValue.getRegion();
				}
			}
			if ( queryCache.getRegion().getName().equals( regionName ) ) {
				return queryCache.getRegion();
			}
			if ( updateTimestampsCache.getRegion().getName().equals( regionName ) ) {
				return updateTimestampsCache.getRegion();
			}
		}
		// no Region with the specified name
		return null;
	}

	@Override
	public Region getNaturalIdCacheRegion(String regionName) {
		return naturalIdRegionMap.get( regionName );
	}

	@Override
	public Map<String, Region> getAllSecondLevelCacheRegions() {

		// Order is important!
		// To keep results of #getSecondLevelCacheRegion and #getAllSecondLevelCacheRegions consistent,
		// the order of lookups in getSecondLevelCacheRegion needs to be the opposite of the order in
		// which Map contents are added to getAllSecondLevelCacheRegions.
		// In addition, if there is a CollectionRegion and an EntityRegion with the same name, then we
		// want the EntityRegion to be in the Map that gets returned.
		final Map<String, Region> allCacheRegions = new HashMap<String, Region>();
		if ( settings.isQueryCacheEnabled() ) {
			allCacheRegions.put( updateTimestampsCache.getRegion().getName(), updateTimestampsCache.getRegion() );
			allCacheRegions.put( queryCache.getRegion().getName(), queryCache.getRegion() );
			// keys in queryCaches may not be equal to the Region names
			// obtained from queryCaches values; we need to be sure we are adding
			// the actual QueryCache region name as the key in allCacheRegions.
			for ( QueryCache queryCacheValue : queryCaches.values() ) {
				allCacheRegions.put( queryCacheValue.getRegion().getName(), queryCacheValue.getRegion() );
			}
		}
		allCacheRegions.putAll( otherRegionMap );
		allCacheRegions.putAll( naturalIdRegionMap );
		allCacheRegions.putAll( collectionRegionMap );
		allCacheRegions.putAll( entityRegionMap );
		return allCacheRegions;
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
