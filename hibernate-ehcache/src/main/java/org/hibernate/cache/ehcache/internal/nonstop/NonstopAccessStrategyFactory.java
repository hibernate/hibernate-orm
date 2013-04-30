/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
