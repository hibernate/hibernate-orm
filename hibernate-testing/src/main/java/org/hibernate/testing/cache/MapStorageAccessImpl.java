/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.cache.spi.support.DomainDataStorageAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * StorageAccess impl wrapping a simple data Map (ConcurrentMap)
 *
 * @author Steve Ebersole
 */
public class MapStorageAccessImpl implements DomainDataStorageAccess {
	private ConcurrentMap data;

	@Override
	public boolean contains(Object key) {
		return data != null && data.containsKey( key );
	}

	@Override
	public Object getFromCache(Object key, SharedSessionContractImplementor session) {
		if ( data == null ) {
			return null;
		}
		return data.get( key );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void putIntoCache(Object key, Object value, SharedSessionContractImplementor session) {
		getOrMakeDataMap().put( key, value );
	}

	@SuppressWarnings("WeakerAccess")
	protected ConcurrentMap getOrMakeDataMap() {
		if ( data == null ) {
			data = new ConcurrentHashMap();
		}
		return data;
	}

	@Override
	public void removeFromCache(Object key, SharedSessionContractImplementor session) {
		if ( data == null ) {
			return;
		}

		data.remove( key );
	}

	@Override
	public void clearCache(SharedSessionContractImplementor session) {
		if ( data == null ) {
			return;
		}

		data.clear();
	}

	@Override
	public void evictData() {
		if ( data != null ) {
			data.clear();
		}
	}

	@Override
	public void evictData(Object key) {
		if ( data != null ) {
			data.remove( key );
		}
	}

	@Override
	public void release() {
		if ( data != null ) {
			data.clear();
			data = null;
		}
	}
}
