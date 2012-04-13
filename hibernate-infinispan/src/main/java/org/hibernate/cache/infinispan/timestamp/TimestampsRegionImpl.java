package org.hibernate.cache.infinispan.timestamp;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseGeneralDataRegion;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.infinispan.util.FlagAdapter;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;

/**
 * Defines the behavior of the timestamps cache region for Infinispan.
 * 
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
@Listener
public class TimestampsRegionImpl extends BaseGeneralDataRegion implements TimestampsRegion {

   private Map localCache = new ConcurrentHashMap();

   public TimestampsRegionImpl(CacheAdapter cacheAdapter, String name, TransactionManager transactionManager, RegionFactory factory) {
      super(cacheAdapter, name, transactionManager, factory);
      cacheAdapter.addListener(this);
      populateLocalCache();
   }

   @Override
   public void evict(Object key) throws CacheException {
      // TODO Is this a valid operation on a timestamps cache?
      cacheAdapter.remove(key);
   }

   public void evictAll() throws CacheException {
      // TODO Is this a valid operation on a timestamps cache?
      Transaction tx = suspend();
      try {
         invalidateRegion(); // Invalidate the local region and then go remote
         cacheAdapter.broadcastEvictAll();
      } finally {
         resume(tx);
      }
   }

   public Object get(Object key) throws CacheException {
      Object value = localCache.get(key);

      // If the region is not valid, skip cache store to avoid going remote to retrieve the query.
      // The aim of this is to maintain same logic/semantics as when state transfer was configured.
      // TODO: Once https://issues.jboss.org/browse/ISPN-835 has been resolved, revert to state transfer and remove workaround
      boolean skipCacheStore = false;
      if (!isValid())
         skipCacheStore = true;

      if (value == null && checkValid()) {
         if (skipCacheStore)
            value = get(key, false, FlagAdapter.SKIP_CACHE_STORE);
         else
            value = get(key, false);

         if (value != null)
            localCache.put(key, value);
      }
      return value;
   }

   public void put(final Object key, final Object value) throws CacheException {
      try {
         // We ensure ASYNC semantics (JBCACHE-1175) and make sure previous
         // value is not loaded from cache store cos it's not needed.
         cacheAdapter.withFlags(FlagAdapter.FORCE_ASYNCHRONOUS).put(key, value);
      } catch (Exception e) {
         throw new CacheException(e);
      }
   }

   @Override
   public void destroy() throws CacheException {
      localCache.clear();
      cacheAdapter.removeListener(this);
      super.destroy();
   }

   /**
    * Monitors cache events and updates the local cache
    * 
    * @param event
    */
   @CacheEntryModified
   @SuppressWarnings("unused")
   public void nodeModified(CacheEntryModifiedEvent event) {
      if (!event.isPre())
         localCache.put(event.getKey(), event.getValue());
   }

   /**
    * Monitors cache events and updates the local cache
    * 
    * @param event
    */
   @CacheEntryRemoved
   @SuppressWarnings("unused")
   public void nodeRemoved(CacheEntryRemovedEvent event) {
      if (event.isPre()) return;
      localCache.remove(event.getKey());
   }

   @Override
   public void invalidateRegion() {
      super.invalidateRegion(); // Invalidate first
      localCache.clear();
   }

   /**
    * Brings all data from the distributed cache into our local cache.
    */
   private void populateLocalCache() {
      Set children = cacheAdapter.keySet();
      for (Object key : children)
         get(key);
   }

}