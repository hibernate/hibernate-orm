/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.cache.spi.AbstractDomainDataRegion;
import org.hibernate.cache.spi.access.CachedDomainDataAccess;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractCachedDomainDataAccess implements CachedDomainDataAccess, AbstractDomainDataRegion.Destructible {
	private static final Logger log = Logger.getLogger( AbstractCachedDomainDataAccess.class );

	private final DomainDataRegionImpl region;

	private Map data;

	protected AbstractCachedDomainDataAccess(DomainDataRegionImpl region) {
		this.region = region;
	}

	@Override
	public DomainDataRegionImpl getRegion() {
		return region;
	}

	protected Object getFromCache(Object key) {
		log.debugf( "Locating entry in cache data map [region=`%s`] : %s", key );
		if ( data == null ) {
			return null;
		}
		return data.get( key );
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected void addToCache(Object key, Object value) {
		log.debugf( "Adding entry to cache data map [region=`%s`] : %s -> %s", getRegion().getName(), key, value );
		getOrMakeData().put( key, value );
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected void removeFromCache(Object key) {
		log.debugf( "Removing entry from cache data map [region=`%s`] : %s", key );
		if ( data != null ) {
			data.remove( key );
		}
	}

	@Override
	public void removeAll(SharedSessionContractImplementor session) {
		data.clear();
	}

	@SuppressWarnings({"unchecked", "WeakerAccess"})
	protected void clearCache() {
		log.debugf( "Clearing cache data map [region=`%s`]" );
		if ( data != null ) {
			data.clear();
		}
	}

	public Map getData() {
		return data == null ? Collections.emptyMap() : data;
	}

	private Map getOrMakeData() {
		if ( data == null ) {
			data = new ConcurrentHashMap();
		}
		return data;
	}

	@Override
	public boolean contains(Object key) {
		return data != null && data.containsKey( key );
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
		addToCache( key, value );
		return true;
	}

	@Override
	public void remove(SharedSessionContractImplementor session, Object key) {
		removeFromCache( key );
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
		data.clear();
	}
}
