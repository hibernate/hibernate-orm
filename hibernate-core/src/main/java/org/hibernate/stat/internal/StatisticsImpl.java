/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.stat.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.Service;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Implementation of {@link Statistics} based on the {@link java.util.concurrent} package.
 *
 * @author Alex Snaps
 * @author Sanne Grinovero
 */
public class StatisticsImpl implements StatisticsImplementor, Service {

	private static final CoreMessageLogger log = messageLogger( StatisticsImpl.class );

	private final MappingMetamodelImplementor metamodel;
	private final CacheImplementor cache;

	private final String[] allEntityNames;
	private final String[] allCollectionRoles;

	private final boolean secondLevelCacheEnabled;
	private final boolean queryCacheEnabled;

	private volatile boolean isStatisticsEnabled;
	private volatile Instant startTime;

	private final LongAdder sessionOpenCount = new LongAdder();
	private final LongAdder sessionCloseCount = new LongAdder();
	private final LongAdder flushCount = new LongAdder();
	private final LongAdder connectCount = new LongAdder();

	private final LongAdder prepareStatementCount = new LongAdder();
	private final LongAdder closeStatementCount = new LongAdder();

	private final LongAdder entityLoadCount = new LongAdder();
	private final LongAdder entityUpdateCount = new LongAdder();
	private final LongAdder entityUpsertCount = new LongAdder();
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
	private volatile @Nullable String naturalIdQueryExecutionMaxTimeRegion;
	private volatile @Nullable String naturalIdQueryExecutionMaxTimeEntity;

	private final LongAdder queryExecutionCount = new LongAdder();
	private final AtomicLong queryExecutionMaxTime = new AtomicLong();
	private volatile @Nullable String queryExecutionMaxTimeQueryString;
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

	private final StatsNamedContainer<EntityStatisticsImpl> entityStatsMap = new StatsNamedContainer<>();
	private final StatsNamedContainer<NaturalIdStatisticsImpl> naturalIdQueryStatsMap = new StatsNamedContainer<>();
	private final StatsNamedContainer<CollectionStatisticsImpl> collectionStatsMap = new StatsNamedContainer<>();

	/**
	 * Keyed by query string
	 */
	private final StatsNamedContainer<QueryStatisticsImpl> queryStatsMap;

	/**
	 * Keyed by region name
	 */
	private final StatsNamedContainer<CacheRegionStatisticsImpl> l2CacheStatsMap = new StatsNamedContainer<>();

	/**
	 * Keyed by query SQL
	 */
	private final Map<String, Long> slowQueries = new ConcurrentHashMap<>();

	public StatisticsImpl(SessionFactoryImplementor sessionFactory) {
		Objects.requireNonNull( sessionFactory );
		SessionFactoryOptions sessionFactoryOptions = sessionFactory.getSessionFactoryOptions();
		this.queryStatsMap = new StatsNamedContainer<>(
				sessionFactoryOptions.getQueryStatisticsMaxSize(),
				20
		);
		resetStart();
		metamodel = sessionFactory.getMappingMetamodel();
		cache = sessionFactory.getCache();
		secondLevelCacheEnabled = sessionFactoryOptions.isSecondLevelCacheEnabled();
		queryCacheEnabled = sessionFactoryOptions.isQueryCacheEnabled();

		final List<String> entityNames = new ArrayList<>();
		metamodel.forEachEntityDescriptor( (entityDescriptor) -> entityNames.add( entityDescriptor.getEntityName() ) );
		this.allEntityNames = entityNames.toArray( new String[0] );

		final List<String> collectionRoles = new ArrayList<>();
		metamodel.forEachCollectionDescriptor( (collectionDescriptor) -> collectionRoles.add( collectionDescriptor.getRole() ) );
		this.allCollectionRoles = collectionRoles.toArray( new String[0] );
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
		entityUpsertCount.reset();
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

		queryPlanCacheHitCount.reset();
		queryPlanCacheMissCount.reset();

		resetStart();
	}

	private void resetStart(@UnknownInitialization StatisticsImpl this) {
		startTime = Instant.now();
	}

	@Override
	public Instant getStart() {
		return startTime;
	}

