package org.hibernate.cache.infinispan.impl;

import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.GeneralDataRegion;
import org.infinispan.Cache;

/**
 * Support for Infinispan {@link GeneralDataRegion} implementors.
 * 
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class BaseGeneralDataRegion extends BaseRegion implements GeneralDataRegion {

   public BaseGeneralDataRegion(Cache<Object, Object> cache, String name, TransactionManager transactionManager) {
      super(cache, name, transactionManager);
   }

   public void evict(Object key) throws CacheException {
      getCache().evict(key);
   }

   public void evictAll() throws CacheException {
      getCache().clear();
   }

   public Object get(Object key) throws CacheException {
      return getCache().get(key);
   }

   public void put(Object key, Object value) throws CacheException {
      getCache().put(key, value);
   }

}