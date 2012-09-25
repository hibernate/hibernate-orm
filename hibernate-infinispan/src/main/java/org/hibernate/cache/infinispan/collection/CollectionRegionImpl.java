package org.hibernate.cache.infinispan.collection;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.infinispan.AdvancedCache;

/**
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class CollectionRegionImpl extends BaseTransactionalDataRegion implements CollectionRegion {

   public CollectionRegionImpl(AdvancedCache cache, String name,
         CacheDataDescription metadata, RegionFactory factory) {
      super(cache, name, metadata, factory);
   }

   public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
      if (AccessType.READ_ONLY.equals(accessType)
            || AccessType.TRANSACTIONAL.equals(accessType))
         return new TransactionalAccess(this);

      throw new CacheException("Unsupported access type [" + accessType.getExternalName() + "]");
   }

   public PutFromLoadValidator getPutFromLoadValidator() {
      return new PutFromLoadValidator(cache);
   }

}
