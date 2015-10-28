/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.entity;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.test.cache.infinispan.AbstractExtraAPITest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the "extra API" in EntityRegionAccessStrategy;.
 * <p>
 * By "extra API" we mean those methods that are superfluous to the 
 * function of the JBC integration, where the impl is a no-op or a static
 * false return value, UnsupportedOperationException, etc.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class EntityRegionExtraAPITest extends AbstractExtraAPITest<EntityRegionAccessStrategy> {
	public static final String VALUE1 = "VALUE1";
	public static final String VALUE2 = "VALUE2";

	@Override
	protected EntityRegionAccessStrategy getAccessStrategy() {
		return environment.getEntityRegion( REGION_NAME, CACHE_DATA_DESCRIPTION).buildAccessStrategy( accessType );
	}

	@Test
	@SuppressWarnings( {"UnnecessaryBoxing"})
	public void testAfterInsert() {
		boolean retval = accessStrategy.afterInsert(SESSION,	KEY, VALUE1, Integer.valueOf( 1 ));
		assertEquals(accessType == AccessType.NONSTRICT_READ_WRITE, retval);
	}

	@Test
	@SuppressWarnings( {"UnnecessaryBoxing"})
	public void testAfterUpdate() {
		if (accessType == AccessType.READ_ONLY) {
			return;
		}
		boolean retval = accessStrategy.afterUpdate(SESSION,	KEY, VALUE2, Integer.valueOf( 1 ), Integer.valueOf( 2 ),	new MockSoftLock());
		assertEquals(accessType == AccessType.NONSTRICT_READ_WRITE, retval);
	}
}
