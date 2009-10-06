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

package org.hibernate.test.cache.jbc;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.util.CacheManagerTestSetup;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheManager;

/**
 * Tests the validity of the JBC configs in jbc2-configs.xml.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class JbcConfigsXmlValidityTestCase extends AbstractJBossCacheTestCase
{
   private static final AtomicReference<CacheManager> cacheManagerRef = new AtomicReference<CacheManager>();
   
   private static final Set<String> stdConfigs = new HashSet<String>();
   
   static
   {
      stdConfigs.add("optimistic-entity");
      stdConfigs.add("pessimistic-entity");
      stdConfigs.add("pessimistic-entity-repeatable");
      stdConfigs.add("optimistic-shared");
      stdConfigs.add("pessimistic-shared");
      stdConfigs.add("pessimistic-shared-repeatable");
      stdConfigs.add("local-query");
      stdConfigs.add("replicated-query");
      stdConfigs.add("timestamps-cache");
   }
   
   private CacheManager mgr;
   private String cacheName;
   private Cache cache;
   
   /**
    * Create a new JbcConfigsXmlValidityTestCase.
    * 
    * @param name
    */
   public JbcConfigsXmlValidityTestCase(String name)
   {
      super(name);
   }
   
   public static Test suite() throws Exception {
       TestSuite suite = new TestSuite(JbcConfigsXmlValidityTestCase.class);
       return new CacheManagerTestSetup(suite, cacheManagerRef);
   }
   
   
   
   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      this.mgr = cacheManagerRef.get();
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
      
      if (cache != null)
      {
         try
         {
            mgr.releaseCache(this.cacheName);
         }
         catch (Exception ignored) {}
         
         cache = null;
      }
      
      mgr = null;
   }

   public void testOptimisticEntity() throws Exception
   {
      stdConfigTest("optimistic-entity");
   }
   
   public void testPessimisticEntity() throws Exception
   {
      stdConfigTest("pessimistic-entity");
   }
   
   public void testPessimisticEntityRepeatable() throws Exception
   {
      stdConfigTest("pessimistic-entity-repeatable");
   }
   
   public void testOptimisticShared() throws Exception
   {
      stdConfigTest("optimistic-shared");
   }
   
   public void testPessimisticShared() throws Exception
   {
      stdConfigTest("pessimistic-shared");
   }
   
   public void testPessimisticSharedRepeatable() throws Exception
   {
      stdConfigTest("pessimistic-shared-repeatable");
   }
   
   public void testLocalQuery() throws Exception
   {
      stdConfigTest("local-query");
   }
   
   public void testReplicatedQuery() throws Exception
   {
      stdConfigTest("replicated-query");
   }
   
   public void testTimestampsCache() throws Exception
   {
      stdConfigTest("timestamps-cache");
   }
   
   public void testAdditionalConfigs() throws Exception
   {
      Set<String> names = new HashSet<String>(this.mgr.getConfigurationNames());
      names.removeAll(stdConfigs);
      for (String name : names)
      {
         configTest(name);
      }
   }
   
   private void stdConfigTest(String configName) throws Exception
   {
      assertTrue(this.mgr.getConfigurationNames().contains(configName));
      configTest(configName);
   }
   
   private void configTest(String configName) throws Exception
   {      
      this.cacheName = configName;
      this.cache = mgr.getCache(configName, true);
      this.cache.start();
      this.mgr.releaseCache(this.cacheName);
      this.cache = null;      
   }
}
