package org.hibernate.cache.infinispan.query;

import java.util.Properties;

import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.CacheHelper;
import org.infinispan.Cache;
import org.infinispan.context.Flag;

/**
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class QueryResultsRegionImpl extends BaseTransactionalDataRegion implements QueryResultsRegion {
   private boolean localOnly;

   public QueryResultsRegionImpl(Cache<Object, Object> cache, String name, Properties properties, TransactionManager transactionManager) {
      super(cache, name, null, transactionManager);
      
      // If Infinispan is using INVALIDATION for query cache, we don't want to propagate changes.
      // We use the Timestamps cache to manage invalidation
      localOnly = CacheHelper.isClusteredInvalidation(cache);
   }

   public void evict(Object key) throws CacheException {
      if (localOnly)
         CacheHelper.removeKey(getCache(), key, Flag.CACHE_MODE_LOCAL);
      else 
         CacheHelper.removeKey(getCache(), key);
   }

   public void evictAll() throws CacheException {
      if (localOnly)
         CacheHelper.removeAll(getCache(), Flag.CACHE_MODE_LOCAL);
      else 
         CacheHelper.removeAll(getCache());
   }

   public Object get(Object key) throws CacheException {
      // Don't hold the JBC node lock throughout the tx, as that
      // prevents updates
      // Add a zero (or low) timeout option so we don't block
      // waiting for tx's that did a put to commit
      return suspendAndGet(key, Flag.ZERO_LOCK_ACQUISITION_TIMEOUT, true);
   }

   public void put(Object key, Object value) throws CacheException {
      // Here we don't want to suspend the tx. If we do:
      // 1) We might be caching query results that reflect uncommitted
      // changes. No tx == no WL on cache node, so other threads
      // can prematurely see those query results
      // 2) No tx == immediate replication. More overhead, plus we
      // spread issue #1 above around the cluster

      // Add a zero (or quite low) timeout option so we don't block.
      // Ignore any TimeoutException. Basically we forego caching the
      // query result in order to avoid blocking.
      // Reads are done with suspended tx, so they should not hold the
      // lock for long.  Not caching the query result is OK, since
      // any subsequent read will just see the old result with its
      // out-of-date timestamp; that result will be discarded and the
      // db query performed again.
      if (localOnly)
         CacheHelper.putAllowingTimeout(getCache(), key, value, Flag.ZERO_LOCK_ACQUISITION_TIMEOUT, Flag.CACHE_MODE_LOCAL);
      else 
         CacheHelper.putAllowingTimeout(getCache(), key, value, Flag.ZERO_LOCK_ACQUISITION_TIMEOUT);
      
   }

}