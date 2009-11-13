package org.hibernate.test.cache.infinispan.functional;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.entry.CacheEntry;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public class BasicTransactionalTestCase extends AbstractFunctionalTestCase {

   public BasicTransactionalTestCase(String string) {
      super(string, "transactional");
   }

   public void testEntityCache() {
      Item item = new Item("chris", "Chris's Item");

      Session s = openSession();
      Statistics stats = s.getSessionFactory().getStatistics();
      s.getTransaction().begin();
      s.persist(item);
      s.getTransaction().commit();
      s.close();

      s = openSession();
      Item found = (Item) s.load(Item.class, item.getId());
      System.out.println(stats);
      assertEquals(item.getDescription(), found.getDescription());
      assertEquals(0, stats.getSecondLevelCacheMissCount());
      assertEquals(1, stats.getSecondLevelCacheHitCount());
      s.delete(found);
      s.close();
   }
   
   public void testCollectionCache() {
      Item item = new Item("chris", "Chris's Item");
      Item another = new Item("another", "Owned Item");
      item.addItem(another);

      Session s = openSession();
      s.getTransaction().begin();
      s.persist(item);
      s.persist(another);
      s.getTransaction().commit();
      s.close();

      s = openSession();
      Statistics stats = s.getSessionFactory().getStatistics();
      Item loaded = (Item) s.load(Item.class, item.getId());
      assertEquals(1, loaded.getItems().size());
      s.close();

      s = openSession();
      SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics(Item.class.getName() + ".items");
      Item loadedWithCachedCollection = (Item) s.load(Item.class, item.getId());
      stats.logSummary();
      assertEquals(item.getName(), loadedWithCachedCollection.getName());
      assertEquals(item.getItems().size(), loadedWithCachedCollection.getItems().size());
      assertEquals(1, cStats.getHitCount());
      Map cacheEntries = cStats.getEntries();
      assertEquals(1, cacheEntries.size());
      s.close();
   }

   public void testStaleWritesLeaveCacheConsistent() {
      Session s = openSession();
      Transaction txn = s.beginTransaction();
      VersionedItem item = new VersionedItem();
      item.setName("steve");
      item.setDescription("steve's item");
      s.save(item);
      txn.commit();
      s.close();

      Long initialVersion = item.getVersion();

      // manually revert the version property
      item.setVersion(new Long(item.getVersion().longValue() - 1));

      try {
          s = openSession();
          txn = s.beginTransaction();
          s.update(item);
          txn.commit();
          s.close();
          fail("expected stale write to fail");
      } catch (Throwable expected) {
          // expected behavior here
          if (txn != null) {
              try {
                  txn.rollback();
              } catch (Throwable ignore) {
              }
          }
      } finally {
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

      // cleanup
      s = openSession();
      txn = s.beginTransaction();
      item = (VersionedItem) s.load(VersionedItem.class, item.getId());
      s.delete(item);
      txn.commit();
      s.close();
  }

   public void testQueryCacheInvalidation() {
      Session s = openSession();
      Transaction t = s.beginTransaction();
      Item i = new Item();
      i.setName("widget");
      i.setDescription("A really top-quality, full-featured widget.");
      s.persist(i);
      t.commit();
      s.close();

      SecondLevelCacheStatistics slcs = s.getSessionFactory().getStatistics().getSecondLevelCacheStatistics(Item.class.getName());

      assertEquals(slcs.getPutCount(), 1);
      assertEquals(slcs.getElementCountInMemory(), 1);
      assertEquals(slcs.getEntries().size(), 1);

      s = openSession();
      t = s.beginTransaction();
      i = (Item) s.get(Item.class, i.getId());

      assertEquals(slcs.getHitCount(), 1);
      assertEquals(slcs.getMissCount(), 0);

      i.setDescription("A bog standard item");

      t.commit();
      s.close();

      assertEquals(slcs.getPutCount(), 2);

      CacheEntry entry = (CacheEntry) slcs.getEntries().get(i.getId());
      Serializable[] ser = entry.getDisassembledState();
      assertTrue(ser[0].equals("widget"));
      assertTrue(ser[1].equals("A bog standard item"));
      
      // cleanup
      s = openSession();
      t = s.beginTransaction();
      s.delete(i);
      t.commit();
      s.close();
   }
   
   public void testQueryCache() {
      Item item = new Item("chris", "Chris's Item");

      Session s = openSession();
      s.getTransaction().begin();
      s.persist(item);
      s.getTransaction().commit();
      s.close();

      s = openSession();
      s.createQuery("from Item").setCacheable(true).list();
      s.close();

      s = openSession();
      Statistics stats = s.getSessionFactory().getStatistics();
      s.createQuery("from Item").setCacheable(true).list();
      assertEquals(1, stats.getQueryCacheHitCount());
      s.createQuery("delete from Item").executeUpdate();
      s.close();
   }
}
