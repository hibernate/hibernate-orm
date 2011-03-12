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
package org.hibernate.test.cache.jbc.entity;

import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.jbc.JBossCacheRegionFactory;
import org.hibernate.cache.jbc.MultiplexedJBossCacheRegionFactory;
import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;
import org.hibernate.cache.jbc.entity.TransactionalAccess;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.jbc.AbstractJBossCacheTestCase;
import org.hibernate.test.util.CacheTestUtil;
import org.jboss.cache.Cache;

/**
 * Tests for the "extra API" in EntityRegionAccessStrategy; in this base
 * version using Optimistic locking with TRANSACTIONAL access.
 * <p>
 * By "extra API" we mean those methods that are superfluous to the 
 * function of the JBC integration, where the impl is a no-op or a static
 * false return value, UnsupportedOperationException, etc.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class OptimisticTransactionalExtraAPITestCase extends AbstractJBossCacheTestCase {

    public static final String REGION_NAME = "test/com.foo.test";
    public static final String KEY = "KEY";
    public static final String VALUE1 = "VALUE1";
    public static final String VALUE2 = "VALUE2";
    
    private static EntityRegionAccessStrategy localAccessStrategy;
    
    private static boolean optimistic;
    
    /**
     * Create a new TransactionalAccessTestCase.
     * 
     * @param name
     */
    public OptimisticTransactionalExtraAPITestCase(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        
        if (getEntityAccessStrategy() == null) {
            Configuration cfg = createConfiguration();
            JBossCacheRegionFactory rf  = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
            Cache localCache = rf.getCacheInstanceManager().getEntityCacheInstance();
            optimistic = localCache.getConfiguration().getNodeLockingScheme() == org.jboss.cache.config.Configuration.NodeLockingScheme.OPTIMISTIC;
            
            // Sleep a bit to avoid concurrent FLUSH problem
            avoidConcurrentFlush();
            
            EntityRegion localEntityRegion = rf.buildEntityRegion(REGION_NAME, cfg.getProperties(), null);
            setEntityRegionAccessStrategy(localEntityRegion.buildAccessStrategy(getAccessType()));
        }
    }

    protected void tearDown() throws Exception {
        
        super.tearDown();
    }
    
    protected Configuration createConfiguration() {
        Configuration cfg = CacheTestUtil.buildConfiguration(REGION_PREFIX, MultiplexedJBossCacheRegionFactory.class, true, false);
        cfg.setProperty(MultiplexingCacheInstanceManager.ENTITY_CACHE_RESOURCE_PROP, getCacheConfigName());
        return cfg;
    }
    
    protected String getCacheConfigName() {
        return "optimistic-entity";
    }
    
    protected boolean isUsingOptimisticLocking() {
        return optimistic;
    }

    protected AccessType getAccessType() {
        return AccessType.TRANSACTIONAL;
    }
    
    protected EntityRegionAccessStrategy getEntityAccessStrategy() {
        return localAccessStrategy;
    }
    
    protected void setEntityRegionAccessStrategy(EntityRegionAccessStrategy strategy) {
        localAccessStrategy = strategy;
    }
    
    /**
     * This is just a setup test where we assert that the cache config is
     * as we expected.
     */
    public void testCacheConfiguration() {
        assertTrue("Using Optimistic locking", isUsingOptimisticLocking());
    }
    
    /**
     * Test method for {@link TransactionalAccess#lockItem(java.lang.Object, java.lang.Object)}.
     */
    public void testLockItem() {
        assertNull(getEntityAccessStrategy().lockItem(KEY, new Integer(1)));
    }

    /**
     * Test method for {@link TransactionalAccess#lockRegion()}.
     */
    public void testLockRegion() {
        assertNull(getEntityAccessStrategy().lockRegion());
    }

    /**
     * Test method for {@link TransactionalAccess#unlockItem(java.lang.Object, org.hibernate.cache.access.SoftLock)}.
     */
    public void testUnlockItem() {
        getEntityAccessStrategy().unlockItem(KEY, new MockSoftLock());
    }

    /**
     * Test method for {@link TransactionalAccess#unlockRegion(org.hibernate.cache.access.SoftLock)}.
     */
    public void testUnlockRegion() {
        getEntityAccessStrategy().unlockItem(KEY, new MockSoftLock());
    }

    /**
     * Test method for {@link TransactionalAccess#afterInsert(java.lang.Object, java.lang.Object, java.lang.Object)}.
     */
    public void testAfterInsert() {
        assertFalse("afterInsert always returns false", getEntityAccessStrategy().afterInsert(KEY, VALUE1, new Integer(1)));
    }

    /**
     * Test method for {@link TransactionalAccess#afterUpdate(java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object, org.hibernate.cache.access.SoftLock)}.
     */
    public void testAfterUpdate() {
        assertFalse("afterInsert always returns false", getEntityAccessStrategy().afterUpdate(KEY, VALUE2, new Integer(1), new Integer(2), new MockSoftLock()));
    }
    
    public static class MockSoftLock implements SoftLock {
        
    }

}
