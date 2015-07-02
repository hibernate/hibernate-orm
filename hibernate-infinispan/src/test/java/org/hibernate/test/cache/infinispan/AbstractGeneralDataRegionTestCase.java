/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.spi.GeneralDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.infinispan.AdvancedCache;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.jboss.logging.Logger;
import org.junit.Test;

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

	protected StandardServiceRegistryBuilder createStandardServiceRegistryBuilder() {
		return CacheTestUtil.buildBaselineStandardServiceRegistryBuilder(
				"test",
				InfinispanRegionFactory.class,
				false,
				true
		);
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
		final StandardServiceRegistryBuilder ssrb = createStandardServiceRegistryBuilder();
		StandardServiceRegistry registry1 = ssrb.build();
		StandardServiceRegistry registry2 = ssrb.build();
		try {
			InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
					registry1,
					getCacheTestSupport()
			);

			final Properties properties = CacheTestUtil.toProperties( ssrb.getSettings() );

			boolean invalidation = false;

			// Sleep a bit to avoid concurrent FLUSH problem
			avoidConcurrentFlush();

			final GeneralDataRegion localRegion = (GeneralDataRegion) createRegion(
					regionFactory,
					getStandardRegionName( REGION_PREFIX ),
					properties,
					null
			);

			regionFactory = CacheTestUtil.startRegionFactory(
					registry2,
					getCacheTestSupport()
			);

			final GeneralDataRegion remoteRegion = (GeneralDataRegion) createRegion(
					regionFactory,
					getStandardRegionName( REGION_PREFIX ),
					properties,
					null
			);
			assertNull( "local is clean", localRegion.get( KEY ) );
			assertNull( "remote is clean", remoteRegion.get( KEY ) );

			regionPut( localRegion );

			Callable<Object> getFromLocalRegion = new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					return localRegion.get(KEY);
				}
			};
			Callable<Object> getFromRemoteRegion = new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					return remoteRegion.get(KEY);
				}
			};

			assertEqualsEventually(VALUE1, getFromLocalRegion, 10, TimeUnit.SECONDS);
			Object expected = invalidation ? null : VALUE1;
			assertEqualsEventually(expected, getFromRemoteRegion, 10, TimeUnit.SECONDS);

			regionEvict(localRegion);

			assertEqualsEventually(null, getFromLocalRegion, 10, TimeUnit.SECONDS);
			assertEqualsEventually(null, getFromRemoteRegion, 10, TimeUnit.SECONDS);
		} finally {
			StandardServiceRegistryBuilder.destroy( registry1 );
			StandardServiceRegistryBuilder.destroy( registry2 );
		}
	}

   protected void regionEvict(GeneralDataRegion region) throws Exception {
	  region.evict(KEY);
   }

   protected void regionPut(GeneralDataRegion region) throws Exception {
	  region.put(KEY, VALUE1);
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
		final StandardServiceRegistryBuilder ssrb = createStandardServiceRegistryBuilder();
		StandardServiceRegistry registry1 = ssrb.build();
		StandardServiceRegistry registry2 = ssrb.build();

		try {
			final Properties properties = CacheTestUtil.toProperties( ssrb.getSettings() );

			InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
					registry1,
					getCacheTestSupport()
			);
			AdvancedCache localCache = getInfinispanCache( regionFactory );

			// Sleep a bit to avoid concurrent FLUSH problem
			avoidConcurrentFlush();

			GeneralDataRegion localRegion = (GeneralDataRegion) createRegion(
					regionFactory,
					getStandardRegionName( REGION_PREFIX ),
					properties,
					null
			);

			regionFactory = CacheTestUtil.startRegionFactory(
					registry2,
					getCacheTestSupport()
			);
			AdvancedCache remoteCache = getInfinispanCache( regionFactory );

			// Sleep a bit to avoid concurrent FLUSH problem
			avoidConcurrentFlush();

			GeneralDataRegion remoteRegion = (GeneralDataRegion) createRegion(
					regionFactory,
					getStandardRegionName( REGION_PREFIX ),
					properties,
					null
			);

			Set keys = localCache.keySet();
			assertEquals( "No valid children in " + keys, 0, getValidKeyCount( keys ) );

			keys = remoteCache.keySet();
			assertEquals( "No valid children in " + keys, 0, getValidKeyCount( keys ) );

			assertNull( "local is clean", localRegion.get( KEY ) );
			assertNull( "remote is clean", remoteRegion.get( KEY ) );

			regionPut(localRegion);
			assertEquals( VALUE1, localRegion.get( KEY ) );

			// Allow async propagation
			sleep( 250 );

			regionPut(remoteRegion);
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
		finally {
			StandardServiceRegistryBuilder.destroy( registry1 );
			StandardServiceRegistryBuilder.destroy( registry2 );
		}
	}

	protected void rollback() {
		try {
			BatchModeTransactionManager.getInstance().rollback();
		}
		catch (Exception e) {
			log.error( e.getMessage(), e );
		}
	}
}