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
import org.hibernate.cache.ehcache.internal.regions.EhcacheEntityRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * Ehcache specific read-only entity region access strategy
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
public class ReadOnlyEhcacheEntityRegionAccessStrategy extends AbstractEhcacheAccessStrategy<EhcacheEntityRegion>
		implements EntityRegionAccessStrategy {

	/**
	 * Create a read-only access strategy accessing the given entity region.
	 *
	 * @param region The wrapped region
	 * @param settings The Hibernate settings
	 */
	public ReadOnlyEhcacheEntityRegionAccessStrategy(EhcacheEntityRegion region, Settings settings) {
		super( region, settings );
	}

	@Override
	public EntityRegion getRegion() {
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

	@Override
	public SoftLock lockItem(Object key, Object version) throws UnsupportedOperationException {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * A no-op since this cache is read-only
	 */
	@Override
	public void unlockItem(Object key, SoftLock lock) throws CacheException {
		evict( key );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * This cache is asynchronous hence a no-op
	 */
	@Override
	public boolean insert(Object key, Object value, Object version) throws CacheException {
		return false;
	}

	@Override
	public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
		region().put( key, value );
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Throws UnsupportedOperationException since this cache is read-only
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException( "Can't write to a readonly object" );
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Throws UnsupportedOperationException since this cache is read-only
	 *
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException( "Can't write to a readonly object" );
	}
}
