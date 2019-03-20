package org.hibernate.redis.ehcache.regions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.EhCacheMessageLogger;
import org.hibernate.cache.ehcache.internal.nonstop.HibernateNonstopCacheExceptionHandler;
import org.hibernate.cache.internal.CacheKeyHelper;
import org.hibernate.cache.spi.Region;
import org.hibernate.redis.ehcache.strategy.AbstractReadWriteEhcacheAccessStrategy.Lock;
import org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory;
import org.jboss.logging.Logger;
import org.redisson.api.BatchOptions;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.nonstop.NonStopCacheException;
import net.sf.ehcache.util.Timestamper;

public class EhcacheDataRegion implements Region {

	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(EhCacheMessageLogger.class,
			EhcacheDataRegion.class.getName());
	private static final String CACHE_LOCK_TIMEOUT_PROPERTY = "net.sf.ehcache.hibernate.cache_lock_timeout";
	private static final int DEFAULT_CACHE_LOCK_TIMEOUT = 60000;

	private final Ehcache cache;
	private final RLocalCachedMap<Object, Object> distributedNeutrinoCache;
	private final Map<Object, Object> localNeutrinoCache;
	private final EhcacheAccessStrategyFactory accessStrategyFactory;
	private final int cacheLockTimeout;
	private final DistributedCacheLockWrapper dummyRemoteCacheResponse = new DistributedCacheLockWrapper(null);

	/**
	 * Create a Hibernate data region backed by the given Ehcache instance which is
	 * further backed by REDIS for Invalidation.
	 */
	EhcacheDataRegion(EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache cache, Properties properties,
			RedissonClient redisson, BatchOptions batchOptions,
			LocalCachedMapOptions<Object, Object> localCachedMapOptions) {
		this.accessStrategyFactory = accessStrategyFactory;
		this.cache = cache;
		final String timeout = properties.getProperty(CACHE_LOCK_TIMEOUT_PROPERTY,
				Integer.toString(DEFAULT_CACHE_LOCK_TIMEOUT));
		this.cacheLockTimeout = Timestamper.ONE_MS * Integer.decode(timeout);
		this.distributedNeutrinoCache = redisson.getLocalCachedMap(getName(), localCachedMapOptions);
		this.localNeutrinoCache = new ConcurrentHashMap<>();
	}

	/**
	 * Ehcache instance backing this Hibernate data region.
	 */
	protected Ehcache getCache() {
		return cache;
	}

	protected RLocalCachedMap<Object, Object> getDistributedNeutrinoCache() {
		return distributedNeutrinoCache;
	}

	/**
	 * The {@link org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory} used
	 * for creating various access strategies
	 */
	protected EhcacheAccessStrategyFactory getAccessStrategyFactory() {
		return accessStrategyFactory;
	}

	/**
	 * Return the Ehcache instance backing this Hibernate data region.
	 *
	 * @return The underlying ehcache cache
	 */
	public Ehcache getEhcache() {
		return getCache();
	}

	@Override
	public String getName() {
		return getCache().getName();
	}

	@Override
	public void destroy() throws CacheException {
		try {
			getCache().getCacheManager().removeCache(getCache().getName());
		} catch (IllegalStateException e) {
			// When Spring and Hibernate are both involved this will happen in normal
			// shutdown operation.
			// Do not throw an exception, simply log this one.
			LOG.debug("This can happen if multiple frameworks both try to shutdown ehcache", e);
		} catch (NonStopCacheException e) {
			HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
		} catch (net.sf.ehcache.CacheException e) {
			throw new CacheException(e);
		}
	}

	@Override
	public long getSizeInMemory() {
		try {
			return getCache().calculateInMemorySize();
		} catch (NonStopCacheException e) {
			HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
			return -1;
		} catch (Throwable t) {
			return -1;
		}
	}

