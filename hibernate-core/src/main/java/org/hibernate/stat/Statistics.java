/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

import javax.management.MXBean;

/**
 * Exposes statistics for a particular {@link org.hibernate.SessionFactory}.  Beware of milliseconds metrics, they
 * are dependent of the JVM precision: you may then encounter a 10 ms approximation depending on you OS platform.
 * Please refer to the JVM documentation for more information.
 *
 * @author Emmanuel Bernard
 */
@MXBean
public interface Statistics {

	int DEFAULT_QUERY_STATISTICS_MAX_SIZE = 5000;

	/**
	 * Are statistics enabled
	 */
	boolean isStatisticsEnabled();

	/**
	 * Enable statistics logs (this is a dynamic parameter)
	 */
	void setStatisticsEnabled(boolean b);
	/**
	 * reset all statistics
	 */
	void clear();

	/**
	 * log in info level the main statistics
	 */
	void logSummary();


    /**
	 * find entity statistics per name
	 *
	 * @param entityName entity name
	 * @return EntityStatistics object
	 */
	EntityStatistics getEntityStatistics(String entityName);

	/**
	 * Get collection statistics per role
	 *
	 * @param role collection role
	 * @return CollectionStatistics
	 */
	CollectionStatistics getCollectionStatistics(String role);

	/**
	 * Natural id resolution query statistics for an entity type
	 *
	 * @param entityName The entity name that is the root of the hierarchy containing the
	 * natural id
	 * @return NaturalIdCacheStatistics
	 */
	NaturalIdStatistics getNaturalIdStatistics(String entityName);

	/**
	 * Query statistics from query string (HQL or SQL)
	 *
	 * @param queryString query string
	 * @return QueryStatistics
	 */
	QueryStatistics getQueryStatistics(String queryString);

	/**
	 * Second-level cache statistics per domain data (entity, collection, natural-id) region
	 *
	 * @param regionName The unqualified region name
	 *
	 * @return The stats for the named region, or {@code null} if the second level cache is
	 * not enabled
	 *
	 * @throws IllegalArgumentException if the region name could not be resolved
	 */
	CacheRegionStatistics getDomainDataRegionStatistics(String regionName);

	/**
	 * Second-level cache statistics per query region
	 *
	 * @param regionName The unqualified region name
	 *
	 * @return Stats for the named region, or {@code null} if (1) query result caching is
	 * not enabled or (2) no query region exists with that name
	 */
	CacheRegionStatistics getQueryRegionStatistics(String regionName);

	/**
	 * Get statistics for either a domain-data or query-result region - this
	 * method checks both, preferring domain data region if one.  Think of it
	 * as a cascading check to:<ol>
	 *     <li>{@link #getDomainDataRegionStatistics}</li>
	 *     <li>{@link #getQueryRegionStatistics}</li>
	 * </ol>
	 * Note that returning null is preferred here over throwing an exception when
	 * no region exists with that name.
	 *
	 * @param regionName The unqualified region name
	 *
	 * @return Stats for the named region, or {@code null} if no such region exists
	 */
	CacheRegionStatistics getCacheRegionStatistics(String regionName);

    /**
     * Get global number of entity deletes
	 * @return entity deletion count
	 */
	long getEntityDeleteCount();

    /**
     * Get global number of entity inserts
	 * @return entity insertion count
	 */
	long getEntityInsertCount();

    /**
     * Get global number of entity loads
	 * @return entity load (from DB)
	 */
	long getEntityLoadCount();

	/**
     * Get global number of entity fetches
	 * @return entity fetch (from DB)
	 */
	long getEntityFetchCount();

	/**
     * Get global number of entity updates
	 * @return entity update
	 */
	long getEntityUpdateCount();

    /**
     * Get global number of executed queries
	 * @return query execution count
	 */
	long getQueryExecutionCount();

    /**
     * Get the time in milliseconds of the slowest query.
     */
	long getQueryExecutionMaxTime();

	/**
	 * Get the query string for the slowest query.
	 */
	String getQueryExecutionMaxTimeQueryString();

    /**
     * Get the global number of cached queries successfully retrieved from cache
     */
	long getQueryCacheHitCount();

    /**
     * Get the global number of cached queries *not* found in cache
     */
	long getQueryCacheMissCount();

    /**
     * Get the global number of cacheable queries put in cache
     */
	long getQueryCachePutCount();

	/**
	 * Get the global number of natural id queries executed against the database
	 */
	long getNaturalIdQueryExecutionCount();

	/**
	 * Get the global maximum query time for natural id queries executed against the database
	 */
	long getNaturalIdQueryExecutionMaxTime();

	/**
	 * Get the region for the maximum natural id query time
	 */
	String getNaturalIdQueryExecutionMaxTimeRegion();

	/**
	 * Get the entity for the maximum natural id query time
	 */
	String getNaturalIdQueryExecutionMaxTimeEntity();

