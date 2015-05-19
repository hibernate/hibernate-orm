/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional;

import org.hibernate.Session;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.Callable;

import static org.infinispan.test.TestingUtil.withTx;
import static org.junit.Assert.assertEquals;

/**
 * Parent tests for both transactional and
 * read-only tests are defined in this class.
 *
 * @author Galder Zamarreño
 * @since 4.1
 */
public abstract class AbstractFunctionalTestCase extends SingleNodeTestCase {

   static final Log log = LogFactory.getLog(AbstractFunctionalTestCase.class);

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
      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            s.getTransaction().begin();
            s.persist(item);
            s.getTransaction().commit();
            s.close();
            return null;
         }
      });

      log.info("Entry persisted, let's load and delete it.");

      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            s.getTransaction().begin();
            Item found = (Item) s.load(Item.class, item.getId());
            log.info(stats.toString());
            assertEquals(item.getDescription(), found.getDescription());
            assertEquals(0, stats.getSecondLevelCacheMissCount());
            assertEquals(1, stats.getSecondLevelCacheHitCount());
            s.delete(found);
            s.getTransaction().commit();
            s.close();
            return null;
         }
      });
   }

   @Test
   public void testInsertClearCacheDeleteEntity() throws Exception {
      final Statistics stats = sessionFactory().getStatistics();
      stats.clear();

      final Item item = new Item( "chris", "Chris's Item" );
      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            s.getTransaction().begin();
            s.persist(item);
            s.getTransaction().commit();
            assertEquals(0, stats.getSecondLevelCacheMissCount());
            assertEquals(0, stats.getSecondLevelCacheHitCount());
            assertEquals(1, stats.getSecondLevelCachePutCount());
            s.close();
            return null;
         }
      });

      log.info("Entry persisted, let's load and delete it.");

      cleanupCache();

      withTx(tm, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            Session s = openSession();
            s.getTransaction().begin();
            Item found = (Item) s.load(Item.class, item.getId());
            log.info(stats.toString());
            assertEquals(item.getDescription(), found.getDescription());
            assertEquals(1, stats.getSecondLevelCacheMissCount());
            assertEquals(0, stats.getSecondLevelCacheHitCount());
            assertEquals(2, stats.getSecondLevelCachePutCount());
            s.delete(found);
            s.getTransaction().commit();
            s.close();
            return null;
         }
      });
   }

}
