/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.spi;

import org.hibernate.service.Service;
import org.hibernate.stat.Statistics;

/**
 * Statistics SPI for the Hibernate core.  This is essentially the "statistic collector" API, its the contract
 * called to collect various stats.
 * 
 * @author Emmanuel Bernard
 */
public interface StatisticsImplementor extends Statistics, Service {
	/**
	 * Callback about a session being opened.
	 */
	void openSession();

	/**
	 * Callback about a session being closed.
	 */
	void closeSession();

	/**
	 * Callback about a flush occurring
	 */
	void flush();

	/**
	 * Callback about a connection being obtained from {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}
	 */
	void connect();

	/**
	 * Callback about a statement being prepared.
	 */
	void prepareStatement();

	/**
	 * Callback about a statement being closed.
	 */
	void closeStatement();

	/**
	 * Callback about a transaction completing.
	 *
	 * @param success Was the transaction successful?
	 */
	void endTransaction(boolean success);

	/**
	 * Callback about an entity being loaded.  This might indicate a proxy or a fully initialized entity, but in either
	 * case it means without a separate SQL query being needed.
	 *
	 * @param entityName The name of the entity loaded.
	 */
	void loadEntity(String entityName);

	/**
	 * Callback about an entity being fetched.  Unlike {@link #loadEntity} this indicates a separate query being
	 * performed.
	 *
	 * @param entityName The name of the entity fetched.
	 */
	void fetchEntity(String entityName);

	/**
	 * Callback about an entity being updated.
	 *
	 * @param entityName The name of the entity updated.
	 */
	void updateEntity(String entityName);

	/**
	 * Callback about an entity being inserted
	 *
	 * @param entityName The name of the entity inserted
	 */
	void insertEntity(String entityName);

	/**
	 * Callback about an entity being deleted.
	 *
	 * @param entityName The name of the entity deleted.
	 */
	void deleteEntity(String entityName);

	/**
	 * Callback about an optimistic lock failure on an entity
	 *
	 * @param entityName The name of the entity.
	 */
	void optimisticFailure(String entityName);

	/**
	 * Callback about a collection loading.  This might indicate a lazy collection or an initialized collection being
	 * created, but in either case it means without a separate SQL query being needed.
	 *
	 * @param role The collection role.
	 */
	void loadCollection(String role);

	/**
	 * Callback to indicate a collection being fetched.  Unlike {@link #loadCollection}, this indicates a separate
	 * query was needed.
	 *
	 * @param role The collection role.
	 */
	void fetchCollection(String role);

	/**
	 * Callback indicating a collection was updated.
	 *
	 * @param role The collection role.
	 */
	void updateCollection(String role);

	/**
	 * Callback indicating a collection recreation (full deletion + full (re-)insertion).
	 *
	 * @param role The collection role.
	 */
	void recreateCollection(String role);

	/**
	 * Callback indicating a collection removal.
	 *
	 * @param role The collection role.
	 */
	void removeCollection(String role);

	/**
	 * Callback indicating a put into second level cache.
	 *
	 * @param regionName The name of the cache region
	 */
	void secondLevelCachePut(String regionName);

	/**
	 * Callback indicating a get from second level cache resulted in a hit.
	 *
	 * @param regionName The name of the cache region
	 */
	void secondLevelCacheHit(String regionName);

	/**
	 * Callback indicating a get from second level cache resulted in a miss.
	 *
	 * @param regionName The name of the cache region
	 */
	void secondLevelCacheMiss(String regionName);
	
	/**
	 * Callback indicating a put into natural id cache.
	 *
	 * @param regionName The name of the cache region
	 */
	void naturalIdCachePut(String regionName);
	
	/**
	 * Callback indicating a get from natural id cache resulted in a hit.
	 *
	 * @param regionName The name of the cache region
	 */
	void naturalIdCacheHit(String regionName);
	
	/**
	 * Callback indicating a get from natural id cache resulted in a miss.
	 *
	 * @param regionName The name of the cache region
	 */
	void naturalIdCacheMiss(String regionName);

	/**
	 * Callback indicating execution of a natural id query
	 *
	 * @param regionName The name of the cache region
	 * @param time execution time
	 */
	void naturalIdQueryExecuted(String regionName, long time);

	/**
	 * Callback indicating a put into the query cache.
	 *
	 * @param hql The query
	 * @param regionName The cache region
	 */
	void queryCachePut(String hql, String regionName);

	/**
	 * Callback indicating a get from the query cache resulted in a hit.
	 *
	 * @param hql The query
	 * @param regionName The name of the cache region
	 */
	void queryCacheHit(String hql, String regionName);

	/**
	 * Callback indicating a get from the query cache resulted in a miss.
	 *
	 * @param hql The query
	 * @param regionName The name of the cache region
	 */
	void queryCacheMiss(String hql, String regionName);

	/**
	 * Callback indicating execution of a sql/hql query
	 *
	 * @param hql The query
	 * @param rows Number of rows returned
	 * @param time execution time
	 */
	void queryExecuted(String hql, int rows, long time);


	/**
	 * Callback indicating a hit to the timestamp cache
	 */
	void updateTimestampsCacheHit();

	/**
	 * Callback indicating a miss to the timestamp cache
	 */
	void updateTimestampsCacheMiss();

	/**
	 * Callback indicating a put to the timestamp cache
	 */
	void updateTimestampsCachePut();
}
