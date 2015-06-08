/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
class TransactionalCollectionRegionAccessStrategy extends BaseCollectionRegionAccessStrategy {
	TransactionalCollectionRegionAccessStrategy(CollectionRegionImpl region) {
		super( region );
	}

	@Override
	public void remove(Object key) throws CacheException {
		evict( key );
	}

}
