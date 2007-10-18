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

package org.hibernate.test.cache.jbc2;

import java.util.Properties;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.Region;
import org.hibernate.cache.impl.CacheDataDescriptionImpl;
import org.hibernate.cache.jbc2.JBossCacheRegionFactory;
import org.hibernate.cache.jbc2.SharedJBossCacheRegionFactory;
import org.hibernate.cache.jbc2.builder.SharedCacheInstanceManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.util.CacheTestUtil;
import org.hibernate.util.ComparableComparator;
import org.jboss.cache.Cache;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.config.Option;
import org.jgroups.JChannelFactory;

/**
 * Base class for tests of Region implementations.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractRegionImplTestCase extends AbstractJBossCacheTestCase {

    /**
     * Create a new RegionImplTestCaseBase.
     * 
     * @param name
     */
    public AbstractRegionImplTestCase(String name) {
        super(name);
    }  

    /**
     * Tests proper handling of region initialization and destruction.
     * 
     * @throws Exception
     */
    public void testActivationDeactivation() throws Exception {
        
        // Set up a cache to monitor affects of starting the region
        Cache remoteCache = DefaultCacheFactory.getInstance().createCache(SharedCacheInstanceManager.DEFAULT_CACHE_RESOURCE, false);
        
        // This test assumes replication; verify that's correct
        assertEquals("Cache is REPL_SYNC", "REPL_SYNC", remoteCache.getConfiguration().getCacheModeString());
        
        JChannelFactory channelFactory = new JChannelFactory();
        channelFactory.setMultiplexerConfig(SharedCacheInstanceManager.DEF_MULTIPLEXER_RESOURCE);
        remoteCache.getConfiguration().getRuntimeConfig().setMuxChannelFactory(channelFactory);
        remoteCache.start();
        
        // Make sure we stop the remoteCache
        registerCache(remoteCache);
        
        Fqn regionFqn = getRegionFqn("test/test", "test");
        
        assertNull("No region node", remoteCache.getRoot().getChild( regionFqn ));
        
        Configuration cfg = CacheTestUtil.buildConfiguration("test", SharedJBossCacheRegionFactory.class, true, true);
        JBossCacheRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        
        Region region = createRegion(regionFactory, "test/test", cfg.getProperties(), getCacheDataDescription());
        
        Cache localCache = getJBossCache( regionFactory );
        
        // This test assumes replication; verify that's correct
        assertEquals("Cache is REPL_SYNC", "REPL_SYNC", localCache.getConfiguration().getCacheModeString());
        
        // Region creation should not have affected remoteCache

        assertNull("No region node", remoteCache.getRoot().getChild( regionFqn ));
        Node regionRoot = localCache.getRoot().getChild( regionFqn );
        assertTrue("Has a node at " + regionFqn, regionRoot != null );
        assertTrue(regionFqn + " is resident", regionRoot.isResident() );
        
        // Confirm region destroy does not affect remote cache
        
        Option option = new Option();
        option.setCacheModeLocal(true);
        remoteCache.getInvocationContext().setOptionOverrides(option);
        remoteCache.put(regionFqn, "test", "test");
        
        assertEquals("Put succeeded", "test", remoteCache.get(regionFqn, "test"));
        assertNull("Put was local", localCache.get(regionFqn, "test"));
        
        region.destroy();
        
        assertEquals("Remote cache unchanged", "test", remoteCache.get(regionFqn, "test"));
        assertNull("No region node", localCache.getRoot().getChild( regionFqn ));
    }
    
    protected abstract Cache getJBossCache(JBossCacheRegionFactory regionFactory);
    
    protected abstract Fqn getRegionFqn(String regionName, String regionPrefix);
    
    protected abstract Region createRegion(JBossCacheRegionFactory regionFactory, String regionName, Properties properties, CacheDataDescription cdd);
    
    protected CacheDataDescription getCacheDataDescription() {
       return new CacheDataDescriptionImpl(true, true, ComparableComparator.INSTANCE);
   }   

}
