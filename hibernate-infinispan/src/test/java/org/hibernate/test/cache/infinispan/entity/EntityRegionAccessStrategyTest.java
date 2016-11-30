/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.entity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.test.cache.infinispan.AbstractRegionAccessStrategyTest;
import org.hibernate.test.cache.infinispan.NodeEnvironment;
import org.hibernate.test.cache.infinispan.util.TestSynchronization;
import org.hibernate.test.cache.infinispan.util.TestingKeyFactory;
import org.junit.Ignore;
import org.junit.Test;
import junit.framework.AssertionFailedError;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Base class for tests of EntityRegionAccessStrategy impls.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class EntityRegionAccessStrategyTest extends
		AbstractRegionAccessStrategyTest<EntityRegionImpl, EntityRegionAccessStrategy> {
	protected static int testCount;

	@Override
	protected Object generateNextKey() {
		return TestingKeyFactory.generateEntityCacheKey( KEY_BASE + testCount++ );
	}

	@Override
	protected EntityRegionImpl getRegion(NodeEnvironment environment) {
		return environment.getEntityRegion(REGION_NAME, CACHE_DATA_DESCRIPTION);
	}

	@Override
	protected EntityRegionAccessStrategy getAccessStrategy(EntityRegionImpl region) {
		return region.buildAccessStrategy( accessType );
	}

	@Test
	public void testGetRegion() {
		assertEquals("Correct region", localRegion, localAccessStrategy.getRegion());
	}

	@Test
	public void testPutFromLoad() throws Exception {
		if (accessType == AccessType.READ_ONLY) {
			putFromLoadTestReadOnly(false);
		} else {
			putFromLoadTest(false, false);
		}
	}

	@Test
	public void testPutFromLoadMinimal() throws Exception {
		if (accessType == AccessType.READ_ONLY) {
			putFromLoadTestReadOnly(true);
		} else {
			putFromLoadTest(true, false);
		}
	}

	@Test
	public void testInsert() throws Exception {
		final Object KEY = generateNextKey();

		final CountDownLatch readLatch = new CountDownLatch(1);
		final CountDownLatch commitLatch = new CountDownLatch(1);
		final CountDownLatch completionLatch = new CountDownLatch(2);

		CountDownLatch asyncInsertLatch = expectAfterUpdate();

		Thread inserter = new Thread(() -> {
				try {
					SharedSessionContractImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						assertNull("Correct initial value", localAccessStrategy.get(session, KEY, session.getTimestamp()));

						doInsert(localAccessStrategy, session, KEY, VALUE1, 1);

						readLatch.countDown();
						commitLatch.await();
						return null;
					});
				} catch (Exception e) {
					log.error("node1 caught exception", e);
					node1Exception = e;
				} catch (AssertionFailedError e) {
					node1Failure = e;
				} finally {

					completionLatch.countDown();
				}
			}, "testInsert-inserter");

		Thread reader = new Thread(() -> {
				try {
					SharedSessionContractImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						readLatch.await();

						assertNull("Correct initial value", localAccessStrategy.get(session, KEY, session.getTimestamp()));
						return null;
					});
				} catch (Exception e) {
					log.error("node1 caught exception", e);
					node1Exception = e;
				} catch (AssertionFailedError e) {
					node1Failure = e;
				} finally {
					commitLatch.countDown();
					completionLatch.countDown();
				}
			}, "testInsert-reader");

		inserter.setDaemon(true);
		reader.setDaemon(true);
		inserter.start();
		reader.start();

		assertTrue("Threads completed", completionLatch.await(10, TimeUnit.SECONDS));

		assertThreadsRanCleanly();

		SharedSessionContractImplementor s1 = mockedSession();
		assertEquals("Correct node1 value", VALUE1, localAccessStrategy.get(s1, KEY, s1.getTimestamp()));

		assertTrue(asyncInsertLatch.await(10, TimeUnit.SECONDS));
		Object expected = isUsingInvalidation() ? null : VALUE1;
		SharedSessionContractImplementor s2 = mockedSession();
		assertEquals("Correct node2 value", expected, remoteAccessStrategy.get(s2, KEY, s2.getTimestamp()));
	}

	protected void doInsert(EntityRegionAccessStrategy strategy, SharedSessionContractImplementor session, Object key, String value, Object version) {
		strategy.insert(session, key, value, null);
		session.getTransactionCoordinator().getLocalSynchronizations().registerSynchronization(
				new TestSynchronization.AfterInsert(strategy, session, key, value, version));
	}

	protected void putFromLoadTestReadOnly(boolean minimal) throws Exception {
		final Object KEY = TestingKeyFactory.generateEntityCacheKey( KEY_BASE + testCount++ );

		CountDownLatch remotePutFromLoadLatch = expectPutFromLoad();

		SharedSessionContractImplementor session = mockedSession();
		withTx(localEnvironment, session, () -> {
			assertNull(localAccessStrategy.get(session, KEY, session.getTimestamp()));
			if (minimal)
				localAccessStrategy.putFromLoad(session, KEY, VALUE1, session.getTimestamp(), 1, true);
			else
				localAccessStrategy.putFromLoad(session, KEY, VALUE1, session.getTimestamp(), 1);
			return null;
		});

		SharedSessionContractImplementor s2 = mockedSession();
		assertEquals(VALUE1, localAccessStrategy.get(s2, KEY, s2.getTimestamp()));
		SharedSessionContractImplementor s3 = mockedSession();
		Object expected;
		if (isUsingInvalidation()) {
			expected = null;
		} else {
			if (accessType != AccessType.NONSTRICT_READ_WRITE) {
				assertTrue(remotePutFromLoadLatch.await(2, TimeUnit.SECONDS));
			}
			expected = VALUE1;
		}
		assertEquals(expected, remoteAccessStrategy.get(s3, KEY, s3.getTimestamp()));
	}

	@Test
	public void testUpdate() throws Exception {
		if (accessType == AccessType.READ_ONLY) {
			return;
		}

		final Object KEY = generateNextKey();

		// Set up initial state
		SharedSessionContractImplementor s1 = mockedSession();
		localAccessStrategy.putFromLoad(s1, KEY, VALUE1, s1.getTimestamp(), 1);
		SharedSessionContractImplementor s2 = mockedSession();
		remoteAccessStrategy.putFromLoad(s2, KEY, VALUE1, s2.getTimestamp(), 1);

		// both nodes are updated, we don't have to wait for any async replication of putFromLoad
		CountDownLatch asyncUpdateLatch = expectAfterUpdate();

		final CountDownLatch readLatch = new CountDownLatch(1);
		final CountDownLatch commitLatch = new CountDownLatch(1);
		final CountDownLatch completionLatch = new CountDownLatch(2);

		Thread updater = new Thread(() -> {
				try {
					SharedSessionContractImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						log.debug("Transaction began, get initial value");
						assertEquals("Correct initial value", VALUE1, localAccessStrategy.get(session, KEY, session.getTimestamp()));
						log.debug("Now update value");
						doUpdate(localAccessStrategy, session, KEY, VALUE2, 2);
						log.debug("Notify the read latch");
						readLatch.countDown();
						log.debug("Await commit");
						commitLatch.await();
						return null;
					});
				} catch (Exception e) {
					log.error("node1 caught exception", e);
					node1Exception = e;
				} catch (AssertionFailedError e) {
					node1Failure = e;
				} finally {
					if (readLatch.getCount() > 0) {
						readLatch.countDown();
					}
					log.debug("Completion latch countdown");
					completionLatch.countDown();
				}
			}, "testUpdate-updater");

		Thread reader = new Thread(() -> {
				try {
					SharedSessionContractImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						log.debug("Transaction began, await read latch");
						readLatch.await();
						log.debug("Read latch acquired, verify local access strategy");

						// This won't block w/ mvc and will read the old value (if transactional as the transaction
						// is not being committed yet, or if non-strict as we do the actual update only after transaction)
						// or null if non-transactional
						Object expected = isTransactional() || accessType == AccessType.NONSTRICT_READ_WRITE ? VALUE1 : null;
						assertEquals("Correct value", expected, localAccessStrategy.get(session, KEY, session.getTimestamp()));
						return null;
					});
				} catch (Exception e) {
					log.error("node1 caught exception", e);
					node1Exception = e;
				} catch (AssertionFailedError e) {
					node1Failure = e;
				} finally {
					commitLatch.countDown();
					log.debug("Completion latch countdown");
					completionLatch.countDown();
				}
			}, "testUpdate-reader");

		updater.setDaemon(true);
		reader.setDaemon(true);
		updater.start();
		reader.start();

		assertTrue(completionLatch.await(2, TimeUnit.SECONDS));

		assertThreadsRanCleanly();

		SharedSessionContractImplementor s3 = mockedSession();
		assertEquals("Correct node1 value", VALUE2, localAccessStrategy.get(s3, KEY, s3.getTimestamp()));
		assertTrue(asyncUpdateLatch.await(10, TimeUnit.SECONDS));
		Object expected = isUsingInvalidation() ? null : VALUE2;
		SharedSessionContractImplementor s4 = mockedSession();
		assertEquals("Correct node2 value", expected, remoteAccessStrategy.get(s4, KEY, s4.getTimestamp()));
	}

	@Override
	protected void doUpdate(EntityRegionAccessStrategy strategy, SharedSessionContractImplementor session, Object key, Object value, Object version) throws javax.transaction.RollbackException, javax.transaction.SystemException {
		SoftLock softLock = strategy.lockItem(session, key, null);
		strategy.update(session, key, value, null, null);
		session.getTransactionCoordinator().getLocalSynchronizations().registerSynchronization(
				new TestSynchronization.AfterUpdate(strategy, session, key, value, version, softLock));
	}

	/**
	 * This test fails in CI too often because it depends on very short timeout. The behaviour is basically
	 * non-testable as we want to make sure that the "Putter" is always progressing; however, it is sometimes
	 * progressing in different thread (on different node), and sometimes even in system, sending a message
	 * over network. Therefore even checking that some OOB/remote thread is in RUNNABLE/RUNNING state is prone
	 * to spurious failure (and we can't grab the state of all threads atomically).
    */
	@Ignore
	@Test
	public void testContestedPutFromLoad() throws Exception {
		if (accessType == AccessType.READ_ONLY) {
			return;
		}

		final Object KEY = TestingKeyFactory.generateEntityCacheKey(KEY_BASE + testCount++);

		SharedSessionContractImplementor s1 = mockedSession();
		localAccessStrategy.putFromLoad(s1, KEY, VALUE1, s1.getTimestamp(), 1);

		final CountDownLatch pferLatch = new CountDownLatch(1);
		final CountDownLatch pferCompletionLatch = new CountDownLatch(1);
		final CountDownLatch commitLatch = new CountDownLatch(1);
		final CountDownLatch completionLatch = new CountDownLatch(1);

		Thread blocker = new Thread("Blocker") {
			@Override
			public void run() {
				try {
					SharedSessionContractImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						assertEquals("Correct initial value", VALUE1, localAccessStrategy.get(session, KEY, session.getTimestamp()));

						doUpdate(localAccessStrategy, session, KEY, VALUE2, 2);

						pferLatch.countDown();
						commitLatch.await();
						return null;
					});
				} catch (Exception e) {
					log.error("node1 caught exception", e);
					node1Exception = e;
				} catch (AssertionFailedError e) {
					node1Failure = e;
				} finally {
					completionLatch.countDown();
				}
			}
		};

		Thread putter = new Thread("Putter") {
			@Override
			public void run() {
				try {
					SharedSessionContractImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						localAccessStrategy.putFromLoad(session, KEY, VALUE1, session.getTimestamp(), 1);
						return null;
					});
				} catch (Exception e) {
					log.error("node1 caught exception", e);
					node1Exception = e;
				} catch (AssertionFailedError e) {
					node1Failure = e;
				} finally {
					pferCompletionLatch.countDown();
				}
			}
		};

		blocker.start();
		assertTrue("Active tx has done an update", pferLatch.await(1, TimeUnit.SECONDS));
		putter.start();
		assertTrue("putFromLoad returns promptly", pferCompletionLatch.await(10, TimeUnit.MILLISECONDS));

		commitLatch.countDown();

		assertTrue("Threads completed", completionLatch.await(1, TimeUnit.SECONDS));

		assertThreadsRanCleanly();

		SharedSessionContractImplementor session = mockedSession();
		assertEquals("Correct node1 value", VALUE2, localAccessStrategy.get(session, KEY, session.getTimestamp()));
	}
}
