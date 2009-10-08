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
package org.hibernate.cache.jbc.query;

import java.util.Properties;

import javax.transaction.Transaction;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.jbc.TransactionalDataRegionAdapter;
import org.hibernate.cache.jbc.util.CacheHelper;
import org.hibernate.util.PropertiesHelper;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.config.Option;
import org.jboss.cache.notifications.annotation.CacheListener;

/**
 * Defines the behavior of the query cache regions for JBossCache 2.x.
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
@CacheListener
public class QueryResultsRegionImpl extends TransactionalDataRegionAdapter implements QueryResultsRegion {

    public static final String QUERY_CACHE_LOCAL_ONLY_PROP = "hibernate.cache.jbc.query.localonly";
    public static final String LEGACY_QUERY_CACHE_LOCAL_ONLY_PROP = "hibernate.cache.region.jbc2.query.localonly";
    public static final String TYPE = "QUERY";
    
    /**
     * Whether we should set an option to disable propagation of changes around
     * cluster.
     */
    private boolean localOnly;

    /**
     * Create a new QueryResultsRegionImpl.
     * 
     * @param jbcCache The JBC cache instance to use to store the query results
     * @param regionName The name of the region (within the JBC cache)
     * @param regionPrefix Any region prefix to apply
	 * @param properties The configuration properties.
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
        	if (CacheHelper.isClusteredReplication(jbcCache)) {
        		if (properties.containsKey(QUERY_CACHE_LOCAL_ONLY_PROP)) {
        			localOnly = PropertiesHelper.getBoolean(QUERY_CACHE_LOCAL_ONLY_PROP, properties, false);
        		}
        		else {
        			localOnly = PropertiesHelper.getBoolean(LEGACY_QUERY_CACHE_LOCAL_ONLY_PROP, properties, false);
        		}
        	}
        }
    }

    public void evict(Object key) throws CacheException {
       
        ensureRegionRootExists();
        
        Option opt = getNonLockingDataVersionOption(false);
        if (localOnly)
            opt.setCacheModeLocal(true);
        CacheHelper.removeNode(getCacheInstance(), getRegionFqn(), key, opt);
    }

    public void evictAll() throws CacheException {
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
       
        if (!checkValid())
           return null;
       
        ensureRegionRootExists();

        // Don't hold the JBC node lock throughout the tx, as that
        // prevents updates
        // Add a zero (or low) timeout option so we don't block
        // waiting for tx's that did a put to commit
        Option opt = new Option();
        opt.setLockAcquisitionTimeout(0);
        return suspendAndGet(key, opt, true);
    }

    public void put(Object key, Object value) throws CacheException {
       
        if (checkValid()) {
           ensureRegionRootExists();
   
           // Here we don't want to suspend the tx. If we do:
           // 1) We might be caching query results that reflect uncommitted
           // changes. No tx == no WL on cache node, so other threads
           // can prematurely see those query results
           // 2) No tx == immediate replication. More overhead, plus we
           // spread issue #1 above around the cluster
   
           // Add a zero (or quite low) timeout option so we don't block.
           // Ignore any TimeoutException. Basically we forego caching the
           // query result in order to avoid blocking.
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
    }

    @Override
    protected Fqn<String> createRegionFqn(String regionName, String regionPrefix) {
        return getTypeLastRegionFqn(regionName, regionPrefix, TYPE);
    }

}
