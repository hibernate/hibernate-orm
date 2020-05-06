/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests to verify that even when dealing with Enhanced Proxies, we will still attempt
 * to load them from 2LC in preference of loading from the DB.
 *
 * @author Sanne Grinovero
 */
@TestForIssue( jiraKey = "HHH-14004" )
@RunWith(BytecodeEnhancerRunner.class)
public class EnhancedProxyCacheTest extends BaseCoreFunctionalTestCase {

	private static final AtomicLong countryId = new AtomicLong();

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Country.class, Continent.class };
	}

	@Test
	public void testPreferenceFor2LCOverUninitializedProxy() throws Exception {
		final Statistics stats = sessionFactory().getStatistics();
		storeTestData();
		clearAllCaches();
		stats.clear();
		assertTrue( stats.isStatisticsEnabled() );
		assertEquals( 0, stats.getEntityFetchCount() );
		assertEquals( 0, stats.getSecondLevelCacheHitCount() );

		// First we load the Country once, then trigger initialization of the related Continent proxy.
		// 2LC is empty, so stats should show that these objects are being loaded from the DB.
		inSession( s -> {
			Country nl = s.get( Country.class, countryId.get() );
			assertNotNull( nl );

			assertEquals( 0, stats.getSecondLevelCacheHitCount() );
			assertEquals( 1, stats.getSecondLevelCacheMissCount() );
			assertEquals( 1, stats.getEntityLoadCount() );

			Continent continent = nl.getContinent();

			//Check that this is indeed an enhanced proxy so to ensure we're testing in the right conditions.
			//The following casts should not fail:
			final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) continent;
			final EnhancementAsProxyLazinessInterceptor interceptor = (EnhancementAsProxyLazinessInterceptor) interceptable.$$_hibernate_getInterceptor();

			assertFalse( interceptor.isInitialized() );
			assertFalse( interceptor.isAttributeLoaded( "code" ) );

			//Trigger initialization of the enhanced proxy:
			assertEquals( "EU", continent.getCode() );

			assertTrue( interceptor.isInitialized() );
			assertEquals( 0, stats.getSecondLevelCacheHitCount() );
			assertEquals( 2, stats.getEntityLoadCount() );

		} );

		stats.clear();

		//Now load the same objects again; we expect to hit 2LC this time,
		//and we should see no needs to hit the DB.
		//Also, since all data is readily available we won't need to make
		//all attributes lazy.
		inSession( s -> {

			assertEquals( 0, stats.getSecondLevelCacheHitCount() );
			assertEquals( 0, stats.getSecondLevelCacheMissCount() );
			assertEquals( 0, stats.getEntityLoadCount() );

			Country nl = s.get( Country.class, countryId.get() );
			assertNotNull( nl );

			assertEquals( 1, stats.getSecondLevelCacheHitCount() );
			assertEquals( 0, stats.getSecondLevelCacheMissCount() );
			assertEquals( 0, stats.getEntityLoadCount() );

			Continent continent = nl.getContinent();

			final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) continent;
			final EnhancementAsProxyLazinessInterceptor interceptor = (EnhancementAsProxyLazinessInterceptor) interceptable.$$_hibernate_getInterceptor();

			assertFalse( interceptor.isInitialized() );
			assertFalse( interceptor.isAttributeLoaded( "code" ) );

			assertEquals( 1, stats.getSecondLevelCacheHitCount() );
			assertEquals( 0, stats.getSecondLevelCacheMissCount() );
			assertEquals( 0, stats.getEntityLoadCount() );

			//Trigger initialization of the enhanced proxy:
			assertEquals( "EU", continent.getCode() );

			assertTrue( interceptor.isInitialized() );
			assertEquals( 2, stats.getSecondLevelCacheHitCount() );
			assertEquals( 0, stats.getSecondLevelCacheMissCount() );
			assertEquals( 0, stats.getEntityLoadCount() );

		} );

	}

	private void clearAllCaches() {
		final CacheImplementor cache = sessionFactory().getCache();
		for (String name : cache.getCacheRegionNames() ) {
			cache.getRegion( name ).clear();
		}
	}

	private void storeTestData() {
		inTransaction( s -> {
			Continent continent = new Continent();
			continent.setCode( "EU" );
			continent.setName( "Europe" );
			s.persist( continent );
			Country c = new Country();
			c.setCode( "NL" );
			c.setName( "Nederland" );
			c.setContinent( continent );
			s.persist( c );
			countryId.set( c.getId() );
		} );
	}

}
