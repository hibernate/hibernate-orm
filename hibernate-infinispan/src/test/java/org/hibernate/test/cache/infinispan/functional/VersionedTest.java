package org.hibernate.test.cache.infinispan.functional;

import org.hibernate.PessimisticLockException;
import org.hibernate.Session;
import org.hibernate.StaleStateException;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.VersionedEntry;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.test.cache.infinispan.functional.entities.Item;
import org.infinispan.commons.util.ByRef;
import org.junit.Test;

import javax.transaction.Synchronization;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;


/**
 * Tests specific to versioned entries -based caches.
 * Similar to {@link TombstoneTest} but some cases have been removed since
 * we are modifying the cache only once, therefore some sequences of operations
 * would fail before touching the cache.
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class VersionedTest extends AbstractNonInvalidationTest {
   @Override
   public List<Object[]> getParameters() {
      return Arrays.asList(NONSTRICT_REPLICATED, NONSTRICT_DISTRIBUTED);
   }

   @Test
   public void testTwoRemoves() throws Exception {
      CyclicBarrier loadBarrier = new CyclicBarrier(2);
      CountDownLatch flushLatch = new CountDownLatch(2);
      CountDownLatch commitLatch = new CountDownLatch(1);

      Future<Boolean> first = removeFlushWait(itemId, loadBarrier, null, flushLatch, commitLatch);
      Future<Boolean> second = removeFlushWait(itemId, loadBarrier, null, flushLatch, commitLatch);
      awaitOrThrow(flushLatch);

      assertSingleCacheEntry();

      commitLatch.countDown();
      boolean firstResult = first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      boolean secondResult = second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      assertTrue(firstResult != secondResult);

      assertSingleEmpty();

      TIME_SERVICE.advance(TIMEOUT + 1);
      assertEmptyCache();
   }

   @Test
   public void testRemoveRolledBack() throws Exception {
      withTxSession(s -> {
         Item item = s.load(Item.class, itemId);
         s.delete(item);
         assertSingleCacheEntry();
         s.flush();
         assertSingleCacheEntry();
         markRollbackOnly(s);
      });
      assertSingleCacheEntry();
   }

   @Test
   public void testUpdateRolledBack() throws Exception {
      ByRef<Object> entryRef = new ByRef<>(null);
      withTxSession(s -> {
         Item item = s.load(Item.class, itemId);
         item.getDescription();
         Object prevEntry = assertSingleCacheEntry();
         entryRef.set(prevEntry);
         item.setDescription("Updated item");
         s.update(item);
         assertEquals(prevEntry, assertSingleCacheEntry());
         s.flush();
         assertEquals(prevEntry, assertSingleCacheEntry());
         markRollbackOnly(s);
      });
      assertEquals(entryRef.get(), assertSingleCacheEntry());
   }

   @Test
   public void testStaleReadDuringUpdate() throws Exception {
      ByRef<Object> entryRef = testStaleRead((s, item) -> {
         item.setDescription("Updated item");
         s.update(item);
      });
      assertNotEquals(entryRef.get(), assertSingleCacheEntry());
      withTxSession(s -> {
         Item item = s.load(Item.class, itemId);
         assertEquals("Updated item", item.getDescription());
      });
   }

   @Test
   public void testStaleReadDuringRemove() throws Exception {
      testStaleRead((s, item) -> s.delete(item));
      assertSingleEmpty();
      withTxSession(s -> {
         Item item = s.get(Item.class, itemId);
         assertNull(item);
      });
   }

   protected ByRef<Object> testStaleRead(BiConsumer<Session, Item> consumer) throws Exception {
      AtomicReference<Exception> synchronizationException = new AtomicReference<>();
      CountDownLatch syncLatch = new CountDownLatch(1);
      CountDownLatch commitLatch = new CountDownLatch(1);

      Future<Boolean> action = executor.submit(() -> withTxSessionApply(s -> {
         try {
            ((SessionImplementor) s).getTransactionCoordinator().getLocalSynchronizations().registerSynchronization(new Synchronization() {
               @Override
               public void beforeCompletion() {
               }

               @Override
               public void afterCompletion(int i) {
                  syncLatch.countDown();
                  try {
                     awaitOrThrow(commitLatch);
                  } catch (Exception e) {
                     synchronizationException.set(e);
                  }
               }
            });
            Item item = s.load(Item.class, itemId);
            consumer.accept(s, item);
            s.flush();
         } catch (StaleStateException e) {
            log.info("Exception thrown: ", e);
            markRollbackOnly(s);
            return false;
         } catch (PessimisticLockException e) {
            log.info("Exception thrown: ", e);
            markRollbackOnly(s);
            return false;
         }
         return true;
      }));
      awaitOrThrow(syncLatch);
      ByRef<Object> entryRef = new ByRef<>(null);
      try {
         withTxSession(s -> {
            Item item = s.load(Item.class, itemId);
            assertEquals("Original item", item.getDescription());
            entryRef.set(assertSingleCacheEntry());
         });
      } finally {
         commitLatch.countDown();
      }
      assertTrue(action.get(WAIT_TIMEOUT, TimeUnit.SECONDS));
      assertNull(synchronizationException.get());
      return entryRef;
   }

   @Test
   public void testUpdateEvictExpiration() throws Exception {
      CyclicBarrier loadBarrier = new CyclicBarrier(2);
      CountDownLatch preEvictLatch = new CountDownLatch(1);
      CountDownLatch postEvictLatch = new CountDownLatch(1);
      CountDownLatch flushLatch = new CountDownLatch(1);
      CountDownLatch commitLatch = new CountDownLatch(1);

      Future<Boolean> first = updateFlushWait(itemId, loadBarrier, null, flushLatch, commitLatch);
      Future<Boolean> second = evictWait(itemId, loadBarrier, preEvictLatch, postEvictLatch);
      awaitOrThrow(flushLatch);

      assertSingleCacheEntry();

      preEvictLatch.countDown();
      awaitOrThrow(postEvictLatch);
      assertSingleEmpty();

      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      assertSingleEmpty();

      TIME_SERVICE.advance(TIMEOUT + 1);
      assertEmptyCache();
   }

   @Test
   public void testEvictUpdateExpiration() throws Exception {
      // since the timestamp for update is based on session open/tx begin time, we have to do this sequentially
      sessionFactory().getCache().evictEntity(Item.class, itemId);

      Map contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      assertEquals(VersionedEntry.class, contents.get(itemId).getClass());

      TIME_SERVICE.advance(1);

      withTxSession(s -> {
         Item item = s.load(Item.class, itemId);
         item.setDescription("Updated item");
         s.update(item);
      });

      assertSingleCacheEntry();
      TIME_SERVICE.advance(TIMEOUT + 1);
      assertSingleCacheEntry();
   }

   protected void assertSingleEmpty() {
      Map contents = Caches.entrySet(entityCache).toMap();
      Object value;
      assertEquals(1, contents.size());
      value = contents.get(itemId);
      assertEquals(VersionedEntry.class, value.getClass());
      assertNull(((VersionedEntry) value).getValue());
   }

   protected void assertEmptyCache() {
      assertNull(entityCache.get(itemId)); // force expiration
      Map contents = Caches.entrySet(entityCache).toMap();
      assertEquals(Collections.EMPTY_MAP, contents);
   }

   protected Object assertSingleCacheEntry() {
      Map contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      Object value = contents.get(itemId);
      assertTrue(contents.toString(), value instanceof CacheEntry);
      return value;
   }
}
