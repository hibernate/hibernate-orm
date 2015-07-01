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
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Ehcache specific read/write NaturalId region access strategy
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public class ReadWriteEhcacheNaturalIdRegionAccessStrategy
		extends AbstractReadWriteEhcacheAccessStrategy<EhcacheNaturalIdRegion>
		implements NaturalIdRegionAccessStrategy {

	/**
	 * Create a read/write access strategy accessing the given NaturalId region.
	 *
	 * @param region The wrapped region
	 * @param settings The Hibernate settings
	 */
	public ReadWriteEhcacheNaturalIdRegionAccessStrategy(EhcacheNaturalIdRegion region, SessionFactoryOptions settings) {
		super( region, settings );
	}

	@Override
	public NaturalIdRegion getRegion() {
		return region();
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * A no-op since this is an asynchronous cache access strategy.
	 */
	@Override
	public boolean insert(Object key, Object value) throws CacheException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Inserts will only succeed if there is no existing value mapped to this key.
	 */
	@Override
	public boolean afterInsert(Object key, Object value) throws CacheException {
		region().writeLock( key );
		try {
			final Lockable item = (Lockable) region().get( key );
			if ( item == null ) {
				region().put( key, new Item( value, null, region().nextTimestamp() ) );
				return true;
			}
			else {
				return false;
			}
		}
		finally {
			region().writeUnlock( key );
		}
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * A no-op since this is an asynchronous cache access strategy.
	 */
	@Override
	public boolean update(Object key, Object value) throws CacheException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Updates will only succeed if this entry was locked by this transaction and exclusively this transaction for the
	 * duration of this transaction.  It is important to also note that updates will fail if the soft-lock expired during
	 * the course of this transaction.
	 */
	@Override
	public boolean afterUpdate(Object key, Object value, SoftLock lock) throws CacheException {
		//what should we do with previousVersion here?
		region().writeLock( key );
		try {
			final Lockable item = (Lockable) region().get( key );

			if ( item != null && item.isUnlockable( lock ) ) {
				final Lock lockItem = (Lock) item;
				if ( lockItem.wasLockedConcurrently() ) {
					decrementLock( key, lockItem );
					return false;
				}
				else {
					region().put( key, new Item( value, null, region().nextTimestamp() ) );
					return true;
				}
			}
			else {
				handleLockExpiry( key, item );
				return false;
			}
		}
		finally {
			region().writeUnlock( key );
		}
	}

	@Override
	public Object generateCacheKey(Object[] naturalIdValues, EntityPersister persister, SessionImplementor session) {
		return DefaultCacheKeysFactory.createNaturalIdKey(naturalIdValues, persister, session);
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return DefaultCacheKeysFactory.getNaturalIdValues(cacheKey);
	}
}
