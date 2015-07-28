/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hibernate.PessimisticLockException;
import org.hibernate.Session;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.spi.Region;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.TestForIssue;
import org.infinispan.AdvancedCache;
import org.infinispan.commands.read.GetKeyValueCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.Test;

import static org.infinispan.test.TestingUtil.withTx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Parent tests for both transactional and
 * read-only tests are defined in this class.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
public abstract class AbstractFunctionalTestCase extends SingleNodeTestCase {

   static final Log log = LogFactory.getLog(AbstractFunctionalTestCase.class);

   @Test
   public void testEmptySecondLevelCacheEntry() throws Exception {
      sessionFactory().getCache().evictCollectionRegion( Item.class.getName() + ".items" );
      Statistics stats = sessionFactory().getStatistics();
      stats.clear();
      SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );
      Map cacheEntries = statistics.getEntries();
      assertEquals( 0, cacheEntries.size() );
   }

   @Test
   public void testInsertDeleteEntity() throws Exception {
      final Statistics stats = sessionFactory().getStatistics();
      stats.clear();

      final Item item = new Item( "chris", "Chris's Item" );
      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            s.getTransaction().begin();
            s.persist(item);
            s.getTransaction().commit();
            s.close();
            return null;
         }
      });

      log.info("Entry persisted, let's load and delete it.");

      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            s.getTransaction().begin();
            Item found = (Item) s.load(Item.class, item.getId());
            log.info(stats.toString());
            assertEquals(item.getDescription(), found.getDescription());
            assertEquals(0, stats.getSecondLevelCacheMissCount());
            assertEquals(1, stats.getSecondLevelCacheHitCount());
            s.delete(found);
            s.getTransaction().commit();
            s.close();
            return null;
         }
      });
   }

   @Test
   public void testInsertClearCacheDeleteEntity() throws Exception {
      final Statistics stats = sessionFactory().getStatistics();
      stats.clear();

      final Item item = new Item( "chris", "Chris's Item" );
      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            s.getTransaction().begin();
            s.persist(item);
            s.getTransaction().commit();
            assertEquals(0, stats.getSecondLevelCacheMissCount());
            assertEquals(0, stats.getSecondLevelCacheHitCount());
            assertEquals(1, stats.getSecondLevelCachePutCount());
            s.close();
            return null;
         }
      });

      log.info("Entry persisted, let's load and delete it.");

      cleanupCache();
      Thread.sleep(10);

      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            s.getTransaction().begin();
            Item found = (Item) s.load(Item.class, item.getId());
            log.info(stats.toString());
            assertEquals(item.getDescription(), found.getDescription());
            assertEquals(1, stats.getSecondLevelCacheMissCount());
            assertEquals(0, stats.getSecondLevelCacheHitCount());
            assertEquals(2, stats.getSecondLevelCachePutCount());
            s.delete(found);
            s.getTransaction().commit();
            s.close();
            return null;
         }
      });
   }

   @Test
   @TestForIssue(jiraKey = "HHH-9868")
   public void testConcurrentRemoveAndPutFromLoad() throws Exception {
      final Item item = new Item( "chris", "Chris's Item" );

      withTx(tm, () -> {
         Session s = openSession();
         s.getTransaction().begin();
         s.persist(item);
         s.getTransaction().commit();
         s.close();
         return null;
      });
      Region region = sessionFactory().getSecondLevelCacheRegion(Item.class.getName());

      Phaser deletePhaser = new Phaser(2);
      Phaser getPhaser = new Phaser(2);
      HookInterceptor hook = new HookInterceptor();

      AdvancedCache entityCache = ((EntityRegionImpl) region).getCache();
      AdvancedCache pendingPutsCache = entityCache.getCacheManager().getCache(
            entityCache.getName() + "-" + InfinispanRegionFactory.PENDING_PUTS_CACHE_NAME).getAdvancedCache();
      pendingPutsCache.addInterceptor(hook, 0);

      Thread deleteThread = new Thread(() -> {
         try {
            withTx(tm, () -> {
               Session s = openSession();
               log.trace("Session opened");
               s.getTransaction().begin();
               log.trace("TX started");
               Item loadedItem = s.get(Item.class, item.getId());
               assertNotNull(loadedItem);
               arriveAndAwait(deletePhaser);
               arriveAndAwait(deletePhaser);
               log.trace("Item loaded");
               s.delete(loadedItem);
               log.trace("Item deleted");
               s.getTransaction().commit();
               log.trace("TX committed");
               // start get-thread here
               arriveAndAwait(deletePhaser);
               arriveAndAwait(deletePhaser);
               s.close();
               log.trace("Session closed");
               return null;
            });
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }, "delete-thread");
      Thread getThread = new Thread(() -> {
         try {
            withTx(tm, () -> {
               Session s = openSession();
               log.trace("Session opened");
               s.getTransaction().begin();
               log.trace("TX started");
               // DB load should happen before the record is deleted,
               // putFromLoad should happen after deleteThread ends
               Item loadedItem = s.get(Item.class, item.getId());
               assertNotNull(loadedItem);
               s.close();
               log.trace("Session closed");
               return null;
            });
         } catch (PessimisticLockException e) {
            // If we end up here, database locks guard us against situation tested
            // in this case and HHH-9868 cannot happen.
            // (delete-thread has ITEMS table write-locked and we try to acquire read-lock)
            try {
               arriveAndAwait(getPhaser);
               arriveAndAwait(getPhaser);
            } catch (Exception e1) {
               throw new RuntimeException(e1);
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }, "get-thread");

      deleteThread.start();
      // deleteThread loads the entity
      arriveAndAwait(deletePhaser);
      withTx(tm, () -> {
         sessionFactory().getCache().evictEntity(Item.class, item.getId());
         assertFalse(sessionFactory().getCache().containsEntity(Item.class, item.getId()));
         return null;
      });
      arriveAndAwait(deletePhaser);
      // delete thread invalidates PFER
      arriveAndAwait(deletePhaser);
      // get thread gets the entity from DB
      hook.block(getPhaser, getThread);
      getThread.start();
      arriveAndAwait(getPhaser);
      arriveAndAwait(deletePhaser);
      // delete thread finishes the remove from DB and cache
      deleteThread.join();
      hook.unblock();
      arriveAndAwait(getPhaser);
      // get thread puts the entry into cache
      getThread.join();

      withTx(tm, () -> {
         Session s = openSession();
         s.getTransaction().begin();
         Item loadedItem = s.get(Item.class, item.getId());
         assertNull(loadedItem);
         s.getTransaction().commit();
         s.close();
         return null;
      });
   }

   protected static void arriveAndAwait(Phaser phaser) throws TimeoutException, InterruptedException {
      phaser.awaitAdvanceInterruptibly(phaser.arrive(), 10, TimeUnit.SECONDS);
   }

   private static class HookInterceptor extends BaseCustomInterceptor {
      Phaser phaser;
      Thread thread;

      public synchronized void block(Phaser phaser, Thread thread) {
         this.phaser = phaser;
         this.thread = thread;
      }

      public synchronized void unblock() {
         phaser = null;
         thread = null;
      }

      @Override
      public Object visitGetKeyValueCommand(InvocationContext ctx, GetKeyValueCommand command) throws Throwable {
         Phaser phaser;
         Thread thread;
         synchronized (this) {
            phaser = this.phaser;
            thread = this.thread;
         }
         if (phaser != null && Thread.currentThread() == thread) {
            arriveAndAwait(phaser);
            arriveAndAwait(phaser);
         }
         return super.visitGetKeyValueCommand(ctx, command);
      }
   }
}
