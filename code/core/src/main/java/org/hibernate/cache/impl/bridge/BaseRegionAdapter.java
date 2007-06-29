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
