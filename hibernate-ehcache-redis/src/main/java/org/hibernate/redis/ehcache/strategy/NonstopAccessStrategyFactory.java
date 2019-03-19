package org.hibernate.redis.ehcache.strategy;

import org.hibernate.redis.ehcache.regions.EhcacheCollectionRegion;
import org.hibernate.redis.ehcache.regions.EhcacheEntityRegion;
import org.hibernate.redis.ehcache.regions.EhcacheNaturalIdRegion;
import org.hibernate.cache.ehcache.internal.nonstop.HibernateNonstopCacheExceptionHandler;
import org.hibernate.cache.ehcache.internal.nonstop.NonstopAwareCollectionRegionAccessStrategy;
import org.hibernate.cache.ehcache.internal.nonstop.NonstopAwareEntityRegionAccessStrategy;
import org.hibernate.cache.ehcache.internal.nonstop.NonstopAwareNaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;

public class NonstopAccessStrategyFactory implements EhcacheAccessStrategyFactory {

	private final EhcacheAccessStrategyFactory actualFactory;

	/**
	 * Constructor accepting the actual factory
	 *
	 * @param actualFactory
	 *            The wrapped RegionAccessStrategy factory
	 */
	public NonstopAccessStrategyFactory(EhcacheAccessStrategyFactory actualFactory) {
		this.actualFactory = actualFactory;
	}

	/* (non-Javadoc)
	 * @see org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory#createEntityRegionAccessStrategy(org.hibernate.redis.ehcache.regions.EhcacheEntityRegion, org.hibernate.cache.spi.access.AccessType)
	 */
	@Override
	public EntityRegionAccessStrategy createEntityRegionAccessStrategy(EhcacheEntityRegion entityRegion,
			AccessType accessType) {
		return new NonstopAwareEntityRegionAccessStrategy(
				actualFactory.createEntityRegionAccessStrategy(entityRegion, accessType),
				HibernateNonstopCacheExceptionHandler.getInstance());
	}

	/* (non-Javadoc)
	 * @see org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory#createNaturalIdRegionAccessStrategy(org.hibernate.redis.ehcache.regions.EhcacheNaturalIdRegion, org.hibernate.cache.spi.access.AccessType)
	 */
	@Override
	public NaturalIdRegionAccessStrategy createNaturalIdRegionAccessStrategy(EhcacheNaturalIdRegion naturalIdRegion,
			AccessType accessType) {
		return new NonstopAwareNaturalIdRegionAccessStrategy(
				actualFactory.createNaturalIdRegionAccessStrategy(naturalIdRegion, accessType),
				HibernateNonstopCacheExceptionHandler.getInstance());
	}

	/* (non-Javadoc)
	 * @see org.hibernate.redis.ehcache.strategy.EhcacheAccessStrategyFactory#createCollectionRegionAccessStrategy(org.hibernate.redis.ehcache.regions.EhcacheCollectionRegion, org.hibernate.cache.spi.access.AccessType)
	 */
	@Override
	public CollectionRegionAccessStrategy createCollectionRegionAccessStrategy(EhcacheCollectionRegion collectionRegion,
			AccessType accessType) {
		return new NonstopAwareCollectionRegionAccessStrategy(
				actualFactory.createCollectionRegionAccessStrategy(collectionRegion, accessType),
				HibernateNonstopCacheExceptionHandler.getInstance());
	}

}
