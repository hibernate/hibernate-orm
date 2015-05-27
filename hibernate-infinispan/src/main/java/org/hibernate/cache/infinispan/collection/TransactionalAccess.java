/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.collection;

import java.io.Serializable;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.TransactionalAccessDelegate;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CollectionCacheKey;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Transactional collection region access for Infinispan.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
class TransactionalAccess implements CollectionRegionAccessStrategy {

	private final CollectionRegionImpl region;

	private final TransactionalAccessDelegate delegate;

	TransactionalAccess(CollectionRegionImpl region) {
		this.region = region;
		this.delegate = new TransactionalAccessDelegate( region, region.getPutFromLoadValidator() );
	}

	public void evict(CollectionCacheKey key) throws CacheException {
		delegate.evict( key );
	}

	public void evictAll() throws CacheException {
		delegate.evictAll();
	}

	public Object get(CollectionCacheKey key, long txTimestamp) throws CacheException {
		return delegate.get( key, txTimestamp );
	}

	public boolean putFromLoad(CollectionCacheKey key, Object value, long txTimestamp, Object version) throws CacheException {
		return delegate.putFromLoad( key, value, txTimestamp, version );
	}

	public boolean putFromLoad(CollectionCacheKey key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		return delegate.putFromLoad( key, value, txTimestamp, version, minimalPutOverride );
	}

	public void remove(CollectionCacheKey key) throws CacheException {
		delegate.remove( key );
	}

	public void removeAll() throws CacheException {
		delegate.removeAll();
	}

	public CollectionRegion getRegion() {
		return region;
	}

	public SoftLock lockItem(CollectionCacheKey key, Object version) throws CacheException {
		return null;
	}

	public SoftLock lockRegion() throws CacheException {
		return null;
	}

	public void unlockItem(CollectionCacheKey key, SoftLock lock) throws CacheException {
	}

	public void unlockRegion(SoftLock lock) throws CacheException {
	}

	@Override
	public CollectionCacheKey generateCacheKey(Serializable id, CollectionPersister persister, SessionFactoryImplementor factory, String tenantIdentifier) {
		return DefaultCacheKeysFactory.createCollectionKey( id, persister, factory, tenantIdentifier );
	}

}
