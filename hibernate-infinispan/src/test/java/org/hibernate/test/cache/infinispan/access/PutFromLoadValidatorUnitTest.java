/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.access;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.transaction.TransactionManager;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.test.cache.infinispan.util.TestTimeService;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.TestForIssue;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeJtaTransactionManagerImpl;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.hibernate.testing.junit4.CustomRunner;
import org.infinispan.AdvancedCache;
import org.infinispan.test.fwk.TestResourceTracker;
import org.junit.After;
import org.junit.Test;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.junit.runner.RunWith;

import static org.infinispan.test.Exceptions.expectException;
import static org.infinispan.test.TestingUtil.withTx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests of {@link PutFromLoadValidator}.
 *
 * @author Brian Stansberry
 * @author Galder Zamarreño
 * @version $Revision: $
 */
@RunWith(CustomRunner.class)
public class PutFromLoadValidatorUnitTest {

	private static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(
			PutFromLoadValidatorUnitTest.class);
	private static final TestTimeService TIME_SERVICE = new TestTimeService();

	private Object KEY1 = "KEY1";

	private TransactionManager tm;
	private EmbeddedCacheManager cm;
	private AdvancedCache<Object, Object> cache;
	private List<Runnable> cleanup = new ArrayList<>();

	@BeforeClassOnce
	public void setUp() throws Exception {
		TestResourceTracker.testStarted(getClass().getSimpleName());
		tm = DualNodeJtaTransactionManagerImpl.getInstance("test");
		cm = TestCacheManagerFactory.createCacheManager(true);
		cache = cm.getCache().getAdvancedCache();
	}

	@AfterClassOnce
	public void stop() {
		tm = null;
		cm.stop();
		TestResourceTracker.testFinished(getClass().getSimpleName());
	}

	@After
	public void tearDown() throws Exception {
		cleanup.forEach(Runnable::run);
		cleanup.clear();
		try {
			DualNodeJtaTransactionManagerImpl.cleanupTransactions();
		}
		finally {
			DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
		}
		cache.clear();
		cm.getCache(cache.getName() + "-" + InfinispanRegionFactory.DEF_PENDING_PUTS_RESOURCE).clear();
	}

