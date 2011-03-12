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
package org.hibernate.test.cache.jbc.collection;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.extensions.TestSetup;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.impl.CacheDataDescriptionImpl;
import org.hibernate.cache.jbc.BasicRegionAdapter;
import org.hibernate.cache.jbc.JBossCacheRegionFactory;
import org.hibernate.cache.jbc.MultiplexedJBossCacheRegionFactory;
import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;
import org.hibernate.cache.jbc.collection.CollectionRegionImpl;
import org.hibernate.cache.jbc.entity.TransactionalAccess;
import org.hibernate.cache.jbc.util.CacheHelper;
import org.hibernate.cache.jbc.util.NonLockingDataVersion;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.jbc.AbstractJBossCacheTestCase;
import org.hibernate.test.util.CacheTestUtil;
import org.hibernate.util.ComparableComparator;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.NodeSPI;
import org.jboss.cache.transaction.BatchModeTransactionManager;

/**
 * Base class for tests of CollectionRegionAccessStrategy impls.
 *
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractCollectionRegionAccessStrategyTestCase extends AbstractJBossCacheTestCase {

    public static final String REGION_NAME = "test/com.foo.test";
    public static final String KEY_BASE = "KEY";
    public static final String VALUE1 = "VALUE1";
    public static final String VALUE2 = "VALUE2";

    protected static int testCount;

    protected static Configuration localCfg;
    protected static JBossCacheRegionFactory localRegionFactory;
    protected static Cache localCache;
    protected static Configuration remoteCfg;
    protected static JBossCacheRegionFactory remoteRegionFactory;
    protected static Cache remoteCache;

    protected CollectionRegion localCollectionRegion;
    protected CollectionRegionAccessStrategy localAccessStrategy;

    protected CollectionRegion remoteCollectionRegion;
    protected CollectionRegionAccessStrategy remoteAccessStrategy;

    protected boolean invalidation;
    protected boolean optimistic;
    protected boolean synchronous;

    protected Exception node1Exception;
    protected Exception node2Exception;

    protected AssertionFailedError node1Failure;
    protected AssertionFailedError node2Failure;

    public static Test getTestSetup(Class testClass, String configName) {
        TestSuite suite = new TestSuite(testClass);
        return new AccessStrategyTestSetup(suite, configName);
    }

    public static Test getTestSetup(Test test, String configName) {
        return new AccessStrategyTestSetup(test, configName);
    }

    /**
     * Create a new TransactionalAccessTestCase.
     *
     * @param name
     */
    public AbstractCollectionRegionAccessStrategyTestCase(String name) {
        super(name);
    }

    protected abstract AccessType getAccessType();

    protected void setUp() throws Exception {
        super.setUp();

        // Sleep a bit to avoid concurrent FLUSH problem
        avoidConcurrentFlush();

        invalidation = CacheHelper.isClusteredInvalidation(localCache);
        synchronous = CacheHelper.isSynchronous(localCache);
        optimistic = localCache.getConfiguration().getNodeLockingScheme() == org.jboss.cache.config.Configuration.NodeLockingScheme.OPTIMISTIC;
        localCollectionRegion = localRegionFactory.buildCollectionRegion(REGION_NAME, localCfg.getProperties(), getCacheDataDescription());
        localAccessStrategy = localCollectionRegion.buildAccessStrategy(getAccessType());

        // Sleep a bit to avoid concurrent FLUSH problem
        avoidConcurrentFlush();

        remoteCollectionRegion = remoteRegionFactory.buildCollectionRegion(REGION_NAME, remoteCfg.getProperties(), getCacheDataDescription());
        remoteAccessStrategy = remoteCollectionRegion.buildAccessStrategy(getAccessType());

        node1Exception = null;
        node2Exception = null;

        node1Failure = null;
        node2Failure  = null;
    }

    protected void tearDown() throws Exception {

        super.tearDown();

        if (localCollectionRegion != null)
            localCollectionRegion.destroy();
        if (remoteCollectionRegion != null)
            remoteCollectionRegion.destroy();

        try {
            localCache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
            localCache.removeNode(Fqn.ROOT);
        }
        catch (Exception e) {
            log.error("Problem purging local cache" ,e);
        }

        try {
            remoteCache.getInvocationContext().getOptionOverrides().setCacheModeLocal(true);
            remoteCache.removeNode(Fqn.ROOT);
        }
        catch (Exception e) {
            log.error("Problem purging remote cache" ,e);
        }

        node1Exception = null;
        node2Exception = null;

        node1Failure = null;
        node2Failure  = null;
    }

    protected static Configuration createConfiguration(String configName, String configResource) {
        Configuration cfg = CacheTestUtil.buildConfiguration(REGION_PREFIX, MultiplexedJBossCacheRegionFactory.class, true, false);
        cfg.setProperty(MultiplexingCacheInstanceManager.ENTITY_CACHE_RESOURCE_PROP, configName);
        if (configResource != null) {
           cfg.setProperty(MultiplexingCacheInstanceManager.CACHE_FACTORY_RESOURCE_PROP, configResource);
        }
        return cfg;
    }

    protected CacheDataDescription getCacheDataDescription() {
        return new CacheDataDescriptionImpl(true, true, ComparableComparator.INSTANCE);
    }

    protected boolean isUsingOptimisticLocking() {
        return optimistic;
    }

    public boolean isBlockingReads()
    {
    	return !isUsingOptimisticLocking();
    }

    protected boolean isUsingInvalidation() {
        return invalidation;
    }

    protected boolean isSynchronous() {
        return synchronous;
    }

    protected Fqn getRegionFqn(String regionName, String regionPrefix) {
        return BasicRegionAdapter.getTypeLastRegionFqn(regionName, regionPrefix, CollectionRegionImpl.TYPE);
    }

    /**
     * This is just a setup test where we assert that the cache config is
     * as we expected.
     */
    public abstract void testCacheConfiguration();

    /**
     * Test method for {@link TransactionalAccess#getRegion()}.
     */
    public void testGetRegion() {
        assertEquals("Correct region", localCollectionRegion, localAccessStrategy.getRegion());
    }

    /**
     * Test method for {@link TransactionalAccess#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object)}.
     */
    public void testPutFromLoad() throws Exception {
        putFromLoadTest(false);
    }

    /**
     * Test method for {@link TransactionalAccess#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object, boolean)}.
     */
    public void testPutFromLoadMinimal() throws Exception {
        putFromLoadTest(true);
    }

    /**
     * Simulate 2 nodes, both start, tx do a get, experience a cache miss,
     * then 'read from db.' First does a putFromLoad, then an evict (to represent a change).
     * Second tries to do a putFromLoad with stale data (i.e. it took
     * longer to read from the db).  Both commit their tx. Then
     * both start a new tx and get. First should see the updated data;
     * second should either see the updated data (isInvalidation()( == false)
     * or null (isInvalidation() == true).
     *
     * @param useMinimalAPI
     * @throws Exception
     */
    private void putFromLoadTest(final boolean useMinimalAPI) throws Exception {

        final String KEY = KEY_BASE + testCount++;

        final CountDownLatch writeLatch1 = new CountDownLatch(1);
        final CountDownLatch writeLatch2 = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(2);

        Thread node1 = new Thread() {

            public void run() {

                try {
                    long txTimestamp = System.currentTimeMillis();
                    BatchModeTransactionManager.getInstance().begin();

                    assertEquals("node1 starts clean", null, localAccessStrategy.get(KEY, txTimestamp));

                    writeLatch1.await();

                    if (useMinimalAPI) {
                        localAccessStrategy.putFromLoad(KEY, VALUE2, txTimestamp, new Integer(2), true);
                    }
                    else {
                        localAccessStrategy.putFromLoad(KEY, VALUE2, txTimestamp, new Integer(2));
                    }

                    BatchModeTransactionManager.getInstance().commit();
                }
                catch (Exception e) {
                    log.error("node1 caught exception", e);
                    node1Exception = e;
                    rollback();
                }
                catch (AssertionFailedError e) {
                    node1Failure = e;
                    rollback();
                }
                finally {
                    // Let node2 write
                    writeLatch2.countDown();
                    completionLatch.countDown();
                }
            }
        };

        Thread node2 = new Thread() {

            public void run() {

                try {
                    long txTimestamp = System.currentTimeMillis();
                    BatchModeTransactionManager.getInstance().begin();

                    assertNull("node2 starts clean", remoteAccessStrategy.get(KEY, txTimestamp));

                    // Let node1 write
                    writeLatch1.countDown();
                    // Wait for node1 to finish
                    writeLatch2.await();

                    // Let the first PFER propagate
                    sleep(200);

                    if (useMinimalAPI) {
                        remoteAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1), true);
                    }
                    else {
                        remoteAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1));
                    }

                    BatchModeTransactionManager.getInstance().commit();
                }
                catch (Exception e) {
                    log.error("node2 caught exception", e);
                    node2Exception = e;
                    rollback();
                }
                catch (AssertionFailedError e) {
                    node2Failure = e;
                    rollback();
                }
                finally {
                    completionLatch.countDown();
                }
            }
        };

        node1.setDaemon(true);
        node2.setDaemon(true);

        node1.start();
        node2.start();

        assertTrue("Threads completed", completionLatch.await(2, TimeUnit.SECONDS));

        if (node1Failure != null)
            throw node1Failure;
        if (node2Failure != null)
            throw node2Failure;

        assertEquals("node1 saw no exceptions", null, node1Exception);
        assertEquals("node2 saw no exceptions", null, node2Exception);

        // let the final PFER propagate
        sleep(100);

        long txTimestamp = System.currentTimeMillis();
        String msg1 = "Correct node1 value";
        String msg2 = "Correct node2 value";
        Object expected1 = null;
        Object expected2 = null;
        if (isUsingInvalidation()) {
            // PFER does not generate any invalidation, so each node should
            // succeed. We count on database locking and Hibernate removing
            // the collection on any update to prevent the situation we have
            // here where the caches have inconsistent data
            expected1 = VALUE2;
            expected2 = VALUE1;
        }
        else {
            // the initial VALUE2 should prevent the node2 put
            expected1 = VALUE2;
            expected2 = VALUE2;
        }

        assertEquals(msg1, expected1, localAccessStrategy.get(KEY, txTimestamp));
        assertEquals(msg2, expected2, remoteAccessStrategy.get(KEY, txTimestamp));
    }

    /**
     * Test method for {@link TransactionalAccess#remove(java.lang.Object)}.
     */
    public void testRemove() {
        evictOrRemoveTest(false);
    }

    /**
     * Test method for {@link TransactionalAccess#removeAll()}.
     */
    public void testRemoveAll() {
        evictOrRemoveAllTest(false);
    }

    /**
     * Test method for {@link TransactionalAccess#evict(java.lang.Object)}.
     *
     * FIXME add testing of the "immediately without regard for transaction
     * isolation" bit in the CollectionRegionAccessStrategy API.
     */
    public void testEvict() {
        evictOrRemoveTest(true);
    }

    /**
     * Test method for {@link TransactionalAccess#evictAll()}.
     *
     * FIXME add testing of the "immediately without regard for transaction
     * isolation" bit in the CollectionRegionAccessStrategy API.
     */
    public void testEvictAll() {
        evictOrRemoveAllTest(true);
    }

    private void evictOrRemoveTest(boolean evict) {

        final String KEY = KEY_BASE + testCount++;

        assertNull("local is clean", localAccessStrategy.get(KEY, System.currentTimeMillis()));
        assertNull("remote is clean", remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

        localAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
        assertEquals(VALUE1, localAccessStrategy.get(KEY, System.currentTimeMillis()));
        remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
        assertEquals(VALUE1, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

        // Wait for async propagation
        sleep(250);

        if (evict)
            localAccessStrategy.evict(KEY);
        else
            localAccessStrategy.remove(KEY);

        assertEquals(null, localAccessStrategy.get(KEY, System.currentTimeMillis()));

        assertEquals(null, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
    }

    private void evictOrRemoveAllTest(boolean evict) {

        final String KEY = KEY_BASE + testCount++;

        Fqn regionFqn = getRegionFqn(REGION_NAME, REGION_PREFIX);

        Node regionRoot = localCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        assertEquals(0, getValidChildrenCount(regionRoot));
        assertTrue(regionRoot.isResident());

        if (isUsingOptimisticLocking()) {
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
        }

        regionRoot = remoteCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        assertEquals(0, getValidChildrenCount(regionRoot));
        assertTrue(regionRoot.isResident());

        if (isUsingOptimisticLocking()) {
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
        }

        assertNull("local is clean", localAccessStrategy.get(KEY, System.currentTimeMillis()));
        assertNull("remote is clean", remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

        localAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
        assertEquals(VALUE1, localAccessStrategy.get(KEY, System.currentTimeMillis()));
        remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
        assertEquals(VALUE1, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

        // Wait for async propagation
        sleep(250);

        if (isUsingOptimisticLocking()) {
            regionRoot = localCache.getRoot().getChild(regionFqn);
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
            regionRoot = remoteCache.getRoot().getChild(regionFqn);
            assertEquals(NonLockingDataVersion.class, ((NodeSPI) regionRoot).getVersion().getClass());
        }

        if (evict)
            localAccessStrategy.evictAll();
        else
            localAccessStrategy.removeAll();

        // This should re-establish the region root node
        assertNull(localAccessStrategy.get(KEY, System.currentTimeMillis()));

        regionRoot = localCache.getRoot().getChild(regionFqn);
         assertFalse(regionRoot == null);
         assertEquals(0, getValidChildrenCount(regionRoot));
         assertTrue(regionRoot.isValid());
         assertTrue(regionRoot.isResident());

        // Re-establishing the region root on the local node doesn't
        // propagate it to other nodes. Do a get on the remote node to re-establish
        assertEquals(null, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

        regionRoot = remoteCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        assertTrue(regionRoot.isValid());
        assertTrue(regionRoot.isResident());
        // Not invalidation, so we didn't insert a child above
        assertEquals(0, getValidChildrenCount(regionRoot));

        // Test whether the get above messes up the optimistic version
        remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
        assertEquals(VALUE1, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

        // Revalidate the region root
        regionRoot = remoteCache.getRoot().getChild(regionFqn);
        assertFalse(regionRoot == null);
        assertTrue(regionRoot.isValid());
        assertTrue(regionRoot.isResident());
        // Region root should have 1 child -- the one we added above
        assertEquals(1, getValidChildrenCount(regionRoot));

        // Wait for async propagation of the putFromLoad
        sleep(250);

        assertEquals("local is correct", (isUsingInvalidation() ? null : VALUE1), localAccessStrategy.get(KEY, System.currentTimeMillis()));
        assertEquals("remote is correct", VALUE1, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
    }

    private void rollback() {
        try {
            BatchModeTransactionManager.getInstance().rollback();
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    private static class AccessStrategyTestSetup extends TestSetup {

        private static final String PREFER_IPV4STACK = "java.net.preferIPv4Stack";

        private final String configResource;
        private final String configName;
        private String preferIPv4Stack;

        public AccessStrategyTestSetup(Test test, String configName) {
            this(test, configName, null);
        }

        public AccessStrategyTestSetup(Test test, String configName, String configResource) {
            super(test);
            this.configName = configName;
            this.configResource = configResource;
        }

        @Override
        protected void setUp() throws Exception {
            super.setUp();

            // Try to ensure we use IPv4; otherwise cluster formation is very slow
            preferIPv4Stack = System.getProperty(PREFER_IPV4STACK);
            System.setProperty(PREFER_IPV4STACK, "true");

            localCfg = createConfiguration(configName, configResource);
            localRegionFactory = CacheTestUtil.startRegionFactory(localCfg);
            localCache = localRegionFactory.getCacheInstanceManager().getCollectionCacheInstance();

            remoteCfg = createConfiguration(configName, configResource);
            remoteRegionFactory  = CacheTestUtil.startRegionFactory(remoteCfg);
            remoteCache = remoteRegionFactory.getCacheInstanceManager().getCollectionCacheInstance();
        }

        @Override
        protected void tearDown() throws Exception {
            try {
                super.tearDown();
            }
            finally {
                if (preferIPv4Stack == null)
                    System.clearProperty(PREFER_IPV4STACK);
                else
                    System.setProperty(PREFER_IPV4STACK, preferIPv4Stack);
            }

            if (localRegionFactory != null)
                localRegionFactory.stop();

            if (remoteRegionFactory != null)
                remoteRegionFactory.stop();
        }


    }

}
