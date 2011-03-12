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
package org.hibernate.test.cache.jbc;

import java.util.Properties;

import org.hibernate.cache.jbc.CacheInstanceManager;
import org.hibernate.cache.jbc.JBossCacheRegionFactory;
import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;
import org.hibernate.cache.jbc.builder.SharedCacheInstanceManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Settings;
import org.hibernate.test.util.CacheTestUtil;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheStatus;

/**
 * A JBossCacheRegionFactoryTestCase.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class JBossCacheRegionFactoryTestCase extends AbstractJBossCacheTestCase {

    /**
     * Create a new JBossCacheRegionFactoryTestCase.
     * 
     * @param name
     */
    public JBossCacheRegionFactoryTestCase(String name) {
        super(name);
    }

    public void testDefaultConfig() throws Exception {

        Configuration cfg = CacheTestUtil.buildConfiguration("", JBossCacheRegionFactory.class, true, true);
        
        JBossCacheRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        
        CacheInstanceManager mgr = regionFactory.getCacheInstanceManager();
        assertTrue("Correct default CacheInstanceManager type", mgr instanceof SharedCacheInstanceManager);
        
        Cache cache = mgr.getEntityCacheInstance();
        assertTrue("entity cache exists", cache != null);
        assertEquals("Used correct config", "TestSharedCache", cache.getConfiguration().getClusterName());
        assertEquals("Cache started", CacheStatus.STARTED, cache.getCacheStatus());        
        
        CacheTestUtil.stopRegionFactory(regionFactory, getCacheTestSupport());
        
        assertEquals("Cache destroyed", CacheStatus.DESTROYED, cache.getCacheStatus());
    }
    
    public void testInjectedCacheInstanceManager() {

        Configuration cfg = CacheTestUtil.buildConfiguration("", JBossCacheRegionFactory.class, true, true);
        
        CacheInstanceManager cim = new MultiplexingCacheInstanceManager();
        JBossCacheRegionFactory regionFactory = new JBossCacheRegionFactory(cim);
        
        Settings settings = cfg.buildSettings();
        Properties properties = cfg.getProperties();
        
        regionFactory.start(settings, properties);        
        // Ensure we clean up
        registerFactory(regionFactory);
        
        assertEquals("Used injected CacheInstanceManager", cim, regionFactory.getCacheInstanceManager());
        
        CacheTestUtil.stopRegionFactory(regionFactory, getCacheTestSupport());
    }

}
