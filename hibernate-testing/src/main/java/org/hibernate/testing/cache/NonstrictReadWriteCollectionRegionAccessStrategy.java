/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * @author Strong Liu
 */
class NonstrictReadWriteCollectionRegionAccessStrategy extends BaseCollectionRegionAccessStrategy {
	NonstrictReadWriteCollectionRegionAccessStrategy(CollectionRegionImpl region) {
		super( region );
	}

	@Override
	public void unlockItem(Object key, SoftLock lock) throws CacheException {
		evict( key );
	}

	@Override
	public void remove(Object key) throws CacheException {
		evict( key );
	}
}
