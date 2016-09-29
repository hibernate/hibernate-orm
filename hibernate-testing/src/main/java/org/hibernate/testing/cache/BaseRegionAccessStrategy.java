/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 */
public abstract class BaseRegionAccessStrategy implements RegionAccessStrategy {

	private static final Logger LOG = Logger.getLogger( BaseRegionAccessStrategy.class );

	protected abstract BaseGeneralDataRegion getInternalRegion();

	protected abstract boolean isDefaultMinimalPutOverride();

	@Override
	public Object get(SharedSessionContractImplementor session, Object key, long txTimestamp) throws CacheException {
		return getInternalRegion().get( session, key );
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version) throws CacheException {
		return putFromLoad(session, key, value, txTimestamp, version, isDefaultMinimalPutOverride() );
	}

	@Override
	public boolean putFromLoad(SharedSessionContractImplementor session, Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {

		if ( key == null || value == null ) {
			return false;
		}
		if ( minimalPutOverride && getInternalRegion().contains( key ) ) {
			LOG.debugf( "Item already cached: %s", key );
			return false;
		}
		LOG.debugf( "Caching: %s", key );
		getInternalRegion().put( session, key, value );
		return true;

	}

	/**
	 * Region locks are not supported.
	 *
	 * @return <code>null</code>
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#lockRegion()
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#lockRegion()
	 */
	@Override
	public SoftLock lockRegion() throws CacheException {
		return null;
	}

	/**
	 * Region locks are not supported - perform a cache clear as a precaution.
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#unlockRegion(org.hibernate.cache.spi.access.SoftLock)
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#unlockRegion(org.hibernate.cache.spi.access.SoftLock)
	 */
	@Override
	public void unlockRegion(SoftLock lock) throws CacheException {
		evictAll();
	}

	@Override
	public SoftLock lockItem(SharedSessionContractImplementor session, Object key, Object version) throws CacheException {
		return null;
	}

	@Override
	public void unlockItem(SharedSessionContractImplementor session, Object key, SoftLock lock) throws CacheException {
	}


	/**
	 * A no-op since this is an asynchronous cache access strategy.
	 *
	 * @see RegionAccessStrategy#remove(SharedSessionContractImplementor, Object)
	 */
	@Override
	public void remove(SharedSessionContractImplementor session, Object key) throws CacheException {
	}

	/**
	 * Called to evict data from the entire region
	 *
	 * @throws CacheException Propogated from underlying {@link org.hibernate.cache.spi.Region}
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#removeAll()
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#removeAll()
	 */
	@Override
	public void removeAll() throws CacheException {
		evictAll();
	}

	@Override
	public void evict(Object key) throws CacheException {
		getInternalRegion().evict( key );
	}

	@Override
	public void evictAll() throws CacheException {
		getInternalRegion().evictAll();
	}
}
