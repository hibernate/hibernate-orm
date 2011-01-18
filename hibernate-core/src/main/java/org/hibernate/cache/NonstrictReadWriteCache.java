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

import java.util.Comparator;
import org.hibernate.Logger;
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

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class, Logger.class.getPackage().getName());

	private Cache cache;

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
        LOG.debug("Cache lookup: " + key);

		Object result = cache.get( key );
        if (result != null) LOG.debug("Cache hit: " + key);
        else LOG.debug("Cache miss: " + key);
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
            LOG.debug("Item already cached: " + key);
			return false;
		}
        LOG.debug("Caching: " + key);

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
        LOG.debug("Removing: " + key);
		cache.remove( key );
	}

	public void clear() throws CacheException {
        LOG.debug("Clearing");
		cache.clear();
	}

	public void destroy() {
		try {
			cache.destroy();
		}
		catch ( Exception e ) {
            LOG.unableToDestroyCache(e.getMessage());
		}
	}

	/**
	 * Invalidate the item
	 */
	public void evict(Object key) throws CacheException {
        LOG.debug("Invalidating: " + key);
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
        LOG.debug("Invalidating: " + key);
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

	@Override
    public String toString() {
		return cache + "(nonstrict-read-write)";
	}
}
