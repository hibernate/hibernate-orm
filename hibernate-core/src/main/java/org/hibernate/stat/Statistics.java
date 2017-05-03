/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

/**
 * Exposes statistics for a particular {@link org.hibernate.SessionFactory}.  Beware of milliseconds metrics, they
 * are dependent of the JVM precision: you may then encounter a 10 ms approximation depending on you OS platform.
 * Please refer to the JVM documentation for more information.
 * 
 * @author Emmanuel Bernard
 */
public interface Statistics {
	/**
	 * reset all statistics
	 */
	void clear();

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
	 * Second level cache statistics per region
	 *
	 * @param regionName region name
	 *
	 * @return SecondLevelCacheStatistics or {@code null} if the second level cache is not enabled
	 *
	 * @throws IllegalArgumentException if the region name could not be resolved
	 */
	SecondLevelCacheStatistics getSecondLevelCacheStatistics(String regionName);

    /**
	 * Natural id cache statistics per region
	 * 
	 * @param regionName region name
	 * @return NaturalIdCacheStatistics
	 */
	NaturalIdCacheStatistics getNaturalIdCacheStatistics(String regionName);

    /**
	 * Query statistics from query string (HQL or SQL)
	 * 
	 * @param queryString query string
	 * @return QueryStatistics
	 */
	QueryStatistics getQueryStatistics(String queryString);

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
     * Get global number of entity fetchs
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
	 * Get the global number of naturalId queries executed against the database
	 */
	long getNaturalIdQueryExecutionCount();

	/**
	 * Get the global maximum query time for naturalId queries executed against the database
	 */
	long getNaturalIdQueryExecutionMaxTime();

	/**
	 * Get the region for the maximum naturalId query time 
	 */
	String getNaturalIdQueryExecutionMaxTimeRegion();

    /**
     * Get the global number of cached naturalId lookups successfully retrieved from cache
     */
	long getNaturalIdCacheHitCount();

    /**
     * Get the global number of cached naturalId lookups *not* found in cache
     */
	long getNaturalIdCacheMissCount();

    /**
     * Get the global number of cacheable naturalId lookups put in cache
     */
	long getNaturalIdCachePutCount();

    /**
     * Get the global number of timestamps successfully retrieved from cache
     */
	long getUpdateTimestampsCacheHitCount();

    /**
     * Get the global number of tables for which no update timestamps was *not* found in cache
     */
	long getUpdateTimestampsCacheMissCount();

    /**
     * Get the global number of timestamps put in cache
     */
	long getUpdateTimestampsCachePutCount();

	/**
     * Get the global number of flush executed by sessions (either implicit or explicit)
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
	 * @return start time in ms (JVM standards {@link System#currentTimeMillis()})
	 */
	long getStartTime();

	/**
	 * log in info level the main statistics
	 */
	void logSummary();

	/**
	 * Are statistics logged
	 */
	boolean isStatisticsEnabled();

	/**
	 * Enable statistics logs (this is a dynamic parameter)
	 */
	void setStatisticsEnabled(boolean b);

	/**
	 * Get all executed query strings
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
	 * Get all second-level cache region names
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
	 * The number of <tt>StaleObjectStateException</tt>s 
	 * that occurred
	 */
	long getOptimisticFailureCount();
}
