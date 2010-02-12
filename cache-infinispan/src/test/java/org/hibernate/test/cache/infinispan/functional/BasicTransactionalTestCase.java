package org.hibernate.test.cache.infinispan.functional;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.entry.CacheEntry;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public class BasicTransactionalTestCase extends SingleNodeTestCase {
   private static final Log log = LogFactory.getLog(BasicTransactionalTestCase.class);

   public BasicTransactionalTestCase(String string) {
      super(string);
   }

   @Override
   public void configure(Configuration cfg) {
      super.configure(cfg);
   }

   public void testEntityCache() throws Exception {
      Item item = new Item("chris", "Chris's Item");
      beginTx();
      try {
         Session s = openSession();
         s.getTransaction().begin();
         s.persist(item);
         s.getTransaction().commit();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }

      beginTx();
      try {
         Session s = openSession();
         Item found = (Item) s.load(Item.class, item.getId());
         Statistics stats = s.getSessionFactory().getStatistics();
         log.info(stats.toString());
         assertEquals(item.getDescription(), found.getDescription());
         assertEquals(0, stats.getSecondLevelCacheMissCount());
         assertEquals(1, stats.getSecondLevelCacheHitCount());
         s.delete(found);
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }
   }

   public void testCollectionCache() throws Exception {
      Item item = new Item("chris", "Chris's Item");
      Item another = new Item("another", "Owned Item");
      item.addItem(another);

      beginTx();
      try {
         Session s = openSession();
         s.getTransaction().begin();
         s.persist(item);
         s.persist(another);
         s.getTransaction().commit();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }

      beginTx();
      try {
         Session s = openSession();
         Item loaded = (Item) s.load(Item.class, item.getId());
         assertEquals(1, loaded.getItems().size());
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }

      beginTx();
      try {
         Session s = openSession();
         Statistics stats = s.getSessionFactory().getStatistics();
         SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics(Item.class.getName() + ".items");
         Item loadedWithCachedCollection = (Item) s.load(Item.class, item.getId());
         stats.logSummary();
         assertEquals(item.getName(), loadedWithCachedCollection.getName());
         assertEquals(item.getItems().size(), loadedWithCachedCollection.getItems().size());
         assertEquals(1, cStats.getHitCount());
         Map cacheEntries = cStats.getEntries();
         assertEquals(1, cacheEntries.size());
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }
   }

   public void testStaleWritesLeaveCacheConsistent() throws Exception {
      VersionedItem item = null;
      Transaction txn = null;
      Session s = null;
      beginTx();
      try {
         s = openSession();
         txn = s.beginTransaction();
         item = new VersionedItem();
         item.setName("steve");
         item.setDescription("steve's item");
         s.save(item);
         txn.commit();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }

      Long initialVersion = item.getVersion();

      // manually revert the version property
      item.setVersion(new Long(item.getVersion().longValue() - 1));

      beginTx();
      try {
         s = openSession();
         txn = s.beginTransaction();
         s.update(item);
         txn.commit();
         fail("expected stale write to fail");
      } catch (Exception e) {
         setRollbackOnlyTxExpected(e);
      } finally {
         commitOrRollbackTx();
         if (s != null && s.isOpen()) {
            try {
               s.close();
            } catch (Throwable ignore) {
            }
         }
      }

      // check the version value in the cache...
      SecondLevelCacheStatistics slcs = sfi().getStatistics().getSecondLevelCacheStatistics(VersionedItem.class.getName());

      Object entry = slcs.getEntries().get(item.getId());
      Long cachedVersionValue;
      cachedVersionValue = (Long) ((CacheEntry) entry).getVersion();
      assertEquals(initialVersion.longValue(), cachedVersionValue.longValue());

      beginTx();
      try {
         // cleanup
         s = openSession();
         txn = s.beginTransaction();
         item = (VersionedItem) s.load(VersionedItem.class, item.getId());
         s.delete(item);
         txn.commit();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }
  }

   public void testQueryCacheInvalidation() throws Exception {
      Session s = null;
      Transaction t = null;
      Item i = null;
      
      beginTx();
      try {
         s = openSession();
         t = s.beginTransaction();
         i = new Item();
         i.setName("widget");
         i.setDescription("A really top-quality, full-featured widget.");
         s.persist(i);
         t.commit();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }

      SecondLevelCacheStatistics slcs = s.getSessionFactory().getStatistics().getSecondLevelCacheStatistics(Item.class.getName());

      assertEquals(slcs.getPutCount(), 1);
      assertEquals(slcs.getElementCountInMemory(), 1);
      assertEquals(slcs.getEntries().size(), 1);

      beginTx();
      try {
         s = openSession();
         t = s.beginTransaction();
         i = (Item) s.get(Item.class, i.getId());
         assertEquals(slcs.getHitCount(), 1);
         assertEquals(slcs.getMissCount(), 0);
         i.setDescription("A bog standard item");
         t.commit();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }

      assertEquals(slcs.getPutCount(), 2);

      CacheEntry entry = (CacheEntry) slcs.getEntries().get(i.getId());
      Serializable[] ser = entry.getDisassembledState();
      assertTrue(ser[0].equals("widget"));
      assertTrue(ser[1].equals("A bog standard item"));
      
      beginTx();
      try {
         // cleanup
         s = openSession();
         t = s.beginTransaction();
         s.delete(i);
         t.commit();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }
   }
   
   public void testQueryCache() throws Exception {
      Session s = null;
      Item item = new Item("chris", "Chris's Item");
      
      beginTx();
      try {
         s = openSession();
         s.getTransaction().begin();
         s.persist(item);
         s.getTransaction().commit();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }

      beginTx();
      try {
         s = openSession();
         s.createQuery("from Item").setCacheable(true).list();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }

      beginTx();
      try {
         s = openSession();
         Statistics stats = s.getSessionFactory().getStatistics();
         s.createQuery("from Item").setCacheable(true).list();
         assertEquals(1, stats.getQueryCacheHitCount());
         s.createQuery("delete from Item").executeUpdate();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }
   }

   public void testQueryCacheHitInSameTransaction() throws Exception {
      Session s = null;
      Item item = new Item("galder", "Galder's Item");

      beginTx();
      try {
         s = openSession();
         s.getTransaction().begin();
         s.persist(item);
         s.getTransaction().commit();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }

      beginTx();
      try {
         s = openSession();
         Statistics stats = s.getSessionFactory().getStatistics();
         s.createQuery("from Item").setCacheable(true).list();
         s.createQuery("from Item").setCacheable(true).list();
         assertEquals(1, stats.getQueryCacheHitCount());
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }

      beginTx();
      try {
         s = openSession();
         s.createQuery("delete from Item").executeUpdate();
         s.close();
      } catch (Exception e) {
         setRollbackOnlyTx(e);
      } finally {
         commitOrRollbackTx();
      }
   }   
}
