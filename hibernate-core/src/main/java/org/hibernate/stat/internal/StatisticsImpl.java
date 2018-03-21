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

import org.hibernate.cache.spi.QueryResultRegionAccess;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.Service;
import org.hibernate.stat.spi.StatisticsImplementor;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Implementation of {@link org.hibernate.stat.Statistics} based on the {@link java.util.concurrent} package.
 *
 * @author Alex Snaps
 */
@SuppressWarnings({ "unchecked" })
public class StatisticsImpl implements StatisticsImplementor, Service {
	private static final CoreMessageLogger LOG = messageLogger( StatisticsImpl.class );

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
	private volatile String naturalIdQueryExecutionMaxTimeEntity;

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

	private final ConcurrentMap<String,EntityStatisticsImpl> entityStatsMap = new ConcurrentHashMap();
	private final ConcurrentMap<String,NaturalIdStatisticsImpl> naturalIdQueryStatsMap = new ConcurrentHashMap();
	private final ConcurrentMap<String,CollectionStatisticsImpl> collectionStatsMap = new ConcurrentHashMap();

	/**
	 * Keyed by query string
	 */
	private final ConcurrentMap<String, QueryStatisticsImpl> queryStatsMap = new ConcurrentHashMap();

	/**
	 * Keyed by region name
	 */
	private final ConcurrentMap<String,CacheRegionStatisticsImpl> l2CacheStatsMap = new ConcurrentHashMap<>();

	private final ConcurrentMap<String,DeprecatedNaturalIdCacheStatisticsImpl> deprecatedNaturalIdStatsMap = new ConcurrentHashMap();


	@SuppressWarnings({ "UnusedDeclaration" })
	public StatisticsImpl() {
		clear();
	}

	public StatisticsImpl(SessionFactoryImplementor sessionFactory) {
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
		naturalIdQueryExecutionMaxTimeEntity = null;

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

		entityStatsMap.clear();
		collectionStatsMap.clear();
		naturalIdQueryStatsMap.clear();
		l2CacheStatsMap.clear();
		queryStatsMap.clear();
		deprecatedNaturalIdStatsMap.clear();

		startTime = System.currentTimeMillis();
	}

	@Override
	public long getStartTime() {
		return startTime;
	}

	@Override
	public boolean isStatisticsEnabled() {
		return isStatisticsEnabled;
	}

