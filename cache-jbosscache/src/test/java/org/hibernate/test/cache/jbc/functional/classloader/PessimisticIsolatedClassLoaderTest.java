/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.cache.jbc.functional.classloader;


import javax.transaction.TransactionManager;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.SessionFactory;
import org.hibernate.cache.StandardQueryCache;
import org.hibernate.cache.jbc.BasicRegionAdapter;
import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;
import org.hibernate.cache.jbc.query.QueryResultsRegionImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.jbc.functional.DualNodeTestCaseBase;
import org.hibernate.test.cache.jbc.functional.util.DualNodeJtaTransactionManagerImpl;
import org.hibernate.test.cache.jbc.functional.util.DualNodeTestUtil;
import org.hibernate.test.cache.jbc.functional.util.IsolatedCacheTestSetup;
import org.hibernate.test.cache.jbc.functional.util.TestCacheInstanceManager;
import org.hibernate.test.cache.jbc.functional.util.TestJBossCacheRegionFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheManager;
import org.jboss.cache.Fqn;
import org.jboss.cache.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests entity and query caching when class of objects being cached are not
 * visible to JBoss Cache's classloader.  Also serves as a general integration
 * test.
 * <p/>
 * This test stores an object (AccountHolder) that isn't visible to the JBC 
 * classloader in the cache in two places:
 * 
 * 1) As part of the value tuple in an Account entity
 * 2) As part of the FQN in a query cache entry (see query in 
 *    ClassLoaderTestDAO.getBranch())
 */
