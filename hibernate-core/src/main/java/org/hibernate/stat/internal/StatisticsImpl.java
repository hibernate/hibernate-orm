/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Manageable;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Implementation of {@link org.hibernate.stat.Statistics} based on the {@link java.util.concurrent} package.
 *
 * @author Alex Snaps
 * @author Sanne Grinovero
 */
@SuppressWarnings({ "unchecked" })
public class StatisticsImpl implements StatisticsImplementor, Service, Manageable {

	private static final CoreMessageLogger LOG = messageLogger( StatisticsImpl.class );

	private final MetamodelImplementor metamodel;
	private final CacheImplementor cache;
	private final String cacheRegionPrefix;
	private final boolean secondLevelCacheEnabled;
	private final boolean queryCacheEnabled;

	private volatile boolean isStatisticsEnabled;
	private volatile long startTime;

	private final LongAdder sessionOpenCount = new LongAdder();
	private final LongAdder sessionCloseCount = new LongAdder();
	private final LongAdder flushCount = new LongAdder();
	private final LongAdder connectCount = new LongAdder();

	private final LongAdder prepareStatementCount = new LongAdder();
	private final LongAdder closeStatementCount = new LongAdder();

	private final LongAdder entityLoadCount = new LongAdder();
	private final LongAdder entityUpdateCount = new LongAdder();
	private final LongAdder entityInsertCount = new LongAdder();
	private final LongAdder entityDeleteCount = new LongAdder();
	private final LongAdder entityFetchCount = new LongAdder();
	private final LongAdder collectionLoadCount = new LongAdder();
	private final LongAdder collectionUpdateCount = new LongAdder();
	private final LongAdder collectionRemoveCount = new LongAdder();
	private final LongAdder collectionRecreateCount = new LongAdder();
	private final LongAdder collectionFetchCount = new LongAdder();

	private final LongAdder secondLevelCacheHitCount = new LongAdder();
	private final LongAdder secondLevelCacheMissCount = new LongAdder();
	private final LongAdder secondLevelCachePutCount = new LongAdder();
	
	private final LongAdder naturalIdCacheHitCount = new LongAdder();
	private final LongAdder naturalIdCacheMissCount = new LongAdder();
	private final LongAdder naturalIdCachePutCount = new LongAdder();
	private final LongAdder naturalIdQueryExecutionCount = new LongAdder();
	private final AtomicLong naturalIdQueryExecutionMaxTime = new AtomicLong();
	private volatile String naturalIdQueryExecutionMaxTimeRegion;
	private volatile String naturalIdQueryExecutionMaxTimeEntity;

	private final LongAdder queryExecutionCount = new LongAdder();
	private final AtomicLong queryExecutionMaxTime = new AtomicLong();
	private volatile String queryExecutionMaxTimeQueryString;
	private final LongAdder queryCacheHitCount = new LongAdder();
	private final LongAdder queryCacheMissCount = new LongAdder();
	private final LongAdder queryCachePutCount = new LongAdder();

	private final LongAdder queryPlanCacheHitCount = new LongAdder();
	private final LongAdder queryPlanCacheMissCount = new LongAdder();

	private final LongAdder updateTimestampsCacheHitCount = new LongAdder();
	private final LongAdder updateTimestampsCacheMissCount = new LongAdder();
	private final LongAdder updateTimestampsCachePutCount = new LongAdder();

	private final LongAdder committedTransactionCount = new LongAdder();
	private final LongAdder transactionCount = new LongAdder();

	private final LongAdder optimisticFailureCount = new LongAdder();

	private final StatsNamedContainer<EntityStatisticsImpl> entityStatsMap = new StatsNamedContainer();
	private final StatsNamedContainer<NaturalIdStatisticsImpl> naturalIdQueryStatsMap = new StatsNamedContainer();
	private final StatsNamedContainer<CollectionStatisticsImpl> collectionStatsMap = new StatsNamedContainer();

	/**
	 * Keyed by query string
	 */
	private final StatsNamedContainer<QueryStatisticsImpl> queryStatsMap;

	/**
	 * Keyed by region name
	 */
	private final StatsNamedContainer<CacheRegionStatisticsImpl> l2CacheStatsMap = new StatsNamedContainer<>();

	private final StatsNamedContainer<DeprecatedNaturalIdCacheStatisticsImpl> deprecatedNaturalIdStatsMap = new StatsNamedContainer();

