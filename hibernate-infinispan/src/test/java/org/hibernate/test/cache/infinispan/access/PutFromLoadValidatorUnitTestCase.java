/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat, Inc or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.cache.infinispan.access;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeJtaTransactionManagerImpl;

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
@Ignore
public class PutFromLoadValidatorUnitTestCase {
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

   private void nakedPutTest(boolean transactional) throws Exception {
      PutFromLoadValidator testee = new PutFromLoadValidator(transactional ? tm : null);
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
   }
   @Test
   public void testRegisteredPut() throws Exception {
      registeredPutTest(false);
   }
   @Test
   public void testRegisteredPutTransactional() throws Exception {
      registeredPutTest(true);
   }

   private void registeredPutTest(boolean transactional) throws Exception {
      PutFromLoadValidator testee = new PutFromLoadValidator(
            transactional ? tm : null);
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

   private void nakedPutAfterRemovalTest(boolean transactional, boolean removeRegion)
         throws Exception {
      PutFromLoadValidator testee = new PutFromLoadValidator(
            transactional ? tm : null);
      if (removeRegion) {
         testee.invalidateRegion();
      } else {
         testee.invalidateKey(KEY1);
      }
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

   private void registeredPutAfterRemovalTest(boolean transactional, boolean removeRegion)
         throws Exception {
      PutFromLoadValidator testee = new PutFromLoadValidator(
            transactional ? tm : null);
      if (removeRegion) {
         testee.invalidateRegion();
      } else {
         testee.invalidateKey(KEY1);
      }
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

   private void registeredPutWithInterveningRemovalTest(boolean transactional, boolean removeRegion)
         throws Exception {
      PutFromLoadValidator testee = new PutFromLoadValidator(
            transactional ? tm : null);
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

   private void delayedNakedPutAfterRemovalTest(boolean transactional, boolean removeRegion)
         throws Exception {
      PutFromLoadValidator testee = new TestValidator(transactional ? tm : null, 100, 1000, 500, 10000);
      if (removeRegion) {
         testee.invalidateRegion();
      } else {
         testee.invalidateKey(KEY1);
      }
      if (transactional) {
         tm.begin();
      }
      Thread.sleep(110);

      boolean lockable = testee.acquirePutFromLoadLock(KEY1);
      try {
         assertTrue(lockable);
      }
      finally {
         if (lockable) {
            testee.releasePutFromLoadLock(KEY1);
         }
      }
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
      final PutFromLoadValidator testee = new PutFromLoadValidator(transactional ? tm : null);

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
                     success.incrementAndGet();
                  }
                  finally {
                     testee.releasePutFromLoadLock(KEY1);
                  }
               }
               finishedLatch.countDown();
            }
            catch (Exception e) {
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

      finishedLatch.await(5, TimeUnit.SECONDS);

      assertEquals("All threads succeeded", 3, success.get());
   }

   /**
    * White box test for ensuring key removals get cleaned up. <b>Note</b>: Since this test is test sensitive, if you
    * add trace logging, it might fail
    *
    * @throws Exception
    */
   @Test
   public void testRemovalCleanup() throws Exception {
      TestValidator testee = new TestValidator(null, 200, 1000, 500, 10000);
      testee.invalidateKey("KEY1");
      testee.invalidateKey("KEY2");
      expectRemovalLenth(2, testee, 3000l);
      assertEquals(2, testee.getRemovalQueueLength());
      expectRemovalLenth(2, testee, 3000l);
      assertEquals(2, testee.getRemovalQueueLength());
      expectRemovalLenth( 2, testee, 3000l );
   }

   private void expectRemovalLenth(int expectedLength, TestValidator testee, long timeout) throws InterruptedException {
      long timeoutMilestone = System.currentTimeMillis() + timeout;
      while ( true ) {
         int queueLength = testee.getRemovalQueueLength();
         if ( queueLength == expectedLength ) {
            //finally it happened
            return;
         }
         else {
            if ( System.currentTimeMillis() > timeoutMilestone ) {
               fail( "condition not reached after " + timeout + " milliseconds. giving up!" );
            }
            Thread.sleep(20);
         }
      }
   }

   /**
    * Very much a white box test of the logic for ensuring pending put registrations get cleaned up.
    *
    * @throws Exception
    */
   @Test
   public void testPendingPutCleanup() throws Exception {
      TestValidator testee = new TestValidator(tm, 5000, 600, 300, 900);

      // Start with a regionRemoval so we can confirm at the end that all
      // registrations have been cleaned out
      testee.invalidateRegion();

      testee.registerPendingPut("1");
      testee.registerPendingPut("2");
      testee.registerPendingPut("3");
      testee.registerPendingPut("4");
      testee.registerPendingPut("5");
      testee.registerPendingPut("6");
      testee.acquirePutFromLoadLock("6");
      testee.releasePutFromLoadLock("6");
      testee.acquirePutFromLoadLock("2");
      testee.releasePutFromLoadLock("2");
      // ppq = [1,2(c),3,4,5,6(c)]
      assertEquals(6, testee.getPendingPutQueueLength());
      assertEquals(0, testee.getOveragePendingPutQueueLength());

      // Sleep past "pendingPutRecentPeriod"
      Thread.sleep(310);
      testee.registerPendingPut("7");
      // White box -- should have cleaned out 2 (completed) but
      // not gotten to 6 (also removed)
      // ppq = [1,3,4,5,6(c),7]
      assertEquals(0, testee.getOveragePendingPutQueueLength());
      assertEquals(6, testee.getPendingPutQueueLength());

      // Sleep past "pendingPutOveragePeriod"
      Thread.sleep(310);
      testee.registerPendingPut("8");
      // White box -- should have cleaned out 6 (completed) and
      // moved 1, 3, 4  and 5 to overage queue
      // oppq = [1,3,4,5] ppq = [7,8]
      assertEquals(4, testee.getOveragePendingPutQueueLength());
      assertEquals(2, testee.getPendingPutQueueLength());

      // Sleep past "maxPendingPutDelay"
      Thread.sleep(310);
      testee.acquirePutFromLoadLock("3");
      testee.releasePutFromLoadLock("3");
      // White box -- should have cleaned out 1 (overage) and
      // moved 7 to overage queue
      // oppq = [3(c),4,5,7] ppq=[8]
      assertEquals(4, testee.getOveragePendingPutQueueLength());
      assertEquals(1, testee.getPendingPutQueueLength());

      // Sleep past "maxPendingPutDelay"
      Thread.sleep(310);
      tm.begin();
      testee.registerPendingPut("7");
      Transaction tx = tm.suspend();

      // White box -- should have cleaned out 3 (completed)
      // and 4 (overage) and moved 8 to overage queue
      // We now have 5,7,8 in overage and 7tx in pending
      // oppq = [5,7,8] ppq=[7tx]
      assertEquals(3, testee.getOveragePendingPutQueueLength());
      assertEquals(1, testee.getPendingPutQueueLength());

      // Validate that only expected items can do puts, thus indirectly
      // proving the others have been cleaned out of pendingPuts map
      boolean locked = testee.acquirePutFromLoadLock("1");
      if (locked) {
         testee.releasePutFromLoadLock("1");
      }
      assertFalse(locked);
      // 5 was overage, so should have been cleaned
      assertEquals(2, testee.getOveragePendingPutQueueLength());
      locked = testee.acquirePutFromLoadLock("2");
      if (locked) {
         testee.releasePutFromLoadLock("1");
      }
      assertFalse(locked);
      // 7 was overage, so should have been cleaned
      assertEquals(1, testee.getOveragePendingPutQueueLength());
      locked = testee.acquirePutFromLoadLock("3");
      if (locked) {
         testee.releasePutFromLoadLock("1");
      }
      assertFalse(locked);
      locked = testee.acquirePutFromLoadLock("4");
      if (locked) {
         testee.releasePutFromLoadLock("1");
      }
      assertFalse(locked);
      locked = testee.acquirePutFromLoadLock("5");
      if (locked) {
         testee.releasePutFromLoadLock("1");
      }
      assertFalse(locked);
      locked = testee.acquirePutFromLoadLock("1");
      if (locked) {
         testee.releasePutFromLoadLock("1");
      }
      assertFalse(testee.acquirePutFromLoadLock("6"));
      locked = testee.acquirePutFromLoadLock("7");
      if (locked) {
         testee.releasePutFromLoadLock("1");
      }
      assertFalse(locked);
      assertTrue(testee.acquirePutFromLoadLock("8"));
      testee.releasePutFromLoadLock("8");
      tm.resume(tx);
      assertTrue(testee.acquirePutFromLoadLock("7"));
      testee.releasePutFromLoadLock("7");
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
      final PutFromLoadValidator testee = new PutFromLoadValidator(null);
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
         invalidateFuture.get(1, TimeUnit.SECONDS);
         fail("invalidateFuture did not block");
      }
      catch (TimeoutException good) {}

      pferLatch.countDown();

      assertTrue(pferFuture.get(5, TimeUnit.SECONDS));
      invalidateFuture.get(5, TimeUnit.SECONDS);

      assertNull(cache.get());

   }

   private static class TestValidator extends PutFromLoadValidator {

      protected TestValidator(TransactionManager transactionManager,
                              long nakedPutInvalidationPeriod, long pendingPutOveragePeriod,
                              long pendingPutRecentPeriod, long maxPendingPutDelay) {
         super(transactionManager, nakedPutInvalidationPeriod, pendingPutOveragePeriod,
               pendingPutRecentPeriod, maxPendingPutDelay);
      }

      @Override
      public int getOveragePendingPutQueueLength() {
         // TODO Auto-generated method stub
         return super.getOveragePendingPutQueueLength();
      }

      @Override
      public int getPendingPutQueueLength() {
         // TODO Auto-generated method stub
         return super.getPendingPutQueueLength();
      }

      @Override
      public int getRemovalQueueLength() {
         // TODO Auto-generated method stub
         return super.getRemovalQueueLength();
      }

   }
}
