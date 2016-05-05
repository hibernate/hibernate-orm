/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.entity;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.test.cache.infinispan.util.TestingKeyFactory;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Base class for tests of TRANSACTIONAL access.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class EntityRegionReadOnlyAccessTest extends AbstractEntityRegionAccessStrategyTest {

	@Override
	protected AccessType getAccessType() {
		return AccessType.READ_ONLY;
	}


	protected void putFromLoadTest(boolean minimal) throws Exception {
		final Object KEY = TestingKeyFactory.generateEntityCacheKey( KEY_BASE + testCount++ );

		Object expected = isUsingInvalidation() ? null : VALUE1;

		long txTimestamp = System.currentTimeMillis();
		SessionImplementor session = mockedSession();
		withTx(localEnvironment, session, () -> {
			assertNull(localAccessStrategy.get(session, KEY, System.currentTimeMillis()));
			if (minimal)
				localAccessStrategy.putFromLoad(session, KEY, VALUE1, txTimestamp, 1, true);
			else
				localAccessStrategy.putFromLoad(session, KEY, VALUE1, txTimestamp, 1);
			return null;
		});

		assertEquals(VALUE1, localAccessStrategy.get(session, KEY, System.currentTimeMillis()));
		assertEquals(expected, remoteAccessStrategy.get(mockedSession(), KEY, System.currentTimeMillis()));
	}

	@Test(expected = UnsupportedOperationException.class)
	@Override
	public void testUpdate() throws Exception {
		final Object KEY = TestingKeyFactory.generateEntityCacheKey( KEY_BASE + testCount++ );
		SessionImplementor session = mockedSession();
		SoftLock softLock = localAccessStrategy.lockItem(session, KEY, null);
		localAccessStrategy.update(session, KEY, VALUE2, 2, 1);
		localAccessStrategy.unlockItem(session, KEY, softLock);
	}

	@Ignore
	@Override
	public void testContestedPutFromLoad() throws Exception {
	}

	public static class Invalidation extends EntityRegionReadOnlyAccessTest {
		@Test
		@Override
		public void testCacheConfiguration() {
			assertTrue("Using Invalidation", isUsingInvalidation());
		}
	}
}
