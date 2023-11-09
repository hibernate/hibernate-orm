/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import java.util.Collection;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateEvent;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.jboss.logging.Logger;

/**
 * Standard implementation of TimestampsCache
 *
 * @author Steve Ebersole
 */
public class TimestampsCacheEnabledImpl implements TimestampsCache {
	private static final Logger log = Logger.getLogger( TimestampsCacheEnabledImpl.class );

	public static final boolean DEBUG_ENABLED = log.isDebugEnabled();

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

		final Long ts = regionFactory.nextTimestamp() + regionFactory.getTimeout();

		final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
		final boolean debugEnabled = log.isDebugEnabled();

		for ( String space : spaces ) {
			if ( debugEnabled ) {
				log.debugf( "Pre-invalidating space [%s], timestamp: %s", space, ts );
			}
			final EventManager eventManager = session.getEventManager();
			final HibernateEvent cachePutEvent = eventManager.beginCachePutEvent();
			try {
				eventListenerManager.cachePutStart();

				//put() has nowait semantics, is this really appropriate?
				//note that it needs to be async replication, never local or sync
				timestampsRegion.putIntoCache( space, ts, session );
			}
			finally {
				eventManager.completeCachePutEvent(
						cachePutEvent,
						session,
						timestampsRegion,
						true,
						EventManager.CacheActionDescription.TIMESTAMP_PRE_INVALIDATE
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
		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		final boolean stats = statistics.isStatisticsEnabled();

		final Long ts = session.getFactory().getCache().getRegionFactory().nextTimestamp();
		final boolean debugEnabled = log.isDebugEnabled();

		for ( String space : spaces ) {
			if ( debugEnabled ) {
				log.debugf( "Invalidating space [%s], timestamp: %s", space, ts );
			}

			final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
			final EventManager eventManager = session.getEventManager();
			final HibernateEvent cachePutEvent = eventManager.beginCachePutEvent();
			try {
				eventListenerManager.cachePutStart();
				timestampsRegion.putIntoCache( space, ts, session );
			}
			finally {
				eventManager.completeCachePutEvent(
						cachePutEvent,
						session,
						timestampsRegion,
						true,
						EventManager.CacheActionDescription.TIMESTAMP_INVALIDATE
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
		}
		else {
			if ( DEBUG_ENABLED ) {
				log.debugf(
						"[%s] last update timestamp: %s",
						space,
						lastUpdate + ", result set timestamp: " + timestamp
				);
			}

			if ( statistics.isStatisticsEnabled() ) {
				statistics.updateTimestampsCacheHit();
			}

			//noinspection RedundantIfStatement
			if ( lastUpdate >= timestamp ) {
				return true;
			}
		}

		return false;
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
		Long ts = null;
		final EventManager eventManager = session.getEventManager();
		final HibernateEvent cacheGetEvent = eventManager.beginCacheGetEvent();
		try {
			session.getEventListenerManager().cacheGetStart();
			ts = (Long) timestampsRegion.getFromCache( space, session );
		}
		finally {
			eventManager.completeCacheGetEvent(
					cacheGetEvent,
					session,
					timestampsRegion,
					ts != null
			);
			session.getEventListenerManager().cacheGetEnd( ts != null );
		}
		return ts;
	}

}
