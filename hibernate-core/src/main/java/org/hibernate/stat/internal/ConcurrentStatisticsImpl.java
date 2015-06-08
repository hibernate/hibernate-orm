/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.service.Service;
import org.hibernate.stat.CollectionStatistics;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.NaturalIdCacheStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Implementation of {@link org.hibernate.stat.Statistics} based on the {@link java.util.concurrent} package.
 *
 * @author Alex Snaps
 */
@SuppressWarnings({ "unchecked" })
public class ConcurrentStatisticsImpl implements StatisticsImplementor, Service {
	private static final CoreMessageLogger LOG = messageLogger( ConcurrentStatisticsImpl.class );

	private SessionFactoryImplementor sessionFactory;

	private volatile boolean isStatisticsEnabled;
	private volatile long startTime;
	private AtomicLong sessionOpenCount = new AtomicLong();
	private AtomicLong sessionCloseCount = new AtomicLong();
	private AtomicLong flushCount = new AtomicLong();
	private AtomicLong connectCount = new AtomicLong();

	private AtomicLong prepareStatementCount = new AtomicLong();
	private AtomicLong closeStatementCount = new AtomicLong();

	private AtomicLong entityLoadCount = new AtomicLong();
	private AtomicLong entityUpdateCount = new AtomicLong();
	private AtomicLong entityInsertCount = new AtomicLong();
	private AtomicLong entityDeleteCount = new AtomicLong();
	private AtomicLong entityFetchCount = new AtomicLong();
	private AtomicLong collectionLoadCount = new AtomicLong();
	private AtomicLong collectionUpdateCount = new AtomicLong();
	private AtomicLong collectionRemoveCount = new AtomicLong();
	private AtomicLong collectionRecreateCount = new AtomicLong();
	private AtomicLong collectionFetchCount = new AtomicLong();

	private AtomicLong secondLevelCacheHitCount = new AtomicLong();
	private AtomicLong secondLevelCacheMissCount = new AtomicLong();
	private AtomicLong secondLevelCachePutCount = new AtomicLong();
	
	private AtomicLong naturalIdCacheHitCount = new AtomicLong();
	private AtomicLong naturalIdCacheMissCount = new AtomicLong();
	private AtomicLong naturalIdCachePutCount = new AtomicLong();
	private AtomicLong naturalIdQueryExecutionCount = new AtomicLong();
	private AtomicLong naturalIdQueryExecutionMaxTime = new AtomicLong();
	private volatile String naturalIdQueryExecutionMaxTimeRegion;
	
	private AtomicLong queryExecutionCount = new AtomicLong();
	private AtomicLong queryExecutionMaxTime = new AtomicLong();
	private volatile String queryExecutionMaxTimeQueryString;
	private AtomicLong queryCacheHitCount = new AtomicLong();
	private AtomicLong queryCacheMissCount = new AtomicLong();
	private AtomicLong queryCachePutCount = new AtomicLong();

	private AtomicLong updateTimestampsCacheHitCount = new AtomicLong();
	private AtomicLong updateTimestampsCacheMissCount = new AtomicLong();
	private AtomicLong updateTimestampsCachePutCount = new AtomicLong();

	private AtomicLong committedTransactionCount = new AtomicLong();
	private AtomicLong transactionCount = new AtomicLong();

	private AtomicLong optimisticFailureCount = new AtomicLong();

	/**
	 * natural id cache statistics per region
	 */
	private final ConcurrentMap naturalIdCacheStatistics = new ConcurrentHashMap();
	/**
	 * second level cache statistics per region
	 */
	private final ConcurrentMap secondLevelCacheStatistics = new ConcurrentHashMap();
	/**
	 * entity statistics per name
	 */
	private final ConcurrentMap entityStatistics = new ConcurrentHashMap();
	/**
	 * collection statistics per name
	 */
	private final ConcurrentMap collectionStatistics = new ConcurrentHashMap();
	/**
	 * entity statistics per query string (HQL or SQL)
	 */
	private final ConcurrentMap queryStatistics = new ConcurrentHashMap();

