/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.infinispan.query;

import javax.transaction.Transaction;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;

import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;

/**
 * Region for caching query results.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class QueryResultsRegionImpl extends BaseTransactionalDataRegion implements QueryResultsRegion {

	private final AdvancedCache evictCache;
	private final AdvancedCache putCache;
	private final AdvancedCache getCache;

   /**
    * Query region constructor
    *
    * @param cache instance to store queries
    * @param name of the query region
    * @param factory for the query region
    */
	public QueryResultsRegionImpl(AdvancedCache cache, String name, RegionFactory factory) {
		super( cache, name, null, factory );
		// If Infinispan is using INVALIDATION for query cache, we don't want to propagate changes.
		// We use the Timestamps cache to manage invalidation
		final boolean localOnly = Caches.isInvalidationCache( cache );

		this.evictCache = localOnly ? Caches.localCache( cache ) : cache;

		this.putCache = localOnly ?
				Caches.failSilentWriteCache( cache, Flag.CACHE_MODE_LOCAL ) :
				Caches.failSilentWriteCache( cache );

		this.getCache = Caches.failSilentReadCache( cache );
	}

	@Override
	public void evict(Object key) throws CacheException {
		evictCache.remove( key );
	}

	@Override
	public void evictAll() throws CacheException {
		final Transaction tx = suspend();
		try {
			// Invalidate the local region and then go remote
			invalidateRegion();
			Caches.broadcastEvictAll( cache );
		}
		finally {
			resume( tx );
		}
	}

	@Override
	public Object get(Object key) throws CacheException {
		// If the region is not valid, skip cache store to avoid going remote to retrieve the query.
		// The aim of this is to maintain same logic/semantics as when state transfer was configured.
		// TODO: Once https://issues.jboss.org/browse/ISPN-835 has been resolved, revert to state transfer and remove workaround
		boolean skipCacheStore = false;
		if ( !isValid() ) {
			skipCacheStore = true;
		}

		if ( !checkValid() ) {
			return null;
		}

		// In Infinispan get doesn't acquire any locks, so no need to suspend the tx.
		// In the past, when get operations acquired locks, suspending the tx was a way
		// to avoid holding locks that would prevent updates.
		// Add a zero (or low) timeout option so we don't block
		// waiting for tx's that did a put to commit
		if ( skipCacheStore ) {
			return getCache.withFlags( Flag.SKIP_CACHE_STORE ).get( key );
		}
		else {
			return getCache.get( key );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void put(Object key, Object value) throws CacheException {
		if ( checkValid() ) {
			// Here we don't want to suspend the tx. If we do:
			// 1) We might be caching query results that reflect uncommitted
			// changes. No tx == no WL on cache node, so other threads
			// can prematurely see those query results
			// 2) No tx == immediate replication. More overhead, plus we
			// spread issue #1 above around the cluster

			// Add a zero (or quite low) timeout option so we don't block.
			// Ignore any TimeoutException. Basically we forego caching the
			// query result in order to avoid blocking.
			// Reads are done with suspended tx, so they should not hold the
			// lock for long.  Not caching the query result is OK, since
			// any subsequent read will just see the old result with its
			// out-of-date timestamp; that result will be discarded and the
			// db query performed again.
			putCache.put( key, value );
		}
	}

}
