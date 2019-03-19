package org.hibernate.redis.ehcache.strategy;

import org.hibernate.redis.ehcache.regions.EhcacheCollectionRegion;
import org.hibernate.redis.ehcache.regions.EhcacheEntityRegion;
import org.hibernate.redis.ehcache.regions.EhcacheNaturalIdRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;

public interface EhcacheAccessStrategyFactory {
	/**
	 * Create {@link EntityRegionAccessStrategy} for the input
	 * {@link EhcacheEntityRegion} and {@link AccessType}
	 *
	 * @param entityRegion
	 *            The entity region being wrapped
	 * @param accessType
	 *            The type of access to allow to the region
	 *
	 * @return the created {@link EntityRegionAccessStrategy}
	 */
	public EntityRegionAccessStrategy createEntityRegionAccessStrategy(EhcacheEntityRegion entityRegion,
			AccessType accessType);

	/**
	 * Create {@link CollectionRegionAccessStrategy} for the input
	 * {@link EhcacheCollectionRegion} and {@link AccessType}
	 *
	 * @param collectionRegion
	 *            The collection region being wrapped
	 * @param accessType
	 *            The type of access to allow to the region
	 *
	 * @return the created {@link CollectionRegionAccessStrategy}
	 */
	public CollectionRegionAccessStrategy createCollectionRegionAccessStrategy(EhcacheCollectionRegion collectionRegion,
			AccessType accessType);

	/**
	 * Create {@link NaturalIdRegionAccessStrategy} for the input
	 * {@link EhcacheNaturalIdRegion} and {@link AccessType}
	 *
	 * @param naturalIdRegion
	 *            The natural-id region being wrapped
	 * @param accessType
	 *            The type of access to allow to the region
	 *
	 * @return the created {@link NaturalIdRegionAccessStrategy}
	 */
	public NaturalIdRegionAccessStrategy createNaturalIdRegionAccessStrategy(EhcacheNaturalIdRegion naturalIdRegion,
			AccessType accessType);

}