	public StatisticsImpl(SessionFactoryImplementor sessionFactory) {
		Objects.requireNonNull( sessionFactory );
		SessionFactoryOptions sessionFactoryOptions = sessionFactory.getSessionFactoryOptions();
		this.queryStatsMap = new StatsNamedContainer(
				sessionFactory != null ?
					sessionFactoryOptions.getQueryStatisticsMaxSize() :
					Statistics.DEFAULT_QUERY_STATISTICS_MAX_SIZE,
				20
		);
		clear();
		metamodel = sessionFactory.getMetamodel();
		cache = sessionFactory.getCache();
		cacheRegionPrefix = sessionFactoryOptions.getCacheRegionPrefix();
		secondLevelCacheEnabled = sessionFactoryOptions.isSecondLevelCacheEnabled();
		queryCacheEnabled = sessionFactoryOptions.isQueryCacheEnabled();
	}

	/**
	 * reset all statistics
	 */
	public void clear() {
		secondLevelCacheHitCount.reset();
		secondLevelCacheMissCount.reset();
		secondLevelCachePutCount.reset();
		
		naturalIdCacheHitCount.reset();
		naturalIdCacheMissCount.reset();
		naturalIdCachePutCount.reset();
		naturalIdQueryExecutionCount.reset();
		naturalIdQueryExecutionMaxTime.set( 0L );
		naturalIdQueryExecutionMaxTimeRegion = null;
		naturalIdQueryExecutionMaxTimeEntity = null;

		sessionCloseCount.reset();
		sessionOpenCount.reset();
		flushCount.reset();
		connectCount.reset();

		prepareStatementCount.reset();
		closeStatementCount.reset();

		entityDeleteCount.reset();
		entityInsertCount.reset();
		entityUpdateCount.reset();
		entityLoadCount.reset();
		entityFetchCount.reset();

		collectionRemoveCount.reset();
		collectionUpdateCount.reset();
		collectionRecreateCount.reset();
		collectionLoadCount.reset();
		collectionFetchCount.reset();

		queryExecutionCount.reset();
		queryCacheHitCount.reset();
		queryExecutionMaxTime.set( 0L );
		queryExecutionMaxTimeQueryString = null;
		queryCacheMissCount.reset();
		queryCachePutCount.reset();

		updateTimestampsCacheMissCount.reset();
		updateTimestampsCacheHitCount.reset();
		updateTimestampsCachePutCount.reset();

		transactionCount.reset();
		committedTransactionCount.reset();

		optimisticFailureCount.reset();

		entityStatsMap.clear();
		collectionStatsMap.clear();
		naturalIdQueryStatsMap.clear();
		l2CacheStatsMap.clear();
		queryStatsMap.clear();
		deprecatedNaturalIdStatsMap.clear();

		queryPlanCacheHitCount.reset();
		queryPlanCacheMissCount.reset();

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
		return metamodel.getAllEntityNames();
	}

	@Override
	public EntityStatisticsImpl getEntityStatistics(String entityName) {
		return entityStatsMap.getOrCompute(
				entityName,
				s -> new EntityStatisticsImpl( metamodel.entityPersister( s ) )
		);
	}

	@Override
	public long getEntityLoadCount() {
		return entityLoadCount.sum();
	}

	@Override
	public long getEntityFetchCount() {
		return entityFetchCount.sum();
	}

	@Override
	public long getEntityDeleteCount() {
		return entityDeleteCount.sum();
	}

	@Override
	public long getEntityInsertCount() {
		return entityInsertCount.sum();
	}

	@Override
	public long getEntityUpdateCount() {
		return entityUpdateCount.sum();
	}

	@Override
	public long getOptimisticFailureCount() {
		return optimisticFailureCount.sum();
	}

	@Override
	public void loadEntity(String entityName) {
		entityLoadCount.increment();
		getEntityStatistics( entityName ).incrementLoadCount();
	}

	@Override
	public void fetchEntity(String entityName) {
		entityFetchCount.increment();
		getEntityStatistics( entityName ).incrementFetchCount();
	}

	@Override
	public void updateEntity(String entityName) {
		entityUpdateCount.increment();
		getEntityStatistics( entityName ).incrementUpdateCount();
	}

