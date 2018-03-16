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

	protected Object getFromCache(Object key) {
		log.debugf( "Locating entry in cache storage [region=`%s`] : %s", key );
		return storageAccess.getFromCache( key );
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected void addToCache(Object key, Object value) {
		log.debugf( "Adding entry to cache storage [region=`%s`] : %s -> %s", getRegion().getName(), key, value );
		storageAccess.putIntoCache( key, value );
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected void removeFromCache(Object key) {
		log.debugf( "Removing entry from cache storage [region=`%s`] : %s", key );
		storageAccess.removeFromCache( key );
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected void clearCache() {
		log.debugf( "Clearing cache data map [region=`%s`]" );
		storageAccess.clearCache();
	}

	@Override
	public boolean contains(Object key) {
		return storageAccess.contains( key );
	}

	@Override
	public Object get(SharedSessionContractImplementor session, Object key) {
		return getFromCache( key );
	}

	@Override
	public boolean putFromLoad(
			SharedSessionContractImplementor session,
			Object key,
			Object value,
			Object version) {
		addToCache( key, value );
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
		removeFromCache( key );
	}

	@Override
	public void removeAll() {
		clearCache();
	}

	@Override
	public void evict(Object key) {
		removeFromCache( key );
	}

	@Override
	public void evictAll() {
		clearCache();
	}

	@Override
	public void destroy() {
		storageAccess.release();
	}
}
