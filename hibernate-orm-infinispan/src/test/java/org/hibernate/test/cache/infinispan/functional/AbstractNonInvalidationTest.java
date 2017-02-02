package org.hibernate.test.cache.infinispan.functional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.OptimisticLockException;
import javax.persistence.PessimisticLockException;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.cache.spi.Region;

import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.test.cache.infinispan.functional.entities.Item;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.test.cache.infinispan.util.TestTimeService;
import org.junit.After;
import org.junit.Before;

import org.infinispan.AdvancedCache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Common base for TombstoneTest and VersionedTest
 *
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public abstract class AbstractNonInvalidationTest extends SingleNodeTest {
   protected static final int WAIT_TIMEOUT = 2000;
   protected static final TestTimeService TIME_SERVICE = new TestTimeService();

   protected long TIMEOUT;
   protected ExecutorService executor;
   protected InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(getClass());
   protected AdvancedCache entityCache;
   protected long itemId;
   protected Region region;
   protected long timeout;
   protected final List<Runnable> cleanup = new ArrayList<>();

   @BeforeClassOnce
   public void setup() {
      executor = Executors.newCachedThreadPool(new ThreadFactory() {
         AtomicInteger counter = new AtomicInteger();

         @Override
         public Thread newThread(Runnable r) {
            return new Thread(r, "Executor-" +  counter.incrementAndGet());
         }
      });
   }

   @AfterClassOnce
   public void shutdown() {
      executor.shutdown();
   }

   @Override
   protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
      // This applies to manually set LOCK_TIMEOUT for H2 DB. AvailableSettings.JPA_LOCK_TIMEOUT
      // works only for queries, not for CRUDs, so we have to modify the connection URL.
      // Alternative could be executing SET LOCK_TIMEOUT 100 as a native query.
      String url = (String) ssrb.getSettings().get(AvailableSettings.URL);
      if (url != null && url.contains("LOCK_TIMEOUT")) {
         url = url.replaceAll("LOCK_TIMEOUT=[^;]*", "LOCK_TIMEOUT=100");
      }
      ssrb.applySetting(AvailableSettings.URL, url);
   }

   @Override
   protected void startUp() {
      super.startUp();
      InfinispanRegionFactory regionFactory = (InfinispanRegionFactory) sessionFactory().getSettings().getRegionFactory();
      TIMEOUT = regionFactory.getPendingPutsCacheConfiguration().expiration().maxIdle();
      region = sessionFactory().getSecondLevelCacheRegion(Item.class.getName());
      entityCache = ((EntityRegionImpl) region).getCache();
   }

   @Before
   public void insertAndClearCache() throws Exception {
      region = sessionFactory().getSecondLevelCacheRegion(Item.class.getName());
      entityCache = ((EntityRegionImpl) region).getCache();
      timeout = ((EntityRegionImpl) region).getRegionFactory().getPendingPutsCacheConfiguration().expiration().maxIdle();
      Item item = new Item("my item", "Original item");
      withTxSession(s -> s.persist(item));
      entityCache.clear();
      assertEquals("Cache is not empty", Collections.EMPTY_SET, entityCache.keySet());
      itemId = item.getId();
      log.info("Insert and clear finished");
   }

   @After
   public void cleanup() throws Exception {
      cleanup.forEach(Runnable::run);
      cleanup.clear();
      withTxSession(s -> {
         s.createQuery("delete from Item").executeUpdate();
      });
   }

   protected Future<Boolean> removeFlushWait(long id, CyclicBarrier loadBarrier, CountDownLatch preFlushLatch, CountDownLatch flushLatch, CountDownLatch commitLatch) throws Exception {
      return executor.submit(() -> withTxSessionApply(s -> {
         try {
            Item item = s.load(Item.class, id);
            item.getName(); // force load & putFromLoad before the barrier
            loadBarrier.await(WAIT_TIMEOUT, TimeUnit.SECONDS);
            s.delete(item);
            if (preFlushLatch != null) {
               awaitOrThrow(preFlushLatch);
            }
            s.flush();
         } catch (OptimisticLockException e) {
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
      return executor.submit(() -> withTxSessionApply(s -> {
         try {
            Item item = s.load(Item.class, id);
            item.getName(); // force load & putFromLoad before the barrier
            if (loadBarrier != null) {
               loadBarrier.await(WAIT_TIMEOUT, TimeUnit.SECONDS);
            }
            item.setDescription("Updated item");
            s.update(item);
            if (preFlushLatch != null) {
               awaitOrThrow(preFlushLatch);
            }
            s.flush();
         } catch (OptimisticLockException e) {
            log.info("Exception thrown: ", e);
            markRollbackOnly(s);
            return false;
         } catch (PessimisticLockException | org.hibernate.PessimisticLockException e) {
            log.info("Exception thrown: ", e);
            markRollbackOnly(s);
            return false;
         } finally {
            if (flushLatch != null) {
               flushLatch.countDown();
            }
         }
         if (commitLatch != null) {
            awaitOrThrow(commitLatch);
         }
         return true;
      }));
   }

   protected Future<Boolean> evictWait(long id, CyclicBarrier loadBarrier, CountDownLatch preEvictLatch, CountDownLatch postEvictLatch) throws Exception {
      return executor.submit(() -> {
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

   @Override
   protected void addSettings(Map settings) {
      super.addSettings(settings);
      settings.put(TestInfinispanRegionFactory.TIME_SERVICE, TIME_SERVICE);
   }

   protected void assertEmptyCache() {
      assertNull(entityCache.get(itemId)); // force expiration
      Map contents = Caches.entrySet(entityCache).toMap();
      assertEquals(Collections.EMPTY_MAP, contents);
   }

   protected <T> T assertCacheContains(Class<T> expected) {
      Map contents = Caches.entrySet(entityCache).toMap();
      assertEquals("Cache does not have single element", 1, contents.size());
      Object value = contents.get(itemId);
      assertTrue(String.valueOf(value), expected.isInstance(value));
      return (T) value;
   }

   protected Object assertSingleCacheEntry() {
      return assertCacheContains(CacheEntry.class);
   }
}
