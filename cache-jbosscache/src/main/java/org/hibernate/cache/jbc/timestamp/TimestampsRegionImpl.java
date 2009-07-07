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

package org.hibernate.cache.jbc.timestamp;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Transaction;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.jbc.TransactionalDataRegionAdapter;
import org.hibernate.cache.jbc.util.CacheHelper;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.config.Option;
import org.jboss.cache.notifications.annotation.CacheListener;
import org.jboss.cache.notifications.annotation.NodeModified;
import org.jboss.cache.notifications.annotation.NodeRemoved;
import org.jboss.cache.notifications.event.NodeInvalidatedEvent;
import org.jboss.cache.notifications.event.NodeModifiedEvent;
import org.jboss.cache.notifications.event.NodeRemovedEvent;

/**
 * Defines the behavior of the timestamps cache region for JBossCache 2.x.
 * 
 * TODO Need to define a way to ensure asynchronous replication events
 * do not result in timestamps moving backward, while dealing with the fact
 * that the normal sequence of UpdateTimestampsCache.preinvalidate() then
 * UpdateTimestampsCache.invalidate() will result in 2 calls to put() with
 * the latter call having an earlier timestamp.
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
@CacheListener
public class TimestampsRegionImpl extends TransactionalDataRegionAdapter implements TimestampsRegion {

    public static final String TYPE = "TS";

    private Map localCache = new ConcurrentHashMap();
    
    /**
     * Create a new TimestampsRegionImpl.
	 *
     * @param jbcCache The JBC cache instance to use to store the timestamps data
     * @param regionName The name of the region (within the JBC cache)
     * @param regionPrefix Any region prefix to apply
	 * @param properties The configuration properties.
     */
    public TimestampsRegionImpl(Cache jbcCache, String regionName, String regionPrefix, Properties properties) {
        super(jbcCache, regionName, regionPrefix, null);

        jbcCache.addCacheListener(this);

        populateLocalCache();
    }

    @Override
    protected Fqn<String> createRegionFqn(String regionName, String regionPrefix) {
        return getTypeFirstRegionFqn(regionName, regionPrefix, TYPE);
    }

    public void evict(Object key) throws CacheException {
       
        ensureRegionRootExists();
        
        // TODO Is this a valid operation on a timestamps cache?
        Option opt = getNonLockingDataVersionOption(true);
        CacheHelper.removeNode(getCacheInstance(), getRegionFqn(), key, opt);
    }

    public void evictAll() throws CacheException {
        // TODO Is this a valid operation on a timestamps cache?
        Transaction tx = suspend();
        try {        
           ensureRegionRootExists();
           Option opt = getNonLockingDataVersionOption(true);
           CacheHelper.sendEvictAllNotification(jbcCache, regionFqn, getMemberId(), opt);
        }
        finally {
           resume(tx);
        }        
    }

    public Object get(Object key) throws CacheException {

        Object value = localCache.get(key);
        if (value == null && checkValid()) {
           
            ensureRegionRootExists();
            
            value = suspendAndGet(key, null, false);
            if (value != null)
                localCache.put(key, value);
        }
        return value;
    }

    public void put(Object key, Object value) throws CacheException {
       
        ensureRegionRootExists();

        // Don't hold the JBC node lock throughout the tx, as that
        // prevents reads and other updates
        Transaction tx = suspend();
        try {
            // TODO Why not use the timestamp in a DataVersion?
            Option opt = getNonLockingDataVersionOption(false);
            // We ensure ASYNC semantics (JBCACHE-1175)
            opt.setForceAsynchronous(true);
            CacheHelper.put(getCacheInstance(), getRegionFqn(), key, value, opt);
        } catch (Exception e) {
            throw new CacheException(e);
        } finally {
            resume(tx);
        }
    }

    @Override
    public void destroy() throws CacheException {
        localCache.clear();
        getCacheInstance().removeCacheListener(this);
        super.destroy();
    }

    /**
     * Monitors cache events and updates the local cache
     * 
     * @param event
     */
    @NodeModified
    public void nodeModified(NodeModifiedEvent event) {
       
        if (!handleEvictAllModification(event) && !event.isPre()) {
   
           Fqn fqn = event.getFqn();
           Fqn regFqn = getRegionFqn();
           if (fqn.size() == regFqn.size() + 1 && fqn.isChildOf(regFqn)) {
               Object key = fqn.get(regFqn.size());
               localCache.put(key, event.getData().get(ITEM));
           }
        }
    }

    /**
     * Monitors cache events and updates the local cache
     * 
     * @param event
     */
    @NodeRemoved
    public void nodeRemoved(NodeRemovedEvent event) {
        if (event.isPre())
            return;

        Fqn fqn = event.getFqn();
        Fqn regFqn = getRegionFqn();
        if (fqn.size() == regFqn.size() + 1 && fqn.isChildOf(regFqn)) {
            Object key = fqn.get(regFqn.size());
            localCache.remove(key);
        }
        else if (fqn.equals(regFqn)) {
            localCache.clear();
        }
    }
    
    

    @Override
   protected boolean handleEvictAllInvalidation(NodeInvalidatedEvent event)
   {
      boolean result = super.handleEvictAllInvalidation(event);
      if (result) {
         localCache.clear();
      }
      return result;
   }

   @Override
   protected boolean handleEvictAllModification(NodeModifiedEvent event)
   {
      boolean result = super.handleEvictAllModification(event);
      if (result) {
         localCache.clear();
      }
      return result;
   }

   /**
     * Brings all data from the distributed cache into our local cache.
     */
    private void populateLocalCache() {
        Set children = CacheHelper.getChildrenNames(getCacheInstance(), getRegionFqn());
        for (Object key : children) {
            get(key);
        }
    }
}