	@SuppressWarnings({ "UnusedDeclaration" })
	public ConcurrentStatisticsImpl() {
		clear();
	}

	public ConcurrentStatisticsImpl(SessionFactoryImplementor sessionFactory) {
		clear();
		this.sessionFactory = sessionFactory;
	}

	/**
	 * reset all statistics
	 */
	public void clear() {
		secondLevelCacheHitCount.set( 0 );
		secondLevelCacheMissCount.set( 0 );
		secondLevelCachePutCount.set( 0 );
		
		naturalIdCacheHitCount.set( 0 );
		naturalIdCacheMissCount.set( 0 );
		naturalIdCachePutCount.set( 0 );
		naturalIdQueryExecutionCount.set( 0 );
		naturalIdQueryExecutionMaxTime.set( 0 );
		naturalIdQueryExecutionMaxTimeRegion = null;

		sessionCloseCount.set( 0 );
		sessionOpenCount.set( 0 );
		flushCount.set( 0 );
		connectCount.set( 0 );

		prepareStatementCount.set( 0 );
		closeStatementCount.set( 0 );

		entityDeleteCount.set( 0 );
		entityInsertCount.set( 0 );
		entityUpdateCount.set( 0 );
		entityLoadCount.set( 0 );
		entityFetchCount.set( 0 );

		collectionRemoveCount.set( 0 );
		collectionUpdateCount.set( 0 );
		collectionRecreateCount.set( 0 );
		collectionLoadCount.set( 0 );
		collectionFetchCount.set( 0 );

		queryExecutionCount.set( 0 );
		queryCacheHitCount.set( 0 );
		queryExecutionMaxTime.set( 0 );
		queryExecutionMaxTimeQueryString = null;
		queryCacheMissCount.set( 0 );
		queryCachePutCount.set( 0 );

		updateTimestampsCacheMissCount.set( 0 );
		updateTimestampsCacheHitCount.set( 0 );
		updateTimestampsCachePutCount.set( 0 );

		transactionCount.set( 0 );
		committedTransactionCount.set( 0 );

		optimisticFailureCount.set( 0 );

		secondLevelCacheStatistics.clear();
		entityStatistics.clear();
		collectionStatistics.clear();
		queryStatistics.clear();
		naturalIdCacheStatistics.clear();

		startTime = System.currentTimeMillis();
	}

	public void openSession() {
		sessionOpenCount.getAndIncrement();
	}

	public void closeSession() {
		sessionCloseCount.getAndIncrement();
	}

	public void flush() {
		flushCount.getAndIncrement();
	}

	public void connect() {
		connectCount.getAndIncrement();
	}

	public void loadEntity(String entityName) {
		entityLoadCount.getAndIncrement();
		( (ConcurrentEntityStatisticsImpl) getEntityStatistics( entityName ) ).incrementLoadCount();
	}

	public void fetchEntity(String entityName) {
		entityFetchCount.getAndIncrement();
		( (ConcurrentEntityStatisticsImpl) getEntityStatistics( entityName ) ).incrementFetchCount();
	}

	/**
	 * find entity statistics per name
	 *
	 * @param entityName entity name
	 *
	 * @return EntityStatistics object
	 */
	public EntityStatistics getEntityStatistics(String entityName) {
		ConcurrentEntityStatisticsImpl es = (ConcurrentEntityStatisticsImpl) entityStatistics.get( entityName );
		if ( es == null ) {
			es = new ConcurrentEntityStatisticsImpl( entityName );
			ConcurrentEntityStatisticsImpl previous;
			if ( ( previous = (ConcurrentEntityStatisticsImpl) entityStatistics.putIfAbsent(
					entityName, es
			) ) != null ) {
				es = previous;
			}
		}
		return es;
	}

	public void updateEntity(String entityName) {
		entityUpdateCount.getAndIncrement();
		ConcurrentEntityStatisticsImpl es = (ConcurrentEntityStatisticsImpl) getEntityStatistics( entityName );
		es.incrementUpdateCount();
	}

