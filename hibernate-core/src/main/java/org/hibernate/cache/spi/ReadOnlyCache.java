/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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

import java.util.Comparator;

import org.jboss.logging.Logger;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Caches data that is never updated.
 * @see org.hibernate.cache.spi.CacheConcurrencyStrategy
 */
@Deprecated
public class ReadOnlyCache implements CacheConcurrencyStrategy {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, ReadOnlyCache.class.getName());

	private Cache cache;

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
        if (result != null) LOG.debugf("Cache hit: %s", key);
		return result;
	}

	/**
	 * Unsupported!
	 */
	public SoftLock lock(Object key, Object version) {
        LOG.invalidEditOfReadOnlyItem(key);
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
            LOG.debugf("Item already cached: %s", key);
			return false;
		}
        LOG.debugf("Caching: %s", key);
		cache.put(key, value);
		return true;
	}

	/**
	 * Unsupported!
	 */
	public void release(Object key, SoftLock lock) {
        LOG.invalidEditOfReadOnlyItem(key);
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
            LOG.unableToDestroyCache(e.getMessage());
		}
	}

	/**
	 * Unsupported!
	 */
	public boolean afterUpdate(Object key, Object value, Object version, SoftLock lock) throws CacheException {
        LOG.invalidEditOfReadOnlyItem(key);
		throw new UnsupportedOperationException("Can't write to a readonly object");
	}

	/**
	 * Do nothing.
	 */
	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
        LOG.debugf("Caching after insert: %s", key);
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
        LOG.invalidEditOfReadOnlyItem(key);
		throw new UnsupportedOperationException("Can't write to a readonly object");
	}

	@Override
    public String toString() {
		return cache + "(read-only)";
	}

}






