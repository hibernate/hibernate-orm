/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Brian Stansberry
 */

package org.hibernate.test.cache.jbc.functional;

import javax.transaction.TransactionManager;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.SessionFactory;
import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.cache.jbc.functional.classloader.Account;
import org.hibernate.test.cache.jbc.functional.classloader.ClassLoaderTestDAO;
import org.hibernate.test.cache.jbc.functional.util.DualNodeJtaTransactionManagerImpl;
import org.hibernate.test.cache.jbc.functional.util.DualNodeTestUtil;
import org.hibernate.test.cache.jbc.functional.util.IsolatedCacheTestSetup;
import org.hibernate.test.cache.jbc.functional.util.TestCacheInstanceManager;
import org.hibernate.test.cache.jbc.functional.util.TestJBossCacheRegionFactory;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A test that a Session.refresh(...) operation works properly when the
 * item being refreshed is in the 2nd level cache.
 * 
 * Uses the infrastructure of our dual-node tests, but 
 * {@link #configureSecondNode(Configuration)} plays a trick and disables
 * the 2nd level cache on the second node.  We then use that second node
 * to simulate an external process that changes the DB while bypassing the 
 * cache.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class PessimisticSessionRefreshTest extends DualNodeTestCaseBase
{
   public static final String OUR_PACKAGE = PessimisticSessionRefreshTest.class.getPackage().getName();
   
   private static final String CACHE_CONFIG = "pessimistic-entity";
   
   protected final Logger log = LoggerFactory.getLogger(getClass());

   static int test = 0;
   
   private Cache<Object, Object> localCache;
   
   /**
    * Create a new PessimisticSessionRefreshTest.
    * 
    * @param x
    */
   public PessimisticSessionRefreshTest(String x)
   {
      super(x);
   }
   
   public static Test suite() throws Exception {
       TestSuite suite = new TestSuite(PessimisticSessionRefreshTest.class);
       String[] acctClasses = { OUR_PACKAGE + ".Account", OUR_PACKAGE + ".AccountHolder" };
       return new IsolatedCacheTestSetup(suite, acctClasses, CACHE_CONFIG);
   }
   
   // --------------------------------------------------------------- Overrides

   /**
    * Disables use of the second level cache for this session factory.
    * 
    * {@inheritDoc} 
    */
   @Override
   protected void configureSecondNode(Configuration cfg)
   {
      super.configureSecondNode(cfg);
      cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
   }

   @Override
   protected void configureCacheFactory(Configuration cfg)
   {
      cfg.setProperty(MultiplexingCacheInstanceManager.ENTITY_CACHE_RESOURCE_PROP, 
            getEntityCacheConfigName()); 
   }

   @Override
   protected Class<?> getCacheRegionFactory()
   {
      return TestJBossCacheRegionFactory.class;
   }

   @Override
   protected boolean getUseQueryCache()
   {
      return false;
   }

   protected String getEntityCacheConfigName() {
       return CACHE_CONFIG;
   } 

   @Override
   public String[] getMappings()
   {
      return new String[] { "cache/jbc/functional/classloader/Account.hbm.xml" };
   }
   
   @Override
   protected void cleanupTransactionManagement() {
      // Don't clean up the managers, just the transactions
      // Managers are still needed by the long-lived caches
      DualNodeJtaTransactionManagerImpl.cleanupTransactions();
   }
   
   // ------------------------------------------------------------------  Tests
   
   public void testRefreshAfterExternalChange() throws Exception
   {
      // First session factory uses a cache
      CacheManager localManager = TestCacheInstanceManager.getTestCacheManager(DualNodeTestUtil.LOCAL);
      this.localCache = localManager.getCache(getEntityCacheConfigName(), true);      
      TransactionManager localTM = localCache.getConfiguration().getRuntimeConfig().getTransactionManager();
      SessionFactory localFactory = getEnvironment().getSessionFactory();
      
      // Second session factory doesn't; just needs a transaction manager
      TransactionManager remoteTM = DualNodeJtaTransactionManagerImpl.getInstance(DualNodeTestUtil.REMOTE);
      SessionFactory remoteFactory = getSecondNodeEnvironment().getSessionFactory();
      
      ClassLoaderTestDAO dao0 = new ClassLoaderTestDAO(localFactory, localTM);      
      ClassLoaderTestDAO dao1 = new ClassLoaderTestDAO(remoteFactory, remoteTM);
      
      Integer id = new Integer(1);
      dao0.createAccount(dao0.getSmith(), id, new Integer(5), DualNodeTestUtil.LOCAL);
      
      // Basic sanity check
      Account acct1 = dao1.getAccount(id);
      assertNotNull(acct1);
      assertEquals(DualNodeTestUtil.LOCAL, acct1.getBranch());
      
      // This dao's session factory isn't caching, so cache won't see this change
      dao1.updateAccountBranch(id, DualNodeTestUtil.REMOTE);
      
      // dao1's session doesn't touch the cache, 
      // so reading from dao0 should show a stale value from the cache
      // (we check to confirm the cache is used)
      Account acct0 = dao0.getAccount(id);
      assertNotNull(acct0);
      assertEquals(DualNodeTestUtil.LOCAL, acct0.getBranch());
      
      // Now call session.refresh and confirm we get the correct value
      acct0 = dao0.getAccountWithRefresh(id);
      assertNotNull(acct0);
      assertEquals(DualNodeTestUtil.REMOTE, acct0.getBranch());
      
      // Double check with a brand new session, in case the other session
      // for some reason bypassed the 2nd level cache
      ClassLoaderTestDAO dao0A = new ClassLoaderTestDAO(localFactory, localTM);
      Account acct0A = dao0A.getAccount(id);
      assertNotNull(acct0A);
      assertEquals(DualNodeTestUtil.REMOTE, acct0A.getBranch());
   }

}