	public void insertEntity(String entityName) {
		entityInsertCount.getAndIncrement();
		ConcurrentEntityStatisticsImpl es = (ConcurrentEntityStatisticsImpl) getEntityStatistics( entityName );
		es.incrementInsertCount();
	}

	public void deleteEntity(String entityName) {
		entityDeleteCount.getAndIncrement();
		ConcurrentEntityStatisticsImpl es = (ConcurrentEntityStatisticsImpl) getEntityStatistics( entityName );
		es.incrementDeleteCount();
	}

	/**
	 * Get collection statistics per role
	 *
	 * @param role collection role
	 *
	 * @return CollectionStatistics
	 */
	public CollectionStatistics getCollectionStatistics(String role) {
		ConcurrentCollectionStatisticsImpl cs = (ConcurrentCollectionStatisticsImpl) collectionStatistics.get( role );
		if ( cs == null ) {
			cs = new ConcurrentCollectionStatisticsImpl( role );
			ConcurrentCollectionStatisticsImpl previous;
			if ( ( previous = (ConcurrentCollectionStatisticsImpl) collectionStatistics.putIfAbsent(
					role, cs
			) ) != null ) {
				cs = previous;
			}
		}
		return cs;
	}

	public void loadCollection(String role) {
		collectionLoadCount.getAndIncrement();
		( (ConcurrentCollectionStatisticsImpl) getCollectionStatistics( role ) ).incrementLoadCount();
	}

	public void fetchCollection(String role) {
		collectionFetchCount.getAndIncrement();
		( (ConcurrentCollectionStatisticsImpl) getCollectionStatistics( role ) ).incrementFetchCount();
	}

	public void updateCollection(String role) {
		collectionUpdateCount.getAndIncrement();
		( (ConcurrentCollectionStatisticsImpl) getCollectionStatistics( role ) ).incrementUpdateCount();
	}

	public void recreateCollection(String role) {
		collectionRecreateCount.getAndIncrement();
		( (ConcurrentCollectionStatisticsImpl) getCollectionStatistics( role ) ).incrementRecreateCount();
	}

	public void removeCollection(String role) {
		collectionRemoveCount.getAndIncrement();
		( (ConcurrentCollectionStatisticsImpl) getCollectionStatistics( role ) ).incrementRemoveCount();
	}
	

	@Override
	public NaturalIdCacheStatistics getNaturalIdCacheStatistics(String regionName) {
		ConcurrentNaturalIdCacheStatisticsImpl nics =
				(ConcurrentNaturalIdCacheStatisticsImpl) naturalIdCacheStatistics.get( regionName );
		
		if ( nics == null ) {
			if ( sessionFactory == null ) {
				return null;
			}
			Region region = sessionFactory.getNaturalIdCacheRegion( regionName );
			if ( region == null ) {
				return null;
			}
			NaturalIdRegionAccessStrategy accessStrategy
					= (NaturalIdRegionAccessStrategy) sessionFactory.getNaturalIdCacheRegionAccessStrategy(regionName);

			nics = new ConcurrentNaturalIdCacheStatisticsImpl( region, accessStrategy );
			ConcurrentNaturalIdCacheStatisticsImpl previous;
			if ( ( previous = (ConcurrentNaturalIdCacheStatisticsImpl) naturalIdCacheStatistics.putIfAbsent(
					regionName, nics
			) ) != null ) {
				nics = previous;
			}
		}
		return nics;
	}