	@Override
	public void setStatisticsEnabled(boolean b) {
		isStatisticsEnabled = b;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity stats

	@Override
	public String[] getEntityNames() {
		if ( sessionFactory == null ) {
			return ArrayHelper.toStringArray( entityStatsMap.keySet() );
		}
		else {
			return sessionFactory.getMetamodel().getAllEntityNames();
		}
	}

	@Override
	public EntityStatisticsImpl getEntityStatistics(String entityName) {
		if ( sessionFactory == null ) {
			return null;
		}

		return entityStatsMap.computeIfAbsent(
				entityName,
				s -> new EntityStatisticsImpl( sessionFactory.getMetamodel().entityPersister( entityName ) )
		);
	}

	@Override
	public long getEntityLoadCount() {
		return entityLoadCount.get();
	}

	@Override
	public long getEntityFetchCount() {
		return entityFetchCount.get();
	}

	@Override
	public long getEntityDeleteCount() {
		return entityDeleteCount.get();
	}

	@Override
	public long getEntityInsertCount() {
		return entityInsertCount.get();
	}

	@Override
	public long getEntityUpdateCount() {
		return entityUpdateCount.get();
	}

	@Override
	public long getOptimisticFailureCount() {
		return optimisticFailureCount.get();
	}

	@Override
	public void loadEntity(String entityName) {
		entityLoadCount.getAndIncrement();
		getEntityStatistics( entityName ).incrementLoadCount();
	}

	@Override
	public void fetchEntity(String entityName) {
		entityFetchCount.getAndIncrement();
		getEntityStatistics( entityName ).incrementFetchCount();
	}

	@Override
	public void updateEntity(String entityName) {
		entityUpdateCount.getAndIncrement();
		getEntityStatistics( entityName ).incrementUpdateCount();
	}

	@Override
	public void insertEntity(String entityName) {
		entityInsertCount.getAndIncrement();
		getEntityStatistics( entityName ).incrementInsertCount();
	}

	@Override
	public void deleteEntity(String entityName) {
		entityDeleteCount.getAndIncrement();
		getEntityStatistics( entityName ).incrementDeleteCount();
	}

	@Override
	public void optimisticFailure(String entityName) {
		optimisticFailureCount.getAndIncrement();
		getEntityStatistics( entityName ).incrementOptimisticFailureCount();
	}

	@Override
	public void entityCachePut(NavigableRole entityName, String regionName) {
		secondLevelCachePutCount.getAndIncrement();
		getDomainDataRegionStatistics( regionName ).incrementPutCount();
		getEntityStatistics( entityName.getFullPath() ).incrementCachePutCount();
	}

	@Override
	public void entityCacheHit(NavigableRole entityName, String regionName) {
		secondLevelCacheHitCount.getAndIncrement();
		getDomainDataRegionStatistics( regionName ).incrementHitCount();
		getEntityStatistics( entityName.getFullPath() ).incrementCacheHitCount();
	}

	@Override
	public void entityCacheMiss(NavigableRole entityName, String regionName) {
		secondLevelCacheMissCount.getAndIncrement();
		getDomainDataRegionStatistics( regionName ).incrementMissCount();
		getEntityStatistics( entityName.getFullPath() ).incrementCacheMissCount();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection stats

	@Override
	public String[] getCollectionRoleNames() {
		if ( sessionFactory == null ) {
			return ArrayHelper.toStringArray( collectionStatsMap.keySet() );
		}
		else {
			return sessionFactory.getMetamodel().getAllCollectionRoles();
		}
	}

	@Override
	public CollectionStatisticsImpl getCollectionStatistics(String role) {
		if ( sessionFactory == null ) {
			return null;
		}

		return collectionStatsMap.computeIfAbsent(
				role,
				s -> new CollectionStatisticsImpl( sessionFactory.getMetamodel().collectionPersister( role ) )
		);
	}

	@Override
	public long getCollectionLoadCount() {
		return collectionLoadCount.get();
	}

	@Override
	public long getCollectionFetchCount() {
		return collectionFetchCount.get();
	}

	@Override
	public long getCollectionUpdateCount() {
		return collectionUpdateCount.get();
	}

	@Override
	public long getCollectionRemoveCount() {
		return collectionRemoveCount.get();
	}

	@Override
	public long getCollectionRecreateCount() {
		return collectionRecreateCount.get();
	}

	@Override
	public void loadCollection(String role) {
		collectionLoadCount.getAndIncrement();
		getCollectionStatistics( role ).incrementLoadCount();
	}

	@Override
	public void fetchCollection(String role) {
		collectionFetchCount.getAndIncrement();
		getCollectionStatistics( role ).incrementFetchCount();
	}

	@Override
	public void updateCollection(String role) {
		collectionUpdateCount.getAndIncrement();
		getCollectionStatistics( role ).incrementUpdateCount();
	}

	@Override
	public void recreateCollection(String role) {
		collectionRecreateCount.getAndIncrement();
		getCollectionStatistics( role ).incrementRecreateCount();
	}

	@Override
	public void removeCollection(String role) {
		collectionRemoveCount.getAndIncrement();
		getCollectionStatistics( role ).incrementRemoveCount();
	}

	@Override
	public void collectionCachePut(NavigableRole collectionRole, String regionName) {
		secondLevelCachePutCount.getAndIncrement();
		getDomainDataRegionStatistics( regionName ).incrementPutCount();
		getCollectionStatistics( collectionRole.getFullPath() ).incrementCachePutCount();
	}

	@Override
	public void collectionCacheHit(NavigableRole collectionRole, String regionName) {
		secondLevelCacheHitCount.getAndIncrement();
		getDomainDataRegionStatistics( regionName ).incrementHitCount();
		getCollectionStatistics( collectionRole.getFullPath() ).incrementCacheHitCount();
	}

	@Override
	public void collectionCacheMiss(NavigableRole collectionRole, String regionName) {
		secondLevelCacheMissCount.getAndIncrement();
		getDomainDataRegionStatistics( regionName ).incrementMissCount();
		getCollectionStatistics( collectionRole.getFullPath() ).incrementCacheMissCount();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Natural-id stats

	@Override
	public NaturalIdStatisticsImpl getNaturalIdStatistics(String rootEntityName) {
		if ( sessionFactory == null ) {
			return null;
		}

		return naturalIdQueryStatsMap.computeIfAbsent(
				rootEntityName,
				s -> {
					final EntityPersister entityDescriptor = sessionFactory.getMetamodel().entityPersister( rootEntityName );
					if ( !entityDescriptor.hasNaturalIdentifier() ) {
						throw new IllegalArgumentException( "Given entity [" + rootEntityName + "] does not define natural-id" );
					}
					return new NaturalIdStatisticsImpl( entityDescriptor );
				}
		);
	}

	@Override
	public DeprecatedNaturalIdCacheStatisticsImpl getNaturalIdCacheStatistics(String regionName) {
		return deprecatedNaturalIdStatsMap.computeIfAbsent(
				sessionFactory.getCache().unqualifyRegionName( regionName ),
				unqualifiedRegionName -> new DeprecatedNaturalIdCacheStatisticsImpl(
						unqualifiedRegionName,
						sessionFactory.getCache().getNaturalIdAccessesInRegion( unqualifiedRegionName )
				)
		);
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
	public String getNaturalIdQueryExecutionMaxTimeEntity() {
		return naturalIdQueryExecutionMaxTimeEntity;
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

	@Override
	public void naturalIdCachePut(
			NavigableRole rootEntityName,
			String regionName) {
		naturalIdCachePutCount.getAndIncrement();

		getDomainDataRegionStatistics( regionName ).incrementPutCount();

		getNaturalIdStatistics( rootEntityName.getFullPath() ).incrementCachePutCount();

		getNaturalIdCacheStatistics( qualify( regionName ) ).incrementPutCount();
	}

	@Override
	public void naturalIdCacheHit(
			NavigableRole rootEntityName,
			String regionName) {
		naturalIdCacheHitCount.getAndIncrement();

		getDomainDataRegionStatistics( regionName ).incrementHitCount();

		getNaturalIdStatistics( rootEntityName.getFullPath() ).incrementCacheHitCount();

		getNaturalIdCacheStatistics( qualify( regionName ) ).incrementHitCount();
	}

	@Override
	public void naturalIdCacheMiss(
			NavigableRole rootEntityName,
			String regionName) {
		naturalIdCacheMissCount.getAndIncrement();

		getDomainDataRegionStatistics( regionName ).incrementMissCount();

		getNaturalIdStatistics( rootEntityName.getFullPath() ).incrementCacheMissCount();

		getNaturalIdCacheStatistics( qualify( regionName ) ).incrementMissCount();
	}

	protected String qualify(String regionName) {
		return sessionFactory.getSessionFactoryOptions().getCacheRegionPrefix() == null
					? regionName
					: sessionFactory.getSessionFactoryOptions().getCacheRegionPrefix() + '.' + regionName;
	}

	@Override
	public void naturalIdQueryExecuted(String rootEntityName, long time) {
		naturalIdQueryExecutionCount.getAndIncrement();

		boolean isLongestQuery;
		//noinspection StatementWithEmptyBody
		for ( long old = naturalIdQueryExecutionMaxTime.get();
				( isLongestQuery = time > old ) && ( !naturalIdQueryExecutionMaxTime.compareAndSet( old, time ) );
				old = naturalIdQueryExecutionMaxTime.get() ) {
			// nothing to do here given the odd loop structure...
		}

		if ( isLongestQuery ) {
			naturalIdQueryExecutionMaxTimeEntity = rootEntityName;
		}

		final EntityPersister rootEntityPersister = sessionFactory.getMetamodel().entityPersister( rootEntityName );

		getNaturalIdStatistics( rootEntityName ).queryExecuted( time );

		if ( rootEntityPersister.hasNaturalIdCache() ) {
			final String naturalIdRegionName = rootEntityPersister.getNaturalIdCacheAccessStrategy()
					.getRegion()
					.getName();
			getNaturalIdCacheStatistics( qualify( naturalIdRegionName ) ).queryExecuted( time );

			if ( isLongestQuery ) {
				naturalIdQueryExecutionMaxTimeRegion = naturalIdRegionName;
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Second-level cache region stats

	@Override
	public String[] getSecondLevelCacheRegionNames() {
		if ( sessionFactory == null ) {
			throw new IllegalStateException( "Statistics no longer associated with SessionFactory - cannot get (legacy) region names" );
		}

		return sessionFactory.getCache().getSecondLevelCacheRegionNames();
	}

	@Override
	public CacheRegionStatisticsImpl getDomainDataRegionStatistics(String regionName) {
		if ( sessionFactory == null ) {
			return null;
		}

		return l2CacheStatsMap.computeIfAbsent(
				regionName,
				s -> {
					final Region region = sessionFactory.getCache().getRegion( regionName );

					if ( region == null ) {
						throw new IllegalArgumentException( "Unknown cache region : " + regionName );
					}

					if ( region instanceof QueryResultsRegion ) {
						throw new IllegalArgumentException(
								"Region name [" + regionName + "] referred to a query result region, not a domain data region"
						);
					}

					return new CacheRegionStatisticsImpl( region );
				}
		);
	}

	@Override
	public CacheRegionStatisticsImpl getQueryRegionStatistics(String regionName) {
		final CacheRegionStatisticsImpl existing = l2CacheStatsMap.get( regionName );
		if ( existing != null ) {
			return existing;
		}

		if ( sessionFactory == null ) {
			return null;
		}

		final QueryResultRegionAccess regionAccess = sessionFactory.getCache()
				.getQueryResultsRegionAccessStrictly( regionName );
		if ( regionAccess == null ) {
			return null;
		}

		return l2CacheStatsMap.computeIfAbsent(
				regionName,
				s -> new CacheRegionStatisticsImpl( regionAccess.getRegion() )
		);
	}

	@Override
	public CacheRegionStatisticsImpl getCacheRegionStatistics(String regionName) {
		if ( sessionFactory == null ) {
			return null;
		}

		if ( ! sessionFactory.getSessionFactoryOptions().isSecondLevelCacheEnabled() ) {
			return null;
		}

		return l2CacheStatsMap.computeIfAbsent(
				regionName,
				s -> {
					final Region region = sessionFactory.getCache().getRegion( regionName );

					if ( region == null ) {
						throw new IllegalArgumentException( "Unknown cache region : " + regionName );
					}

					return new CacheRegionStatisticsImpl( region );
				}
		);
	}

	@Override
	public CacheRegionStatisticsImpl getSecondLevelCacheStatistics(String regionName) {
		return getCacheRegionStatistics( sessionFactory.getCache().unqualifyRegionName( regionName ) );
	}

	@Override
	public long getSecondLevelCacheHitCount() {
		return secondLevelCacheHitCount.get();
	}

	@Override
	public long getSecondLevelCacheMissCount() {
		return secondLevelCacheMissCount.get();
	}

	@Override
	public long getSecondLevelCachePutCount() {
		return secondLevelCachePutCount.get();
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query statistics

	@Override
	public String[] getQueries() {
		return ArrayHelper.toStringArray( queryStatsMap.keySet() );
	}

	@Override
	public QueryStatisticsImpl getQueryStatistics(String queryString) {
		return queryStatsMap.computeIfAbsent(
				queryString,
				s -> new QueryStatisticsImpl( queryString )
		);
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
	public String getQueryExecutionMaxTimeQueryString() {
		return queryExecutionMaxTimeQueryString;
	}

	@Override
	public long getQueryExecutionMaxTime() {
		return queryExecutionMaxTime.get();
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
			getQueryStatistics( hql ).executed( rows, time );
		}
	}

	@Override
	public void queryCacheHit(String hql, String regionName) {
		LOG.tracef( "Statistics#queryCacheHit( `%s`, `%s` )", hql, regionName );

		queryCacheHitCount.getAndIncrement();

		getQueryRegionStats( regionName ).incrementHitCount();

		if ( hql != null ) {
			getQueryStatistics( hql ).incrementCacheHitCount();
		}
	}

	private CacheRegionStatisticsImpl getQueryRegionStats(String regionName) {
		return l2CacheStatsMap.computeIfAbsent(
				regionName,
				s -> new CacheRegionStatisticsImpl( sessionFactory.getCache().getQueryResultsRegionAccess( regionName ).getRegion() )
		);
	}


	@Override
	public void queryCacheMiss(String hql, String regionName) {
		LOG.tracef( "Statistics#queryCacheMiss( `%s`, `%s` )", hql, regionName );

		queryCacheMissCount.getAndIncrement();

		getQueryRegionStats( regionName ).incrementMissCount();

		if ( hql != null ) {
			getQueryStatistics( hql ).incrementCacheMissCount();
		}
	}

	@Override
	public void queryCachePut(String hql, String regionName) {
		LOG.tracef( "Statistics#queryCachePut( `%s`, `%s` )", hql, regionName );

		queryCachePutCount.getAndIncrement();

		getQueryRegionStats( regionName ).incrementPutCount();

		if ( hql != null ) {
			getQueryStatistics( hql ).incrementCachePutCount();
		}
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Session/misc stats

	@Override
	public long getSessionOpenCount() {
		return sessionOpenCount.get();
	}

	@Override
	public long getSessionCloseCount() {
		return sessionCloseCount.get();
	}

	@Override
	public long getFlushCount() {
		return flushCount.get();
	}

	@Override
	public long getConnectCount() {
		return connectCount.get();
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
	public long getCloseStatementCount() {
		return closeStatementCount.get();
	}

	@Override
	public long getPrepareStatementCount() {
		return prepareStatementCount.get();
	}

	@Override
	public void openSession() {
		sessionOpenCount.getAndIncrement();
	}

	@Override
	public void closeSession() {
		sessionCloseCount.getAndIncrement();
	}

	@Override
	public void flush() {
		flushCount.getAndIncrement();
	}

	@Override
	public void connect() {
		connectCount.getAndIncrement();
	}

	@Override
	public void prepareStatement() {
		prepareStatementCount.getAndIncrement();
	}

	@Override
	public void closeStatement() {
		closeStatementCount.getAndIncrement();
	}

	@Override
	public void endTransaction(boolean success) {
		transactionCount.getAndIncrement();
		if ( success ) {
			committedTransactionCount.getAndIncrement();
		}
	}



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
}
