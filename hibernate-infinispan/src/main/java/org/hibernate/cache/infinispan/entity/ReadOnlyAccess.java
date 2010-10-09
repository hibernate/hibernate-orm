package org.hibernate.cache.infinispan.entity;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.SoftLock;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A specialization of {@link TransactionalAccess} that ensures we never update data. Infinispan
 * access is always transactional.
 * 
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
class ReadOnlyAccess extends TransactionalAccess {
   private static final Log log = LogFactory.getLog(ReadOnlyAccess.class);

   ReadOnlyAccess(EntityRegionImpl region) {
      super(region);
   }

   public SoftLock lockItem(Object key, Object version) throws CacheException {
      throw new UnsupportedOperationException("Illegal attempt to edit read only item");
   }

   public SoftLock lockRegion() throws CacheException {
      throw new UnsupportedOperationException("Illegal attempt to edit read only item");
   }

   public void unlockItem(Object key, SoftLock lock) throws CacheException {
      log.error("Illegal attempt to edit read only item");
   }

   public void unlockRegion(SoftLock lock) throws CacheException {
      log.error("Illegal attempt to edit read only item");
   }

   @Override
   public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
      throw new UnsupportedOperationException("Illegal attempt to edit read only item");
   }

   @Override
   public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
            throws CacheException {
      throw new UnsupportedOperationException("Illegal attempt to edit read only item");
   }
}