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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;

import org.hibernate.cache.access.AccessType;
import org.infinispan.transaction.tm.BatchModeTransactionManager;

/**
 * Base class for tests of TRANSACTIONAL access.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractTransactionalAccessTestCase extends AbstractEntityRegionAccessStrategyTestCase {

   public AbstractTransactionalAccessTestCase(String name) {
      super(name);
   }

   @Override
   protected AccessType getAccessType() {
      return AccessType.TRANSACTIONAL;
   }

   public void testContestedPutFromLoad() throws Exception {

      final String KEY = KEY_BASE + testCount++;

      localAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));

      final CountDownLatch pferLatch = new CountDownLatch(1);
      final CountDownLatch pferCompletionLatch = new CountDownLatch(1);
      final CountDownLatch commitLatch = new CountDownLatch(1);
      final CountDownLatch completionLatch = new CountDownLatch(1);

      Thread blocker = new Thread("Blocker") {

         public void run() {

            try {
               long txTimestamp = System.currentTimeMillis();
               BatchModeTransactionManager.getInstance().begin();

               assertEquals("Correct initial value", VALUE1, localAccessStrategy.get(KEY,
                        txTimestamp));

               localAccessStrategy.update(KEY, VALUE2, new Integer(2), new Integer(1));

               pferLatch.countDown();
               commitLatch.await();

               BatchModeTransactionManager.getInstance().commit();
            } catch (Exception e) {
               log.error("node1 caught exception", e);
               node1Exception = e;
               rollback();
            } catch (AssertionFailedError e) {
               node1Failure = e;
               rollback();
            } finally {
               completionLatch.countDown();
            }
         }
      };

      Thread putter = new Thread("Putter") {

         public void run() {

            try {
               long txTimestamp = System.currentTimeMillis();
               BatchModeTransactionManager.getInstance().begin();

               localAccessStrategy.putFromLoad(KEY, VALUE1, txTimestamp, new Integer(1));

               BatchModeTransactionManager.getInstance().commit();
            } catch (Exception e) {
               log.error("node1 caught exception", e);
               node1Exception = e;
               rollback();
            } catch (AssertionFailedError e) {
               node1Failure = e;
               rollback();
            } finally {
               pferCompletionLatch.countDown();
            }
         }
      };

      blocker.start();
      assertTrue("Active tx has done an update", pferLatch.await(1, TimeUnit.SECONDS));
      putter.start();
      assertTrue("putFromLoadreturns promtly", pferCompletionLatch.await(10, TimeUnit.MILLISECONDS));

      commitLatch.countDown();

      assertTrue("Threads completed", completionLatch.await(1, TimeUnit.SECONDS));

      assertThreadsRanCleanly();

      long txTimestamp = System.currentTimeMillis();
      assertEquals("Correct node1 value", VALUE2, localAccessStrategy.get(KEY, txTimestamp));
   }

}
