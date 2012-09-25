package org.hibernate.cache.infinispan.impl;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.infinispan.util.Caches;
import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import org.hibernate.cache.CacheException;
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

   private static final Log log = LogFactory.getLog(BaseRegion.class);

   private enum InvalidateState {
      INVALID, CLEARING, VALID
   }

   private final String name;
   private final AdvancedCache regionClearCache;
   private final TransactionManager tm;
   private final Object invalidationMutex = new Object();
   private final AtomicReference<InvalidateState> invalidateState =
         new AtomicReference<InvalidateState>(InvalidateState.VALID);
   private final RegionFactory factory;

   protected final AdvancedCache cache;

   public BaseRegion(AdvancedCache cache, String name, RegionFactory factory) {
      this.cache = cache;
      this.name = name;
      this.tm = cache.getTransactionManager();
      this.factory = factory;
      this.regionClearCache = cache.withFlags(
            Flag.CACHE_MODE_LOCAL, Flag.ZERO_LOCK_ACQUISITION_TIMEOUT);
   }

   public String getName() {
      return name;
   }

   public long getElementCountInMemory() {
      if (checkValid())
         return cache.size();

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
         return cache;

      return Collections.EMPTY_MAP;
   }

   public void destroy() throws CacheException {
      try {
         cache.stop();
      } finally {
         cache.removeListener(this);
      }
   }

   public boolean contains(Object key) {
      return checkValid() && cache.containsKey(key);
   }

   public boolean checkValid() {
      boolean valid = isValid();
      if (!valid) {
         synchronized (invalidationMutex) {
            if (invalidateState.compareAndSet(
                  InvalidateState.INVALID, InvalidateState.CLEARING)) {
               Transaction tx = suspend();
               try {
                  // Clear region in a separate transaction
                  Caches.withinTx(cache, new Callable<Void>() {
                     @Override
                     public Void call() throws Exception {
                        regionClearCache.clear();
                        return null;
                     }
                  });
                  invalidateState.compareAndSet(
                        InvalidateState.CLEARING, InvalidateState.VALID);
               }
               catch (Exception e) {
                  if (log.isTraceEnabled()) {
                     log.trace("Could not invalidate region: "
                           + e.getLocalizedMessage());
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
    * Tell the TransactionManager to suspend any ongoing transaction.
    * 
    * @return the transaction that was suspended, or <code>null</code> if
    *         there wasn't one
    */
   public Transaction suspend() {
       Transaction tx = null;
       try {
           if (tm != null) {
               tx = tm.suspend();
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
               tm.resume(tx);
       } catch (Exception e) {
           throw new CacheException("Could not resume transaction", e);
       }
   }

   public void invalidateRegion() {
      if (log.isTraceEnabled()) log.trace("Invalidate region: " + name);
      invalidateState.set(InvalidateState.INVALID);
   }

   public TransactionManager getTransactionManager() {
      return tm;
   }

   // Used to satisfy TransactionalDataRegion.isTransactionAware in subclasses
   @SuppressWarnings("unused")
   public boolean isTransactionAware() {
      return tm != null;
   }

   public AdvancedCache getCache() {
      return cache;
   }

}
