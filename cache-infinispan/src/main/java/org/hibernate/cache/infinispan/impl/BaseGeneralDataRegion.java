package org.hibernate.cache.infinispan.impl;

import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.GeneralDataRegion;
import org.hibernate.cache.infinispan.util.CacheAdapter;

/**
 * Support for Infinispan {@link GeneralDataRegion} implementors.
 * 
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class BaseGeneralDataRegion extends BaseRegion implements GeneralDataRegion {

   public BaseGeneralDataRegion(CacheAdapter cacheAdapter, String name, TransactionManager transactionManager) {
      super(cacheAdapter, name, transactionManager);
   }

   public void evict(Object key) throws CacheException {
      cacheAdapter.evict(key);
   }

   public void evictAll() throws CacheException {
      cacheAdapter.clear();
   }

   public Object get(Object key) throws CacheException {
      return cacheAdapter.get(key);
   }

   public void put(Object key, Object value) throws CacheException {
      cacheAdapter.put(key, value);
   }

}