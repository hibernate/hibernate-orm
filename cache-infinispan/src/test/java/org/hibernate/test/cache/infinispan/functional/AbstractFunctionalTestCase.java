package org.hibernate.test.cache.infinispan.functional;

import java.util.Map;

import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractFunctionalTestCase extends FunctionalTestCase {
   private final String cacheConcurrencyStrategy;

   public AbstractFunctionalTestCase(String string, String cacheConcurrencyStrategy) {
      super(string);
      this.cacheConcurrencyStrategy = cacheConcurrencyStrategy;
   }

   public String[] getMappings() {
      return new String[] { "cache/infinispan/functional/Item.hbm.xml" };
   }

   @Override
   public String getCacheConcurrencyStrategy() {
      return cacheConcurrencyStrategy;
   }
   
   public void testEmptySecondLevelCacheEntry() throws Exception {
//      getSessions().evictEntity(Item.class.getName());
      getSessions().getCache().evictEntityRegion(Item.class.getName());
      Statistics stats = getSessions().getStatistics();
      stats.clear();
      SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(Item.class.getName() + ".items");
      Map cacheEntries = statistics.getEntries();
      assertEquals(0, cacheEntries.size());
  }
}