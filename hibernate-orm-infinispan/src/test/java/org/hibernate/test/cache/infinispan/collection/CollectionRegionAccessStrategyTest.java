/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.collection;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.cache.infinispan.access.AccessDelegate;
import org.hibernate.cache.infinispan.access.NonTxInvalidationCacheAccessDelegate;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.access.TxInvalidationCacheAccessDelegate;
import org.hibernate.cache.infinispan.collection.CollectionRegionImpl;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.test.cache.infinispan.AbstractRegionAccessStrategyTest;
import org.hibernate.test.cache.infinispan.NodeEnvironment;
import org.hibernate.test.cache.infinispan.util.TestSynchronization;
import org.hibernate.test.cache.infinispan.util.TestingKeyFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/**
 * Base class for tests of CollectionRegionAccessStrategy impls.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class CollectionRegionAccessStrategyTest extends
		AbstractRegionAccessStrategyTest<CollectionRegionImpl, CollectionRegionAccessStrategy> {
	protected static int testCount;

	@Override
	protected Object generateNextKey() {
		return TestingKeyFactory.generateCollectionCacheKey( KEY_BASE + testCount++ );
	}

	@Override
	protected CollectionRegionImpl getRegion(NodeEnvironment environment) {
		return environment.getCollectionRegion( REGION_NAME, CACHE_DATA_DESCRIPTION );
	}

	@Override
	protected CollectionRegionAccessStrategy getAccessStrategy(CollectionRegionImpl region) {
		return region.buildAccessStrategy( accessType );
	}

	@Test
	public void testGetRegion() {
		assertEquals( "Correct region", localRegion, localAccessStrategy.getRegion() );
	}

	@Test
	public void testPutFromLoadRemoveDoesNotProduceStaleData() throws Exception {
		if (!cacheMode.isInvalidation()) {
			return;
		}
		final CountDownLatch pferLatch = new CountDownLatch( 1 );
		final CountDownLatch removeLatch = new CountDownLatch( 1 );
		// remove the interceptor inserted by default PutFromLoadValidator, we're using different one
		PutFromLoadValidator originalValidator = PutFromLoadValidator.removeFromCache(localRegion.getCache());
		PutFromLoadValidator mockValidator = spy(originalValidator);
		doAnswer(invocation -> {
			try {
				return invocation.callRealMethod();
			} finally {
				try {
					removeLatch.countDown();
					// the remove should be blocked because the putFromLoad has been acquired
					// and the remove can continue only after we've inserted the entry
					assertFalse(pferLatch.await( 2, TimeUnit.SECONDS ) );
				}
				catch (InterruptedException e) {
					log.debug( "Interrupted" );
					Thread.currentThread().interrupt();
				}
				catch (Exception e) {
					log.error( "Error", e );
					throw new RuntimeException( "Error", e );
				}
			}
		}).when(mockValidator).acquirePutFromLoadLock(any(), any(), anyLong());
		PutFromLoadValidator.addToCache(localRegion.getCache(), mockValidator);
		cleanup.add(() -> {
			PutFromLoadValidator.removeFromCache(localRegion.getCache());
			PutFromLoadValidator.addToCache(localRegion.getCache(), originalValidator);
		});

		final AccessDelegate delegate = localRegion.getCache().getCacheConfiguration().transaction().transactionMode().isTransactional() ?
			new TxInvalidationCacheAccessDelegate(localRegion, mockValidator) :
			new NonTxInvalidationCacheAccessDelegate(localRegion, mockValidator);

		ExecutorService executorService = Executors.newCachedThreadPool();
		cleanup.add(() -> executorService.shutdownNow());

		final String KEY = "k1";
		Future<Void> pferFuture = executorService.submit(() -> {
			SharedSessionContractImplementor session = mockedSession();
			delegate.putFromLoad(session, KEY, "v1", session.getTimestamp(), null);
			return null;
		});

		Future<Void> removeFuture = executorService.submit(() -> {
			removeLatch.await();
			SharedSessionContractImplementor session = mockedSession();
			withTx(localEnvironment, session, () -> {
				delegate.remove(session, KEY);
				return null;
			});
			pferLatch.countDown();
			return null;
		});

		pferFuture.get();
		removeFuture.get();

		assertFalse(localRegion.getCache().containsKey(KEY));
		assertFalse(remoteRegion.getCache().containsKey(KEY));
	}

	@Test
	public void testPutFromLoad() throws Exception {
		putFromLoadTest(false, true);
	}

	@Test
	public void testPutFromLoadMinimal() throws Exception {
		putFromLoadTest(true, true);
	}

	@Override
	protected void doUpdate(CollectionRegionAccessStrategy strategy, SharedSessionContractImplementor session, Object key, Object value, Object version) throws javax.transaction.RollbackException, javax.transaction.SystemException {
		SoftLock softLock = strategy.lockItem(session, key, version);
		strategy.remove(session, key);
		session.getTransactionCoordinator().getLocalSynchronizations().registerSynchronization(
			new TestSynchronization.UnlockItem(strategy, session, key, softLock));
	}
}