	/**
	 * Second level cache statistics per region
	 *
	 * @param regionName region name
	 *
	 * @return SecondLevelCacheStatistics
	 */
	public SecondLevelCacheStatistics getSecondLevelCacheStatistics(String regionName) {
		ConcurrentSecondLevelCacheStatisticsImpl slcs
				= (ConcurrentSecondLevelCacheStatisticsImpl) secondLevelCacheStatistics.get( regionName );
		if ( slcs == null ) {
			if ( sessionFactory == null ) {
				return null;
			}
			Region region = sessionFactory.getSecondLevelCacheRegion( regionName );
			if ( region == null ) {
				return null;
			}
			RegionAccessStrategy accessStrategy = sessionFactory.getSecondLevelCacheRegionAccessStrategy(regionName);

			EntityRegionAccessStrategy entityRegionAccessStrategy
					= accessStrategy instanceof EntityRegionAccessStrategy ?
					(EntityRegionAccessStrategy) accessStrategy : null;
			CollectionRegionAccessStrategy collectionRegionAccessStrategy
					= accessStrategy instanceof CollectionRegionAccessStrategy ?
					(CollectionRegionAccessStrategy) accessStrategy : null;

			slcs = new ConcurrentSecondLevelCacheStatisticsImpl( region, entityRegionAccessStrategy, collectionRegionAccessStrategy );
			ConcurrentSecondLevelCacheStatisticsImpl previous;
			if ( ( previous = (ConcurrentSecondLevelCacheStatisticsImpl) secondLevelCacheStatistics.putIfAbsent(
					regionName, slcs
			) ) != null ) {
				slcs = previous;
			}
		}
		return slcs;
	}

	public void secondLevelCachePut(String regionName) {
		secondLevelCachePutCount.getAndIncrement();
		( (ConcurrentSecondLevelCacheStatisticsImpl) getSecondLevelCacheStatistics( regionName ) ).incrementPutCount();
	}

	public void secondLevelCacheHit(String regionName) {
		secondLevelCacheHitCount.getAndIncrement();
		( (ConcurrentSecondLevelCacheStatisticsImpl) getSecondLevelCacheStatistics( regionName ) ).incrementHitCount();
	}

	public void secondLevelCacheMiss(String regionName) {
		secondLevelCacheMissCount.getAndIncrement();
		( (ConcurrentSecondLevelCacheStatisticsImpl) getSecondLevelCacheStatistics( regionName ) ).incrementMissCount();
	}
	
	@Override
	public void naturalIdCachePut(String regionName) {
		naturalIdCachePutCount.getAndIncrement();
		( (ConcurrentNaturalIdCacheStatisticsImpl) getNaturalIdCacheStatistics( regionName ) ).incrementPutCount();
	}

	@Override
	public void naturalIdCacheHit(String regionName) {
		naturalIdCacheHitCount.getAndIncrement();
		( (ConcurrentNaturalIdCacheStatisticsImpl) getNaturalIdCacheStatistics( regionName ) ).incrementHitCount();
	}

	@Override
	public void naturalIdCacheMiss(String regionName) {
		naturalIdCacheMissCount.getAndIncrement();
		( (ConcurrentNaturalIdCacheStatisticsImpl) getNaturalIdCacheStatistics( regionName ) ).incrementMissCount();
	}
	
	@Override
	public void naturalIdQueryExecuted(String regionName, long time) {
		naturalIdQueryExecutionCount.getAndIncrement();
		boolean isLongestQuery;
		//noinspection StatementWithEmptyBody
		for ( long old = naturalIdQueryExecutionMaxTime.get();
				( isLongestQuery = time > old ) && ( !naturalIdQueryExecutionMaxTime.compareAndSet( old, time ) );
				old = naturalIdQueryExecutionMaxTime.get() ) {
			// nothing to do here given the odd loop structure...
		}
		if ( isLongestQuery && regionName != null ) {
			naturalIdQueryExecutionMaxTimeRegion = regionName;
		}
		if ( regionName != null ) {
			( (ConcurrentNaturalIdCacheStatisticsImpl) getNaturalIdCacheStatistics( regionName ) ).queryExecuted( time );
		}
	}

