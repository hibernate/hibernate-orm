/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.entity;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.test.cache.infinispan.util.TestingKeyFactory;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.junit.Test;

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

      final Object KEY = TestingKeyFactory.generateEntityCacheKey( KEY_BASE + testCount++ );

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
      final Object KEY = TestingKeyFactory.generateEntityCacheKey( KEY_BASE + testCount++ );
      localAccessStrategy.update( KEY, VALUE2, 2, 1);
   }

}
