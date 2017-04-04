/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.naturalid;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.AccessDelegate;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
class ReadOnlyAccess implements NaturalIdRegionAccessStrategy {

	protected final NaturalIdRegionImpl region;
	protected final AccessDelegate delegate;

	ReadOnlyAccess(NaturalIdRegionImpl region, AccessDelegate delegate) {
		this.region = region;
		this.delegate = delegate;
	}

	@Override
	public boolean insert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		return delegate.insert( session, key, value, null );
	}

	@Override
	public boolean update(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		throw new UnsupportedOperationException( "Illegal attempt to edit read only item" );
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
	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		return delegate.get( session, key, txTimestamp );
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version) throws CacheException {
		return delegate.putFromLoad( session, key, value, txTimestamp, version );
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		return delegate.putFromLoad( session, key, value, txTimestamp, version, minimalPutOverride );
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		delegate.remove( session, key );
	}

	@Override
	public void removeAll() throws CacheException {
		delegate.removeAll();
	}

	@Override
	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) throws CacheException {
		return null;
	}

	@Override
	public SoftLock lockRegion() throws CacheException {
		return null;
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		delegate.unlockItem( session, key );
	}

	@Override
	public void unlockRegion(SoftLock lock) throws CacheException {
	}

	@Override
	public boolean afterInsert(SharedSessionContractImplementor session, Object key, Object value) throws CacheException {
		return delegate.afterInsert( session, key, value, null );
	}

	@Override
	public boolean afterUpdate(SharedSessionContractImplementor session, Object key, Object value, SoftLock lock) throws CacheException {
		throw new UnsupportedOperationException( "Illegal attempt to edit read only item" );
	}

	@Override
	public Object generateCacheKey(Object[] naturalIdValues, EntityPersister persister, SharedSessionContractImplementor session) {
		return region.getCacheKeysFactory().createNaturalIdKey(naturalIdValues, persister, session);
	}

	@Override
	public Object[] getNaturalIdValues(Object cacheKey) {
		return region.getCacheKeysFactory().getNaturalIdValues(cacheKey);
	}
}
