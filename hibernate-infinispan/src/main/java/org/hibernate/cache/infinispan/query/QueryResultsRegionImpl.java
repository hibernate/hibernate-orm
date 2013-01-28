package org.hibernate.cache.infinispan.query;

import javax.transaction.Transaction;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;

/**
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class QueryResultsRegionImpl extends BaseTransactionalDataRegion implements QueryResultsRegion {

   private final AdvancedCache evictCache;
   private final AdvancedCache putCache;
   private final AdvancedCache getCache;

   public QueryResultsRegionImpl(AdvancedCache cache, String name, RegionFactory factory) {
      super(cache, name, null, factory);
      // If Infinispan is using INVALIDATION for query cache, we don't want to propagate changes.
      // We use the Timestamps cache to manage invalidation
      boolean localOnly = Caches.isInvalidationCache(cache);

      this.evictCache = localOnly ? Caches.localCache(cache) : cache;

      this.putCache = localOnly ?
            Caches.failSilentWriteCache(cache, Flag.CACHE_MODE_LOCAL) :
            Caches.failSilentWriteCache(cache);

      this.getCache = Caches.failSilentReadCache(cache);
   }

   public void evict(Object key) throws CacheException {
      evictCache.remove(key);
   }

   public void evictAll() throws CacheException {
      Transaction tx = suspend();
      try {
         invalidateRegion(); // Invalidate the local region and then go remote
         Caches.broadcastEvictAll(cache);
      } finally {
         resume(tx);
      }
   }

   public Object get(Object key) throws CacheException {
      // If the region is not valid, skip cache store to avoid going remote to retrieve the query.
      // The aim of this is to maintain same logic/semantics as when state transfer was configured.
      // TODO: Once https://issues.jboss.org/browse/ISPN-835 has been resolved, revert to state transfer and remove workaround
      boolean skipCacheStore = false;
      if (!isValid())
         skipCacheStore = true;

      if (!checkValid())
         return null;

      // In Infinispan get doesn't acquire any locks, so no need to suspend the tx.
      // In the past, when get operations acquired locks, suspending the tx was a way
      // to avoid holding locks that would prevent updates.
      // Add a zero (or low) timeout option so we don't block
      // waiting for tx's that did a put to commit
      if (skipCacheStore)
         return getCache.withFlags(Flag.SKIP_CACHE_STORE).get(key);
      else
         return getCache.get(key);
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
         putCache.put(key, value);
      }
   }

}