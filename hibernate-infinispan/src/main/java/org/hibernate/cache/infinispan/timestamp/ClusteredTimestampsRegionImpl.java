/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.timestamp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.Transaction;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.infinispan.AdvancedCache;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;

/**
 * Timestamp cache region for clustered environments.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
@Listener
public class ClusteredTimestampsRegionImpl extends TimestampsRegionImpl {

	/**
	 * Maintains a local (authoritative) cache of timestamps along with the
	 * replicated cache held in Infinispan. It listens for changes in the
	 * cache and updates the local cache accordingly. This approach allows
	 * timestamp changes to be replicated asynchronously.
	 */
	private final Map localCache = new ConcurrentHashMap();

   /**
    * Clustered timestamps region constructor.
    *
    * @param cache instance to store update timestamps
    * @param name of the update timestamps region
    * @param factory for the update timestamps region
    */
	public ClusteredTimestampsRegionImpl(
			AdvancedCache cache,
			String name, InfinispanRegionFactory factory) {
		super( cache, name, factory );
		cache.addListener( this );
		populateLocalCache();
	}

	@Override
	protected AdvancedCache getTimestampsPutCache(AdvancedCache cache) {
		return Caches.asyncWriteCache( cache, Flag.SKIP_LOCKING );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object get(SharedSessionContractImplementor session, Object key) throws CacheException {
		Object value = localCache.get( key );

		if ( value == null && checkValid() ) {
			value = cache.get( key );

			if ( value != null ) {
				localCache.put( key, value );
			}
		}
		return value;
	}

	@Override
	public void evictAll() throws CacheException {
		// TODO Is this a valid operation on a timestamps cache?
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
	public void invalidateRegion() {
		// Invalidate first
		super.invalidateRegion();
		localCache.clear();
	}

	@Override
	public void destroy() throws CacheException {
		localCache.clear();
		cache.removeListener( this );
		super.destroy();
	}

	/**
	 * Brings all data from the distributed cache into our local cache.
	 */
	private void populateLocalCache() {
		CloseableIterator iterator = cache.keySet().iterator();
		try {
			while (iterator.hasNext()) {
				get(null, iterator.next());
			}
		}
		finally {
			iterator.close();
		}
	}

	/**
	 * Monitors cache events and updates the local cache
	 *
	 * @param event The event
	 */
	@CacheEntryModified
	@SuppressWarnings({"unused", "unchecked"})
	public void nodeModified(CacheEntryModifiedEvent event) {
		if ( !event.isPre() ) {
			localCache.put( event.getKey(), event.getValue() );
		}
	}

	/**
	 * Monitors cache events and updates the local cache
	 *
	 * @param event The event
	 */
	@CacheEntryRemoved
	@SuppressWarnings("unused")
	public void nodeRemoved(CacheEntryRemovedEvent event) {
		if ( event.isPre() ) {
			return;
		}
		localCache.remove( event.getKey() );
	}

}
