/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests to verify that even when dealing with Enhanced Proxies, we will still attempt
 * to load them from 2LC in preference of loading from the DB.
 *
 * @author Sanne Grinovero
 */
@JiraKey( "HHH-14004" )
@DomainModel(
		annotatedClasses = {
				Country.class, Continent.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class EnhancedProxyCacheTest {

	private static final AtomicLong countryId = new AtomicLong();

	@Test
	public void testPreferenceFor2LCOverUninitializedProxy(SessionFactoryScope scope) throws Exception {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		storeTestData( scope );
		clearAllCaches( scope );
		stats.clear();
		assertTrue( stats.isStatisticsEnabled() );
		assertEquals( 0, stats.getEntityFetchCount() );
		assertEquals( 0, stats.getSecondLevelCacheHitCount() );

		// First we load the Country once, then trigger initialization of the related Continent proxy.
		// 2LC is empty, so stats should show that these objects are being loaded from the DB.
		scope.inSession( s -> {
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
		scope.inSession( s -> {

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

	private void clearAllCaches(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();
		for (String name : cache.getCacheRegionNames() ) {
			cache.getRegion( name ).clear();
		}
	}

	private void storeTestData(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
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
