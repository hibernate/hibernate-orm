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
package org.hibernate.test.cache.jbc.access;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import junit.framework.TestCase;

import org.hibernate.cache.jbc.access.PutFromLoadValidator;
import org.hibernate.test.cache.jbc.functional.util.DualNodeJtaTransactionManagerImpl;

/**
 * Tests of {@link PutFromLoadValidator}.
 * 
 * @author Brian Stansberry
 * 
 * @version $Revision: $
 */
public class PutFromLoadValidatorUnitTestCase extends TestCase {
	private Object KEY1 = "KEY1";

	private TransactionManager tm;

	public PutFromLoadValidatorUnitTestCase(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		tm = DualNodeJtaTransactionManagerImpl.getInstance("test");
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			super.tearDown();
		} finally {
			tm = null;
			try {
				DualNodeJtaTransactionManagerImpl.cleanupTransactions();
			} finally {
				DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
			}
		}
	}

	public void testNakedPut() throws Exception {
		nakedPutTest(false);
	}

	public void testNakedPutTransactional() throws Exception {
		nakedPutTest(true);
	}

	private void nakedPutTest(boolean transactional) throws Exception {
		PutFromLoadValidator testee = new PutFromLoadValidator(
				transactional ? tm : null);
		if (transactional) {
			tm.begin();
		}
		assertTrue(testee.isPutValid(KEY1));
	}

	public void testRegisteredPut() throws Exception {
		registeredPutTest(false);
	}

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
		assertTrue(testee.isPutValid(KEY1));
	}

	public void testNakedPutAfterKeyRemoval() throws Exception {
		nakedPutAfterRemovalTest(false, false);
	}

	public void testNakedPutAfterKeyRemovalTransactional() throws Exception {
		nakedPutAfterRemovalTest(true, false);
	}

	public void testNakedPutAfterRegionRemoval() throws Exception {
		nakedPutAfterRemovalTest(false, true);
	}

	public void testNakedPutAfterRegionRemovalTransactional() throws Exception {
		nakedPutAfterRemovalTest(true, true);
	}

	private void nakedPutAfterRemovalTest(boolean transactional,
			boolean removeRegion) throws Exception {
		PutFromLoadValidator testee = new PutFromLoadValidator(
				transactional ? tm : null);
		if (removeRegion) {
			testee.regionRemoved();
		} else {
			testee.keyRemoved(KEY1);
		}
		if (transactional) {
			tm.begin();
		}
		assertFalse(testee.isPutValid(KEY1));

	}

	public void testRegisteredPutAfterKeyRemoval() throws Exception {
		registeredPutAfterRemovalTest(false, false);
	}

	public void testRegisteredPutAfterKeyRemovalTransactional()
			throws Exception {
		registeredPutAfterRemovalTest(true, false);
	}

	public void testRegisteredPutAfterRegionRemoval() throws Exception {
		registeredPutAfterRemovalTest(false, true);
	}

	public void testRegisteredPutAfterRegionRemovalTransactional()
			throws Exception {
		registeredPutAfterRemovalTest(true, true);
	}

	private void registeredPutAfterRemovalTest(boolean transactional,
			boolean removeRegion) throws Exception {
		PutFromLoadValidator testee = new PutFromLoadValidator(
				transactional ? tm : null);
		if (removeRegion) {
			testee.regionRemoved();
		} else {
			testee.keyRemoved(KEY1);
		}
		if (transactional) {
			tm.begin();
		}
		testee.registerPendingPut(KEY1);
		assertTrue(testee.isPutValid(KEY1));
	}

	public void testRegisteredPutWithInterveningKeyRemoval() throws Exception {
		registeredPutWithInterveningRemovalTest(false, false);
	}

	public void testRegisteredPutWithInterveningKeyRemovalTransactional()
			throws Exception {
		registeredPutWithInterveningRemovalTest(true, false);
	}

	public void testRegisteredPutWithInterveningRegionRemoval()
			throws Exception {
		registeredPutWithInterveningRemovalTest(false, true);
	}

	public void testRegisteredPutWithInterveningRegionRemovalTransactional()
			throws Exception {
		registeredPutWithInterveningRemovalTest(true, true);
	}

	private void registeredPutWithInterveningRemovalTest(boolean transactional,
			boolean removeRegion) throws Exception {
		PutFromLoadValidator testee = new PutFromLoadValidator(
				transactional ? tm : null);
		if (transactional) {
			tm.begin();
		}
		testee.registerPendingPut(KEY1);
		if (removeRegion) {
			testee.regionRemoved();
		} else {
			testee.keyRemoved(KEY1);
		}
		assertFalse(testee.isPutValid(KEY1));
	}

	public void testDelayedNakedPutAfterKeyRemoval() throws Exception {
		delayedNakedPutAfterRemovalTest(false, false);
	}

	public void testDelayedNakedPutAfterKeyRemovalTransactional()
			throws Exception {
		delayedNakedPutAfterRemovalTest(true, false);
	}

	public void testDelayedNakedPutAfterRegionRemoval() throws Exception {
		delayedNakedPutAfterRemovalTest(false, true);
	}

	public void testDelayedNakedPutAfterRegionRemovalTransactional()
			throws Exception {
		delayedNakedPutAfterRemovalTest(true, true);
	}

	private void delayedNakedPutAfterRemovalTest(boolean transactional,
			boolean removeRegion) throws Exception {
		PutFromLoadValidator testee = new TestValidator(transactional ? tm
				: null, 100, 1000, 500, 10000);
		if (removeRegion) {
			testee.regionRemoved();
		} else {
			testee.keyRemoved(KEY1);
		}
		if (transactional) {
			tm.begin();
		}
		Thread.sleep(110);
		assertTrue(testee.isPutValid(KEY1));

	}
	
	public void testMultipleRegistrations() throws Exception {
		multipleRegistrationtest(false);
	}
	
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
					if (testee.isPutValid(KEY1)) {
						success.incrementAndGet();
					}
					finishedLatch.countDown();
				}
				catch (Exception e)  {
					e.printStackTrace();
				}
			}
		};
		
		ExecutorService executor = Executors.newFixedThreadPool(3);
		
		// Start with a removal so the "isPutValid" calls will fail if
		// any of the concurrent activity isn't handled properly
		
		testee.regionRemoved();
		
		// Do the registration + isPutValid calls
		executor.execute(r);
		executor.execute(r);
		executor.execute(r);
		
		finishedLatch.await(5, TimeUnit.SECONDS);
		
		assertEquals("All threads succeeded", 3, success.get());
	}
	
	/**
	 * White box test for ensuring key removals get cleaned up.
	 * 
	 * @throws Exception
	 */
	public void testRemovalCleanup() throws Exception {
		TestValidator testee = new TestValidator(null, 200, 1000, 500, 10000);
		testee.keyRemoved("KEY1");
		testee.keyRemoved("KEY2");
		Thread.sleep(210);
		assertEquals(2, testee.getRemovalQueueLength());
		testee.keyRemoved("KEY1");
		assertEquals(2, testee.getRemovalQueueLength());
		testee.keyRemoved("KEY2");
		assertEquals(2, testee.getRemovalQueueLength());
	}
	
	/** 
	 * Very much a white box test of the logic for ensuring pending
	 * put registrations get cleaned up.
	 *  
	 * @throws Exception
	 */
	public void testPendingPutCleanup() throws Exception {
		TestValidator testee = new TestValidator(tm, 5000, 600, 300, 900);
		
		// Start with a regionRemoval so we can confirm at the end that all
		// registrations have been cleaned out
		testee.regionRemoved();
		
		testee.registerPendingPut("1");
		testee.registerPendingPut("2");
		testee.registerPendingPut("3");
		testee.registerPendingPut("4");
		testee.registerPendingPut("5");
		testee.registerPendingPut("6");
		testee.isPutValid("6");
		testee.isPutValid("2");
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
		testee.isPutValid("3");
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
		assertFalse(testee.isPutValid("1"));
		// 5 was overage, so should have been cleaned
		assertEquals(2, testee.getOveragePendingPutQueueLength());
		assertFalse(testee.isPutValid("2"));
		// 7 was overage, so should have been cleaned
		assertEquals(1, testee.getOveragePendingPutQueueLength());
		assertFalse(testee.isPutValid("3"));
		assertFalse(testee.isPutValid("4"));
		assertFalse(testee.isPutValid("5"));
		assertFalse(testee.isPutValid("6"));
		assertFalse(testee.isPutValid("7"));
		assertTrue(testee.isPutValid("8"));
		tm.resume(tx);
		assertTrue(testee.isPutValid("7"));
	}

	private static class TestValidator extends PutFromLoadValidator {

		protected TestValidator(TransactionManager transactionManager,
				long nakedPutInvalidationPeriod, long pendingPutOveragePeriod,
				long pendingPutRecentPeriod, long maxPendingPutDelay) {
			super(transactionManager, nakedPutInvalidationPeriod,
					pendingPutOveragePeriod, pendingPutRecentPeriod,
					maxPendingPutDelay);
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
