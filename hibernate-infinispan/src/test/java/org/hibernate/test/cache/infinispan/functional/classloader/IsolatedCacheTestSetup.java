/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.cache.infinispan.functional.classloader;

import junit.framework.Test;

import org.hibernate.test.cache.infinispan.functional.cluster.ClusterAwareRegionFactory;
import org.hibernate.test.cache.infinispan.functional.cluster.DualNodeJtaTransactionManagerImpl;

/**
 * A TestSetup that uses SelectedClassnameClassLoader to ensure that certain classes are not visible
 * to Infinispan or JGroups' classloader.
 * 
 * @author Galder Zamarreño
 */
public class IsolatedCacheTestSetup extends SelectedClassnameClassLoaderTestSetup {

   private String[] isolatedClasses;
   private String cacheConfig;

   /**
    * Create a new IsolatedCacheTestSetup.
    */
   public IsolatedCacheTestSetup(Test test, String[] isolatedClasses) {
      super(test, null, null, isolatedClasses);
      this.isolatedClasses = isolatedClasses;
   }

   @Override
   protected void setUp() throws Exception {
      super.setUp();

      // At this point the TCCL cannot see the isolatedClasses
      // We want the caches to use this CL as their default classloader
      ClassLoader tccl = Thread.currentThread().getContextClassLoader();

      // Now make the isolatedClasses visible to the test driver itself
      SelectedClassnameClassLoader visible = new SelectedClassnameClassLoader(isolatedClasses, null, null, tccl);
      Thread.currentThread().setContextClassLoader(visible);
   }

   @Override
   protected void tearDown() throws Exception {
      try {
         super.tearDown();
      } finally {
         ClusterAwareRegionFactory.clearCacheManagers();
         DualNodeJtaTransactionManagerImpl.cleanupTransactions();
         DualNodeJtaTransactionManagerImpl.cleanupTransactionManagers();
      }
   }

}
