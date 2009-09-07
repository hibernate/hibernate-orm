package org.hibernate.cache.infinispan.impl;

import java.util.Map;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.Region;
import org.hibernate.cache.infinispan.util.CacheHelper;
import org.infinispan.Cache;
import org.infinispan.context.Flag;

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
   private final Cache cache;
   private final String name;
   protected final TransactionManager transactionManager;

   public BaseRegion(Cache cache, String name, TransactionManager transactionManager) {
      this.cache = cache;
      this.name = name;
      this.transactionManager = transactionManager;
   }

   public Cache getCache() {
      return cache;
   }

   public String getName() {
      return name;
   }

   public long getElementCountInMemory() {
      return cache.size();
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
      return System.currentTimeMillis() / 100;
   }

   public Map toMap() {
      return cache;
   }

   public void destroy() throws CacheException {
      cache.clear();
   }
   
   public boolean contains(Object key) {
      return CacheHelper.containsKey(cache, key, Flag.ZERO_LOCK_ACQUISITION_TIMEOUT);
   }
   
   /**
    * Performs a JBoss Cache <code>get(Fqn, Object)</code> after first
    * {@link #suspend suspending any ongoing transaction}. Wraps any exception
    * in a {@link CacheException}. Ensures any ongoing transaction is resumed.
    * 
    * @param key The key of the item to get
    * @param opt any option to add to the get invocation. May be <code>null</code>
    * @param suppressTimeout should any TimeoutException be suppressed?
    * @return The retrieved object
      * @throws CacheException issue managing transaction or talking to cache
    */
   protected Object suspendAndGet(Object key, Flag opt, boolean suppressTimeout) throws CacheException {
       Transaction tx = suspend();
       try {
           if (suppressTimeout)
               return CacheHelper.getAllowingTimeout(cache, key);
           else
               return CacheHelper.get(cache, key);
       } finally {
           resume(tx);
       }
   }
   
   /**
    * Tell the TransactionManager to suspend any ongoing transaction.
    * 
    * @return the transaction that was suspended, or <code>null</code> if
    *         there wasn't one
    */
   protected Transaction suspend() {
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
   protected void resume(Transaction tx) {
       try {
           if (tx != null)
               transactionManager.resume(tx);
       } catch (Exception e) {
           throw new CacheException("Could not resume transaction", e);
       }
   }
   
//   /**
//    * HACKY WAY TO GET THE TRANSACTION MANAGER, TODO: resolve it!
//    */
//   private static TransactionManager getTransactionManager(Properties properties) {
////      return cache == null ? null : extractComponent(cache, TransactionManager.class);
//      return TransactionManagerLookupFactory.getTransactionManager(properties);
//   }
//   
//   public static <T> T extractComponent(Cache cache, Class<T> componentType) {
//      ComponentRegistry cr = extractComponentRegistry(cache);
//      return cr.getComponent(componentType);
//   }
//   
//   public static ComponentRegistry extractComponentRegistry(Cache cache) {
//      return (ComponentRegistry) extractField(cache, "componentRegistry");
//   }
//   
//   public static Object extractField(Object target, String fieldName) {
//      return extractField(target.getClass(), target, fieldName);
//   }
//   
//   public static Object extractField(Class type, Object target, String fieldName) {
//      Field field;
//      try {
//         field = type.getDeclaredField(fieldName);
//         field.setAccessible(true);
//         return field.get(target);
//      }
//      catch (Exception e) {
//         if (type.equals(Object.class)) {
//            e.printStackTrace();
//            return null;
//         } else {
//            // try with superclass!!
//            return extractField(type.getSuperclass(), target, fieldName);
//         }
//      }
//   }

}