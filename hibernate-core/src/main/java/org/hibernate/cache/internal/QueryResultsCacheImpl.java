/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;

import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;

/**
 * The standard implementation of the {@link QueryResultsCache} interface.
 * Works in conjunction with {@link TimestampsCache} to help in recognizing
 * stale query results.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class QueryResultsCacheImpl implements QueryResultsCache {

	private final QueryResultsRegion cacheRegion;
	private final TimestampsCache timestampsCache;

	private record CacheItem(long timestamp, List<?> results)
			implements Serializable {
	}

	QueryResultsCacheImpl(
			QueryResultsRegion cacheRegion,
			TimestampsCache timestampsCache) {
		this.cacheRegion = cacheRegion;
		this.timestampsCache = timestampsCache;
	}

	@Override
	public QueryResultsRegion getRegion() {
		return cacheRegion;
	}

	@Override
	public boolean put(
			final QueryKey key,
			final List<?> results,
			final SharedSessionContractImplementor session) {
		final var synchronization = session.getCacheTransactionSynchronization();
		if ( L2CACHE_LOGGER.isTraceEnabled() ) {
			L2CACHE_LOGGER.cachingQueryResults(
					cacheRegion.getName(),
					synchronization.getCachingTimestamp() );
		}
		final var eventMonitor = session.getEventMonitor();
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		final var listenerManager = session.getEventListenerManager();
		final var cacheItem = new CacheItem( synchronization.getCachingTimestamp(), deepCopy( results ) );
		try {
			listenerManager.cachePutStart();
			cacheRegion.putIntoCache( key, cacheItem, session );
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cacheRegion,
					true,
					EventMonitor.CacheActionDescription.QUERY_RESULT
			);
			listenerManager.cachePutEnd();
		}
		return true;
	}

	private static <T> List<T> deepCopy(List<T> results) {
		return new ArrayList<>( results );
	}

	@Override
	public List<?> get(
			final QueryKey key,
			final Set<String> spaces,
			final SharedSessionContractImplementor session) {
		final boolean loggerTraceEnabled = L2CACHE_LOGGER.isTraceEnabled();
		if ( loggerTraceEnabled ) {
			L2CACHE_LOGGER.checkingCachedQueryResults( cacheRegion.getName() );
		}

		final var cacheItem = getCachedData( key, session );
		if ( cacheItem == null ) {
			if ( loggerTraceEnabled ) {
				L2CACHE_LOGGER.queryResultsNotFound();
			}
			return null;
		}

		if ( !timestampsCache.isUpToDate( spaces, cacheItem.timestamp, session ) ) {
			if ( loggerTraceEnabled ) {
				L2CACHE_LOGGER.cachedQueryResultsStale();
			}
			return null;
		}

		if ( loggerTraceEnabled ) {
			L2CACHE_LOGGER.returningCachedQueryResults();
		}

		// No need to copy results, since consumers will never mutate
		return cacheItem.results;
	}

	@Override
	public List<?> get(
			final QueryKey key,
			final String[] spaces,
			final SharedSessionContractImplementor session) {
		final boolean loggerTraceEnabled = L2CACHE_LOGGER.isTraceEnabled();

		if ( loggerTraceEnabled ) {
			L2CACHE_LOGGER.checkingCachedQueryResults( cacheRegion.getName() );
		}

		final var cacheItem = getCachedData( key, session );
		if ( cacheItem == null ) {
			if ( loggerTraceEnabled ) {
				L2CACHE_LOGGER.queryResultsNotFound();
			}
			return null;
		}

		if ( !timestampsCache.isUpToDate( spaces, cacheItem.timestamp, session ) ) {
			if ( loggerTraceEnabled ) {
				L2CACHE_LOGGER.cachedQueryResultsStale();
			}
			return null;
		}

		if ( loggerTraceEnabled ) {
			L2CACHE_LOGGER.returningCachedQueryResults();
		}

		return deepCopy( cacheItem.results );
	}

	private CacheItem getCachedData(QueryKey key, SharedSessionContractImplementor session) {
		final var eventMonitor = session.getEventMonitor();
		final var cacheGetEvent = eventMonitor.beginCacheGetEvent();
		final var eventListenerManager = session.getEventListenerManager();
		boolean success = false;
		try {
			eventListenerManager.cacheGetStart();
			final var item = (CacheItem) cacheRegion.getFromCache( key, session );
			success = item != null;
			return item;
		}
		finally {
			eventMonitor.completeCacheGetEvent(
					cacheGetEvent,
					session,
					cacheRegion,
					success
			);
			eventListenerManager.cacheGetEnd( success );
		}
	}

	@Override
	public String toString() {
		return "QueryResultsCache(" + cacheRegion.getName() + ')';
	}
}
