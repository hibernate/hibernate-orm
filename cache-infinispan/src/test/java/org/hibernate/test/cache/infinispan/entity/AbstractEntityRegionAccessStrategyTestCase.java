/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
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
package org.hibernate.test.cache.infinispan.entity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.extensions.TestSetup;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.impl.CacheDataDescriptionImpl;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.infinispan.util.FlagAdapter;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.infinispan.AbstractNonFunctionalTestCase;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.hibernate.util.ComparableComparator;
import org.infinispan.transaction.tm.BatchModeTransactionManager;

/**
 * Base class for tests of EntityRegionAccessStrategy impls.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractEntityRegionAccessStrategyTestCase extends AbstractNonFunctionalTestCase {

   public static final String REGION_NAME = "test/com.foo.test";
   public static final String KEY_BASE = "KEY";
   public static final String VALUE1 = "VALUE1";
   public static final String VALUE2 = "VALUE2";

   protected static int testCount;

   protected static Configuration localCfg;
   protected static InfinispanRegionFactory localRegionFactory;
   protected CacheAdapter localCache;
   protected static Configuration remoteCfg;
   protected static InfinispanRegionFactory remoteRegionFactory;
   protected CacheAdapter remoteCache;

   protected boolean invalidation;
   protected boolean synchronous;

   protected EntityRegion localEntityRegion;
   protected EntityRegionAccessStrategy localAccessStrategy;

   protected EntityRegion remoteEntityRegion;
   protected EntityRegionAccessStrategy remoteAccessStrategy;

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
   public AbstractEntityRegionAccessStrategyTestCase(String name) {
      super(name);
   }

   protected abstract AccessType getAccessType();

   protected void setUp() throws Exception {
      super.setUp();

      // Sleep a bit to avoid concurrent FLUSH problem
      avoidConcurrentFlush();

      localEntityRegion = localRegionFactory.buildEntityRegion(REGION_NAME, localCfg
               .getProperties(), getCacheDataDescription());
      localAccessStrategy = localEntityRegion.buildAccessStrategy(getAccessType());

      localCache = ((BaseRegion) localEntityRegion).getCacheAdapter();

      invalidation = localCache.isClusteredInvalidation();
      synchronous = localCache.isSynchronous();

      // Sleep a bit to avoid concurrent FLUSH problem
      avoidConcurrentFlush();

      remoteEntityRegion = remoteRegionFactory.buildEntityRegion(REGION_NAME, remoteCfg
               .getProperties(), getCacheDataDescription());
      remoteAccessStrategy = remoteEntityRegion.buildAccessStrategy(getAccessType());

      remoteCache = ((BaseRegion) remoteEntityRegion).getCacheAdapter();

      node1Exception = null;
      node2Exception = null;

      node1Failure = null;
      node2Failure = null;
   }

   protected void tearDown() throws Exception {

      super.tearDown();

      if (localEntityRegion != null)
         localEntityRegion.destroy();
      if (remoteEntityRegion != null)
         remoteEntityRegion.destroy();

      try {
         localCache.withFlags(FlagAdapter.CACHE_MODE_LOCAL).clear();
      } catch (Exception e) {
         log.error("Problem purging local cache", e);
      }

      try {
         remoteCache.withFlags(FlagAdapter.CACHE_MODE_LOCAL).clear();
      } catch (Exception e) {
         log.error("Problem purging remote cache", e);
      }

      node1Exception = null;
      node2Exception = null;

      node1Failure = null;
      node2Failure = null;
   }

   protected static Configuration createConfiguration(String configName) {
      Configuration cfg = CacheTestUtil.buildConfiguration(REGION_PREFIX, InfinispanRegionFactory.class, true, false);
      cfg.setProperty(InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, configName);
      return cfg;
   }

   protected CacheDataDescription getCacheDataDescription() {
      return new CacheDataDescriptionImpl(true, true, ComparableComparator.INSTANCE);
   }

   protected boolean isUsingInvalidation() {
      return invalidation;
   }

   protected boolean isSynchronous() {
      return synchronous;
   }

   protected void assertThreadsRanCleanly() {
      if (node1Failure != null)
         throw node1Failure;
      if (node2Failure != null)
         throw node2Failure;

      if (node1Exception != null) {
         log.error("node1 saw an exception", node1Exception);
         assertEquals("node1 saw no exceptions", null, node1Exception);
      }

      if (node2Exception != null) {
         log.error("node2 saw an exception", node2Exception);
         assertEquals("node2 saw no exceptions", null, node2Exception);
      }
   }

   /**
    * This is just a setup test where we assert that the cache config is as we expected.
    */
   public abstract void testCacheConfiguration();

   /**
    * Test method for {@link TransactionalAccess#getRegion()}.
    */
   public void testGetRegion() {
      assertEquals("Correct region", localEntityRegion, localAccessStrategy.getRegion());
   }

   /**
    * Test method for
    * {@link TransactionalAccess#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object)}
    * .
    */
   public void testPutFromLoad() throws Exception {
      putFromLoadTest(false);
   }

   /**
    * Test method for
    * {@link TransactionalAccess#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object, boolean)}
    * .
    */
   public void testPutFromLoadMinimal() throws Exception {
      putFromLoadTest(true);
   }

   /**
    * Simulate 2 nodes, both start, tx do a get, experience a cache miss, then 'read from db.' First
    * does a putFromLoad, then an update. Second tries to do a putFromLoad with stale data (i.e. it
    * took longer to read from the db). Both commit their tx. Then both start a new tx and get.
    * First should see the updated data; second should either see the updated data (isInvalidation()
    * == false) or null (isInvalidation() == true).
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

               assertNull("node1 starts clean", localAccessStrategy.get(KEY, txTimestamp));

               writeLatch1.await();

               if (useMinimalAPI) {
                  localAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1), true);
               } else {
                  localAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1));
               }

               localAccessStrategy.update(KEY, VALUE2, new Integer(2), new Integer(1));

               BatchModeTransactionManager.getInstance().commit();
            } catch (Exception e) {
               log.error("node1 caught exception", e);
               node1Exception = e;
               rollback();
            } catch (AssertionFailedError e) {
               node1Failure = e;
               rollback();
            } finally {
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

               assertNull("node1 starts clean", remoteAccessStrategy.get(KEY, txTimestamp));

               // Let node1 write
               writeLatch1.countDown();
               // Wait for node1 to finish
               writeLatch2.await();

               if (useMinimalAPI) {
                  remoteAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1), true);
               } else {
                  remoteAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1));
               }

               BatchModeTransactionManager.getInstance().commit();
            } catch (Exception e) {
               log.error("node2 caught exception", e);
               node2Exception = e;
               rollback();
            } catch (AssertionFailedError e) {
               node2Failure = e;
               rollback();
            } finally {
               completionLatch.countDown();
            }
         }
      };

      node1.setDaemon(true);
      node2.setDaemon(true);

      node1.start();
      node2.start();

      assertTrue("Threads completed", completionLatch.await(2, TimeUnit.SECONDS));

      assertThreadsRanCleanly();

      long txTimestamp = System.currentTimeMillis();
      assertEquals("Correct node1 value", VALUE2, localAccessStrategy.get(KEY, txTimestamp));

      if (isUsingInvalidation()) {
         // no data version to prevent the PFER; we count on db locks preventing this
         assertEquals("Expected node2 value", VALUE1, remoteAccessStrategy.get(KEY, txTimestamp));
      } else {
         // The node1 update is replicated, preventing the node2 PFER
         assertEquals("Correct node2 value", VALUE2, remoteAccessStrategy.get(KEY, txTimestamp));
      }
   }

   /**
    * Test method for
    * {@link TransactionalAccess#insert(java.lang.Object, java.lang.Object, java.lang.Object)}.
    */
   public void testInsert() throws Exception {

      final String KEY = KEY_BASE + testCount++;

      final CountDownLatch readLatch = new CountDownLatch(1);
      final CountDownLatch commitLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(2);

      Thread inserter = new Thread() {

         public void run() {

            try {
               long txTimestamp = System.currentTimeMillis();
               BatchModeTransactionManager.getInstance().begin();

               assertNull("Correct initial value", localAccessStrategy.get(KEY, txTimestamp));

               localAccessStrategy.insert(KEY, VALUE1, new Integer(1));

               readLatch.countDown();
               commitLatch.await();

               BatchModeTransactionManager.getInstance().commit();
            } catch (Exception e) {
               log.error("node1 caught exception", e);
               node1Exception = e;
               rollback();
            } catch (AssertionFailedError e) {
               node1Failure = e;
               rollback();
            } finally {
               completionLatch.countDown();
            }
         }
      };

      Thread reader = new Thread() {

         public void run() {

            try {
               long txTimestamp = System.currentTimeMillis();
               BatchModeTransactionManager.getInstance().begin();

               readLatch.await();
//               Object expected = !isBlockingReads() ? null : VALUE1;
               Object expected = null;

               assertEquals("Correct initial value", expected, localAccessStrategy.get(KEY,
                        txTimestamp));

               BatchModeTransactionManager.getInstance().commit();
            } catch (Exception e) {
               log.error("node1 caught exception", e);
               node1Exception = e;
               rollback();
            } catch (AssertionFailedError e) {
               node1Failure = e;
               rollback();
            } finally {
               commitLatch.countDown();
               completionLatch.countDown();
            }
         }
      };

      inserter.setDaemon(true);
      reader.setDaemon(true);
      inserter.start();
      reader.start();

      assertTrue("Threads completed", completionLatch.await(1, TimeUnit.SECONDS));

      assertThreadsRanCleanly();

      long txTimestamp = System.currentTimeMillis();
      assertEquals("Correct node1 value", VALUE1, localAccessStrategy.get(KEY, txTimestamp));
      Object expected = isUsingInvalidation() ? null : VALUE1;
      assertEquals("Correct node2 value", expected, remoteAccessStrategy.get(KEY, txTimestamp));
   }

   /**
    * Test method for
    * {@link TransactionalAccess#update(java.lang.Object, java.lang.Object, java.lang.Object, java.lang.Object)}
    * .
    */
   public void testUpdate() throws Exception {

      final String KEY = KEY_BASE + testCount++;

      // Set up initial state
      localAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
      remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));

      // Let the async put propagate
      sleep(250);

      final CountDownLatch readLatch = new CountDownLatch(1);
      final CountDownLatch commitLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(2);

      Thread updater = new Thread("testUpdate-updater") {

         public void run() {
            boolean readerUnlocked = false;
            try {
               long txTimestamp = System.currentTimeMillis();
               BatchModeTransactionManager.getInstance().begin();
               log.debug("Transaction began, get initial value");
               assertEquals("Correct initial value", VALUE1, localAccessStrategy.get(KEY, txTimestamp));
               log.debug("Now update value");
               localAccessStrategy.update(KEY, VALUE2, new Integer(2), new Integer(1));
               log.debug("Notify the read latch");
               readLatch.countDown();
               readerUnlocked = true;
               log.debug("Await commit");
               commitLatch.await();
               BatchModeTransactionManager.getInstance().commit();
            } catch (Exception e) {
               log.error("node1 caught exception", e);
               node1Exception = e;
               rollback();
            } catch (AssertionFailedError e) {
               node1Failure = e;
               rollback();
            } finally {
               if (!readerUnlocked) readLatch.countDown();
               log.debug("Completion latch countdown");
               completionLatch.countDown();
            }
         }
      };

      Thread reader = new Thread("testUpdate-reader") {

         public void run() {
            try {
               long txTimestamp = System.currentTimeMillis();
               BatchModeTransactionManager.getInstance().begin();
               log.debug("Transaction began, await read latch");
               readLatch.await();
               log.debug("Read latch acquired, verify local access strategy");

               // This won't block w/ mvc and will read the old value
               Object expected = VALUE1;
               assertEquals("Correct value", expected, localAccessStrategy.get(KEY, txTimestamp));

               BatchModeTransactionManager.getInstance().commit();
            } catch (Exception e) {
               log.error("node1 caught exception", e);
               node1Exception = e;
               rollback();
            } catch (AssertionFailedError e) {
               node1Failure = e;
               rollback();
            } finally {
               commitLatch.countDown();
               log.debug("Completion latch countdown");
               completionLatch.countDown();
            }
         }
      };

      updater.setDaemon(true);
      reader.setDaemon(true);
      updater.start();
      reader.start();

      // Should complete promptly
      assertTrue(completionLatch.await(2, TimeUnit.SECONDS));

      assertThreadsRanCleanly();

      long txTimestamp = System.currentTimeMillis();
      assertEquals("Correct node1 value", VALUE2, localAccessStrategy.get(KEY, txTimestamp));
      Object expected = isUsingInvalidation() ? null : VALUE2;
      assertEquals("Correct node2 value", expected, remoteAccessStrategy.get(KEY, txTimestamp));
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
    * FIXME add testing of the "immediately without regard for transaction isolation" bit in the
    * EntityRegionAccessStrategy API.
    */
   public void testEvict() {
      evictOrRemoveTest(true);
   }

   /**
    * Test method for {@link TransactionalAccess#evictAll()}.
    * 
    * FIXME add testing of the "immediately without regard for transaction isolation" bit in the
    * EntityRegionAccessStrategy API.
    */
   public void testEvictAll() {
      evictOrRemoveAllTest(true);
   }

   private void evictOrRemoveTest(boolean evict) {
      final String KEY = KEY_BASE + testCount++;
      assertEquals(0, getValidKeyCount(localCache.keySet()));
      assertEquals(0, getValidKeyCount(remoteCache.keySet()));

      assertNull("local is clean", localAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertNull("remote is clean", remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

      localAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
      assertEquals(VALUE1, localAccessStrategy.get(KEY, System.currentTimeMillis()));
      remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
      assertEquals(VALUE1, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

      if (evict)
         localAccessStrategy.evict(KEY);
      else
         localAccessStrategy.remove(KEY);

      assertEquals(null, localAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertEquals(0, getValidKeyCount(localCache.keySet()));
      assertEquals(null, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertEquals(0, getValidKeyCount(remoteCache.keySet()));
   }

   private void evictOrRemoveAllTest(boolean evict) {
      final String KEY = KEY_BASE + testCount++;
      assertEquals(0, getValidKeyCount(localCache.keySet()));
      assertEquals(0, getValidKeyCount(remoteCache.keySet()));
      assertNull("local is clean", localAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertNull("remote is clean", remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

      localAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
      assertEquals(VALUE1, localAccessStrategy.get(KEY, System.currentTimeMillis()));

      // Wait for async propagation
      sleep(250);

      remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
      assertEquals(VALUE1, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

      // Wait for async propagation
      sleep(250);

      if (evict) {
         log.debug("Call evict all locally");
         localAccessStrategy.evictAll();
      } else {
         localAccessStrategy.removeAll();
      }

      // This should re-establish the region root node in the optimistic case
      assertNull(localAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertEquals(0, getValidKeyCount(localCache.keySet()));

      // Re-establishing the region root on the local node doesn't
      // propagate it to other nodes. Do a get on the remote node to re-establish
      assertEquals(null, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertEquals(0, getValidKeyCount(remoteCache.keySet()));

      // Test whether the get above messes up the optimistic version
      remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
      assertEquals(VALUE1, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertEquals(1, getValidKeyCount(remoteCache.keySet()));

      // Wait for async propagation
      sleep(250);

      assertEquals("local is correct", (isUsingInvalidation() ? null : VALUE1), localAccessStrategy
               .get(KEY, System.currentTimeMillis()));
      assertEquals("remote is correct", VALUE1, remoteAccessStrategy.get(KEY, System
               .currentTimeMillis()));
   }

   protected void rollback() {
      try {
         BatchModeTransactionManager.getInstance().rollback();
      } catch (Exception e) {
         log.error(e.getMessage(), e);
      }
   }

   private static class AccessStrategyTestSetup extends TestSetup {

      private static final String PREFER_IPV4STACK = "java.net.preferIPv4Stack";
      private final String configName;
      private String preferIPv4Stack;

      public AccessStrategyTestSetup(Test test, String configName) {
         super(test);
         this.configName = configName;
      }

      @Override
      protected void setUp() throws Exception {
         try {
            super.tearDown();
         } finally {
            if (preferIPv4Stack == null)
               System.clearProperty(PREFER_IPV4STACK);
            else
               System.setProperty(PREFER_IPV4STACK, preferIPv4Stack);
         }

         // Try to ensure we use IPv4; otherwise cluster formation is very slow
         preferIPv4Stack = System.getProperty(PREFER_IPV4STACK);
         System.setProperty(PREFER_IPV4STACK, "true");

         localCfg = createConfiguration(configName);
         localRegionFactory = CacheTestUtil.startRegionFactory(localCfg);

         remoteCfg = createConfiguration(configName);
         remoteRegionFactory = CacheTestUtil.startRegionFactory(remoteCfg);
      }

      @Override
      protected void tearDown() throws Exception {
         super.tearDown();

         if (localRegionFactory != null)
            localRegionFactory.stop();

         if (remoteRegionFactory != null)
            remoteRegionFactory.stop();
      }

   }

}
