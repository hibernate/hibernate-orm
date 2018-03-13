/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class EntityNonStrictReadWriteAccess extends BaseEntityDataAccess {
	public EntityNonStrictReadWriteAccess(DomainDataRegionImpl domainDataRegion, EntityDataCachingConfig entityAccessConfig) {
		super( domainDataRegion );
	}


	@Override
	public boolean insert(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version) {
		return false;
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
		return false;
	}

	@Override
	public boolean update(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion) {
		removeFromCache( key );
		return false;
	}

	@Override
	public boolean afterUpdate(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object currentVersion,
			Object previousVersion,
			SoftLock lock) {
		unlockItem( session, key, lock );
		return false;
	}

	/**
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		removeFromCache( key );
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) {
		removeFromCache( key );
	}
}
