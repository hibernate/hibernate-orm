/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import java.io.Serializable;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
	@Nonnull
	SessionFactoryImplementor getSessionFactory();

	/**
	 * The underlying {@link RegionFactory} in use.
	 *
	 * @apiNote {@code CacheImplementor} acts partially as a wrapper for
	 *          details of interacting with the configured {@code RegionFactory}.
	 *          Care should be taken when accessing the {@code RegionFactory}
	 *          directly.
	 */
	@Nonnull
	RegionFactory getRegionFactory();

	/**
	 * An initialization phase allowing the caching provider to prime itself
	 * from the passed configurations.
	 *
	 * @since 5.3
	 */
	void prime(@Nonnull Set<DomainDataRegionConfig> cacheRegionConfigs);

	/**
	 * Get a cache Region by name. If there is both a {@link DomainDataRegion}
	 * and a {@link QueryResultsRegion} with the specified name, then the
	 * {@link DomainDataRegion} will be returned.
	 *
	 * @apiNote It is illegal to call this method before {@link #prime} has
	 *          been called.
	 *
	 * @since 5.3
	 */
	@Nullable
	Region getRegion(@Nonnull String regionName);

	/**
	 * The unqualified name of all regions. Intended for use with {@link #getRegion}.
	 *
	 * @since 5.3
	 */
	@Nonnull
	Set<String> getCacheRegionNames();

	/**
	 * The cache data access strategy for the timestamp cache, or return {@code null}
	 * if Hibernate is not configured for query result caching.
	 *
	 * @since 5.3
	 */
	@Nullable
	TimestampsCache getTimestampsCache();

	/**
	 * Access to the "default" region used to store query results when caching is
	 * requested, but no region was explicitly named, or return {@code null} if
	 * Hibernate is not configured for query result caching.
	 */
	@Nullable
	QueryResultsCache getDefaultQueryResultsCache();

	/**
	 * Get the named region of the query results cache, creating it if it does not
	 * already exist. If the given region name is null, the default query cache
	 * region is returned. Return {@code null} if Hibernate is not configured for
	 * query result caching.
	 */
	@Nullable
	QueryResultsCache getQueryResultsCache(@Nullable String regionName);

	/**
	 * Get the named region of the query cache, but without creating it if it does
	 * not already exist, or return {@code null} if Hibernate is not configured for
	 * query result caching or if no such region (yet) exists.
	 *
	 * @apiNote This is intended for use by statistics.
	 *
	 * @since 5.3
	 */
	@Nullable
	QueryResultsCache getQueryResultsCacheStrictly(@Nullable String regionName);

	/**
	 * Clean up the default query results cache.
	 *
	 * @deprecated only because it's currently never called
	 */
	@Deprecated
	default void evictQueries() throws HibernateException {
		final var cache = getDefaultQueryResultsCache();
		if ( cache != null ) {
			cache.clear();
		}
	}

	/**
	 * Close this "cache", releasing all underlying resources.
	 */
	void close();

	/**
	 * Find the cache data access strategy for an entity, or return {@code null}
	 * if the given entity is not configured for caching.
	 *
	 * @param rootEntityName The NavigableRole representation of the root entity
	 *
	 * @implSpec It is illegal to call this method before {@link #prime} has
	 *           been called.
	 *
	 * @apiNote Use {@link EntityPersister#getCacheAccessStrategy()} instead
	 */
	@Internal
	@Remove
	@Nullable
	EntityDataAccess getEntityRegionAccess(@Nonnull NavigableRole rootEntityName);

	/**
	 * Find the cache data access strategy for the given entity's natural id cache,
	 * or return {@code null} when the entity does not define a natural id, or its
	 * natural id is not configured for caching.
	 *
	 * @param rootEntityName The NavigableRole representation of the root entity
	 *
	 * @implSpec It is illegal to call this method before {@link #prime} has
	 *           been called.
	 *
	 * @apiNote Use {@link EntityPersister#getNaturalIdCacheAccessStrategy()} instead
	 */
	@Internal
	@Remove
	@Nullable
	NaturalIdDataAccess getNaturalIdCacheRegionAccessStrategy(@Nonnull NavigableRole rootEntityName);

	/**
	 * Find the cache data access strategy for the given collection, or return
	 * {@code null} when the collection is not configured for caching.
	 *
	 * @implSpec It is illegal to call this method before {@link #prime} has
	 *           been called.
	 *
	 * @apiNote Use {@link EntityPersister#getNaturalIdCacheAccessStrategy()} instead
	 */
	@Internal
	@Remove
	@Nullable
	CollectionDataAccess getCollectionRegionAccess(@Nonnull NavigableRole collectionRole);
}
