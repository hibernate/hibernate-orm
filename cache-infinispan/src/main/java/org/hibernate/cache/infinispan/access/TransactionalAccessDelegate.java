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

import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.infinispan.util.CacheHelper;
import org.hibernate.cache.infinispan.util.FlagAdapter;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

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
   protected final CacheAdapter cacheAdapter;
   protected final BaseRegion region;
   protected final PutFromLoadValidator putValidator;

   public TransactionalAccessDelegate(BaseRegion region, PutFromLoadValidator validator) {
      this.region = region;
      this.cacheAdapter = region.getCacheAdapter();
      this.putValidator = validator;
   }

   public Object get(Object key, long txTimestamp) throws CacheException {
      if (!region.checkValid()) 
         return null;
      Object val = cacheAdapter.get(key);
      if (val == null)
         putValidator.registerPendingPut(key);
      return val;
   }

   public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
      if (!region.checkValid())
         return false;

      if (!putValidator.acquirePutFromLoadLock(key))
         return false;

      try {
         cacheAdapter.putForExternalRead(key, value);
      } finally {
         putValidator.releasePutFromLoadLock(key);
      }

      return true;
   }

   public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
            throws CacheException {
      // We ignore minimalPutOverride. Infinispan putForExternalRead is
      // already about as minimal as we can get; it will promptly return
      // if it discovers that the node we want to write to already exists
      return putFromLoad(key, value, txTimestamp, version);
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

      if (cacheAdapter.isClusteredInvalidation())
         cacheAdapter.withFlags(FlagAdapter.CACHE_MODE_LOCAL).put(key, value);
      else
         cacheAdapter.put(key, value);

      return true;
   }

   public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
      return false;
   }

   public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
      // We update whether or not the region is valid. Other nodes
      // may have already restored the region so they need to
      // be informed of the change.
      cacheAdapter.put(key, value);
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
      cacheAdapter.remove(key);
   }

   public void removeAll() throws CacheException {
       if (!putValidator.invalidateRegion()) {
         throw new CacheException("Failed to invalidate pending putFromLoad calls for region " + region.getName());
       }
      cacheAdapter.clear();
   }

   public void evict(Object key) throws CacheException {
      if (!putValidator.invalidateKey(key)) {
         throw new CacheException("Failed to invalidate pending putFromLoad calls for key " + key + " from region " + region.getName());
      }      
      cacheAdapter.remove(key);
   }

   public void evictAll() throws CacheException {
      if (!putValidator.invalidateRegion()) {
         throw new CacheException("Failed to invalidate pending putFromLoad calls for region " + region.getName());
      }
      Transaction tx = region.suspend();
      try {
         CacheHelper.sendEvictAllNotification(cacheAdapter, region.getAddress());
      } finally {
         region.resume(tx);
      }
   }
}
