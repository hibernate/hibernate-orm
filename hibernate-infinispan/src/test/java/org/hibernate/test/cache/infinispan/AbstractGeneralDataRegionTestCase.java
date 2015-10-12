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

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.hibernate.cfg.Configuration;
import org.infinispan.AdvancedCache;
import org.jboss.logging.Logger;
import org.junit.Test;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.spi.GeneralDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;

import static org.hibernate.test.cache.infinispan.util.CacheTestUtil.assertEqualsEventually;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Base class for tests of QueryResultsRegion and TimestampsRegion.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractGeneralDataRegionTestCase extends AbstractRegionImplTestCase {
	private static final Logger log = Logger.getLogger( AbstractGeneralDataRegionTestCase.class );

	protected static final String KEY = "Key";

	protected static final String VALUE1 = "value1";
	protected static final String VALUE2 = "value2";
	protected static final String VALUE3 = "value3";

	protected Configuration createConfiguration() {
		return CacheTestUtil.buildConfiguration( "test", InfinispanRegionFactory.class, false, true );
	}

	@Override
	protected void putInRegion(Region region, Object key, Object value) {
		((GeneralDataRegion) region).put( key, value );
	}

	@Override
	protected void removeFromRegion(Region region, Object key) {
		((GeneralDataRegion) region).evict( key );
	}

	@Test
	public void testEvict() throws Exception {
		evictOrRemoveTest();
	}

	private void evictOrRemoveTest() throws Exception {
		Configuration cfg = createConfiguration();
		InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
				new ServiceRegistryBuilder().applySettings( cfg.getProperties() ).buildServiceRegistry(),
				cfg,
				getCacheTestSupport()
		);
		boolean invalidation = false;

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		final GeneralDataRegion localRegion = (GeneralDataRegion) createRegion(
				regionFactory,
				getStandardRegionName( REGION_PREFIX ), cfg.getProperties(), null
		);

		cfg = createConfiguration();
		regionFactory = CacheTestUtil.startRegionFactory(
				new ServiceRegistryBuilder().applySettings( cfg.getProperties() ).buildServiceRegistry(),
				cfg,
				getCacheTestSupport()
		);

		final GeneralDataRegion remoteRegion = (GeneralDataRegion) createRegion(
				regionFactory,
				getStandardRegionName( REGION_PREFIX ),
				cfg.getProperties(),
				null
		);

		assertNull( "local is clean", localRegion.get( KEY ) );
		assertNull( "remote is clean", remoteRegion.get( KEY ) );

		regionPut(localRegion, KEY, VALUE1);

		Callable<Object> getFromLocalRegion = new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return regionGet(localRegion, KEY);
			}
		};
		Callable<Object> getFromRemoteRegion = new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return regionGet(remoteRegion, KEY);
			}
		};

		assertEqualsEventually(VALUE1, getFromLocalRegion, 10, TimeUnit.SECONDS);
		assertEqualsEventually(VALUE1, getFromRemoteRegion, 10, TimeUnit.SECONDS);

		regionEvict(localRegion, KEY);

		assertEqualsEventually(null, getFromLocalRegion, 10, TimeUnit.SECONDS);
		assertEqualsEventually(null, getFromRemoteRegion, 10, TimeUnit.SECONDS);
	}

	protected void regionEvict(GeneralDataRegion region, String key) throws Exception {
		region.evict(key);
	}

	protected void regionPut(GeneralDataRegion region, String key, String value) throws Exception {
		region.put(key, value);
	}

	protected Object regionGet(GeneralDataRegion region, String key) throws Exception {
		return region.get(key);
	}

	protected abstract String getStandardRegionName(String regionPrefix);

	/**
	 * Test method for {@link QueryResultsRegion#evictAll()}.
	 * <p/>
	 * FIXME add testing of the "immediately without regard for transaction isolation" bit in the
	 * CollectionRegionAccessStrategy API.
	 */
	public void testEvictAll() throws Exception {
		evictOrRemoveAllTest( "entity" );
	}

	private void evictOrRemoveAllTest(String configName) throws Exception {
		Configuration cfg = createConfiguration();
		InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
				new ServiceRegistryBuilder().applySettings( cfg.getProperties() ).buildServiceRegistry(),
				cfg,
				getCacheTestSupport()
		);
		AdvancedCache localCache = getInfinispanCache( regionFactory );

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		GeneralDataRegion localRegion = (GeneralDataRegion) createRegion(
				regionFactory,
				getStandardRegionName( REGION_PREFIX ),
				cfg.getProperties(),
				null
		);

		cfg = createConfiguration();
		regionFactory = CacheTestUtil.startRegionFactory(
				new ServiceRegistryBuilder().applySettings( cfg.getProperties() ).buildServiceRegistry(),
				cfg,
				getCacheTestSupport()
		);
		AdvancedCache remoteCache = getInfinispanCache( regionFactory );

		// Sleep a bit to avoid concurrent FLUSH problem
		avoidConcurrentFlush();

		GeneralDataRegion remoteRegion = (GeneralDataRegion) createRegion(
				regionFactory,
				getStandardRegionName( REGION_PREFIX ),
				cfg.getProperties(),
				null
		);

		Set keys = localCache.keySet();
		assertEquals( "No valid children in " + keys, 0, getValidKeyCount( keys ) );

		keys = remoteCache.keySet();
		assertEquals( "No valid children in " + keys, 0, getValidKeyCount( keys ) );

		assertNull( "local is clean", localRegion.get( KEY ) );
		assertNull( "remote is clean", remoteRegion.get( KEY ) );

		regionPut(localRegion, KEY, VALUE1);
		assertEquals( VALUE1, localRegion.get( KEY ) );

		// Allow async propagation
		sleep( 250 );

		regionPut(remoteRegion, KEY, VALUE1);
		assertEquals( VALUE1, remoteRegion.get( KEY ) );

		// Allow async propagation
		sleep( 250 );

		localRegion.evictAll();

		// allow async propagation
		sleep( 250 );
		// This should re-establish the region root node in the optimistic case
		assertNull( localRegion.get( KEY ) );
		assertEquals( "No valid children in " + keys, 0, getValidKeyCount( localCache.keySet() ) );

		// Re-establishing the region root on the local node doesn't
		// propagate it to other nodes. Do a get on the remote node to re-establish
		// This only adds a node in the case of optimistic locking
		assertEquals( null, remoteRegion.get( KEY ) );
		assertEquals( "No valid children in " + keys, 0, getValidKeyCount( remoteCache.keySet() ) );

		assertEquals( "local is clean", null, localRegion.get( KEY ) );
		assertEquals( "remote is clean", null, remoteRegion.get( KEY ) );
	}
}