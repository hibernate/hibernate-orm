/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.Region;

/**
 * @author Strong Liu
 */
class BaseRegion implements Region {
	protected final Map cache = new ConcurrentHashMap();
	private final String name;
	private static int timeout = Timestamper.ONE_MS * 60000;  //60s

	BaseRegion(String name) {
		this.name = name;
	}

	@Override
	public boolean contains(Object key) {
		return key != null ? cache.containsKey( key ) : false;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void destroy() throws CacheException {
		cache.clear();
	}

	@Override
	public long getSizeInMemory() {
		return -1;
	}

	@Override
	public long getElementCountInMemory() {
		return cache.size();
	}

	@Override
	public long getElementCountOnDisk() {
		return 0;
	}

	@Override
	public Map toMap() {
		return Collections.unmodifiableMap( cache );
	}

	@Override
	public long nextTimestamp() {
		return Timestamper.next();
	}

	@Override
	public int getTimeout() {
		return timeout;
	}

}


