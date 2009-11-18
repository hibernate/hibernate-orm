package org.hibernate.cache.infinispan.collection;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.SoftLock;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * This defines the strategy for transactional access to collection data in a
 * Infinispan instance.
 * <p/>
 * The read-only access to a Infinispan really is still transactional, just with 
 * the extra semantic or guarantee that we will not update data.
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
class ReadOnlyAccess extends TransactionalAccess {
   private static final Log log = LogFactory.getLog(ReadOnlyAccess.class);

   ReadOnlyAccess(CollectionRegionImpl region) {
      super(region);
   }
   public SoftLock lockItem(Object key, Object version) throws CacheException {
      throw new UnsupportedOperationException("Illegal attempt to edit read only item");
   }

   public SoftLock lockRegion() throws CacheException {
      throw new UnsupportedOperationException("Illegal attempt to edit read only region");
   }

   public void unlockItem(Object key, SoftLock lock) throws CacheException {
      log.error("Illegal attempt to edit read only item");
   }

   public void unlockRegion(SoftLock lock) throws CacheException {
      log.error("Illegal attempt to edit read only item");
   }

}
