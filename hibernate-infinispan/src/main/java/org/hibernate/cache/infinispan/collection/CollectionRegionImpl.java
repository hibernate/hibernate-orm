package org.hibernate.cache.infinispan.collection;

import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.infinispan.notifications.Listener;

/**
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
@Listener
public class CollectionRegionImpl extends BaseTransactionalDataRegion implements CollectionRegion {

   public CollectionRegionImpl(CacheAdapter cacheAdapter, String name, CacheDataDescription metadata, 
            TransactionManager transactionManager, RegionFactory factory) {
      super(cacheAdapter, name, metadata, transactionManager, factory);
   }

   public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
      if (AccessType.READ_ONLY.equals(accessType)) {
         return new ReadOnlyAccess(this);
      } else if (AccessType.TRANSACTIONAL.equals(accessType)) {
         return new TransactionalAccess(this);
      }
      throw new CacheException("Unsupported access type [" + accessType.getName() + "]");
   }

   public PutFromLoadValidator getPutFromLoadValidator() {
      return new PutFromLoadValidator(transactionManager);
   }
}
