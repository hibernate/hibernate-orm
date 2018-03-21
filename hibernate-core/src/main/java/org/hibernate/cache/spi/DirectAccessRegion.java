/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.support.StorageAccess;

/**
 * Specialized Region whose data is accessed directly - not requiring
 * key wrapping, e.g.
 *
 * @author Steve Ebersole
 */
public interface DirectAccessRegion extends Region {
	StorageAccess getStorageAccess();

	@Override
	default void clear() {
		getStorageAccess().clearCache();
	}

	@Override
	default void destroy() throws CacheException {
		getStorageAccess().release();
	}
}
