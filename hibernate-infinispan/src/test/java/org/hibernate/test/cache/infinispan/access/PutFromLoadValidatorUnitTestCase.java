/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.access;

import javax.transaction.TransactionManager;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeJtaTransactionManagerImpl;
import org.hibernate.test.cache.infinispan.util.InfinispanTestingSetup;
import org.hibernate.testing.TestForIssue;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.CacheManagerCallable;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.infinispan.test.TestingUtil.withCacheManager;
import static org.infinispan.test.TestingUtil.withTx;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.*;

/**
 * Tests of {@link PutFromLoadValidator}.
 *
 * @author Brian Stansberry
 * @author Galder Zamarreño
 * @version $Revision: $
 */
public class PutFromLoadValidatorUnitTestCase {

   private static final Log log = LogFactory.getLog(
         PutFromLoadValidatorUnitTestCase.class);

   @Rule
   public InfinispanTestingSetup infinispanTestIdentifier = new InfinispanTestingSetup();

   private Object KEY1 = "KEY1";

   private TransactionManager tm;

   @Before
   public void setUp() throws Exception {
      tm = DualNodeJtaTransactionManagerImpl.getInstance("test");
   }

   @After
   public void tearDown() throws Exception {
      tm = null;
      try {
         DualNodeJtaTransactionManagerImpl.cleanupTransactions();
      }
      finally {
         DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
      }
   }

   private static EmbeddedCacheManager createCacheManager() {
      EmbeddedCacheManager cacheManager = TestCacheManagerFactory.createCacheManager(false);
      cacheManager.defineConfiguration(InfinispanRegionFactory.PENDING_PUTS_CACHE_NAME,
            InfinispanRegionFactory.PENDING_PUTS_CACHE_CONFIGURATION);
      return cacheManager;
   }

   @Test
   public void testNakedPut() throws Exception {
      nakedPutTest(false);
   }
   @Test
   public void testNakedPutTransactional() throws Exception {
      nakedPutTest(true);
   }