	@Override
	public void queryExecuted(String hql, int rows, long time) {
		LOG.hql(hql, time, (long) rows );
		queryExecutionCount.getAndIncrement();
		boolean isLongestQuery;
		//noinspection StatementWithEmptyBody
		for ( long old = queryExecutionMaxTime.get();
				( isLongestQuery = time > old ) && ( !queryExecutionMaxTime.compareAndSet( old, time ) );
				old = queryExecutionMaxTime.get() ) {
			// nothing to do here given the odd loop structure...
		}
		if ( isLongestQuery ) {
			queryExecutionMaxTimeQueryString = hql;
		}
		if ( hql != null ) {
			ConcurrentQueryStatisticsImpl qs = (ConcurrentQueryStatisticsImpl) getQueryStatistics( hql );
			qs.executed( rows, time );
		}
	}
	@Override
	public void queryCacheHit(String hql, String regionName) {
		queryCacheHitCount.getAndIncrement();
		if ( hql != null ) {
			ConcurrentQueryStatisticsImpl qs = (ConcurrentQueryStatisticsImpl) getQueryStatistics( hql );
			qs.incrementCacheHitCount();
		}
		ConcurrentSecondLevelCacheStatisticsImpl slcs = (ConcurrentSecondLevelCacheStatisticsImpl) getSecondLevelCacheStatistics(
				regionName
		);
		slcs.incrementHitCount();
	}
	@Override
	public void queryCacheMiss(String hql, String regionName) {
		queryCacheMissCount.getAndIncrement();
		if ( hql != null ) {
			ConcurrentQueryStatisticsImpl qs = (ConcurrentQueryStatisticsImpl) getQueryStatistics( hql );
			qs.incrementCacheMissCount();
		}
		ConcurrentSecondLevelCacheStatisticsImpl slcs = (ConcurrentSecondLevelCacheStatisticsImpl) getSecondLevelCacheStatistics(
				regionName
		);
		slcs.incrementMissCount();
	}
	@Override
	public void queryCachePut(String hql, String regionName) {
		queryCachePutCount.getAndIncrement();
		if ( hql != null ) {
			ConcurrentQueryStatisticsImpl qs = (ConcurrentQueryStatisticsImpl) getQueryStatistics( hql );
			qs.incrementCachePutCount();
		}
		ConcurrentSecondLevelCacheStatisticsImpl slcs = (ConcurrentSecondLevelCacheStatisticsImpl) getSecondLevelCacheStatistics(
				regionName
		);
		slcs.incrementPutCount();
	}

	@Override
	public void updateTimestampsCacheHit() {
		updateTimestampsCacheHitCount.getAndIncrement();
	}

	@Override
	public void updateTimestampsCacheMiss() {
		updateTimestampsCacheMissCount.getAndIncrement();
	}

	@Override
	public void updateTimestampsCachePut() {
		updateTimestampsCachePutCount.getAndIncrement();
	}

	/**
	 * Query statistics from query string (HQL or SQL)
	 *
	 * @param queryString query string
	 *
	 * @return QueryStatistics
	 */
	@Override
	public QueryStatistics getQueryStatistics(String queryString) {
		ConcurrentQueryStatisticsImpl qs = (ConcurrentQueryStatisticsImpl) queryStatistics.get( queryString );
		if ( qs == null ) {
			qs = new ConcurrentQueryStatisticsImpl( queryString );
			ConcurrentQueryStatisticsImpl previous;
			if ( ( previous = (ConcurrentQueryStatisticsImpl) queryStatistics.putIfAbsent(
					queryString, qs
			) ) != null ) {
				qs = previous;
			}
		}
		return qs;
	}

	/**
	 * @return entity deletion count
	 */
	@Override
	public long getEntityDeleteCount() {
		return entityDeleteCount.get();
	}

	/**
	 * @return entity insertion count
	 */
	@Override
	public long getEntityInsertCount() {
		return entityInsertCount.get();
	}

	/**
	 * @return entity load (from DB)
	 */
	@Override
	public long getEntityLoadCount() {
		return entityLoadCount.get();
	}

	/**
	 * @return entity fetch (from DB)
	 */
	@Override
	public long getEntityFetchCount() {
		return entityFetchCount.get();
	}

