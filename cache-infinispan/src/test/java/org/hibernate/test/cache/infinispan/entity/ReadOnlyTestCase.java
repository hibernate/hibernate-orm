/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
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
package org.hibernate.test.cache.infinispan.entity;

import org.hibernate.test.cache.infinispan.util.CacheTestUtil;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests READ_ONLY access when pessimistic locking and invalidation are used.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class ReadOnlyTestCase extends AbstractReadOnlyAccessTestCase {

   /**
    * Create a new PessimisticTransactionalAccessTestCase.
    * 
    * @param name
    */
   public ReadOnlyTestCase(String name) {
      super(name);
   }

   public static Test suite() throws Exception {
      TestSuite suite = CacheTestUtil.createFailureExpectedSuite(ReadOnlyTestCase.class);
      return getTestSetup(suite, "entity");
   }

   // Known failures

   // Overrides

   @Override
   public void testCacheConfiguration() {
      assertTrue("Using Invalidation", isUsingInvalidation());
   }

}
