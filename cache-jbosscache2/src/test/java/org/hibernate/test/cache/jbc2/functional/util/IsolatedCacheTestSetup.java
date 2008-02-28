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

package org.hibernate.test.cache.jbc2.functional.util;

import junit.framework.Test;

import org.hibernate.test.util.SelectedClassnameClassLoader;
import org.hibernate.test.util.SelectedClassnameClassLoaderTestSetup;
import org.jboss.cache.Cache;
import org.jboss.cache.config.Configuration;

/**
 * A TestSetup that uses SelectedClassnameClassLoader to ensure that
 * certain classes are not visible to JBoss Cache or JGroups' classloader.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class IsolatedCacheTestSetup extends SelectedClassnameClassLoaderTestSetup
{

   public static final String DEF_CACHE_FACTORY_RESOURCE = "org/hibernate/cache/jbc2/builder/jbc2-configs.xml";    
   public static final String DEF_JGROUPS_RESOURCE = "org/hibernate/cache/jbc2/builder/jgroups-stacks.xml";
   
   private String[] isolatedClasses;
   private String cacheConfig;
   
   /**
    * Create a new IsolatedCacheTestSetup.
    */
   public IsolatedCacheTestSetup(Test test,
                                 String[] isolatedClasses,
                                 String cacheConfig)
   {      
      super(test, null, null, isolatedClasses);
      this.isolatedClasses = isolatedClasses;
      this.cacheConfig = cacheConfig;
   }

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      
      // At this point the TCCL cannot see the isolatedClasses
      ClassLoader tccl = Thread.currentThread().getContextClassLoader();
      
      org.jgroups.ChannelFactory cf = new org.jgroups.JChannelFactory();
      cf.setMultiplexerConfig(DEF_JGROUPS_RESOURCE);
      
      org.jboss.cache.CacheManagerImpl cm = new org.jboss.cache.CacheManagerImpl(DEF_CACHE_FACTORY_RESOURCE, cf);
      cm.start();
      TestCacheInstanceManager.addTestCacheManager(DualNodeTestUtil.LOCAL, cm);
      
      // Inject the desired defaultClassLoader into our caches
      Configuration cfg = cm.getConfigurationRegistry().getConfiguration(cacheConfig);
      Cache cache = new CustomClassLoaderCacheFactory(tccl).createCache(cfg, false);
      cache.getConfiguration().getRuntimeConfig().setMuxChannelFactory(cm.getChannelFactory());      
      cm.registerCache(cache, cacheConfig);
      
      cf = new org.jgroups.JChannelFactory();
      cf.setMultiplexerConfig(DEF_JGROUPS_RESOURCE);
      
      cm = new org.jboss.cache.CacheManagerImpl(DEF_CACHE_FACTORY_RESOURCE, cf);
      cm.start();
      TestCacheInstanceManager.addTestCacheManager(DualNodeTestUtil.REMOTE, cm);
      
      cfg = cm.getConfigurationRegistry().getConfiguration(cacheConfig);
      cache = new CustomClassLoaderCacheFactory(tccl).createCache(cfg, false);
      cache.getConfiguration().getRuntimeConfig().setMuxChannelFactory(cm.getChannelFactory()); 
      cm.registerCache(cache, cacheConfig);
      
      // Now make the isolatedClasses visible
      SelectedClassnameClassLoader visible = new SelectedClassnameClassLoader(isolatedClasses, null, null, tccl);
      Thread.currentThread().setContextClassLoader(visible);
   }

   @Override
   protected void tearDown() throws Exception
   {
      try {
         super.tearDown();
      }
      finally {
         TestCacheInstanceManager.clearCacheManagers();
         DualNodeJtaTransactionManagerImpl.cleanupTransactions();
         DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
      }
   }

}
