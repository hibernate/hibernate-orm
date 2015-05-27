/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.internal.strategy;

import java.io.Serializable;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.regions.EhcacheCollectionRegion;
import org.hibernate.cache.spi.CollectionCacheKey;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * JTA CollectionRegionAccessStrategy.
 *
 * @author Chris Dennis
 * @author Ludovic Orban
 * @author Alex Snaps
 */
public class TransactionalEhcacheCollectionRegionAccessStrategy
		extends AbstractEhcacheAccessStrategy<EhcacheCollectionRegion,CollectionCacheKey>
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
			SessionFactoryOptions settings) {
		super( region, settings );
		this.ehcache = ehcache;
	}

	@Override
	public Object get(CollectionCacheKey key, long txTimestamp) throws CacheException {
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
	public SoftLock lockItem(CollectionCacheKey key, Object version) throws CacheException {
		return null;
	}

	@Override
	public boolean putFromLoad(
			CollectionCacheKey key,
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
	public void remove(CollectionCacheKey key) throws CacheException {
		try {
			ehcache.remove( key );
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public void unlockItem(CollectionCacheKey key, SoftLock lock) throws CacheException {
		// no-op
	}

}
