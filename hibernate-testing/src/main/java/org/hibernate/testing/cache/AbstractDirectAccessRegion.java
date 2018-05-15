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
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDirectAccessRegion
		extends AbstractRegion
		implements DirectAccessRegion {
	private Map dataMap;

	public AbstractDirectAccessRegion(String name) {
		super( name );
	}

	@Override
	public Object getFromCache(Object key, SharedSessionContractImplementor session) {
		if ( dataMap == null ) {
			return null;
		}
		return dataMap.get( key );
	}

	@Override
	public void putIntoCache(Object key, Object value, SharedSessionContractImplementor session) {
		if ( dataMap == null ) {
			dataMap = new ConcurrentHashMap();
		}

		dataMap.put( key, value );
	}

	@Override
	public void clear() {
		if ( dataMap != null ) {
			dataMap.clear();
		}
	}
}
