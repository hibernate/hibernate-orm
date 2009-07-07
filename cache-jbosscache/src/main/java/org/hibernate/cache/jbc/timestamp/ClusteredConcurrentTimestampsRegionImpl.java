/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Brian Stansberry
 */

package org.hibernate.cache.jbc.timestamp;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

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
import org.jboss.cache.notifications.event.NodeModifiedEvent;
import org.jboss.cache.notifications.event.NodeRemovedEvent;

/**
 * Prototype of a clustered timestamps cache region impl usable if the
 * TimestampsRegion API is changed.
 * <p>
 * Maintains a local (authoritative) cache of timestamps along with the
 * distributed cache held in JBoss Cache. Listens for changes in the distributed
 * cache and updates the local cache accordingly. Ensures that any changes in
 * the local cache represent either 1) an increase in the timestamp or 
 * 2) a stepback in the timestamp by the caller that initially increased
 * it as part of a pre-invalidate call. This approach allows
 * timestamp changes to be replicated asynchronously by JBoss Cache while still
 * preventing invalid backward changes in timestamps.
 * </p>
 * 
 * NOTE: This is just a prototype!!! Only useful if we change the 
 * TimestampsRegion API.
 * 
 * @author Brian Stansberry
 * @version $Revision: 14106 $
 */
@CacheListener
public class ClusteredConcurrentTimestampsRegionImpl extends TransactionalDataRegionAdapter implements TimestampsRegion {

    public static final String TYPE = "TS";
    
    private final ConcurrentHashMap localCache = new ConcurrentHashMap();
    
    /**
     * Create a new ClusteredConccurentTimestampsRegionImpl.
     * 
     * @param jbcCache
     * @param regionName
     * @param regionPrefix
     *            TODO
     * @param metadata
     */
    public ClusteredConcurrentTimestampsRegionImpl(Cache jbcCache, String regionName, String regionPrefix, Properties properties) {
        super(jbcCache, regionName, regionPrefix, null);

        jbcCache.addCacheListener(this);

        populateLocalCache();
    }

    @Override
    protected Fqn<String> createRegionFqn(String regionName, String regionPrefix) {
        return getTypeFirstRegionFqn(regionName, regionPrefix, TYPE);
    }

    public void evict(Object key) throws CacheException {
        Option opt = getNonLockingDataVersionOption(true);
        CacheHelper.removeNode(getCacheInstance(), getRegionFqn(), key, opt);
    }

    public void evictAll() throws CacheException {
        Option opt = getNonLockingDataVersionOption(true);
        CacheHelper.removeAll(getCacheInstance(), getRegionFqn(), opt);
    }

    public Object get(Object key) throws CacheException {
        Entry entry = getLocalEntry(key);
        Object timestamp = entry.getCurrent();
        if (timestamp == null) {
            // Double check the distributed cache
            Object[] vals = (Object[]) suspendAndGet(key, null, false);
            if (vals != null) {
                storeDataFromJBC(key, vals);
                timestamp = entry.getCurrent();
            }
        }
        return timestamp;
    }

    public void put(Object key, Object value) throws CacheException {
        
        throw new UnsupportedOperationException("Prototype only; Hibernate core must change the API before really using");
    }
    
    public void preInvalidate(Object key, Object value) throws CacheException {
        
        Entry entry = getLocalEntry(key);
        if (entry.preInvalidate(value)) {
            putInJBossCache(key, entry);
        }
    }
    
    public void invalidate(Object key, Object value, Object preInvalidateValue) throws CacheException {
        
        Entry entry = getLocalEntry(key);
        if (entry.invalidate(value, preInvalidateValue)) {
            putInJBossCache(key, entry);
        }
    }
    
    private void putInJBossCache(Object key, Entry entry) {        
    
        // Get an exclusive right to update JBC for this key from this node.
        boolean locked = false;
        try {
            entry.acquireJBCWriteMutex();
            locked = true;
            // We have the JBCWriteMutex, so no other *local* thread will 
            // be trying to write this key. 
            // It's possible here some remote thread has come in and
            // changed the values again, but since we are reading the
            // values to write to JBC right now, we know we are writing
            // the latest values; i.e. we don't assume that what we cached
            // in entry.update() above is what we should write to JBC *now*.
            // Our write could be redundant, i.e. we are writing what
            // some remote thread just came in an wrote.  There is a chance 
            // that yet another remote thread will update us, and we'll then
            // overwrite that later data in JBC.  But, all remote nodes will
            // ignore that change in their localCache; the only place it 
            // will live will be in JBC, where it can only effect the 
            // initial state transfer values on newly joined nodes 
            // (i.e. populateLocalCache()).
            
            // Don't hold the JBC node lock throughout the tx, as that
            // prevents reads and other updates
            Transaction tx = suspend();
            try {
                Option opt = getNonLockingDataVersionOption(false);
                // We ensure ASYNC semantics (JBCACHE-1175)
                opt.setForceAsynchronous(true);
                CacheHelper.put(getCacheInstance(), getRegionFqn(), key, entry.getJBCUpdateValues(), opt);
            } 
            finally {
                resume(tx);
            }  
        } 
        catch (InterruptedException e) {
            throw new CacheException("Interrupted while acquiring right to update " + key, e);
        } 
        finally {
            if (locked) {
                entry.releaseJBCWriteMutex();
            }
        }
    }

