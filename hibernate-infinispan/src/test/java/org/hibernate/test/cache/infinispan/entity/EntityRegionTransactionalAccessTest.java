/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.entity;

import org.hibernate.cache.spi.access.AccessType;
import org.jboss.logging.Logger;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Base class for tests of TRANSACTIONAL access.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class EntityRegionTransactionalAccessTest extends AbstractEntityRegionAccessStrategyTest {
	private static final Logger log = Logger.getLogger( EntityRegionTransactionalAccessTest.class );

	@Override
	protected AccessType getAccessType() {
		return AccessType.TRANSACTIONAL;
	}

	@Override
	protected boolean useTransactionalCache() {
		return true;
	}

	@Test
	@Override
	public void testCacheConfiguration() {
		assertTrue(isTransactional());
		assertTrue("Synchronous mode", isSynchronous());
	}
}
