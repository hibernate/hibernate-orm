/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.collection;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.test.cache.infinispan.AbstractNonFunctionalTestCase;
import org.hibernate.test.cache.infinispan.NodeEnvironment;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.hibernate.test.cache.infinispan.util.TestingKeyFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * TransactionalExtraAPITestCase.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class TransactionalExtraAPITestCase extends AbstractNonFunctionalTestCase {

	public static final String REGION_NAME = "test/com.foo.test";
	public static final Object KEY = TestingKeyFactory.generateCollectionCacheKey( "KEY" );
	public static final CacheDataDescription CACHE_DATA_DESCRIPTION
			= new CacheDataDescriptionImpl(false, false, ComparableComparator.INSTANCE, null);

	private NodeEnvironment environment;
	private static CollectionRegionAccessStrategy accessStrategy;

	@Before
	public final void prepareLocalAccessStrategy() throws Exception {
		environment = new NodeEnvironment( createStandardServiceRegistryBuilder() );
		environment.prepare();

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		accessStrategy = environment.getCollectionRegion( REGION_NAME, CACHE_DATA_DESCRIPTION).buildAccessStrategy( getAccessType() );
	}

	protected StandardServiceRegistryBuilder createStandardServiceRegistryBuilder() {
		StandardServiceRegistryBuilder ssrb = CacheTestUtil.buildBaselineStandardServiceRegistryBuilder(
				REGION_PREFIX, InfinispanRegionFactory.class, true, false
		);
		ssrb.applySetting( InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, getCacheConfigName() );
		return ssrb;
	}

	protected String getCacheConfigName() {
		return "entity";
	}

	protected AccessType getAccessType() {
		return AccessType.TRANSACTIONAL;
	}

	@After
	public final void releaseLocalAccessStrategy() throws Exception {
		if ( environment != null ) {
			environment.release();
		}
	}

	protected CollectionRegionAccessStrategy getCollectionAccessStrategy() {
		return accessStrategy;
	}

	@Test
	public void testLockItem() {
		assertNull( getCollectionAccessStrategy().lockItem( KEY, new Integer( 1 ) ) );
	}

	@Test
	public void testLockRegion() {
		assertNull( getCollectionAccessStrategy().lockRegion() );
	}

	@Test
	public void testUnlockItem() {
		getCollectionAccessStrategy().unlockItem( KEY, new MockSoftLock() );
	}

	@Test
	public void testUnlockRegion() {
		getCollectionAccessStrategy().unlockItem( KEY, new MockSoftLock() );
	}

	public static class MockSoftLock implements SoftLock {
	}
}
