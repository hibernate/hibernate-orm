/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * @author Strong Liu
 */
class BaseEntityRegionAccessStrategy extends BaseRegionAccessStrategy implements EntityRegionAccessStrategy {
	private final EntityRegionImpl region;

	BaseEntityRegionAccessStrategy(EntityRegionImpl region) {
		this.region = region;
	}


	@Override
	public EntityRegion getRegion() {
		return region;
	}

	@Override
	public boolean insert(Object key, Object value, Object version) throws CacheException {
		return putFromLoad( key, value, 0, version );
	}

	@Override
	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
		return true;
	}

	@Override
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		return false;
	}

	@Override
	public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		return false;
	}

	@Override
	protected BaseGeneralDataRegion getInternalRegion() {
		return region;
	}

	@Override
	protected boolean isDefaultMinimalPutOverride() {
		return region.getSettings().isMinimalPutsEnabled();
	}
}
