/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.cache.spi.support.StorageAccess;

/**
 * StorageAccess impl wrapping a simple data Map (ConcurrentMap)
 *
 * @author Steve Ebersole
 */
public class StorageAccessImpl implements StorageAccess {
	private ConcurrentMap data;

	@Override
	public Object getFromCache(Object key) {
		if ( data == null ) {
			return null;
		}
		return data.get( key );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void putIntoCache(Object key, Object value) {
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
	public void removeFromCache(Object key) {
		if ( data == null ) {
			return;
		}

		data.remove( key );
	}

	@Override
	public void clearCache() {
		if ( data == null ) {
			return;
		}

		data.clear();
	}

	@Override
	public void release() {
		clearCache();
		data = null;
	}
}
