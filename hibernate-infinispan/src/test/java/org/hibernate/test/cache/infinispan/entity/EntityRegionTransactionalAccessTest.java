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
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.test.cache.infinispan.util.TestingKeyFactory;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.jboss.logging.Logger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Base class for tests of TRANSACTIONAL access.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class EntityRegionTransactionalAccessTest extends AbstractEntityRegionAccessStrategyTest {
	private static final Logger log = Logger.getLogger( EntityRegionTransactionalAccessTest.class );

	@Override
	protected AccessType getAccessType() {
		return AccessType.TRANSACTIONAL;
	}

	@Override
	protected Class<? extends RegionFactory> getRegionFactoryClass() {
		return TestInfinispanRegionFactory.Transactional.class;
	}

	/**
	 * @author Galder Zamarreño
	 */
	public static class Invalidation extends EntityRegionTransactionalAccessTest {
		@Test
		@Override
		public void testCacheConfiguration() {
			assertTrue(isTransactional());
			assertTrue("Using Invalidation", isUsingInvalidation());
			assertTrue("Synchronous mode", isSynchronous());
		}
	}
}
