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

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.TransactionalDataRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.jbc.JBossCacheRegionFactory;
import org.hibernate.cache.jbc.MultiplexedJBossCacheRegionFactory;
import org.hibernate.cache.jbc.SharedJBossCacheRegionFactory;
import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;
import org.hibernate.cache.jbc.builder.SharedCacheInstanceManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.util.CacheTestUtil;

/**
 * Base class for tests of EntityRegion and CollectionRegion implementations.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractEntityCollectionRegionTestCase extends AbstractRegionImplTestCase {

    /**
     * Create a new EntityCollectionRegionTestCaseBase.
     * 
     * @param name
     */
    public AbstractEntityCollectionRegionTestCase(String name) {
        super(name);
    }
   
    /** 
     * Create a Region backed by an OPTIMISTIC locking JBoss Cache, and then 
     * ensure that it handles calls to buildAccessStrategy as expected when 
     * all the various {@link AccessType}s are passed as arguments.
     */
    public void testSupportedAccessTypesOptimistic() throws Exception {
        
        supportedAccessTypeTest(true);
    }

    /** 
     * Creates a Region backed by an PESSIMISTIC locking JBoss Cache, and then 
     * ensures that it handles calls to buildAccessStrategy as expected when 
     * all the various {@link AccessType}s are passed as arguments.
     */
    public void testSupportedAccessTypesPessimistic() throws Exception {
        
        supportedAccessTypeTest(false);
    }
    
    private void supportedAccessTypeTest(boolean optimistic) throws Exception {
        
        Configuration cfg = CacheTestUtil.buildConfiguration("test", MultiplexedJBossCacheRegionFactory.class, true, false);
        String entityCfg = optimistic ? "optimistic-entity" : "pessimistic-entity";
        cfg.setProperty(MultiplexingCacheInstanceManager.ENTITY_CACHE_RESOURCE_PROP, entityCfg);
        
        JBossCacheRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        
        supportedAccessTypeTest(regionFactory, cfg.getProperties());
    }
    
    /** 
     * Creates a Region using the given factory, and then ensure that it
     * handles calls to buildAccessStrategy as expected when all the
     * various {@link AccessType}s are passed as arguments.
     */
    protected abstract void supportedAccessTypeTest(RegionFactory regionFactory, Properties properties);
    
    /**
     * Test that the Region properly implements 
     * {@link TransactionalDataRegion#isTransactionAware()}.
     * 
     * @throws Exception
     */
    public void testIsTransactionAware() throws Exception {
        
        Configuration cfg = CacheTestUtil.buildConfiguration("test", SharedJBossCacheRegionFactory.class, true, false);
        cfg.setProperty(SharedCacheInstanceManager.CACHE_RESOURCE_PROP, CacheTestUtil.LOCAL_PESSIMISTIC_CACHE);
        
        JBossCacheRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        
        TransactionalDataRegion region = (TransactionalDataRegion) createRegion(regionFactory, "test/test", cfg.getProperties(), getCacheDataDescription());
        
        assertTrue("Region is transaction-aware", region.isTransactionAware());
        
        CacheTestUtil.stopRegionFactory(regionFactory, getCacheTestSupport());
        
        cfg = CacheTestUtil.buildConfiguration("test", SharedJBossCacheRegionFactory.class, true, false);
        cfg.setProperty(SharedCacheInstanceManager.CACHE_RESOURCE_PROP, CacheTestUtil.LOCAL_PESSIMISTIC_CACHE);
        // Make it non-transactional
        cfg.getProperties().remove(Environment.TRANSACTION_MANAGER_STRATEGY);
        
        regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        
        region = (TransactionalDataRegion) createRegion(regionFactory, "test/test", cfg.getProperties(), getCacheDataDescription());
        
        assertFalse("Region is not transaction-aware", region.isTransactionAware());
        
        CacheTestUtil.stopRegionFactory(regionFactory, getCacheTestSupport());
    }
    
    public void testGetCacheDataDescription() throws Exception {
        Configuration cfg = CacheTestUtil.buildConfiguration("test", SharedJBossCacheRegionFactory.class, true, false);
        cfg.setProperty(SharedCacheInstanceManager.CACHE_RESOURCE_PROP, CacheTestUtil.LOCAL_PESSIMISTIC_CACHE);
        
        JBossCacheRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        
        TransactionalDataRegion region = (TransactionalDataRegion) createRegion(regionFactory, "test/test", cfg.getProperties(), getCacheDataDescription());
        
        CacheDataDescription cdd = region.getCacheDataDescription();
        
        assertNotNull(cdd);
        
        CacheDataDescription expected = getCacheDataDescription();
        assertEquals(expected.isMutable(), cdd.isMutable());
        assertEquals(expected.isVersioned(), cdd.isVersioned());
        assertEquals(expected.getVersionComparator(), cdd.getVersionComparator());
        
    }
}
