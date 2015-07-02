/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.entity;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.test.cache.infinispan.AbstractNonFunctionalTestCase;
import org.hibernate.test.cache.infinispan.NodeEnvironment;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.hibernate.test.cache.infinispan.util.TestingKeyFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

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
public class TransactionalExtraAPITestCase extends AbstractNonFunctionalTestCase {
	public static final String REGION_NAME = "test/com.foo.test";
	public static final Object KEY = TestingKeyFactory.generateEntityCacheKey( "KEY" );
	public static final String VALUE1 = "VALUE1";
	public static final String VALUE2 = "VALUE2";
	protected static final CacheDataDescriptionImpl CACHE_DATA_DESCRIPTION
			= new CacheDataDescriptionImpl(true, false, ComparableComparator.INSTANCE, null);

	private NodeEnvironment environment;
	private EntityRegionAccessStrategy accessStrategy;

	@Before
	public final void prepareLocalAccessStrategy() throws Exception {
		environment = new NodeEnvironment( createStandardServiceRegistryBuilder() );
		environment.prepare();

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		accessStrategy = environment.getEntityRegion( REGION_NAME, CACHE_DATA_DESCRIPTION).buildAccessStrategy( getAccessType() );
   }

	protected StandardServiceRegistryBuilder createStandardServiceRegistryBuilder() {
		StandardServiceRegistryBuilder ssrb = CacheTestUtil.buildBaselineStandardServiceRegistryBuilder(
				REGION_PREFIX,
				InfinispanRegionFactory.class,
				true,
				false
		);
		ssrb.applySetting( InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, getCacheConfigName() );
		return ssrb;
	}

	@After
	public final void releaseLocalAccessStrategy() throws Exception {
		if ( environment != null ) {
			environment.release();
		}
	}

	protected final EntityRegionAccessStrategy getEntityAccessStrategy() {
		return accessStrategy;
	}

	protected String getCacheConfigName() {
		return "entity";
	}

	protected AccessType getAccessType() {
		return AccessType.TRANSACTIONAL;
	}

	@Test
	@SuppressWarnings( {"UnnecessaryBoxing"})
	public void testLockItem() {
		assertNull( getEntityAccessStrategy().lockItem( KEY, Integer.valueOf( 1 ) ) );
	}

	@Test
	public void testLockRegion() {
		assertNull( getEntityAccessStrategy().lockRegion() );
	}

	@Test
	public void testUnlockItem() {
		getEntityAccessStrategy().unlockItem( KEY, new MockSoftLock() );
	}

	@Test
	public void testUnlockRegion() {
		getEntityAccessStrategy().unlockItem( KEY, new MockSoftLock() );
	}

	@Test
	@SuppressWarnings( {"UnnecessaryBoxing"})
	public void testAfterInsert() {
		assertFalse(
				"afterInsert always returns false",
				getEntityAccessStrategy().afterInsert(
						KEY,
						VALUE1,
						Integer.valueOf( 1 )
				)
		);
	}

	@Test
	@SuppressWarnings( {"UnnecessaryBoxing"})
	public void testAfterUpdate() {
		assertFalse(
				"afterInsert always returns false",
				getEntityAccessStrategy().afterUpdate(
						KEY,
						VALUE2,
						Integer.valueOf( 1 ),
						Integer.valueOf( 2 ),
						new MockSoftLock()
				)
		);
	}

	public static class MockSoftLock implements SoftLock {
	}
}
