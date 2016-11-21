package org.hibernate.test.cache.infinispan;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.transaction.internal.TransactionImpl;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransactionAccess;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.test.cache.infinispan.util.BatchModeJtaPlatform;
import org.hibernate.test.cache.infinispan.util.BatchModeTransactionCoordinator;
import org.hibernate.test.cache.infinispan.util.InfinispanTestingSetup;
import org.hibernate.test.cache.infinispan.util.JdbcResourceTransactionMock;
import org.hibernate.test.cache.infinispan.util.TestSynchronization;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import junit.framework.AssertionFailedError;

import org.infinispan.Cache;
import org.infinispan.test.TestingUtil;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public abstract class AbstractRegionAccessStrategyTest<R extends BaseRegion, S extends RegionAccessStrategy>
		extends AbstractNonFunctionalTest {
	protected final Logger log = Logger.getLogger(getClass());

	@Rule
	public InfinispanTestingSetup infinispanTestIdentifier = new InfinispanTestingSetup();

	public static final String REGION_NAME = "test/com.foo.test";
	public static final String KEY_BASE = "KEY";
	public static final String VALUE1 = "VALUE1";
	public static final String VALUE2 = "VALUE2";
	public static final CacheDataDescription CACHE_DATA_DESCRIPTION
			= new CacheDataDescriptionImpl(true, true, ComparableComparator.INSTANCE, null);

	protected NodeEnvironment localEnvironment;
	protected R localRegion;
	protected S localAccessStrategy;

	protected NodeEnvironment remoteEnvironment;
	protected R remoteRegion;
	protected S remoteAccessStrategy;

	protected boolean transactional;
	protected boolean invalidation;
	protected boolean synchronous;
	protected Exception node1Exception;
	protected Exception node2Exception;
	protected AssertionFailedError node1Failure;
	protected AssertionFailedError node2Failure;

	@Override
	protected boolean canUseLocalMode() {
		return false;
	}

	@Before
	public void prepareResources() throws Exception {
		// to mimic exactly the old code results, both environments here are exactly the same...
		StandardServiceRegistryBuilder ssrb = createStandardServiceRegistryBuilder();
		localEnvironment = new NodeEnvironment( ssrb );
		localEnvironment.prepare();

		localRegion = getRegion(localEnvironment);
		localAccessStrategy = getAccessStrategy(localRegion);

		transactional = Caches.isTransactionalCache(localRegion.getCache());
		invalidation = Caches.isInvalidationCache(localRegion.getCache());
		synchronous = Caches.isSynchronousCache(localRegion.getCache());

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		remoteEnvironment = new NodeEnvironment( ssrb );
		remoteEnvironment.prepare();

		remoteRegion = getRegion(remoteEnvironment);
		remoteAccessStrategy = getAccessStrategy(remoteRegion);

		waitForClusterToForm(localRegion.getCache(), remoteRegion.getCache());
	}

	private interface SessionMock extends Session, SharedSessionContractImplementor {
	}

	private interface NonJtaTransactionCoordinator extends TransactionCoordinatorOwner, JdbcResourceTransactionAccess {
	}

	protected SharedSessionContractImplementor mockedSession() {
		SessionMock session = mock(SessionMock.class);
		when(session.isClosed()).thenReturn(false);
		when(session.getTimestamp()).thenReturn(System.currentTimeMillis());
		if (jtaPlatform == BatchModeJtaPlatform.class) {
			BatchModeTransactionCoordinator txCoord = new BatchModeTransactionCoordinator();
			when(session.getTransactionCoordinator()).thenReturn(txCoord);
			when(session.beginTransaction()).then(invocation -> {
				Transaction tx = txCoord.newTransaction();
				tx.begin();
				return tx;
			});
		} else if (jtaPlatform == null) {
			Connection connection = mock(Connection.class);
			JdbcConnectionAccess jdbcConnectionAccess = mock(JdbcConnectionAccess.class);
			try {
				when(jdbcConnectionAccess.obtainConnection()).thenReturn(connection);
			} catch (SQLException e) {
				// never thrown from mock
			}
			JdbcSessionOwner jdbcSessionOwner = mock(JdbcSessionOwner.class);
			when(jdbcSessionOwner.getJdbcConnectionAccess()).thenReturn(jdbcConnectionAccess);
			SqlExceptionHelper sqlExceptionHelper = mock(SqlExceptionHelper.class);
			JdbcServices jdbcServices = mock(JdbcServices.class);
			when(jdbcServices.getSqlExceptionHelper()).thenReturn(sqlExceptionHelper);
			ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
			when(serviceRegistry.getService(JdbcServices.class)).thenReturn(jdbcServices);
			JdbcSessionContext jdbcSessionContext = mock(JdbcSessionContext.class);
			when(jdbcSessionContext.getServiceRegistry()).thenReturn(serviceRegistry);
			when(jdbcSessionOwner.getJdbcSessionContext()).thenReturn(jdbcSessionContext);
			NonJtaTransactionCoordinator txOwner = mock(NonJtaTransactionCoordinator.class);
			when(txOwner.getResourceLocalTransaction()).thenReturn(new JdbcResourceTransactionMock());
			when(txOwner.getJdbcSessionOwner()).thenReturn(jdbcSessionOwner);
			when(txOwner.isActive()).thenReturn(true);
			TransactionCoordinator txCoord = JdbcResourceLocalTransactionCoordinatorBuilderImpl.INSTANCE
							.buildTransactionCoordinator(txOwner, null);
			when(session.getTransactionCoordinator()).thenReturn(txCoord);
			when(session.beginTransaction()).then(invocation -> {
				Transaction tx = new TransactionImpl(txCoord, session.getExceptionConverter());
				tx.begin();
				return tx;
			});
		} else {
			throw new IllegalStateException("Unknown JtaPlatform: " + jtaPlatform);
		}
		return session;
	}

	protected abstract S getAccessStrategy(R region);

	@Test
	public void testRemove() throws Exception {
		evictOrRemoveTest( false );
	}

	@Test
	public void testEvict() throws Exception {
		evictOrRemoveTest( true );
	}

	protected abstract R getRegion(NodeEnvironment environment);

	protected void waitForClusterToForm(Cache... caches) {
		TestingUtil.blockUntilViewsReceived(10000, Arrays.asList(caches));
	}

	@After
	public void releaseResources() throws Exception {
		try {
			if (localEnvironment != null) {
				localEnvironment.release();
			}
		}
		finally {
			if (remoteEnvironment != null) {
				remoteEnvironment.release();
			}
		}
	}

	protected boolean isTransactional() {
		return transactional;
	}

	protected boolean isUsingInvalidation() {
		return invalidation;
	}

	protected boolean isSynchronous() {
		return synchronous;
	}

	protected void evictOrRemoveTest(final boolean evict) throws Exception {
		final Object KEY = generateNextKey();
		assertEquals(0, localRegion.getCache().size());
		assertEquals(0, remoteRegion.getCache().size());

		SharedSessionContractImplementor s1 = mockedSession();
		assertNull("local is clean", localAccessStrategy.get(s1, KEY, s1.getTimestamp()));
		SharedSessionContractImplementor s2 = mockedSession();
		assertNull("remote is clean", remoteAccessStrategy.get(s2, KEY, s2.getTimestamp()));

		SharedSessionContractImplementor s3 = mockedSession();
		localAccessStrategy.putFromLoad(s3, KEY, VALUE1, s3.getTimestamp(), 1);
		SharedSessionContractImplementor s4 = mockedSession();
		assertEquals(VALUE1, localAccessStrategy.get(s4, KEY, s4.getTimestamp()));
		SharedSessionContractImplementor s5 = mockedSession();
		remoteAccessStrategy.putFromLoad(s5, KEY, VALUE1, s5.getTimestamp(), new Integer(1));
		SharedSessionContractImplementor s6 = mockedSession();
		assertEquals(VALUE1, remoteAccessStrategy.get(s6, KEY, s6.getTimestamp()));

		SharedSessionContractImplementor session = mockedSession();
		withTx(localEnvironment, session, () -> {
			if (evict) {
				localAccessStrategy.evict(KEY);
			}
			else {
				doRemove(localRegion.getTransactionManager(), localAccessStrategy, session, KEY);
			}
			return null;
		});

		SharedSessionContractImplementor s7 = mockedSession();
		assertNull(localAccessStrategy.get(s7, KEY, s7.getTimestamp()));
		assertEquals(0, localRegion.getCache().size());
		SharedSessionContractImplementor s8 = mockedSession();
		assertNull(remoteAccessStrategy.get(s8, KEY, s8.getTimestamp()));
		assertEquals(0, remoteRegion.getCache().size());
	}

	protected void doRemove(TransactionManager tm, S strategy, SharedSessionContractImplementor session, Object key) throws SystemException, RollbackException {
		SoftLock softLock = strategy.lockItem(session, key, null);
		strategy.remove(session, key);
		session.getTransactionCoordinator().getLocalSynchronizations().registerSynchronization(
				new TestSynchronization.UnlockItem(strategy, session, key, softLock));
	}

	@Test
	public void testRemoveAll() throws Exception {
		evictOrRemoveAllTest(false);
	}

	@Test
	public void testEvictAll() throws Exception {
		evictOrRemoveAllTest(true);
	}

	protected void assertThreadsRanCleanly() {
		if (node1Failure != null) {
			throw node1Failure;
		}
		if (node2Failure != null) {
			throw node2Failure;
		}

		if (node1Exception != null) {
			log.error("node1 saw an exception", node1Exception);
			assertEquals("node1 saw no exceptions", null, node1Exception);
		}

		if (node2Exception != null) {
			log.error("node2 saw an exception", node2Exception);
			assertEquals("node2 saw no exceptions", null, node2Exception);
		}
	}

	protected abstract Object generateNextKey();

	protected void evictOrRemoveAllTest(final boolean evict) throws Exception {
		final Object KEY = generateNextKey();
		assertEquals(0, localRegion.getCache().size());
		assertEquals(0, remoteRegion.getCache().size());
		SharedSessionContractImplementor s1 = mockedSession();
		assertNull("local is clean", localAccessStrategy.get(s1, KEY, s1.getTimestamp()));
		SharedSessionContractImplementor s2 = mockedSession();
		assertNull("remote is clean", remoteAccessStrategy.get(s2, KEY, s2.getTimestamp()));

		SharedSessionContractImplementor s3 = mockedSession();
		localAccessStrategy.putFromLoad(s3, KEY, VALUE1, s3.getTimestamp(), 1);
		SharedSessionContractImplementor s4 = mockedSession();
		assertEquals(VALUE1, localAccessStrategy.get(s4, KEY, s4.getTimestamp()));
		SharedSessionContractImplementor s5 = mockedSession();
		remoteAccessStrategy.putFromLoad(s5, KEY, VALUE1, s5.getTimestamp(), 1);
		SharedSessionContractImplementor s6 = mockedSession();
		assertEquals(VALUE1, remoteAccessStrategy.get(s6, KEY, s6.getTimestamp()));

		// Wait for async propagation
		sleep(250);

		withTx(localEnvironment, mockedSession(), () -> {
			if (evict) {
				localAccessStrategy.evictAll();
			} else {
				SoftLock softLock = localAccessStrategy.lockRegion();
				localAccessStrategy.removeAll();
				localAccessStrategy.unlockRegion(softLock);
			}
			return null;
		});
		// This should re-establish the region root node in the optimistic case
		SharedSessionContractImplementor s7 = mockedSession();
		assertNull(localAccessStrategy.get(s7, KEY, s7.getTimestamp()));
		assertEquals(0, localRegion.getCache().size());

		// Re-establishing the region root on the local node doesn't
		// propagate it to other nodes. Do a get on the remote node to re-establish
		SharedSessionContractImplementor s8 = mockedSession();
		assertNull(remoteAccessStrategy.get(s8, KEY, s8.getTimestamp()));
		assertEquals(0, remoteRegion.getCache().size());

		// Wait for async propagation of EndInvalidationCommand beforeQuery executing naked put
		sleep(250);

		// Test whether the get above messes up the optimistic version
		SharedSessionContractImplementor s9 = mockedSession();
 		assertTrue(remoteAccessStrategy.putFromLoad(s9, KEY, VALUE1, s9.getTimestamp(), 1));
		SharedSessionContractImplementor s10 = mockedSession();
		assertEquals(VALUE1, remoteAccessStrategy.get(s10, KEY, s10.getTimestamp()));
		assertEquals(1, remoteRegion.getCache().size());

		// Wait for async propagation
		sleep(250);

		SharedSessionContractImplementor s11 = mockedSession();
		assertEquals((isUsingInvalidation() ? null : VALUE1), localAccessStrategy.get(s11, KEY, s11.getTimestamp()));
		SharedSessionContractImplementor s12 = mockedSession();
		assertEquals(VALUE1, remoteAccessStrategy.get(s12, KEY, s12.getTimestamp()));
	}

	protected class PutFromLoadNode2 extends Thread {
		private final Object KEY;
		private final CountDownLatch writeLatch1;
		private final CountDownLatch writeLatch2;
		private final boolean useMinimalAPI;
		private final CountDownLatch completionLatch;

		public PutFromLoadNode2(Object KEY, CountDownLatch writeLatch1, CountDownLatch writeLatch2, boolean useMinimalAPI, CountDownLatch completionLatch) {
			this.KEY = KEY;
			this.writeLatch1 = writeLatch1;
			this.writeLatch2 = writeLatch2;
			this.useMinimalAPI = useMinimalAPI;
			this.completionLatch = completionLatch;
		}

		@Override
		public void run() {
			try {
				SharedSessionContractImplementor session = mockedSession();
				withTx(remoteEnvironment, session, () -> {

					assertNull(remoteAccessStrategy.get(session, KEY, session.getTimestamp()));

					// Let node1 write
					writeLatch1.countDown();
					// Wait for node1 to finish
					writeLatch2.await();

					if (useMinimalAPI) {
						remoteAccessStrategy.putFromLoad(session, KEY, VALUE1, session.getTimestamp(), 1, true);
					} else {
						remoteAccessStrategy.putFromLoad(session, KEY, VALUE1, session.getTimestamp(), 1);
					}
					return null;
				});
			} catch (Exception e) {
				log.error("node2 caught exception", e);
				node2Exception = e;
			} catch (AssertionFailedError e) {
				node2Failure = e;
			} finally {
				completionLatch.countDown();
			}
		}
	}
}
