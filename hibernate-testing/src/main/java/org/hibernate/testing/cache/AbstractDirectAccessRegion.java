/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.cache.spi.DirectAccessRegion;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDirectAccessRegion
		extends AbstractRegion
		implements DirectAccessRegion, DirectAccessRegion.DataAccess {
	private Map dataMap;

	public AbstractDirectAccessRegion(String name) {
		super( name );
	}

	@Override
	public DataAccess getAccess() {
		return this;
	}

	@Override
	public Object getFromCache(Object key) {
		if ( dataMap == null ) {
			return null;
		}
		return dataMap.get( key );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void addToCache(Object key, Object value) {
		if ( dataMap == null ) {
			dataMap = new ConcurrentHashMap();
		}

		dataMap.put( key, value );
	}

	@Override
	public void removeFromCache(Object key) {
		if ( dataMap != null ) {
			dataMap.remove( key );
		}
	}

	@Override
	public void clearCache() {
		if ( dataMap != null ) {
			dataMap.clear();
		}
	}
}
