/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.jcache.JCacheEntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Alex Snaps
 */
public class ReadWriteEntityRegionAccessStrategy
		extends AbstractReadWriteRegionAccessStrategy<JCacheEntityRegion>
		implements EntityRegionAccessStrategy {


	public ReadWriteEntityRegionAccessStrategy(JCacheEntityRegion region) {
		super( region );
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return region.putIfAbsent( key, new Item(value, version, region.nextTimestamp(), nextItemId() ));
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		return false;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		while (true) {
			Lockable item = (Lockable) region.get( key );

			if ( item != null && item.isUnlockable( lock ) ) {
				Lock lockItem = (Lock) item;
				if ( lockItem.wasLockedConcurrently() ) {
					if (region.replace( key, lockItem, lockItem.unlock( region.nextTimestamp() ))) {
						return false;
					}
				}
				else {
					if (region.replace( key, lockItem, new Item(value, currentVersion, region.nextTimestamp(), nextItemId() ))) {
						return true;
					}
				}
			}
			else {
				handleMissingLock( key, item );
				return false;
			}

		}
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
