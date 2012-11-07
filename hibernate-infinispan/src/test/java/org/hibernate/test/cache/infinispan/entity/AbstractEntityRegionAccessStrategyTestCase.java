/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;
import org.hibernate.cache.infinispan.util.Caches;
import org.infinispan.Cache;
import org.infinispan.test.TestingUtil;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.test.cache.infinispan.AbstractNonFunctionalTestCase;
import org.hibernate.test.cache.infinispan.NodeEnvironment;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Base class for tests of EntityRegionAccessStrategy impls.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractEntityRegionAccessStrategyTestCase extends AbstractNonFunctionalTestCase {

   private static final Logger log = Logger.getLogger(AbstractEntityRegionAccessStrategyTestCase.class);

   public static final String REGION_NAME = "test/com.foo.test";
   public static final String KEY_BASE = "KEY";
   public static final String VALUE1 = "VALUE1";
   public static final String VALUE2 = "VALUE2";

   protected static int testCount;

   protected NodeEnvironment localEnvironment;
   protected EntityRegionImpl localEntityRegion;
   protected EntityRegionAccessStrategy localAccessStrategy;

   protected NodeEnvironment remoteEnvironment;
   protected EntityRegionImpl remoteEntityRegion;
   protected EntityRegionAccessStrategy remoteAccessStrategy;

   protected boolean invalidation;
   protected boolean synchronous;

   protected Exception node1Exception;
   protected Exception node2Exception;

   protected AssertionFailedError node1Failure;
   protected AssertionFailedError node2Failure;

   @Before
   public void prepareResources() throws Exception {
      // to mimic exactly the old code results, both environments here are exactly the same...
      Configuration cfg = createConfiguration(getConfigurationName());
      localEnvironment = new NodeEnvironment(cfg);
      localEnvironment.prepare();

      localEntityRegion = localEnvironment.getEntityRegion(REGION_NAME, getCacheDataDescription());
      localAccessStrategy = localEntityRegion.buildAccessStrategy(getAccessType());

      invalidation = Caches.isInvalidationCache(localEntityRegion.getCache());
      synchronous = Caches.isSynchronousCache(localEntityRegion.getCache());

      // Sleep a bit to avoid concurrent FLUSH problem
      avoidConcurrentFlush();

      remoteEnvironment = new NodeEnvironment(cfg);
      remoteEnvironment.prepare();

      remoteEntityRegion = remoteEnvironment.getEntityRegion(REGION_NAME, getCacheDataDescription());
      remoteAccessStrategy = remoteEntityRegion.buildAccessStrategy(getAccessType());

      waitForClusterToForm(localEntityRegion.getCache(),
            remoteEntityRegion.getCache());
   }

   protected void waitForClusterToForm(Cache... caches) {
      TestingUtil.blockUntilViewsReceived(10000, Arrays.asList(caches));
   }

   protected abstract String getConfigurationName();

   protected static Configuration createConfiguration(String configName) {
      Configuration cfg = CacheTestUtil.buildConfiguration(
            REGION_PREFIX,
            InfinispanRegionFactory.class,
            true,
            false
      );
      cfg.setProperty(InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, configName);
      return cfg;
   }

   protected CacheDataDescription getCacheDataDescription() {
      return new CacheDataDescriptionImpl(true, true, ComparableComparator.INSTANCE);
   }

   @After
   public void releaseResources() throws Exception {
      try {
         if (localEnvironment != null) {
            localEnvironment.release();
         }
      } finally {
         if (remoteEnvironment != null) {
            remoteEnvironment.release();
         }
      }
   }

   protected abstract AccessType getAccessType();

   protected boolean isUsingInvalidation() {
      return invalidation;
   }

   protected boolean isSynchronous() {
      return synchronous;
   }

   protected void assertThreadsRanCleanly() {
      if (node1Failure != null) {
         throw node1Failure;
      }
      if (node2Failure != null) {
         throw node2Failure;
      }

      if (node1Exception != null) {
         log.error("node1 saw an exception", node1Exception);
         assertEquals("node1 saw no exceptions", null, node1Exception);
      }

      if (node2Exception != null) {
         log.error("node2 saw an exception", node2Exception);
         assertEquals("node2 saw no exceptions", null, node2Exception);
      }
   }

   @Test
   public abstract void testCacheConfiguration();

   @Test
   public void testGetRegion() {
      assertEquals("Correct region", localEntityRegion, localAccessStrategy.getRegion());
   }

   @Test
   public void testPutFromLoad() throws Exception {
      putFromLoadTest(false);
   }

   @Test
   public void testPutFromLoadMinimal() throws Exception {
      putFromLoadTest(true);
   }

   /**
    * Simulate 2 nodes, both start, tx do a get, experience a cache miss, then
    * 'read from db.' First does a putFromLoad, then an update. Second tries to
    * do a putFromLoad with stale data (i.e. it took longer to read from the db).
    * Both commit their tx. Then both start a new tx and get. First should see
    * the updated data; second should either see the updated data
    * (isInvalidation() == false) or null (isInvalidation() == true).
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

         @Override
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

         @Override
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

   @Test
   public void testInsert() throws Exception {

      final String KEY = KEY_BASE + testCount++;

      final CountDownLatch readLatch = new CountDownLatch(1);
      final CountDownLatch commitLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(2);

      Thread inserter = new Thread() {

         @Override
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

         @Override
         public void run() {

            try {
               long txTimestamp = System.currentTimeMillis();
               BatchModeTransactionManager.getInstance().begin();

               readLatch.await();
//               Object expected = !isBlockingReads() ? null : VALUE1;
               Object expected = null;

               assertEquals(
                     "Correct initial value", expected, localAccessStrategy.get(
                     KEY,
                     txTimestamp
               )
               );

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

   @Test
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

         @Override
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
               if (!readerUnlocked) {
                  readLatch.countDown();
               }
               log.debug("Completion latch countdown");
               completionLatch.countDown();
            }
         }
      };

      Thread reader = new Thread("testUpdate-reader") {

         @Override
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

   @Test
   public void testRemove() throws Exception {
      evictOrRemoveTest(false);
   }

   @Test
   public void testRemoveAll() throws Exception {
      evictOrRemoveAllTest(false);
   }

   @Test
   public void testEvict() throws Exception {
      evictOrRemoveTest(true);
   }

   @Test
   public void testEvictAll() throws Exception {
      evictOrRemoveAllTest(true);
   }

   private void evictOrRemoveTest(final boolean evict) throws Exception {
      final String KEY = KEY_BASE + testCount++;
      assertEquals(0, getValidKeyCount(localEntityRegion.getCache().keySet()));
      assertEquals(0, getValidKeyCount(remoteEntityRegion.getCache().keySet()));

      assertNull("local is clean", localAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertNull("remote is clean", remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

      localAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
      assertEquals(VALUE1, localAccessStrategy.get(KEY, System.currentTimeMillis()));
      remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
      assertEquals(VALUE1, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

      Caches.withinTx(localEntityRegion.getTransactionManager(), new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            if (evict)
               localAccessStrategy.evict(KEY);
            else
               localAccessStrategy.remove(KEY);
            return null;
         }
      });
      assertEquals(null, localAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertEquals(0, getValidKeyCount(localEntityRegion.getCache().keySet()));
      assertEquals(null, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertEquals(0, getValidKeyCount(remoteEntityRegion.getCache().keySet()));
   }

   private void evictOrRemoveAllTest(final boolean evict) throws Exception {
      final String KEY = KEY_BASE + testCount++;
      assertEquals(0, getValidKeyCount(localEntityRegion.getCache().keySet()));
      assertEquals(0, getValidKeyCount(remoteEntityRegion.getCache().keySet()));
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

      Caches.withinTx(localEntityRegion.getTransactionManager(), new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            if (evict) {
               log.debug("Call evict all locally");
               localAccessStrategy.evictAll();
            } else {
               localAccessStrategy.removeAll();
            }
            return null;
         }
      });

      // This should re-establish the region root node in the optimistic case
      assertNull(localAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertEquals(0, getValidKeyCount(localEntityRegion.getCache().keySet()));

      // Re-establishing the region root on the local node doesn't
      // propagate it to other nodes. Do a get on the remote node to re-establish
      assertEquals(null, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertEquals(0, getValidKeyCount(remoteEntityRegion.getCache().keySet()));

      // Test whether the get above messes up the optimistic version
      remoteAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
      assertEquals(VALUE1, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertEquals(1, getValidKeyCount(remoteEntityRegion.getCache().keySet()));

      // Wait for async propagation
      sleep(250);

      assertEquals(
            "local is correct", (isUsingInvalidation() ? null : VALUE1), localAccessStrategy
            .get(KEY, System.currentTimeMillis())
      );
      assertEquals(
            "remote is correct", VALUE1, remoteAccessStrategy.get(
            KEY, System
            .currentTimeMillis()
      )
      );
   }

   protected void rollback() {
      try {
         BatchModeTransactionManager.getInstance().rollback();
      } catch (Exception e) {
         log.error(e.getMessage(), e);
      }
   }
}