	@Override
	public void insertEntity(String entityName) {
		entityInsertCount.increment();
		getEntityStatistics( entityName ).incrementInsertCount();
	}

	@Override
	public void deleteEntity(String entityName) {
		entityDeleteCount.increment();
		getEntityStatistics( entityName ).incrementDeleteCount();
	}

	@Override
	public void optimisticFailure(String entityName) {
		optimisticFailureCount.increment();
		getEntityStatistics( entityName ).incrementOptimisticFailureCount();
	}

	@Override
	public void entityCachePut(NavigableRole entityName, String regionName) {
		secondLevelCachePutCount.increment();
		getDomainDataRegionStatistics( regionName ).incrementPutCount();
		getEntityStatistics( entityName.getFullPath() ).incrementCachePutCount();
	}

	@Override
	public void entityCacheHit(NavigableRole entityName, String regionName) {
		secondLevelCacheHitCount.increment();
		getDomainDataRegionStatistics( regionName ).incrementHitCount();
		getEntityStatistics( entityName.getFullPath() ).incrementCacheHitCount();
	}

	@Override
	public void entityCacheMiss(NavigableRole entityName, String regionName) {
		secondLevelCacheMissCount.increment();
		getDomainDataRegionStatistics( regionName ).incrementMissCount();
		getEntityStatistics( entityName.getFullPath() ).incrementCacheMissCount();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection stats

	@Override
	public String[] getCollectionRoleNames() {
		return metamodel.getAllCollectionRoles();
	}

	@Override
	public CollectionStatisticsImpl getCollectionStatistics(String role) {
		return collectionStatsMap.getOrCompute(
				role,
				s -> new CollectionStatisticsImpl( metamodel.collectionPersister( s ) )
		);
	}

	@Override
	public long getCollectionLoadCount() {
		return collectionLoadCount.sum();
	}

	@Override
	public long getCollectionFetchCount() {
		return collectionFetchCount.sum();
	}

	@Override
	public long getCollectionUpdateCount() {
		return collectionUpdateCount.sum();
	}

	@Override
	public long getCollectionRemoveCount() {
		return collectionRemoveCount.sum();
	}

	@Override
	public long getCollectionRecreateCount() {
		return collectionRecreateCount.sum();
	}

	@Override
	public void loadCollection(String role) {
		collectionLoadCount.increment();
		getCollectionStatistics( role ).incrementLoadCount();
	}

	@Override
	public void fetchCollection(String role) {
		collectionFetchCount.increment();
		getCollectionStatistics( role ).incrementFetchCount();
	}

	@Override
	public void updateCollection(String role) {
		collectionUpdateCount.increment();
		getCollectionStatistics( role ).incrementUpdateCount();
	}

	@Override
	public void recreateCollection(String role) {
		collectionRecreateCount.increment();
		getCollectionStatistics( role ).incrementRecreateCount();
	}

	@Override
	public void removeCollection(String role) {
		collectionRemoveCount.increment();
		getCollectionStatistics( role ).incrementRemoveCount();
	}

	@Override
	public void collectionCachePut(NavigableRole collectionRole, String regionName) {
		secondLevelCachePutCount.increment();
		getDomainDataRegionStatistics( regionName ).incrementPutCount();
		getCollectionStatistics( collectionRole.getFullPath() ).incrementCachePutCount();
	}

	@Override
	public void collectionCacheHit(NavigableRole collectionRole, String regionName) {
		secondLevelCacheHitCount.increment();
		getDomainDataRegionStatistics( regionName ).incrementHitCount();
		getCollectionStatistics( collectionRole.getFullPath() ).incrementCacheHitCount();
	}

	@Override
	public void collectionCacheMiss(NavigableRole collectionRole, String regionName) {
		secondLevelCacheMissCount.increment();
		getDomainDataRegionStatistics( regionName ).incrementMissCount();
		getCollectionStatistics( collectionRole.getFullPath() ).incrementCacheMissCount();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Natural-id stats

	@Override
	public NaturalIdStatisticsImpl getNaturalIdStatistics(String rootEntityName) {
		return naturalIdQueryStatsMap.getOrCompute(
				rootEntityName,
				s -> {
					final EntityPersister entityDescriptor = metamodel.entityPersister( s );
					if ( !entityDescriptor.hasNaturalIdentifier() ) {
						throw new IllegalArgumentException( "Given entity [" + s + "] does not define natural-id" );
					}
					return new NaturalIdStatisticsImpl( entityDescriptor );
				}
		);
	}

	@Override
	public DeprecatedNaturalIdCacheStatisticsImpl getNaturalIdCacheStatistics(String regionName) {
		final String key = cache.unqualifyRegionName( regionName );
		return deprecatedNaturalIdStatsMap.getOrCompute(
				key,
				unqualifiedRegionName -> new DeprecatedNaturalIdCacheStatisticsImpl(
						unqualifiedRegionName,
						cache.getNaturalIdAccessesInRegion( unqualifiedRegionName )
				)
		);
	}

	@Override
	public long getNaturalIdQueryExecutionCount() {
		return naturalIdQueryExecutionCount.sum();
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
		return naturalIdCacheHitCount.sum();
	}

	@Override
	public long getNaturalIdCacheMissCount() {
		return naturalIdCacheMissCount.sum();
	}

	@Override
	public long getNaturalIdCachePutCount() {
		return naturalIdCachePutCount.sum();
	}

	@Override
	public void naturalIdCachePut(
			NavigableRole rootEntityName,
			String regionName) {
		naturalIdCachePutCount.increment();

		getDomainDataRegionStatistics( regionName ).incrementPutCount();

		getNaturalIdStatistics( rootEntityName.getFullPath() ).incrementCachePutCount();

		getNaturalIdCacheStatistics( qualify( regionName ) ).incrementPutCount();
	}

	@Override
	public void naturalIdCacheHit(
			NavigableRole rootEntityName,
			String regionName) {
		naturalIdCacheHitCount.increment();

		getDomainDataRegionStatistics( regionName ).incrementHitCount();

		getNaturalIdStatistics( rootEntityName.getFullPath() ).incrementCacheHitCount();

		getNaturalIdCacheStatistics( qualify( regionName ) ).incrementHitCount();
	}

	@Override
	public void naturalIdCacheMiss(
			NavigableRole rootEntityName,
			String regionName) {
		naturalIdCacheMissCount.increment();

		getDomainDataRegionStatistics( regionName ).incrementMissCount();

		getNaturalIdStatistics( rootEntityName.getFullPath() ).incrementCacheMissCount();

		getNaturalIdCacheStatistics( qualify( regionName ) ).incrementMissCount();
	}

	private String qualify(final String regionName) {
		return cacheRegionPrefix == null
					? regionName
					: cacheRegionPrefix + '.' + regionName;
	}

	@Override
	public void naturalIdQueryExecuted(String rootEntityName, long time) {
		naturalIdQueryExecutionCount.increment();

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

		final EntityPersister rootEntityPersister = metamodel.entityPersister( rootEntityName );

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
		return cache.getSecondLevelCacheRegionNames();
	}

	@Override
	public CacheRegionStatisticsImpl getDomainDataRegionStatistics(String regionName) {
		return l2CacheStatsMap.getOrCompute(
				regionName,
				s -> {
					final Region region = cache.getRegion( s );

					if ( region == null ) {
						throw new IllegalArgumentException( "Unknown cache region : " + s );
					}

					if ( region instanceof QueryResultsRegion ) {
						throw new IllegalArgumentException(
								"Region name [" + s + "] referred to a query result region, not a domain data region"
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

		final QueryResultsCache regionAccess = cache
				.getQueryResultsCacheStrictly( regionName );
		if ( regionAccess == null ) {
			return null;
		}

		return l2CacheStatsMap.getOrCompute(
				regionName,
				s -> new CacheRegionStatisticsImpl( regionAccess.getRegion() )
		);
	}

	@Override
	public CacheRegionStatisticsImpl getCacheRegionStatistics(String regionName) {
		if ( ! secondLevelCacheEnabled ) {
			return null;
		}

		return l2CacheStatsMap.getOrCompute(
				regionName,
				s -> {
					Region region = cache.getRegion( s );

					if ( region == null ) {

						if ( ! queryCacheEnabled ) {
							return null;
						}

						// this is the pre-5.3 behavior.  and since this is a pre-5.3 method it should behave consistently
						// NOTE that this method is deprecated
						region = cache.getQueryResultsCache( s ).getRegion();
					}

					return new CacheRegionStatisticsImpl( region );
				}
		);
	}

	@Override
	public CacheRegionStatisticsImpl getSecondLevelCacheStatistics(String regionName) {
		return getCacheRegionStatistics( cache.unqualifyRegionName( regionName ) );
	}

	@Override
	public long getSecondLevelCacheHitCount() {
		return secondLevelCacheHitCount.sum();
	}

	@Override
	public long getSecondLevelCacheMissCount() {
		return secondLevelCacheMissCount.sum();
	}

	@Override
	public long getSecondLevelCachePutCount() {
		return secondLevelCachePutCount.sum();
	}

	@Override
	public long getUpdateTimestampsCacheHitCount() {
		return updateTimestampsCacheHitCount.sum();
	}

	@Override
	public long getUpdateTimestampsCacheMissCount() {
		return updateTimestampsCacheMissCount.sum();
	}

	@Override
	public long getUpdateTimestampsCachePutCount() {
		return updateTimestampsCachePutCount.sum();
	}

	@Override
	public void updateTimestampsCacheHit() {
		updateTimestampsCacheHitCount.increment();
	}

	@Override
	public void updateTimestampsCacheMiss() {
		updateTimestampsCacheMissCount.increment();
	}

	@Override
	public void updateTimestampsCachePut() {
		updateTimestampsCachePutCount.increment();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Query statistics

	@Override
	public String[] getQueries() {
		return queryStatsMap.keysAsArray();
	}

	@Override
	public QueryStatisticsImpl getQueryStatistics(String queryString) {
		return queryStatsMap.getOrCompute(
				queryString,
				s -> new QueryStatisticsImpl( s )
		);
	}

	@Override
	public long getQueryExecutionCount() {
		return queryExecutionCount.sum();
	}

	@Override
	public long getQueryCacheHitCount() {
		return queryCacheHitCount.sum();
	}

	@Override
	public long getQueryCacheMissCount() {
		return queryCacheMissCount.sum();
	}

	@Override
	public long getQueryCachePutCount() {
		return queryCachePutCount.sum();
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
		LOG.hql( hql, time, (long) rows );
		queryExecutionCount.increment();

		boolean isLongestQuery;
		//noinspection StatementWithEmptyBody
		for ( long old = queryExecutionMaxTime.get();
				( isLongestQuery = time > old ) && ( ! queryExecutionMaxTime.compareAndSet( old, time ) );
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

		queryCacheHitCount.increment();

		getQueryRegionStats( regionName ).incrementHitCount();

		if ( hql != null ) {
			getQueryStatistics( hql ).incrementCacheHitCount();
		}
	}

	@Override
	public void queryCacheMiss(String hql, String regionName) {
		LOG.tracef( "Statistics#queryCacheMiss( `%s`, `%s` )", hql, regionName );

		queryCacheMissCount.increment();

		getQueryRegionStats( regionName ).incrementMissCount();

		if ( hql != null ) {
			getQueryStatistics( hql ).incrementCacheMissCount();
		}
	}

	@Override
	public void queryCachePut(String hql, String regionName) {
		LOG.tracef( "Statistics#queryCachePut( `%s`, `%s` )", hql, regionName );

		queryCachePutCount.increment();

		getQueryRegionStats( regionName ).incrementPutCount();

		if ( hql != null ) {
			getQueryStatistics( hql ).incrementCachePutCount();
		}
	}

	@Override
	public long getQueryPlanCacheHitCount() {
		return queryPlanCacheHitCount.sum();
	}

	@Override
	public long getQueryPlanCacheMissCount() {
		return queryPlanCacheMissCount.sum();
	}

	@Override
	public void queryCompiled(String hql, long microseconds) {
		queryPlanCacheMissCount.increment();

		if ( hql != null ) {
			getQueryStatistics( hql ).compiled( microseconds );
		}
	}

	@Override
	public void queryPlanCacheHit(String hql) {
		queryPlanCacheHitCount.increment();

		if ( hql != null ) {
			getQueryStatistics( hql ).incrementPlanCacheHitCount();
		}
	}

	private CacheRegionStatisticsImpl getQueryRegionStats(String regionName) {
		return l2CacheStatsMap.getOrCompute(
				regionName,
				s -> new CacheRegionStatisticsImpl( cache.getQueryResultsCache( regionName ).getRegion() )
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Session/misc stats

	@Override
	public long getSessionOpenCount() {
		return sessionOpenCount.sum();
	}

	@Override
	public long getSessionCloseCount() {
		return sessionCloseCount.sum();
	}

	@Override
	public long getFlushCount() {
		return flushCount.sum();
	}

	@Override
	public long getConnectCount() {
		return connectCount.sum();
	}

	@Override
	public long getSuccessfulTransactionCount() {
		return committedTransactionCount.sum();
	}

	@Override
	public long getTransactionCount() {
		return transactionCount.sum();
	}

	@Override
	public long getCloseStatementCount() {
		return closeStatementCount.sum();
	}

	@Override
	public long getPrepareStatementCount() {
		return prepareStatementCount.sum();
	}

	@Override
	public void openSession() {
		sessionOpenCount.increment();
	}

	@Override
	public void closeSession() {
		sessionCloseCount.increment();
	}

	@Override
	public void flush() {
		flushCount.increment();
	}

	@Override
	public void connect() {
		connectCount.increment();
	}

	@Override
	public void prepareStatement() {
		prepareStatementCount.increment();
	}

	@Override
	public void closeStatement() {
		closeStatementCount.increment();
	}

	@Override
	public void endTransaction(boolean success) {
		transactionCount.increment();
		if ( success ) {
			committedTransactionCount.increment();
		}
	}

	@Override
	public void logSummary() {
		LOG.loggingStatistics();
		LOG.startTime( startTime );
		LOG.sessionsOpened( sessionOpenCount.sum() );
		LOG.sessionsClosed( sessionCloseCount.sum() );
		LOG.transactions( transactionCount.sum() );
		LOG.successfulTransactions( committedTransactionCount.sum() );
		LOG.optimisticLockFailures( optimisticFailureCount.sum() );
		LOG.flushes( flushCount.sum() );
		LOG.connectionsObtained( connectCount.sum() );
		LOG.statementsPrepared( prepareStatementCount.sum() );
		LOG.statementsClosed( closeStatementCount.sum() );
		LOG.secondLevelCachePuts( secondLevelCachePutCount.sum() );
		LOG.secondLevelCacheHits( secondLevelCacheHitCount.sum() );
		LOG.secondLevelCacheMisses( secondLevelCacheMissCount.sum() );
		LOG.entitiesLoaded( entityLoadCount.sum() );
		LOG.entitiesUpdated( entityUpdateCount.sum() );
		LOG.entitiesInserted( entityInsertCount.sum() );
		LOG.entitiesDeleted( entityDeleteCount.sum() );
		LOG.entitiesFetched( entityFetchCount.sum() );
		LOG.collectionsLoaded( collectionLoadCount.sum() );
		LOG.collectionsUpdated( collectionUpdateCount.sum() );
		LOG.collectionsRemoved( collectionRemoveCount.sum() );
		LOG.collectionsRecreated( collectionRecreateCount.sum() );
		LOG.collectionsFetched( collectionFetchCount.sum() );
		LOG.naturalIdCachePuts( naturalIdCachePutCount.sum() );
		LOG.naturalIdCacheHits( naturalIdCacheHitCount.sum() );
		LOG.naturalIdCacheMisses( naturalIdCacheMissCount.sum() );
		LOG.naturalIdMaxQueryTime( naturalIdQueryExecutionMaxTime.get() );
		LOG.naturalIdQueriesExecuted( naturalIdQueryExecutionCount.sum() );
		LOG.queriesExecuted( queryExecutionCount.sum() );
		LOG.queryCachePuts( queryCachePutCount.sum() );
		LOG.timestampCachePuts( updateTimestampsCachePutCount.sum() );
		LOG.timestampCacheHits( updateTimestampsCacheHitCount.sum() );
		LOG.timestampCacheMisses( updateTimestampsCacheMissCount.sum() );
		LOG.queryCacheHits( queryCacheHitCount.sum() );
		LOG.queryCacheMisses( queryCacheMissCount.sum() );
		LOG.maxQueryTime( queryExecutionMaxTime.get() );
		LOG.queryPlanCacheHits( queryPlanCacheHitCount.sum() );
		LOG.queryPlanCacheMisses( queryPlanCacheMissCount.sum() );
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
				.append( ",query plan cache hits=" ).append( queryPlanCacheHitCount )
				.append( ",query plan cache misses=" ).append( queryPlanCacheMissCount )
				.append( ']' )
				.toString();
	}
}
