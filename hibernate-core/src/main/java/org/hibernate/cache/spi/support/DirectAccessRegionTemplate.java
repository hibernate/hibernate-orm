/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.spi.DirectAccessRegion;
import org.hibernate.cache.spi.RegionFactory;

/**
 * @author Steve Ebersole
 */
public abstract class DirectAccessRegionTemplate extends AbstractRegion implements DirectAccessRegion {
	public DirectAccessRegionTemplate(String name, RegionFactory regionFactory) {
		super( name, regionFactory );
	}

	public abstract StorageAccess getStorageAccess();

	@Override
	public boolean contains(Object key) {
		return getStorageAccess().contains( key );
	}

	@Override
	public Object getFromCache(Object key) {
		return getStorageAccess().getFromCache( key );
	}

	@Override
	public void putIntoCache(Object key, Object value) {
		getStorageAccess().putIntoCache( key, value );
	}

	@Override
	public void removeFromCache(Object key) {
		getStorageAccess().removeFromCache( key );
	}

	@Override
	public void clear() {
		getStorageAccess().clearCache();
	}

	@Override
	public void destroy() {
		getStorageAccess().release();
	}

}
