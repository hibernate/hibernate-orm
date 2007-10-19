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

package org.hibernate.cache.jbc2.timestamp;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Transaction;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.jbc2.TransactionalDataRegionAdapter;
import org.hibernate.cache.jbc2.util.CacheHelper;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.config.Option;
import org.jboss.cache.notifications.annotation.CacheListener;
import org.jboss.cache.notifications.annotation.NodeModified;
import org.jboss.cache.notifications.annotation.NodeRemoved;
import org.jboss.cache.notifications.event.NodeModifiedEvent;
import org.jboss.cache.notifications.event.NodeRemovedEvent;

/**
 * Defines the behavior of the timestamps cache region for JBossCache 2.x.
 * <p>
 * Maintains a local (authoritative) cache of timestamps along with the
 * distributed cache held in JBoss Cache. Listens for changes in the distributed
 * cache and updates the local cache accordingly. Ensures that any changes in
 * the local cache represent an increase in the timestamp. This approach allows
 * timestamp changes to be replicated asynchronously by JBoss Cache while still
 * preventing backward changes in timestamps.
 * </p>
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
@CacheListener
public class TimestampsRegionImpl extends TransactionalDataRegionAdapter implements TimestampsRegion {

    public static final String TYPE = "TS";
    
    private final Map localCache = new ConcurrentHashMap();

    /**
     * Create a new TimestampsRegionImpl.
     * 
     * @param jbcCache
     * @param regionName
     * @param regionPrefix
     *            TODO
     * @param metadata
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
        // TODO Is this a valid operation on a timestamps cache?
        localCache.remove(key);
        Option opt = getNonLockingDataVersionOption(true);
        CacheHelper.removeNode(getCacheInstance(), getRegionFqn(), key, opt);
    }

    public void evictAll() throws CacheException {
        // TODO Is this a valid operation on a timestamps cache?
        localCache.clear();
        Option opt = getNonLockingDataVersionOption(true);
        CacheHelper.removeAll(getCacheInstance(), getRegionFqn(), opt);
        // Restore the region root node
        CacheHelper.addNode(getCacheInstance(), getRegionFqn(), false, true, null);   
    }

    public Object get(Object key) throws CacheException {

        Object timestamp = localCache.get(key);
        if (timestamp == null) {
            // Check the cluster-wide cache
            // Don't hold the cache node lock throughout the tx, as that
            // prevents updates
            timestamp = suspendAndGet(key, null, false);
            updateLocalCache(key, timestamp);
        }
        return timestamp;
    }

    public void put(Object key, Object value) throws CacheException {

        // Immediately update the local cache
        boolean incremented = updateLocalCache(key, value);

        if (incremented) {
            // Now the cluster-wide cache

            // TODO there's a race here where 2 threads can get through
            // updateLocalCache() in proper sequence but then the earlier
            // one updates JBC *later*. This should only affect newly
            // joined nodes who populate their initial localCache from JBC.

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
        if (event.isOriginLocal() || event.isPre())
            return;

        Fqn fqn = event.getFqn();
        Fqn regFqn = getRegionFqn();
        if (fqn.size() == regFqn.size() + 1 && fqn.isChildOf(regFqn)) {
            Object key = fqn.get(regFqn.size());
            updateLocalCache(key, event.getData().get(ITEM));
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
        if (fqn.size() == regFqn.size() + 1 && fqn.isChildOf(regFqn)) {
            Object key = fqn.get(regFqn.size());
            localCache.remove(key);
        }
        else if (fqn.equals(regFqn)) {
            localCache.clear();
        }
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

    /**
     * Updates the local cache, ensuring that the new value represents a higher
     * value than the old (i.e. timestamp never goes back in time).
     * 
     * @param key
     * @param value
     */
    private boolean updateLocalCache(Object key, Object value) {
        if (value == null)
            return false;

        boolean increase = true;

        long newVal = 0;
        try {
            newVal = ((Long) value).longValue();

            Long oldVal = (Long) localCache.get(key);
            increase = oldVal == null || newVal > oldVal.longValue();
            if (increase) {
                oldVal = (Long) localCache.put(key, value);
                // Double check that it was an increase
                if (oldVal != null && oldVal.longValue() > newVal) {                    
                    // Nope; Restore the old value
                    updateLocalCache(key, oldVal);
                    increase = false;
                }
            }
        } catch (ClassCastException cce) {
            // TODO -- this is stupid; look into changing TimestampsRegion API
            // not using Long; just store it
            localCache.put(key, value);
        }

        return increase;

    }

}
