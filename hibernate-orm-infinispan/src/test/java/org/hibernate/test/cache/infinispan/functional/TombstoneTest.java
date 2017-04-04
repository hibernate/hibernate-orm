package org.hibernate.test.cache.infinispan.functional;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hibernate.cache.infinispan.util.Tombstone;

import org.hibernate.test.cache.infinispan.functional.entities.Item;
import org.hibernate.testing.TestForIssue;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.distribution.BlockingInterceptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


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

      // Second remove fails due to being unable to lock entry *before* writing the tombstone
      assertTombstone(1);

      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      // after commit, the tombstone should still be in memory for some time (though, updatable)
      assertTombstone(1);

      TIME_SERVICE.advance(timeout + 1);
      assertEmptyCache();
   }

   @Test
   public void testTwoUpdates1() throws Exception {
      CyclicBarrier loadBarrier = new CyclicBarrier(2);
      CountDownLatch preFlushLatch = new CountDownLatch(1);
      CountDownLatch flushLatch1 = new CountDownLatch(1);
      CountDownLatch flushLatch2 = new CountDownLatch(1);
      CountDownLatch commitLatch1 = new CountDownLatch(1);
      CountDownLatch commitLatch2 = new CountDownLatch(1);

      // Note: this is a single node case, we don't have to deal with async replication
      Future<Boolean> update1 = updateFlushWait(itemId, loadBarrier, null, flushLatch1, commitLatch1);
      Future<Boolean> update2 = updateFlushWait(itemId, loadBarrier, preFlushLatch, flushLatch2, commitLatch2);

      awaitOrThrow(flushLatch1);
      assertTombstone(1);

      preFlushLatch.countDown();
      awaitOrThrow(flushLatch2);

      // Second update fails due to being unable to lock entry *before* writing the tombstone
      assertTombstone(1);

      commitLatch2.countDown();
      assertFalse(update2.get(WAIT_TIMEOUT, TimeUnit.SECONDS));
      assertTombstone(1);

      commitLatch1.countDown();
      assertTrue(update1.get(WAIT_TIMEOUT, TimeUnit.SECONDS));
      assertSingleCacheEntry();
   }

   @Test
   public void testTwoUpdates2() throws Exception {
      CyclicBarrier loadBarrier = new CyclicBarrier(2);
      CountDownLatch preFlushLatch = new CountDownLatch(1);
      CountDownLatch flushLatch1 = new CountDownLatch(1);
      CountDownLatch flushLatch2 = new CountDownLatch(1);
      CountDownLatch commitLatch1 = new CountDownLatch(1);
      CountDownLatch commitLatch2 = new CountDownLatch(1);

      // Note: this is a single node case, we don't have to deal with async replication
      Future<Boolean> update1 = updateFlushWait(itemId, loadBarrier, null, flushLatch1, commitLatch1);
      Future<Boolean> update2 = updateFlushWait(itemId, loadBarrier, preFlushLatch, flushLatch2, commitLatch2);

      awaitOrThrow(flushLatch1);
      assertCacheContains(Tombstone.class);

      preFlushLatch.countDown();
      awaitOrThrow(flushLatch2);

      // Second update fails due to being unable to lock entry *before* writing the tombstone
      assertTombstone(1);

      commitLatch1.countDown();
      assertTrue(update1.get(WAIT_TIMEOUT, TimeUnit.SECONDS));
      assertSingleCacheEntry();

      commitLatch2.countDown();
      assertFalse(update2.get(WAIT_TIMEOUT, TimeUnit.SECONDS));
      assertSingleCacheEntry();

      TIME_SERVICE.advance(TIMEOUT + 1);
      assertSingleCacheEntry();
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

      // Second update fails due to being unable to lock entry *before* writing the tombstone
      assertTombstone(1);

      preFlushLatch.countDown();
      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      assertTombstone(1);

      TIME_SERVICE.advance(timeout + 1);
      assertEmptyCache();
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

      assertTombstone(1);

      preFlushLatch.countDown();
      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      boolean removeSucceeded = second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      if (removeSucceeded) {
         assertCacheContains(Tombstone.class);
         TIME_SERVICE.advance(timeout + 1);
         assertEmptyCache();
      } else {
         assertSingleCacheEntry();
         TIME_SERVICE.advance(timeout + 1);
         assertSingleCacheEntry();
      }
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

      assertTombstone(1);

      preEvictLatch.countDown();
      awaitOrThrow(postEvictLatch);
      assertTombstone(1);

      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      assertSingleCacheEntry();

      TIME_SERVICE.advance(timeout + 1);

      assertSingleCacheEntry();
   }

   @Test
   public void testEvictUpdate() throws Exception {
      CyclicBarrier loadBarrier = new CyclicBarrier(2);
      CountDownLatch preFlushLatch = new CountDownLatch(1);
      CountDownLatch postEvictLatch = new CountDownLatch(1);
      CountDownLatch flushLatch = new CountDownLatch(1);
      CountDownLatch commitLatch = new CountDownLatch(1);

      Future<Boolean> first = evictWait(itemId, loadBarrier, null, postEvictLatch);
      Future<Boolean> second = updateFlushWait(itemId, loadBarrier, preFlushLatch, flushLatch, commitLatch);
      awaitOrThrow(postEvictLatch);

      assertEmptyCache();

      preFlushLatch.countDown();
      awaitOrThrow(flushLatch);
      // The tombstone from update has overwritten the eviction tombstone as it has timestamp = now + 60s
      assertTombstone(1);

      commitLatch.countDown();
      first.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      second.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      // Since evict was executed during the update, we cannot insert the entry into cache
      assertSingleCacheEntry();

      TIME_SERVICE.advance(timeout + 1);
      assertSingleCacheEntry();
   }

   @Test
   public void testEvictUpdate2() throws Exception {
      CountDownLatch flushLatch = new CountDownLatch(1);
      CountDownLatch commitLatch = new CountDownLatch(1);

      sessionFactory().getCache().evictEntity(Item.class, itemId);
      // When the cache was empty, the tombstone is not stored
      assertEmptyCache();

      TIME_SERVICE.advance(1);
      Future<Boolean> update = updateFlushWait(itemId, null, null, flushLatch, commitLatch);
      awaitOrThrow(flushLatch);
      assertTombstone(1);

      commitLatch.countDown();
      update.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      assertSingleCacheEntry();

      TIME_SERVICE.advance(timeout + 2);
      assertSingleCacheEntry();
   }

   @Test
   public void testEvictPutFromLoad() throws Exception {
      sessionFactory().getCache().evictEntity(Item.class, itemId);
      assertEmptyCache();

      TIME_SERVICE.advance(1);
      assertItemDescription("Original item");
      assertSingleCacheEntry();

      TIME_SERVICE.advance(timeout + 2);
      assertSingleCacheEntry();
   }

   protected void assertItemDescription(String expected) throws Exception {
      assertEquals(expected, withTxSessionApply(s -> s.load(Item.class, itemId).getDescription()));
   }

   @Test
   public void testPutFromLoadDuringUpdate() throws Exception {
      CountDownLatch flushLatch = new CountDownLatch(1);
      CountDownLatch commitLatch = new CountDownLatch(1);
      CyclicBarrier putFromLoadBarrier = new CyclicBarrier(2);

      // We cannot just do load during update because that could be blocked in DB
      Future<?> putFromLoad = blockedPutFromLoad(putFromLoadBarrier);

      Future<Boolean> update = updateFlushWait(itemId, null, null, flushLatch, commitLatch);
      awaitOrThrow(flushLatch);
      assertTombstone(1);

      unblockPutFromLoad(putFromLoadBarrier, putFromLoad);

      commitLatch.countDown();
      update.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
      assertSingleCacheEntry();
      assertItemDescription("Updated item");
   }

   @TestForIssue(jiraKey = "HHH-11323")
   @Test
   public void testEvictPutFromLoadDuringUpdate() throws Exception {
      CountDownLatch flushLatch = new CountDownLatch(1);
      CountDownLatch commitLatch = new CountDownLatch(1);
      CyclicBarrier putFromLoadBarrier = new CyclicBarrier(2);

      Future<?> putFromLoad = blockedPutFromLoad(putFromLoadBarrier);

      Future<Boolean> update = updateFlushWait(itemId, null, null, flushLatch, commitLatch);
      // Flush stores FutureUpdate(timestamp, null)
      awaitOrThrow(flushLatch);

      sessionFactory().getCache().evictEntity(Item.class, itemId);

      commitLatch.countDown();
      update.get(WAIT_TIMEOUT, TimeUnit.SECONDS);

      unblockPutFromLoad(putFromLoadBarrier, putFromLoad);

      assertItemDescription("Updated item");
   }

   private Future<?> blockedPutFromLoad(CyclicBarrier putFromLoadBarrier) throws InterruptedException, BrokenBarrierException, TimeoutException {
      BlockingInterceptor blockingInterceptor = new BlockingInterceptor(putFromLoadBarrier, PutKeyValueCommand.class, false, true);
      entityCache.addInterceptor(blockingInterceptor, 0);
      cleanup.add(() -> entityCache.removeInterceptor(BlockingInterceptor.class));
      // the putFromLoad should be blocked in the interceptor
      Future<?> putFromLoad = executor.submit(() -> withTxSessionApply(s -> {
         assertEquals("Original item", s.load(Item.class, itemId).getDescription());
         return null;
      }));
      putFromLoadBarrier.await(WAIT_TIMEOUT, TimeUnit.SECONDS);
      blockingInterceptor.suspend(true);
      return putFromLoad;
   }

   private void unblockPutFromLoad(CyclicBarrier putFromLoadBarrier, Future<?> putFromLoad) throws InterruptedException, BrokenBarrierException, TimeoutException, java.util.concurrent.ExecutionException {
      putFromLoadBarrier.await(WAIT_TIMEOUT, TimeUnit.SECONDS);
      putFromLoad.get(WAIT_TIMEOUT, TimeUnit.SECONDS);
   }

   private void assertTombstone(int expectedSize) {
      Tombstone tombstone = assertCacheContains(Tombstone.class);
      assertEquals("Tombstone is " + tombstone, expectedSize, tombstone.size());
   }
}
