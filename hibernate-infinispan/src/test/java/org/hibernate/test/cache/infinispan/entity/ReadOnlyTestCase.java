/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.entity;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests READ_ONLY access when pessimistic locking and invalidation are used.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class ReadOnlyTestCase extends AbstractReadOnlyAccessTestCase {
	@Override
	protected String getConfigurationName() {
		return "entity";
	}

	@Test
	@Override
	public void testCacheConfiguration() {
		assertTrue("Using Invalidation", isUsingInvalidation());
	}
}
