package org.hibernate.cache.infinispan.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.Region;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.infinispan.util.AddressAdapter;
import org.hibernate.cache.infinispan.util.AddressAdapterImpl;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.infinispan.util.CacheHelper;
import org.hibernate.cache.infinispan.util.FlagAdapter;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryInvalidatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Support for Infinispan {@link Region}s. Handles common "utility" methods for an underlying named
 * Cache. In other words, this implementation doesn't actually read or write data. Subclasses are
 * expected to provide core cache interaction appropriate to the semantics needed.
 * 
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class BaseRegion implements Region {
   private enum InvalidateState { INVALID, CLEARING, VALID };
   private static final Log log = LogFactory.getLog(BaseRegion.class);
   private final String name;
   protected final CacheAdapter cacheAdapter;
   protected final AddressAdapter address;
   protected final Set<AddressAdapter> currentView = new HashSet<AddressAdapter>();
   protected final TransactionManager transactionManager;
   protected final boolean replication;
   protected final Object invalidationMutex = new Object();
   protected final AtomicReference<InvalidateState> invalidateState = new AtomicReference<InvalidateState>(InvalidateState.VALID);
   private final RegionFactory factory;

   public BaseRegion(CacheAdapter cacheAdapter, String name, TransactionManager transactionManager, RegionFactory factory) {
      this.cacheAdapter = cacheAdapter;
      this.name = name;
      this.transactionManager = transactionManager;
      this.replication = cacheAdapter.isClusteredReplication();
      this.address = this.cacheAdapter.getAddress();
      this.cacheAdapter.addListener(this);
      this.factory = factory;
   }

   public void start() {
      if (address != null) {
         synchronized (currentView) {
            List<AddressAdapter> view = cacheAdapter.getMembers();
            if (view != null) {
               currentView.addAll(view);
               establishInternalNodes();
            }
         }
      }
   }

   /**
    * Calls to this method must be done from synchronized (currentView) blocks only!!
    */
   private void establishInternalNodes() {
      Transaction tx = suspend();
      try {
         for (AddressAdapter member : currentView) {
            CacheHelper.initInternalEvict(cacheAdapter, member);
         }
      } finally {
         resume(tx);
      }
   }

   public String getName() {
      return name;
   }

   public CacheAdapter getCacheAdapter() {
      return cacheAdapter;
   }

   public long getElementCountInMemory() {
      if (checkValid()) {
         Set keySet = cacheAdapter.keySet();
         int size = cacheAdapter.size();
         if (CacheHelper.containsEvictAllNotification(keySet, address))
            size--;
         return size;
      }
      return 0;
   }

   /**
    * Not supported.
    * 
    * @return -1
    */
   public long getElementCountOnDisk() {
      return -1;
   }

   /**
    * Not supported.
    * 
    * @return -1
    */
   public long getSizeInMemory() {
      return -1;
   }

   public int getTimeout() {
      return 600; // 60 seconds
   }

   public long nextTimestamp() {
      return factory.nextTimestamp();
   }

   public Map toMap() {
      if (checkValid()) {
         // If copying causes issues, provide a lazily loaded Map
         Map map = new HashMap();
         Set<Map.Entry> entries = cacheAdapter.toMap().entrySet();
         for (Map.Entry entry : entries) {
            Object key = entry.getKey();
            if (!CacheHelper.isEvictAllNotification(key)) {
               map.put(key, entry.getValue());
            }
         }
         return map;
      }
      return Collections.EMPTY_MAP;
   }

   public void destroy() throws CacheException {
      try {
         cacheAdapter.clear();
      } finally {
         cacheAdapter.removeListener(this);
      }
   }

   public boolean contains(Object key) {
      if (!checkValid())
         return false;
      // Reads are non-blocking in Infinispan, so not sure of the necessity of passing ZERO_LOCK_ACQUISITION_TIMEOUT
      return cacheAdapter.withFlags(FlagAdapter.ZERO_LOCK_ACQUISITION_TIMEOUT).containsKey(key);
   }

   public AddressAdapter getAddress() {
      return address;
   }

   public boolean checkValid() {
      boolean valid = invalidateState.get() == InvalidateState.VALID;
      if (!valid) {
         synchronized (invalidationMutex) {
            if (invalidateState.compareAndSet(InvalidateState.INVALID, InvalidateState.CLEARING)) {
               Transaction tx = suspend();
               try {
                  cacheAdapter.withFlags(FlagAdapter.CACHE_MODE_LOCAL, FlagAdapter.ZERO_LOCK_ACQUISITION_TIMEOUT).clear();
                  invalidateState.compareAndSet(InvalidateState.CLEARING, InvalidateState.VALID);
               }
               catch (Exception e) {
                  if (log.isTraceEnabled()) {
                     log.trace("Could not invalidate region: " + e.getLocalizedMessage());
                  }
               }
               finally {
                  resume(tx);
               }
            }
         }
         valid = invalidateState.get() == InvalidateState.VALID;
      }
      
      return valid;
   }

   /**
    * Performs a Infinispan <code>get(Fqn, Object)</code>
    *
    * @param key The key of the item to get
    * @param opt any option to add to the get invocation. May be <code>null</code>
    * @param suppressTimeout should any TimeoutException be suppressed?
    * @return The retrieved object
      * @throws CacheException issue managing transaction or talking to cache
    */
   protected Object get(Object key, FlagAdapter opt, boolean suppressTimeout) throws CacheException {
      if (suppressTimeout)
         return cacheAdapter.getAllowingTimeout(key);
      else
         return cacheAdapter.get(key);
   }
   
   public Object getOwnerForPut() {
      Transaction tx = null;
      try {
          if (transactionManager != null) {
              tx = transactionManager.getTransaction();
          }
      } catch (SystemException se) {
          throw new CacheException("Could not obtain transaction", se);
      }
      return tx == null ? Thread.currentThread() : tx;
   }

   /**
    * Tell the TransactionManager to suspend any ongoing transaction.
    * 
    * @return the transaction that was suspended, or <code>null</code> if
    *         there wasn't one
    */
   public Transaction suspend() {
       Transaction tx = null;
       try {
           if (transactionManager != null) {
               tx = transactionManager.suspend();
           }
       } catch (SystemException se) {
           throw new CacheException("Could not suspend transaction", se);
       }
       return tx;
   }

   /**
    * Tell the TransactionManager to resume the given transaction
    * 
    * @param tx
    *            the transaction to suspend. May be <code>null</code>.
    */
   public void resume(Transaction tx) {
       try {
           if (tx != null)
               transactionManager.resume(tx);
       } catch (Exception e) {
           throw new CacheException("Could not resume transaction", e);
       }
   }

   @CacheEntryModified
   public void entryModified(CacheEntryModifiedEvent event) {
      handleEvictAllModification(event);
   }

   protected boolean handleEvictAllModification(CacheEntryModifiedEvent event) {
      if (!event.isPre() && (replication || event.isOriginLocal()) && CacheHelper.isEvictAllNotification(event.getKey(), event.getValue())) {
         if (log.isTraceEnabled()) log.trace("Set invalid state because marker cache entry was put: {0}", event);
         invalidateState.set(InvalidateState.INVALID);
         return true;
      }
      return false;
   }

   @CacheEntryInvalidated
   public void entryInvalidated(CacheEntryInvalidatedEvent event) {
      if (log.isTraceEnabled()) log.trace("Cache entry invalidated: {0}", event);
      handleEvictAllInvalidation(event);
   }

   protected boolean handleEvictAllInvalidation(CacheEntryInvalidatedEvent event) {
      if (!event.isPre() && CacheHelper.isEvictAllNotification(event.getKey())) {
         if (log.isTraceEnabled()) log.trace("Set invalid state because marker cache entry was invalidated: {0}", event);
         invalidateState.set(InvalidateState.INVALID);
         return true;
      }
      return false;
   }

   @ViewChanged
   public void viewChanged(ViewChangedEvent event) {
      synchronized (currentView) {
         List<AddressAdapter> view = AddressAdapterImpl.toAddressAdapter(event.getNewMembers());
         if (view != null) {
            currentView.addAll(view);
            establishInternalNodes();
         }
      }
   }

}