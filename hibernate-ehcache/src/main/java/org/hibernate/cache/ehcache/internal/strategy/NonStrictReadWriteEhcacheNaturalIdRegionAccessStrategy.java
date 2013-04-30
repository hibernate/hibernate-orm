/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.ehcache.internal.strategy;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.regions.EhcacheNaturalIdRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * Ehcache specific non-strict read/write NaturalId region access strategy
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public class NonStrictReadWriteEhcacheNaturalIdRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheNaturalIdRegion>
		implements NaturalIdRegionAccessStrategy {

	/**
	 * Create a non-strict read/write access strategy accessing the given NaturalId region.
	 *
	 * @param region The wrapped region
	 * @param settings The Hibernate settings
	 */
	public NonStrictReadWriteEhcacheNaturalIdRegionAccessStrategy(EhcacheNaturalIdRegion region, Settings settings) {
		super( region, settings );
	}

	@Override
	public NaturalIdRegion getRegion() {
		return region();
	}

	@Override
	public Object get(Object key, long txTimestamp) throws CacheException {
		return region().get( key );
	}

	@Override
	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException {
		if ( minimalPutOverride && region().contains( key ) ) {
			return false;
		}
		else {
			region().put( key, value );
			return true;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public SoftLock lockItem(Object key, Object version) throws CacheException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Since this is a non-strict read/write strategy item locking is not used.
	 */
	@Override
	public void unlockItem(Object key, SoftLock lock) throws CacheException {
		region().remove( key );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Returns <code>false</code> since this is an asynchronous cache access strategy.
	 */
	@Override
	public boolean insert(Object key, Object value) throws CacheException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Returns <code>false</code> since this is a non-strict read/write cache access strategy
	 */
	@Override
	public boolean afterInsert(Object key, Object value) throws CacheException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Removes the entry since this is a non-strict read/write cache strategy.
	 */
	@Override
	public boolean update(Object key, Object value) throws CacheException {
		remove( key );
		return false;
	}

	@Override
	public boolean afterUpdate(Object key, Object value, SoftLock lock) throws CacheException {
		unlockItem( key, lock );
		return false;
	}

	@Override
	public void remove(Object key) throws CacheException {
		region().remove( key );
	}
}
