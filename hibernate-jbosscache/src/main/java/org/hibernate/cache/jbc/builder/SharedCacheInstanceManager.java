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
package org.hibernate.cache.jbc.builder;

import java.util.Properties;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheStatus;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.config.Configuration;
import org.jgroups.ChannelFactory;
import org.jgroups.JChannelFactory;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.jbc.CacheInstanceManager;
import org.hibernate.cache.jbc.util.CacheHelper;
import org.hibernate.cfg.Settings;
import org.hibernate.util.PropertiesHelper;

/**
 * A {@link CacheInstanceManager} implementation where we use a single JBoss Cache
 * instance for each type of region. If operating on a cluster, the cache must
 * be configured for REPL_SYNC if query caching is enabled. If query caching
 * is not used, REPL_SYNC or INVALIDATION_SYNC are valid, with 
 * INVALIDATION_SYNC preferred.
 * 
 * @author Steve Ebersole
 * @author Brian Stansberry
 */
public class SharedCacheInstanceManager implements CacheInstanceManager {
    
    private static final Logger log = LoggerFactory.getLogger(SharedCacheInstanceManager.class);

    /**
     * Classpath or filesystem resource containing JBoss Cache 
     * configuration settings the {@link Cache} should use.
     * 
     * @see #DEFAULT_CACHE_RESOURCE
     */
    public static final String CACHE_RESOURCE_PROP = "hibernate.cache.jbc.cfg.shared";
    
    /**
     * Legacy name for configuration property {@link #CACHE_RESOURCE_PROP}.
     * 
     * @see #DEFAULT_CACHE_RESOURCE
     */
    public static final String LEGACY_CACHE_RESOURCE_PROP = "hibernate.cache.region.jbc2.cfg.shared";
    
    /**
     * Default name for the JBoss Cache configuration file.
     */
    public static final String DEFAULT_CACHE_RESOURCE = "treecache.xml";
    /**
     * Classpath or filesystem resource containing JGroups protocol
     * stack configurations the <code>org.jgroups.ChannelFactory</code>
     * should use.
     * 
     * @see #DEF_JGROUPS_RESOURCE
     */
    public static final String CHANNEL_FACTORY_RESOURCE_PROP = "hibernate.cache.jbc.cfg.jgroups.stacks";
    /**
     * Legacy name for configuration property {@link #CHANNEL_FACTORY_RESOURCE_PROP}.
     * 
     * @see #DEF_JGROUPS_RESOURCE
     */
    public static final String LEGACY_CHANNEL_FACTORY_RESOURCE_PROP = "hibernate.cache.region.jbc2.cfg.jgroups.stacks";
    /**
     * Default value for {@link #CHANNEL_FACTORY_RESOURCE_PROP}.  Specifies
     * the "jgroups-stacks.xml" file in this package.
     */
    public static final String DEF_JGROUPS_RESOURCE = "org/hibernate/cache/jbc/builder/jgroups-stacks.xml";

    private Cache cache;
    private ChannelFactory channelFactory;
    private boolean use2ndLevel;
    private boolean useQuery;
    
    public SharedCacheInstanceManager() {
    }

    public SharedCacheInstanceManager(ChannelFactory channelFactory) {
        this.channelFactory = channelFactory;
    }
    
    public SharedCacheInstanceManager(Cache cache) {
        this.cache = cache;
    }

    /**
     * {@inheritDoc}
     */
    public Cache getEntityCacheInstance() {
        return use2ndLevel ? cache : null;
    }

    /**
     * {@inheritDoc}
     */
    public Cache getCollectionCacheInstance() {
        return use2ndLevel ? cache : null;
    }

    /**
     * {@inheritDoc}
     */
    public Cache getQueryCacheInstance() {
        
        if (!useQuery)
            return null;
        
        if (CacheHelper.isClusteredInvalidation(cache)) {
            throw new CacheException("Query cache not supported for clustered invalidation");
        }
        return cache;
    }

