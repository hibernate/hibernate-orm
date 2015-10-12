/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
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
package org.hibernate.test.cache.infinispan.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;
import org.hibernate.testing.TestForIssue;
import org.infinispan.AdvancedCache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.logging.Logger;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.internal.StandardQueryCache;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.GeneralDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.test.cache.infinispan.AbstractGeneralDataRegionTestCase;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.junit.Test;

import static org.hibernate.cache.infinispan.util.Caches.withinTx;
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
	private final BatchModeTransactionManager tm = BatchModeTransactionManager.getInstance();

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
	protected void regionPut(final GeneralDataRegion region, final String key, final String value) throws Exception {
		withinTx(tm, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				region.put(key, value);
				return null;
			}
		});
	}

	@Override
	protected void regionEvict(final GeneralDataRegion region, final String key) throws Exception {
		withinTx(tm, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				region.evict(key);
				return null;
			}
		});
	}

	@Override
	protected Object regionGet(final GeneralDataRegion region, final String key) throws Exception {
		return withinTx(tm, new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return region.get(key);
			}
		});
	}

	@Override
	protected AdvancedCache getInfinispanCache(InfinispanRegionFactory regionFactory) {
		return regionFactory.getCacheManager().getCache( getStandardRegionName( REGION_PREFIX ) ).getAdvancedCache();
	}

	@Override
	protected Configuration createConfiguration() {
		return CacheTestUtil.buildCustomQueryCacheConfiguration( "test", "replicated-query" );
	}

	@Test
	public void testPutDoesNotBlockGet() throws Exception {
		Configuration cfg = createConfiguration();
		InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
				new ServiceRegistryBuilder().applySettings( cfg.getProperties() ).buildServiceRegistry(),
				cfg,
				getCacheTestSupport()
		);

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		final QueryResultsRegion region = regionFactory.buildQueryResultsRegion(
				getStandardRegionName( REGION_PREFIX ),
				cfg.getProperties()
		);

		regionPut(region, KEY, VALUE1);
		assertEquals( VALUE1, region.get( KEY ) );

		final CountDownLatch readerLatch = new CountDownLatch( 1 );
		final CountDownLatch writerLatch = new CountDownLatch( 1 );
		final CountDownLatch completionLatch = new CountDownLatch( 1 );
		final ExceptionHolder holder = new ExceptionHolder();

		Thread reader = new Thread() {
			@Override
			public void run() {
				try {
					withinTx(tm, new Callable() {
						@Override
						public Object call() throws Exception {
							assertTrue( VALUE2.equals( region.get( KEY ) ) == false );
							return null;
						}
					});
				}
				catch (AssertionFailedError e) {
					holder.addAssertionFailure(e);
				}
				catch (Exception e) {
					holder.addException(e);
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
					withinTx(tm, new Callable() {
						@Override
						public Object call() throws Exception {
							region.put( KEY, VALUE2 );
							writerLatch.await();
							return null;
						}
					});
				}
				catch (Exception e) {
					holder.addException(e);
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
		assertTrue( "Reader finished promptly", readerLatch.await( 100, TimeUnit.MILLISECONDS ) );

		writerLatch.countDown();
		assertTrue( "Reader finished promptly", completionLatch.await( 100, TimeUnit.MILLISECONDS ) );

		assertEquals( VALUE2, region.get( KEY ) );

		holder.checkExceptions();
	}

	@Test
	public void testGetDoesNotBlockPut() throws Exception {
		Configuration cfg = createConfiguration();
		InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
				new ServiceRegistryBuilder().applySettings( cfg.getProperties() ).buildServiceRegistry(),
				cfg,
				getCacheTestSupport()
		);

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		final QueryResultsRegion region = regionFactory.buildQueryResultsRegion(
				getStandardRegionName( REGION_PREFIX ),
				cfg.getProperties()
		);

		regionPut(region, KEY, VALUE1);
		assertEquals( VALUE1, region.get( KEY ) );

		// final Fqn rootFqn = getRegionFqn(getStandardRegionName(REGION_PREFIX), REGION_PREFIX);
		final AdvancedCache cache = getInfinispanCache(regionFactory);
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
					cache.addListener( blocker );
					withinTx(tm, new Callable() {
						@Override
						public Object call() throws Exception {
							return region.get( KEY );
						}
					});
				}
				catch (Exception e) {
					holder.addException(e);
				}
				finally {
					cache.removeListener( blocker );
				}
			}
		};

		Thread writer = new Thread() {

			@Override
			public void run() {
				try {
					writerLatch.await();
					regionPut(region, KEY, VALUE2);
				}
				catch (Exception e) {
					holder.addException(e);
				}
				finally {
					completionLatch.countDown();
				}
			}
		};

		blocker.setDaemon( true );
		writer.setDaemon( true );

		try {
			blocker.start();
			writer.start();

			assertFalse( "Blocker is blocking", completionLatch.await( 100, TimeUnit.MILLISECONDS ) );
			// Start the writer
			writerLatch.countDown();
			assertTrue( "Writer finished promptly", completionLatch.await( 100, TimeUnit.MILLISECONDS ) );

			blockerLatch.countDown();

			if ( IsolationLevel.REPEATABLE_READ.equals( cache.getCacheConfiguration().locking().isolationLevel() ) ) {
				assertEquals( VALUE1, region.get( KEY ) );
			}
			else {
				assertEquals( VALUE2, region.get( KEY ) );
			}

			holder.checkExceptions();
		}
		finally {
			blockerLatch.countDown();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7898")
	public void testPutDuringPut() throws Exception {
		Configuration cfg = createConfiguration();
		InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
				new ServiceRegistryBuilder().applySettings( cfg.getProperties() ).buildServiceRegistry(),
				cfg,
				getCacheTestSupport()
		);

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		final QueryResultsRegion region = regionFactory.buildQueryResultsRegion(
				getStandardRegionName( REGION_PREFIX ),
				cfg.getProperties()
		);

		regionPut(region, KEY, VALUE1);
		assertEquals( VALUE1, region.get( KEY ) );

		final AdvancedCache cache = getInfinispanCache(regionFactory);
		final CountDownLatch blockerLatch = new CountDownLatch(1);
		final CountDownLatch triggerLatch = new CountDownLatch(1);
		final ExceptionHolder holder = new ExceptionHolder();

		Thread blocking = new Thread() {
			@Override
			public void run() {
				PutBlocker blocker = null;
				try {
					blocker = new PutBlocker(blockerLatch, triggerLatch, KEY);
					cache.addListener(blocker);
					regionPut(region, KEY, VALUE2);
				} catch (Exception e) {
					holder.addException(e);
				} finally {
					if (blocker != null) {
						cache.removeListener(blocker);
					}
					if (triggerLatch.getCount() > 0) {
						triggerLatch.countDown();
					}
				}
			}
		};

		Thread blocked = new Thread() {
			@Override
			public void run() {
				try {
					triggerLatch.await();
					// this should silently fail
					regionPut(region, KEY, VALUE3);
				} catch (Exception e) {
					holder.addException(e);
				}
			}
		};

		blocking.setName("blocking-thread");
		blocking.start();
		blocked.setName("blocked-thread");
		blocked.start();
		blocked.join();
		blockerLatch.countDown();
		blocking.join();

		holder.checkExceptions();

		assertEquals(VALUE2, region.get(KEY));
	}

	@Test
	public void testQueryUpdate() throws Exception {
		Configuration cfg = createConfiguration();
		InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
				new ServiceRegistryBuilder().applySettings( cfg.getProperties() ).buildServiceRegistry(),
				cfg,
				getCacheTestSupport()
		);

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		final QueryResultsRegion region = regionFactory.buildQueryResultsRegion(
				getStandardRegionName( REGION_PREFIX ),
				cfg.getProperties()
		);

		final ExceptionHolder holder = new ExceptionHolder();
		final CyclicBarrier barrier = new CyclicBarrier(2);
		regionPut(region, KEY, VALUE1);

		Thread updater = new Thread() {
			@Override
			public void run() {
				try {
					withinTx(tm, new Callable<Void>() {
						@Override						
						public Void call() throws Exception {
							assertEquals(VALUE1, region.get(KEY));
							region.put(KEY, VALUE2);
							assertEquals(VALUE2, region.get(KEY));
							barrier.await(5, TimeUnit.SECONDS);
							barrier.await(5, TimeUnit.SECONDS);
							region.put(KEY, VALUE3);
							assertEquals(VALUE3, region.get(KEY));
							barrier.await(5, TimeUnit.SECONDS);
							barrier.await(5, TimeUnit.SECONDS);
							return null;
						}
					});
				} catch (AssertionFailedError e) {
					holder.addAssertionFailure(e);
					barrier.reset();
				} catch (Exception e) {
					holder.addException(e);
					barrier.reset();
				}
			}
		};

		Thread reader = new Thread() {
			@Override
			public void run() {
				try {
					withinTx(tm, new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							assertEquals(VALUE1, region.get(KEY));
							barrier.await(5, TimeUnit.SECONDS);
							assertEquals(VALUE1, region.get(KEY));
							barrier.await(5, TimeUnit.SECONDS);
							barrier.await(5, TimeUnit.SECONDS);
							assertEquals(VALUE1, region.get(KEY));
							barrier.await(5, TimeUnit.SECONDS);
							return null;
						}
					});
				} catch (AssertionFailedError e) {
					holder.addAssertionFailure(e);
					barrier.reset();
				} catch (Exception e) {
					holder.addException(e);
					barrier.reset();
				}
			}
		};

		updater.start();
		reader.start();
		updater.join();
		reader.join();
		holder.checkExceptions();

		assertEquals(VALUE3, regionGet(region, KEY));
	}

	@Listener
	public class GetBlocker {
		private final CountDownLatch latch;
		private final Object key;

		GetBlocker(CountDownLatch latch,	Object key) {
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

	@Listener
	public class PutBlocker {
		private final CountDownLatch blockLatch, triggerLatch;
		private final Object key;
		private boolean enabled = true;

		PutBlocker(CountDownLatch blockLatch, CountDownLatch triggerLatch, Object key) {
			this.blockLatch = blockLatch;
			this.triggerLatch = triggerLatch;
			this.key = key;
		}

		@CacheEntryModified
		public void nodeVisisted(CacheEntryModifiedEvent event) {
			// we need isPre since lock is acquired in the commit phase
			if ( !event.isPre() && event.getKey().equals( key ) ) {
				try {
					boolean shouldBlock = false;
					synchronized (this) {
						if (enabled) {
							shouldBlock = true;
							triggerLatch.countDown();
							enabled = false;
						}
					}
					if (shouldBlock) {
						blockLatch.await();
					}
				}
				catch (InterruptedException e) {
					log.error( "Interrupted waiting for latch", e );
				}
			}
		}
	}

	private class ExceptionHolder {
		private final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
		private final List<AssertionFailedError> assertionFailures = Collections.synchronizedList(new ArrayList<AssertionFailedError>());

		public void addException(Exception e) {
			exceptions.add(e);
		}

		public void addAssertionFailure(AssertionFailedError e) {
			assertionFailures.add(e);
		}

		public void checkExceptions() throws Exception {
			for (AssertionFailedError a : assertionFailures) {
				throw a;
			}
			for (Exception e : exceptions) {
				throw e;
			}
		}
	}
}
