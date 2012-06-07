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

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * @author Eric Dalquist
 */
class BaseNaturalIdRegionAccessStrategy extends BaseRegionAccessStrategy implements NaturalIdRegionAccessStrategy {
	private final NaturalIdRegionImpl region;

	@Override
	protected BaseGeneralDataRegion getInternalRegion() {
		return region;
	}

	@Override
	protected boolean isDefaultMinimalPutOverride() {
		return region.getSettings().isMinimalPutsEnabled();
	}

	@Override
	public NaturalIdRegion getRegion() {
		return region;
	}

	@Override
	public boolean insert(Object key, Object value ) throws CacheException {
		return putFromLoad( key, value, 0 , null );
	}

	@Override
	public boolean afterInsert(Object key, Object value ) throws CacheException {
		return false;
	}

	@Override
	public boolean update(Object key, Object value ) throws CacheException {
		return putFromLoad( key, value, 0 , null );
	}

	@Override
	public boolean afterUpdate(Object key, Object value, SoftLock lock) throws CacheException {
		return false;
	}

	BaseNaturalIdRegionAccessStrategy(NaturalIdRegionImpl region) {
		this.region = region;
	}
}
