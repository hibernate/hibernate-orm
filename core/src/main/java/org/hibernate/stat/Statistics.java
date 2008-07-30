/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.stat;


/**
 * Statistics for a particular <tt>SessionFactory</tt>.
 * Beware of milliseconds metrics, they are depdendent of the JVM precision:
 * you may then encounter a 10 ms approximation dending on you OS platform.
 * Please refer to the JVM documentation for more information.
 * 
 * @author Emmanuel Bernard
 */
public interface Statistics {
	/**
	 * reset all statistics
	 */
	public void clear();

    /**
	 * find entity statistics per name
	 * 
	 * @param entityName entity name
	 * @return EntityStatistics object
	 */
	public EntityStatistics getEntityStatistics(String entityName);
	/**
	 * Get collection statistics per role
	 * 
	 * @param role collection role
	 * @return CollectionStatistics
	 */
	public CollectionStatistics getCollectionStatistics(String role);

    /**
	 * Second level cache statistics per region
	 * 
	 * @param regionName region name
	 * @return SecondLevelCacheStatistics
	 */
	public SecondLevelCacheStatistics getSecondLevelCacheStatistics(String regionName);

    /**
	 * Query statistics from query string (HQL or SQL)
	 * 
	 * @param queryString query string
	 * @return QueryStatistics
	 */
	public QueryStatistics getQueryStatistics(String queryString);

    /**
     * Get global number of entity deletes
	 * @return entity deletion count
	 */
	public long getEntityDeleteCount();

    /**
     * Get global number of entity inserts
	 * @return entity insertion count
	 */
	public long getEntityInsertCount();

    /**
     * Get global number of entity loads
	 * @return entity load (from DB)
	 */
	public long getEntityLoadCount();
	/**
     * Get global number of entity fetchs
	 * @return entity fetch (from DB)
	 */
	public long getEntityFetchCount();

	/**
     * Get global number of entity updates
	 * @return entity update
	 */
	public long getEntityUpdateCount();

    /**
     * Get global number of executed queries
	 * @return query execution count
	 */
	public long getQueryExecutionCount();

    /**
     * Get the time in milliseconds of the slowest query.
     */
	public long getQueryExecutionMaxTime();
	/**
	 * Get the query string for the slowest query.
	 */
	public String getQueryExecutionMaxTimeQueryString();

    /**
     * Get the global number of cached queries successfully retrieved from cache
     */
	public long getQueryCacheHitCount();
    /**
     * Get the global number of cached queries *not* found in cache
     */
	public long getQueryCacheMissCount();
    /**
     * Get the global number of cacheable queries put in cache
     */
	public long getQueryCachePutCount();
	/**
     * Get the global number of flush executed by sessions (either implicit or explicit)
     */
	public long getFlushCount();
	/**
	 * Get the global number of connections asked by the sessions
     * (the actual number of connections used may be much smaller depending
     * whether you use a connection pool or not)
	 */
	public long getConnectCount();
	/**
     * Global number of cacheable entities/collections successfully retrieved from the cache
     */
	public long getSecondLevelCacheHitCount();
	/**
     * Global number of cacheable entities/collections not found in the cache and loaded from the database.
     */
	public long getSecondLevelCacheMissCount();
	/**
	 * Global number of cacheable entities/collections put in the cache
	 */
	public long getSecondLevelCachePutCount();
	/**
	 * Global number of sessions closed
	 */
	public long getSessionCloseCount();
	/**
	 * Global number of sessions opened
	 */
	public long getSessionOpenCount();
	/**
	 * Global number of collections loaded
	 */
	public long getCollectionLoadCount();
	/**
	 * Global number of collections fetched
	 */
	public long getCollectionFetchCount();
	/**
	 * Global number of collections updated
	 */
	public long getCollectionUpdateCount();
	/**
	 * Global number of collections removed
	 */
    //even on inverse="true"
	public long getCollectionRemoveCount();
	/**
	 * Global number of collections recreated
	 */
	public long getCollectionRecreateCount();
	/**
	 * @return start time in ms (JVM standards {@link System#currentTimeMillis()})
	 */
	public long getStartTime();
	/**
	 * log in info level the main statistics
	 */
	public void logSummary();
	/**
	 * Are statistics logged
	 */
	public boolean isStatisticsEnabled();
	/**
	 * Enable statistics logs (this is a dynamic parameter)
	 */
	public void setStatisticsEnabled(boolean b);

	/**
	 * Get all executed query strings
	 */
	public String[] getQueries();
	/**
	 * Get the names of all entities
	 */
	public String[] getEntityNames();
	/**
	 * Get the names of all collection roles
	 */
	public String[] getCollectionRoleNames();
	/**
	 * Get all second-level cache region names
	 */
	public String[] getSecondLevelCacheRegionNames();
	/**
	 * The number of transactions we know to have been successful
	 */
	public long getSuccessfulTransactionCount();
	/**
	 * The number of transactions we know to have completed
	 */
	public long getTransactionCount();
	/**
	 * The number of prepared statements that were acquired
	 */
	public long getPrepareStatementCount();
	/**
	 * The number of prepared statements that were released
	 */
	public long getCloseStatementCount();
	/**
	 * The number of <tt>StaleObjectStateException</tt>s 
	 * that occurred
	 */
	public long getOptimisticFailureCount();
}