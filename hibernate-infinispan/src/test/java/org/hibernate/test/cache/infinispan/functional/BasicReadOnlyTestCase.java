package org.hibernate.test.cache.infinispan.functional;

import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

import java.util.Map;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public class BasicReadOnlyTestCase extends SingleNodeTestCase {

   public BasicReadOnlyTestCase(String string) {
      super(string);
   }

   @Override
   public String getCacheConcurrencyStrategy() {
      return "read-only";
   }

   public void testEmptySecondLevelCacheEntry() throws Exception {
      getSessions().getCache().evictEntityRegion(Item.class.getName());
      Statistics stats = getSessions().getStatistics();
      stats.clear();
      SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(Item.class.getName() + ".items");
      Map cacheEntries = statistics.getEntries();
      assertEquals(0, cacheEntries.size());
   }
   
}