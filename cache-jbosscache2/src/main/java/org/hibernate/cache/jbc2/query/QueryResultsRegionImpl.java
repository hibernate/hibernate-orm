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

package org.hibernate.cache.jbc2.query;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.jbc2.TransactionalDataRegionAdapter;
import org.hibernate.cache.jbc2.util.CacheHelper;
import org.hibernate.util.PropertiesHelper;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.config.Option;

/**
 * Defines the behavior of the query cache regions for JBossCache 2.x.
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
public class QueryResultsRegionImpl extends TransactionalDataRegionAdapter implements QueryResultsRegion {

    public static final String QUERY_CACHE_LOCAL_ONLY_PROP = "hibernate.cache.region.jbc2.query.localonly";
    
    
    /**
     * Whether we should set an option to disable propagation of changes around
     * cluster.
     */
    private boolean localOnly;

    /**
     * Create a new QueryResultsRegionImpl.
     * 
     * @param jbcCache
     * @param regionName
     * @param regionPrefix
     *            TODO
     */
    public QueryResultsRegionImpl(Cache jbcCache, String regionName, String regionPrefix, Properties properties) {
        super(jbcCache, regionName, regionPrefix, null);

        // If JBC is using INVALIDATION, we don't want to propagate changes.
        // We use the Timestamps cache to manage invalidation
        localOnly = CacheHelper.isClusteredInvalidation(jbcCache);
        if (!localOnly) {
            // We don't want to waste effort setting an option if JBC is
            // already in LOCAL mode. If JBC is REPL_(A)SYNC then check
            // if they passed an config option to disable query replication
            localOnly = CacheHelper.isClusteredReplication(jbcCache)
                    && PropertiesHelper.getBoolean(QUERY_CACHE_LOCAL_ONLY_PROP, properties, false);
        }
    }

    public void evict(Object key) throws CacheException {
        Option opt = getNonLockingDataVersionOption(false);
        if (localOnly)
            opt.setCacheModeLocal(true);
        CacheHelper.remove(getCacheInstance(), getRegionFqn(), key, opt);
    }

    public void evictAll() throws CacheException {
        Option opt = getNonLockingDataVersionOption(false);
        if (localOnly)
            opt.setCacheModeLocal(true);
        CacheHelper.removeAll(getCacheInstance(), getRegionFqn(), opt);
    }

    public Object get(Object key) throws CacheException {

        // Don't hold the JBC node lock throughout the tx, as that
        // prevents updates
        // Add a zero (or low) timeout option so we don't block
        // waiting for tx's that did a put to commit
        Option opt = new Option();
        opt.setLockAcquisitionTimeout(0);
        return suspendAndGet(key, opt, true);
    }

    public void put(Object key, Object value) throws CacheException {

        // Here we don't want to suspend the tx. If we do:
        // 1) We might be caching query results that reflect uncommitted
        // changes. No tx == no WL on cache node, so other threads
        // can prematurely see those query results
        // 2) No tx == immediate replication. More overhead, plus we
        // spread issue #1 above around the cluster

        // Add a zero (or quite low) timeout option so we don't block
        // Ignore any TimeoutException. Basically we forego caching the
        // query result in order to avoid blocking for concurrent reads.
        // Reads are done with suspended tx, so they should not hold the
        // lock for long.  Not caching the query result is OK, since
        // any subsequent read will just see the old result with its
        // out-of-date timestamp; that result will be discarded and the
        // db query performed again.
        Option opt = getNonLockingDataVersionOption(false);
        opt.setLockAcquisitionTimeout(2);
        if (localOnly)
            opt.setCacheModeLocal(true);
        CacheHelper.putAllowingTimeout(getCacheInstance(), getRegionFqn(), key, value, opt);
    }

    @Override
    protected Fqn<String> createRegionFqn(String regionName, String regionPrefix) {
        return Fqn.fromString(escapeRegionName(regionName, regionPrefix));
    }

}
