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
 * Ehcache specific read-only entity region access strategy
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public class ReadOnlyEhcacheEntityRegionAccessStrategy extends AbstractEhcacheAccessStrategy<EhcacheEntityRegion>
		implements EntityRegionAccessStrategy {

	/**
	 * Create a read-only access strategy accessing the given entity region.
	 *
	 * @param region The wrapped region
	 * @param settings The Hibernate settings
	 */
	public ReadOnlyEhcacheEntityRegionAccessStrategy(EhcacheEntityRegion region, SessionFactoryOptions settings) {
		super( region, settings );
	}

	@Override
	public EntityRegion getRegion() {
		return region();
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

	@Override
	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) throws UnsupportedOperationException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * A no-op since this cache is read-only
	 */
	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		evict( key );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * This cache is asynchronous hence a no-op
	 */
	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		region().put( key, value );
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Throws UnsupportedOperationException since this cache is read-only
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException( "Can't write to a readonly object" );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Throws UnsupportedOperationException since this cache is read-only
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException( "Can't write to a readonly object" );
	}

	@Override
	public Object generateCacheKey(Object id, EntityPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return DefaultCacheKeysFactory.staticCreateEntityKey( id, persister, factory, tenantIdentifier );
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return DefaultCacheKeysFactory.staticGetEntityId(cacheKey);
	}
}
