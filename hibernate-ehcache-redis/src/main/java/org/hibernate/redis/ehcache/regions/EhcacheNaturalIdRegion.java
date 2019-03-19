package org.hibernate.redis.ehcache.regions;

import java.util.Properties;

import org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.redisson.api.BatchOptions;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RedissonClient;

import net.sf.ehcache.Ehcache;

public class EhcacheNaturalIdRegion extends EhcacheTransactionalDataRegion implements NaturalIdRegion {
	/**
	 * Constructs an EhcacheNaturalIdRegion around the given underlying cache.
	 *
	 * @param accessStrategyFactory
	 *            The factory for building needed NaturalIdRegionAccessStrategy
	 *            instance
	 * @param underlyingCache
	 *            The ehcache cache instance
	 * @param settings
	 *            The Hibernate settings
	 * @param metadata
	 *            Metadata about the data to be cached in this region
	 * @param properties
	 *            Any additional[ properties
	 */
	public EhcacheNaturalIdRegion(EhcacheAccessStrategyFactory accessStrategyFactory, Ehcache underlyingCache,
			SessionFactoryOptions settings, CacheDataDescription metadata, Properties properties,
			RedissonClient redisson, BatchOptions batchOptions,
			LocalCachedMapOptions<Object, Object> localCachedMapOptions) {
		super(accessStrategyFactory, underlyingCache, settings, metadata, properties, redisson, batchOptions,
				localCachedMapOptions);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hibernate.cache.spi.NaturalIdRegion#buildAccessStrategy(org.hibernate.
	 * cache.spi.access.AccessType)
	 */
	@Override
	public NaturalIdRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		return getAccessStrategyFactory().createNaturalIdRegionAccessStrategy(this, accessType);
	}
}
