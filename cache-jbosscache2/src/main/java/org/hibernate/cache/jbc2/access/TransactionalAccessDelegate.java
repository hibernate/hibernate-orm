/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.cache.jbc2.access;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.Transaction;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.jbc2.BasicRegionAdapter;
import org.hibernate.cache.jbc2.util.CacheHelper;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;

/**
 * Defines the strategy for transactional access to entity or collection data in
 * a pessimistic-locking JBoss Cache using its 2.x APIs.
 * <p>
 * The intent of this class is to encapsulate common code and serve as a
 * delegate for {@link EntityRegionAccessStrategy} and
 * {@link CollectionRegionAccessStrategy} implementations.
 * </p>
 * 
 * @author Brian Stansberry
 */
public class TransactionalAccessDelegate {
        
    protected final Cache cache;
    protected final Fqn regionFqn;
    protected final BasicRegionAdapter region;
    protected final ConcurrentMap<Object, Set<Object>> pendingPuts = 
       new ConcurrentHashMap<Object, Set<Object>>();

    public TransactionalAccessDelegate(BasicRegionAdapter adapter) {
        this.region = adapter;
        this.cache = adapter.getCacheInstance();
        this.regionFqn = adapter.getRegionFqn();
    }

    public Object get(Object key, long txTimestamp) throws CacheException {
        
        if (!region.checkValid())
           return null;
        
        region.ensureRegionRootExists();
        
        Object val = CacheHelper.get(cache, regionFqn, key);
        
        if (val == null) {
           registerPendingPut(key);
        }
        
        return val;
    }

   public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
       
        if (!region.checkValid())
            return false;
        
        if (!isPutValid(key))
            return false;
       
        region.ensureRegionRootExists();

        return CacheHelper.putForExternalRead(cache, regionFqn, key, value);
    }

   public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
            throws CacheException {
       
        if (!region.checkValid())
            return false;
        
        if (!isPutValid(key))
            return false;
       
        region.ensureRegionRootExists();

        // We ignore minimalPutOverride. JBossCache putForExternalRead is
        // already about as minimal as we can get; it will promptly return
        // if it discovers that the node we want to write to already exists
        return CacheHelper.putForExternalRead(cache, regionFqn, key, value);
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
       
        pendingPuts.remove(key);
        
        if (!region.checkValid())
            return false;
       
        region.ensureRegionRootExists();

        CacheHelper.put(cache, regionFqn, key, value);
        return true;
    }

    public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
        return false;
    }

    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
            throws CacheException {
       
        pendingPuts.remove(key);
       
        // We update whether or not the region is valid. Other nodes
        // may have already restored the region so they need to
        // be informed of the change.
       
        region.ensureRegionRootExists();

        CacheHelper.put(cache, regionFqn, key, value);
        return true;
    }

    public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
            throws CacheException {
        return false;
    }

    public void remove(Object key) throws CacheException {
       
        pendingPuts.remove(key);
       
        // We remove whether or not the region is valid. Other nodes
        // may have already restored the region so they need to
        // be informed of the change.
       
        region.ensureRegionRootExists();

        CacheHelper.remove(cache, regionFqn, key);
    }

    public void removeAll() throws CacheException {
       pendingPuts.clear();
       CacheHelper.removeAll(cache, regionFqn); 
    }

    public void evict(Object key) throws CacheException {
       
        pendingPuts.remove(key);
       
        region.ensureRegionRootExists();
        
        CacheHelper.remove(cache, regionFqn, key);
    }

    public void evictAll() throws CacheException {
       pendingPuts.clear();
       Transaction tx = region.suspend();
       try {        
          region.ensureRegionRootExists();
          
          CacheHelper.sendEvictAllNotification(cache, regionFqn, region.getMemberId(), null);
       }
       finally {
          region.resume(tx);
       }        
    }

    protected void registerPendingPut(Object key)
    {
      Set<Object> pending = pendingPuts.get(key);
      if (pending == null) {
         pending = new HashSet<Object>();
      }
      
      synchronized (pending) {
         Object owner = region.getOwnerForPut();
         pending.add(owner);
         Set<Object> existing = pendingPuts.putIfAbsent(key, pending);
         if (existing != pending) {
            // try again
            registerPendingPut(key);
         }
      }
    }

    protected boolean isPutValid(Object key)
    {
       boolean valid = false;
       Set<Object> pending = pendingPuts.get(key);
       if (pending != null) {
          synchronized (pending) {
             valid = pending.remove(region.getOwnerForPut());
             if (valid && pending.size() == 0) {
                pendingPuts.remove(key);
             }
          }
       }
      return valid;
    }
}
