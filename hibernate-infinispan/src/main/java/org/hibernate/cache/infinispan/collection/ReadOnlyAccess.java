package org.hibernate.cache.infinispan.collection;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;

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
   ReadOnlyAccess(CollectionRegionImpl region) {
      super(region);
   }

}
