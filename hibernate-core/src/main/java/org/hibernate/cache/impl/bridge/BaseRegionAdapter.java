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
package org.hibernate.cache.impl.bridge;

import java.util.Map;

import org.hibernate.cache.Region;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Settings;

/**
 * Basic adapter bridging between {@link Region} and {@link Cache}.
 *
 * @author Steve Ebersole
 */
public abstract class BaseRegionAdapter implements Region {
	protected final Cache underlyingCache;
	protected final Settings settings;

	protected BaseRegionAdapter(Cache underlyingCache, Settings settings) {
		this.underlyingCache = underlyingCache;
		this.settings = settings;
	}

	public String getName() {
		return underlyingCache.getRegionName();
	}

	public void clear() throws CacheException {
		underlyingCache.clear();
	}

	public void destroy() throws CacheException {
		underlyingCache.destroy();
	}

	public boolean contains(Object key) {
		// safer to utilize the toMap() as oposed to say get(key) != null
		return underlyingCache.toMap().containsKey( key );
	}

	public long getSizeInMemory() {
		return underlyingCache.getSizeInMemory();
	}

	public long getElementCountInMemory() {
		return underlyingCache.getElementCountInMemory();
	}

	public long getElementCountOnDisk() {
		return underlyingCache.getElementCountOnDisk();
	}

	public Map toMap() {
		return underlyingCache.toMap();
	}

	public long nextTimestamp() {
		return underlyingCache.nextTimestamp();
	}

	public int getTimeout() {
		return underlyingCache.getTimeout();
	}
}
