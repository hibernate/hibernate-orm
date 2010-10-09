/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.cache;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Settings;

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
	private static final Logger log = LoggerFactory.getLogger( UpdateTimestampsCache.class );

	private final TimestampsRegion region;

	public UpdateTimestampsCache(Settings settings, Properties props) throws HibernateException {
		String prefix = settings.getCacheRegionPrefix();
		String regionName = prefix == null ? REGION_NAME : prefix + '.' + REGION_NAME;
		log.info( "starting update timestamps cache at region: " + regionName );
		this.region = settings.getRegionFactory().buildTimestampsRegion( regionName, props );
	}

	public synchronized void preinvalidate(Serializable[] spaces) throws CacheException {
		//TODO: to handle concurrent writes correctly, this should return a Lock to the client
		Long ts = new Long( region.nextTimestamp() + region.getTimeout() );
		for ( int i=0; i<spaces.length; i++ ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "Pre-invalidating space [" + spaces[i] + "]" );
			}
			//put() has nowait semantics, is this really appropriate?
			//note that it needs to be async replication, never local or sync
			region.put( spaces[i], ts );
		}
		//TODO: return new Lock(ts);
	}

	 public synchronized void invalidate(Serializable[] spaces) throws CacheException {
	 	//TODO: to handle concurrent writes correctly, the client should pass in a Lock
		Long ts = new Long( region.nextTimestamp() );
		//TODO: if lock.getTimestamp().equals(ts)
		for ( int i=0; i<spaces.length; i++ ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "Invalidating space [" + spaces[i] + "], timestamp: " + ts);
			}
			//put() has nowait semantics, is this really appropriate?
			//note that it needs to be async replication, never local or sync
			region.put( spaces[i], ts );
		}
	}

	public synchronized boolean isUpToDate(Set spaces, Long timestamp) throws HibernateException {
		Iterator iter = spaces.iterator();
		while ( iter.hasNext() ) {
			Serializable space = (Serializable) iter.next();
			Long lastUpdate = (Long) region.get(space);
			if ( lastUpdate==null ) {
				//the last update timestamp was lost from the cache
				//(or there were no updates since startup!)
				//updateTimestamps.put( space, new Long( updateTimestamps.nextTimestamp() ) );
				//result = false; // safer
			}
			else {
				if ( log.isDebugEnabled() ) {
					log.debug("[" + space + "] last update timestamp: " + lastUpdate + ", result set timestamp: " + timestamp );
				}
				if ( lastUpdate.longValue() >= timestamp.longValue() ) {
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
			log.warn("could not destroy UpdateTimestamps cache", e);
		}
	}

	public TimestampsRegion getRegion() {
		return region;
	}
	
	public String toString() {
		return "UpdateTimestampeCache";
	}

}