   private void nakedPutTest(final boolean transactional) throws Exception {
      withCacheManager(new CacheManagerCallable(createCacheManager()) {
         @Override
         public void call() {
            PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm);
            exec(transactional, new NakedPut(testee, true));
         }
      });
   }

   @Test
   public void testRegisteredPut() throws Exception {
      registeredPutTest(false);
   }
   @Test
   public void testRegisteredPutTransactional() throws Exception {
      registeredPutTest(true);
   }

   private void registeredPutTest(final boolean transactional) throws Exception {
      withCacheManager(new CacheManagerCallable(createCacheManager()) {
         @Override
         public void call() {
            PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm);
            exec(transactional, new RegularPut(testee));
         }
      });
   }

   @Test
   public void testNakedPutAfterKeyRemoval() throws Exception {
      nakedPutAfterRemovalTest(false, false);
   }
   @Test
   public void testNakedPutAfterKeyRemovalTransactional() throws Exception {
      nakedPutAfterRemovalTest(true, false);
   }
   @Test
   public void testNakedPutAfterRegionRemoval() throws Exception {
      nakedPutAfterRemovalTest(false, true);
   }
   @Test
   public void testNakedPutAfterRegionRemovalTransactional() throws Exception {
      nakedPutAfterRemovalTest(true, true);
   }

   private void nakedPutAfterRemovalTest(final boolean transactional,
         final boolean removeRegion) throws Exception {
      withCacheManager(new CacheManagerCallable(createCacheManager()) {
         @Override
         public void call() {
            PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache());
            Invalidation invalidation = new Invalidation(testee, removeRegion);
            // the naked put can succeed because it has txTimestamp after invalidation
            NakedPut nakedPut = new NakedPut(testee, true);
            exec(transactional, invalidation, nakedPut);
         }
      });

   }

   @Test
   public void testRegisteredPutAfterKeyRemoval() throws Exception {
      registeredPutAfterRemovalTest(false, false);
   }
   @Test
   public void testRegisteredPutAfterKeyRemovalTransactional() throws Exception {
      registeredPutAfterRemovalTest(true, false);
   }
    @Test
   public void testRegisteredPutAfterRegionRemoval() throws Exception {
      registeredPutAfterRemovalTest(false, true);
   }
    @Test
   public void testRegisteredPutAfterRegionRemovalTransactional() throws Exception {
      registeredPutAfterRemovalTest(true, true);
   }

   private void registeredPutAfterRemovalTest(final boolean transactional,
         final boolean removeRegion) throws Exception {
      withCacheManager(new CacheManagerCallable(createCacheManager()) {
         @Override
         public void call() {
            PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm);
            Invalidation invalidation = new Invalidation(testee, removeRegion);
            RegularPut regularPut = new RegularPut(testee);
            exec(transactional, invalidation, regularPut);
         }
      });

   }
    @Test
   public void testRegisteredPutWithInterveningKeyRemoval() throws Exception {
      registeredPutWithInterveningRemovalTest(false, false);
   }
    @Test
   public void testRegisteredPutWithInterveningKeyRemovalTransactional() throws Exception {
      registeredPutWithInterveningRemovalTest(true, false);
   }
    @Test
   public void testRegisteredPutWithInterveningRegionRemoval() throws Exception {
      registeredPutWithInterveningRemovalTest(false, true);
   }
    @Test
   public void testRegisteredPutWithInterveningRegionRemovalTransactional() throws Exception {
      registeredPutWithInterveningRemovalTest(true, true);
   }

   private void registeredPutWithInterveningRemovalTest(
         final boolean transactional, final boolean removeRegion)
         throws Exception {
      withCacheManager(new CacheManagerCallable(createCacheManager()) {
         @Override
         public void call() {
            PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm);
            try {
               long txTimestamp = System.currentTimeMillis();
               if (transactional) {
                  tm.begin();
               }
               SessionImplementor session1 = mock(SessionImplementor.class);
               SessionImplementor session2 = mock(SessionImplementor.class);
               testee.registerPendingPut(session1, KEY1, txTimestamp);
               if (removeRegion) {
                  testee.beginInvalidatingRegion();
               } else {
                  testee.beginInvalidatingKey(session2, KEY1);
               }

               PutFromLoadValidator.Lock lock = testee.acquirePutFromLoadLock(session1, KEY1, txTimestamp);
               try {
                  assertNull(lock);
               }
               finally {
                  if (lock != null) {
                     testee.releasePutFromLoadLock(KEY1, lock);
                  }
                  if (removeRegion) {
                     testee.endInvalidatingRegion();
                  } else {
                     testee.endInvalidatingKey(session2, KEY1);
                  }
               }
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         }
      });
   }

   @Test
   public void testMultipleRegistrations() throws Exception {
      multipleRegistrationtest(false);
   }

   @Test
   public void testMultipleRegistrationsTransactional() throws Exception {
      multipleRegistrationtest(true);
   }

   private void multipleRegistrationtest(final boolean transactional) throws Exception {
      withCacheManager(new CacheManagerCallable(createCacheManager()) {
         @Override
         public void call() {
            final PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm);

            final CountDownLatch registeredLatch = new CountDownLatch(3);
            final CountDownLatch finishedLatch = new CountDownLatch(3);
            final AtomicInteger success = new AtomicInteger();

            Runnable r = new Runnable() {
               public void run() {
                  try {
                     long txTimestamp = System.currentTimeMillis();
                     if (transactional) {
                        tm.begin();
                     }
                     SessionImplementor session = mock (SessionImplementor.class);
                     testee.registerPendingPut(session, KEY1, txTimestamp);
                     registeredLatch.countDown();
                     registeredLatch.await(5, TimeUnit.SECONDS);
                     PutFromLoadValidator.Lock lock = testee.acquirePutFromLoadLock(session, KEY1, txTimestamp);
                     if (lock != null) {
                        try {
                           log.trace("Put from load lock acquired for key = " + KEY1);
                           success.incrementAndGet();
                        } finally {
                           testee.releasePutFromLoadLock(KEY1, lock);
                        }
                     } else {
                        log.trace("Unable to acquired putFromLoad lock for key = " + KEY1);
                     }
                     finishedLatch.countDown();
                  } catch (Exception e) {
                     e.printStackTrace();
                  }
               }
            };

            ExecutorService executor = Executors.newFixedThreadPool(3);

            // Start with a removal so the "isPutValid" calls will fail if
            // any of the concurrent activity isn't handled properly

            testee.beginInvalidatingRegion();
            testee.endInvalidatingRegion();
            try {
               Thread.sleep(10);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }

            // Do the registration + isPutValid calls
            executor.execute(r);
            executor.execute(r);
            executor.execute(r);

            try {
               finishedLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
               throw new RuntimeException(e);
            }

            assertEquals("All threads succeeded", 3, success.get());
         }
      });
   }

   @Test
   public void testInvalidateKeyBlocksForInProgressPut() throws Exception {
      invalidationBlocksForInProgressPutTest(true);
   }

   @Test
   public void testInvalidateRegionBlocksForInProgressPut() throws Exception {
      invalidationBlocksForInProgressPutTest(false);
   }

   private void invalidationBlocksForInProgressPutTest(final boolean keyOnly) throws Exception {
      withCacheManager(new CacheManagerCallable(createCacheManager()) {
         @Override
         public void call() {
            final PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm);
            final CountDownLatch removeLatch = new CountDownLatch(1);
            final CountDownLatch pferLatch = new CountDownLatch(1);
            final AtomicReference<Object> cache = new AtomicReference<Object>("INITIAL");

            Callable<Boolean> pferCallable = new Callable<Boolean>() {
               public Boolean call() throws Exception {
                  long txTimestamp = System.currentTimeMillis();
                  SessionImplementor session = mock (SessionImplementor.class);
                  testee.registerPendingPut(session, KEY1, txTimestamp);
                  PutFromLoadValidator.Lock lock = testee.acquirePutFromLoadLock(session, KEY1, txTimestamp);
                  if (lock != null) {
                     try {
                        removeLatch.countDown();
                        pferLatch.await();
                        cache.set("PFER");
                        return Boolean.TRUE;
                     }
                     finally {
                        testee.releasePutFromLoadLock(KEY1, lock);
                     }
                  }
                  return Boolean.FALSE;
               }
            };

            Callable<Void> invalidateCallable = new Callable<Void>() {
               public Void call() throws Exception {
                  removeLatch.await();
                  if (keyOnly) {
                     SessionImplementor session = mock (SessionImplementor.class);
                     testee.beginInvalidatingKey(session, KEY1);
                  } else {
                     testee.beginInvalidatingRegion();
                  }
                  cache.set(null);
                  return null;
               }
            };

            ExecutorService executorService = Executors.newCachedThreadPool();
            Future<Boolean> pferFuture = executorService.submit(pferCallable);
            Future<Void> invalidateFuture = executorService.submit(invalidateCallable);

            try {
               try {
                  invalidateFuture.get(1, TimeUnit.SECONDS);
                  fail("invalidateFuture did not block");
               }
               catch (TimeoutException good) {}

               pferLatch.countDown();

               assertTrue(pferFuture.get(5, TimeUnit.SECONDS));
               invalidateFuture.get(5, TimeUnit.SECONDS);

               assertNull(cache.get());
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         }
      });
   }

   protected void exec(boolean transactional, Callable<?>... callables) {
      try {
         if (transactional) {
            for (Callable<?> c : callables) {
               withTx(tm, c);
            }
         } else {
            for (Callable<?> c : callables) {
               c.call();
            }
         }
      } catch (RuntimeException e) {
         throw e;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private class Invalidation implements Callable<Void> {
      private PutFromLoadValidator putFromLoadValidator;
      private boolean removeRegion;

      public Invalidation(PutFromLoadValidator putFromLoadValidator, boolean removeRegion) {
         this.putFromLoadValidator = putFromLoadValidator;
         this.removeRegion = removeRegion;
      }

      @Override
      public Void call() throws Exception {
         if (removeRegion) {
            boolean success = putFromLoadValidator.beginInvalidatingRegion();
            assertTrue(success);
            putFromLoadValidator.endInvalidatingRegion();;
         } else {
            SessionImplementor session = mock (SessionImplementor.class);
            boolean success = putFromLoadValidator.beginInvalidatingKey(session, KEY1);
            assertTrue(success);
            success = putFromLoadValidator.endInvalidatingKey(session, KEY1);
            assertTrue(success);
         }
         // if we go for the timestamp-based approach, invalidation in the same millisecond
         // as the registerPendingPut/acquirePutFromLoad lock results in failure.
         Thread.sleep(10);
         return null;
      }
   }

   private class RegularPut implements Callable<Void> {
      private PutFromLoadValidator putFromLoadValidator;

      public RegularPut(PutFromLoadValidator putFromLoadValidator) {
         this.putFromLoadValidator = putFromLoadValidator;
      }

      @Override
      public Void call() throws Exception {
         try {
            long txTimestamp = System.currentTimeMillis(); // this should be acquired before UserTransaction.begin()
            SessionImplementor session = mock (SessionImplementor.class);
            putFromLoadValidator.registerPendingPut(session, KEY1, txTimestamp);

            PutFromLoadValidator.Lock lock = putFromLoadValidator.acquirePutFromLoadLock(session, KEY1, txTimestamp);
            try {
               assertNotNull(lock);
            } finally {
               if (lock != null) {
                  putFromLoadValidator.releasePutFromLoadLock(KEY1, lock);
               }
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
         return null;
      }
   }

   private class NakedPut implements Callable<Void> {
      private final PutFromLoadValidator testee;
      private final boolean expectSuccess;

      public NakedPut(PutFromLoadValidator testee, boolean expectSuccess) {
         this.testee = testee;
         this.expectSuccess = expectSuccess;
      }

      @Override
      public Void call() throws Exception {
         try {
            long txTimestamp = System.currentTimeMillis(); // this should be acquired before UserTransaction.begin()
            SessionImplementor session = mock (SessionImplementor.class);
            PutFromLoadValidator.Lock lock = testee.acquirePutFromLoadLock(session, KEY1, txTimestamp);
            try {
               if (expectSuccess) {
                  assertNotNull(lock);
               } else {
                  assertNull(lock);
               }
            }
            finally {
               if (lock != null) {
                  testee.releasePutFromLoadLock(KEY1, lock);
               }
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
         return null;
      }
   }

   @Test
   @TestForIssue(jiraKey = "HHH-9928")
   public void testGetForNullReleasePuts() {
      EmbeddedCacheManager cm = TestCacheManagerFactory.createCacheManager(false);
      ConfigurationBuilder cb = new ConfigurationBuilder().read(InfinispanRegionFactory.PENDING_PUTS_CACHE_CONFIGURATION);
      cb.expiration().maxIdle(500);
      cm.defineConfiguration(InfinispanRegionFactory.PENDING_PUTS_CACHE_NAME, cb.build());
      withCacheManager(new CacheManagerCallable(cm) {
         @Override
         public void call() {
            PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm);
            long lastInsert = Long.MAX_VALUE;
            for (int i = 0; i < 100; ++i) {
               lastInsert = System.currentTimeMillis();
               try {
                  withTx(tm, new Callable<Object>() {
                     @Override
                     public Object call() throws Exception {
                        SessionImplementor session = mock (SessionImplementor.class);
                        testee.registerPendingPut(session, KEY1, 0);
                        return null;
                     }
                  });
                  Thread.sleep(10);
               } catch (Exception e) {
                  throw new RuntimeException(e);
               }
            }
            String ppName = cm.getCache().getName() + "-" + InfinispanRegionFactory.PENDING_PUTS_CACHE_NAME;
            Map ppCache = cm.getCache(ppName, false);
            assertNotNull(ppCache);
            Object pendingPutMap = ppCache.get(KEY1);
            long end = System.currentTimeMillis();
            if (end - lastInsert > 500) {
               log.warn("Test took too long");
               return;
            }
            assertNotNull(pendingPutMap);
            int size;
            try {
               Method sizeMethod = pendingPutMap.getClass().getMethod("size");
               sizeMethod.setAccessible(true);
               size = (Integer) sizeMethod.invoke(pendingPutMap);
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
            // some of the pending puts need to be expired by now
            assertTrue(size < 100);
            // but some are still registered
            assertTrue(size > 0);
         }
      });
   }
}
