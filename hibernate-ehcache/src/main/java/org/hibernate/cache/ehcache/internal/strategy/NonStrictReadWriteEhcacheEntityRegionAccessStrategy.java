/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.strategy;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.regions.EhcacheEntityRegion;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Ehcache specific non-strict read/write entity region access strategy
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public class NonStrictReadWriteEhcacheEntityRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheEntityRegion>
		implements EntityRegionAccessStrategy {

	/**
	 * Create a non-strict read/write access strategy accessing the given collection region.
	 *
	 * @param region The wrapped region
	 * @param settings The Hibernate settings
	 */
	public NonStrictReadWriteEhcacheEntityRegionAccessStrategy(EhcacheEntityRegion region, SessionFactoryOptions settings) {
		super( region, settings );
	}

	@Override
	public EntityRegion getRegion() {
		return super.region();
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		return region().get( key );
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
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
	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) throws CacheException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		region().remove( key );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Returns <code>false</code> since this is an asynchronous cache access strategy.
	 */
	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Returns <code>false</code> since this is a non-strict read/write cache access strategy
	 */
	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Removes the entry since this is a non-strict read/write cache strategy.
	 */
	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		remove( session, key );
		return false;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		unlockItem( session, key, lock );
		return false;
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		region().remove( key );
	}

	@Override
	public Object generateCacheKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return DefaultCacheKeysFactory.staticCreateEntityKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return DefaultCacheKeysFactory.staticGetEntityId( cacheKey );
	}
}
