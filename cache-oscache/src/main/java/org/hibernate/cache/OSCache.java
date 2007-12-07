/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 */
package org.hibernate.cache;

import java.util.Map;

import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;

/**
 * @author <a href="mailto:m.bogaert@intrasoft.be">Mathias Bogaert</a>
 */
public class OSCache implements Cache {

	/** 
	 * The OSCache 2.0 cache administrator. 
	 */
	private GeneralCacheAdministrator cache = new GeneralCacheAdministrator();

	private final int refreshPeriod;
	private final String cron;
	private final String regionName;
	
	private String toString(Object key) {
		return String.valueOf(key) + '.' + regionName;
	}

	public OSCache(int refreshPeriod, String cron, String region) {
		this.refreshPeriod = refreshPeriod;
		this.cron = cron;
		this.regionName = region;
	}

	public void setCacheCapacity(int cacheCapacity) {
		cache.setCacheCapacity(cacheCapacity);
	}

	public Object get(Object key) throws CacheException {
		try {
			return cache.getFromCache( toString(key), refreshPeriod, cron );
		}
		catch (NeedsRefreshException e) {
			cache.cancelUpdate( toString(key) );
			return null;
		}
	}

	public Object read(Object key) throws CacheException {
		return get(key);
	}
	
	public void update(Object key, Object value) throws CacheException {
		put(key, value);
	}
	
	public void put(Object key, Object value) throws CacheException {
		cache.putInCache( toString(key), value );
	}

	public void remove(Object key) throws CacheException {
		cache.flushEntry( toString(key) );
	}

	public void clear() throws CacheException {
		cache.flushAll();
	}

	public void destroy() throws CacheException {
		cache.destroy();
	}

	public void lock(Object key) throws CacheException {
		// local cache, so we use synchronization
	}

	public void unlock(Object key) throws CacheException {
		// local cache, so we use synchronization
	}

	public long nextTimestamp() {
		return Timestamper.next();
	}

	public int getTimeout() {
		return Timestamper.ONE_MS * 60000; //ie. 60 seconds
	}

	public String getRegionName() {
		return regionName;
	}

	public long getSizeInMemory() {
		return -1;
	}

	public long getElementCountInMemory() {
		return -1;
	}

	public long getElementCountOnDisk() {
		return -1;
	}

	public Map toMap() {
		throw new UnsupportedOperationException();
	}

	public String toString() {
		return "OSCache(" + regionName + ')';
	}

}
