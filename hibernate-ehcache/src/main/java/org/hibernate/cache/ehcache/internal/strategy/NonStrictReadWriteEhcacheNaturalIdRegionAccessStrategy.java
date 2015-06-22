/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.strategy;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.regions.EhcacheNaturalIdRegion;
import org.hibernate.cache.spi.NaturalIdCacheKey;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Ehcache specific non-strict read/write NaturalId region access strategy
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public class NonStrictReadWriteEhcacheNaturalIdRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheNaturalIdRegion,NaturalIdCacheKey>
		implements NaturalIdRegionAccessStrategy {

	/**
	 * Create a non-strict read/write access strategy accessing the given NaturalId region.
	 *
	 * @param region The wrapped region
	 * @param settings The Hibernate settings
	 */
	public NonStrictReadWriteEhcacheNaturalIdRegionAccessStrategy(EhcacheNaturalIdRegion region, SessionFactoryOptions settings) {
		super( region, settings );
	}

	@Override
	public NaturalIdRegion getRegion() {
		return region();
	}

	@Override
	public Object get(NaturalIdCacheKey key, long txTimestamp) throws CacheException {
		return region().get( key );
	}

	@Override
	public boolean putFromLoad(NaturalIdCacheKey key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
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
	public SoftLock lockItem(NaturalIdCacheKey key, Object version) throws CacheException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public void unlockItem(NaturalIdCacheKey key, SoftLock lock) throws CacheException {
		region().remove( key );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Returns <code>false</code> since this is an asynchronous cache access strategy.
	 */
	@Override
	public boolean insert(NaturalIdCacheKey key, Object value) throws CacheException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Returns <code>false</code> since this is a non-strict read/write cache access strategy
	 */
	@Override
	public boolean afterInsert(NaturalIdCacheKey key, Object value) throws CacheException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Removes the entry since this is a non-strict read/write cache strategy.
	 */
	@Override
	public boolean update(NaturalIdCacheKey key, Object value) throws CacheException {
		remove( key );
		return false;
	}

	@Override
	public boolean afterUpdate(NaturalIdCacheKey key, Object value, SoftLock lock) throws CacheException {
		unlockItem( key, lock );
		return false;
	}

	@Override
	public void remove(NaturalIdCacheKey key) throws CacheException {
		region().remove( key );
	}

}
