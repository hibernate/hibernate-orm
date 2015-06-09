/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan;

import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TransactionalDataRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Base class for tests of EntityRegion and CollectionRegion implementations.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractEntityCollectionRegionTestCase extends AbstractRegionImplTestCase {
	protected static CacheDataDescription MUTABLE_NON_VERSIONED = new CacheDataDescriptionImpl(true, false, ComparableComparator.INSTANCE, null);

	@Test
	public void testSupportedAccessTypes() throws Exception {
		supportedAccessTypeTest();
	}

	private void supportedAccessTypeTest() throws Exception {
		StandardServiceRegistryBuilder ssrb = CacheTestUtil.buildBaselineStandardServiceRegistryBuilder(
				"test",
				InfinispanRegionFactory.class,
				true,
				false
		);
		ssrb.applySetting( InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, "entity" );
		final StandardServiceRegistry registry = ssrb.build();
		try {
			InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
					registry,
					getCacheTestSupport()
			);
			supportedAccessTypeTest( regionFactory, CacheTestUtil.toProperties( ssrb.getSettings() ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	/**
	 * Creates a Region using the given factory, and then ensure that it handles calls to
	 * buildAccessStrategy as expected when all the various {@link AccessType}s are passed as
	 * arguments.
	 */
	protected abstract void supportedAccessTypeTest(RegionFactory regionFactory, Properties properties);

	@Test
	public void testIsTransactionAware() throws Exception {
		StandardServiceRegistryBuilder ssrb = CacheTestUtil.buildBaselineStandardServiceRegistryBuilder(
				"test",
				InfinispanRegionFactory.class,
				true,
				false
		);
		final StandardServiceRegistry registry = ssrb.build();
		try {
			Properties properties = CacheTestUtil.toProperties( ssrb.getSettings() );
			InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
					registry,
					getCacheTestSupport()
			);
			TransactionalDataRegion region = (TransactionalDataRegion) createRegion(
					regionFactory,
					"test/test",
					properties,
					getCacheDataDescription()
			);
			assertTrue( "Region is transaction-aware", region.isTransactionAware() );
			CacheTestUtil.stopRegionFactory( regionFactory, getCacheTestSupport() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	public void testGetCacheDataDescription() throws Exception {
		StandardServiceRegistryBuilder ssrb = CacheTestUtil.buildBaselineStandardServiceRegistryBuilder(
				"test",
				InfinispanRegionFactory.class,
				true,
				false
		);
		final StandardServiceRegistry registry = ssrb.build();
		try {
			Properties properties = CacheTestUtil.toProperties( ssrb.getSettings() );
			InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
					registry,
					getCacheTestSupport()
			);
			TransactionalDataRegion region = (TransactionalDataRegion) createRegion(
					regionFactory,
					"test/test",
					properties,
					getCacheDataDescription()
			);
			CacheDataDescription cdd = region.getCacheDataDescription();
			assertNotNull( cdd );
			CacheDataDescription expected = getCacheDataDescription();
			assertEquals( expected.isMutable(), cdd.isMutable() );
			assertEquals( expected.isVersioned(), cdd.isVersioned() );
			assertEquals( expected.getVersionComparator(), cdd.getVersionComparator() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}
}
