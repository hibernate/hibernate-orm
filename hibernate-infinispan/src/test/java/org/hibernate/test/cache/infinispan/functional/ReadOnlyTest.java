/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional;

import java.util.List;
import java.util.Map;

import org.hibernate.cache.infinispan.util.InfinispanMessageLogger;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.test.cache.infinispan.functional.entities.Item;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.test.cache.infinispan.util.TestTimeService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Parent tests for both transactional and
 * read-only tests are defined in this class.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
public class ReadOnlyTest extends SingleNodeTest {
	protected static final InfinispanMessageLogger log = InfinispanMessageLogger.Provider.getLog(ReadOnlyTest.class);
	protected static final TestTimeService TIME_SERVICE = new TestTimeService();

	@Override
	public List<Object[]> getParameters() {
		return getParameters(false, false, true, true);
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
		TIME_SERVICE.advance(1);

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

	@Override
	protected void addSettings(Map settings) {
		super.addSettings(settings);
		settings.put(TestInfinispanRegionFactory.TIME_SERVICE, TIME_SERVICE);
	}
}
