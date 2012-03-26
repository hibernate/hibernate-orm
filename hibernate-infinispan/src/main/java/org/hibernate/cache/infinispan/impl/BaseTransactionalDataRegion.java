package org.hibernate.cache.infinispan.impl;

import javax.transaction.TransactionManager;

import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TransactionalDataRegion;

/**
 * Support for Inifinispan {@link org.hibernate.cache.spi.TransactionalDataRegion} implementors.
 * 
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class BaseTransactionalDataRegion
      extends BaseRegion implements TransactionalDataRegion {

   private final CacheDataDescription metadata;

   public BaseTransactionalDataRegion(CacheAdapter cacheAdapter, String name,
            CacheDataDescription metadata, TransactionManager transactionManager,
            RegionFactory factory) {
      super(cacheAdapter, name, transactionManager, factory);
      this.metadata = metadata;
   }

   public CacheDataDescription getCacheDataDescription() {
      return metadata;
   }

   public boolean isTransactionAware() {
      return transactionManager != null;
   }

}