/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.infinispan.access;

import javax.transaction.Transaction;

import org.hibernate.cache.infinispan.util.Caches;
import org.infinispan.AdvancedCache;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

/**
 * Defines the strategy for transactional access to entity or collection data in a Infinispan instance.
 * <p>
 * The intent of this class is to encapsulate common code and serve as a delegate for
 * {@link EntityRegionAccessStrategy} and {@link CollectionRegionAccessStrategy} implementations.
 * 
 * @author Brian Stansberry
 * @author Galder Zamarreño
 * @since 3.5
 */
public class TransactionalAccessDelegate {
   private static final Log log = LogFactory.getLog(TransactionalAccessDelegate.class);
   private static final boolean isTrace = log.isTraceEnabled();
   private final AdvancedCache cache;
   private final BaseRegion region;
   private final PutFromLoadValidator putValidator;
   private final AdvancedCache<Object, Object> writeCache;

   public TransactionalAccessDelegate(BaseRegion region, PutFromLoadValidator validator) {
      this.region = region;
      this.cache = region.getCache();
      this.putValidator = validator;
      this.writeCache = Caches.ignoreReturnValuesCache(cache);
   }

   public Object get(Object key, long txTimestamp) throws CacheException {
      if (!region.checkValid()) 
         return null;
      Object val = cache.get(key);
      if (val == null)
         putValidator.registerPendingPut(key);
      return val;
   }

   public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) {
      return putFromLoad(key, value, txTimestamp, version, false);
   }

   public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
            throws CacheException {
      if (!region.checkValid()) {
         if (isTrace) log.tracef("Region %s not valid", region.getName());
         return false;
      }

      // In theory, since putForExternalRead is already as minimal as it can
      // get, we shouldn't be need this check. However, without the check and
      // without https://issues.jboss.org/browse/ISPN-1986, it's impossible to
      // know whether the put actually occurred. Knowing this is crucial so
      // that Hibernate can expose accurate statistics.
      if (minimalPutOverride && cache.containsKey(key))
         return false;

      if (!putValidator.acquirePutFromLoadLock(key)) {
         if (isTrace) log.tracef("Put from load lock not acquired for key %s", key);
         return false;
      }

      try {
         writeCache.putForExternalRead(key, value);
      } finally {
         putValidator.releasePutFromLoadLock(key);
      }

      return true;
   }

   public SoftLock lockItem(Object key, Object version) throws CacheException {
      return null;
   }

   public SoftLock lockRegion() throws CacheException {
      return null;
   }

   public void unlockItem(Object key, SoftLock lock) throws CacheException {
   }

   public void unlockRegion(SoftLock lock) throws CacheException {
   }

   public boolean insert(Object key, Object value, Object version) throws CacheException {
      if (!region.checkValid())
         return false;

      writeCache.put(key, value);
      return true;
   }

   public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
      return false;
   }

   public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
      // We update whether or not the region is valid. Other nodes
      // may have already restored the region so they need to
      // be informed of the change.
      writeCache.put(key, value);
      return true;
   }

   public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
            throws CacheException {
      return false;
   }

   public void remove(Object key) throws CacheException {
      if (!putValidator.invalidateKey(key)) {
         throw new CacheException("Failed to invalidate pending putFromLoad calls for key " + key + " from region " + region.getName());
      }
      // We update whether or not the region is valid. Other nodes
      // may have already restored the region so they need to
      // be informed of the change.
      writeCache.remove(key);
   }

   public void removeAll() throws CacheException {
       if (!putValidator.invalidateRegion()) {
         throw new CacheException("Failed to invalidate pending putFromLoad calls for region " + region.getName());
       }
      cache.clear();
   }

   public void evict(Object key) throws CacheException {
      if (!putValidator.invalidateKey(key)) {
         throw new CacheException("Failed to invalidate pending putFromLoad calls for key " + key + " from region " + region.getName());
      }
      writeCache.remove(key);
   }

   public void evictAll() throws CacheException {
      if (!putValidator.invalidateRegion()) {
         throw new CacheException("Failed to invalidate pending putFromLoad calls for region " + region.getName());
      }
      Transaction tx = region.suspend();
      try {
         region.invalidateRegion(); // Invalidate the local region and then go remote
         Caches.broadcastEvictAll(cache);
      } finally {
         region.resume(tx);
      }
   }

}
