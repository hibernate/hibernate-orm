/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.cache.infinispan.timestamp;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.Transaction;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.spi.RegionFactory;

import org.infinispan.AdvancedCache;
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
			String name, RegionFactory factory) {
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
	public Object get(Object key) throws CacheException {
		Object value = localCache.get( key );

		// If the region is not valid, skip cache store to avoid going remote to retrieve the query.
		// The aim of this is to maintain same logic/semantics as when state transfer was configured.
		// TODO: Once https://issues.jboss.org/browse/ISPN-835 has been resolved, revert to state transfer and remove workaround
		boolean skipCacheStore = false;
		if ( !isValid() ) {
			skipCacheStore = true;
		}

		if ( value == null && checkValid() ) {
			if ( skipCacheStore ) {
				value = cache.withFlags( Flag.SKIP_CACHE_STORE ).get( key );
			}
			else {
				value = cache.get( key );
			}

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
		final Set children = cache.keySet();
		for ( Object key : children ) {
			get( key );
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