    /**
     * Get the global number of cached natural id lookups successfully retrieved from cache
     */
	long getNaturalIdCacheHitCount();

    /**
     * Get the global number of cached natural id lookups *not* found in cache
     */
	long getNaturalIdCacheMissCount();

    /**
     * Get the global number of cacheable natural id lookups put in cache
     */
	long getNaturalIdCachePutCount();

    /**
     * Get the global number of timestamps successfully retrieved from cache
     */
	long getUpdateTimestampsCacheHitCount();

    /**
     * Get the global number of timestamp requests that were not found in the cache
     */
	long getUpdateTimestampsCacheMissCount();

    /**
     * Get the global number of timestamps put in cache
     */
	long getUpdateTimestampsCachePutCount();

	/**
     * Get the global number of flush operations executed (either manual or automatic).
     */
	long getFlushCount();

	/**
	 * Get the global number of connections asked by the sessions
     * (the actual number of connections used may be much smaller depending
     * whether you use a connection pool or not)
	 */
	long getConnectCount();

	/**
     * Global number of cacheable entities/collections successfully retrieved from the cache
     */
	long getSecondLevelCacheHitCount();

	/**
     * Global number of cacheable entities/collections not found in the cache and loaded from the database.
     */
	long getSecondLevelCacheMissCount();

	/**
	 * Global number of cacheable entities/collections put in the cache
	 */
	long getSecondLevelCachePutCount();

	/**
	 * Global number of sessions closed
	 */
	long getSessionCloseCount();

	/**
	 * Global number of sessions opened
	 */
	long getSessionOpenCount();

	/**
	 * Global number of collections loaded
	 */
	long getCollectionLoadCount();

	/**
	 * Global number of collections fetched
	 */
	long getCollectionFetchCount();

	/**
	 * Global number of collections updated
	 */
	long getCollectionUpdateCount();

	/**
	 * Global number of collections removed
	 */
    //even on inverse="true"
	long getCollectionRemoveCount();

	/**
	 * Global number of collections recreated
	 */
	long getCollectionRecreateCount();

	/**
	 * The milliseconds (JVM standard {@link System#currentTimeMillis()})
	 * since the initial creation of this Statistics
	 * instance or the last time {@link #clear()} was called.
	 *
	 * @apiNote This time(stamp) is
	 */
	long getStartTime();

	/**
	 * Get all executed query strings.
	 *
	 * The maximum number of queries tracked by the Hibernate statistics is given by the {@code hibernate.statistics.query_max_size} property.
	 */
	String[] getQueries();

	/**
	 * Get the names of all entities
	 */
	String[] getEntityNames();

	/**
	 * Get the names of all collection roles
	 */
	String[] getCollectionRoleNames();

	/**
	 * Get all second-level cache region names.  Note: for backwards
	 * compatibility this method returns just the names of regions
	 * storing domain data, not query result regions
	 */
	String[] getSecondLevelCacheRegionNames();

	/**
	 * The number of transactions we know to have been successful
	 */
	long getSuccessfulTransactionCount();

	/**
	 * The number of transactions we know to have completed
	 */
	long getTransactionCount();

	/**
	 * The number of prepared statements that were acquired
	 */
	long getPrepareStatementCount();

	/**
	 * The number of prepared statements that were released
	 */
	long getCloseStatementCount();

	/**
	 * The number of Hibernate <tt>StaleObjectStateException</tt>s or JPA <tt>OptimisticLockException</tt>s
	 * that occurred.
	 */
	long getOptimisticFailureCount();


	/**
	 * Second-level cache statistics per region
	 *
	 * @param regionName qualified region name
	 *
	 * @return SecondLevelCacheStatistics or {@code null} if the second level cache is not enabled
	 *
	 * @throws IllegalArgumentException if the region name could not be resolved
	 *
	 * @deprecated (since 5.3) Use {@link #getDomainDataRegionStatistics} instead
	 */
	@Deprecated
	SecondLevelCacheStatistics getSecondLevelCacheStatistics(String regionName);

	/**
	 * Natural id cache statistics per region
	 *
	 * @param regionName region name
	 * @return NaturalIdCacheStatistics
	 *
	 * @deprecated (since 5.3) Use {@link #getNaturalIdStatistics} or
	 * {@link #getDomainDataRegionStatistics} instead depending on need
	 */
	@Deprecated
	NaturalIdCacheStatistics getNaturalIdCacheStatistics(String regionName);

	/**
	 * Get the global number of query plans successfully retrieved from cache
	 */
	default long getQueryPlanCacheHitCount() {
		//For backward compatibility
		return 0;
	}

	/**
	 * Get the global number of query plans lookups *not* found in cache
	 */
	default long getQueryPlanCacheMissCount() {
		//For backward compatibility
		return 0;
	}
}