	/**
	 * @return entity update
	 */
	@Override
	public long getEntityUpdateCount() {
		return entityUpdateCount.get();
	}
	@Override
	public long getQueryExecutionCount() {
		return queryExecutionCount.get();
	}
	@Override
	public long getQueryCacheHitCount() {
		return queryCacheHitCount.get();
	}
	@Override
	public long getQueryCacheMissCount() {
		return queryCacheMissCount.get();
	}
	@Override
	public long getQueryCachePutCount() {
		return queryCachePutCount.get();
	}
	@Override
	public long getUpdateTimestampsCacheHitCount() {
		return updateTimestampsCacheHitCount.get();
	}
	@Override
	public long getUpdateTimestampsCacheMissCount() {
		return updateTimestampsCacheMissCount.get();
	}
	@Override
	public long getUpdateTimestampsCachePutCount() {
		return updateTimestampsCachePutCount.get();
	}

	/**
	 * @return flush
	 */
	@Override
	public long getFlushCount() {
		return flushCount.get();
	}

	/**
	 * @return session connect
	 */
	@Override
	public long getConnectCount() {
		return connectCount.get();
	}

	/**
	 * @return second level cache hit
	 */
	@Override
	public long getSecondLevelCacheHitCount() {
		return secondLevelCacheHitCount.get();
	}

	/**
	 * @return second level cache miss
	 */
	@Override
	public long getSecondLevelCacheMissCount() {
		return secondLevelCacheMissCount.get();
	}

	/**
	 * @return second level cache put
	 */
	@Override
	public long getSecondLevelCachePutCount() {
		return secondLevelCachePutCount.get();
	}

	@Override
	public long getNaturalIdQueryExecutionCount() {
		return naturalIdQueryExecutionCount.get();
	}

	@Override
	public long getNaturalIdQueryExecutionMaxTime() {
		return naturalIdQueryExecutionMaxTime.get();
	}
	
	@Override
	public String getNaturalIdQueryExecutionMaxTimeRegion() {
		return naturalIdQueryExecutionMaxTimeRegion;
	}
	
	@Override
	public long getNaturalIdCacheHitCount() {
		return naturalIdCacheHitCount.get();
	}

	@Override
	public long getNaturalIdCacheMissCount() {
		return naturalIdCacheMissCount.get();
	}

	@Override
	public long getNaturalIdCachePutCount() {
		return naturalIdCachePutCount.get();
	}

	/**
	 * @return session closing
	 */
	@Override
	public long getSessionCloseCount() {
		return sessionCloseCount.get();
	}

	/**
	 * @return session opening
	 */
	@Override
	public long getSessionOpenCount() {
		return sessionOpenCount.get();
	}

	/**
	 * @return collection loading (from DB)
	 */
	@Override
	public long getCollectionLoadCount() {
		return collectionLoadCount.get();
	}

	/**
	 * @return collection fetching (from DB)
	 */
	@Override
	public long getCollectionFetchCount() {
		return collectionFetchCount.get();
	}

	/**
	 * @return collection update
	 */
	@Override
	public long getCollectionUpdateCount() {
		return collectionUpdateCount.get();
	}

	/**
	 * @return collection removal
	 *         FIXME: even if isInverse="true"?
	 */
	@Override
	public long getCollectionRemoveCount() {
		return collectionRemoveCount.get();
	}

	/**
	 * @return collection recreation
	 */
	@Override
	public long getCollectionRecreateCount() {
		return collectionRecreateCount.get();
	}

	/**
	 * @return start time in ms (JVM standards {@link System#currentTimeMillis()})
	 */
	@Override
	public long getStartTime() {
		return startTime;
	}

