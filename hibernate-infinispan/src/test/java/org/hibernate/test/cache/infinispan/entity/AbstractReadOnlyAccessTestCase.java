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

import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.junit.Test;

import org.hibernate.cache.spi.access.AccessType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Base class for tests of TRANSACTIONAL access.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractReadOnlyAccessTestCase extends AbstractEntityRegionAccessStrategyTestCase {

   @Override
   protected AccessType getAccessType() {
      return AccessType.READ_ONLY;
   }

   @Test
   @Override
   public void testPutFromLoad() throws Exception {
      putFromLoadTest(false);
   }

   @Test
   @Override
   public void testPutFromLoadMinimal() throws Exception {
      putFromLoadTest(true);
   }

   private void putFromLoadTest(boolean minimal) throws Exception {

      final String KEY = KEY_BASE + testCount++;

      long txTimestamp = System.currentTimeMillis();
      BatchModeTransactionManager.getInstance().begin();
      assertNull(localAccessStrategy.get(KEY, System.currentTimeMillis()));
      if (minimal)
         localAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, 1, true);
      else
         localAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, 1);

      sleep(250);
      Object expected = isUsingInvalidation() ? null : VALUE1;
      assertEquals(expected, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));

      BatchModeTransactionManager.getInstance().commit();
      assertEquals(VALUE1, localAccessStrategy.get(KEY, System.currentTimeMillis()));
      assertEquals(expected, remoteAccessStrategy.get(KEY, System.currentTimeMillis()));
   }

   @Test(expected = UnsupportedOperationException.class)
   @Override
   public void testUpdate() throws Exception {
      localAccessStrategy.update(KEY_BASE + testCount++,
            VALUE2, 2, 1);
   }

}
