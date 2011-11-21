/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing.cache;

import java.util.Comparator;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * @author Strong Liu
 */
class ReadWriteEntityRegionAccessStrategy extends AbstractReadWriteAccessStrategy
		implements EntityRegionAccessStrategy {
	private final EntityRegionImpl region;

	ReadWriteEntityRegionAccessStrategy(EntityRegionImpl region) {
		this.region = region;
	}

	@Override
	public boolean insert(Object key, Object value, Object version) throws CacheException {
		return false;
	}

	@Override
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
			throws CacheException {
		return false;
	}

	@Override
	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {

		try {
			writeLock.lock();
			Lockable item = (Lockable) region.get( key );
			if ( item == null ) {
				region.put( key, new Item( value, version, region.nextTimestamp() ) );
				return true;
			}
			else {
				return false;
			}
		}
		finally {
			writeLock.unlock();
		}
	}


	@Override
	public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws CacheException {
		try {
			writeLock.lock();
			Lockable item = (Lockable) region.get( key );

			if ( item != null && item.isUnlockable( lock ) ) {
				Lock lockItem = (Lock) item;
				if ( lockItem.wasLockedConcurrently() ) {
					decrementLock( key, lockItem );
					return false;
				}
				else {
					region.put( key, new Item( value, currentVersion, region.nextTimestamp() ) );
					return true;
				}
			}
			else {
				handleLockExpiry( key, item );
				return false;
			}
		}
		finally {
			writeLock.unlock();
		}
	}


	@Override
	protected BaseGeneralDataRegion getInternalRegion() {
		return region;
	}

	@Override
	protected boolean isDefaultMinimalPutOverride() {
		return region.getSettings().isMinimalPutsEnabled();
	}

	@Override
	Comparator getVersionComparator() {
		return region.getCacheDataDescription().getVersionComparator();
	}

	@Override
	public EntityRegion getRegion() {
		return region;
	}
}
