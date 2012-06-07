package org.hibernate.cache.infinispan.impl;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.util.AddressAdapter;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.infinispan.util.FlagAdapter;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;

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
      this.factory = factory;
   }

   public String getName() {
      return name;
   }

   public CacheAdapter getCacheAdapter() {
      return cacheAdapter;
   }

   public long getElementCountInMemory() {
      if (checkValid())
         return cacheAdapter.size();

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
      if (checkValid())
         return cacheAdapter.toMap();

      return Collections.EMPTY_MAP;
   }

   public void destroy() throws CacheException {
      try {
         cacheAdapter.stop();
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
      boolean valid = isValid();
      if (!valid) {
         synchronized (invalidationMutex) {
            if (invalidateState.compareAndSet(InvalidateState.INVALID, InvalidateState.CLEARING)) {
               Transaction tx = suspend();
               try {
                  // Clear region in a separate transaction
                  cacheAdapter.withinTx(new Callable<Void>() {
                     @Override
                     public Void call() throws Exception {
                        cacheAdapter.withFlags(FlagAdapter.CACHE_MODE_LOCAL,
                              FlagAdapter.ZERO_LOCK_ACQUISITION_TIMEOUT).clear();
                        return null;
                     }
                  });
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
         valid = isValid();
      }
      
      return valid;
   }



   protected boolean isValid() {
      return invalidateState.get() == InvalidateState.VALID;
   }

   /**
    * Performs a Infinispan <code>get(Fqn, Object)</code>
    *
    * @param key The key of the item to get
    * @param suppressTimeout should any TimeoutException be suppressed?
    * @param flagAdapters flags to add to the get invocation
    * @return The retrieved object
    * @throws CacheException issue managing transaction or talking to cache
    */
   protected Object get(Object key, boolean suppressTimeout, FlagAdapter... flagAdapters) throws CacheException {
      CacheAdapter localCacheAdapter = cacheAdapter;
      if (flagAdapters != null && flagAdapters.length > 0)
         localCacheAdapter = cacheAdapter.withFlags(flagAdapters);

      if (suppressTimeout)
         return localCacheAdapter.getAllowingTimeout(key);
      else
         return localCacheAdapter.get(key);
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

   public void invalidateRegion() {
      if (log.isTraceEnabled()) log.trace("Invalidate region: " + name);
      invalidateState.set(InvalidateState.INVALID);
   }

   public TransactionManager getTransactionManager() {
      return transactionManager;
   }

}
