package org.hibernate.cache.infinispan.impl;

import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TransactionalDataRegion;
import org.infinispan.AdvancedCache;

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

   public BaseTransactionalDataRegion(AdvancedCache cache, String name,
         CacheDataDescription metadata, RegionFactory factory) {
      super(cache, name, factory);
      this.metadata = metadata;
   }

	@Override
   public CacheDataDescription getCacheDataDescription() {
      return metadata;
   }

}