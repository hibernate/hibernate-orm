/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.strategy;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.regions.EhcacheCollectionRegion;
import org.hibernate.cache.spi.CollectionCacheKey;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * Ehcache specific non-strict read/write collection region access strategy
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public class NonStrictReadWriteEhcacheCollectionRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheCollectionRegion,CollectionCacheKey>
		implements CollectionRegionAccessStrategy {

	/**
	 * Create a non-strict read/write access strategy accessing the given collection region.
	 *
	 * @param region The wrapped region
	 * @param settings The Hibernate settings
	 */
	public NonStrictReadWriteEhcacheCollectionRegionAccessStrategy(EhcacheCollectionRegion region, SessionFactoryOptions settings) {
		super( region, settings );
	}

	@Override
	public CollectionRegion getRegion() {
		return region();
	}

	@Override
	public Object get(CollectionCacheKey key, long txTimestamp) throws CacheException {
		return region().get( key );
	}

	@Override
	public boolean putFromLoad(CollectionCacheKey key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		if ( minimalPutOverride && region().contains( key ) ) {
			return false;
		}
		else {
			region().put( key, value );
			return true;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public SoftLock lockItem(CollectionCacheKey key, Object version) throws CacheException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public void unlockItem(CollectionCacheKey key, SoftLock lock) throws CacheException {
		region().remove( key );
	}

	@Override
	public void remove(CollectionCacheKey key) throws CacheException {
		region().remove( key );
	}
}
