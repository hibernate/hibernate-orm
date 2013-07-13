/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
	public void openSession();

	/**
	 * Callback about a session being closed.
	 */
	public void closeSession();

	/**
	 * Callback about a flush occurring
	 */
	public void flush();

	/**
	 * Callback about a connection being obtained from {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}
	 */
	public void connect();

	/**
	 * Callback about a statement being prepared.
	 */
	public void prepareStatement();

	/**
	 * Callback about a statement being closed.
	 */
	public void closeStatement();

	/**
	 * Callback about a transaction completing.
	 *
	 * @param success Was the transaction successful?
	 */
	public void endTransaction(boolean success);

	/**
	 * Callback about an entity being loaded.  This might indicate a proxy or a fully initialized entity, but in either
	 * case it means without a separate SQL query being needed.
	 *
	 * @param entityName The name of the entity loaded.
	 */
	public void loadEntity(String entityName);

	/**
	 * Callback about an entity being fetched.  Unlike {@link #loadEntity} this indicates a separate query being
	 * performed.
	 *
	 * @param entityName The name of the entity fetched.
	 */
	public void fetchEntity(String entityName);

	/**
	 * Callback about an entity being updated.
	 *
	 * @param entityName The name of the entity updated.
	 */
	public void updateEntity(String entityName);

	/**
	 * Callback about an entity being inserted
	 *
	 * @param entityName The name of the entity inserted
	 */
	public void insertEntity(String entityName);

	/**
	 * Callback about an entity being deleted.
	 *
	 * @param entityName The name of the entity deleted.
	 */
	public void deleteEntity(String entityName);

	/**
	 * Callback about an optimistic lock failure on an entity
	 *
	 * @param entityName The name of the entity.
	 */
	public void optimisticFailure(String entityName);

	/**
	 * Callback about a collection loading.  This might indicate a lazy collection or an initialized collection being
	 * created, but in either case it means without a separate SQL query being needed.
	 *
	 * @param role The collection role.
	 */
	public void loadCollection(String role);

	/**
	 * Callback to indicate a collection being fetched.  Unlike {@link #loadCollection}, this indicates a separate
	 * query was needed.
	 *
	 * @param role The collection role.
	 */
	public void fetchCollection(String role);

	/**
	 * Callback indicating a collection was updated.
	 *
	 * @param role The collection role.
	 */
	public void updateCollection(String role);

	/**
	 * Callback indicating a collection recreation (full deletion + full (re-)insertion).
	 *
	 * @param role The collection role.
	 */
	public void recreateCollection(String role);

	/**
	 * Callback indicating a collection removal.
	 *
	 * @param role The collection role.
	 */
	public void removeCollection(String role);

	/**
	 * Callback indicating a put into second level cache.
	 *
	 * @param regionName The name of the cache region
	 */
	public void secondLevelCachePut(String regionName);

	/**
	 * Callback indicating a get from second level cache resulted in a hit.
	 *
	 * @param regionName The name of the cache region
	 */
	public void secondLevelCacheHit(String regionName);

	/**
	 * Callback indicating a get from second level cache resulted in a miss.
	 *
	 * @param regionName The name of the cache region
	 */
	public void secondLevelCacheMiss(String regionName);
	
	/**
	 * Callback indicating a put into natural id cache.
	 *
	 * @param regionName The name of the cache region
	 */
	public void naturalIdCachePut(String regionName);
	
	/**
	 * Callback indicating a get from natural id cache resulted in a hit.
	 *
	 * @param regionName The name of the cache region
	 */
	public void naturalIdCacheHit(String regionName);
	
	/**
	 * Callback indicating a get from natural id cache resulted in a miss.
	 *
	 * @param regionName The name of the cache region
	 */
	public void naturalIdCacheMiss(String regionName);

	/**
	 * Callback indicating execution of a natural id query
	 *
	 * @param regionName The name of the cache region
	 * @param time execution time
	 */
	public void naturalIdQueryExecuted(String regionName, long time);

	/**
	 * Callback indicating a put into the query cache.
	 *
	 * @param hql The query
	 * @param regionName The cache region
	 */
	public void queryCachePut(String hql, String regionName);

	/**
	 * Callback indicating a get from the query cache resulted in a hit.
	 *
	 * @param hql The query
	 * @param regionName The name of the cache region
	 */
	public void queryCacheHit(String hql, String regionName);

	/**
	 * Callback indicating a get from the query cache resulted in a miss.
	 *
	 * @param hql The query
	 * @param regionName The name of the cache region
	 */
	public void queryCacheMiss(String hql, String regionName);

	/**
	 * Callback indicating execution of a sql/hql query
	 *
	 * @param hql The query
	 * @param rows Number of rows returned
	 * @param time execution time
	 */
	public void queryExecuted(String hql, int rows, long time);


	/**
	 * Callback indicating a hit to the timestamp cache
	 */
	public void updateTimestampsCacheHit();

	/**
	 * Callback indicating a miss to the timestamp cache
	 */
	public void updateTimestampsCacheMiss();

	/**
	 * Callback indicating a put to the timestamp cache
	 */
	public void updateTimestampsCachePut();
}