    /**
     * {@inheritDoc}
     */
    public void start(Settings settings, Properties properties) throws CacheException {

        use2ndLevel = settings.isSecondLevelCacheEnabled();
        useQuery = settings.isQueryCacheEnabled();
        
        if (cache == null) {
            
            if (channelFactory == null) {
                String muxStacks = PropertiesHelper.getString(CHANNEL_FACTORY_RESOURCE_PROP, properties, null);
                if (muxStacks == null) {
                	PropertiesHelper.getString(LEGACY_CHANNEL_FACTORY_RESOURCE_PROP, properties, DEF_JGROUPS_RESOURCE);
                }
                if (muxStacks != null) {
                    channelFactory = new JChannelFactory();
                    try {
                        channelFactory.setMultiplexerConfig(muxStacks);
                    }
                    catch (Exception e) {
                        throw new CacheException("Problem setting ChannelFactory config", e);
                    }
                }
            }
            cache = createSharedCache(settings, properties);
            configureTransactionManager(cache, settings, properties);
            if (cache.getConfiguration().getMultiplexerStack() != null
                    && cache.getConfiguration().getRuntimeConfig().getMuxChannelFactory() == null) {
                cache.getConfiguration().getRuntimeConfig().setMuxChannelFactory(channelFactory);
            }
        }
        cache.start();
    }

    /**
     * {@inheritDoc}
     */
    public Cache getTimestampsCacheInstance() {
        
        if (!useQuery)
            return null;
        
        if (CacheHelper.isClusteredInvalidation(cache)) {
            throw new CacheException("Query cache not supported for clustered invalidation");
        }
        return cache;
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        if (cache != null) {
            stopSharedCache(cache);
        }
    }

    /**
     * Create a cache using the given settings and properties.
     *
     * @param settings The Hibernate settings
     * @param properties The configuration properties
     * @return The created cache
     */
    protected Cache createSharedCache(Settings settings, Properties properties)
    {
        String configResource = PropertiesHelper.getString(CACHE_RESOURCE_PROP, properties, null);
        if (configResource == null) {
        	configResource = PropertiesHelper.getString(LEGACY_CACHE_RESOURCE_PROP, properties, DEFAULT_CACHE_RESOURCE);
        }
        return new DefaultCacheFactory().createCache(configResource, false);
    }
    
    /**
     * Injects the TransactionManager found via {@link Settings#getTransactionManagerLookup()}
     * into the cache.
     * 
     * @param cache The cache instance
     * @param settings The Hibernate settings
     * @param properties The configuration properties
     * 
     * @throws CacheException if <code>cache</code> is already started and is 
     *                        configured with a different TransactionManager
     *                        than the one we would inject
     */
    protected void configureTransactionManager(Cache cache, Settings settings, Properties properties) {
        
        TransactionManager tm = null;
        if (settings.getTransactionManagerLookup() != null) {
            tm = settings.getTransactionManagerLookup().getTransactionManager(properties);
        }
        
        Configuration cacheConfig = cache.getConfiguration();
        TransactionManager cacheTm = cacheConfig.getRuntimeConfig().getTransactionManager();
        
        if (!safeEquals(tm, cacheTm)) {            
            if (cache.getCacheStatus() != CacheStatus.INSTANTIATED
                    && cache.getCacheStatus() != CacheStatus.DESTROYED) {
               log.debug("JBoss Cache is already started with a transaction manager ("
                     + cacheTm + ") that is not equal to our own (" + tm + ")");    
            } else {
                // Configure the cache to use our TM
                cacheConfig.getRuntimeConfig().setTransactionManager(tm);
                if (tm == null) {
                    // Make sure JBC doesn't look one up
                    cacheConfig.setTransactionManagerLookupClass(null);
                }
            }
        }
    }

    private boolean safeEquals(Object a, Object b) {
        return (a == b || (a != null && a.equals(b)));
    }

    /**
     * Stops the shared cache.
     * @param cache the shared cache
     */
    protected void stopSharedCache(Cache cache) {
        try {
            if (cache.getCacheStatus() == CacheStatus.STARTED) {
                cache.stop();
            }
            if (cache.getCacheStatus() != CacheStatus.DESTROYED
                    && cache.getCacheStatus() != CacheStatus.INSTANTIATED) {
                cache.destroy();
            }
        } catch (Throwable t) {
            log.warn("Unable to stop cache instance", t);
        }
    }
}
