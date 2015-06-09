/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.collection;

import javax.transaction.TransactionManager;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.access.TransactionalAccessDelegate;
import org.hibernate.cache.infinispan.collection.CollectionRegionImpl;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.test.cache.infinispan.AbstractNonFunctionalTestCase;
import org.hibernate.test.cache.infinispan.NodeEnvironment;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.hibernate.test.cache.infinispan.util.TestingKeyFactory;
import org.infinispan.test.CacheManagerCallable;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.infinispan.test.TestingUtil.withCacheManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Base class for tests of CollectionRegionAccessStrategy impls.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractCollectionRegionAccessStrategyTestCase extends AbstractNonFunctionalTestCase {
	private static final Logger log = Logger.getLogger( AbstractCollectionRegionAccessStrategyTestCase.class );
	public static final String REGION_NAME = "test/com.foo.test";
	public static final String KEY_BASE = "KEY";
	public static final String VALUE1 = "VALUE1";
	public static final String VALUE2 = "VALUE2";

	protected static int testCount;

	protected NodeEnvironment localEnvironment;
	protected CollectionRegionImpl localCollectionRegion;
	protected CollectionRegionAccessStrategy localAccessStrategy;

	protected NodeEnvironment remoteEnvironment;
	protected CollectionRegionImpl remoteCollectionRegion;
	protected CollectionRegionAccessStrategy remoteAccessStrategy;

	protected boolean invalidation;
	protected boolean synchronous;

	protected Exception node1Exception;
	protected Exception node2Exception;

	protected AssertionFailedError node1Failure;
	protected AssertionFailedError node2Failure;

	protected abstract AccessType getAccessType();

	@Before
	public void prepareResources() throws Exception {
		// to mimic exactly the old code results, both environments here are exactly the same...
		StandardServiceRegistryBuilder ssrb = createStandardServiceRegistryBuilder( getConfigurationName() );
		localEnvironment = new NodeEnvironment( ssrb );
		localEnvironment.prepare();

		localCollectionRegion = localEnvironment.getCollectionRegion( REGION_NAME, getCacheDataDescription() );
		localAccessStrategy = localCollectionRegion.buildAccessStrategy( getAccessType() );

		invalidation = Caches.isInvalidationCache(localCollectionRegion.getCache());
		synchronous = Caches.isSynchronousCache(localCollectionRegion.getCache());

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		remoteEnvironment = new NodeEnvironment( ssrb );
		remoteEnvironment.prepare();

		remoteCollectionRegion = remoteEnvironment.getCollectionRegion( REGION_NAME, getCacheDataDescription() );
		remoteAccessStrategy = remoteCollectionRegion.buildAccessStrategy( getAccessType() );
	}

	protected abstract String getConfigurationName();

	protected static StandardServiceRegistryBuilder createStandardServiceRegistryBuilder(String configName) {
		final StandardServiceRegistryBuilder ssrb = CacheTestUtil.buildBaselineStandardServiceRegistryBuilder(
				REGION_PREFIX,
				InfinispanRegionFactory.class,
				true,
				false
		);
		ssrb.applySetting( InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, configName );
		return ssrb;
	}

	protected CacheDataDescription getCacheDataDescription() {
		return new CacheDataDescriptionImpl( true, true, ComparableComparator.INSTANCE, null);
	}

	@After
	public void releaseResources() throws Exception {
		if ( localEnvironment != null ) {
			localEnvironment.release();
		}
		if ( remoteEnvironment != null ) {
			remoteEnvironment.release();
		}
	}

	protected boolean isUsingInvalidation() {
		return invalidation;
	}

	protected boolean isSynchronous() {
		return synchronous;
	}

	@Test
	public abstract void testCacheConfiguration();

	@Test
	public void testGetRegion() {
		assertEquals( "Correct region", localCollectionRegion, localAccessStrategy.getRegion() );
	}

	@Test
	public void testPutFromLoadRemoveDoesNotProduceStaleData() throws Exception {
		final CountDownLatch pferLatch = new CountDownLatch( 1 );
		final CountDownLatch removeLatch = new CountDownLatch( 1 );
      final TransactionManager remoteTm = remoteCollectionRegion.getTransactionManager();
      withCacheManager(new CacheManagerCallable(TestCacheManagerFactory.createCacheManager(false)) {
         @Override
         public void call() {
            PutFromLoadValidator validator = new PutFromLoadValidator(remoteCollectionRegion.getCache(), cm,
                  remoteTm, 20000) {
               @Override
               public boolean acquirePutFromLoadLock(Object key) {
                  boolean acquired = super.acquirePutFromLoadLock( key );
                  try {
                     removeLatch.countDown();
                     pferLatch.await( 2, TimeUnit.SECONDS );
                  }
                  catch (InterruptedException e) {
                     log.debug( "Interrupted" );
                     Thread.currentThread().interrupt();
                  }
                  catch (Exception e) {
                     log.error( "Error", e );
                     throw new RuntimeException( "Error", e );
                  }
                  return acquired;
               }
            };

            final TransactionalAccessDelegate delegate =
                  new TransactionalAccessDelegate(localCollectionRegion, validator);
            final TransactionManager localTm = localCollectionRegion.getTransactionManager();

            Callable<Void> pferCallable = new Callable<Void>() {
               public Void call() throws Exception {
                  delegate.putFromLoad( "k1", "v1", 0, null );
                  return null;
               }
            };

            Callable<Void> removeCallable = new Callable<Void>() {
               public Void call() throws Exception {
                  removeLatch.await();
                  Caches.withinTx(localTm, new Callable<Void>() {
                     @Override
                     public Void call() throws Exception {
                        delegate.remove("k1");
                        return null;
                     }
                  });
                  pferLatch.countDown();
                  return null;
               }
            };

            ExecutorService executorService = Executors.newCachedThreadPool();
            Future<Void> pferFuture = executorService.submit( pferCallable );
            Future<Void> removeFuture = executorService.submit( removeCallable );

            try {
               pferFuture.get();
               removeFuture.get();
            } catch (Exception e) {
               throw new RuntimeException(e);
            }

            assertFalse(localCollectionRegion.getCache().containsKey("k1"));
         }
      });
	}

	@Test
	public void testPutFromLoad() throws Exception {
		putFromLoadTest( false );
	}

	@Test
	public void testPutFromLoadMinimal() throws Exception {
		putFromLoadTest( true );
	}

	private void putFromLoadTest(final boolean useMinimalAPI) throws Exception {

		final Object KEY = TestingKeyFactory.generateCollectionCacheKey( KEY_BASE + testCount++ );

		final CountDownLatch writeLatch1 = new CountDownLatch( 1 );
		final CountDownLatch writeLatch2 = new CountDownLatch( 1 );
		final CountDownLatch completionLatch = new CountDownLatch( 2 );

		Thread node1 = new Thread() {

			public void run() {

				try {
					long txTimestamp = System.currentTimeMillis();
					BatchModeTransactionManager.getInstance().begin();

					assertEquals( "node1 starts clean", null, localAccessStrategy.get( KEY, txTimestamp ) );

					writeLatch1.await();

					if ( useMinimalAPI ) {
						localAccessStrategy.putFromLoad( KEY, VALUE2, txTimestamp, new Integer( 2 ), true );
					}
					else {
						localAccessStrategy.putFromLoad( KEY, VALUE2, txTimestamp, new Integer( 2 ) );
					}

					BatchModeTransactionManager.getInstance().commit();
				}
				catch (Exception e) {
					log.error( "node1 caught exception", e );
					node1Exception = e;
					rollback();
				}
				catch (AssertionFailedError e) {
					node1Failure = e;
					rollback();
				}
				finally {
					// Let node2 write
					writeLatch2.countDown();
					completionLatch.countDown();
				}
			}
		};

		Thread node2 = new Thread() {

			public void run() {

				try {
					long txTimestamp = System.currentTimeMillis();
					BatchModeTransactionManager.getInstance().begin();

					assertNull( "node2 starts clean", remoteAccessStrategy.get( KEY, txTimestamp ) );

					// Let node1 write
					writeLatch1.countDown();
					// Wait for node1 to finish
					writeLatch2.await();

					// Let the first PFER propagate
					sleep( 200 );

					if ( useMinimalAPI ) {
						remoteAccessStrategy.putFromLoad( KEY, VALUE1, txTimestamp, new Integer( 1 ), true );
					}
					else {
						remoteAccessStrategy.putFromLoad( KEY, VALUE1, txTimestamp, new Integer( 1 ) );
					}

					BatchModeTransactionManager.getInstance().commit();
				}
				catch (Exception e) {
					log.error( "node2 caught exception", e );
					node2Exception = e;
					rollback();
				}
				catch (AssertionFailedError e) {
					node2Failure = e;
					rollback();
				}
				finally {
					completionLatch.countDown();
				}
			}
		};

		node1.setDaemon( true );
		node2.setDaemon( true );

		node1.start();
		node2.start();

		assertTrue( "Threads completed", completionLatch.await( 2, TimeUnit.SECONDS ) );

		if ( node1Failure != null ) {
			throw node1Failure;
		}
		if ( node2Failure != null ) {
			throw node2Failure;
		}

		assertEquals( "node1 saw no exceptions", null, node1Exception );
		assertEquals( "node2 saw no exceptions", null, node2Exception );

		// let the final PFER propagate
		sleep( 100 );

		long txTimestamp = System.currentTimeMillis();
		String msg1 = "Correct node1 value";
		String msg2 = "Correct node2 value";
		Object expected1 = null;
		Object expected2 = null;
		if ( isUsingInvalidation() ) {
			// PFER does not generate any invalidation, so each node should
			// succeed. We count on database locking and Hibernate removing
			// the collection on any update to prevent the situation we have
			// here where the caches have inconsistent data
			expected1 = VALUE2;
			expected2 = VALUE1;
		}
		else {
			// the initial VALUE2 should prevent the node2 put
			expected1 = VALUE2;
			expected2 = VALUE2;
		}

		assertEquals( msg1, expected1, localAccessStrategy.get( KEY, txTimestamp ) );
		assertEquals( msg2, expected2, remoteAccessStrategy.get( KEY, txTimestamp ) );
	}

	@Test
	public void testRemove() throws Exception {
		evictOrRemoveTest( false );
	}

	@Test
	public void testRemoveAll() throws Exception {
		evictOrRemoveAllTest( false );
	}

	@Test
	public void testEvict() throws Exception {
		evictOrRemoveTest( true );
	}

	@Test
	public void testEvictAll() throws Exception {
		evictOrRemoveAllTest( true );
	}

	private void evictOrRemoveTest(final boolean evict) throws Exception {

		final Object KEY = TestingKeyFactory.generateCollectionCacheKey( KEY_BASE + testCount++ );

		assertNull( "local is clean", localAccessStrategy.get( KEY, System.currentTimeMillis() ) );
		assertNull( "remote is clean", remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		localAccessStrategy.putFromLoad( KEY, VALUE1, System.currentTimeMillis(), new Integer( 1 ) );
		assertEquals( VALUE1, localAccessStrategy.get( KEY, System.currentTimeMillis() ) );
		remoteAccessStrategy.putFromLoad( KEY, VALUE1, System.currentTimeMillis(), new Integer( 1 ) );
		assertEquals( VALUE1, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		// Wait for async propagation
		sleep( 250 );

      Caches.withinTx(localCollectionRegion.getTransactionManager(), new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            if (evict)
               localAccessStrategy.evict(KEY);
            else
               localAccessStrategy.remove(KEY);
            return null;
         }
      });

		assertEquals( null, localAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		assertEquals( null, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );
	}

	private void evictOrRemoveAllTest(final boolean evict) throws Exception {

		final Object KEY = TestingKeyFactory.generateCollectionCacheKey( KEY_BASE + testCount++ );

		assertEquals( 0, getValidKeyCount( localCollectionRegion.getCache().keySet() ) );

		assertEquals( 0, getValidKeyCount( remoteCollectionRegion.getCache().keySet() ) );

		assertNull( "local is clean", localAccessStrategy.get( KEY, System.currentTimeMillis() ) );
		assertNull( "remote is clean", remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		localAccessStrategy.putFromLoad( KEY, VALUE1, System.currentTimeMillis(), new Integer( 1 ) );
		assertEquals( VALUE1, localAccessStrategy.get( KEY, System.currentTimeMillis() ) );
		remoteAccessStrategy.putFromLoad( KEY, VALUE1, System.currentTimeMillis(), new Integer( 1 ) );
		assertEquals( VALUE1, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		// Wait for async propagation
		sleep( 250 );

      Caches.withinTx(localCollectionRegion.getTransactionManager(), new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            if (evict)
               localAccessStrategy.evictAll();
            else
               localAccessStrategy.removeAll();
            return null;
         }
      });

		// This should re-establish the region root node
		assertNull( localAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		assertEquals( 0, getValidKeyCount( localCollectionRegion.getCache().keySet() ) );

		// Re-establishing the region root on the local node doesn't
		// propagate it to other nodes. Do a get on the remote node to re-establish
		assertEquals( null, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		assertEquals( 0, getValidKeyCount( remoteCollectionRegion.getCache().keySet() ) );

		// Test whether the get above messes up the optimistic version
		remoteAccessStrategy.putFromLoad( KEY, VALUE1, System.currentTimeMillis(), new Integer( 1 ) );
		assertEquals( VALUE1, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );

		assertEquals( 1, getValidKeyCount( remoteCollectionRegion.getCache().keySet() ) );

		// Wait for async propagation of the putFromLoad
		sleep( 250 );

		assertEquals(
				"local is correct", (isUsingInvalidation() ? null : VALUE1), localAccessStrategy.get(
				KEY, System
				.currentTimeMillis()
		)
		);
		assertEquals( "remote is correct", VALUE1, remoteAccessStrategy.get( KEY, System.currentTimeMillis() ) );
	}

	private void rollback() {
		try {
			BatchModeTransactionManager.getInstance().rollback();
		}
		catch (Exception e) {
			log.error( e.getMessage(), e );
		}

	}

}
