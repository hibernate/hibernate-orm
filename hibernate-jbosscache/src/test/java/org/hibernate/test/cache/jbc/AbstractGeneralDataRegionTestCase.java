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

import java.util.Iterator;

import org.hibernate.cache.GeneralDataRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.Region;
import org.hibernate.cache.jbc.JBossCacheRegionFactory;
import org.hibernate.cache.jbc.MultiplexedJBossCacheRegionFactory;
import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;
import org.hibernate.cache.jbc.util.CacheHelper;
import org.hibernate.cache.jbc.util.NonLockingDataVersion;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.util.CacheTestUtil;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.NodeSPI;
import org.jboss.cache.transaction.BatchModeTransactionManager;

/**
 * Base class for tests of QueryResultsRegion and TimestampsRegion.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractGeneralDataRegionTestCase extends AbstractRegionImplTestCase {

    protected static final String KEY = "Key";
    protected static final String VALUE1 = "value1";
    protected static final String VALUE2 = "value2";

    public AbstractGeneralDataRegionTestCase(String name) {
        super(name);
    }

    @Override
    protected void putInRegion(Region region, Object key, Object value) {
        ((GeneralDataRegion) region).put(key, value);
    }

    @Override
    protected void removeFromRegion(Region region, Object key) {
        ((GeneralDataRegion) region).evict(key);        
    }  

    /**
     * Test method for {@link QueryResultsRegion#evict(java.lang.Object)}.
     * 
     * FIXME add testing of the "immediately without regard for transaction
     * isolation" bit in the CollectionRegionAccessStrategy API.
     */
    public void testEvictOptimistic() throws Exception {
        evictOrRemoveTest("optimistic-shared");
    }

    /**
     * Test method for {@link QueryResultsRegion#evict(java.lang.Object)}.
     * 
     * FIXME add testing of the "immediately without regard for transaction
     * isolation" bit in the CollectionRegionAccessStrategy API.
     */
    public void testEvictPessimistic() throws Exception {
        evictOrRemoveTest("pessimistic-shared");
    }

    private void evictOrRemoveTest(String configName) throws Exception {
    
        Configuration cfg = createConfiguration(configName);
        JBossCacheRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        Cache localCache = getJBossCache(regionFactory);
        boolean invalidation = CacheHelper.isClusteredInvalidation(localCache);
        
        // Sleep a bit to avoid concurrent FLUSH problem
        avoidConcurrentFlush();
        
        GeneralDataRegion localRegion = (GeneralDataRegion) createRegion(regionFactory, getStandardRegionName(REGION_PREFIX), cfg.getProperties(), null);
        
        cfg = createConfiguration(configName);
        regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        
        GeneralDataRegion remoteRegion = (GeneralDataRegion) createRegion(regionFactory, getStandardRegionName(REGION_PREFIX), cfg.getProperties(), null);
        
        assertNull("local is clean", localRegion.get(KEY));
        assertNull("remote is clean", remoteRegion.get(KEY));
        
        localRegion.put(KEY, VALUE1);
        assertEquals(VALUE1, localRegion.get(KEY));
        
        // allow async propagation
        sleep(250);
        Object expected = invalidation ? null : VALUE1;
        assertEquals(expected, remoteRegion.get(KEY));
        
        localRegion.evict(KEY);
        
        assertEquals(null, localRegion.get(KEY));
        
        assertEquals(null, remoteRegion.get(KEY));
    }

    protected abstract String getStandardRegionName(String regionPrefix);
    
    /**
     * Test method for {@link QueryResultsRegion#evictAll()}.
     * 
     * FIXME add testing of the "immediately without regard for transaction
     * isolation" bit in the CollectionRegionAccessStrategy API.
     */
    public void testEvictAllOptimistic() throws Exception {
        evictOrRemoveAllTest("optimistic-shared");
    }

    /**
     * Test method for {@link QueryResultsRegion#evictAll()}.
     * 
     * FIXME add testing of the "immediately without regard for transaction
     * isolation" bit in the CollectionRegionAccessStrategy API.
     */
    public void testEvictAllPessimistic() throws Exception {
        evictOrRemoveAllTest("pessimistic-shared");
    }

    private void evictOrRemoveAllTest(String configName) throws Exception {
    
        Configuration cfg = createConfiguration(configName);
        JBossCacheRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        Cache localCache = getJBossCache(regionFactory);
        boolean optimistic = "OPTIMISTIC".equals(localCache.getConfiguration().getNodeLockingSchemeString());
        boolean invalidation = CacheHelper.isClusteredInvalidation(localCache);
        
        // Sleep a bit to avoid concurrent FLUSH problem
        avoidConcurrentFlush();
    
        GeneralDataRegion localRegion = (GeneralDataRegion) createRegion(regionFactory, getStandardRegionName(REGION_PREFIX), cfg.getProperties(), null);
        
        cfg = createConfiguration(configName);
        regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        Cache remoteCache = getJBossCache(regionFactory);
        
        // Sleep a bit to avoid concurrent FLUSH problem
        avoidConcurrentFlush();
    
        GeneralDataRegion remoteRegion = (GeneralDataRegion) createRegion(regionFactory, getStandardRegionName(REGION_PREFIX), cfg.getProperties(), null);
        Fqn regionFqn = getRegionFqn(getStandardRegionName(REGION_PREFIX), REGION_PREFIX);
        
        Node regionRoot = localCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        assertEquals("No children in " + regionRoot, 0, getValidChildrenCount(regionRoot));
        assertTrue(regionRoot.isResident());
        
        if (optimistic) {
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
        }
    
        regionRoot = remoteCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        assertEquals(0, getValidChildrenCount(regionRoot));
        assertTrue(regionRoot.isResident());
        
        if (optimistic) {
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
        }
        
        assertNull("local is clean", localRegion.get(KEY));
        assertNull("remote is clean", remoteRegion.get(KEY));
        
        localRegion.put(KEY, VALUE1);
        assertEquals(VALUE1, localRegion.get(KEY));     
        
        // Allow async propagation
        sleep(250);
        
        remoteRegion.put(KEY, VALUE1);
        assertEquals(VALUE1, remoteRegion.get(KEY));     
        
        // Allow async propagation
        sleep(250);
        
        if (optimistic) {
            regionRoot = localCache.getRoot().getChild(regionFqn);
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
            regionRoot = remoteCache.getRoot().getChild(regionFqn);
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
        }
        
        localRegion.evictAll();
        
        // This should re-establish the region root node
        assertNull(localRegion.get(KEY));
        
        regionRoot = localCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        assertEquals(0, getValidChildrenCount(regionRoot));
        assertTrue(regionRoot.isValid());
        assertTrue(regionRoot.isResident());

        // Re-establishing the region root on the local node doesn't 
        // propagate it to other nodes. Do a get on the remote node to re-establish
        assertEquals(null, remoteRegion.get(KEY));
        
        regionRoot = remoteCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        assertEquals(0, getValidChildrenCount(regionRoot));
        assertTrue(regionRoot.isValid());
        assertTrue(regionRoot.isResident());
        
        assertEquals("local is clean", null, localRegion.get(KEY));
        assertEquals("remote is clean", null, remoteRegion.get(KEY));
    }

    private void checkNodeIsEmpty(Node node) {
        assertEquals("Known issue JBCACHE-1200. node " + node.getFqn() + " should not have keys", 0, node.getKeys().size());
        for (Iterator it = node.getChildren().iterator(); it.hasNext(); ) {
            checkNodeIsEmpty((Node) it.next());
        }
    }

    protected Configuration createConfiguration(String configName) {
        Configuration cfg = CacheTestUtil.buildConfiguration("test", MultiplexedJBossCacheRegionFactory.class, false, true);
        cfg.setProperty(MultiplexingCacheInstanceManager.QUERY_CACHE_RESOURCE_PROP, configName);
        cfg.setProperty(MultiplexingCacheInstanceManager.TIMESTAMP_CACHE_RESOURCE_PROP, configName);
        return cfg;
    }

    protected void rollback() {
        try {
            BatchModeTransactionManager.getInstance().rollback();
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        
    }

}