    @Override
    public void destroy() throws CacheException {

        getCacheInstance().removeCacheListener(this);
        super.destroy();
        localCache.clear();
    }

    /**
     * Monitors cache events and updates the local cache
     * 
     * @param event
     */
    @NodeModified
    public void nodeModified(NodeModifiedEvent event) {
        if (event.isOriginLocal() || event.isPre())
            return;

        Fqn fqn = event.getFqn();
        Fqn regFqn = getRegionFqn();
        if (fqn.size() == regFqn.size() + 1 && fqn.isChildOf(regFqn)) {
            Object key = fqn.get(regFqn.size());
            Object[] vals = (Object[]) event.getData().get(ITEM);
            storeDataFromJBC(key, vals);
            // TODO consider this hack instead of the simple entry.update above:
//            if (!entry.update(vals[0], vals[1])) {
//                // Hack! Use the fact that the Object[] stored in JBC is
//                // mutable to correct our local JBC state in this callback
//                Object[] correct = entry.getJBCUpdateValues();
//                vals[0] = correct[0];
//                vals[1] = correct[1];
//            }
        }
    }
    
    private void storeDataFromJBC(Object key, Object[] vals) {
        Entry entry = getLocalEntry(key);
        if (vals[0].equals(vals[1])) {
            entry.preInvalidate(vals[0]);
        }
        else {
            entry.invalidate(vals[0], vals[1]);
        }
    }

    /**
     * Monitors cache events and updates the local cache
     * 
     * @param event
     */
    @NodeRemoved
    public void nodeRemoved(NodeRemovedEvent event) {
        if (event.isOriginLocal() || event.isPre())
            return;

        Fqn fqn = event.getFqn();
        Fqn regFqn = getRegionFqn();
        if (fqn.isChildOrEquals(regFqn)) {
            if (fqn.size() == regFqn.size()) {
                localCache.clear();
            }
            else {
                Object key = fqn.get(regFqn.size());
                localCache.remove(key);
            }
        }
    }

    /**
     * Brings all data from the distributed cache into our local cache.
     */
    private void populateLocalCache() {
        Set children = CacheHelper.getChildrenNames(getCacheInstance(), getRegionFqn());
        for (Object key : children) {
            Object[] vals = (Object[]) suspendAndGet(key, null, false);
            if (vals != null) {
                storeDataFromJBC(key, vals);
            }
        }
    }
    
    private Entry getLocalEntry(Object key) {
        
        Entry entry = new Entry();
        Entry oldEntry = (Entry) localCache.putIfAbsent(key, entry);
        return (oldEntry == null ? entry : oldEntry);
    }
    
    private class Entry {
        
        private Semaphore writeMutex = new Semaphore(1);
        private boolean preInvalidated = false;
        private Object preInval  = null;
        private Object current = null;
        
        void acquireJBCWriteMutex() throws InterruptedException {
            writeMutex.acquire();
        }
        
        void releaseJBCWriteMutex() {
            writeMutex.release();
        }
        
        synchronized boolean preInvalidate(Object newVal) {
            
            boolean result = false;
            if (newVal instanceof Comparable) {
                if (current == null || ((Comparable) newVal).compareTo(current) > 0) {
                    preInval = current = newVal;
                    preInvalidated = true;
                    result = true;
                }
            }
            else {
                preInval = current = newVal;
                result = true;
            }
            
            return result;
        }
        
        synchronized boolean invalidate(Object newVal, Object preInvalidateValue) {
            
            boolean result = false;
            
            if (current == null) {
                // Initial load from JBC
                current = newVal;
                preInval = preInvalidateValue;
                preInvalidated = false;
                result = true;     
            }
            else if (preInvalidated) {
                if (newVal instanceof Comparable) {
                    if (safeEquals(preInvalidateValue, this.preInval)
                            || ((Comparable) newVal).compareTo(preInval) > 0) {
                        current = newVal;
                        preInval = preInvalidateValue;
                        preInvalidated = false;
                        result =  true;                    
                    }
                }
                else {
                    current = newVal;
                    preInval = preInvalidateValue;
                    result =  true;
                }
            }
            else if (newVal instanceof Comparable) {
                // See if we had a 2nd invalidation from the same initial
                // preinvalidation timestamp. If so, only increment
                // if the new current value is an increase
                if (safeEquals(preInvalidateValue, this.preInval)
                        && ((Comparable) newVal).compareTo(current) > 0) {
                    current = newVal;
                    preInval = preInvalidateValue;
                    result =  true;                    
                }
            }  
            
            return result;
        }
        
        synchronized Object getCurrent() {
            return current;
        }
        
        synchronized Object getPreInval() {
            return preInval;
        }
        
        synchronized Object[] getJBCUpdateValues() {
            return new Object[] {current, preInval};
        }
        
        private boolean safeEquals(Object a, Object b) {
            return (a == b || (a != null && a.equals(b)));
        }
    }
    
    

}
