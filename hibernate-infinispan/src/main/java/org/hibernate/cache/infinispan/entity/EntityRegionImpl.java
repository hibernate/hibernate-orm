package org.hibernate.cache.infinispan.entity;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.infinispan.AdvancedCache;

/**
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class EntityRegionImpl extends BaseTransactionalDataRegion implements EntityRegion {

   public EntityRegionImpl(AdvancedCache cache, String name,
         CacheDataDescription metadata, RegionFactory factory) {
      super(cache, name, metadata, factory);
   }

   public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
      if (AccessType.READ_ONLY.equals(accessType)) {
         return new ReadOnlyAccess(this);
      } else if (AccessType.TRANSACTIONAL.equals(accessType)) {
         return new TransactionalAccess(this);
      }
      throw new CacheException("Unsupported access type [" + accessType.getExternalName() + "]");
   }

   public PutFromLoadValidator getPutFromLoadValidator() {
      return new PutFromLoadValidator(cache);
   }

}