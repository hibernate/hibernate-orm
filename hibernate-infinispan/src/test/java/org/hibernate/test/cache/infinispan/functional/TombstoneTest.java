package org.hibernate.test.cache.infinispan.functional;

import org.hibernate.PessimisticLockException;
import org.hibernate.StaleStateException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.FutureUpdate;
import org.hibernate.cache.infinispan.util.Tombstone;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;
import org.hibernate.test.cache.infinispan.functional.entities.Item;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.test.cache.infinispan.util.TestTimeService;
import org.infinispan.AdvancedCache;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;


/**
 * Tests specific to tombstone-based caches
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class TombstoneTest extends SingleNodeTest {
   private static Log log = LogFactory.getLog(TombstoneTest.class);

   private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
      AtomicInteger counter = new AtomicInteger();

      @Override
      public Thread newThread(Runnable r) {
         return new Thread(r, TombstoneTest.class.getSimpleName() + "-executor-" +  counter.incrementAndGet());
      }
   });
   private static final long TOMBSTONE_TIMEOUT = InfinispanRegionFactory.PENDING_PUTS_CACHE_CONFIGURATION.expiration().maxIdle();
   private static final int WAIT_TIMEOUT = 2000;
   private static final TestTimeService TIME_SERVICE = new TestTimeService();
   private Region region;
   private AdvancedCache entityCache;
   private long itemId;

   @Override
   public List<Object[]> getParameters() {
      return Arrays.asList(READ_WRITE_REPLICATED, READ_WRITE_DISTRIBUTED);
   }

   @Override
   protected void startUp() {
      super.startUp();
      region = sessionFactory().getSecondLevelCacheRegion(Item.class.getName());
      entityCache = ((EntityRegionImpl) region).getCache();
   }

   @Before
   public void insertAndClearCache() throws Exception {
      Item item = new Item("my item", "item that belongs to me");
      withTxSession(s -> s.persist(item));
      entityCache.clear();
      assertEquals("Cache is not empty", Collections.EMPTY_SET, Caches.keys(entityCache).toSet());
      itemId = item.getId();
   }

   @After
   public void cleanup() throws Exception {
      withTxSession(s -> {
         s.createQuery("delete from Item").executeUpdate();
      });
   }

   @AfterClass
   public static void shutdown() {
      EXECUTOR.shutdown();
   }

   @Override
   protected void addSettings(Map settings) {
      super.addSettings(settings);
      settings.put(TestInfinispanRegionFactory.TIME_SERVICE, TIME_SERVICE);
   }

   @Test
   public void testTombstoneExpiration() throws Exception {
      CyclicBarrier loadBarrier = new CyclicBarrier(2);
      CountDownLatch flushLatch = new CountDownLatch(2);
      CountDownLatch commitLatch = new CountDownLatch(1);

      Future<Boolean> first = removeFlushWait(itemId, loadBarrier, null, flushLatch, commitLatch);
      Future<Boolean> second = removeFlushWait(itemId, loadBarrier, null, flushLatch, commitLatch);
      awaitOrThrow(flushLatch);

      Map contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      assertEquals(Tombstone.class, contents.get(itemId).getClass());
      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      // after commit, the tombstone should still be in memory for some time (though, updatable)
      contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      assertEquals(Tombstone.class, contents.get(itemId).getClass());

      TIME_SERVICE.advance(TOMBSTONE_TIMEOUT + 1);
      assertNull(entityCache.get(itemId)); // force expiration
      contents = Caches.entrySet(entityCache).toMap();
      assertEquals(Collections.EMPTY_MAP, contents);
   }

   @Test
   public void testFutureUpdateExpiration() throws Exception {
      CyclicBarrier loadBarrier = new CyclicBarrier(2);
      CountDownLatch flushLatch = new CountDownLatch(2);
      CountDownLatch commitLatch = new CountDownLatch(1);

      Future<Boolean> first = updateFlushWait(itemId, loadBarrier, null, flushLatch, commitLatch);
      Future<Boolean> second = updateFlushWait(itemId, loadBarrier, null, flushLatch, commitLatch);
      awaitOrThrow(flushLatch);

      Map contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      assertEquals(FutureUpdate.class, contents.get(itemId).getClass());
      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      // since we had two concurrent updates, the result should be invalid
      contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      Object value = contents.get(itemId);
      if (value instanceof FutureUpdate) {
         // DB did not blocked two concurrent updates
         TIME_SERVICE.advance(TOMBSTONE_TIMEOUT + 1);
         assertNull(entityCache.get(itemId));
         contents = Caches.entrySet(entityCache).toMap();
         assertEquals(Collections.EMPTY_MAP, contents);
      } else {
         // DB left only one update to proceed, and the entry should not be expired
         assertNotNull(value);
         assertEquals(StandardCacheEntryImpl.class, value.getClass());
         TIME_SERVICE.advance(TOMBSTONE_TIMEOUT + 1);
         assertEquals(value, entityCache.get(itemId));
      }
   }

   @Test
   public void testRemoveUpdateExpiration() throws Exception {
      CyclicBarrier loadBarrier = new CyclicBarrier(2);
      CountDownLatch preFlushLatch = new CountDownLatch(1);
      CountDownLatch flushLatch = new CountDownLatch(1);
      CountDownLatch commitLatch = new CountDownLatch(1);

      Future<Boolean> first = removeFlushWait(itemId, loadBarrier, null, flushLatch, commitLatch);
      Future<Boolean> second = updateFlushWait(itemId, loadBarrier, preFlushLatch, null, commitLatch);
      awaitOrThrow(flushLatch);

      Map contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      assertEquals(Tombstone.class, contents.get(itemId).getClass());

      preFlushLatch.countDown();
      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      assertEquals(Tombstone.class, contents.get(itemId).getClass());

      TIME_SERVICE.advance(TOMBSTONE_TIMEOUT + 1);
      assertNull(entityCache.get(itemId)); // force expiration
      contents = Caches.entrySet(entityCache).toMap();
      assertEquals(Collections.EMPTY_MAP, contents);
   }

   @Test
   public void testUpdateRemoveExpiration() throws Exception {
      CyclicBarrier loadBarrier = new CyclicBarrier(2);
      CountDownLatch preFlushLatch = new CountDownLatch(1);
      CountDownLatch flushLatch = new CountDownLatch(1);
      CountDownLatch commitLatch = new CountDownLatch(1);

      Future<Boolean> first = updateFlushWait(itemId, loadBarrier, null, flushLatch, commitLatch);
      Future<Boolean> second = removeFlushWait(itemId, loadBarrier, preFlushLatch, null, commitLatch);
      awaitOrThrow(flushLatch);

      Map contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      assertEquals(FutureUpdate.class, contents.get(itemId).getClass());

      preFlushLatch.countDown();
      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      boolean removeSucceeded = second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      Object value = contents.get(itemId);
      if (removeSucceeded) {
         assertEquals(Tombstone.class, value.getClass());
         TIME_SERVICE.advance(TOMBSTONE_TIMEOUT + 1);
         assertNull(entityCache.get(itemId)); // force expiration
         contents = Caches.entrySet(entityCache).toMap();
         assertEquals(Collections.EMPTY_MAP, contents);
      } else {
         assertNotNull(value);
         assertEquals(StandardCacheEntryImpl.class, value.getClass());
         TIME_SERVICE.advance(TOMBSTONE_TIMEOUT + 1);
         assertEquals(value, entityCache.get(itemId));
      }
   }

   @Test
   public void testUpdateEvictExpiration() throws Exception {
      CyclicBarrier loadBarrier = new CyclicBarrier(2);
      CountDownLatch preEvictLatch = new CountDownLatch(1);
      CountDownLatch flushLatch = new CountDownLatch(1);
      CountDownLatch commitLatch = new CountDownLatch(1);

      Future<Boolean> first = updateFlushWait(itemId, loadBarrier, null, flushLatch, commitLatch);
      Future<Boolean> second = evictWait(itemId, loadBarrier, preEvictLatch, null);
      awaitOrThrow(flushLatch);

      Map contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      assertEquals(FutureUpdate.class, contents.get(itemId).getClass());

      preEvictLatch.countDown();
      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      contents = Caches.entrySet(entityCache).toMap();
      assertEquals(0, contents.size());
      assertNull(contents.get(itemId));
   }

   @Test
   public void testEvictUpdateExpiration() throws Exception {
      CyclicBarrier loadBarrier = new CyclicBarrier(2);
      CountDownLatch preFlushLatch = new CountDownLatch(1);
      CountDownLatch postEvictLatch = new CountDownLatch(1);
      CountDownLatch flushLatch = new CountDownLatch(1);
      CountDownLatch commitLatch = new CountDownLatch(1);

      Future<Boolean> first = evictWait(itemId, loadBarrier, null, postEvictLatch);
      Future<Boolean> second = updateFlushWait(itemId, loadBarrier, preFlushLatch, flushLatch, commitLatch);
      awaitOrThrow(postEvictLatch);

      Map contents = Caches.entrySet(entityCache).toMap();
      assertEquals(Collections.EMPTY_MAP, contents);
      assertNull(contents.get(itemId));

      preFlushLatch.countDown();
      awaitOrThrow(flushLatch);
      contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      assertEquals(FutureUpdate.class, contents.get(itemId).getClass());

      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      Object value = contents.get(itemId);
      assertNotNull(value);
      assertEquals(StandardCacheEntryImpl.class, value.getClass());
      TIME_SERVICE.advance(TOMBSTONE_TIMEOUT + 1);
      assertEquals(value, entityCache.get(itemId));
   }

   protected Future<Boolean> removeFlushWait(long id, CyclicBarrier loadBarrier, CountDownLatch preFlushLatch, CountDownLatch flushLatch, CountDownLatch commitLatch) throws Exception {
      return EXECUTOR.submit(() -> withTxSessionApply(s -> {
         try {
            Item item = s.load(Item.class, id);
            item.getName(); // force load & putFromLoad before the barrier
            loadBarrier.await(WAIT_TIMEOUT, TimeUnit.SECONDS);
            s.delete(item);
            if (preFlushLatch != null) {
               awaitOrThrow(preFlushLatch);
            }
            s.flush();
         } catch (StaleStateException e) {
            log.info("Exception thrown: ", e);
            markRollbackOnly(s);
            return false;
         } catch (PessimisticLockException e) {
            log.info("Exception thrown: ", e);
            markRollbackOnly(s);
            return false;
         } finally {
            if (flushLatch != null) {
               flushLatch.countDown();
            }
         }
         awaitOrThrow(commitLatch);
         return true;
      }));
   }

   protected Future<Boolean> updateFlushWait(long id, CyclicBarrier loadBarrier, CountDownLatch preFlushLatch, CountDownLatch flushLatch, CountDownLatch commitLatch) throws Exception {
      return EXECUTOR.submit(() -> withTxSessionApply(s -> {
         try {
            Item item = s.load(Item.class, id);
            item.getName(); // force load & putFromLoad before the barrier
            loadBarrier.await(WAIT_TIMEOUT, TimeUnit.SECONDS);
            item.setDescription("Updated item");
            s.update(item);
            if (preFlushLatch != null) {
               awaitOrThrow(preFlushLatch);
            }
            s.flush();
         } catch (StaleStateException e) {
            log.info("Exception thrown: ", e);
            markRollbackOnly(s);
            return false;
         } catch (PessimisticLockException e) {
            log.info("Exception thrown: ", e);
            markRollbackOnly(s);
            return false;
         } finally {
            if (flushLatch != null) {
               flushLatch.countDown();
            }
         }
         awaitOrThrow(commitLatch);
         return true;
      }));
   }

   protected Future<Boolean> evictWait(long id, CyclicBarrier loadBarrier, CountDownLatch preEvictLatch, CountDownLatch postEvictLatch) throws Exception {
      return EXECUTOR.submit(() -> {
         try {
            loadBarrier.await(WAIT_TIMEOUT, TimeUnit.SECONDS);
            if (preEvictLatch != null) {
               awaitOrThrow(preEvictLatch);
            }
            sessionFactory().getCache().evictEntity(Item.class, id);
         } finally {
            if (postEvictLatch != null) {
               postEvictLatch.countDown();
            }
         }
         return true;
      });
   }

   protected void awaitOrThrow(CountDownLatch latch) throws InterruptedException, TimeoutException {
      if (!latch.await(WAIT_TIMEOUT, TimeUnit.SECONDS)) {
         throw new TimeoutException();
      }
   }
}
