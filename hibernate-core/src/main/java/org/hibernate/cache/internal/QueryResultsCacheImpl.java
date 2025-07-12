/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.monitor.spi.DiagnosticEvent;

import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;

/**
 * The standard implementation of the Hibernate QueryCache interface.  Works
 * hind-in-hand with {@link TimestampsCache} to help in recognizing
 * stale query results.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class QueryResultsCacheImpl implements QueryResultsCache {

	private final QueryResultsRegion cacheRegion;
	private final TimestampsCache timestampsCache;

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
			final SharedSessionContractImplementor session) throws HibernateException {
		if ( L2CACHE_LOGGER.isTraceEnabled() ) {
			L2CACHE_LOGGER.tracef( "Caching query results in region: %s; timestamp=%s",
					cacheRegion.getName(),
					session.getCacheTransactionSynchronization().getCachingTimestamp() );
		}

		final CacheItem cacheItem = new CacheItem(
				session.getCacheTransactionSynchronization().getCachingTimestamp(),
				deepCopy( results )
		);

		final EventMonitor eventMonitor = session.getEventMonitor();
		final DiagnosticEvent cachePutEvent = eventMonitor.beginCachePutEvent();
		try {
			session.getEventListenerManager().cachePutStart();
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
			session.getEventListenerManager().cachePutEnd();
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
			final SharedSessionContractImplementor session) throws HibernateException {
		final boolean loggerTraceEnabled = L2CACHE_LOGGER.isTraceEnabled();
		if ( loggerTraceEnabled ) {
			L2CACHE_LOGGER.tracef( "Checking cached query results in region: %s", cacheRegion.getName() );
		}

		final CacheItem cacheItem = getCachedData( key, session );
		if ( cacheItem == null ) {
			if ( loggerTraceEnabled ) {
				L2CACHE_LOGGER.trace( "Query results were not found in cache" );
			}
			return null;
		}

		if ( !timestampsCache.isUpToDate( spaces, cacheItem.timestamp, session ) ) {
			if ( loggerTraceEnabled ) {
				L2CACHE_LOGGER.trace( "Cached query results were not up-to-date" );
			}
			return null;
		}

		if ( loggerTraceEnabled ) {
			L2CACHE_LOGGER.trace( "Returning cached query results" );
		}

		// No need to copy results, since consumers will never mutate
		return cacheItem.results;
	}

	@Override
	public List<?> get(
			final QueryKey key,
			final String[] spaces,
			final SharedSessionContractImplementor session) throws HibernateException {
		final boolean loggerTraceEnabled = L2CACHE_LOGGER.isTraceEnabled();
		if ( loggerTraceEnabled ) {
			L2CACHE_LOGGER.tracef( "Checking cached query results in region: %s", cacheRegion.getName() );
		}

		final CacheItem cacheItem = getCachedData( key, session );
		if ( cacheItem == null ) {
			if ( loggerTraceEnabled ) {
				L2CACHE_LOGGER.trace( "Query results were not found in cache" );
			}
			return null;
		}

		if ( !timestampsCache.isUpToDate( spaces, cacheItem.timestamp, session ) ) {
			if ( loggerTraceEnabled ) {
				L2CACHE_LOGGER.trace( "Cached query results were not up-to-date" );
			}
			return null;
		}

		if ( loggerTraceEnabled ) {
			L2CACHE_LOGGER.trace( "Returning cached query results" );
		}

		return deepCopy( cacheItem.results );
	}

	private CacheItem getCachedData(QueryKey key, SharedSessionContractImplementor session) {
		CacheItem cachedItem = null;
		final EventMonitor eventMonitor = session.getEventMonitor();
		final DiagnosticEvent cacheGetEvent = eventMonitor.beginCacheGetEvent();
		try {
			session.getEventListenerManager().cacheGetStart();
			cachedItem = (CacheItem) cacheRegion.getFromCache( key, session );
		}
		finally {
			eventMonitor.completeCacheGetEvent(
					cacheGetEvent,
					session,
					cacheRegion,
					cachedItem != null
			);
			session.getEventListenerManager().cacheGetEnd( cachedItem != null );
		}
		return cachedItem;
	}

	@Override
	public String toString() {
		return "QueryResultsCache(" + cacheRegion.getName() + ')';
	}

	static class CacheItem implements Serializable {
		private final Long timestamp;
		private final List<?> results;

		CacheItem(long timestamp, List<?> results) {
			this.timestamp = Long.valueOf( timestamp );
			this.results = results;
		}
	}
}
