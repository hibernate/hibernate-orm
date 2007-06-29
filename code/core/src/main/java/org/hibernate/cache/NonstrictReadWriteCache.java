//$Id: NonstrictReadWriteCache.java 11398 2007-04-10 14:54:07Z steve.ebersole@jboss.com $
package org.hibernate.cache;

import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.cache.access.SoftLock;

/**
 * Caches data that is sometimes updated without ever locking the cache.
 * If concurrent access to an item is possible, this concurrency strategy
 * makes no guarantee that the item returned from the cache is the latest
 * version available in the database. Configure your cache timeout accordingly!
 * This is an "asynchronous" concurrency strategy.
 *
 * @author Gavin King
 * @see ReadWriteCache for a much stricter algorithm
 */
public class NonstrictReadWriteCache implements CacheConcurrencyStrategy {

	private Cache cache;

	private static final Log log = LogFactory.getLog( NonstrictReadWriteCache.class );

	public NonstrictReadWriteCache() {
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	public Cache getCache() {
		return cache;
	}

	/**
	 * Get the most recent version, if available.
	 */
	public Object get(Object key, long txTimestamp) throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "Cache lookup: " + key );
		}

		Object result = cache.get( key );
		if ( result != null ) {
			log.debug( "Cache hit" );
		}
		else {
			log.debug( "Cache miss" );
		}
		return result;
	}

	/**
	 * Add an item to the cache.
	 */
	public boolean put(
			Object key,
	        Object value,
	        long txTimestamp,
	        Object version,
	        Comparator versionComparator,
	        boolean minimalPut) throws CacheException {
		if ( minimalPut && cache.get( key ) != null ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "item already cached: " + key );
			}
			return false;
		}
		if ( log.isDebugEnabled() ) {
			log.debug( "Caching: " + key );
		}

		cache.put( key, value );
		return true;

	}

	/**
	 * Do nothing.
	 *
	 * @return null, no lock
	 */
	public SoftLock lock(Object key, Object version) throws CacheException {
		return null;
	}

	public void remove(Object key) throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "Removing: " + key );
		}
		cache.remove( key );
	}

	public void clear() throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "Clearing" );
		}
		cache.clear();
	}

	public void destroy() {
		try {
			cache.destroy();
		}
		catch ( Exception e ) {
			log.warn( "could not destroy cache", e );
		}
	}

	/**
	 * Invalidate the item
	 */
	public void evict(Object key) throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "Invalidating: " + key );
		}

		cache.remove( key );
	}

	/**
	 * Invalidate the item
	 */
	public boolean insert(Object key, Object value, Object currentVersion) {
		return false;
	}

	/**
	 * Do nothing.
	 */
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) {
		evict( key );
		return false;
	}

	/**
	 * Invalidate the item (again, for safety).
	 */
	public void release(Object key, SoftLock lock) throws CacheException {
		if ( log.isDebugEnabled() ) {
			log.debug( "Invalidating (again): " + key );
		}

		cache.remove( key );
	}

	/**
	 * Invalidate the item (again, for safety).
	 */
	public boolean afterUpdate(Object key, Object value, Object version, SoftLock lock) throws CacheException {
		release( key, lock );
		return false;
	}

	/**
	 * Do nothing.
	 */
	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
		return false;
	}

	public String getRegionName() {
		return cache.getRegionName();
	}

	public String toString() {
		return cache + "(nonstrict-read-write)";
	}
}