public class PessimisticIsolatedClassLoaderTest
extends DualNodeTestCaseBase
{
   public static final String OUR_PACKAGE = PessimisticIsolatedClassLoaderTest.class.getPackage().getName();
   
   private static final String CACHE_CONFIG = "pessimistic-shared";

   protected static final long SLEEP_TIME = 300L;
   
   protected final Logger log = LoggerFactory.getLogger(getClass());

   static int test = 0;
   
   private Cache localCache;
   private CacheAccessListener localListener;
   
   private Cache remoteCache;
   private CacheAccessListener remoteListener;
   
   public PessimisticIsolatedClassLoaderTest(String name)
   {
      super(name);
   }
   
   public static Test suite() throws Exception {
       TestSuite suite = new TestSuite(PessimisticIsolatedClassLoaderTest.class);
       String[] acctClasses = { OUR_PACKAGE + ".Account", OUR_PACKAGE + ".AccountHolder" };
       return new IsolatedCacheTestSetup(suite, acctClasses, CACHE_CONFIG);
   }

   @Override
   protected Class getCacheRegionFactory()
   {
      return TestJBossCacheRegionFactory.class;
   }

   @Override
   protected boolean getUseQueryCache()
   {
      return true;
   }

   @Override
   public String[] getMappings()
   {
      return new String[] { "cache/jbc/functional/classloader/Account.hbm.xml" };
   }

   @Override
   protected void configureCacheFactory(Configuration cfg)
   {
      cfg.setProperty(MultiplexingCacheInstanceManager.ENTITY_CACHE_RESOURCE_PROP, 
                      getEntityCacheConfigName()); 
      cfg.setProperty(MultiplexingCacheInstanceManager.TIMESTAMP_CACHE_RESOURCE_PROP, 
                      getEntityCacheConfigName());     
   }

   protected String getEntityCacheConfigName() {
       return CACHE_CONFIG;
   } 
   
   @Override
   protected void cleanupTransactionManagement() {
      // Don't clean up the managers, just the transactions
      // Managers are still needed by the long-lived caches
      DualNodeJtaTransactionManagerImpl.cleanupTransactions();
   }  

   @Override
   protected void cleanupTest() throws Exception
   {
      try
      {
      if (localCache != null && localListener != null)
         localCache.removeCacheListener(localListener);
      if (remoteCache != null && remoteListener != null)
         remoteCache.removeCacheListener(remoteListener);
      }
      finally
      {
         super.cleanupTest();
      }
   }

   /**
    * Simply confirms that the test fixture's classloader isolation setup
    * is functioning as expected.
    * 
    * @throws Exception
    */
   public void testIsolatedSetup() throws Exception
   {
      // Bind a listener to the "local" cache
      // Our region factory makes its CacheManager available to us
      org.jboss.cache.CacheManager localManager = TestCacheInstanceManager.getTestCacheManager(DualNodeTestUtil.LOCAL);
      org.jboss.cache.Cache localCache = localManager.getCache(getEntityCacheConfigName(), true);
      
      // Bind a listener to the "remote" cache
      org.jboss.cache.CacheManager remoteManager = TestCacheInstanceManager.getTestCacheManager(DualNodeTestUtil.REMOTE);
      org.jboss.cache.Cache remoteCache = remoteManager.getCache(getEntityCacheConfigName(), true);
      
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      log.info("TCCL is " + cl);
      Thread.currentThread().setContextClassLoader(cl.getParent());
      
      org.jboss.cache.Fqn fqn = org.jboss.cache.Fqn.fromString("/isolated1");
      org.jboss.cache.Region r = localCache.getRegion(fqn, true);
      r.registerContextClassLoader(cl.getParent());
      r.activate();
      
      r = remoteCache.getRegion(fqn, true);
      r.registerContextClassLoader(cl.getParent());
      r.activate();
      Thread.currentThread().setContextClassLoader(cl);
      Account acct = new Account();
      acct.setAccountHolder(new AccountHolder());
      
      try
      {
         localCache.put(fqn, "key", acct);
         fail("Should not have succeeded in putting acct -- classloader not isolated");
      }
      catch (Exception e) {
          log.info("Caught exception as desired", e);
      }
      
      localCache.getRegion(fqn, false).registerContextClassLoader(Thread.currentThread().getContextClassLoader());
      remoteCache.getRegion(fqn, false).registerContextClassLoader(Thread.currentThread().getContextClassLoader());
      
      localCache.put(fqn, "key", acct);
      assertEquals(acct.getClass().getName(), remoteCache.get(fqn, "key").getClass().getName());
   }
   
   public void testClassLoaderHandlingNamedQueryRegion() throws Exception {
      queryTest(true);
   }
   
   public void testClassLoaderHandlingStandardQueryCache() throws Exception {
      queryTest(false);
   }
   
   protected void queryTest(boolean useNamedRegion) throws Exception
   {
      // Bind a listener to the "local" cache
      // Our region factory makes its CacheManager available to us
      CacheManager localManager = TestCacheInstanceManager.getTestCacheManager(DualNodeTestUtil.LOCAL);
      this.localCache = localManager.getCache(getEntityCacheConfigName(), true);
      this.localListener = new CacheAccessListener();
      localCache.addCacheListener(localListener);
      
      TransactionManager localTM = localCache.getConfiguration().getRuntimeConfig().getTransactionManager();
      
      // Bind a listener to the "remote" cache
      CacheManager remoteManager = TestCacheInstanceManager.getTestCacheManager(DualNodeTestUtil.REMOTE);
      this.remoteCache = remoteManager.getCache(getEntityCacheConfigName(), true);
      this.remoteListener = new CacheAccessListener();
      remoteCache.addCacheListener(remoteListener);      
      
      TransactionManager remoteTM = remoteCache.getConfiguration().getRuntimeConfig().getTransactionManager();
      
      SessionFactory localFactory = getEnvironment().getSessionFactory();
      SessionFactory remoteFactory = getSecondNodeEnvironment().getSessionFactory();
      
      ClassLoaderTestDAO dao0 = new ClassLoaderTestDAO(localFactory, localTM);      
      ClassLoaderTestDAO dao1 = new ClassLoaderTestDAO(remoteFactory, remoteTM);
      
      // Determine whether our query region is already there (in which case it
      // will receive remote messages immediately) or is yet to be created on
      // first use (in which case it will initially discard remote messages)
      String regionName = createRegionName(useNamedRegion ? "AccountRegion" : StandardQueryCache.class.getName());
      Region queryRegion = remoteCache.getRegion(Fqn.fromString(regionName), false);
      boolean queryRegionExists = queryRegion != null && queryRegion.isActive();
      
      // Initial ops on node 0
      setupEntities(dao0);
      
      // Query on post code count
      assertEquals("63088 has correct # of accounts", 6, dao0.getCountForBranch("63088", useNamedRegion));
      
      assertTrue("Query cache used " + regionName, 
            localListener.getSawRegionModification(regionName));
      // Clear the access state
      localListener.getSawRegionAccess(regionName);
      
      log.info("First query on node0 done");
      
      // Sleep a bit to allow async repl to happen
      sleep(SLEEP_TIME);
      
      // If region isn't activated yet, should not have been modified      
      if (!queryRegionExists)
      {
         assertFalse("Query cache remotely modified " + regionName, 
               remoteListener.getSawRegionModification(regionName));
         // Clear the access state
         remoteListener.getSawRegionAccess(regionName);
      }
      else
      {
         assertTrue("Query cache remotely modified " + regionName, 
               remoteListener.getSawRegionModification(regionName));
         // Clear the access state
         remoteListener.getSawRegionAccess(regionName);         
      }
      
      // Do query again from node 1      
      assertEquals("63088 has correct # of accounts", 6, dao1.getCountForBranch("63088", useNamedRegion));
      
      if (!queryRegionExists)
      {
         // Query should have activated the region and then been inserted
         assertTrue("Query cache modified " + regionName, 
               remoteListener.getSawRegionModification(regionName));
         // Clear the access state
         remoteListener.getSawRegionAccess(regionName);
      }
      
      log.info("First query on node 1 done");
      
      // We now have the query cache region activated on both nodes.
      
      // Sleep a bit to allow async repl to happen
      sleep(SLEEP_TIME);
      
      // Do some more queries on node 0
      
      assertEquals("Correct branch for Smith", "94536", dao0.getBranch(dao0.getSmith(), useNamedRegion));
      
      assertEquals("Correct high balances for Jones", 40, dao0.getTotalBalance(dao0.getJones(), useNamedRegion));
      
      assertTrue("Query cache used " + regionName, 
            localListener.getSawRegionModification(regionName));
      // Clear the access state
      localListener.getSawRegionAccess(regionName);
      
      log.info("Second set of queries on node0 done");
      
      // Sleep a bit to allow async repl to happen
      sleep(SLEEP_TIME);
             
      // Check if the previous queries replicated      
      assertTrue("Query cache remotely modified " + regionName, 
            remoteListener.getSawRegionModification(regionName));
      // Clear the access state
      remoteListener.getSawRegionAccess(regionName);
      
      // Do queries again from node 1      
      assertEquals("Correct branch for Smith", "94536", dao1.getBranch(dao1.getSmith(), useNamedRegion));
      
      assertEquals("Correct high balances for Jones", 40, dao1.getTotalBalance(dao1.getJones(), useNamedRegion));
      
      // Should be no change; query was already there
      assertFalse("Query cache modified " + regionName, 
            remoteListener.getSawRegionModification(regionName));
      assertTrue("Query cache accessed " + regionName, 
            remoteListener.getSawRegionAccess(regionName));
      
      log.info("Second set of queries on node1 done");
      
      // allow async to propagate
      sleep(SLEEP_TIME);
      
      // Modify underlying data on node 1
      modifyEntities(dao1);
      
      // allow async timestamp change to propagate
      sleep(SLEEP_TIME);
      
      // Confirm query results are correct on node 0
      
      assertEquals("63088 has correct # of accounts", 7, dao0.getCountForBranch("63088", useNamedRegion));
      
      assertEquals("Correct branch for Smith", "63088", dao0.getBranch(dao0.getSmith(), useNamedRegion));
      
      assertEquals("Correct high balances for Jones", 50, dao0.getTotalBalance(dao0.getJones(), useNamedRegion));
      
      log.info("Third set of queries on node0 done");
   }
   
   protected String createRegionName(String noPrefix)
   {
      String combined = getRegionPrefix() == null ? noPrefix : getRegionPrefix() + '.' + noPrefix;
      return BasicRegionAdapter.getTypeLastRegionFqn(combined, getRegionPrefix(), QueryResultsRegionImpl.TYPE).toString();
   }
   
   protected void setupEntities(ClassLoaderTestDAO dao) throws Exception
   {
      dao.cleanup();
      
      dao.createAccount(dao.getSmith(), new Integer(1001), new Integer(5), "94536");
      dao.createAccount(dao.getSmith(), new Integer(1002), new Integer(15), "94536");
      dao.createAccount(dao.getSmith(), new Integer(1003), new Integer(20), "94536");
      
      dao.createAccount(dao.getJones(), new Integer(2001), new Integer(5), "63088");
      dao.createAccount(dao.getJones(), new Integer(2002), new Integer(15), "63088");
      dao.createAccount(dao.getJones(), new Integer(2003), new Integer(20), "63088");
      
      dao.createAccount(dao.getBarney(), new Integer(3001), new Integer(5), "63088");
      dao.createAccount(dao.getBarney(), new Integer(3002), new Integer(15), "63088");
      dao.createAccount(dao.getBarney(), new Integer(3003), new Integer(20), "63088");
      
      log.info("Standard entities created");
   }
   
   protected void resetRegionUsageState(CacheAccessListener localListener, CacheAccessListener remoteListener)
   {  
      String stdName = createRegionName(StandardQueryCache.class.getName());
      String acctName = createRegionName("AccountRegion");
      
      localListener.getSawRegionModification(stdName);
      localListener.getSawRegionModification(acctName);
      
      localListener.getSawRegionAccess(stdName);
      localListener.getSawRegionAccess(acctName);
      
      remoteListener.getSawRegionModification(stdName);
      remoteListener.getSawRegionModification(acctName);
      
      remoteListener.getSawRegionAccess(stdName);
      remoteListener.getSawRegionAccess(acctName);
      
      log.info("Region usage state cleared");      
   }
   
   protected void modifyEntities(ClassLoaderTestDAO dao) throws Exception
   {
      dao.updateAccountBranch(1001, "63088");
      dao.updateAccountBalance(2001, 15);
      
      log.info("Entities modified");
   }
}
