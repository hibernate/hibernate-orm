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
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

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
		return environment.getEntityRegion( REGION_NAME, CACHE_DATA_DESCRIPTION).buildAccessStrategy( getAccessType() );
	}

	protected AccessType getAccessType() {
		return AccessType.TRANSACTIONAL;
	}

	@Test
	@SuppressWarnings( {"UnnecessaryBoxing"})
	public void testAfterInsert() {
		assertFalse("afterInsert always returns false",	accessStrategy.afterInsert(SESSION,	KEY, VALUE1, Integer.valueOf( 1 )));
	}

	@Test
	@SuppressWarnings( {"UnnecessaryBoxing"})
	public void testAfterUpdate() {
		assertFalse("afterInsert always returns false",	accessStrategy.afterUpdate(
						SESSION,	KEY, VALUE2, Integer.valueOf( 1 ), Integer.valueOf( 2 ),	new MockSoftLock()));
	}

	public static class Transactional extends EntityRegionExtraAPITest {
		@Override
		protected AccessType getAccessType() {
			return AccessType.TRANSACTIONAL;
		}

		@Override
		protected boolean useTransactionalCache() {
			return true;
		}
	}

	public static class ReadWrite extends EntityRegionExtraAPITest {
		@Override
		protected AccessType getAccessType() {
			return AccessType.READ_WRITE;
		}
	}


	public static class ReadOnly extends EntityRegionExtraAPITest {
		@Override
		protected AccessType getAccessType() {
			return AccessType.READ_ONLY;
		}

		@Test(expected = UnsupportedOperationException.class)
		@Override
		public void testAfterUpdate() {
			accessStrategy.afterUpdate(null, KEY, VALUE2, 1, 2, new MockSoftLock());
		}
	}
}
