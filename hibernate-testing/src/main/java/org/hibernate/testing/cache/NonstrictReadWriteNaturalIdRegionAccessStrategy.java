/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * @author Eric Dalquist
 */
class NonstrictReadWriteNaturalIdRegionAccessStrategy extends BaseNaturalIdRegionAccessStrategy {
	NonstrictReadWriteNaturalIdRegionAccessStrategy(NaturalIdRegionImpl region) {
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
	
	/**
	 * Returns <code>false</code> since this is an asynchronous cache access strategy.
	 * @see org.hibernate.cache.ehcache.internal.strategy.NonStrictReadWriteEhcacheNaturalIdRegionAccessStrategy 
	 */
	@Override
	public boolean insert(Object key, Object value ) throws CacheException {
		return false;
	}

	/**
	 * Returns <code>false</code> since this is a non-strict read/write cache access strategy
	 * @see org.hibernate.cache.ehcache.internal.strategy.NonStrictReadWriteEhcacheNaturalIdRegionAccessStrategy 
	 */
	@Override
	public boolean afterInsert(Object key, Object value ) throws CacheException {
		return false;
	}
	
	/**
	 * Removes the entry since this is a non-strict read/write cache strategy.
	 * @see org.hibernate.cache.ehcache.internal.strategy.NonStrictReadWriteEhcacheNaturalIdRegionAccessStrategy 
	 */
	public boolean update(Object key, Object value ) throws CacheException {
		remove( key );
		return false;
	}
}
