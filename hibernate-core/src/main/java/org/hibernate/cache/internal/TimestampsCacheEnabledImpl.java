/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
	@Nonnull
	public TimestampsRegion getRegion() {
		return timestampsRegion;
	}

	@Override
	public void preInvalidate(
			@Nonnull String[] spaces,
			@Nonnull SharedSessionContractImplementor session) {
		final var factory = session.getFactory();
		final var regionFactory = factory.getCache().getRegionFactory();
		final var statistics = factory.getStatistics();
		final boolean stats = statistics.isStatisticsEnabled();

		final Long timestamp = regionFactory.nextTimestamp() + regionFactory.getTimeout();

		final var eventListenerManager = session.getEventListenerManager();
		final var eventMonitor = session.getEventMonitor();
		final boolean traceEnabled = L2CACHE_LOGGER.isTraceEnabled();
		for ( String space : spaces ) {
			if ( traceEnabled ) {
				L2CACHE_LOGGER.preInvalidatingSpace( space, timestamp );
			}
			final var cachePutEvent = eventMonitor.beginCachePutEvent();
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
			@Nonnull String[] spaces,
			@Nonnull SharedSessionContractImplementor session) {
		final var factory = session.getFactory();
		final var statistics = factory.getStatistics();
		final boolean stats = statistics.isStatisticsEnabled();

		final Long timestamp = factory.getCache().getRegionFactory().nextTimestamp();

		final var eventListenerManager = session.getEventListenerManager();
		final var eventMonitor = session.getEventMonitor();
		final boolean traceEnabled = L2CACHE_LOGGER.isTraceEnabled();
		for ( String space : spaces ) {
			if ( traceEnabled ) {
				L2CACHE_LOGGER.invalidatingSpace( space, timestamp );
			}

			final var cachePutEvent = eventMonitor.beginCachePutEvent();
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
			@Nonnull String[] spaces,
			@Nonnull Long timestamp,
			@Nonnull SharedSessionContractImplementor session) {
		final var statistics = session.getFactory().getStatistics();
		for ( String space : spaces ) {
			if ( isSpaceOutOfDate( space, timestamp, session, statistics ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean isSpaceOutOfDate(
			@Nonnull String space,
			@Nonnull Long timestamp,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull StatisticsImplementor statistics) {
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
			@Nonnull Collection<String> spaces,
			@Nonnull Long timestamp,
			@Nonnull SharedSessionContractImplementor session) {
		final var statistics = session.getFactory().getStatistics();
		for ( String space : spaces ) {
			if ( isSpaceOutOfDate( space, timestamp, session, statistics ) ) {
				return false;
			}
		}
		return true;
	}

	@Nullable
	private Long getLastUpdateTimestampForSpace(
			@Nonnull String space,
			@Nonnull SharedSessionContractImplementor session) {
		boolean found = false;
		final var eventMonitor = session.getEventMonitor();
		final var eventListenerManager = session.getEventListenerManager();
		final var cacheGetEvent = eventMonitor.beginCacheGetEvent();
		try {
			eventListenerManager.cacheGetStart();
			final Long timestamp = (Long) timestampsRegion.getFromCache( space, session );
			found = timestamp != null;
			return timestamp;
		}
		finally {
			eventMonitor.completeCacheGetEvent( cacheGetEvent, session, timestampsRegion, found );
			eventListenerManager.cacheGetEnd( found );
		}
	}

	@Override
	public void clear() throws CacheException {
		getRegion().clear();
	}
}
