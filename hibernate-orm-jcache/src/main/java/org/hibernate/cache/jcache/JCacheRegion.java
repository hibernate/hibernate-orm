/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache;

import java.util.HashMap;
import java.util.Map;
import javax.cache.Cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.Region;

/**
 * @author Alex Snaps
 */
public class JCacheRegion implements Region {

	protected final Cache<Object, Object> cache;

	public JCacheRegion(Cache<Object, Object> cache) {
		if(cache == null) {
			throw new NullPointerException("JCacheRegion requires a Cache!");
		}
		this.cache = cache;
	}

	public String getName() {
		return cache.getName();
	}

	public void destroy() throws CacheException {
		cache.getCacheManager().destroyCache( cache.getName() );
	}

	public boolean contains(Object key) {
		return cache.containsKey( key );
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
		final Map<Object, Object> map = new HashMap<Object, Object>();
		for ( Cache.Entry<Object, Object> entry : cache ) {
			map.put( entry.getKey(), entry.getValue() );
		}
		return map;
	}

	public long nextTimestamp() {
		return JCacheRegionFactory.nextTS();
	}

	public int getTimeout() {
		return JCacheRegionFactory.timeOut();
	}

	Cache<Object, Object> getCache() {
		return cache;
	}
}
