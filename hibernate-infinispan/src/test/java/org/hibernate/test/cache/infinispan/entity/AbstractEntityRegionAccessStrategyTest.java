/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.entity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.test.cache.infinispan.AbstractRegionAccessStrategyTest;
import org.hibernate.test.cache.infinispan.NodeEnvironment;
import org.hibernate.test.cache.infinispan.util.TestSynchronization;
import org.hibernate.test.cache.infinispan.util.TestingKeyFactory;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Base class for tests of EntityRegionAccessStrategy impls.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractEntityRegionAccessStrategyTest extends
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
		return region.buildAccessStrategy(getAccessType());
	}

	@Test
	public void testCacheConfiguration() {
	}

	@Test
	public void testGetRegion() {
		assertEquals("Correct region", localRegion, localAccessStrategy.getRegion());
	}

	/**
	 * Simulate 2 nodes, both start, tx do a get, experience a cache miss, then
	 * 'read from db.' First does a putFromLoad, then an update. Second tries to
	 * do a putFromLoad with stale data (i.e. it took longer to read from the db).
	 * Both commit their tx. Then both start a new tx and get. First should see
	 * the updated data; second should either see the updated data
	 * (isInvalidation() == false) or null (isInvalidation() == true).
	 *
	 * @param useMinimalAPI
	 * @throws Exception
	 */
	protected void putFromLoadTest(final boolean useMinimalAPI) throws Exception {

		final Object KEY = generateNextKey();

		final CountDownLatch writeLatch1 = new CountDownLatch(1);
		final CountDownLatch writeLatch2 = new CountDownLatch(1);
		final CountDownLatch completionLatch = new CountDownLatch(2);

		Thread node1 = new Thread() {
			@Override
			public void run() {
				try {
					long txTimestamp = System.currentTimeMillis();
					SessionImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						assertNull(localAccessStrategy.get(session, KEY, txTimestamp));

						writeLatch1.await();

						if (useMinimalAPI) {
							localAccessStrategy.putFromLoad(session, KEY, VALUE1, txTimestamp, null, true);
						} else {
							localAccessStrategy.putFromLoad(session, KEY, VALUE1, txTimestamp, null);
						}

						doUpdate(localAccessStrategy, session, KEY, VALUE2);
						return null;
					});
				} catch (Exception e) {
					log.error("node1 caught exception", e);
					node1Exception = e;
				} catch (AssertionFailedError e) {
					node1Failure = e;
				} finally {
					// Let node2 write
					writeLatch2.countDown();
					completionLatch.countDown();
				}
			}
		};

		Thread node2 = new PutFromLoadNode2(KEY, writeLatch1, writeLatch2, useMinimalAPI, completionLatch);

		node1.setDaemon(true);
		node2.setDaemon(true);

		node1.start();
		node2.start();

		assertTrue("Threads completed", completionLatch.await(2, TimeUnit.SECONDS));

		assertThreadsRanCleanly();

		long txTimestamp = System.currentTimeMillis();
		assertEquals( VALUE2, localAccessStrategy.get(mockedSession(), KEY, txTimestamp));
		Object remoteValue = remoteAccessStrategy.get(mockedSession(), KEY, txTimestamp);
		if (isUsingInvalidation()) {
			// invalidation command invalidates pending put
			assertNull(remoteValue);
		}
		else {
			// The node1 update is replicated, preventing the node2 PFER
			assertEquals( VALUE2, remoteValue);
		}
	}

	@Test
	public void testInsert() throws Exception {

		final Object KEY = generateNextKey();

		final CountDownLatch readLatch = new CountDownLatch(1);
		final CountDownLatch commitLatch = new CountDownLatch(1);
		final CountDownLatch completionLatch = new CountDownLatch(2);

		Thread inserter = new Thread() {

			@Override
			public void run() {
				try {
					long txTimestamp = System.currentTimeMillis();
					SessionImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						assertNull("Correct initial value", localAccessStrategy.get(session, KEY, txTimestamp));

						doInsert(localAccessStrategy, session, KEY, VALUE1);

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
			}
		};

		Thread reader = new Thread() {
			@Override
			public void run() {
				try {
					long txTimestamp = System.currentTimeMillis();
					SessionImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						readLatch.await();

						assertNull("Correct initial value", localAccessStrategy.get(session, KEY, txTimestamp));
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
			}
		};

		inserter.setDaemon(true);
		reader.setDaemon(true);
		inserter.start();
		reader.start();

		assertTrue("Threads completed", completionLatch.await(1000, TimeUnit.SECONDS));

		assertThreadsRanCleanly();

		long txTimestamp = System.currentTimeMillis();
		assertEquals("Correct node1 value", VALUE1, localAccessStrategy.get(mockedSession(), KEY, txTimestamp));
		Object expected = isUsingInvalidation() ? null : VALUE1;
		assertEquals("Correct node2 value", expected, remoteAccessStrategy.get(mockedSession(), KEY, txTimestamp));
	}

	protected void doInsert(EntityRegionAccessStrategy strategy, SessionImplementor session, Object key, String value) {
		strategy.insert(session, key, value, null);
		session.getTransactionCoordinator().getLocalSynchronizations().registerSynchronization(
				new TestSynchronization.AfterInsert(strategy, session, key, value));
	}

	@Test
	public void testUpdate() throws Exception {

		final Object KEY = generateNextKey();

		// Set up initial state
		localAccessStrategy.putFromLoad(mockedSession(), KEY, VALUE1, System.currentTimeMillis(), new Integer(1));
		remoteAccessStrategy.putFromLoad(mockedSession(), KEY, VALUE1, System.currentTimeMillis(), new Integer(1));

		// Let the async put propagate
		sleep(250);

		final CountDownLatch readLatch = new CountDownLatch(1);
		final CountDownLatch commitLatch = new CountDownLatch(1);
		final CountDownLatch completionLatch = new CountDownLatch(2);

		Thread updater = new Thread("testUpdate-updater") {

			@Override
			public void run() {
				try {
					long txTimestamp = System.currentTimeMillis();
					withTx(localEnvironment, mockedSession(), () -> {
						log.debug("Transaction began, get initial value");
						assertEquals("Correct initial value", VALUE1, localAccessStrategy.get(mockedSession(), KEY, txTimestamp));
						log.debug("Now update value");
						doUpdate(AbstractEntityRegionAccessStrategyTest.this.localAccessStrategy, mockedSession(), KEY, VALUE2);
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
			}
		};

		Thread reader = new Thread("testUpdate-reader") {

			@Override
			public void run() {
				try {
					long txTimestamp = System.currentTimeMillis();
					SessionImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						log.debug("Transaction began, await read latch");
						readLatch.await();
						log.debug("Read latch acquired, verify local access strategy");

						// This won't block w/ mvc and will read the old value
						Object expected = VALUE1;
						assertEquals("Correct value", expected, localAccessStrategy.get(session, KEY, txTimestamp));
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
			}
		};

		updater.setDaemon(true);
		reader.setDaemon(true);
		updater.start();
		reader.start();

		// Should complete promptly
		assertTrue(completionLatch.await(2, TimeUnit.SECONDS));

		assertThreadsRanCleanly();

		long txTimestamp = System.currentTimeMillis();
		assertEquals("Correct node1 value", VALUE2, localAccessStrategy.get(mockedSession(), KEY, txTimestamp));
		Object expected = isUsingInvalidation() ? null : VALUE2;
		assertEquals("Correct node2 value", expected, remoteAccessStrategy.get(mockedSession(), KEY, txTimestamp));
	}

	protected void doUpdate(EntityRegionAccessStrategy strategy, SessionImplementor session, Object key, Object value) throws javax.transaction.RollbackException, javax.transaction.SystemException {
		SoftLock softLock = strategy.lockItem(session, key, null);
		strategy.update(session, key, value, null, null);
		session.getTransactionCoordinator().getLocalSynchronizations().registerSynchronization(
				new TestSynchronization.AfterUpdate(strategy, session, key, value, softLock));
	}

	@Test
	public void testContestedPutFromLoad() throws Exception {

		final Object KEY = TestingKeyFactory.generateEntityCacheKey(KEY_BASE + testCount++);

		localAccessStrategy.putFromLoad(mockedSession(), KEY, VALUE1, System.currentTimeMillis(), new Integer(1));

		final CountDownLatch pferLatch = new CountDownLatch(1);
		final CountDownLatch pferCompletionLatch = new CountDownLatch(1);
		final CountDownLatch commitLatch = new CountDownLatch(1);
		final CountDownLatch completionLatch = new CountDownLatch(1);

		Thread blocker = new Thread("Blocker") {

			@Override
			public void run() {

				try {
					SessionImplementor session = mockedSession();
					long txTimestamp = System.currentTimeMillis();
					withTx(localEnvironment, session, () -> {
						assertEquals("Correct initial value", VALUE1, localAccessStrategy.get(session, KEY, txTimestamp));

						doUpdate(localAccessStrategy, session, KEY, VALUE2);

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
					long txTimestamp = System.currentTimeMillis();
					SessionImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						localAccessStrategy.putFromLoad(session, KEY, VALUE1, txTimestamp, new Integer(1));
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
		assertTrue("putFromLoadreturns promtly", pferCompletionLatch.await(10, TimeUnit.MILLISECONDS));

		commitLatch.countDown();

		assertTrue("Threads completed", completionLatch.await(1, TimeUnit.SECONDS));

		assertThreadsRanCleanly();

		long txTimestamp = System.currentTimeMillis();
		assertEquals("Correct node1 value", VALUE2, localAccessStrategy.get(null, KEY, txTimestamp));
	}
}
