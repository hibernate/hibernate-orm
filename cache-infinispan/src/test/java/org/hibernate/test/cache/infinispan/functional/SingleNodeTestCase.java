package org.hibernate.test.cache.infinispan.functional;

import java.util.Map;

import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.transaction.CMTTransactionFactory;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class SingleNodeTestCase extends FunctionalTestCase {
   private static final Log log = LogFactory.getLog(SingleNodeTestCase.class);
   private final TransactionManager tm;

   public SingleNodeTestCase(String string) {
      super(string);
      tm = getTransactionManager();
   }

   protected TransactionManager getTransactionManager() {
      try {
         if (getTransactionManagerLookupClass() == null)
            return null;
         else
            return getTransactionManagerLookupClass().newInstance().getTransactionManager(null);
      } catch (Exception e) {
         log.error("Error", e);
         throw new RuntimeException(e);
      }
   }

   
   public String[] getMappings() {
      return new String[] { 
               "cache/infinispan/functional/Item.hbm.xml", 
               "cache/infinispan/functional/Customer.hbm.xml", 
               "cache/infinispan/functional/Contact.hbm.xml"};
   }

   @Override
   public String getCacheConcurrencyStrategy() {
      return "transactional";
   }

   protected Class<? extends RegionFactory> getCacheRegionFactory() {
      return InfinispanRegionFactory.class;
   }

   protected Class<? extends TransactionFactory> getTransactionFactoryClass() {
      return CMTTransactionFactory.class;
   }

   protected Class<? extends ConnectionProvider> getConnectionProviderClass() {
      return org.hibernate.test.cache.infinispan.tm.XaConnectionProvider.class;
   }

   protected Class<? extends TransactionManagerLookup> getTransactionManagerLookupClass() {
      return org.hibernate.test.cache.infinispan.tm.XaTransactionManagerLookup.class;
   }

   protected boolean getUseQueryCache() {
      return true;
   }

   public void configure(Configuration cfg) {
      super.configure(cfg);
      cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
      cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
      cfg.setProperty(Environment.USE_QUERY_CACHE, String.valueOf(getUseQueryCache()));
      cfg.setProperty(Environment.CACHE_REGION_FACTORY, getCacheRegionFactory().getName());
      cfg.setProperty(Environment.CONNECTION_PROVIDER, getConnectionProviderClass().getName());
      if (getTransactionManagerLookupClass() != null) {
         cfg.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, getTransactionManagerLookupClass().getName());
      }
      cfg.setProperty(Environment.TRANSACTION_STRATEGY, getTransactionFactoryClass().getName());
   }

   public void testEmptySecondLevelCacheEntry() throws Exception {
      getSessions().getCache().evictEntityRegion(Item.class.getName());
      Statistics stats = getSessions().getStatistics();
      stats.clear();
      SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(Item.class.getName() + ".items");
      Map cacheEntries = statistics.getEntries();
      assertEquals(0, cacheEntries.size());
   }

   protected void beginTx() throws Exception {
      tm.begin();
   }

   protected void setRollbackOnlyTx() throws Exception {
      tm.setRollbackOnly();
   }

   protected void setRollbackOnlyTx(Exception e) throws Exception {
      log.error("Error", e);
      tm.setRollbackOnly();
      throw e;
   }

   protected void setRollbackOnlyTxExpected(Exception e) throws Exception {
      log.debug("Expected behaivour", e);
      tm.setRollbackOnly();
   }

   protected void commitOrRollbackTx() throws Exception {
      if (tm.getStatus() == Status.STATUS_ACTIVE) tm.commit();
      else tm.rollback();
   }
   
}