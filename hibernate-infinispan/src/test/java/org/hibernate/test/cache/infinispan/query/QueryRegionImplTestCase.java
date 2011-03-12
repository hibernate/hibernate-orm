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
package org.hibernate.test.cache.infinispan.query;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.Region;
import org.hibernate.cache.StandardQueryCache;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.infinispan.util.CacheAdapterImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.infinispan.AbstractGeneralDataRegionTestCase;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.infinispan.util.concurrent.IsolationLevel;

/**
 * Tests of QueryResultRegionImpl.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class QueryRegionImplTestCase extends AbstractGeneralDataRegionTestCase {

   // protected static final String REGION_NAME = "test/" + StandardQueryCache.class.getName();

   /**
    * Create a new EntityRegionImplTestCase.
    * 
    * @param name
    */
   public QueryRegionImplTestCase(String name) {
      super(name);
   }

   @Override
   protected Region createRegion(InfinispanRegionFactory regionFactory, String regionName, Properties properties, CacheDataDescription cdd) {
      return regionFactory.buildQueryResultsRegion(regionName, properties);
   }

   @Override
   protected String getStandardRegionName(String regionPrefix) {
      return regionPrefix + "/" + StandardQueryCache.class.getName();
   }

   @Override
   protected CacheAdapter getInfinispanCache(InfinispanRegionFactory regionFactory) {
      return CacheAdapterImpl.newInstance(regionFactory.getCacheManager().getCache("local-query"));
   }
   
   @Override
   protected Configuration createConfiguration() {
      return CacheTestUtil.buildCustomQueryCacheConfiguration("test", "replicated-query");
   }

   public void testPutDoesNotBlockGet() throws Exception {
      putDoesNotBlockGetTest();
   }

   private void putDoesNotBlockGetTest() throws Exception {
      Configuration cfg = createConfiguration();
      InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());

      // Sleep a bit to avoid concurrent FLUSH problem
      avoidConcurrentFlush();

      final QueryResultsRegion region = regionFactory.buildQueryResultsRegion(getStandardRegionName(REGION_PREFIX), cfg
               .getProperties());

      region.put(KEY, VALUE1);
      assertEquals(VALUE1, region.get(KEY));

      final CountDownLatch readerLatch = new CountDownLatch(1);
      final CountDownLatch writerLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(1);
      final ExceptionHolder holder = new ExceptionHolder();

      Thread reader = new Thread() {
         public void run() {
            try {
               BatchModeTransactionManager.getInstance().begin();
               log.debug("Transaction began, get value for key");
               assertTrue(VALUE2.equals(region.get(KEY)) == false);
               BatchModeTransactionManager.getInstance().commit();
            } catch (AssertionFailedError e) {
               holder.a1 = e;
               rollback();
            } catch (Exception e) {
               holder.e1 = e;
               rollback();
            } finally {
               readerLatch.countDown();
            }
         }
      };

      Thread writer = new Thread() {
         public void run() {
            try {
               BatchModeTransactionManager.getInstance().begin();
               log.debug("Put value2");
               region.put(KEY, VALUE2);
               log.debug("Put finished for value2, await writer latch");
               writerLatch.await();
               log.debug("Writer latch finished");
               BatchModeTransactionManager.getInstance().commit();
               log.debug("Transaction committed");
            } catch (Exception e) {
               holder.e2 = e;
               rollback();
            } finally {
               completionLatch.countDown();
            }
         }
      };

      reader.setDaemon(true);
      writer.setDaemon(true);

      writer.start();
      assertFalse("Writer is blocking", completionLatch.await(100, TimeUnit.MILLISECONDS));

      // Start the reader
      reader.start();
      assertTrue("Reader finished promptly", readerLatch.await(1000000000, TimeUnit.MILLISECONDS));

      writerLatch.countDown();
      assertTrue("Reader finished promptly", completionLatch.await(100, TimeUnit.MILLISECONDS));

      assertEquals(VALUE2, region.get(KEY));

      if (holder.a1 != null)
         throw holder.a1;
      else if (holder.a2 != null)
         throw holder.a2;

      assertEquals("writer saw no exceptions", null, holder.e1);
      assertEquals("reader saw no exceptions", null, holder.e2);
   }

   public void testGetDoesNotBlockPut() throws Exception {
      getDoesNotBlockPutTest();
   }

   private void getDoesNotBlockPutTest() throws Exception {
      Configuration cfg = createConfiguration();
      InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());

      // Sleep a bit to avoid concurrent FLUSH problem
      avoidConcurrentFlush();

      final QueryResultsRegion region = regionFactory.buildQueryResultsRegion(getStandardRegionName(REGION_PREFIX), cfg
               .getProperties());

      region.put(KEY, VALUE1);
      assertEquals(VALUE1, region.get(KEY));

      // final Fqn rootFqn = getRegionFqn(getStandardRegionName(REGION_PREFIX), REGION_PREFIX);
      final CacheAdapter jbc = getInfinispanCache(regionFactory);

      final CountDownLatch blockerLatch = new CountDownLatch(1);
      final CountDownLatch writerLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(1);
      final ExceptionHolder holder = new ExceptionHolder();

      Thread blocker = new Thread() {

         public void run() {
            // Fqn toBlock = new Fqn(rootFqn, KEY);
            GetBlocker blocker = new GetBlocker(blockerLatch, KEY);
            try {
               jbc.addListener(blocker);

               BatchModeTransactionManager.getInstance().begin();
               region.get(KEY);
               BatchModeTransactionManager.getInstance().commit();
            } catch (Exception e) {
               holder.e1 = e;
               rollback();
            } finally {
               jbc.removeListener(blocker);
            }
         }
      };

      Thread writer = new Thread() {

         public void run() {
            try {
               writerLatch.await();

               BatchModeTransactionManager.getInstance().begin();
               region.put(KEY, VALUE2);
               BatchModeTransactionManager.getInstance().commit();
            } catch (Exception e) {
               holder.e2 = e;
               rollback();
            } finally {
               completionLatch.countDown();
            }
         }
      };

      blocker.setDaemon(true);
      writer.setDaemon(true);

      boolean unblocked = false;
      try {
         blocker.start();
         writer.start();

         assertFalse("Blocker is blocking", completionLatch.await(100, TimeUnit.MILLISECONDS));
         // Start the writer
         writerLatch.countDown();
         assertTrue("Writer finished promptly", completionLatch.await(100, TimeUnit.MILLISECONDS));

         blockerLatch.countDown();
         unblocked = true;

         if (IsolationLevel.REPEATABLE_READ.equals(jbc.getConfiguration().getIsolationLevel())) {
            assertEquals(VALUE1, region.get(KEY));
         } else {
            assertEquals(VALUE2, region.get(KEY));
         }

         if (holder.a1 != null)
            throw holder.a1;
         else if (holder.a2 != null)
            throw holder.a2;

         assertEquals("blocker saw no exceptions", null, holder.e1);
         assertEquals("writer saw no exceptions", null, holder.e2);
      } finally {
         if (!unblocked)
            blockerLatch.countDown();
      }
   }

   @Listener
   public class GetBlocker {

      private CountDownLatch latch;
      // private Fqn fqn;
      private Object key;

      GetBlocker(CountDownLatch latch, Object key) {
         this.latch = latch;
         this.key = key;
      }

      @CacheEntryVisited
      public void nodeVisisted(CacheEntryVisitedEvent event) {
         if (event.isPre() && event.getKey().equals(key)) {
            try {
               latch.await();
            } catch (InterruptedException e) {
               log.error("Interrupted waiting for latch", e);
            }
         }
      }
   }

   private class ExceptionHolder {
      Exception e1;
      Exception e2;
      AssertionFailedError a1;
      AssertionFailedError a2;
   }
}
