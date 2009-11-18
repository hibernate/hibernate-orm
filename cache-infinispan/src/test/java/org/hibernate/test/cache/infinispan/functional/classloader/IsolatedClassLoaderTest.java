/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or it's affiliates, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.test.cache.infinispan.functional.classloader;

import javax.transaction.TransactionManager;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.SessionFactory;
import org.hibernate.cache.StandardQueryCache;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeTestCase;
import org.hibernate.test.cache.infinispan.functional.cluster.ClusterAwareRegionFactory;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeJtaTransactionManagerImpl;
import org.infinispan.Cache;
import org.infinispan.manager.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests entity and query caching when class of objects being cached are not visible to Infinispan's
 * classloader. Also serves as a general integration test.
 * <p/>
 * This test stores an object (AccountHolder) that isn't visible to the Infinispan classloader in
 * the cache in two places:
 * 
 * 1) As part of the value tuple in an Account entity 2) As part of the FQN in a query cache entry
 * (see query in ClassLoaderTestDAO.getBranch())
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class IsolatedClassLoaderTest extends DualNodeTestCase {

   public static final String OUR_PACKAGE = IsolatedClassLoaderTest.class.getPackage().getName();

   private static final String CACHE_CONFIG = "classloader";

   protected static final long SLEEP_TIME = 300L;

   protected final Logger log = LoggerFactory.getLogger(getClass());

   static int test = 0;

   private Cache localQueryCache ;
   private CacheAccessListener localQueryListener;

   private Cache remoteQueryCache;
   private CacheAccessListener remoteQueryListener;

   public IsolatedClassLoaderTest(String string) {
      super(string);
   }

   public static Test suite() throws Exception {
      TestSuite suite = new TestSuite(IsolatedClassLoaderTest.class);
      String[] acctClasses = { OUR_PACKAGE + ".Account", OUR_PACKAGE + ".AccountHolder" };
      return new IsolatedCacheTestSetup(suite, acctClasses);
   }

   @Override
   public String[] getMappings() {
      return new String[] { "cache/infinispan/functional/classloader/Account.hbm.xml" };
   }
   
   @Override
   protected void standardConfigure(Configuration cfg) {
      super.standardConfigure(cfg);
      cfg.setProperty(InfinispanRegionFactory.QUERY_CACHE_RESOURCE_PROP, "replicated-query");
   }


   @Override
   protected void cleanupTransactionManagement() {
      // Don't clean up the managers, just the transactions
      // Managers are still needed by the long-lived caches
      DualNodeJtaTransactionManagerImpl.cleanupTransactions();
   }

   @Override
   protected void cleanupTest() throws Exception {
      try {
         if (localQueryCache != null && localQueryListener != null)
            localQueryCache.removeListener(localQueryListener);
         if (remoteQueryCache != null && remoteQueryListener != null)
            remoteQueryCache.removeListener(remoteQueryListener);
      } finally {
         super.cleanupTest();
      }
   }

   /**
    * Simply confirms that the test fixture's classloader isolation setup is functioning as
    * expected.
    * 
    * @throws Exception
    */
   public void testIsolatedSetup() throws Exception {
      // Bind a listener to the "local" cache
      // Our region factory makes its CacheManager available to us
      CacheManager localManager = ClusterAwareRegionFactory.getCacheManager(DualNodeTestCase.LOCAL);
      Cache localReplicatedCache = localManager.getCache("replicated-entity");

      // Bind a listener to the "remote" cache
      CacheManager remoteManager = ClusterAwareRegionFactory.getCacheManager(DualNodeTestCase.REMOTE);
      Cache remoteReplicatedCache = remoteManager.getCache("replicated-entity");

      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(cl.getParent());
      log.info("TCCL is " + cl.getParent());

      Account acct = new Account();
      acct.setAccountHolder(new AccountHolder());

      try {
         localReplicatedCache.put("isolated1", acct);
         // With lazy deserialization, retrieval in remote forces class resolution
         remoteReplicatedCache.get("isolated1");
         fail("Should not have succeeded in putting acct -- classloader not isolated");
      } catch (Exception e) {
         if (e.getCause() instanceof ClassNotFoundException) {
            log.info("Caught exception as desired", e);
         } else {
            throw new IllegalStateException("Unexpected exception", e);
         }
      }

      Thread.currentThread().setContextClassLoader(cl);
      log.info("TCCL is " + cl);
      localReplicatedCache.put("isolated2", acct);
      assertEquals(acct.getClass().getName(), remoteReplicatedCache.get("isolated2").getClass().getName());
   }

   public void testClassLoaderHandlingNamedQueryRegion() throws Exception {
      queryTest(true);
   }

   public void testClassLoaderHandlingStandardQueryCache() throws Exception {
      queryTest(false);
   }

   protected void queryTest(boolean useNamedRegion) throws Exception {
      // Bind a listener to the "local" cache
      // Our region factory makes its CacheManager available to us
      CacheManager localManager = ClusterAwareRegionFactory.getCacheManager(DualNodeTestCase.LOCAL);
      localQueryCache = localManager.getCache("replicated-query");
      localQueryListener = new CacheAccessListener();
      localQueryCache.addListener(localQueryListener);

      TransactionManager localTM = DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestCase.LOCAL);

      // Bind a listener to the "remote" cache
      CacheManager remoteManager = ClusterAwareRegionFactory.getCacheManager(DualNodeTestCase.REMOTE);
      remoteQueryCache = remoteManager.getCache("replicated-query");
      remoteQueryListener = new CacheAccessListener();
      remoteQueryCache.addListener(remoteQueryListener);

      TransactionManager remoteTM = DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestCase.REMOTE);

      SessionFactory localFactory = getEnvironment().getSessionFactory();
      SessionFactory remoteFactory = getSecondNodeEnvironment().getSessionFactory();

      ClassLoaderTestDAO dao0 = new ClassLoaderTestDAO(localFactory, localTM);
      ClassLoaderTestDAO dao1 = new ClassLoaderTestDAO(remoteFactory, remoteTM);

      // Initial ops on node 0
      setupEntities(dao0);

      String branch = "63088";
      // Query on post code count
      assertEquals(branch + " has correct # of accounts", 6, dao0.getCountForBranch(branch, useNamedRegion));
      
      assertEquals("Query cache used", 1, localQueryListener.getSawRegionModificationCount());
      localQueryListener.clearSawRegionModification();
      
