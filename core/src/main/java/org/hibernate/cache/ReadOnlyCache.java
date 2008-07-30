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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cache.access.SoftLock;

/**
 * Caches data that is never updated.
 * @see CacheConcurrencyStrategy
 */
public class ReadOnlyCache implements CacheConcurrencyStrategy {
	
	private Cache cache;
	private static final Logger log = LoggerFactory.getLogger(ReadOnlyCache.class);
	
	public ReadOnlyCache() {}
	
	public void setCache(Cache cache) {
		this.cache=cache;
	}

	public Cache getCache() {
		return cache;
	}

	public String getRegionName() {
		return cache.getRegionName();
	}
	
	public synchronized Object get(Object key, long timestamp) throws CacheException {
		Object result = cache.get(key);
		if ( result!=null && log.isDebugEnabled() ) log.debug("Cache hit: " + key);
		return result;
	}
	
	/**
	 * Unsupported!
	 */
	public SoftLock lock(Object key, Object version) {
		log.error("Application attempted to edit read only item: " + key);
		throw new UnsupportedOperationException("Can't write to a readonly object");
	}
	
	public synchronized boolean put(
			Object key, 
			Object value, 
			long timestamp, 
			Object version, 
			Comparator versionComparator,
			boolean minimalPut) 
	throws CacheException {
		if ( minimalPut && cache.get(key)!=null ) {
			if ( log.isDebugEnabled() ) log.debug("item already cached: " + key);
			return false;
		}
		if ( log.isDebugEnabled() ) log.debug("Caching: " + key);
		cache.put(key, value);
		return true;
	}
	
	/**
	 * Unsupported!
	 */
	public void release(Object key, SoftLock lock) {
		log.error("Application attempted to edit read only item: " + key);
		//throw new UnsupportedOperationException("Can't write to a readonly object");
	}
	
	public void clear() throws CacheException {
		cache.clear();
	}

	public void remove(Object key) throws CacheException {
		cache.remove(key);
	}
	
	public void destroy() {
		try {
			cache.destroy();
		}
		catch (Exception e) {
			log.warn("could not destroy cache", e);
		}
	}

	/**
	 * Unsupported!
	 */
	public boolean afterUpdate(Object key, Object value, Object version, SoftLock lock) throws CacheException {
		log.error("Application attempted to edit read only item: " + key);
		throw new UnsupportedOperationException("Can't write to a readonly object");
	}

	/**
	 * Do nothing.
	 */
	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {		
		if ( log.isDebugEnabled() ) log.debug("Caching after insert: " + key);
		cache.update(key, value);
		return true;
	}

	/**
	 * Do nothing.
	 */
	public void evict(Object key) throws CacheException {
		// noop
	}

	/**
	 * Do nothing.
	 */
	public boolean insert(Object key, Object value, Object currentVersion) {
		return false;
	}

	/**
	 * Unsupported!
	 */
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) {
		log.error("Application attempted to edit read only item: " + key);
		throw new UnsupportedOperationException("Can't write to a readonly object");
	}

	public String toString() {
		return cache + "(read-only)";
	}

}






