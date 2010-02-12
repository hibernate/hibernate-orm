package org.hibernate.cache.infinispan.timestamp;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.infinispan.impl.BaseGeneralDataRegion;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.infinispan.util.CacheHelper;
import org.hibernate.cache.infinispan.util.FlagAdapter;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryInvalidatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;

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
         CacheHelper.sendEvictAllNotification(cacheAdapter, getAddress());
      } finally {
         resume(tx);
      }
   }

   public Object get(Object key) throws CacheException {
      Object value = localCache.get(key);
      if (value == null && checkValid()) {
         value = get(key, null, false);
         if (value != null)
            localCache.put(key, value);
      }
      return value;
   }

   public void put(Object key, Object value) throws CacheException {
      // Don't hold the JBC node lock throughout the tx, as that
      // prevents reads and other updates
      Transaction tx = suspend();
      try {
         // We ensure ASYNC semantics (JBCACHE-1175)
         cacheAdapter.withFlags(FlagAdapter.FORCE_ASYNCHRONOUS).put(key, value);
      } catch (Exception e) {
         throw new CacheException(e);
      } finally {
         resume(tx);
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
   public void nodeModified(CacheEntryModifiedEvent event) {
      if (!handleEvictAllModification(event) && !event.isPre()) {
         localCache.put(event.getKey(), event.getValue());
      }
   }

   /**
    * Monitors cache events and updates the local cache
    * 
    * @param event
    */
   @CacheEntryRemoved
   public void nodeRemoved(CacheEntryRemovedEvent event) {
      if (event.isPre()) return;
      localCache.remove(event.getKey());
   }

   @Override
   protected boolean handleEvictAllModification(CacheEntryModifiedEvent event) {
      boolean result = super.handleEvictAllModification(event);
      if (result) {
         localCache.clear();
      }
      return result;
   }

   @Override
   protected boolean handleEvictAllInvalidation(CacheEntryInvalidatedEvent event) {
      boolean result = super.handleEvictAllInvalidation(event);
      if (result) {
         localCache.clear();
      }
      return result;
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