//      log.info("First query (get count for branch + " + branch + " ) on node0 done, contents of local query cache are: " + TestingUtil.printCache(localQueryCache));

      // Sleep a bit to allow async repl to happen
      sleep(SLEEP_TIME);

      assertEquals("Query cache used", 1, remoteQueryListener.getSawRegionModificationCount());
      remoteQueryListener.clearSawRegionModification();

      // Do query again from node 1
      log.info("Repeat first query (get count for branch + " + branch + " ) on remote node");
      assertEquals("63088 has correct # of accounts", 6, dao1.getCountForBranch(branch, useNamedRegion));
      assertEquals("Query cache used", 1, remoteQueryListener.getSawRegionModificationCount());
      remoteQueryListener.clearSawRegionModification();

      sleep(SLEEP_TIME);

      assertEquals("Query cache used", 1, localQueryListener.getSawRegionModificationCount());
      localQueryListener.clearSawRegionModification();

      log.info("First query on node 1 done");

      // Sleep a bit to allow async repl to happen
      sleep(SLEEP_TIME);

      // Do some more queries on node 0
      log.info("Do query Smith's branch");
      assertEquals("Correct branch for Smith", "94536", dao0.getBranch(dao0.getSmith(), useNamedRegion));
      log.info("Do query Jone's balance");
      assertEquals("Correct high balances for Jones", 40, dao0.getTotalBalance(dao0.getJones(), useNamedRegion));

      assertEquals("Query cache used", 2, localQueryListener.getSawRegionModificationCount());
      localQueryListener.clearSawRegionModification();
//      // Clear the access state
//      localQueryListener.getSawRegionAccess("???");

      log.info("Second set of queries on node0 done");

      // Sleep a bit to allow async repl to happen
      sleep(SLEEP_TIME);

      // Check if the previous queries replicated
      assertEquals("Query cache remotely modified", 2, remoteQueryListener.getSawRegionModificationCount());
      remoteQueryListener.clearSawRegionModification();

      log.info("Repeat second set of queries on node1");

      // Do queries again from node 1
      log.info("Again query Smith's branch");
      assertEquals("Correct branch for Smith", "94536", dao1.getBranch(dao1.getSmith(), useNamedRegion));
      log.info("Again query Jone's balance");
      assertEquals("Correct high balances for Jones", 40, dao1.getTotalBalance(dao1.getJones(), useNamedRegion));

      // Should be no change; query was already there
      assertEquals("Query cache modified", 0, remoteQueryListener.getSawRegionModificationCount());
      assertEquals("Query cache accessed", 2, remoteQueryListener.getSawRegionAccessCount());
      remoteQueryListener.clearSawRegionAccess();

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

   protected void setupEntities(ClassLoaderTestDAO dao) throws Exception {
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

   protected void resetRegionUsageState(CacheAccessListener localListener, CacheAccessListener remoteListener) {
      String stdName = StandardQueryCache.class.getName();
      String acctName = Account.class.getName();

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

   protected void modifyEntities(ClassLoaderTestDAO dao) throws Exception {
      dao.updateAccountBranch(1001, "63088");
      dao.updateAccountBalance(2001, 15);

      log.info("Entities modified");
   }
}
