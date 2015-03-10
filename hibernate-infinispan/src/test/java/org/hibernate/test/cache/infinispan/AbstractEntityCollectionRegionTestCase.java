/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.cache.infinispan;

import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TransactionalDataRegion;
import org.hibernate.cache.spi.access.AccessType;

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
