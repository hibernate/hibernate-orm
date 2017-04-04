/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.strategy;

import org.hibernate.cache.ehcache.internal.regions.EhcacheCollectionRegion;
import org.hibernate.cache.ehcache.internal.regions.EhcacheEntityRegion;
import org.hibernate.cache.ehcache.internal.regions.EhcacheNaturalIdRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;

/**
 * Factory to create {@link org.hibernate.cache.spi.access.RegionAccessStrategy} instance
 *
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public interface EhcacheAccessStrategyFactory {
	/**
	 * Create {@link EntityRegionAccessStrategy} for the input {@link EhcacheEntityRegion} and {@link AccessType}
	 *
	 * @param entityRegion The entity region being wrapped
	 * @param accessType The type of access to allow to the region
	 *
	 * @return the created {@link EntityRegionAccessStrategy}
	 */
	public EntityRegionAccessStrategy createEntityRegionAccessStrategy(
			EhcacheEntityRegion entityRegion,
			AccessType accessType);

	/**
	 * Create {@link CollectionRegionAccessStrategy} for the input {@link EhcacheCollectionRegion} and {@link AccessType}
	 *
	 * @param collectionRegion The collection region being wrapped
	 * @param accessType The type of access to allow to the region
	 *
	 * @return the created {@link CollectionRegionAccessStrategy}
	 */
	public CollectionRegionAccessStrategy createCollectionRegionAccessStrategy(
			EhcacheCollectionRegion collectionRegion,
			AccessType accessType);

	/**
	 * Create {@link NaturalIdRegionAccessStrategy} for the input {@link EhcacheNaturalIdRegion} and {@link AccessType}
	 *
	 * @param naturalIdRegion The natural-id region being wrapped
	 * @param accessType The type of access to allow to the region
	 *
	 * @return the created {@link NaturalIdRegionAccessStrategy}
	 */
	public NaturalIdRegionAccessStrategy createNaturalIdRegionAccessStrategy(
			EhcacheNaturalIdRegion naturalIdRegion,
			AccessType accessType);

}
