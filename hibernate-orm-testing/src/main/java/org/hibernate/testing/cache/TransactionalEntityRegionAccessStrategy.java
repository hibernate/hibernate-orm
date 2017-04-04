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
 * @author Strong Liu <stliu@hibernate.org>
 */
class TransactionalEntityRegionAccessStrategy extends BaseEntityRegionAccessStrategy {
	TransactionalEntityRegionAccessStrategy(EntityRegionImpl region) {
		super( region );
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value, Object version) {
		return false;
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) {
		return false;
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		evict( key );
	}

	@Override
	public boolean update(
			SharedSessionContractImplementor session, Object key, Object value, Object currentVersion,
			Object previousVersion) throws CacheException {
		return insert(session, key, value, currentVersion);
	}
}
