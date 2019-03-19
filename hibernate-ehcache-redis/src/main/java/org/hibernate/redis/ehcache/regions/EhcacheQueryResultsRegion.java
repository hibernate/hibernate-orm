package org.hibernate.redis.ehcache.regions;

import java.util.Properties;

import org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.redisson.api.BatchOptions;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RedissonClient;

import net.sf.ehcache.Ehcache;

public class EhcacheQueryResultsRegion extends EhcacheGeneralDataRegion implements QueryResultsRegion {
	
	/**
	 * @param accessStrategyFactory
	 * @param underlyingCache
	 * @param properties
	 * @param redisson
	 * @param batchOptions
	 * @param localCachedMapOptions
	 */
	public EhcacheQueryResultsRegion(EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache underlyingCache,
			Properties properties, RedissonClient redisson, BatchOptions batchOptions,
			LocalCachedMapOptions<Object, Object> localCachedMapOptions) {
		super(accessStrategyFactory, underlyingCache, properties, redisson, batchOptions, localCachedMapOptions);
	}

}
