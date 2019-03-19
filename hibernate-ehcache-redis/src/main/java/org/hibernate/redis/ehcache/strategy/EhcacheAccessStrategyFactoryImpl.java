package org.hibernate.redis.ehcache.strategy;

import org.hibernate.redis.ehcache.regions.EhcacheCollectionRegion;
import org.hibernate.redis.ehcache.regions.EhcacheEntityRegion;
import org.hibernate.redis.ehcache.regions.EhcacheNaturalIdRegion;
import org.hibernate.cache.ehcache.EhCacheMessageLogger;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.jboss.logging.Logger;

public class EhcacheAccessStrategyFactoryImpl implements EhcacheAccessStrategyFactory {

	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(EhCacheMessageLogger.class,
			EhcacheAccessStrategyFactoryImpl.class.getName());

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory#
	 * createEntityRegionAccessStrategy(org.hibernate.redis.ehcache.regions.
	 * EhcacheEntityRegion, org.hibernate.cache.spi.access.AccessType)
	 */
	@Override
	public EntityRegionAccessStrategy createEntityRegionAccessStrategy(EhcacheEntityRegion entityRegion,
			AccessType accessType) {
		switch (accessType) {
		case READ_ONLY:
			if (entityRegion.getCacheDataDescription().isMutable()) {
				LOG.readOnlyCacheConfiguredForMutableEntity(entityRegion.getName());
			}
			return new ReadOnlyEhcacheEntityRegionAccessStrategy(entityRegion, entityRegion.getSettings());
		case READ_WRITE:
			return new ReadWriteEhcacheEntityRegionAccessStrategy(entityRegion, entityRegion.getSettings());

		case NONSTRICT_READ_WRITE:
			return new NonStrictReadWriteEhcacheEntityRegionAccessStrategy(entityRegion, entityRegion.getSettings());

		case TRANSACTIONAL:
			return new TransactionalEhcacheEntityRegionAccessStrategy(entityRegion, entityRegion.getEhcache(),
					entityRegion.getSettings());
		default:
			throw new IllegalArgumentException("unrecognized access strategy type [" + accessType + "]");

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory#
	 * createCollectionRegionAccessStrategy(org.hibernate.redis.ehcache.regions.
	 * EhcacheCollectionRegion, org.hibernate.cache.spi.access.AccessType)
	 */
	@Override
	public CollectionRegionAccessStrategy createCollectionRegionAccessStrategy(EhcacheCollectionRegion collectionRegion,
			AccessType accessType) {
		switch (accessType) {
		case READ_ONLY:
			if (collectionRegion.getCacheDataDescription().isMutable()) {
				LOG.readOnlyCacheConfiguredForMutableEntity(collectionRegion.getName());
			}
			return new ReadOnlyEhcacheCollectionRegionAccessStrategy(collectionRegion, collectionRegion.getSettings());
		case READ_WRITE:
			return new ReadWriteEhcacheCollectionRegionAccessStrategy(collectionRegion, collectionRegion.getSettings());
		case NONSTRICT_READ_WRITE:
			return new NonStrictReadWriteEhcacheCollectionRegionAccessStrategy(collectionRegion,
					collectionRegion.getSettings());
		case TRANSACTIONAL:
			return new TransactionalEhcacheCollectionRegionAccessStrategy(collectionRegion,
					collectionRegion.getEhcache(), collectionRegion.getSettings());
		default:
			throw new IllegalArgumentException("unrecognized access strategy type [" + accessType + "]");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory#
	 * createNaturalIdRegionAccessStrategy(org.hibernate.redis.ehcache.regions.
	 * EhcacheNaturalIdRegion, org.hibernate.cache.spi.access.AccessType)
	 */
	@Override
	public NaturalIdRegionAccessStrategy createNaturalIdRegionAccessStrategy(EhcacheNaturalIdRegion naturalIdRegion,
			AccessType accessType) {
		switch (accessType) {
		case READ_ONLY:
			if (naturalIdRegion.getCacheDataDescription().isMutable()) {
				LOG.readOnlyCacheConfiguredForMutableEntity(naturalIdRegion.getName());
			}
			return new ReadOnlyEhcacheNaturalIdRegionAccessStrategy(naturalIdRegion, naturalIdRegion.getSettings());
		case READ_WRITE:
			return new ReadWriteEhcacheNaturalIdRegionAccessStrategy(naturalIdRegion, naturalIdRegion.getSettings());
		case NONSTRICT_READ_WRITE:
			return new NonStrictReadWriteEhcacheNaturalIdRegionAccessStrategy(naturalIdRegion,
					naturalIdRegion.getSettings());
		case TRANSACTIONAL:
			return new TransactionalEhcacheNaturalIdRegionAccessStrategy(naturalIdRegion, naturalIdRegion.getEhcache(),
					naturalIdRegion.getSettings());
		default:
			throw new IllegalArgumentException("unrecognized access strategy type [" + accessType + "]");
		}
	}

}
