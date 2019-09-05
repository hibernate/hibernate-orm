/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.hibernate.Cache;
import org.hibernate.HibernateException;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.Service;

/**
 * SPI contract for Hibernate's second-level cache engine
 *
 * @since 4.1
 *
 * @author Strong Liu
 * @author Steve Ebersole
 * @author Gail Badner
 */
@SuppressWarnings("unused")
public interface CacheImplementor extends Service, Cache, org.hibernate.engine.spi.CacheImplementor, Serializable {
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
	 * <p/>
	 * If Hibernate is configured for query result caching, and the {@code regionName}
	 * is the name of the default {@link QueryResultsRegion}, then the default {@link QueryResultsRegion}
	 * will be returned.
	 * <p/>
	 * @apiNote It is only valid to call this method after {@link #prime} has
	 * been performed
	 *
	 * @since 5.3
	 *
	 * @return the {@link Region} or {@code null} if there is none with the specified name.
	 *
	 * @deprecated to access a {@link DomainDataRegion}, use {@link #getDomainDataRegion(String)};
	 * to access the default {@link QueryResultsRegion}, use {@link #getDefaultQueryResultsCache}.getRegion();
	 * to access a named (non-default) {@link QueryResultsRegion}, use
	 * {@link #getQueryResultsCacheStrictly(String)}.getRegion().
	 */
	@Deprecated
	Region getRegion(String regionName);

	/**
	 * The unqualified name of all regions.  Intended for use with {@link #getRegion}
	 * <p/>
	 * If Hibernate is configured for query result caching, then the name of the
	 * default {@link QueryResultsRegion} will be included in the returned Set.
	 *
	 * @since 5.3
	 * @deprecated Use {@link #getDomainDataRegionNames} to get all {@link DomainDataRegion} names;
	 * use {@link #getQueryCacheRegionNames()} to get all named (non-default) {@link DomainDataRegion}
	 * names; use {@link #getDefaultQueryResultsCache()}.getRegion().getName() to get the name of
	 * the default {@link QueryResultsRegion}.
	 */
	@Deprecated
	Set<String> getCacheRegionNames();

	/**
	 * The unqualified names of all {@link DomainDataRegion} regions. Intended for use with
	 * {@link #getDomainDataRegion(String)}.
	 *
	 * @apiNote It is only valid to call this method after {@link #prime} has
	 * been performed
	 *
	 * @since 5.3
	 */
	default Set<String> getDomainDataRegionNames() {
		final Set<String> domainDataRegionNames = new HashSet<>();
		for ( String regionName : getCacheRegionNames() ) {
			final Region region = getRegion( regionName );
			if ( DomainDataRegion.class.isInstance( region ) ) {
				domainDataRegionNames.add( regionName );
			}
		}
		return domainDataRegionNames;
	}

	/**
	 * The unqualified names of all named (non-default) {@link QueryResultsRegion} regions. Intended for use with
	 * {@link #getQueryResultsCacheStrictly(String)}.
	 * <p/>
	 * The name of the default {@link QueryResultsRegion} will not be included in the returned Set.
	 * {@link #getDefaultQueryResultsCache()} should be used to get access to the default
	 * {@link QueryResultsCache}.
	 *
	 * @since 5.3
	 */
	default Set<String> getQueryCacheRegionNames() {
		final Set<String> queryCacheRegionNames = new HashSet<>();
		for ( String regionName : getCacheRegionNames() ) {
			final QueryResultsCache queryResultsCache = getQueryResultsCacheStrictly( regionName );
			if ( queryResultsCache != null ) {
				queryCacheRegionNames.add( queryResultsCache.getRegion().getName() );
			}
		}
		return queryCacheRegionNames;
	}

	/**
	 * Get a {@link DomainDataRegion} by name.
	 *
	 * @apiNote It is only valid to call this method after {@link #prime} has
	 * been performed
	 *
	 * @return the {@link DomainDataRegion} with the specified name, or {@code null}
	 * if there is no {@link DomainDataRegion} with that name.
	 *
	 * @since 5.3
	 */
	default DomainDataRegion getDomainDataRegion(String regionName) {
		final Region region = getRegion( regionName );
		return DomainDataRegion.class.isInstance( region )
				? (DomainDataRegion) region
				: null;
	}

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
	 * Get query cache by <tt>region name</tt> or create a new one if none exist.
	 *
	 * If the region name is null, then default query cache region will be returned.
	 *
	 * Will return {@code null} if Hibernate is not configured for query result caching
	 */
	QueryResultsCache getQueryResultsCache(String regionName);

	/**
	 * Get the named QueryResultRegionAccess but not creating one if it
	 * does not already exist.  This is intended for use by statistics.
	 * <p/>
	 * Will return {@code null} if Hibernate is not configured for query result
	 * caching, if no such region (yet) exists, or if {@code regionName} is the
	 * name of the default {@link QueryResultsRegion}.
	 * <p/>
	 * {@link #getDefaultQueryResultsCache()} should be used to get access to the default
	 * {@link QueryResultsCache}.
	 *
	 * @since 5.3
	 */
	QueryResultsCache getQueryResultsCacheStrictly(String regionName);

	/**
	 * Clean up the default query cache
	 */
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations (5.3)

	/**
	 * Get the *qualified* names of all regions caching entity and collection data.
	 *
	 * @return All cache region names
	 *
	 * @deprecated (since 5.3) Use {@link #getDomainDataRegionNames} to get all {@link DomainDataRegion} names;
	 * use {@link #getQueryCacheRegionNames()} to get all named (non-default) {@link DomainDataRegion}
	 * names; use {@link #getDefaultQueryResultsCache()}.getRegion().getName() to get the name of
	 * the default {@link QueryResultsRegion}.
	 */
	@Deprecated
	String[] getSecondLevelCacheRegionNames();

	/**
	 * Find the cache data access strategy for an entity.  Will
	 * return {@code null} when the entity is not configured for caching.
	 *
	 * @param rootEntityName The NavigableRole representation of the root entity
	 *
	 * @apiNote It is only valid to call this method after {@link #prime} has
	 * been performed
	 *
	 * @deprecated Use {@link EntityPersister#getCacheAccessStrategy()} instead
	 */
	@Deprecated
	EntityDataAccess getEntityRegionAccess(NavigableRole rootEntityName);

	/**
	 * Find the cache data access strategy for the given entity's natural-id cache.
	 * Will return {@code null} when the entity does not define a natural-id, or its
	 * natural-id is not configured for caching.
	 *
	 * @param rootEntityName The NavigableRole representation of the root entity
	 *
	 * @apiNote It is only valid to call this method after {@link #prime} has
	 * been performed
	 *
	 * @deprecated Use {@link EntityPersister#getNaturalIdCacheAccessStrategy()} ()} instead
	 */
	@Deprecated
	NaturalIdDataAccess getNaturalIdCacheRegionAccessStrategy(NavigableRole rootEntityName);

	/**
	 * Find the cache data access strategy for the given collection.  Will
	 * return {@code null} when the collection is not configured for caching.
	 *
	 * @apiNote It is only valid to call this method after {@link #prime} has
	 * been performed
	 *
	 * @deprecated Use {@link EntityPersister#getNaturalIdCacheAccessStrategy()} ()} instead
	 */
	@Deprecated
	CollectionDataAccess getCollectionRegionAccess(NavigableRole collectionRole);


	/**
	 * Get {@code UpdateTimestampsCache} instance managed by the {@code SessionFactory}.
	 *
	 * @deprecated Use {@link #getTimestampsCache} instead
	 */
	@Deprecated
	default UpdateTimestampsCache getUpdateTimestampsCache() {
		return getTimestampsCache();
	}

	/**
	 * Get the default {@code QueryCache}.
	 *
	 * @deprecated Use {@link #getDefaultQueryResultsCache} instead.
	 */
	@Deprecated
	default QueryCache getQueryCache() {
		return getDefaultQueryResultsCache();
	}

	/**
	 * Get the default {@code QueryCache}.
	 *
	 * @deprecated Use {@link #getDefaultQueryResultsCache} instead.
	 */
	@Deprecated
	default QueryCache getDefaultQueryCache() {
		return getDefaultQueryResultsCache();
	}

	/**
	 * @deprecated Use {@link #getQueryResultsCache(String)} instead, but using unqualified name
	 */
	@Deprecated
	default QueryCache getQueryCache(String regionName) throws HibernateException {
		return getQueryResultsCache( unqualifyRegionName( regionName ) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Some new (default) support methods for the above deprecations
	//		- themselves deprecated

	/**
	 * @deprecated (since 5.3) No replacement - added just to continue some backwards compatibility
	 * in supporting the newly deprecated methods expecting a qualified (prefix +) region name
	 */
	@Deprecated
	default String unqualifyRegionName(String name) {
		if ( getSessionFactory().getSessionFactoryOptions().getCacheRegionPrefix() == null ) {
			return name;
		}

		if ( !name.startsWith( getSessionFactory().getSessionFactoryOptions().getCacheRegionPrefix() ) ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Legacy methods for accessing cache information expect a qualified (prefix) region name - " +
									"but passed name [%s] was not qualified by the configured prefix [%s]",
							name,
							getSessionFactory().getSessionFactoryOptions().getCacheRegionPrefix()
					)
			);
		}

		return name.substring( getSessionFactory().getSessionFactoryOptions().getCacheRegionPrefix().length() + 1 );
	}

	/**
	 * @deprecated No replacement - added just for support of the newly deprecated methods expecting a qualified region name
	 */
	@Deprecated
	default Region getRegionByLegacyName(String legacyName) {
		return getRegion( unqualifyRegionName( legacyName ) );
	}

	/**
	 * @deprecated No replacement - added just for support of the newly deprecated methods expecting a qualified region name
	 */
	@Deprecated
	Set<NaturalIdDataAccess> getNaturalIdAccessesInRegion(String legacyQualifiedRegionName);
}
