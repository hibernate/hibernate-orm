/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.Cache;
import org.hibernate.HibernateException;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.service.Service;

/**
 * The SPI extension of {@link Cache}
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface CacheImplementor extends Service, Cache, Serializable {
	@Override
	SessionFactoryImplementor getSessionFactory();

	/**
	 * An initialization phase allowing the "cache engine" to prime itself
	 * from the passed configs
	 */
	void prime(Set<DomainDataRegionConfig> cacheRegionConfigs);

	/**
	 * The underlying RegionFactory in use.
	 *
	 * @return The {@code RegionFactory}
	 */
	RegionFactory getRegionFactory();

	/**
	 * Get a Region by name
	 */
	Region getRegion(String regionName);


	/**
	 * Find the cache data access strategy for the given entity.  Will
	 * return {@code null} when the entity is not configured for caching.
	 */
	EntityDataAccess getEntityRegionAccess(EntityHierarchy hierarchy);

	/**
	 * Find the cache data access strategy for the given entity's natural-id cache.
	 * Will return {@code null} when the entity does not define a natural-id, or its
	 * natural-id is not configured for caching.
	 */
	NaturalIdDataAccess getNaturalIdRegionAccess(EntityHierarchy hierarchy);

	/**
	 * Find the cache data access strategy for the given collection.  Will
	 * return {@code null} when the collection is not configured for caching.
	 */
	CollectionDataAccess getCollectionRegionAccess(PersistentCollectionDescriptor collectionDescriptor);

	/**
	 * Find the cache data access strategy for Hibernate's timestamps cache.
	 * Will return {@code null} if Hibernate is not configured for query result caching
	 */
	TimestampsCache getTimestampsRegionAccess();

	/**
	 * Access to the "default" region used to store query results when caching
	 * was requested but no region was explicitly named.  Will return {@code null}
	 * if Hibernate is not configured for query result caching
	 */
	QueryResultsCache getDefaultQueryResultsRegionAccess();

	/**
	 * Get query cache by <tt>region name</tt> or create a new one if none exist.
	 *
	 * If the region name is null, then default query cache region will be returned.
	 *
	 * Will return {@code null} if Hibernate is not configured for query result caching
	 */
	QueryResultsCache getQueryResultsCache(String regionName);

	/**
	 * Get access to the named query results cache region.  If none if found
	 * (and Hibernate is configured for query result caching), create one.
	 *
	 * Will return {@code null} if Hibernate is not configured for query result caching
	 */
	QueryResultsCache getOrMakeQueryResultsRegionAccess(String regionName);

	/**
	 * Close this "cache", releasing all underlying resources.
	 */
	void close();


	/**
	 * Clean up the default {@code QueryCache}.
	 *
	 * @throws HibernateException
	 */
	default void evictQueries() throws HibernateException {
		QueryResultsCache cache = getDefaultQueryResultsRegionAccess();
		if ( cache != null ) {
			cache.clear();
		}
	}

	/**
	 * Get the names of <tt>all</tt> cache regions, including entity, collection, natural-id and query caches.
	 *
	 * @return All cache region names
	 */
	String[] getSecondLevelCacheRegionNames();

	/**
	 * Get the default {@code QueryCache}.
	 *
	 * @deprecated Use {@link #getDefaultQueryResultsRegionAccess} instead.
	 */
	@Deprecated
	default QueryResultsCache getQueryResultsCache() {
		return getDefaultQueryResultsRegionAccess();
	}
}
