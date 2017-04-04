/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.nonstop;

import org.hibernate.cache.ehcache.internal.regions.EhcacheCollectionRegion;
import org.hibernate.cache.ehcache.internal.regions.EhcacheEntityRegion;
import org.hibernate.cache.ehcache.internal.regions.EhcacheNaturalIdRegion;
import org.hibernate.cache.ehcache.internal.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;

/**
 * Implementation of {@link org.hibernate.cache.ehcache.internal.strategy.EhcacheAccessStrategyFactory} that takes care of Nonstop cache exceptions using
 * {@link HibernateNonstopCacheExceptionHandler}
 *
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public class NonstopAccessStrategyFactory implements EhcacheAccessStrategyFactory {

	private final EhcacheAccessStrategyFactory actualFactory;

	/**
	 * Constructor accepting the actual factory
	 *
	 * @param actualFactory The wrapped RegionAccessStrategy factory
	 */
	public NonstopAccessStrategyFactory(EhcacheAccessStrategyFactory actualFactory) {
		this.actualFactory = actualFactory;
	}

	@Override
	public EntityRegionAccessStrategy createEntityRegionAccessStrategy(
			EhcacheEntityRegion entityRegion,
			AccessType accessType) {
		return new NonstopAwareEntityRegionAccessStrategy(
				actualFactory.createEntityRegionAccessStrategy( entityRegion, accessType ),
				HibernateNonstopCacheExceptionHandler.getInstance()
		);
	}

	@Override
	public NaturalIdRegionAccessStrategy createNaturalIdRegionAccessStrategy(
			EhcacheNaturalIdRegion naturalIdRegion,
			AccessType accessType) {
		return new NonstopAwareNaturalIdRegionAccessStrategy(
				actualFactory.createNaturalIdRegionAccessStrategy(
						naturalIdRegion,
						accessType
				), HibernateNonstopCacheExceptionHandler.getInstance()
		);
	}

	@Override
	public CollectionRegionAccessStrategy createCollectionRegionAccessStrategy(
			EhcacheCollectionRegion collectionRegion,
			AccessType accessType) {
		return new NonstopAwareCollectionRegionAccessStrategy(
				actualFactory.createCollectionRegionAccessStrategy(
						collectionRegion,
						accessType
				), HibernateNonstopCacheExceptionHandler.getInstance()
		);
	}

}
