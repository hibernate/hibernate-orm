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

import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.infinispan.Cache;

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

   protected final Cache cache;

   public TransactionalAccessDelegate(Cache cache) {
      this.cache = cache;
   }

   public Object get(Object key, long txTimestamp) throws CacheException {
      return cache.get(key);
   }

   public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
      cache.putForExternalRead(key, value);
      return true;
   }

   public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
            throws CacheException {
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
      cache.put(key, value);
      return true;
   }

   public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
      return false;
   }

   public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
      cache.put(key, value);
      return true;
   }

   public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
            throws CacheException {
      return false;
   }

   public void remove(Object key) throws CacheException {
      cache.remove(key);
   }

   public void removeAll() throws CacheException {
      cache.clear();
   }

   public void evictAll() throws CacheException {
      evictOrRemoveAll();
   }

   private void evictOrRemoveAll() throws CacheException {
      cache.clear();
   }
}
