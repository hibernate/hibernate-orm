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

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.regions.EhcacheCollectionRegion;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * JTA CollectionRegionAccessStrategy.
 *
 * @author Chris Dennis
 * @author Ludovic Orban
 * @author Alex Snaps
 */
public class TransactionalEhcacheCollectionRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheCollectionRegion>
		implements CollectionRegionAccessStrategy {

	private final Ehcache ehcache;

	/**
	 * Construct a new collection region access strategy.
	 *
	 * @param region the Hibernate region.
	 * @param ehcache the cache.
	 * @param settings the Hibernate settings.
	 */
	public TransactionalEhcacheCollectionRegionAccessStrategy(
			EhcacheCollectionRegion region,
			Ehcache ehcache,
			Settings settings) {
		super( region, settings );
		this.ehcache = ehcache;
	}

	@Override
	public Object get(Object key, long txTimestamp) throws CacheException {
		try {
			final Element element = ehcache.get( key );
			return element == null ? null : element.getObjectValue();
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public CollectionRegion getRegion() {
		return region();
	}

	@Override
	public SoftLock lockItem(Object key, Object version) throws CacheException {
		return null;
	}

	@Override
	public boolean putFromLoad(
			Object key,
			Object value,
			long txTimestamp,
			Object version,
			boolean minimalPutOverride) throws CacheException {
		try {
			if ( minimalPutOverride && ehcache.get( key ) != null ) {
				return false;
			}
			//OptimisticCache? versioning?
			ehcache.put( new Element( key, value ) );
			return true;
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public void remove(Object key) throws CacheException {
		try {
			ehcache.remove( key );
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public void unlockItem(Object key, SoftLock lock) throws CacheException {
		// no-op
	}

}
