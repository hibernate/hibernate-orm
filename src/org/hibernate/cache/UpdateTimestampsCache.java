//$Id$
package org.hibernate.cache;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

	private static final Log log = LogFactory.getLog(UpdateTimestampsCache.class);

	private Cache updateTimestamps;
	private final String regionName;

	public void clear() throws CacheException {
		updateTimestamps.clear();
	}

	public UpdateTimestampsCache(Settings settings, Properties props) throws HibernateException {
		String prefix = settings.getCacheRegionPrefix();
		regionName = prefix == null ? REGION_NAME : prefix + '.' + REGION_NAME;
		log.info( "starting update timestamps cache at region: " + regionName );
		this.updateTimestamps = settings.getCacheProvider().buildCache( regionName, props );
	}

	public synchronized void preinvalidate(Serializable[] spaces) throws CacheException {
		//TODO: to handle concurrent writes correctly, this should return a Lock to the client
		Long ts = new Long( updateTimestamps.nextTimestamp() + updateTimestamps.getTimeout() );
		for ( int i=0; i<spaces.length; i++ ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "Pre-invalidating space [" + spaces[i] + "]" );
			}
			//put() has nowait semantics, is this really appropriate?
			//note that it needs to be async replication, never local or sync
			updateTimestamps.put( spaces[i], ts );
		}
		//TODO: return new Lock(ts);
	}

	 public synchronized void invalidate(Serializable[] spaces) throws CacheException {
	 	//TODO: to handle concurrent writes correctly, the client should pass in a Lock
		Long ts = new Long( updateTimestamps.nextTimestamp() );
		//TODO: if lock.getTimestamp().equals(ts)
		for ( int i=0; i<spaces.length; i++ ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "Invalidating space [" + spaces[i] + "], timestamp: " + ts);
			}
			//put() has nowait semantics, is this really appropriate?
			//note that it needs to be async replication, never local or sync
			updateTimestamps.put( spaces[i], ts );
		}
	}

	public synchronized boolean isUpToDate(Set spaces, Long timestamp) throws HibernateException {
		Iterator iter = spaces.iterator();
		while ( iter.hasNext() ) {
			Serializable space = (Serializable) iter.next();
			Long lastUpdate = (Long) updateTimestamps.get(space);
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

	public void destroy() {
		try {
			updateTimestamps.destroy();
		}
		catch (Exception e) {
			log.warn("could not destroy UpdateTimestamps cache", e);
		}
	}

	public Cache getCache() {
		return updateTimestamps;
	}
	
	public String getRegionName() {
		return regionName;
	}
	
	public String toString() {
		return "UpdateTimestampeCache";
	}

}
