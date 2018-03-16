/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.cache.spi.support.StorageAccess;

/**
 * @author Steve Ebersole
 */
public class StorageAcccessImpl implements StorageAccess {
	private Map data;

	@Override
	public Object getFromCache(Object key) {
		if ( data == null ) {
			return null;
		}
		return data.get( key );
	}

	@Override
	public void putIntoCache(Object key, Object value) {
		getOrMakeDataMap().put( key, value );
	}

	protected Map getOrMakeDataMap() {
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
