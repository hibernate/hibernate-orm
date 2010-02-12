package org.hibernate.cache.infinispan.query;

import java.util.Properties;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.infinispan.util.CacheHelper;
import org.hibernate.cache.infinispan.util.FlagAdapter;
import org.infinispan.notifications.Listener;

/**
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
@Listener
public class QueryResultsRegionImpl extends BaseTransactionalDataRegion implements QueryResultsRegion {
   private boolean localOnly;

   public QueryResultsRegionImpl(CacheAdapter cacheAdapter, String name, Properties properties, TransactionManager transactionManager, RegionFactory factory) {
      super(cacheAdapter, name, null, transactionManager, factory);
      // If Infinispan is using INVALIDATION for query cache, we don't want to propagate changes.
      // We use the Timestamps cache to manage invalidation
      localOnly = cacheAdapter.isClusteredInvalidation();
   }

   public void evict(Object key) throws CacheException {
      if (localOnly)
         cacheAdapter.withFlags(FlagAdapter.CACHE_MODE_LOCAL).remove(key);
      else 
         cacheAdapter.remove(key);
   }

   public void evictAll() throws CacheException {
      Transaction tx = suspend();
      try {
         CacheHelper.sendEvictAllNotification(cacheAdapter, getAddress());
      } finally {
         resume(tx);
      }
   }

   public Object get(Object key) throws CacheException {
      if (!checkValid())
         return null;

      // In Infinispan get doesn't acquire any locks, so no need to suspend the tx.
      // In the past, when get operations acquired locks, suspending the tx was a way
      // to avoid holding locks that would prevent updates.
      // Add a zero (or low) timeout option so we don't block
      // waiting for tx's that did a put to commit
      return get(key, FlagAdapter.ZERO_LOCK_ACQUISITION_TIMEOUT, true);
   }   

   public void put(Object key, Object value) throws CacheException {
      if (checkValid()) {
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
            cacheAdapter.withFlags(FlagAdapter.ZERO_LOCK_ACQUISITION_TIMEOUT, FlagAdapter.CACHE_MODE_LOCAL)
               .putAllowingTimeout(key, value);
         else 
            cacheAdapter.withFlags(FlagAdapter.ZERO_LOCK_ACQUISITION_TIMEOUT)
               .putAllowingTimeout(key, value);
      }
   }

}