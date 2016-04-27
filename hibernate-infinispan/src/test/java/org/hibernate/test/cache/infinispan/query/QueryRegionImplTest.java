/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.query.QueryResultsRegionImpl;
import org.hibernate.cache.internal.StandardQueryCache;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.cache.infinispan.AbstractGeneralDataRegionTest;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.junit.Test;
import junit.framework.AssertionFailedError;

import org.infinispan.AdvancedCache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.infinispan.util.concurrent.IsolationLevel;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests of QueryResultRegionImpl.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class QueryRegionImplTest extends AbstractGeneralDataRegionTest {
	private static final Logger log = Logger.getLogger( QueryRegionImplTest.class );

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
	protected AdvancedCache getInfinispanCache(InfinispanRegionFactory regionFactory) {
		return regionFactory.getCacheManager().getCache( getStandardRegionName( REGION_PREFIX ) ).getAdvancedCache();
	}

	@Override
	protected StandardServiceRegistryBuilder createStandardServiceRegistryBuilder() {
		return CacheTestUtil.buildCustomQueryCacheStandardServiceRegistryBuilder( REGION_PREFIX, "replicated-query", jtaPlatform );
	}

	private interface RegionConsumer {
		void accept(SessionFactory sessionFactory, QueryResultsRegion region) throws Exception;
	}

	private void withQueryRegion(RegionConsumer callable) throws Exception {
		withSessionFactoriesAndRegions(1, (sessionFactories, regions) ->  callable.accept(sessionFactories.get(0), (QueryResultsRegion) regions.get(0)));
	}

	@Test
	public void testPutDoesNotBlockGet() throws Exception {
		withQueryRegion((sessionFactory, region) -> {
			withSession(sessionFactory, session -> region.put(session, KEY, VALUE1));
			assertEquals(VALUE1, callWithSession(sessionFactory, session -> region.get(session, KEY)));

			final CountDownLatch readerLatch = new CountDownLatch(1);
			final CountDownLatch writerLatch = new CountDownLatch(1);
			final CountDownLatch completionLatch = new CountDownLatch(1);
			final ExceptionHolder holder = new ExceptionHolder();

			Thread reader = new Thread() {
				@Override
				public void run() {
					try {
						assertNotEquals(VALUE2, callWithSession(sessionFactory, session -> region.get(session, KEY)));
					} catch (AssertionFailedError e) {
						holder.addAssertionFailure(e);
					} catch (Exception e) {
						holder.addException(e);
					} finally {
						readerLatch.countDown();
					}
				}
			};

			Thread writer = new Thread() {
				@Override
				public void run() {
					try {
						withSession(sessionFactory, session -> {
							region.put(session, KEY, VALUE2);
							writerLatch.await();
						});
					} catch (Exception e) {
						holder.addException(e);
					} finally {
						completionLatch.countDown();
					}
				}
			};

			reader.setDaemon(true);
			writer.setDaemon(true);

			writer.start();
			assertFalse("Writer is blocking", completionLatch.await(100, TimeUnit.MILLISECONDS));

			// Start the reader
			reader.start();
			assertTrue("Reader finished promptly", readerLatch.await(100, TimeUnit.MILLISECONDS));

			writerLatch.countDown();

			assertTrue("Reader finished promptly", completionLatch.await(100, TimeUnit.MILLISECONDS));

			assertEquals(VALUE2, callWithSession(sessionFactory, session -> region.get(session, KEY)));
		});
	}

	@Test
	public void testGetDoesNotBlockPut() throws Exception {
		withQueryRegion((sessionFactory, region) -> {
			withSession(sessionFactory, session -> region.put( session, KEY, VALUE1 ));
			assertEquals(VALUE1, callWithSession(sessionFactory, session -> region.get( session, KEY )));

			final AdvancedCache cache = ((QueryResultsRegionImpl) region).getCache();
			final CountDownLatch blockerLatch = new CountDownLatch( 1 );
			final CountDownLatch writerLatch = new CountDownLatch( 1 );
			final CountDownLatch completionLatch = new CountDownLatch( 1 );
			final ExceptionHolder holder = new ExceptionHolder();

			Thread reader = new Thread() {
				@Override
				public void run() {
					GetBlocker blocker = new GetBlocker( blockerLatch, KEY );
					try {
						cache.addListener( blocker );
						withSession(sessionFactory, session -> region.get(session, KEY ));
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
						withSession(sessionFactory, session -> region.put( session, KEY, VALUE2 ));
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

			boolean unblocked = false;
			try {
				reader.start();
				writer.start();

				assertFalse( "Reader is blocking", completionLatch.await( 100, TimeUnit.MILLISECONDS ) );
				// Start the writer
				writerLatch.countDown();
				assertTrue( "Writer finished promptly", completionLatch.await( 100, TimeUnit.MILLISECONDS ) );

				blockerLatch.countDown();
				unblocked = true;

				if ( IsolationLevel.REPEATABLE_READ.equals( cache.getCacheConfiguration().locking().isolationLevel() ) ) {
					assertEquals( VALUE1, callWithSession(sessionFactory, session -> region.get( session, KEY )) );
				}
				else {
					assertEquals( VALUE2, callWithSession(sessionFactory, session -> region.get( session, KEY )) );
				}

				holder.checkExceptions();
			}
			finally {
				if ( !unblocked ) {
					blockerLatch.countDown();
				}
			}
		});
	}

	protected interface SessionConsumer {
		void accept(SharedSessionContractImplementor session) throws Exception;
	}

	protected interface SessionCallable<T> {
		T call(SharedSessionContractImplementor session) throws Exception;
	}

	protected <T> T callWithSession(SessionFactory sessionFactory, SessionCallable<T> callable) throws Exception {
		Session session = sessionFactory.openSession();
		Transaction tx = session.getTransaction();
		tx.begin();
		try {
			T retval = callable.call((SharedSessionContractImplementor) session);
			tx.commit();
			return retval;
		} catch (Exception e) {
			tx.rollback();
			throw e;
		} finally {
			session.close();
		}
	}

	protected void withSession(SessionFactory sessionFactory, SessionConsumer consumer) throws Exception {
		callWithSession(sessionFactory, session -> { consumer.accept(session); return null;} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7898")
	public void testPutDuringPut() throws Exception {
		withQueryRegion((sessionFactory, region) -> {
			withSession(sessionFactory, session -> region.put(session, KEY, VALUE1));
			assertEquals(VALUE1, callWithSession(sessionFactory, session -> region.get(session, KEY) ));

			final AdvancedCache cache = ((QueryResultsRegionImpl) region).getCache();
			CountDownLatch blockerLatch = new CountDownLatch(1);
			CountDownLatch triggerLatch = new CountDownLatch(1);
			ExceptionHolder holder = new ExceptionHolder();

			Thread blocking = new Thread() {
				@Override
				public void run() {
					PutBlocker blocker = null;
					try {
						blocker = new PutBlocker(blockerLatch, triggerLatch, KEY);
						cache.addListener(blocker);
						withSession(sessionFactory, session -> region.put(session, KEY, VALUE2));
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
						withSession(sessionFactory, session -> region.put(session, KEY, VALUE3));
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

			assertEquals(VALUE2, callWithSession(sessionFactory, session -> region.get(session, KEY)));
		});
	}

	@Test
	public void testQueryUpdate() throws Exception {
		withQueryRegion((sessionFactory, region) -> {
			ExceptionHolder holder = new ExceptionHolder();
			CyclicBarrier barrier = new CyclicBarrier(2);
			withSession(sessionFactory, session -> region.put(session, KEY, VALUE1));

			Thread updater = new Thread() {
				@Override
				public void run() {
					try {
						withSession(sessionFactory, (session) -> {
							assertEquals(VALUE1, region.get(session, KEY));
							region.put(session, KEY, VALUE2);
							assertEquals(VALUE2, region.get(session, KEY));
							barrier.await(5, TimeUnit.SECONDS);
							barrier.await(5, TimeUnit.SECONDS);
							region.put(session, KEY, VALUE3);
							assertEquals(VALUE3, region.get(session, KEY));
							barrier.await(5, TimeUnit.SECONDS);
							barrier.await(5, TimeUnit.SECONDS);
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
						withSession(sessionFactory, (session) -> {
							assertEquals(VALUE1, region.get(session, KEY));
							barrier.await(5, TimeUnit.SECONDS);
							assertEquals(VALUE1, region.get(session, KEY));
							barrier.await(5, TimeUnit.SECONDS);
							barrier.await(5, TimeUnit.SECONDS);
							assertEquals(VALUE1, region.get(session, KEY));
							barrier.await(5, TimeUnit.SECONDS);
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

			assertEquals(VALUE3, callWithSession(sessionFactory, session -> region.get(session, KEY)));
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10163")
	public void testEvictAll() throws Exception {
		withQueryRegion((sessionFactory, region) -> {
			withSession(sessionFactory, s -> region.put(s, KEY, VALUE1));
			withSession(sessionFactory, s -> assertEquals(VALUE1, region.get(s, KEY)));
			region.evictAll();
			withSession(sessionFactory, s -> assertNull(region.get(s, KEY)));
			assertEquals(Collections.EMPTY_MAP, region.toMap());
		});
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
					synchronized (this) {
						if (enabled) {
							triggerLatch.countDown();
							enabled = false;
							blockLatch.await();
						}
					}
				}
				catch (InterruptedException e) {
					log.error( "Interrupted waiting for latch", e );
				}
			}
		}
	}

	private class ExceptionHolder {
		private final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());
		private final List<AssertionFailedError> assertionFailures = Collections.synchronizedList(new ArrayList<>());

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
