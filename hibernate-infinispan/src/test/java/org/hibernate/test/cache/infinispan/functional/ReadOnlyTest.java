/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.PessimisticLockException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.spi.Region;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.test.cache.infinispan.functional.entities.Item;
import org.hibernate.testing.TestForIssue;
import org.infinispan.AdvancedCache;
import org.infinispan.commands.read.GetKeyValueCommand;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Parent tests for both transactional and
 * read-only tests are defined in this class.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
public class ReadOnlyTest extends SingleNodeTest {
	static final Log log = LogFactory.getLog(ReadOnlyTest.class);

	@Override
	public List<Object[]> getParameters() {
		return getParameters(false, false, true);
	}

	@Test
	public void testEmptySecondLevelCacheEntry() throws Exception {
		sessionFactory().getCache().evictCollectionRegion( Item.class.getName() + ".items" );
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );
		Map cacheEntries = statistics.getEntries();
		assertEquals( 0, cacheEntries.size() );
	}

	@Test
	public void testInsertDeleteEntity() throws Exception {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final Item item = new Item( "chris", "Chris's Item" );
		withTxSession(s -> s.persist(item));

		log.info("Entry persisted, let's load and delete it.");

		withTxSession(s -> {
			Item found = s.load(Item.class, item.getId());
			log.info(stats.toString());
			assertEquals(item.getDescription(), found.getDescription());
			assertEquals(0, stats.getSecondLevelCacheMissCount());
			assertEquals(1, stats.getSecondLevelCacheHitCount());
			s.delete(found);
		});
	}

	@Test
	public void testInsertClearCacheDeleteEntity() throws Exception {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		final Item item = new Item( "chris", "Chris's Item" );
		withTxSession(s -> s.persist(item));
		assertEquals(0, stats.getSecondLevelCacheMissCount());
		assertEquals(0, stats.getSecondLevelCacheHitCount());
		assertEquals(1, stats.getSecondLevelCachePutCount());

		log.info("Entry persisted, let's load and delete it.");

		cleanupCache();
		Thread.sleep(10);

		withTxSession(s -> {
			Item found = s.load(Item.class, item.getId());
			log.info(stats.toString());
			assertEquals(item.getDescription(), found.getDescription());
			assertEquals(1, stats.getSecondLevelCacheMissCount());
			assertEquals(0, stats.getSecondLevelCacheHitCount());
			assertEquals(2, stats.getSecondLevelCachePutCount());
			s.delete(found);
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9868")
	public void testConcurrentRemoveAndPutFromLoad() throws Exception {
		if (cacheMode.isDistributed() || cacheMode.isReplicated()) {
			// this test is tailored for invalidation-based access strategies
			return;
		}

		Region region = sessionFactory().getSecondLevelCacheRegion(Item.class.getName());
		AdvancedCache entityCache = ((EntityRegionImpl) region).getCache();

		final Item item = new Item( "chris", "Chris's Item" );
		withTxSession(s -> {
			s.persist(item);
		});

		Phaser deletePhaser = new Phaser(2);
		Phaser getPhaser = new Phaser(2);
		HookInterceptor hook = new HookInterceptor();

		AdvancedCache pendingPutsCache = entityCache.getCacheManager().getCache(
				entityCache.getName() + "-" + InfinispanRegionFactory.PENDING_PUTS_CACHE_NAME).getAdvancedCache();
		pendingPutsCache.addInterceptor(hook, 0);
		AtomicBoolean getThreadBlockedInDB = new AtomicBoolean(false);

		Thread deleteThread = new Thread(() -> {
			try {
				withTxSession(s -> {
					Item loadedItem = s.get(Item.class, item.getId());
					assertNotNull(loadedItem);
					arriveAndAwait(deletePhaser, 2000);
					arriveAndAwait(deletePhaser, 2000);
					log.trace("Item loaded");
					s.delete(loadedItem);
					s.flush();
					log.trace("Item deleted");
					// start get-thread here
					arriveAndAwait(deletePhaser, 2000);
					// we need longer timeout since in non-MVCC DBs the get thread
					// can be blocked
					arriveAndAwait(deletePhaser, 4000);
				});
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, "delete-thread");
		Thread getThread = new Thread(() -> {
			try {
				withTxSession(s -> {
					// DB load should happen before the record is deleted,
					// putFromLoad should happen after deleteThread ends
					Item loadedItem = s.get(Item.class, item.getId());
					if (getThreadBlockedInDB.get()) {
						assertNull(loadedItem);
					} else {
						assertNotNull(loadedItem);
					}
				});
			} catch (PessimisticLockException e) {
				// If we end up here, database locks guard us against situation tested
				// in this case and HHH-9868 cannot happen.
				// (delete-thread has ITEMS table write-locked and we try to acquire read-lock)
				try {
					arriveAndAwait(getPhaser, 2000);
					arriveAndAwait(getPhaser, 2000);
				} catch (Exception e1) {
					throw new RuntimeException(e1);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, "get-thread");

		deleteThread.start();
		// deleteThread loads the entity
		arriveAndAwait(deletePhaser, 2000);
		withTx(() -> {
			sessionFactory().getCache().evictEntity(Item.class, item.getId());
			assertFalse(sessionFactory().getCache().containsEntity(Item.class, item.getId()));
			return null;
		});
		arriveAndAwait(deletePhaser, 2000);
		// delete thread invalidates PFER
		arriveAndAwait(deletePhaser, 2000);
		// get thread gets the entity from DB
		hook.block(getPhaser, getThread);
		getThread.start();
		try {
			arriveAndAwait(getPhaser, 2000);
		} catch (TimeoutException e) {
			getThreadBlockedInDB.set(true);
		}
		arriveAndAwait(deletePhaser, 2000);
		// delete thread finishes the remove from DB and cache
		deleteThread.join();
		hook.unblock();
		arriveAndAwait(getPhaser, 2000);
		// get thread puts the entry into cache
		getThread.join();

		withTxSession(s -> {
			Item loadedItem = s.get(Item.class, item.getId());
			assertNull(loadedItem);
		});
	}

	protected static void arriveAndAwait(Phaser phaser, int timeout) throws TimeoutException, InterruptedException {
		phaser.awaitAdvanceInterruptibly(phaser.arrive(), timeout, TimeUnit.MILLISECONDS);
	}

	private static class HookInterceptor extends BaseCustomInterceptor {
		Phaser phaser;
		Thread thread;

		public synchronized void block(Phaser phaser, Thread thread) {
			this.phaser = phaser;
			this.thread = thread;
		}

		public synchronized void unblock() {
			phaser = null;
			thread = null;
		}

		@Override
		public Object visitGetKeyValueCommand(InvocationContext ctx, GetKeyValueCommand command) throws Throwable {
			Phaser phaser;
			Thread thread;
			synchronized (this) {
				phaser = this.phaser;
				thread = this.thread;
			}
			if (phaser != null && Thread.currentThread() == thread) {
				arriveAndAwait(phaser, 2000);
				arriveAndAwait(phaser, 2000);
			}
			return super.visitGetKeyValueCommand(ctx, command);
		}
	}
}