	/**
	 * log in info level the main statistics
	 */
	@Override
	public void logSummary() {
		LOG.loggingStatistics();
		LOG.startTime( startTime );
		LOG.sessionsOpened( sessionOpenCount.get() );
		LOG.sessionsClosed( sessionCloseCount.get() );
		LOG.transactions( transactionCount.get() );
		LOG.successfulTransactions( committedTransactionCount.get() );
		LOG.optimisticLockFailures( optimisticFailureCount.get() );
		LOG.flushes( flushCount.get() );
		LOG.connectionsObtained( connectCount.get() );
		LOG.statementsPrepared( prepareStatementCount.get() );
		LOG.statementsClosed( closeStatementCount.get() );
		LOG.secondLevelCachePuts( secondLevelCachePutCount.get() );
		LOG.secondLevelCacheHits( secondLevelCacheHitCount.get() );
		LOG.secondLevelCacheMisses( secondLevelCacheMissCount.get() );
		LOG.entitiesLoaded( entityLoadCount.get() );
		LOG.entitiesUpdated( entityUpdateCount.get() );
		LOG.entitiesInserted( entityInsertCount.get() );
		LOG.entitiesDeleted( entityDeleteCount.get() );
		LOG.entitiesFetched( entityFetchCount.get() );
		LOG.collectionsLoaded( collectionLoadCount.get() );
		LOG.collectionsUpdated( collectionUpdateCount.get() );
		LOG.collectionsRemoved( collectionRemoveCount.get() );
		LOG.collectionsRecreated( collectionRecreateCount.get() );
		LOG.collectionsFetched( collectionFetchCount.get() );
		LOG.naturalIdCachePuts( naturalIdCachePutCount.get() );
		LOG.naturalIdCacheHits( naturalIdCacheHitCount.get() );
		LOG.naturalIdCacheMisses( naturalIdCacheMissCount.get() );
		LOG.naturalIdMaxQueryTime( naturalIdQueryExecutionMaxTime.get() );
		LOG.naturalIdQueriesExecuted( naturalIdQueryExecutionCount.get() );
		LOG.queriesExecuted( queryExecutionCount.get() );
		LOG.queryCachePuts( queryCachePutCount.get() );
		LOG.timestampCachePuts( updateTimestampsCachePutCount.get() );
		LOG.timestampCacheHits( updateTimestampsCacheHitCount.get() );
		LOG.timestampCacheMisses( updateTimestampsCacheMissCount.get() );
		LOG.queryCacheHits( queryCacheHitCount.get() );
		LOG.queryCacheMisses( queryCacheMissCount.get() );
		LOG.maxQueryTime( queryExecutionMaxTime.get() );
	}

	/**
	 * Are statistics logged
	 */
	@Override
	public boolean isStatisticsEnabled() {
		return isStatisticsEnabled;
	}

	/**
	 * Enable statistics logs (this is a dynamic parameter)
	 */
	@Override
	public void setStatisticsEnabled(boolean b) {
		isStatisticsEnabled = b;
	}

	/**
	 * @return Returns the max query execution time,
	 *         for all queries
	 */
	@Override
	public long getQueryExecutionMaxTime() {
		return queryExecutionMaxTime.get();
	}

	/**
	 * Get all executed query strings
	 */
	@Override
	public String[] getQueries() {
		return ArrayHelper.toStringArray( queryStatistics.keySet() );
	}

	/**
	 * Get the names of all entities
	 */
	@Override
	public String[] getEntityNames() {
		if ( sessionFactory == null ) {
			return ArrayHelper.toStringArray( entityStatistics.keySet() );
		}
		else {
			return ArrayHelper.toStringArray( sessionFactory.getAllClassMetadata().keySet() );
		}
	}

	/**
	 * Get the names of all collection roles
	 */
	@Override
	public String[] getCollectionRoleNames() {
		if ( sessionFactory == null ) {
			return ArrayHelper.toStringArray( collectionStatistics.keySet() );
		}
		else {
			return ArrayHelper.toStringArray( sessionFactory.getAllCollectionMetadata().keySet() );
		}
	}

