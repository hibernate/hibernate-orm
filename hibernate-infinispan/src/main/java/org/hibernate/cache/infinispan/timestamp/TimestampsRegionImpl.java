/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.timestamp;

import javax.transaction.Transaction;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseGeneralDataRegion;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;

import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;

/**
 * Defines the behavior of the timestamps cache region for Infinispan.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class TimestampsRegionImpl extends BaseGeneralDataRegion implements TimestampsRegion {

	private final AdvancedCache removeCache;
	private final AdvancedCache timestampsPutCache;

   /**
    * Local timestamps region constructor.
    *
    * @param cache instance to store update timestamps
    * @param name of the update timestamps region
    * @param factory for the update timestamps region
    */
	public TimestampsRegionImpl(
			AdvancedCache cache, String name,
			RegionFactory factory) {
		super( cache, name, factory );
		this.removeCache = Caches.ignoreReturnValuesCache( cache );

		// Skip locking when updating timestamps to provide better performance
		// under highly concurrent insert scenarios, where update timestamps
		// for an entity/collection type are constantly updated, creating
		// contention.
		//
		// The worst it can happen is that an earlier an earlier timestamp
		// (i.e. ts=1) will override a later on (i.e. ts=2), so it means that
		// in highly concurrent environments, queries might be considered stale
		// earlier in time. The upside is that inserts/updates are way faster
		// in local set ups.
		this.timestampsPutCache = getTimestampsPutCache( cache );
	}

	protected AdvancedCache getTimestampsPutCache(AdvancedCache cache) {
		return Caches.ignoreReturnValuesCache( cache, Flag.SKIP_LOCKING );
	}

	@Override
	public void evict(Object key) throws CacheException {
		// TODO Is this a valid operation on a timestamps cache?
		removeCache.remove( key );
	}

	@Override
	public void evictAll() throws CacheException {
		// TODO Is this a valid operation on a timestamps cache?
		final Transaction tx = suspend();
		try {
			// Invalidate the local region
			invalidateRegion();
		}
		finally {
			resume( tx );
		}
	}


	@Override
	public Object get(Object key) throws CacheException {
		if ( checkValid() ) {
			return cache.get( key );
		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void put(final Object key, final Object value) throws CacheException {
		try {
			// We ensure ASYNC semantics (JBCACHE-1175) and make sure previous
			// value is not loaded from cache store cos it's not needed.
			timestampsPutCache.put( key, value );
		}
		catch (Exception e) {
			throw new CacheException( e );
		}
	}

}