	private static InfinispanRegionFactory regionFactory(EmbeddedCacheManager cm) {
		Properties properties = new Properties();
		properties.put(TestInfinispanRegionFactory.TIME_SERVICE, TIME_SERVICE);
		InfinispanRegionFactory regionFactory = new TestInfinispanRegionFactory(properties);
		regionFactory.setCacheManager(cm);
		regionFactory.start(CacheTestUtil.sfOptionsForStart(), properties);
		return regionFactory;
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
		PutFromLoadValidator testee = new PutFromLoadValidator(cache, regionFactory(cm));
		exec(transactional, new NakedPut(testee, true));
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
		PutFromLoadValidator testee = new PutFromLoadValidator(cache, regionFactory(cm));
		exec(transactional, new RegularPut(testee));
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
		PutFromLoadValidator testee = new PutFromLoadValidator(cache, regionFactory(cm));
		Invalidation invalidation = new Invalidation(testee, removeRegion);
		// the naked put can succeed because it has txTimestamp after invalidation
		NakedPut nakedPut = new NakedPut(testee, true);
		exec(transactional, invalidation, nakedPut);
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
		PutFromLoadValidator testee = new PutFromLoadValidator(cache, regionFactory(cm));
		Invalidation invalidation = new Invalidation(testee, removeRegion);
		RegularPut regularPut = new RegularPut(testee);
		exec(transactional, invalidation, regularPut);
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
		PutFromLoadValidator testee = new PutFromLoadValidator(cache, regionFactory(cm));
		try {
			long txTimestamp = TIME_SERVICE.wallClockTime();
			if (transactional) {
				tm.begin();
			}
			SharedSessionContractImplementor session1 = mock(SharedSessionContractImplementor.class);
			SharedSessionContractImplementor session2 = mock(SharedSessionContractImplementor.class);
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

	@Test
	public void testMultipleRegistrations() throws Exception {
		multipleRegistrationtest(false);
	}

	@Test
	public void testMultipleRegistrationsTransactional() throws Exception {
		multipleRegistrationtest(true);
	}

	private void multipleRegistrationtest(final boolean transactional) throws Exception {
		final PutFromLoadValidator testee = new PutFromLoadValidator(cache, regionFactory(cm));

		final CountDownLatch registeredLatch = new CountDownLatch(3);
		final CountDownLatch finishedLatch = new CountDownLatch(3);
		final AtomicInteger success = new AtomicInteger();

		Runnable r = () -> {
			try {
				long txTimestamp = TIME_SERVICE.wallClockTime();
				if (transactional) {
					tm.begin();
				}
				SharedSessionContractImplementor session = mock (SharedSessionContractImplementor.class);
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
		};

		ExecutorService executor = Executors.newFixedThreadPool(3);
		cleanup.add(() -> executor.shutdownNow());

		// Start with a removal so the "isPutValid" calls will fail if
		// any of the concurrent activity isn't handled properly

		testee.beginInvalidatingRegion();
		testee.endInvalidatingRegion();
		TIME_SERVICE.advance(1);

		// Do the registration + isPutValid calls
		executor.execute(r);
		executor.execute(r);
		executor.execute(r);

		assertTrue(finishedLatch.await(5, TimeUnit.SECONDS));

		assertEquals("All threads succeeded", 3, success.get());
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
		final PutFromLoadValidator testee = new PutFromLoadValidator(cache, regionFactory(cm));
		final CountDownLatch removeLatch = new CountDownLatch(1);
		final CountDownLatch pferLatch = new CountDownLatch(1);
		final AtomicReference<Object> cache = new AtomicReference<>("INITIAL");

		Callable<Boolean> pferCallable = () -> {
			long txTimestamp = TIME_SERVICE.wallClockTime();
			SharedSessionContractImplementor session = mock (SharedSessionContractImplementor.class);
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
		};

		Callable<Void> invalidateCallable = () -> {
         removeLatch.await();
         if (keyOnly) {
            SharedSessionContractImplementor session = mock (SharedSessionContractImplementor.class);
            testee.beginInvalidatingKey(session, KEY1);
         } else {
            testee.beginInvalidatingRegion();
         }
         cache.set(null);
         return null;
      };

		ExecutorService executor = Executors.newCachedThreadPool();
		cleanup.add(() -> executor.shutdownNow());
		Future<Boolean> pferFuture = executor.submit(pferCallable);
		Future<Void> invalidateFuture = executor.submit(invalidateCallable);

		expectException(TimeoutException.class, () -> invalidateFuture.get(1, TimeUnit.SECONDS));

		pferLatch.countDown();

		assertTrue(pferFuture.get(5, TimeUnit.SECONDS));
		invalidateFuture.get(5, TimeUnit.SECONDS);

		assertNull(cache.get());
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
				SharedSessionContractImplementor session = mock (SharedSessionContractImplementor.class);
				boolean success = putFromLoadValidator.beginInvalidatingKey(session, KEY1);
				assertTrue(success);
				success = putFromLoadValidator.endInvalidatingKey(session, KEY1);
				assertTrue(success);
			}
			// if we go for the timestamp-based approach, invalidation in the same millisecond
			// as the registerPendingPut/acquirePutFromLoad lock results in failure.
			TIME_SERVICE.advance(1);
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
				long txTimestamp = TIME_SERVICE.wallClockTime(); // this should be acquired before UserTransaction.begin()
				SharedSessionContractImplementor session = mock (SharedSessionContractImplementor.class);
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
				long txTimestamp = TIME_SERVICE.wallClockTime(); // this should be acquired before UserTransaction.begin()
				SharedSessionContractImplementor session = mock (SharedSessionContractImplementor.class);
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
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.simpleCache(true).expiration().maxIdle(500);
		Configuration ppCfg = cb.build();

		InfinispanRegionFactory regionFactory = mock(InfinispanRegionFactory.class);
		doReturn(ppCfg).when(regionFactory).getPendingPutsCacheConfiguration();
		doAnswer(invocation -> TIME_SERVICE.wallClockTime()).when(regionFactory).nextTimestamp();

		PutFromLoadValidator testee = new PutFromLoadValidator(cache, regionFactory, cm);

		for (int i = 0; i < 100; ++i) {
			try {
				withTx(tm, () -> {
					SharedSessionContractImplementor session = mock (SharedSessionContractImplementor.class);
					testee.registerPendingPut(session, KEY1, 0);
					return null;
				});
				TIME_SERVICE.advance(10);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		String ppName = cm.getCache().getName() + "-" + InfinispanRegionFactory.DEF_PENDING_PUTS_RESOURCE;
		Map ppCache = cm.getCache(ppName, false);
		assertNotNull(ppCache);
		Object pendingPutMap = ppCache.get(KEY1);
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
}
