/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import java.util.Collection;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.stat.spi.StatisticsImplementor;

import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;
import static org.hibernate.event.monitor.spi.EventMonitor.CacheActionDescription.TIMESTAMP_INVALIDATE;
import static org.hibernate.event.monitor.spi.EventMonitor.CacheActionDescription.TIMESTAMP_PRE_INVALIDATE;

/**
 * Standard implementation of TimestampsCache
 *
 * @author Steve Ebersole
 */
public class TimestampsCacheEnabledImpl implements TimestampsCache {

	private final TimestampsRegion timestampsRegion;

	public TimestampsCacheEnabledImpl(TimestampsRegion timestampsRegion) {
		this.timestampsRegion = timestampsRegion;
	}

	@Override
	public TimestampsRegion getRegion() {
		return timestampsRegion;
	}

	@Override
	public void preInvalidate(
			String[] spaces,
			SharedSessionContractImplementor session) {
		final SessionFactoryImplementor factory = session.getFactory();
		final RegionFactory regionFactory = factory.getCache().getRegionFactory();
		final StatisticsImplementor statistics = factory.getStatistics();
		final boolean stats = statistics.isStatisticsEnabled();

		final Long timestamp = regionFactory.nextTimestamp() + regionFactory.getTimeout();

		final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
		final EventMonitor eventMonitor = session.getEventMonitor();
		final boolean traceEnabled = L2CACHE_LOGGER.isTraceEnabled();
		for ( String space : spaces ) {
			if ( traceEnabled ) {
				L2CACHE_LOGGER.preInvalidatingSpace( space, timestamp );
			}
			final DiagnosticEvent cachePutEvent = eventMonitor.beginCachePutEvent();
			try {
				eventListenerManager.cachePutStart();

				//put() has nowait semantics, is this really appropriate?
				//note that it needs to be async replication, never local or sync
				timestampsRegion.putIntoCache( space, timestamp, session );
			}
			finally {
				eventMonitor.completeCachePutEvent(
						cachePutEvent,
						session,
						timestampsRegion,
						true,
						TIMESTAMP_PRE_INVALIDATE
				);
				eventListenerManager.cachePutEnd();
			}

			if ( stats ) {
				statistics.updateTimestampsCachePut();
			}
		}
	}

	@Override
	public void invalidate(
			String[] spaces,
			SharedSessionContractImplementor session) {
		final SessionFactoryImplementor factory = session.getFactory();
		final StatisticsImplementor statistics = factory.getStatistics();
		final boolean stats = statistics.isStatisticsEnabled();

		final Long timestamp = factory.getCache().getRegionFactory().nextTimestamp();

		final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
		final EventMonitor eventMonitor = session.getEventMonitor();
		final boolean traceEnabled = L2CACHE_LOGGER.isTraceEnabled();
		for ( String space : spaces ) {
			if ( traceEnabled ) {
				L2CACHE_LOGGER.invalidatingSpace( space, timestamp );
			}

			final DiagnosticEvent cachePutEvent = eventMonitor.beginCachePutEvent();
			try {
				eventListenerManager.cachePutStart();
				timestampsRegion.putIntoCache( space, timestamp, session );
			}
			finally {
				eventMonitor.completeCachePutEvent(
						cachePutEvent,
						session,
						timestampsRegion,
						true,
						TIMESTAMP_INVALIDATE
				);
				eventListenerManager.cachePutEnd();

				if ( stats ) {
					statistics.updateTimestampsCachePut();
				}
			}
		}
	}

	@Override
	public boolean isUpToDate(
			String[] spaces,
			Long timestamp,
			SharedSessionContractImplementor session) {
		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		for ( String space : spaces ) {
			if ( isSpaceOutOfDate( space, timestamp, session, statistics ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean isSpaceOutOfDate(
			String space,
			Long timestamp,
			SharedSessionContractImplementor session,
			StatisticsImplementor statistics) {
		final Long lastUpdate = getLastUpdateTimestampForSpace( space, session );
		if ( lastUpdate == null ) {
			// the last update timestamp for the given space was evicted from the
			// cache or there have been no writes to it since startup
			if ( statistics.isStatisticsEnabled() ) {
				statistics.updateTimestampsCacheMiss();
			}
			return false;
		}
		else {
			L2CACHE_LOGGER.lastUpdateTimestampForSpace( space, lastUpdate, timestamp );
			if ( statistics.isStatisticsEnabled() ) {
				statistics.updateTimestampsCacheHit();
			}
			return lastUpdate >= timestamp;
		}
	}

	@Override
	public boolean isUpToDate(
			Collection<String> spaces,
			Long timestamp,
			SharedSessionContractImplementor session) {
		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		for ( String space : spaces ) {
			if ( isSpaceOutOfDate( space, timestamp, session, statistics ) ) {
				return false;
			}
		}
		return true;
	}

	private Long getLastUpdateTimestampForSpace(String space, SharedSessionContractImplementor session) {
		boolean found = false;
		final EventMonitor eventMonitor = session.getEventMonitor();
		final DiagnosticEvent cacheGetEvent = eventMonitor.beginCacheGetEvent();
		try {
			session.getEventListenerManager().cacheGetStart();
			final Long timestamp = (Long) timestampsRegion.getFromCache( space, session );
			found = timestamp != null;
			return timestamp;
		}
		finally {
			eventMonitor.completeCacheGetEvent( cacheGetEvent, session, timestampsRegion, found );
			session.getEventListenerManager().cacheGetEnd( found );
		}
	}

}
