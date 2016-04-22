/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache.access;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.jcache.JCacheTransactionalDataRegion;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Alex Snaps
 */
abstract class JCacheRegionAccessStrategy<R extends JCacheTransactionalDataRegion> implements RegionAccessStrategy {

	private final R region;

	public JCacheRegionAccessStrategy(R region) {
		if ( region == null ) {
			throw new NullPointerException( "Requires a non-null JCacheTransactionalDataRegion" );
		}
		this.region = region;
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		return region.get( key );
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version) throws CacheException {
		final SessionFactoryOptions options = region.getSessionFactoryOptions();
		final boolean minimalPutOverride = options != null && options.isMinimalPutsEnabled();
		return putFromLoad( session, key, value, txTimestamp, version, minimalPutOverride );
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		if ( minimalPutOverride && region.contains( key ) ) {
			return false;
		}
		else {
			region.put( key, value );
			return true;
		}
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
		evict( key );
	}

	@Override
	public void unlockRegion(SoftLock lock) throws CacheException {
		evictAll();
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
		// jcache only supports asynchronous access strategies
	}

	@Override
	public void removeAll() throws CacheException {
		evictAll();
	}

	@Override
	public void evict(Object key) throws CacheException {
		region.remove( key );
	}

	@Override
	public void evictAll() throws CacheException {
		region.clear();
	}

	public R getRegion() {
		return region;
	}
}
