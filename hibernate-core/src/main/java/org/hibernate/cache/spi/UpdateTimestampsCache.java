/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.spi;

import java.io.Serializable;
import java.util.Properties;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Tracks the timestamps of the most recent updates to particular tables. It is
 * important that the cache timeout of the underlying cache implementation be set
 * to a higher value than the timeouts of any of the query caches. In fact, we
 * recommend that the the underlying cache not be configured for expiry at all.
 * Note, in particular, that an LRU cache expiry policy is never appropriate.
 *
 * @author Gavin King
 * @author Mikheil Kapanadze
 */
public class UpdateTimestampsCache {

	public static final String REGION_NAME = UpdateTimestampsCache.class.getName();
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, UpdateTimestampsCache.class.getName() );

	private final SessionFactoryImplementor factory;
	private final TimestampsRegion region;

	public UpdateTimestampsCache(Settings settings, Properties props, final SessionFactoryImplementor factory) throws HibernateException {
		this.factory = factory;
		final String prefix = settings.getCacheRegionPrefix();
		final String regionName = prefix == null ? REGION_NAME : prefix + '.' + REGION_NAME;

		LOG.startingUpdateTimestampsCache( regionName );
		this.region = settings.getRegionFactory().buildTimestampsRegion( regionName, props );
	}

    @SuppressWarnings({"UnusedDeclaration"})
    public UpdateTimestampsCache(Settings settings, Properties props) throws HibernateException {
        this( settings, props, null );
    }

	@SuppressWarnings({"UnnecessaryBoxing"})
	public void preinvalidate(Serializable[] spaces) throws CacheException {
		final boolean debug = LOG.isDebugEnabled();
		final boolean stats = factory != null && factory.getStatistics().isStatisticsEnabled();

		final Long ts = region.nextTimestamp() + region.getTimeout();

		for ( Serializable space : spaces ) {
			if ( debug ) {
				LOG.debugf( "Pre-invalidating space [%s], timestamp: %s", space, ts );
			}
			//put() has nowait semantics, is this really appropriate?
			//note that it needs to be async replication, never local or sync
			region.put( space, ts );
			if ( stats ) {
				factory.getStatisticsImplementor().updateTimestampsCachePut();
			}
		}
	}

	@SuppressWarnings({"UnnecessaryBoxing"})
	public void invalidate(Serializable[] spaces) throws CacheException {
		 final boolean debug = LOG.isDebugEnabled();
		 final boolean stats = factory != null && factory.getStatistics().isStatisticsEnabled();

		 final Long ts = region.nextTimestamp();

		for (Serializable space : spaces) {
			if ( debug ) {
				LOG.debugf( "Invalidating space [%s], timestamp: %s", space, ts );
			}
			//put() has nowait semantics, is this really appropriate?
			//note that it needs to be async replication, never local or sync
			region.put( space, ts );
			if ( stats ) {
				factory.getStatisticsImplementor().updateTimestampsCachePut();
			}
		}
	}

	@SuppressWarnings({"unchecked", "UnnecessaryUnboxing"})
	public boolean isUpToDate(Set spaces, Long timestamp) throws HibernateException {
		final boolean debug = LOG.isDebugEnabled();
		final boolean stats = factory != null && factory.getStatistics().isStatisticsEnabled();

		for ( Serializable space : (Set<Serializable>) spaces ) {
			Long lastUpdate = (Long) region.get( space );
			if ( lastUpdate == null ) {
				if ( stats ) {
					factory.getStatisticsImplementor().updateTimestampsCacheMiss();
				}
				//the last update timestamp was lost from the cache
				//(or there were no updates since startup!)
				//updateTimestamps.put( space, new Long( updateTimestamps.nextTimestamp() ) );
				//result = false; // safer
			}
			else {
				if ( debug ) {
					LOG.debugf(
							"[%s] last update timestamp: %s",
							space,
							lastUpdate + ", result set timestamp: " + timestamp
					);
				}
				if ( stats ) {
					factory.getStatisticsImplementor().updateTimestampsCacheHit();
				}
				if ( lastUpdate >= timestamp ) {
					return false;
				}
			}
		}
		return true;
	}

	public void clear() throws CacheException {
		region.evictAll();
	}

	public void destroy() {
		try {
			region.destroy();
		}
		catch (Exception e) {
			LOG.unableToDestroyUpdateTimestampsCache( region.getName(), e.getMessage() );
		}
	}

	public TimestampsRegion getRegion() {
		return region;
	}

	@Override
    public String toString() {
		return "UpdateTimestampsCache";
	}

}
