package org.hibernate.cache.infinispan.entity;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.infinispan.access.TransactionalAccessDelegate;

/**
 * Transactional entity region access for Infinispan.
 * 
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
class TransactionalAccess implements EntityRegionAccessStrategy {
 
   private final EntityRegionImpl region;
   
   private final TransactionalAccessDelegate delegate;

   TransactionalAccess(EntityRegionImpl region) {
      this.region = region;
      this.delegate = new TransactionalAccessDelegate(region, region.getPutFromLoadValidator());
   }

   public void evict(Object key) throws CacheException {
      delegate.evict(key);
   }

   public void evictAll() throws CacheException {
      delegate.evictAll();
   }

   public Object get(Object key, long txTimestamp) throws CacheException {
      return delegate.get(key, txTimestamp);
   }

   public EntityRegion getRegion() {
      return this.region;
   }

   public boolean insert(Object key, Object value, Object version) throws CacheException {
      return delegate.insert(key, value, version);
   }

   public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
      return delegate.putFromLoad(key, value, txTimestamp, version);
   }

   public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride) throws CacheException {
      return delegate.putFromLoad(key, value, txTimestamp, version, minimalPutOverride);
   }

   public void remove(Object key) throws CacheException {
      delegate.remove(key);
   }

   public void removeAll() throws CacheException {
      delegate.removeAll();
   }

   public boolean update(Object key, Object value, Object currentVersion, Object previousVersion) throws CacheException {
      return delegate.update(key, value, currentVersion, previousVersion);
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

   public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
      return false;
   }

   public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock) throws CacheException {
      return false;
   }
}