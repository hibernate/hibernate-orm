/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.strategy;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.regions.EhcacheNaturalIdRegion;
import org.hibernate.cache.spi.NaturalIdCacheKey;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * JTA NaturalIdRegionAccessStrategy.
 *
 * @author Chris Dennis
 * @author Ludovic Orban
 * @author Alex Snaps
 */
public class TransactionalEhcacheNaturalIdRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheNaturalIdRegion,NaturalIdCacheKey>
		implements NaturalIdRegionAccessStrategy {

	private final Ehcache ehcache;

	/**
	 * Construct a new collection region access strategy.
	 *
	 * @param region the Hibernate region.
	 * @param ehcache the cache.
	 * @param settings the Hibernate settings.
	 */
	public TransactionalEhcacheNaturalIdRegionAccessStrategy(
			EhcacheNaturalIdRegion region,
			Ehcache ehcache,
			SessionFactoryOptions settings) {
		super( region, settings );
		this.ehcache = ehcache;
	}

	@Override
	public boolean afterInsert(NaturalIdCacheKey key, Object value) {
		return false;
	}

	@Override
	public boolean afterUpdate(NaturalIdCacheKey key, Object value, SoftLock lock) {
		return false;
	}

	@Override
	public Object get(NaturalIdCacheKey key, long txTimestamp) throws CacheException {
		try {
			final Element element = ehcache.get( key );
			return element == null ? null : element.getObjectValue();
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public NaturalIdRegion getRegion() {
		return region();
	}

	@Override
	public boolean insert(NaturalIdCacheKey key, Object value) throws CacheException {
		//OptimisticCache? versioning?
		try {
			ehcache.put( new Element( key, value ) );
			return true;
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public SoftLock lockItem(NaturalIdCacheKey key, Object version) throws CacheException {
		return null;
	}

	@Override
	public boolean putFromLoad(
			NaturalIdCacheKey key,
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
	public void remove(NaturalIdCacheKey key) throws CacheException {
		try {
			ehcache.remove( key );
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public void unlockItem(NaturalIdCacheKey key, SoftLock lock) throws CacheException {
		// no-op
	}

	@Override
	public boolean update(NaturalIdCacheKey key, Object value) throws CacheException {
		try {
			ehcache.put( new Element( key, value ) );
			return true;
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

}
