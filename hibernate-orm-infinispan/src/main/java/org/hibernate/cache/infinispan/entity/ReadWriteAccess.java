/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.entity;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.AccessDelegate;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Read-write or transactional entity region access for Infinispan.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
class ReadWriteAccess extends ReadOnlyAccess {

	ReadWriteAccess(EntityRegionImpl region, AccessDelegate delegate) {
		super(region, delegate);
	}

	public boolean update(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		return delegate.update( session, key, value, currentVersion, previousVersion );
	}

	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		return delegate.afterUpdate( session, key, value, currentVersion, previousVersion, lock );
	}
}
