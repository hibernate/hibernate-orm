/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.infinispan.naturalid;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.TransactionalAccessDelegate;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
class TransactionalAccess implements NaturalIdRegionAccessStrategy {
	private final NaturalIdRegionImpl region;
	private final TransactionalAccessDelegate delegate;

	TransactionalAccess(NaturalIdRegionImpl region) {
		this.region = region;
		this.delegate = new TransactionalAccessDelegate( region, region.getPutFromLoadValidator() );
	}

	@Override
	public boolean insert(Object key, Object value) throws CacheException {
		return delegate.insert( key, value, null );
	}

	@Override
	public boolean update(Object key, Object value) throws CacheException {
		return delegate.update( key, value, null, null );
	}

	@Override
	public NaturalIdRegion getRegion() {
		return region;
	}

	@Override
	public void evict(Object key) throws CacheException {
		delegate.evict( key );
	}

	@Override
	public void evictAll() throws CacheException {
		delegate.evictAll();
	}

	@Override
	public Object get(Object key, long txTimestamp) throws CacheException {
		return delegate.get( key, txTimestamp );
	}

	@Override
	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
		return delegate.putFromLoad( key, value, txTimestamp, version );
	}

	@Override
	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		return delegate.putFromLoad( key, value, txTimestamp, version, minimalPutOverride );
	}

	@Override
	public void remove(Object key) throws CacheException {
		delegate.remove( key );
	}

	@Override
	public void removeAll() throws CacheException {
		delegate.removeAll();
	}

	@Override
	public SoftLock lockItem(Object key, Object version) throws CacheException {
		return null;
	}

	@Override
	public SoftLock lockRegion() throws CacheException {
		return null;
	}

	@Override
	public void unlockItem(Object key, SoftLock lock) throws CacheException {
	}

	@Override
	public void unlockRegion(SoftLock lock) throws CacheException {
	}

	@Override
	public boolean afterInsert(Object key, Object value) throws CacheException {
		return false;
	}

	@Override
	public boolean afterUpdate(Object key, Object value, SoftLock lock) throws CacheException {
		return false;
	}

}
