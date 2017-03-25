package org.hibernate.test.cache.infinispan.functional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.PessimisticLockException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.cache.infinispan.functional.entities.Item;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.junit.Ignore;
import org.junit.Test;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.read.GetKeyValueCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests specific to invalidation mode caches
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class InvalidationTest extends SingleNodeTest {
   static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(ReadOnlyTest.class);

   @Override
   public List<Object[]> getParameters() {
      return Arrays.asList(TRANSACTIONAL, READ_WRITE_INVALIDATION);
   }

   @Override
   protected void addSettings(Map settings) {
      super.addSettings(settings);
      settings.put(TestInfinispanRegionFactory.PENDING_PUTS_SIMPLE, false);
   }

   @Test
   @TestForIssue(jiraKey = "HHH-9868")
   public void testConcurrentRemoveAndPutFromLoad() throws Exception {

      final Item item = new Item( "chris", "Chris's Item" );
      withTxSession(s -> {
         s.persist(item);
      });

      Phaser deletePhaser = new Phaser(2);
      Phaser getPhaser = new Phaser(2);
      HookInterceptor hook = new HookInterceptor();

      AdvancedCache pendingPutsCache = getPendingPutsCache(Item.class);
      pendingPutsCache.addInterceptor(hook, 0);
      AtomicBoolean getThreadBlockedInDB = new AtomicBoolean(false);

      Thread deleteThread = new Thread(() -> {
         try {
            withTxSession(s -> {
               Item loadedItem = s.get(Item.class, item.getId());
               assertNotNull(loadedItem);
               arriveAndAwait(deletePhaser, 2000);
               arriveAndAwait(deletePhaser, 2000);
               log.trace("Item loaded");
               s.delete(loadedItem);
               s.flush();
               log.trace("Item deleted");
               // start get-thread here
               arriveAndAwait(deletePhaser, 2000);
               // we need longer timeout since in non-MVCC DBs the get thread
               // can be blocked
               arriveAndAwait(deletePhaser, 4000);
            });
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }, "delete-thread");
      Thread getThread = new Thread(() -> {
         try {
            withTxSession(s -> {
               // DB load should happen before the record is deleted,
               // putFromLoad should happen after deleteThread ends
               Item loadedItem = s.get(Item.class, item.getId());
               if (getThreadBlockedInDB.get()) {
                  assertNull(loadedItem);
               } else {
                  assertNotNull(loadedItem);
               }
            });
         } catch (PessimisticLockException e) {
            // If we end up here, database locks guard us against situation tested
            // in this case and HHH-9868 cannot happen.
            // (delete-thread has ITEMS table write-locked and we try to acquire read-lock)
            try {
               arriveAndAwait(getPhaser, 2000);
               arriveAndAwait(getPhaser, 2000);
            } catch (Exception e1) {
               throw new RuntimeException(e1);
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }, "get-thread");

      deleteThread.start();
      // deleteThread loads the entity
      arriveAndAwait(deletePhaser, 2000);
      withTx(() -> {
         sessionFactory().getCache().evictEntity(Item.class, item.getId());
         assertFalse(sessionFactory().getCache().containsEntity(Item.class, item.getId()));
         return null;
      });
      arriveAndAwait(deletePhaser, 2000);
      // delete thread invalidates PFER
      arriveAndAwait(deletePhaser, 2000);
      // get thread gets the entity from DB
      hook.block(getPhaser, getThread);
      getThread.start();
      try {
         arriveAndAwait(getPhaser, 2000);
      } catch (TimeoutException e) {
         getThreadBlockedInDB.set(true);
      }
      arriveAndAwait(deletePhaser, 2000);
      // delete thread finishes the remove from DB and cache
      deleteThread.join();
      hook.unblock();
      arriveAndAwait(getPhaser, 2000);
      // get thread puts the entry into cache
      getThread.join();

      assertNoInvalidators(pendingPutsCache);

      withTxSession(s -> {
         Item loadedItem = s.get(Item.class, item.getId());
         assertNull(loadedItem);
      });
   }

   protected AdvancedCache getPendingPutsCache(Class<Item> entityClazz) {
      EntityRegionImpl region = (EntityRegionImpl) sessionFactory().getCache()
         .getEntityRegionAccess(entityClazz.getName()).getRegion();
      AdvancedCache entityCache = region.getCache();
      return (AdvancedCache) entityCache.getCacheManager().getCache(
            entityCache.getName() + "-" + InfinispanRegionFactory.DEF_PENDING_PUTS_RESOURCE).getAdvancedCache();
   }

   protected static void arriveAndAwait(Phaser phaser, int timeout) throws TimeoutException, InterruptedException {
      phaser.awaitAdvanceInterruptibly(phaser.arrive(), timeout, TimeUnit.MILLISECONDS);
   }

   @TestForIssue(jiraKey = "HHH-11304")
   @Test
   public void testFailedInsert() throws Exception {
      AdvancedCache pendingPutsCache = getPendingPutsCache(Item.class);
      assertNoInvalidators(pendingPutsCache);
      withTxSession(s -> {
         Item i = new Item("inserted", "bar");
         s.persist(i);
         s.flush();
         s.getTransaction().setRollbackOnly();
      });
      assertNoInvalidators(pendingPutsCache);
   }

   @TestForIssue(jiraKey = "HHH-11304")
   @Test
   public void testFailedUpdate() throws Exception {
      AdvancedCache pendingPutsCache = getPendingPutsCache(Item.class);
      assertNoInvalidators(pendingPutsCache);
      final Item item = new Item("before-update", "bar");
      withTxSession(s -> s.persist(item));

      withTxSession(s -> {
         Item item2 = s.load(Item.class, item.getId());
         assertEquals("before-update", item2.getName());
         item2.setName("after-update");
         s.persist(item2);
         s.flush();
         s.flush(); // workaround for HHH-11312
         s.getTransaction().setRollbackOnly();
      });
      assertNoInvalidators(pendingPutsCache);

      withTxSession(s -> {
         Item item3 = s.load(Item.class, item.getId());
         assertEquals("before-update", item3.getName());
         s.remove(item3);
      });
      assertNoInvalidators(pendingPutsCache);
   }

   @TestForIssue(jiraKey = "HHH-11304")
   @Test
   public void testFailedRemove() throws Exception {
      AdvancedCache pendingPutsCache = getPendingPutsCache(Item.class);
      assertNoInvalidators(pendingPutsCache);
      final Item item = new Item("before-remove", "bar");
      withTxSession(s -> s.persist(item));

      withTxSession(s -> {
         Item item2 = s.load(Item.class, item.getId());
         assertEquals("before-remove", item2.getName());
         s.remove(item2);
         s.flush();
         s.getTransaction().setRollbackOnly();
      });
      assertNoInvalidators(pendingPutsCache);

      withTxSession(s -> {
         Item item3 = s.load(Item.class, item.getId());
         assertEquals("before-remove", item3.getName());
         s.remove(item3);
      });
      assertNoInvalidators(pendingPutsCache);
   }

   protected void assertNoInvalidators(AdvancedCache<Object, Object> pendingPutsCache) throws Exception {
      Method getInvalidators = null;
      for (Map.Entry<Object, Object> entry : pendingPutsCache.entrySet()) {
         if (getInvalidators == null) {
            getInvalidators = entry.getValue().getClass().getMethod("getInvalidators");
            getInvalidators.setAccessible(true);
         }
         Collection invalidators = (Collection) getInvalidators.invoke(entry.getValue());
         if (invalidators != null) {
            assertTrue("Invalidators on key " + entry.getKey() + ": " + invalidators, invalidators.isEmpty());
         }
      }
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
            arriveAndAwait(phaser, 2000);
            arriveAndAwait(phaser, 2000);
         }
         return super.visitGetKeyValueCommand(ctx, command);
      }
   }
}
