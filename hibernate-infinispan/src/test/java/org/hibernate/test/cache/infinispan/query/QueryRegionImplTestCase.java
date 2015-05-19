/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.query;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.internal.StandardQueryCache;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.GeneralDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;

import org.hibernate.test.cache.infinispan.AbstractGeneralDataRegionTestCase;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import junit.framework.AssertionFailedError;

import org.infinispan.AdvancedCache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.infinispan.util.concurrent.IsolationLevel;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests of QueryResultRegionImpl.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class QueryRegionImplTestCase extends AbstractGeneralDataRegionTestCase {
	private static final Logger log = Logger.getLogger( QueryRegionImplTestCase.class );

	@Override
	protected Region createRegion(
			InfinispanRegionFactory regionFactory,
			String regionName,
			Properties properties,
			CacheDataDescription cdd) {
		return regionFactory.buildQueryResultsRegion( regionName, properties );
	}

	@Override
	protected String getStandardRegionName(String regionPrefix) {
		return regionPrefix + "/" + StandardQueryCache.class.getName();
	}

   @Override
   protected void regionPut(final GeneralDataRegion region) throws Exception {
      Caches.withinTx(BatchModeTransactionManager.getInstance(), new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            region.put(KEY, VALUE1);
            return null;
         }
      });
   }

   @Override
   protected void regionEvict(final GeneralDataRegion region) throws Exception {
      Caches.withinTx(BatchModeTransactionManager.getInstance(), new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            region.evict(KEY);
            return null;
         }
      });
   }

   @Override
	protected AdvancedCache getInfinispanCache(InfinispanRegionFactory regionFactory) {
		return regionFactory.getCacheManager().getCache( "local-query" ).getAdvancedCache();
	}

	@Override
	protected StandardServiceRegistryBuilder createStandardServiceRegistryBuilder() {
		return CacheTestUtil.buildCustomQueryCacheStandardServiceRegistryBuilder( "test", "replicated-query" );
	}

	private void putDoesNotBlockGetTest() throws Exception {
		StandardServiceRegistryBuilder ssrb = createStandardServiceRegistryBuilder();
		StandardServiceRegistry registry = ssrb.build();
		try {
			final Properties properties = CacheTestUtil.toProperties( ssrb.getSettings() );

			InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
					registry,
					getCacheTestSupport()
			);

			// Sleep a bit to avoid concurrent FLUSH problem
			avoidConcurrentFlush();

			final QueryResultsRegion region = regionFactory.buildQueryResultsRegion(
					getStandardRegionName( REGION_PREFIX ),
					properties
			);

			region.put( KEY, VALUE1 );
			assertEquals( VALUE1, region.get( KEY ) );

			final CountDownLatch readerLatch = new CountDownLatch( 1 );
			final CountDownLatch writerLatch = new CountDownLatch( 1 );
			final CountDownLatch completionLatch = new CountDownLatch( 1 );
			final ExceptionHolder holder = new ExceptionHolder();

			Thread reader = new Thread() {
				@Override
				public void run() {
					try {
						BatchModeTransactionManager.getInstance().begin();
						log.debug( "Transaction began, get value for key" );
						assertTrue( VALUE2.equals( region.get( KEY ) ) == false );
						BatchModeTransactionManager.getInstance().commit();
					}
					catch (AssertionFailedError e) {
						holder.a1 = e;
						rollback();
					}
					catch (Exception e) {
						holder.e1 = e;
						rollback();
					}
					finally {
						readerLatch.countDown();
					}
				}
			};

			Thread writer = new Thread() {
				@Override
				public void run() {
					try {
						BatchModeTransactionManager.getInstance().begin();
						log.debug( "Put value2" );
						region.put( KEY, VALUE2 );
						log.debug( "Put finished for value2, await writer latch" );
						writerLatch.await();
						log.debug( "Writer latch finished" );
						BatchModeTransactionManager.getInstance().commit();
						log.debug( "Transaction committed" );
					}
					catch (Exception e) {
						holder.e2 = e;
						rollback();
					}
					finally {
						completionLatch.countDown();
					}
				}
			};

			reader.setDaemon( true );
			writer.setDaemon( true );

			writer.start();
			assertFalse( "Writer is blocking", completionLatch.await( 100, TimeUnit.MILLISECONDS ) );

			// Start the reader
			reader.start();
			assertTrue( "Reader finished promptly", readerLatch.await( 1000000000, TimeUnit.MILLISECONDS ) );

			writerLatch.countDown();
			assertTrue( "Reader finished promptly", completionLatch.await( 100, TimeUnit.MILLISECONDS ) );

			assertEquals( VALUE2, region.get( KEY ) );

			if ( holder.a1 != null ) {
				throw holder.a1;
			}
			else if ( holder.a2 != null ) {
				throw holder.a2;
			}

			assertEquals( "writer saw no exceptions", null, holder.e1 );
			assertEquals( "reader saw no exceptions", null, holder.e2 );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	public void testGetDoesNotBlockPut() throws Exception {
		getDoesNotBlockPutTest();
	}

	private void getDoesNotBlockPutTest() throws Exception {
		StandardServiceRegistryBuilder ssrb = createStandardServiceRegistryBuilder();
		StandardServiceRegistry registry = ssrb.build();
		try {
			final Properties properties = CacheTestUtil.toProperties( ssrb.getSettings() );
			InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
					registry,
					getCacheTestSupport()
			);

			// Sleep a bit to avoid concurrent FLUSH problem
			avoidConcurrentFlush();

			final QueryResultsRegion region = regionFactory.buildQueryResultsRegion(
					getStandardRegionName( REGION_PREFIX ),
					properties
			);

			region.put( KEY, VALUE1 );
			assertEquals( VALUE1, region.get( KEY ) );

			// final Fqn rootFqn = getRegionFqn(getStandardRegionName(REGION_PREFIX), REGION_PREFIX);
			final AdvancedCache jbc = getInfinispanCache(regionFactory);

			final CountDownLatch blockerLatch = new CountDownLatch( 1 );
			final CountDownLatch writerLatch = new CountDownLatch( 1 );
			final CountDownLatch completionLatch = new CountDownLatch( 1 );
			final ExceptionHolder holder = new ExceptionHolder();

			Thread blocker = new Thread() {

				@Override
				public void run() {
					// Fqn toBlock = new Fqn(rootFqn, KEY);
					GetBlocker blocker = new GetBlocker( blockerLatch, KEY );
					try {
						jbc.addListener( blocker );

						BatchModeTransactionManager.getInstance().begin();
						region.get( KEY );
						BatchModeTransactionManager.getInstance().commit();
					}
					catch (Exception e) {
						holder.e1 = e;
						rollback();
					}
					finally {
						jbc.removeListener( blocker );
					}
				}
			};

			Thread writer = new Thread() {

				@Override
				public void run() {
					try {
						writerLatch.await();

						BatchModeTransactionManager.getInstance().begin();
						region.put( KEY, VALUE2 );
						BatchModeTransactionManager.getInstance().commit();
					}
					catch (Exception e) {
						holder.e2 = e;
						rollback();
					}
					finally {
						completionLatch.countDown();
					}
				}
			};

			blocker.setDaemon( true );
			writer.setDaemon( true );

			boolean unblocked = false;
			try {
				blocker.start();
				writer.start();

				assertFalse( "Blocker is blocking", completionLatch.await( 100, TimeUnit.MILLISECONDS ) );
				// Start the writer
				writerLatch.countDown();
				assertTrue( "Writer finished promptly", completionLatch.await( 100, TimeUnit.MILLISECONDS ) );

				blockerLatch.countDown();
				unblocked = true;

				if ( IsolationLevel.REPEATABLE_READ.equals( jbc.getCacheConfiguration().locking().isolationLevel() ) ) {
					assertEquals( VALUE1, region.get( KEY ) );
				}
				else {
					assertEquals( VALUE2, region.get( KEY ) );
				}

				if ( holder.a1 != null ) {
					throw holder.a1;
				}
				else if ( holder.a2 != null ) {
					throw holder.a2;
				}

				assertEquals( "blocker saw no exceptions", null, holder.e1 );
				assertEquals( "writer saw no exceptions", null, holder.e2 );
			}
			finally {
				if ( !unblocked ) {
					blockerLatch.countDown();
				}
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Listener
	public class GetBlocker {

		private CountDownLatch latch;
		// private Fqn fqn;
		private Object key;

		GetBlocker(
				CountDownLatch latch,
				Object key
		) {
			this.latch = latch;
			this.key = key;
		}

		@CacheEntryVisited
		public void nodeVisisted(CacheEntryVisitedEvent event) {
			if ( event.isPre() && event.getKey().equals( key ) ) {
				try {
					latch.await();
				}
				catch (InterruptedException e) {
					log.error( "Interrupted waiting for latch", e );
				}
			}
		}
	}

	private class ExceptionHolder {
		Exception e1;
		Exception e2;
		AssertionFailedError a1;
		AssertionFailedError a2;
	}
}
