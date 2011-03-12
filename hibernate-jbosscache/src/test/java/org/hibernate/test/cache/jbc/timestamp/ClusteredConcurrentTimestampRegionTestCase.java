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

package org.hibernate.test.cache.jbc.timestamp;

import java.util.Properties;
import java.util.Random;

import junit.framework.AssertionFailedError;

import org.hibernate.cache.UpdateTimestampsCache;
import org.hibernate.cache.jbc.CacheInstanceManager;
import org.hibernate.cache.jbc.JBossCacheRegionFactory;
import org.hibernate.cache.jbc.MultiplexedJBossCacheRegionFactory;
import org.hibernate.cache.jbc.timestamp.ClusteredConcurrentTimestampsRegionImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.jbc.AbstractJBossCacheTestCase;
import org.hibernate.test.util.CacheTestUtil;
import org.jboss.cache.Cache;

/**
 * A ClusteredConcurrentTimestampCacheTestCase.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class ClusteredConcurrentTimestampRegionTestCase extends AbstractJBossCacheTestCase {

    private static final String KEY1 = "com.foo.test.Entity1";
    private static final String KEY2 = "com.foo.test.Entity2";
    
    private static final Long ONE = new Long(1);
    private static final Long TWO = new Long(2);
    private static final Long THREE = new Long(3);
    private static final Long TEN = new Long(10);
    private static final Long ELEVEN = new Long(11);
    
    private static Cache cache;
    private static Properties properties;
    private ClusteredConcurrentTimestampsRegionImpl region;
    
    /**
     * Create a new ClusteredConcurrentTimestampCacheTestCase.
     * 
     * @param name
     */
    public ClusteredConcurrentTimestampRegionTestCase(String name) {
        super(name);
    }
    
    

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        if (cache == null) {
            Configuration cfg = CacheTestUtil.buildConfiguration("test", MultiplexedJBossCacheRegionFactory.class, false, true);
            properties = cfg.getProperties();
            cache = createCache();
            
            // Sleep a bit to avoid concurrent FLUSH problem
            avoidConcurrentFlush();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        
        if (region != null) {
            region.destroy();
        }
    }
    
    private Cache createCache() throws Exception {
        Configuration cfg = CacheTestUtil.buildConfiguration("test", MultiplexedJBossCacheRegionFactory.class, false, true);
        JBossCacheRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg);
        CacheInstanceManager mgr = regionFactory.getCacheInstanceManager();
        return mgr.getTimestampsCacheInstance();
    }

    protected ClusteredConcurrentTimestampsRegionImpl getTimestampRegion(Cache cache)  throws Exception {        
        
        return new ClusteredConcurrentTimestampsRegionImpl(cache, "test/" + UpdateTimestampsCache.class.getName(), "test", properties);
    }
    
    public void testSimplePreinvalidate() throws Exception {
        
        region = getTimestampRegion(cache);
        
        assertEquals(null, region.get(KEY1));
        region.preInvalidate(KEY1, TWO);
        assertEquals(TWO, region.get(KEY1));
        region.preInvalidate(KEY1, ONE);
        assertEquals(TWO, region.get(KEY1));
        region.preInvalidate(KEY1, TWO);
        assertEquals(TWO, region.get(KEY1));
        region.preInvalidate(KEY1, THREE);
        assertEquals(THREE, region.get(KEY1));
    }
    
    public void testInitialState() throws Exception {
        
        region = getTimestampRegion(cache);
        region.preInvalidate(KEY1, TEN);
        region.preInvalidate(KEY2, ELEVEN);
        region.invalidate(KEY1, ONE, TEN);
        
        Cache cache2 = createCache();
        registerCache(cache2);
        
        // Sleep a bit to avoid concurrent FLUSH problem
        avoidConcurrentFlush();
        
        ClusteredConcurrentTimestampsRegionImpl region2 = getTimestampRegion(cache2);
        assertEquals(ONE, region2.get(KEY1));
        assertEquals(ELEVEN, region2.get(KEY2));
    }
    
    public void testSimpleInvalidate() throws Exception {
        
        region = getTimestampRegion(cache);
        
        assertEquals(null, region.get(KEY1));
        region.preInvalidate(KEY1, TWO);
        assertEquals(TWO, region.get(KEY1));
        region.invalidate(KEY1, ONE, TWO);
        assertEquals(ONE, region.get(KEY1));
        region.preInvalidate(KEY1, TEN);
        region.preInvalidate(KEY1, ELEVEN);
        assertEquals(ELEVEN, region.get(KEY1));
        region.invalidate(KEY1, TWO, TEN);
        assertEquals(ELEVEN, region.get(KEY1));
        region.invalidate(KEY1, TWO, ELEVEN);
        assertEquals(TWO, region.get(KEY1));
        region.preInvalidate(KEY1, TEN);
        assertEquals(TEN, region.get(KEY1));
        region.invalidate(KEY1, THREE, TEN);
        assertEquals(THREE, region.get(KEY1));        
    }
    
    public void testConcurrentActivityClustered() throws Exception {
        concurrentActivityTest(true);
    }
    
    public void testConcurrentActivityNonClustered() throws Exception {
        concurrentActivityTest(false);
    }
    
    private void concurrentActivityTest(boolean clustered) throws Exception {
        
        region = getTimestampRegion(cache);
        ClusteredConcurrentTimestampsRegionImpl region2 = region;
        
        if (clustered) {
            Cache cache2 = createCache();
            registerCache(cache2);
            
            // Sleep a bit to avoid concurrent FLUSH problem
            avoidConcurrentFlush();
            
            region2 = getTimestampRegion(cache2);
        }
        
        Tester[] testers = new Tester[20];
        for (int i = 0; i < testers.length; i++) {
            testers[i] = new Tester((i % 2 == 0) ? region : region2);
            testers[i].start();
        }
        
        for (int j = 0; j < 10; j++) {
            sleep(2000);
            
            log.info("Running for " + ((j + 1) * 2) + " seconds");
            
            for (int i = 0; i < testers.length; i++) {
                if (testers[i].assertionFailure != null)
                    throw testers[i].assertionFailure;
            }
            
            for (int i = 0; i < testers.length; i++) {
                if (testers[i].exception != null)
                    throw testers[i].exception;
            }
        }
        
        for (int i = 0; i < testers.length; i++) {
            testers[i].stop();
        }
        
        for (int i = 0; i < testers.length; i++) {
            if (testers[i].assertionFailure != null)
                throw testers[i].assertionFailure;
        }
        
        for (int i = 0; i < testers.length; i++) {
            if (testers[i].exception != null)
                throw testers[i].exception;
        }
    }
    
    
    
    private class Tester implements Runnable {
        
        ClusteredConcurrentTimestampsRegionImpl region;
        Exception exception;
        AssertionFailedError assertionFailure;
        boolean stopped = true;
        Thread thread;
        Random random = new Random();

        Tester(ClusteredConcurrentTimestampsRegionImpl region) {
            this.region = region;
        }
        
        public void run() {
            stopped = false;
        
            while (!stopped) {
                try {
                    Long pre = new Long(region.nextTimestamp() + region.getTimeout());
                    region.preInvalidate(KEY1, pre);
                    sleep(random.nextInt(1));
                    Long post = new Long(region.nextTimestamp());
                    region.invalidate(KEY1, post, pre);
                    Long ts = (Long) region.get(KEY1);
                    assertTrue(ts + " >= " + post, ts.longValue() >= post.longValue());
                    sleep(random.nextInt(1));
                }
                catch (AssertionFailedError e) {
                    assertionFailure = e;
                }
                catch (Exception e) {
                    if (!stopped)
                        exception = e;
                }
                finally {
                    stopped = true;
                }
            }
        }
        
        void start() {
            if (stopped) {
                if (thread == null) {
                    thread = new Thread(this);
                    thread.setDaemon(true);
                }
                thread.start();
            }            
        }
        
        void stop() {
            if (!stopped) {
                stopped = true;
                try {
                    thread.join(100);
                }
                catch (InterruptedException ignored) {}
                
                if (thread.isAlive())
                    thread.interrupt();
            }
        }
    }
}
