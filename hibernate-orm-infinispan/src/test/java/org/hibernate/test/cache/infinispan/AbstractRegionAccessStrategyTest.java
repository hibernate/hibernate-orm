package org.hibernate.test.cache.infinispan;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.impl.BaseRegion;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.infinispan.util.FutureUpdate;
import org.hibernate.cache.infinispan.util.TombstoneUpdate;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.access.AccessType;
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
import org.hibernate.test.cache.infinispan.util.ExpectingInterceptor;
import org.hibernate.test.cache.infinispan.util.JdbcResourceTransactionMock;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.test.cache.infinispan.util.TestSynchronization;
import org.hibernate.test.cache.infinispan.util.TestTimeService;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.infinispan.commands.write.InvalidateCommand;
import org.infinispan.test.fwk.TestResourceTracker;
import org.infinispan.AdvancedCache;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.junit.After;
import org.junit.Test;
import junit.framework.AssertionFailedError;

import org.infinispan.Cache;
import org.infinispan.test.TestingUtil;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public abstract class AbstractRegionAccessStrategyTest<R extends BaseRegion, S extends RegionAccessStrategy>
		extends AbstractNonFunctionalTest {
	protected final Logger log = Logger.getLogger(getClass());

	public static final String REGION_NAME = "test/com.foo.test";
	public static final String KEY_BASE = "KEY";
	public static final String VALUE1 = "VALUE1";
	public static final String VALUE2 = "VALUE2";
	public static final CacheDataDescription CACHE_DATA_DESCRIPTION
			= new CacheDataDescriptionImpl(true, true, ComparableComparator.INSTANCE, null);

	protected static final TestTimeService TIME_SERVICE = new TestTimeService();

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

	protected List<Runnable> cleanup = new ArrayList<>();

	@Override
	protected boolean canUseLocalMode() {
		return false;
	}

	@BeforeClassOnce
	public void prepareResources() throws Exception {
		TestResourceTracker.testStarted( getClass().getSimpleName() );
		// to mimic exactly the old code results, both environments here are exactly the same...
		StandardServiceRegistryBuilder ssrb = createStandardServiceRegistryBuilder();
		localEnvironment = new NodeEnvironment( ssrb );
		localEnvironment.prepare();

		localRegion = getRegion(localEnvironment);
		localAccessStrategy = getAccessStrategy(localRegion);

		transactional = Caches.isTransactionalCache(localRegion.getCache());
		invalidation = Caches.isInvalidationCache(localRegion.getCache());
		synchronous = Caches.isSynchronousCache(localRegion.getCache());

		remoteEnvironment = new NodeEnvironment( ssrb );
		remoteEnvironment.prepare();

		remoteRegion = getRegion(remoteEnvironment);
		remoteAccessStrategy = getAccessStrategy(remoteRegion);

		waitForClusterToForm(localRegion.getCache(), remoteRegion.getCache());
	}

	@After
	public void cleanup() {
		cleanup.forEach(Runnable::run);
		cleanup.clear();
		if (localRegion != null) localRegion.getCache().clear();
		if (remoteRegion != null) remoteRegion.getCache().clear();
	}

	@AfterClassOnce
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
		TestResourceTracker.testFinished(getClass().getSimpleName());
	}

	@Override
	protected StandardServiceRegistryBuilder createStandardServiceRegistryBuilder() {
		StandardServiceRegistryBuilder ssrb = super.createStandardServiceRegistryBuilder();
		ssrb.applySetting(TestInfinispanRegionFactory.TIME_SERVICE, TIME_SERVICE);
		return ssrb;
	}

	/**
	 * Simulate 2 nodes, both start, tx do a get, experience a cache miss, then
	 * 'read from db.' First does a putFromLoad, then an update (or removal if it is a collection).
	 * Second tries to do a putFromLoad with stale data (i.e. it took longer to read from the db).
	 * Both commit their tx. Then both start a new tx and get. First should see
	 * the updated data; second should either see the updated data
	 * (isInvalidation() == false) or null (isInvalidation() == true).
	 *
	 * @param useMinimalAPI
	 * @param isRemoval
	 * @throws Exception
	 */
	protected void putFromLoadTest(final boolean useMinimalAPI, boolean isRemoval) throws Exception {

		final Object KEY = generateNextKey();

		final CountDownLatch writeLatch1 = new CountDownLatch(1);
		final CountDownLatch writeLatch2 = new CountDownLatch(1);
		final CountDownLatch completionLatch = new CountDownLatch(2);

		Thread node1 = new Thread(() -> {
				try {
					SharedSessionContractImplementor session = mockedSession();
					withTx(localEnvironment, session, () -> {
						assertNull(localAccessStrategy.get(session, KEY, session.getTimestamp()));

						writeLatch1.await();

						if (useMinimalAPI) {
							localAccessStrategy.putFromLoad(session, KEY, VALUE1, session.getTimestamp(), 1, true);
						} else {
							localAccessStrategy.putFromLoad(session, KEY, VALUE1, session.getTimestamp(), 1);
						}

						doUpdate(localAccessStrategy, session, KEY, VALUE2, 2);
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
			});

		Thread node2 = new Thread(() -> {
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
			});

		node1.setDaemon(true);
		node2.setDaemon(true);

		CountDownLatch remoteUpdate = expectAfterUpdate();

		node1.start();
		node2.start();

		assertTrue("Threads completed", completionLatch.await(2, TimeUnit.SECONDS));

		assertThreadsRanCleanly();
		assertTrue("Update was replicated", remoteUpdate.await(2, TimeUnit.SECONDS));

		SharedSessionContractImplementor s1 = mockedSession();
		assertEquals( isRemoval ? null : VALUE2, localAccessStrategy.get(s1, KEY, s1.getTimestamp()));
		SharedSessionContractImplementor s2 = mockedSession();
		Object remoteValue = remoteAccessStrategy.get(s2, KEY, s2.getTimestamp());
		if (isUsingInvalidation() || isRemoval) {
			// invalidation command invalidates pending put
			assertNull(remoteValue);
		}
		else {
			// The node1 update is replicated, preventing the node2 PFER
			assertEquals( VALUE2, remoteValue);
		}
	}

	protected CountDownLatch expectAfterUpdate() {
		return expectPutWithValue(value -> value instanceof FutureUpdate);
	}

	protected CountDownLatch expectPutWithValue(Predicate<Object> valuePredicate) {
		if (!isUsingInvalidation() && accessType != AccessType.NONSTRICT_READ_WRITE) {
			CountDownLatch latch = new CountDownLatch(1);
			ExpectingInterceptor.get(remoteRegion.getCache())
				.when((ctx, cmd) -> cmd instanceof PutKeyValueCommand && valuePredicate.test(((PutKeyValueCommand) cmd).getValue()))
				.countDown(latch);
			cleanup.add(() -> ExpectingInterceptor.cleanup(remoteRegion.getCache()));
			return latch;
		} else {
			return new CountDownLatch(0);
		}
	}

	protected CountDownLatch expectPutFromLoad() {
		return expectPutWithValue(value -> value instanceof TombstoneUpdate);
	}

	protected abstract void doUpdate(S strategy, SharedSessionContractImplementor session, Object key, Object value, Object version) throws RollbackException, SystemException;

	private interface SessionMock extends Session, SharedSessionContractImplementor {
	}

	private interface NonJtaTransactionCoordinator extends TransactionCoordinatorOwner, JdbcResourceTransactionAccess {
	}

	protected SharedSessionContractImplementor mockedSession() {
		SessionMock session = mock(SessionMock.class);
		when(session.isClosed()).thenReturn(false);
		when(session.getTimestamp()).thenReturn(TIME_SERVICE.wallClockTime());
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

		CountDownLatch localPutFromLoadLatch = expectRemotePutFromLoad(remoteRegion.getCache(), localRegion.getCache());
		CountDownLatch remotePutFromLoadLatch = expectRemotePutFromLoad(localRegion.getCache(), remoteRegion.getCache());

		SharedSessionContractImplementor s1 = mockedSession();
		assertNull("local is clean", localAccessStrategy.get(s1, KEY, s1.getTimestamp()));
		SharedSessionContractImplementor s2 = mockedSession();
		assertNull("remote is clean", remoteAccessStrategy.get(s2, KEY, s2.getTimestamp()));

		SharedSessionContractImplementor s3 = mockedSession();
		localAccessStrategy.putFromLoad(s3, KEY, VALUE1, s3.getTimestamp(), 1);
		SharedSessionContractImplementor s5 = mockedSession();
		remoteAccessStrategy.putFromLoad(s5, KEY, VALUE1, s5.getTimestamp(), 1);

		// putFromLoad is applied on local node synchronously, but if there's a concurrent update
		// from the other node it can silently fail when acquiring the loc . Then we could try to read
		// before the update is fully applied.
		assertTrue(localPutFromLoadLatch.await(1, TimeUnit.SECONDS));
		assertTrue(remotePutFromLoadLatch.await(1, TimeUnit.SECONDS));

		SharedSessionContractImplementor s4 = mockedSession();
		assertEquals(VALUE1, localAccessStrategy.get(s4, KEY, s4.getTimestamp()));
		SharedSessionContractImplementor s6 = mockedSession();
		assertEquals(VALUE1, remoteAccessStrategy.get(s6, KEY, s6.getTimestamp()));

		SharedSessionContractImplementor session = mockedSession();
		withTx(localEnvironment, session, () -> {
			if (evict) {
				localAccessStrategy.evict(KEY);
			}
			else {
				doRemove(localAccessStrategy, session, KEY);
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

	protected void doRemove(S strategy, SharedSessionContractImplementor session, Object key) throws SystemException, RollbackException {
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

		CountDownLatch localPutFromLoadLatch = expectRemotePutFromLoad(remoteRegion.getCache(), localRegion.getCache());
		CountDownLatch remotePutFromLoadLatch = expectRemotePutFromLoad(localRegion.getCache(), remoteRegion.getCache());

		SharedSessionContractImplementor s3 = mockedSession();
		localAccessStrategy.putFromLoad(s3, KEY, VALUE1, s3.getTimestamp(), 1);
		SharedSessionContractImplementor s5 = mockedSession();
		remoteAccessStrategy.putFromLoad(s5, KEY, VALUE1, s5.getTimestamp(), 1);

		// putFromLoad is applied on local node synchronously, but if there's a concurrent update
		// from the other node it can silently fail when acquiring the loc . Then we could try to read
		// before the update is fully applied.
		assertTrue(localPutFromLoadLatch.await(1, TimeUnit.SECONDS));
		assertTrue(remotePutFromLoadLatch.await(1, TimeUnit.SECONDS));

		SharedSessionContractImplementor s4 = mockedSession();
		SharedSessionContractImplementor s6 = mockedSession();
		assertEquals(VALUE1, localAccessStrategy.get(s4, KEY, s4.getTimestamp()));
		assertEquals(VALUE1, remoteAccessStrategy.get(s6, KEY, s6.getTimestamp()));

		CountDownLatch endInvalidationLatch;
		if (invalidation && !evict) {
			// removeAll causes transactional remove commands which trigger EndInvalidationCommands on the remote side
			// if the cache is non-transactional, PutFromLoadValidator.registerRemoteInvalidations cannot find
			// current session nor register tx synchronization, so it falls back to simple InvalidationCommand.
			endInvalidationLatch = new CountDownLatch(1);
			if (transactional) {
				PutFromLoadValidator originalValidator = PutFromLoadValidator.removeFromCache(remoteRegion.getCache());
				assertEquals(PutFromLoadValidator.class, originalValidator.getClass());
				PutFromLoadValidator mockValidator = spy(originalValidator);
				doAnswer(invocation -> {
					try {
						return invocation.callRealMethod();
					} finally {
						endInvalidationLatch.countDown();
					}
				}).when(mockValidator).endInvalidatingKey(any(), any());
				PutFromLoadValidator.addToCache(remoteRegion.getCache(), mockValidator);
				cleanup.add(() -> {
					PutFromLoadValidator.removeFromCache(remoteRegion.getCache());
					PutFromLoadValidator.addToCache(remoteRegion.getCache(), originalValidator);
				});
			} else {
				ExpectingInterceptor.get(remoteRegion.getCache())
					.when((ctx, cmd) -> cmd instanceof InvalidateCommand)
					.countDown(endInvalidationLatch);
				cleanup.add(() -> ExpectingInterceptor.cleanup(remoteRegion.getCache()));
			}
		} else {
			endInvalidationLatch = new CountDownLatch(0);
		}

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
		SharedSessionContractImplementor s7 = mockedSession();
		assertNull(localAccessStrategy.get(s7, KEY, s7.getTimestamp()));
		assertEquals(0, localRegion.getCache().size());

		SharedSessionContractImplementor s8 = mockedSession();
		assertNull(remoteAccessStrategy.get(s8, KEY, s8.getTimestamp()));
		assertEquals(0, remoteRegion.getCache().size());

		// Wait for async propagation of EndInvalidationCommand before executing naked put
		assertTrue(endInvalidationLatch.await(1, TimeUnit.SECONDS));
		TIME_SERVICE.advance(1);

		CountDownLatch lastPutFromLoadLatch = expectRemotePutFromLoad(remoteRegion.getCache(), localRegion.getCache());

		// Test whether the get above messes up the optimistic version
		SharedSessionContractImplementor s9 = mockedSession();
		assertTrue(remoteAccessStrategy.putFromLoad(s9, KEY, VALUE1, s9.getTimestamp(), 1));
		SharedSessionContractImplementor s10 = mockedSession();
		assertEquals(VALUE1, remoteAccessStrategy.get(s10, KEY, s10.getTimestamp()));
		assertEquals(1, remoteRegion.getCache().size());

		assertTrue(lastPutFromLoadLatch.await(1, TimeUnit.SECONDS));

		SharedSessionContractImplementor s11 = mockedSession();
		assertEquals((isUsingInvalidation() ? null : VALUE1), localAccessStrategy.get(s11, KEY, s11.getTimestamp()));
		SharedSessionContractImplementor s12 = mockedSession();
		assertEquals(VALUE1, remoteAccessStrategy.get(s12, KEY, s12.getTimestamp()));
	}

	private CountDownLatch expectRemotePutFromLoad(AdvancedCache localCache, AdvancedCache remoteCache) {
		CountDownLatch putFromLoadLatch;
		if (!isUsingInvalidation()) {
			putFromLoadLatch = new CountDownLatch(1);
			// The command may fail to replicate if it can't acquire lock locally
			ExpectingInterceptor.Condition remoteCondition = ExpectingInterceptor.get(remoteCache)
				.when((ctx, cmd) -> !ctx.isOriginLocal() && cmd instanceof PutKeyValueCommand);
			ExpectingInterceptor.Condition localCondition = ExpectingInterceptor.get(localCache)
				.whenFails((ctx, cmd) -> ctx.isOriginLocal() && cmd instanceof PutKeyValueCommand);
			remoteCondition.run(() -> {
				localCondition.cancel();
				putFromLoadLatch.countDown();
			});
			localCondition.run(() -> {
				remoteCondition.cancel();
				putFromLoadLatch.countDown();
			});
			// just for case the test fails and does not remove the interceptor itself
			cleanup.add(() -> ExpectingInterceptor.cleanup(localCache, remoteCache));
		} else {
			putFromLoadLatch = new CountDownLatch(0);
		}
		return putFromLoadLatch;
	}
}
