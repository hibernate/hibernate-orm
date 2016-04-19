package org.hibernate.test.cache.infinispan.functional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.FutureUpdate;
import org.hibernate.cache.infinispan.util.Tombstone;
import org.hibernate.cache.spi.entry.StandardCacheEntryImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


/**
 * Tests specific to tombstone-based caches
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class TombstoneTest extends AbstractNonInvalidationTest {

   @Override
   public List<Object[]> getParameters() {
      return Arrays.asList(READ_WRITE_REPLICATED, READ_WRITE_DISTRIBUTED);
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

      // afterQuery commit, the tombstone should still be in memory for some time (though, updatable)
      contents = Caches.entrySet(entityCache).toMap();
      assertEquals(1, contents.size());
      assertEquals(Tombstone.class, contents.get(itemId).getClass());

      TIME_SERVICE.advance(timeout + 1);
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
         TIME_SERVICE.advance(timeout + 1);
         assertNull(entityCache.get(itemId));
         contents = Caches.entrySet(entityCache).toMap();
         assertEquals(Collections.EMPTY_MAP, contents);
      } else {
         // DB left only one update to proceed, and the entry should not be expired
         assertNotNull(value);
         assertEquals(StandardCacheEntryImpl.class, value.getClass());
         TIME_SERVICE.advance(timeout + 1);
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

      TIME_SERVICE.advance(timeout + 1);
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
         TIME_SERVICE.advance(timeout + 1);
         assertNull(entityCache.get(itemId)); // force expiration
         contents = Caches.entrySet(entityCache).toMap();
         assertEquals(Collections.EMPTY_MAP, contents);
      } else {
         assertNotNull(value);
         assertEquals(StandardCacheEntryImpl.class, value.getClass());
         TIME_SERVICE.advance(timeout + 1);
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
      TIME_SERVICE.advance(timeout + 1);
      assertEquals(value, entityCache.get(itemId));
   }

}
