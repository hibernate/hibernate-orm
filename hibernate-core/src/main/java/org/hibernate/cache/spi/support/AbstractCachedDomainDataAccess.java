/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCachedDomainDataAccess implements CachedDomainDataAccess, AbstractDomainDataRegion.Destructible {
	private static final Logger log = Logger.getLogger( AbstractCachedDomainDataAccess.class );

	private final DomainDataRegion region;
	private final DomainDataStorageAccess storageAccess;

	protected AbstractCachedDomainDataAccess(
			DomainDataRegion region,
			DomainDataStorageAccess storageAccess) {
		this.region = region;
		this.storageAccess = storageAccess;
	}

	@Override
	public DomainDataRegion getRegion() {
		return region;
	}

	protected DomainDataStorageAccess getStorageAccess() {
		return storageAccess;
	}

	protected void clearCache() {
		log.debugf( "Clearing cache data map [region=`%s`]", region.getName() );
		getStorageAccess().evictData();
	}

	@Override
	public boolean contains(Object key) {
		return getStorageAccess().contains( key );
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key) {
		return getStorageAccess().getFromCache( key, session );
	}

	@Override
	public boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version) {
		getStorageAccess().putFromLoad( key, value, session );
		return true;
	}

	@Override
	public boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version,
			boolean minimalPutOverride) {
		return putFromLoad( session, key, value, version );
	}

	private static final SoftLock REGION_LOCK = new SoftLock() {
	};

	@Override
	public SoftLock lockRegion() {
		return REGION_LOCK;
	}

	@Override
	public void unlockRegion(SoftLock lock) {
		evictAll();
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) {
		getStorageAccess().removeFromCache( key, session );
	}

	@Override
	public void removeAll(SharedSessionContractImplementor session) {
		getStorageAccess().clearCache( session );
	}

	@Override
	public void evict(Object key) {
		getStorageAccess().evictData( key );
	}

	@Override
	public void evictAll() {
		getStorageAccess().evictData();
	}

	@Override
	public void destroy() {
		getStorageAccess().release();
	}
}
