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
import org.hibernate.cache.ehcache.internal.EhCacheMessageLogger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceException;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MissingCacheStrategyTest extends BaseUnitTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( EhCacheMessageLogger.INSTANCE );

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
			for ( String regionName : TestHelper.allRegionNames ) {
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
		for ( String regionName : TestHelper.allRegionNames ) {
			triggerables.put(
					regionName,
					logInspection.watchForLogMessages(
							"HHH020009: Missing cache[" + TestHelper.prefix( regionName ) + "] was created on-the-fly"
					)
			);
		}

		try ( SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory( additionalSettings ) ) {
			for ( String regionName : TestHelper.allRegionNames ) {
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

}