	@Override
	public long getElementCountInMemory() {
		try {
			return getCache().getMemoryStoreSize();
		} catch (net.sf.ehcache.CacheException ce) {
			if (ce instanceof NonStopCacheException) {
				HibernateNonstopCacheExceptionHandler.getInstance()
						.handleNonstopCacheException((NonStopCacheException) ce);
				return -1;
			} else {
				throw new CacheException(ce);
			}
		}
	}

	@Override
	public long getElementCountOnDisk() {
		try {
			return getCache().getDiskStoreSize();
		} catch (NonStopCacheException e) {
			HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException(e);
			return -1;
		} catch (net.sf.ehcache.CacheException ce) {
			throw new CacheException(ce);
		}
	}

	@Override
	public Map toMap() {
		try {
			final Map<Object, Object> result = new HashMap<>();
			for (Object key : getCache().getKeys()) {
				result.put(key, getCache().get(key).getObjectValue());
			}
			return result;
		} catch (NonStopCacheException e) {
			HibernateNonstopCacheExceptionHandler.getInstance().handleNonstopCacheException((NonStopCacheException) e);
			return Collections.emptyMap();
		} catch (Exception e) {
			throw new CacheException(e);
		}
	}

	@Override
	public long nextTimestamp() {
		return Timestamper.next();
	}

	@Override
	public int getTimeout() {
		return cacheLockTimeout;
	}

	@Override
	public boolean contains(Object key) {
		return localNeutrinoCache.containsKey(CacheKeyHelper.getId(key));
	}

	public boolean containsInDistributedCache(Object key) {
		return distributedNeutrinoCache.containsKey(CacheKeyHelper.getId(key));
	}

	protected DistributedCacheLockWrapper validateCacheEntryBasedOnTimeStamp(Object key) {
		Object id = CacheKeyHelper.getId(key);
		Object item = distributedNeutrinoCache.get(id);
		if (item instanceof Lock) {
			if (((Lock) item).isWriteable(nextTimestamp(), null, null)) {
				updateTimeStampInRemote(key);
				return dummyRemoteCacheResponse;
			}
			return new DistributedCacheLockWrapper(((Lock) item));
		}

		Long timeFromCache = (Long) item;
		Long timeFromLocal = (Long) localNeutrinoCache.get(id);

		if (timeFromLocal == null) {
			return dummyRemoteCacheResponse;
		} else if (timeFromCache == null || timeFromCache > timeFromLocal) {
			localNeutrinoCache.remove(id);
			return dummyRemoteCacheResponse;
		}
		return null;
	}

	protected void updateTimeStampInAll(Object key) {
		Long currentTime = System.currentTimeMillis();
		Object id = CacheKeyHelper.getId(key);
		distributedNeutrinoCache.put(id, currentTime);
		localNeutrinoCache.put(id, currentTime);
	}

	protected void putLockInDistributedCache(Object key, Lock lock) {
		Object id = CacheKeyHelper.getId(key);
		distributedNeutrinoCache.put(id, lock);
		localNeutrinoCache.put(id, System.currentTimeMillis());
	}

	protected void updateTimeStampInLocal(Object key) {
		localNeutrinoCache.put(CacheKeyHelper.getId(key), System.currentTimeMillis());
	}

	protected void removeTimeStampFromAll(Object key) {
		Object id = CacheKeyHelper.getId(key);
		distributedNeutrinoCache.remove(id);
		localNeutrinoCache.remove(id);
	}

	protected void updateTimeStampInRemote(Object key) {
		Object id = CacheKeyHelper.getId(key);
		distributedNeutrinoCache.put(id, System.currentTimeMillis());
		localNeutrinoCache.remove(id);
	}

	protected void clearTimeStampInLocal() {
		distributedNeutrinoCache.clear();
		localNeutrinoCache.clear();
	}

	protected class DistributedCacheLockWrapper {
		private Lock lock;

		private DistributedCacheLockWrapper(Lock lock) {
			super();
			this.lock = lock;
		}

		public Lock getLock() {
			return lock;
		}

		public void setLock(Lock lock) {
			this.lock = lock;
		}

	}
}