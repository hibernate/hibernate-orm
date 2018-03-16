/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractRegion implements Region {
	private final String name;
	private final RegionFactory regionFactory;
	private final StorageAccess storageAccess;

	public AbstractRegion(String name, RegionFactory regionFactory, StorageAccess storageAccess) {
		this.name = regionFactory.qualify( name );
		this.regionFactory = regionFactory;
		this.storageAccess = storageAccess;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public RegionFactory getRegionFactory() {
		return regionFactory;
	}

	public StorageAccess getStorageAccess() {
		return storageAccess;
	}

	@Override
	public void clear() {
		storageAccess.clearCache();
	}

	@Override
	public void destroy() throws CacheException {
		storageAccess.release();
	}
}
