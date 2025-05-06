/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat;

import java.time.Instant;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Exposes statistics collected from all sessions belonging to a given
 * {@link org.hibernate.SessionFactory}.
 * <ul>
 * <li>Collection of statistics is enabled if the configuration property
 *     {@value org.hibernate.cfg.AvailableSettings#GENERATE_STATISTICS}
 *     is set to {@code true}.
 * <li>Alternatively, statistics collection may be enabled or disabled
 *     at runtime by calling {@link #setStatisticsEnabled(boolean)}.
 * </ul>
 * <p>
 * A custom statistics collector may be supplied by implementing the
 * {@link org.hibernate.stat.spi.StatisticsImplementor} SPI, and
 * supplying a {@link org.hibernate.stat.spi.StatisticsFactory} via
 * the configuration setting
 * {@value org.hibernate.cfg.StatisticsSettings#STATS_BUILDER}.
 *
 * @author Emmanuel Bernard
 */
public interface Statistics {

	/**
	 * The default value of the configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#QUERY_STATISTICS_MAX_SIZE}.
	 */
	int DEFAULT_QUERY_STATISTICS_MAX_SIZE = 5000;

	/**
	 * Is collection of statistics enabled?
	 */
	boolean isStatisticsEnabled();

	/**
	 * Enable or disable statistics collection.
	 */
	void setStatisticsEnabled(boolean enabled);

	/**
	 * Reset all statistics.
	 */
	void clear();

	/**
	 * Log the main statistics at level {@code INFO}.
	 */
	void logSummary();


	/**
	 * Obtain the statistics for the entity with the given name.
	 *
	 * @param entityName the entity name
	 */
	EntityStatistics getEntityStatistics(String entityName);

	/**
	 * Obtain the statistics for the collection with the given role.
	 *
	 * @param role the collection role
	 */
	CollectionStatistics getCollectionStatistics(String role);

	/**
	 * Obtain the natural id resolution statistics for the entity
	 * type with the given name.
	 *
	 * @param entityName The entity name that is the root of the
	 *                   hierarchy containing the natural id
	 */
	NaturalIdStatistics getNaturalIdStatistics(String entityName);

	/**
	 * Obtain the query statistics for the given query string.
	 *
	 * @param queryString the query string, written in HQL or SQL
	 */
	QueryStatistics getQueryStatistics(String queryString);

	/**
	 * Obtain the second-level cache statistics for the given cache
	 * region.
	 *
	 * @param regionName The unqualified region name
	 *
	 * @return the statistics for the named region, or {@code null}
	 *         if the second-level cache is not enabled
	 *
	 * @throws IllegalArgumentException if there is no region with the given name
	 */
	CacheRegionStatistics getDomainDataRegionStatistics(String regionName);

	/**
	 * Obtain the second-level cache statistics for the given query
	 * cache region.
	 *
	 * @param regionName The unqualified region name
	 *
	 * @return the statistics for the named region, or {@code null}
	 *         if either query result caching is not enabled, or no
	 *         query cache region exists with the given name
	 */
	@Nullable CacheRegionStatistics getQueryRegionStatistics(String regionName);

	/**
	 * Obtain the statistics for either a domain data or query result
	 * cache region.
	 * <p>
	 * This method checks both, preferring the domain data region if
	 * there is one. Think of it as a cascading check to:<ol>
	 *     <li>{@link #getDomainDataRegionStatistics}</li>
	 *     <li>{@link #getQueryRegionStatistics}</li>
	 * </ol>
	 * Note that null is returned instead of throwing an exception when
	 * no region exists with the given name.
	 *
	 * @param regionName The unqualified region name
	 *
	 * @return the statistics for the named region, or {@code null} if
	 *         there is no region with the given name
	 */
	@Nullable CacheRegionStatistics getCacheRegionStatistics(String regionName);

	/**
	 * The global number of entity deletes.
	 */
	long getEntityDeleteCount();

	/**
	 * The global number of entity inserts.
	 */
	long getEntityInsertCount();

	/**
	 * The global number of entity loads.
	 */
	long getEntityLoadCount();

	/**
	 * The global number of entity fetches.
	 */
	long getEntityFetchCount();

	/**
	 * The global number of entity updates.
	 */
	long getEntityUpdateCount();

	/**
	 * The global number of entity upserts.
	 */
	long getEntityUpsertCount();

	/**
	 * The global number of executed queries.
	 */
	long getQueryExecutionCount();

	/**
	 * The time in milliseconds of the slowest query.
	 */
	long getQueryExecutionMaxTime();

	/**
	 * The query string for the slowest query.
	 */
	@Nullable String getQueryExecutionMaxTimeQueryString();

	/**
	 * The global number of cached queries successfully retrieved from
	 * the cache.
	 */
	long getQueryCacheHitCount();

	/**
	 * The global number of cached queries <em>not</em>not found in the
	 * cache.
	 */
	long getQueryCacheMissCount();

	/**
	 * The global number of cacheable queries put in cache.
	 */
	long getQueryCachePutCount();

	/**
	 * The global number of natural id queries executed against the
	 * database.
	 */
	long getNaturalIdQueryExecutionCount();

	/**
	 * The global maximum query time for natural id queries executed
	 * against the database.
	 */
	long getNaturalIdQueryExecutionMaxTime();

	/**
	 * The region for the maximum natural id query time.
	 */
	@Nullable String getNaturalIdQueryExecutionMaxTimeRegion();

	/**
	 * The entity name for the maximum natural id query time.
	 */
	@Nullable String getNaturalIdQueryExecutionMaxTimeEntity();

	/**
	 * The global number of cached natural id lookups successfully
	 * retrieved from the cache.
	 */
	long getNaturalIdCacheHitCount();

	/**
	 * The global number of cached natural id lookups <em>not</em> found
	 * in the cache.
	 */
	long getNaturalIdCacheMissCount();

	/**
	 * The global number of cacheable natural id lookups put in cache.
	 */
	long getNaturalIdCachePutCount();

	/**
	 * The global number of timestamps successfully retrieved from cache.
	 */
	long getUpdateTimestampsCacheHitCount();

	/**
	 * The global number of timestamp requests that were not found in the
	 * cache.
	 */
	long getUpdateTimestampsCacheMissCount();

	/**
	 * The global number of timestamps put in cache.
	 */
	long getUpdateTimestampsCachePutCount();

	/**
	 * The global number of flush operations executed, including automatic
	 * (either manual or automatic).
	 */
	long getFlushCount();

	/**
	 * The global number of connections requested by sessions.
	 * <p>
	 * The actual number of connections used may be much smaller, assuming
	 * that a connection pool is in use.
	 */
	long getConnectCount();

	/**
	 * The global number of cacheable entities and collections successfully
	 * retrieved from the cache.
	 */
	long getSecondLevelCacheHitCount();

	/**
	 * The global number of cacheable entities collections not found in the
	 * cache and loaded from the database.
	 */
	long getSecondLevelCacheMissCount();

	/**
	 * The global number of cacheable entities and collections put in the
	 * cache.
	 */
	long getSecondLevelCachePutCount();

	/**
	 * The global number of sessions closed.
	 */
	long getSessionCloseCount();

	/**
	 * The global number of sessions opened.
	 */
	long getSessionOpenCount();

	/**
	 * The global number of collections loaded.
	 */
	long getCollectionLoadCount();

	/**
	 * The global number of collections fetched.
	 */
	long getCollectionFetchCount();

	/**
	 * The global number of collections updated.
	 */
	long getCollectionUpdateCount();

	/**
	 * The global number of collections removed.
	 */
	//even on inverse="true"
	long getCollectionRemoveCount();

	/**
	 * The global number of collections recreated
	 */
	long getCollectionRecreateCount();

	/**
	 * The {@link Instant} at which this instance of {@code Statistics}
	 * was created, or since the last time {@link #clear()} was called.
	 */
	Instant getStart();

	/**
	 * The {@linkplain Instant#toEpochMilli()}  milliseconds since the
	 * initial creation of this instance, or since the last time
	 * {@link #clear()} was called.
	 *
	 * @deprecated use {@link #getStart()}
	 */
	@Deprecated(since = "6.0")
	long getStartTime();

	/**
	 * All executed query strings.
	 * <p>
	 * The maximum number of queries tracked by the Hibernate statistics
	 * is determined by the configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#QUERY_STATISTICS_MAX_SIZE}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#QUERY_STATISTICS_MAX_SIZE
	 */
	String[] getQueries();

	/**
	 * If {@value org.hibernate.cfg.AvailableSettings#LOG_SLOW_QUERY}
	 * is enabled, a map from the SQL query to the maximum execution time
	 * in milliseconds.
	 *
	 * @since 6.3
	 *
	 * @see org.hibernate.cfg.AvailableSettings#LOG_SLOW_QUERY
	 */
	Map<String,Long> getSlowQueries();

	/**
	 * The names of all entities.
	 */
	String[] getEntityNames();

	/**
	 * The names of all collection roles.
	 */
	String[] getCollectionRoleNames();

	/**
	 * All second-level cache region names. For backwards compatibility,
	 * this method returns just the names of regions storing domain data,
	 * not query result cache regions.
	 */
	String[] getSecondLevelCacheRegionNames();

	/**
	 * The number of transactions we know to have been successful.
	 */
	long getSuccessfulTransactionCount();

	/**
	 * The number of transactions we know to have completed.
	 */
	long getTransactionCount();

	/**
	 * The number of prepared statements that were acquired.
	 */
	long getPrepareStatementCount();

	/**
	 * The number of prepared statements that were released.
	 */
	long getCloseStatementCount();

	/**
	 * The number of Hibernate {@code StaleObjectStateException}s or JPA
	 * {@code OptimisticLockException}s that have occurred.
	 */
	long getOptimisticFailureCount();

	/**
	 * The global number of query plans successfully retrieved from cache.
	 */
	long getQueryPlanCacheHitCount();

	/**
	 * The global number of query plans lookups <em>not</em> found in cache.
	 */
	long getQueryPlanCacheMissCount();
}
