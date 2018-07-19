/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.ConfigSettings;
import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceException;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MissingCacheStrategyTest extends BaseUnitTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( SecondLevelCacheLogger.INSTANCE );

	@Test
	public void testMissingCacheStrategyDefault() {
		doTestMissingCacheStrategyCreateWarn(
				ignored -> { } // default settings
		);
	}

	@Test
	public void testMissingCacheStrategyFail() {
		/*
		 * The cache manager is created per session factory, and we don't use any specific ehcache configuration,
		 * so we know the caches don't exist before we start the session factory.
		 */

		// let's try to build the standard testing SessionFactory, without pre-defining caches
		try ( SessionFactoryImplementor ignored = TestHelper.buildStandardSessionFactory(
				builder -> builder.applySetting( ConfigSettings.MISSING_CACHE_STRATEGY, "fail" )
		) ) {
			fail();
		}
		catch (ServiceException expected) {
			assertTyping( CacheException.class, expected.getCause() );
			assertThat( expected.getMessage(), CoreMatchers.equalTo( "Unable to create requested service [" + org.hibernate.cache.spi.CacheImplementor.class.getName() + "]" ) );
			assertThat( expected.getCause().getMessage(), CoreMatchers.startsWith( "On-the-fly creation of Ehcache Cache objects is not supported" ) );
		}
		catch (CacheException expected) {
			assertThat( expected.getMessage(), CoreMatchers.equalTo( "On-the-fly creation of Ehcache Cache objects is not supported" ) );
		}
	}

	@Test
	public void testMissingCacheStrategyFail_legacyNames() {
		/*
		 * The cache manager is created per session factory, and we don't use any specific ehcache configuration,
		 * so we know the caches don't exist before we start the session factory.
		 */

		// let's try to build the standard testing SessionFactory, without pre-defining caches
		try ( SessionFactoryImplementor ignored = TestHelper.buildStandardSessionFactory(
				builder -> builder.applySetting( ConfigSettings.MISSING_CACHE_STRATEGY, "fail" )
		) ) {
			fail();
		}
		catch (ServiceException expected) {
			assertTyping( CacheException.class, expected.getCause() );
			assertThat( expected.getMessage(), CoreMatchers.equalTo( "Unable to create requested service [" + org.hibernate.cache.spi.CacheImplementor.class.getName() + "]" ) );
			assertThat( expected.getCause().getMessage(), CoreMatchers.startsWith( "On-the-fly creation of Ehcache Cache objects is not supported" ) );
		}
		catch (CacheException expected) {
			assertThat( expected.getMessage(), CoreMatchers.equalTo( "On-the-fly creation of Ehcache Cache objects is not supported" ) );
		}
	}

	@Test
	public void testMissingCacheStrategyCreate() {
		/*
		 * The cache manager is created per session factory, and we don't use any specific ehcache configuration,
		 * so we know the caches don't exist before we start the session factory.
		 */

		// and now let's try to build the standard testing SessionFactory, without pre-defining caches
		try ( SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory(
				builder -> builder.applySetting( ConfigSettings.MISSING_CACHE_STRATEGY, "create" )
		) ) {
			// The caches should have been created automatically
			for ( String regionName : TestHelper.allDomainRegionNames ) {
				assertThat( "Cache '" + regionName + "' should have been created",
						TestHelper.getCache( sessionFactory, regionName ), notNullValue() );
			}
		}
	}

	@Test
	public void testMissingCacheStrategyCreateWarn() {
		doTestMissingCacheStrategyCreateWarn(
				builder -> builder.applySetting( ConfigSettings.MISSING_CACHE_STRATEGY, "create-warn" )
		);
	}

	private void doTestMissingCacheStrategyCreateWarn(Consumer<StandardServiceRegistryBuilder> additionalSettings) {
		/*
		 * The cache manager is created per session factory, and we don't use any specific ehcache configuration,
		 * so we know the caches don't exist before we start the session factory.
		 */

		Map<String, Triggerable> triggerables = new HashMap<>();
		for ( String regionName : TestHelper.allDomainRegionNames ) {
			triggerables.put(
					regionName,
					logInspection.watchForLogMessages(
							"HHH90001006: Missing cache[" + TestHelper.prefix( regionName ) + "] was created on-the-fly"
					)
			);
		}

		try ( SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory( additionalSettings ) ) {
			for ( String regionName : TestHelper.allDomainRegionNames ) {
				// The caches should have been created automatically
				assertThat(
						"Cache '" + regionName + "' should have been created",
						TestHelper.getCache( sessionFactory, regionName ), notNullValue()
				);
				// Logs should have been triggered
				assertTrue(
						"Cache '" + regionName + "' should have triggered a warning",
						triggerables.get( regionName ).wasTriggered()
				);
			}
		}
	}

	@Test
	public void testMissingCacheStrategyFailLegacyNames1() {
		doTestMissingCacheStrategyFailLegacyNames(
				"/hibernate-config/ehcache-explicitlegacy1.xml",
				TestHelper.queryRegionLegacyNames1, TestHelper.queryRegionLegacyNames2
		);
	}

	@Test
	public void testMissingCacheStrategyFailLegacyNames2() {
		doTestMissingCacheStrategyFailLegacyNames(
				"/hibernate-config/ehcache-explicitlegacy2.xml",
				TestHelper.queryRegionLegacyNames2, TestHelper.queryRegionLegacyNames1
		);
	}

	private void doTestMissingCacheStrategyFailLegacyNames(String configurationPath,
			String[] existingLegacyCaches, String[] nonExistingLegacyCaches) {
		Map<String, Triggerable> triggerables = new HashMap<>();

		// This is used later for log-related assertions
		for ( int i = 0; i < TestHelper.queryRegionNames.length; ++i ) {
			String currentName = TestHelper.queryRegionNames[i];
			String legacyName = existingLegacyCaches[i];

			triggerables.put(
					legacyName,
					logInspection.watchForLogMessages(
							"HHH90001007: Using legacy cache name [" + legacyName +
									"] because configuration could not be found for cache [" + currentName + "]."
					)
			);
		}

		// and now let's try to build the standard testing SessionFactory
		try ( SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory(
				builder -> builder.applySetting( ConfigSettings.MISSING_CACHE_STRATEGY, "fail" )
						.applySetting( ConfigSettings.EHCACHE_CONFIGURATION_RESOURCE_NAME, configurationPath )
		) ) {
			// The session should start successfully (if we reach this line, we're good)

			// Logs should have been to notify that legacy cache names are being used
			for ( String regionName : existingLegacyCaches ) {
				assertTrue(
						"Use of cache '" + regionName + "' should have triggered a warning",
						triggerables.get( regionName ).wasTriggered()
				);
			}

			// and these caches shouldn't exist
			for ( String regionName : nonExistingLegacyCaches ) {
				assertThat( TestHelper.getCache( sessionFactory, regionName ), nullValue() );
			}
			for ( String regionName : TestHelper.queryRegionNames ) {
				assertThat( TestHelper.getCache( sessionFactory, regionName ), nullValue() );
			}
		}
	}

}
