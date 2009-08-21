package org.hibernate.cache.infinispan.collection;

import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.infinispan.Cache;

/**
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class CollectionRegionImpl extends BaseTransactionalDataRegion implements CollectionRegion {

   public CollectionRegionImpl(Cache<Object, Object> cache, String name, CacheDataDescription metadata, TransactionManager transactionManager) {
      super(cache, name, metadata, transactionManager);
   }

   public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
      if (AccessType.READ_ONLY.equals(accessType)) {
         return new ReadOnlyAccess(this);
      } else if (AccessType.TRANSACTIONAL.equals(accessType)) {
         return new TransactionalAccess(this);
      }
      throw new CacheException("Unsupported access type [" + accessType.getName() + "]");
   }

}
