/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.entity;

import org.hibernate.cache.spi.access.AccessType;
import org.junit.Test;

/**
 * Tests for the "extra API" in EntityRegionAccessStrategy;
 * <p/>
 * By "extra API" we mean those methods that are superfluous to the
 * function of the Infinispan integration, where the impl is a no-op or a static
 * false return value, UnsupportedOperationException, etc.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class ReadOnlyExtraAPITestCase extends TransactionalExtraAPITestCase {
	@Override
	protected AccessType getAccessType() {
		return AccessType.READ_ONLY;
	}

	@Test(expected = UnsupportedOperationException.class)
	@Override
	public void testAfterUpdate() {
		getEntityAccessStrategy().afterUpdate(KEY, VALUE2, 1, 2, new MockSoftLock());
	}
}
