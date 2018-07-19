/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.spi.DirectAccessRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Bridge between DirectAccessRegion and StorageAccess
 *
 * @author Steve Ebersole
 */
public abstract class DirectAccessRegionTemplate extends AbstractRegion implements DirectAccessRegion {
	private final StorageAccess storageAccess;

	/**
	 * Constructs a {@link DirectAccessRegionTemplate}.
	 *
	 * @param name - the unqualified region name
	 * @param regionFactory - the region factory
	 * @param storageAccess - the cache storage access strategy
	 */
	public DirectAccessRegionTemplate(String name, RegionFactory regionFactory, StorageAccess storageAccess) {
		super( name, regionFactory );
		this.storageAccess = storageAccess;
	}

	public StorageAccess getStorageAccess() {
		return storageAccess;
	}

	@Override
	public Object getFromCache(Object key, SharedSessionContractImplementor session) {
		return getStorageAccess().getFromCache( key, session );
	}

	@Override
	public void putIntoCache(Object key, Object value, SharedSessionContractImplementor session) {
		getStorageAccess().putIntoCache( key, value, session );
	}

	@Override
	public void clear() {
		getStorageAccess().evictData();
	}

	@Override
	public void destroy() {
		getStorageAccess().release();
	}

}
