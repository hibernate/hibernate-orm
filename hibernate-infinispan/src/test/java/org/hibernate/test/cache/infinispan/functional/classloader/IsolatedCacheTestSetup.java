/*
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates. All rights reserved.
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
