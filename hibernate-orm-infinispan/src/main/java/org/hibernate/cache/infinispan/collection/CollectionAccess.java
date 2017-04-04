/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.collection;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.AccessDelegate;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Collection region access for Infinispan.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
class CollectionAccess implements CollectionRegionAccessStrategy {
	private final CollectionRegionImpl region;
	private final AccessDelegate delegate;

	CollectionAccess(CollectionRegionImpl region, AccessDelegate delegate) {
		this.region = region;
		this.delegate = delegate;
	}

	public void evict(Object key) throws CacheException {
		delegate.evict( key );
	}

	public void evictAll() throws CacheException {
		delegate.evictAll();
	}

	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		return delegate.get( session, key, txTimestamp );
	}

	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version) throws CacheException {
		return delegate.putFromLoad( session, key, value, txTimestamp, version );
	}

	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		return delegate.putFromLoad( session, key, value, txTimestamp, version, minimalPutOverride );
	}

	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		delegate.remove( session, key );
	}

	public void removeAll() throws CacheException {
		delegate.removeAll();
	}

	public CollectionRegion getRegion() {
		return region;
	}

	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) throws CacheException {
		return null;
	}

	public SoftLock lockRegion() throws CacheException {
		return null;
	}

	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
		delegate.unlockItem( session, key);
	}

	public void unlockRegion(SoftLock lock) throws CacheException {
	}

	@Override
	public Object generateCacheKey(Object id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return region.getCacheKeysFactory().createCollectionKey(id, persister, factory, tenantIdentifier);
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return region.getCacheKeysFactory().getCollectionId(cacheKey);
	}

}