	@Override
	public long getStartTime() {
		return startTime.toEpochMilli();
	}

	@Override
	public boolean isStatisticsEnabled() {
		return isStatisticsEnabled;
	}

	@Override
	public void setStatisticsEnabled(boolean enabled) {
		isStatisticsEnabled = enabled;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity stats

	@Override
	public String[] getEntityNames() {
		return allEntityNames;
	}

	@Override
	public EntityStatisticsImpl getEntityStatistics(String entityName) {
		return NullnessUtil.castNonNull(
					entityStatsMap.getOrCompute(
							entityName,
							this::instantiateEntityStatistics
					)
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
	public long getEntityUpsertCount() {
		return entityUpsertCount.sum();
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
	public void upsertEntity(String entityName) {
		entityUpsertCount.increment();
		getEntityStatistics( entityName ).incrementUpsertCount();
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

	@Override
	public void entityCacheRemove(NavigableRole entityName, String regionName) {
		secondLevelCacheMissCount.increment();
		getDomainDataRegionStatistics( regionName ).incrementRemoveCount();
		getEntityStatistics( entityName.getFullPath() ).incrementCacheRemoveCount();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Collection stats

	@Override
	public String[] getCollectionRoleNames() {
		return allCollectionRoles;
	}

	@Override
	public CollectionStatisticsImpl getCollectionStatistics(String role) {
		return NullnessUtil.castNonNull(
					collectionStatsMap.getOrCompute(
						role,
						this::instantiateCollectionStatistics
					)
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
		return NullnessUtil.castNonNull(
					naturalIdQueryStatsMap.getOrCompute(
						rootEntityName,
						this::instantiateNaturalStatistics
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
	public @Nullable String getNaturalIdQueryExecutionMaxTimeRegion() {
		return naturalIdQueryExecutionMaxTimeRegion;
	}

	@Override
	public @Nullable String getNaturalIdQueryExecutionMaxTimeEntity() {
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
	}

	@Override
	public void naturalIdCacheHit(
			NavigableRole rootEntityName,
			String regionName) {
		naturalIdCacheHitCount.increment();

		getDomainDataRegionStatistics( regionName ).incrementHitCount();

		getNaturalIdStatistics( rootEntityName.getFullPath() ).incrementCacheHitCount();
	}

	@Override
	public void naturalIdCacheMiss(
			NavigableRole rootEntityName,
			String regionName) {
		naturalIdCacheMissCount.increment();

		getDomainDataRegionStatistics( regionName ).incrementMissCount();

		getNaturalIdStatistics( rootEntityName.getFullPath() ).incrementCacheMissCount();
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

		final EntityPersister rootEntityPersister = metamodel.getEntityDescriptor( rootEntityName );

		getNaturalIdStatistics( rootEntityName ).queryExecuted( time );

		if ( isLongestQuery && rootEntityPersister.hasNaturalIdCache() ) {
			naturalIdQueryExecutionMaxTimeRegion
					= rootEntityPersister.getNaturalIdCacheAccessStrategy().getRegion().getName();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Second-level cache region stats

	@Override
	public String[] getSecondLevelCacheRegionNames() {
		return cache.getCacheRegionNames().toArray( new String[0] );
	}

	@Override
	public CacheRegionStatisticsImpl getDomainDataRegionStatistics(String regionName) {
		return NullnessUtil.castNonNull(
					l2CacheStatsMap.getOrCompute(
						regionName,
						this::instantiateCacheRegionStatistics
					)
		);
	}

	@Override
	public @Nullable CacheRegionStatisticsImpl getQueryRegionStatistics(final String regionName) {
		return l2CacheStatsMap.getOrCompute(
				regionName,

				new Function<>() {
					@Override
					public @Nullable CacheRegionStatisticsImpl apply(String regionName1) {
						return StatisticsImpl.this.computeQueryRegionStatistics( regionName1 );
					}
				}
		);
	}

	private @Nullable CacheRegionStatisticsImpl computeQueryRegionStatistics(final String regionName) {
		final QueryResultsCache regionAccess = cache.getQueryResultsCacheStrictly( regionName );
		if ( regionAccess == null ) {
			return null; //this null value will be cached
		}
		else {
			return new CacheRegionStatisticsImpl( regionAccess.getRegion() );
		}
	}


	@Override
	public @Nullable CacheRegionStatisticsImpl getCacheRegionStatistics(String regionName) {
		if ( ! secondLevelCacheEnabled ) {
			return null;
		}

		return l2CacheStatsMap.getOrCompute(
				regionName,
				new Function<>() {
					@Override
					public @Nullable CacheRegionStatisticsImpl apply(String regionName1) {
						return StatisticsImpl.this.createCacheRegionStatistics( regionName1 );
					}
				}
		);
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
		return NullnessUtil.castNonNull(
					queryStatsMap.getOrCompute(
						queryString,
						QueryStatisticsImpl::new
					)
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
	public @Nullable String getQueryExecutionMaxTimeQueryString() {
		return queryExecutionMaxTimeQueryString;
	}

	@Override
	public long getQueryExecutionMaxTime() {
		return queryExecutionMaxTime.get();
	}

	@Override
	public void queryExecuted(String hql, int rows, long time) {
		log.hql( hql, time, (long) rows );
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
		queryCacheHitCount.increment();

		getQueryRegionStats( regionName ).incrementHitCount();

		if ( hql != null ) {
			getQueryStatistics( hql ).incrementCacheHitCount();
		}
	}

	@Override
	public void queryCacheMiss(String hql, String regionName) {
		queryCacheMissCount.increment();

		getQueryRegionStats( regionName ).incrementMissCount();

		if ( hql != null ) {
			getQueryStatistics( hql ).incrementCacheMissCount();
		}
	}

	@Override
	public void queryCachePut(String hql, String regionName) {
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
	public void queryPlanCacheHit(String query) {
		queryPlanCacheHitCount.increment();

		if ( query != null ) {
			getQueryStatistics( query ).incrementPlanCacheHitCount();
		}
	}

	@Override
	public void queryPlanCacheMiss(String query) {
		queryPlanCacheMissCount.increment();

		if ( query != null ) {
			getQueryStatistics( query ).incrementPlanCacheMissCount();
		}
	}

	private CacheRegionStatisticsImpl getQueryRegionStats(String regionName) {
		return NullnessUtil.castNonNull(
					l2CacheStatsMap.getOrCompute(
						regionName,
						this::instantiateCacheRegionStatsForQueryResults
					)
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
		log.logStatistics(
				startTime.toEpochMilli(),
				sessionOpenCount.sum(),
				sessionCloseCount.sum(),
				transactionCount.sum(),
				committedTransactionCount.sum(),
				optimisticFailureCount.sum(),
				flushCount.sum(),
				connectCount.sum(),
				prepareStatementCount.sum(),
				closeStatementCount.sum(),
				secondLevelCachePutCount.sum(),
				secondLevelCacheHitCount.sum(),
				secondLevelCacheMissCount.sum(),
				entityLoadCount.sum(),
				entityFetchCount.sum(),
				entityUpdateCount.sum(),
				entityUpsertCount.sum(),
				entityInsertCount.sum(),
				entityDeleteCount.sum(),
				collectionLoadCount.sum(),
				collectionFetchCount.sum(),
				collectionUpdateCount.sum(),
				collectionRemoveCount.sum(),
				collectionRecreateCount.sum(),
				naturalIdQueryExecutionCount.sum(),
				naturalIdCachePutCount.sum(),
				naturalIdCacheHitCount.sum(),
				naturalIdCacheMissCount.sum(),
				naturalIdQueryExecutionMaxTime.get(),
				queryExecutionCount.sum(),
				queryCachePutCount.sum(),
				queryCacheHitCount.sum(),
				queryCacheMissCount.sum(),
				queryExecutionMaxTime.get(),
				updateTimestampsCachePutCount.sum(),
				updateTimestampsCacheHitCount.sum(),
				updateTimestampsCacheMissCount.sum(),
				queryPlanCacheHitCount.sum(),
				queryPlanCacheMissCount.sum()
		);
	}

	@Override
	public String toString() {
		return "Statistics[" +
				"start time=" + startTime +
				",sessions opened=" + sessionOpenCount +
				",sessions closed=" + sessionCloseCount +
				",transactions=" + transactionCount +
				",successful transactions=" + committedTransactionCount +
				",optimistic lock failures=" + optimisticFailureCount +
				",flushes=" + flushCount +
				",connections obtained=" + connectCount +
				",statements prepared=" + prepareStatementCount +
				",statements closed=" + closeStatementCount +
				",second level cache puts=" + secondLevelCachePutCount +
				",second level cache hits=" + secondLevelCacheHitCount +
				",second level cache misses=" + secondLevelCacheMissCount +
				",entities loaded=" + entityLoadCount +
				",entities updated=" + entityUpdateCount +
				",entities upserted=" + entityUpsertCount +
				",entities inserted=" + entityInsertCount +
				",entities deleted=" + entityDeleteCount +
				",entities fetched=" + entityFetchCount +
				",collections loaded=" + collectionLoadCount +
				",collections updated=" + collectionUpdateCount +
				",collections removed=" + collectionRemoveCount +
				",collections recreated=" + collectionRecreateCount +
				",collections fetched=" + collectionFetchCount +
				",naturalId queries executed to database=" + naturalIdQueryExecutionCount +
				",naturalId cache puts=" + naturalIdCachePutCount +
				",naturalId cache hits=" + naturalIdCacheHitCount +
				",naturalId cache misses=" + naturalIdCacheMissCount +
				",naturalId max query time=" + naturalIdQueryExecutionMaxTime +
				",queries executed to database=" + queryExecutionCount +
				",query cache puts=" + queryCachePutCount +
				",query cache hits=" + queryCacheHitCount +
				",query cache misses=" + queryCacheMissCount +
				",update timestamps cache puts=" + updateTimestampsCachePutCount +
				",update timestamps cache hits=" + updateTimestampsCacheHitCount +
				",update timestamps cache misses=" + updateTimestampsCacheMissCount +
				",max query time=" + queryExecutionMaxTime +
				",query plan cache hits=" + queryPlanCacheHitCount +
				",query plan cache misses=" + queryPlanCacheMissCount +
				']';
	}

	private EntityStatisticsImpl instantiateEntityStatistics(final String entityName) {
		return new EntityStatisticsImpl( metamodel.getEntityDescriptor( entityName ) );
	}

	private CollectionStatisticsImpl instantiateCollectionStatistics(final String role) {
		return new CollectionStatisticsImpl( metamodel.getCollectionDescriptor( role ) );
	}

	private NaturalIdStatisticsImpl instantiateNaturalStatistics(final String entityName) {
		final EntityPersister entityDescriptor = metamodel.getEntityDescriptor( entityName );
		if ( !entityDescriptor.hasNaturalIdentifier() ) {
			throw new IllegalArgumentException( "Given entity [" + entityName + "] does not define natural-id" );
		}
		return new NaturalIdStatisticsImpl( entityDescriptor );
	}

	private CacheRegionStatisticsImpl instantiateCacheRegionStatistics(final String regionName) {
		final Region region = cache.getRegion( regionName );

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

	private CacheRegionStatisticsImpl instantiateCacheRegionStatsForQueryResults(final String regionName) {
		return new CacheRegionStatisticsImpl( cache.getQueryResultsCache( regionName ).getRegion() );
	}

	private @Nullable CacheRegionStatisticsImpl createCacheRegionStatistics(final String regionName) {
		Region region = cache.getRegion( regionName );

		if ( region == null ) {

			if ( !queryCacheEnabled ) {
				return null;
			}

			// this is the pre-5.3 behavior.  and since this is a pre-5.3 method it should behave consistently
			// NOTE that this method is deprecated
			region = cache.getQueryResultsCache( regionName ).getRegion();
		}

		return new CacheRegionStatisticsImpl( region );
	}

	@Override
	public Map<String, Long> getSlowQueries() {
		return slowQueries;
	}

	@Override
	public void slowQuery(String sql, long executionTime) {
		slowQueries.merge( sql, executionTime, Math::max );
	}
}
