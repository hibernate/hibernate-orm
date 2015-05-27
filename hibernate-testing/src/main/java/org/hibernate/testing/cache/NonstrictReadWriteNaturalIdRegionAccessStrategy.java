/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.NaturalIdCacheKey;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * @author Eric Dalquist
 */
class NonstrictReadWriteNaturalIdRegionAccessStrategy extends BaseNaturalIdRegionAccessStrategy {
	NonstrictReadWriteNaturalIdRegionAccessStrategy(NaturalIdRegionImpl region) {
		super( region );
	}

	@Override
	public void unlockItem(NaturalIdCacheKey key, SoftLock lock) throws CacheException {
		evict( key );
	}

	@Override
	public void remove(NaturalIdCacheKey key) throws CacheException {
		evict( key );
	}

	@Override
	public boolean insert(NaturalIdCacheKey key, Object value) throws CacheException {
		return false;
	}

	@Override
	public boolean afterInsert(NaturalIdCacheKey key, Object value) throws CacheException {
		return false;
	}

	@Override
	public boolean update(NaturalIdCacheKey key, Object value) throws CacheException {
		remove( key );
		return false;
	}
}
