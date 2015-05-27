/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.strategy;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.ehcache.internal.regions.EhcacheCollectionRegion;
import org.hibernate.cache.spi.CollectionCacheKey;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;

/**
 * Ehcache specific read/write collection region access strategy
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public class ReadWriteEhcacheCollectionRegionAccessStrategy
		extends AbstractReadWriteEhcacheAccessStrategy<EhcacheCollectionRegion,CollectionCacheKey>
		implements CollectionRegionAccessStrategy {

	/**
	 * Create a read/write access strategy accessing the given collection region.
	 *
	 * @param region The wrapped region
	 * @param settings The Hibernate settings
	 */
	public ReadWriteEhcacheCollectionRegionAccessStrategy(EhcacheCollectionRegion region, SessionFactoryOptions settings) {
		super( region, settings );
	}

	@Override
	public CollectionRegion getRegion() {
		return region();
	}
}
