/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.Cache;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.Remove;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.Service;

/**
 * An SPI supported by any Hibernate {@linkplain Service service} that provides an
 * implementation of the {@link Cache} API. Extends {@code Cache} with operations
 * called internally by Hibernate.
 *
 * @since 4.1
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface CacheImplementor extends Service, Cache, Serializable {
	@Override
	SessionFactoryImplementor getSessionFactory();

	/**
	 * The underlying RegionFactory in use.
	 *
	 * @apiNote CacheImplementor acts partially as a wrapper for details
	 * of interacting with the configured RegionFactory.  Care should
	 * be taken when accessing the RegionFactory directly.
	 */
	RegionFactory getRegionFactory();

	/**
	 * An initialization phase allowing the caching provider to prime itself
	 * from the passed configs
	 *
	 * @since 5.3
	 */
	void prime(Set<DomainDataRegionConfig> cacheRegionConfigs);

	/**
	 * Get a cache Region by name. If there is both a {@link DomainDataRegion}
	 * and a {@link QueryResultsRegion} with the specified name, then the
	 * {@link DomainDataRegion} will be returned.
	 *
	 * @apiNote It is only valid to call this method after {@link #prime} has
	 * been performed
	 *
	 * @since 5.3
	 */
	Region getRegion(String regionName);

	/**
	 * The unqualified name of all regions.  Intended for use with {@link #getRegion}
	 *
	 * @since 5.3
	 */
	Set<String> getCacheRegionNames();

	/**
	 * Find the cache data access strategy for Hibernate's timestamps cache.
	 * Will return {@code null} if Hibernate is not configured for query result caching
	 *
	 * @since 5.3
	 */
	TimestampsCache getTimestampsCache();

	/**
	 * Access to the "default" region used to store query results when caching
	 * was requested but no region was explicitly named.  Will return {@code null}
	 * if Hibernate is not configured for query result caching
	 */
	QueryResultsCache getDefaultQueryResultsCache();

	/**
	 * Get query cache by {@code region name} or create a new one if none exist.
	 *
	 * If the region name is null, then default query cache region will be returned.
	 *
	 * Will return {@code null} if Hibernate is not configured for query result caching
	 */
	QueryResultsCache getQueryResultsCache(String regionName);

	/**
	 * Get the named QueryResultRegionAccess but not creating one if it
	 * does not already exist.  This is intended for use by statistics.
	 *
	 * Will return {@code null} if Hibernate is not configured for query result
	 * caching or if no such region (yet) exists
	 *
	 * @since 5.3
	 */
	QueryResultsCache getQueryResultsCacheStrictly(String regionName);

	/**
	 * Clean up the default query cache
	 *
	 * @deprecated only because it's currently never called
	 */
	@Deprecated
	default void evictQueries() throws HibernateException {
		QueryResultsCache cache = getDefaultQueryResultsCache();
		if ( cache != null ) {
			cache.clear();
		}
	}

	/**
	 * Close this "cache", releasing all underlying resources.
	 */
	void close();

	/**
	 * Find the cache data access strategy for an entity.  Will
	 * return {@code null} when the entity is not configured for caching.
	 *
	 * @param rootEntityName The NavigableRole representation of the root entity
	 *
	 * @implSpec It is only valid to call this method after {@link #prime} has
	 * been performed
	 *
	 * @apiNote Use {@link EntityPersister#getCacheAccessStrategy()} instead
	 */
	@Internal
	@Remove
	EntityDataAccess getEntityRegionAccess(NavigableRole rootEntityName);

	/**
	 * Find the cache data access strategy for the given entity's natural-id cache.
	 * Will return {@code null} when the entity does not define a natural-id, or its
	 * natural-id is not configured for caching.
	 *
	 * @param rootEntityName The NavigableRole representation of the root entity
	 *
	 * @implSpec It is only valid to call this method after {@link #prime} has
	 * been performed
	 *
	 * @apiNote  Use {@link EntityPersister#getNaturalIdCacheAccessStrategy()} instead
	 */
	@Internal
	@Remove
	NaturalIdDataAccess getNaturalIdCacheRegionAccessStrategy(NavigableRole rootEntityName);

	/**
	 * Find the cache data access strategy for the given collection.  Will
	 * return {@code null} when the collection is not configured for caching.
	 *
	 * @implSpec It is only valid to call this method after {@link #prime} has
	 * been performed
	 *
	 * @apiNote  Use {@link EntityPersister#getNaturalIdCacheAccessStrategy()} instead
	 */
	@Internal
	@Remove
	CollectionDataAccess getCollectionRegionAccess(NavigableRole collectionRole);
}
