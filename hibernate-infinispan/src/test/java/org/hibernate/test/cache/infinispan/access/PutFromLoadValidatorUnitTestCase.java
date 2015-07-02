/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.access;

import javax.transaction.TransactionManager;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeJtaTransactionManagerImpl;
import org.infinispan.AdvancedCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.CacheManagerCallable;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.infinispan.test.TestingUtil.withCacheManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

   @Test
   public void testNakedPut() throws Exception {
      nakedPutTest(false);
   }
   @Test
   public void testNakedPutTransactional() throws Exception {
      nakedPutTest(true);
   }

   private void nakedPutTest(final boolean transactional) throws Exception {
      withCacheManager(new CacheManagerCallable(
            TestCacheManagerFactory.createCacheManager(false)) {
         @Override
         public void call() {
            try {
               PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm,
                     transactional ? tm : null,
                     PutFromLoadValidator.NAKED_PUT_INVALIDATION_PERIOD);
               if (transactional) {
                  tm.begin();
               }
               boolean lockable = testee.acquirePutFromLoadLock(KEY1);
               try {
                  assertTrue(lockable);
               }
               finally {
                  if (lockable) {
                     testee.releasePutFromLoadLock(KEY1);
                  }
               }
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
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
      withCacheManager(new CacheManagerCallable(
            TestCacheManagerFactory.createCacheManager(false)) {
         @Override
         public void call() {
            PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm,
                  transactional ? tm : null,
                  PutFromLoadValidator.NAKED_PUT_INVALIDATION_PERIOD);
            try {
               if (transactional) {
                  tm.begin();
               }
               testee.registerPendingPut(KEY1);

               boolean lockable = testee.acquirePutFromLoadLock(KEY1);
               try {
                  assertTrue(lockable);
               }
               finally {
                  if (lockable) {
                     testee.releasePutFromLoadLock(KEY1);
                  }
               }
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
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
      withCacheManager(new CacheManagerCallable(
            TestCacheManagerFactory.createCacheManager(false)) {
         @Override
         public void call() {
            PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm,
                  transactional ? tm : null,
                  PutFromLoadValidator.NAKED_PUT_INVALIDATION_PERIOD);
            if (removeRegion) {
               testee.invalidateRegion();
            } else {
               testee.invalidateKey(KEY1);
            }
            try {
               if (transactional) {
                  tm.begin();
               }

               boolean lockable = testee.acquirePutFromLoadLock(KEY1);
               try {
                  assertFalse(lockable);
               }
               finally {
                  if (lockable) {
                     testee.releasePutFromLoadLock(KEY1);
                  }
               }
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
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
      withCacheManager(new CacheManagerCallable(
            TestCacheManagerFactory.createCacheManager(false)) {
         @Override
         public void call() {
            PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm,
                  transactional ? tm : null,
                  PutFromLoadValidator.NAKED_PUT_INVALIDATION_PERIOD);
            if (removeRegion) {
               testee.invalidateRegion();
            } else {
               testee.invalidateKey(KEY1);
            }
            try {
               if (transactional) {
                  tm.begin();
               }
               testee.registerPendingPut(KEY1);

               boolean lockable = testee.acquirePutFromLoadLock(KEY1);
               try {
                  assertTrue(lockable);
               }
               finally {
                  if (lockable) {
                     testee.releasePutFromLoadLock(KEY1);
                  }
               }
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
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
      withCacheManager(new CacheManagerCallable(
            TestCacheManagerFactory.createCacheManager(false)) {
         @Override
         public void call() {
            PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm,
                  transactional ? tm : null,
                  PutFromLoadValidator.NAKED_PUT_INVALIDATION_PERIOD);
            try {
               if (transactional) {
                  tm.begin();
               }
               testee.registerPendingPut(KEY1);
               if (removeRegion) {
                  testee.invalidateRegion();
               } else {
                  testee.invalidateKey(KEY1);
               }

               boolean lockable = testee.acquirePutFromLoadLock(KEY1);
               try {
                  assertFalse(lockable);
               }
               finally {
                  if (lockable) {
                     testee.releasePutFromLoadLock(KEY1);
                  }
               }
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         }
      });
   }
   @Test
   public void testDelayedNakedPutAfterKeyRemoval() throws Exception {
      delayedNakedPutAfterRemovalTest(false, false);
   }
   @Test
   public void testDelayedNakedPutAfterKeyRemovalTransactional() throws Exception {
      delayedNakedPutAfterRemovalTest(true, false);
   }
    @Test
   public void testDelayedNakedPutAfterRegionRemoval() throws Exception {
      delayedNakedPutAfterRemovalTest(false, true);
   }
   @Test
   public void testDelayedNakedPutAfterRegionRemovalTransactional() throws Exception {
      delayedNakedPutAfterRemovalTest(true, true);
   }

   private void delayedNakedPutAfterRemovalTest(
         final boolean transactional, final boolean removeRegion)
         throws Exception {
      withCacheManager(new CacheManagerCallable(
            TestCacheManagerFactory.createCacheManager(false)) {
         @Override
         public void call() {
            PutFromLoadValidator testee = new TestValidator(cm.getCache().getAdvancedCache(), cm,
                  transactional ? tm : null, 100);
            if (removeRegion) {
               testee.invalidateRegion();
            } else {
               testee.invalidateKey(KEY1);
            }
            try {
               if (transactional) {
                  tm.begin();
               }
               Thread.sleep(110);

               boolean lockable = testee.acquirePutFromLoadLock(KEY1);
               try {
                  assertTrue(lockable);
               } finally {
                  if (lockable) {
                     testee.releasePutFromLoadLock(KEY1);
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
      withCacheManager(new CacheManagerCallable(
            TestCacheManagerFactory.createCacheManager(false)) {
         @Override
         public void call() {
            final PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(), cm,
                  transactional ? tm : null,
                  PutFromLoadValidator.NAKED_PUT_INVALIDATION_PERIOD);

            final CountDownLatch registeredLatch = new CountDownLatch(3);
            final CountDownLatch finishedLatch = new CountDownLatch(3);
            final AtomicInteger success = new AtomicInteger();

            Runnable r = new Runnable() {
               public void run() {
                  try {
                     if (transactional) {
                        tm.begin();
                     }
                     testee.registerPendingPut(KEY1);
                     registeredLatch.countDown();
                     registeredLatch.await(5, TimeUnit.SECONDS);
                     if (testee.acquirePutFromLoadLock(KEY1)) {
                        try {
                           log.trace("Put from load lock acquired for key = " + KEY1);
                           success.incrementAndGet();
                        } finally {
                           testee.releasePutFromLoadLock(KEY1);
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

            testee.invalidateRegion();

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
      withCacheManager(new CacheManagerCallable(
            TestCacheManagerFactory.createCacheManager(false)) {
         @Override
         public void call() {
            final PutFromLoadValidator testee = new PutFromLoadValidator(cm.getCache().getAdvancedCache(),
                    cm, null, PutFromLoadValidator.NAKED_PUT_INVALIDATION_PERIOD);
            final CountDownLatch removeLatch = new CountDownLatch(1);
            final CountDownLatch pferLatch = new CountDownLatch(1);
            final AtomicReference<Object> cache = new AtomicReference<Object>("INITIAL");

            Callable<Boolean> pferCallable = new Callable<Boolean>() {
               public Boolean call() throws Exception {
                  testee.registerPendingPut(KEY1);
                  if (testee.acquirePutFromLoadLock(KEY1)) {
                     try {
                        removeLatch.countDown();
                        pferLatch.await();
                        cache.set("PFER");
                        return Boolean.TRUE;
                     }
                     finally {
                        testee.releasePutFromLoadLock(KEY1);
                     }
                  }
                  return Boolean.FALSE;
               }
            };

            Callable<Void> invalidateCallable = new Callable<Void>() {
               public Void call() throws Exception {
                  removeLatch.await();
                  if (keyOnly) {
                     testee.invalidateKey(KEY1);
                  } else {
                     testee.invalidateRegion();
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

   private static class TestValidator extends PutFromLoadValidator {

      protected TestValidator(AdvancedCache cache, EmbeddedCacheManager cm,
            TransactionManager transactionManager,
            long nakedPutInvalidationPeriod) {
         super(cache, cm, transactionManager, nakedPutInvalidationPeriod);
      }

      @Override
      public int getRemovalQueueLength() {
         return super.getRemovalQueueLength();
      }

   }
}