	/**
	 * Get all second-level cache region names
	 */
	@Override
	public String[] getSecondLevelCacheRegionNames() {
		if ( sessionFactory == null ) {
			return ArrayHelper.toStringArray( secondLevelCacheStatistics.keySet() );
		}
		else {
			return ArrayHelper.toStringArray( sessionFactory.getAllSecondLevelCacheRegions().keySet() );
		}
	}
	@Override
	public void endTransaction(boolean success) {
		transactionCount.getAndIncrement();
		if ( success ) {
			committedTransactionCount.getAndIncrement();
		}
	}
	@Override
	public long getSuccessfulTransactionCount() {
		return committedTransactionCount.get();
	}
	@Override
	public long getTransactionCount() {
		return transactionCount.get();
	}
	@Override
	public void closeStatement() {
		closeStatementCount.getAndIncrement();
	}
	@Override
	public void prepareStatement() {
		prepareStatementCount.getAndIncrement();
	}
	@Override
	public long getCloseStatementCount() {
		return closeStatementCount.get();
	}
	@Override
	public long getPrepareStatementCount() {
		return prepareStatementCount.get();
	}
	@Override
	public void optimisticFailure(String entityName) {
		optimisticFailureCount.getAndIncrement();
		( (ConcurrentEntityStatisticsImpl) getEntityStatistics( entityName ) ).incrementOptimisticFailureCount();
	}
	@Override
	public long getOptimisticFailureCount() {
		return optimisticFailureCount.get();
	}

	@Override
	public String toString() {
		return new StringBuilder()
				.append( "Statistics[" )
				.append( "start time=" ).append( startTime )
				.append( ",sessions opened=" ).append( sessionOpenCount )
				.append( ",sessions closed=" ).append( sessionCloseCount )
				.append( ",transactions=" ).append( transactionCount )
				.append( ",successful transactions=" ).append( committedTransactionCount )
				.append( ",optimistic lock failures=" ).append( optimisticFailureCount )
				.append( ",flushes=" ).append( flushCount )
				.append( ",connections obtained=" ).append( connectCount )
				.append( ",statements prepared=" ).append( prepareStatementCount )
				.append( ",statements closed=" ).append( closeStatementCount )
				.append( ",second level cache puts=" ).append( secondLevelCachePutCount )
				.append( ",second level cache hits=" ).append( secondLevelCacheHitCount )
				.append( ",second level cache misses=" ).append( secondLevelCacheMissCount )
				.append( ",entities loaded=" ).append( entityLoadCount )
				.append( ",entities updated=" ).append( entityUpdateCount )
				.append( ",entities inserted=" ).append( entityInsertCount )
				.append( ",entities deleted=" ).append( entityDeleteCount )
				.append( ",entities fetched=" ).append( entityFetchCount )
				.append( ",collections loaded=" ).append( collectionLoadCount )
				.append( ",collections updated=" ).append( collectionUpdateCount )
				.append( ",collections removed=" ).append( collectionRemoveCount )
				.append( ",collections recreated=" ).append( collectionRecreateCount )
				.append( ",collections fetched=" ).append( collectionFetchCount )
				.append( ",naturalId queries executed to database=" ).append( naturalIdQueryExecutionCount )
				.append( ",naturalId cache puts=" ).append( naturalIdCachePutCount )
				.append( ",naturalId cache hits=" ).append( naturalIdCacheHitCount )
				.append( ",naturalId cache misses=" ).append( naturalIdCacheMissCount )
				.append( ",naturalId max query time=" ).append( naturalIdQueryExecutionMaxTime )
				.append( ",queries executed to database=" ).append( queryExecutionCount )
				.append( ",query cache puts=" ).append( queryCachePutCount )
				.append( ",query cache hits=" ).append( queryCacheHitCount )
				.append( ",query cache misses=" ).append( queryCacheMissCount )
				.append(",update timestamps cache puts=").append(updateTimestampsCachePutCount)
				.append(",update timestamps cache hits=").append(updateTimestampsCacheHitCount)
				.append(",update timestamps cache misses=").append(updateTimestampsCacheMissCount)
				.append( ",max query time=" ).append( queryExecutionMaxTime )
				.append( ']' )
				.toString();
	}
	@Override
	public String getQueryExecutionMaxTimeQueryString() {
		return queryExecutionMaxTimeQueryString;
	}
}
