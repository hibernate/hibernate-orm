package org.hibernate.cache.infinispan.timestamp;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.Transaction;

import org.hibernate.cache.infinispan.util.Caches;
import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseGeneralDataRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;

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

   public TimestampsRegionImpl(AdvancedCache cache, String name,
         RegionFactory factory) {
      super(cache, name, factory);
      this.removeCache = Caches.ignoreReturnValuesCache(cache);

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
      this.timestampsPutCache = getTimestampsPutCache(cache);
   }

   protected AdvancedCache getTimestampsPutCache(AdvancedCache cache) {
      return Caches.ignoreReturnValuesCache(cache, Flag.SKIP_LOCKING);
   }

   @Override
   public void evict(Object key) throws CacheException {
      // TODO Is this a valid operation on a timestamps cache?
      removeCache.remove(key);
   }

   public void evictAll() throws CacheException {
      // TODO Is this a valid operation on a timestamps cache?
      Transaction tx = suspend();
      try {
         invalidateRegion(); // Invalidate the local region
      } finally {
         resume(tx);
      }
   }

   public Object get(Object key) throws CacheException {
      if (checkValid())
         return cache.get(key);

      return null;
   }

   public void put(final Object key, final Object value) throws CacheException {
      try {
         // We ensure ASYNC semantics (JBCACHE-1175) and make sure previous
         // value is not loaded from cache store cos it's not needed.
         timestampsPutCache.put(key, value);
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

}