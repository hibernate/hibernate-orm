/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
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


