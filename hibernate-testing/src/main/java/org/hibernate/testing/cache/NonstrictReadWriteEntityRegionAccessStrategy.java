/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Strong Liu
 */
class NonstrictReadWriteEntityRegionAccessStrategy extends BaseEntityRegionAccessStrategy {
	NonstrictReadWriteEntityRegionAccessStrategy(EntityRegionImpl region) {
		super( region );
	}

	/**
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		evict( key );
	}

	/**
	 * Returns <code>false</code> since this is an asynchronous cache access strategy.
	 */
	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return false;
	}

	/**
	 * Returns <code>false</code> since this is a non-strict read/write cache access strategy
	 */
	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) throws CacheException {
		return false;
	}

	/**
	 * Removes the entry since this is a non-strict read/write cache strategy.
	 */
	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		evict( key );
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
		evict( key );
	}
}
