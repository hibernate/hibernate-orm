package org.hibernate.redis.ehcache.regions;

import java.util.Properties;

import org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.redisson.api.BatchOptions;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RedissonClient;

import net.sf.ehcache.Ehcache;

public class EhcacheEntityRegion extends EhcacheTransactionalDataRegion implements EntityRegion {

	/**
	 * @param accessStrategyFactory
	 * @param underlyingCache
	 * @param settings
	 * @param metadata
	 * @param properties
	 * @param redisson
	 * @param batchOptions
	 * @param localCachedMapOptions
	 */
	public EhcacheEntityRegion(EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache underlyingCache,
			SessionFactoryOptions settings, CacheDataDescription metadata, Properties properties,
			RedissonClient redisson, BatchOptions batchOptions,
			LocalCachedMapOptions<Object, Object> localCachedMapOptions) {
		super(accessStrategyFactory, underlyingCache, settings, metadata, properties, redisson, batchOptions,
				localCachedMapOptions);
	}

	/* (non-Javadoc)
	 * @see org.hibernate.cache.spi.EntityRegion#buildAccessStrategy(org.hibernate.cache.spi.access.AccessType)
	 */
	@Override
	public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		return getAccessStrategyFactory().createEntityRegionAccessStrategy(this, accessType);
	}

}
