/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.entity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.test.cache.infinispan.util.TestingKeyFactory;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Base class for tests of TRANSACTIONAL access.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractTransactionalAccessTestCase extends AbstractEntityRegionAccessStrategyTestCase {
	private static final Logger log = Logger.getLogger( AbstractTransactionalAccessTestCase.class );

	@Override
   protected AccessType getAccessType() {
      return AccessType.TRANSACTIONAL;
   }

    public void testContestedPutFromLoad() throws Exception {

        final Object KEY = TestingKeyFactory.generateEntityCacheKey( KEY_BASE + testCount++ );

        localAccessStrategy.putFromLoad(KEY, VALUE1, System.currentTimeMillis(), new Integer(1));

        final CountDownLatch pferLatch = new CountDownLatch(1);
        final CountDownLatch pferCompletionLatch = new CountDownLatch(1);
        final CountDownLatch commitLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(1);

        Thread blocker = new Thread("Blocker") {

            @Override
            public void run() {

                try {
                    long txTimestamp = System.currentTimeMillis();
                    BatchModeTransactionManager.getInstance().begin();

                    assertEquals("Correct initial value", VALUE1, localAccessStrategy.get(KEY, txTimestamp));

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

            @Override
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
