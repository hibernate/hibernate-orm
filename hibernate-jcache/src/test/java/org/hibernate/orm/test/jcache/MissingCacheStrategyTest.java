/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.LoggingInspections;
import org.hibernate.testing.orm.junit.LoggingInspectionsScope;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests around {@link org.hibernate.cache.jcache.MissingCacheStrategy}
 *
 * @author Steve Ebersole
 * @author Yoann Rodiere
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@LoggingInspections(messages = {
		@LoggingInspections.Message( messageKey = "HHH90001006",
				loggers = @Logger( loggerName = SecondLevelCacheLogger.LOGGER_NAME )
		),
		@LoggingInspections.Message( messageKey = "HHH90001007",
				loggers = @Logger( loggerName = SecondLevelCacheLogger.LOGGER_NAME )
		)
})
public class MissingCacheStrategyTest {
	@BeforeEach
	public void preCheckCaches() {
		// make sure that the region names we think are non-existent really do not exist
		for ( String regionName : TestHelper.allDomainRegionNames ) {
			assertThat( TestHelper.getCache( regionName ) ).isNull();
		}
	}

	@Test
	void testMissingCacheStrategyDefault(LoggingInspectionsScope loggingInspectionsScope) {
		checkCreateWarnStrategy( loggingInspectionsScope, "" );
	}

	@Test
	void testMissingCacheStrategyCreateWarn(LoggingInspectionsScope loggingInspectionsScope) {
		checkCreateWarnStrategy( loggingInspectionsScope, "create-warn" );
	}

	/**
	 * Centralized checks for the create-warn and default (which is create-warn) strategies.
	 * Makes sure we get WARN logging.
	 */
	private void checkCreateWarnStrategy(
			LoggingInspectionsScope loggingInspectionsScope,
			String strategyName) {
		assertThat( loggingInspectionsScope ).isNotNull();
		final MessageKeyWatcher messageKeyWatcher = loggingInspectionsScope.getWatcher(
				"HHH90001006",
				SecondLevelCacheLogger.LOGGER_NAME
		);
		messageKeyWatcher.reset();

		try ( SessionFactoryImplementor ignored = TestHelper.buildSessionFactoryWithMissingCacheStrategy( strategyName ) ) {
			assertThat( messageKeyWatcher.wasTriggered() ).isTrue();

			for ( String regionName : TestHelper.allDomainRegionNames ) {
				// The cache should have been created automatically
				assertThat( TestHelper.getCache( regionName ) )
						.as( () -> String.format( "Cache %s not found", regionName ) )
						.isNotNull();

				// and the message should have been logged
				checkForRegionCreationWarning( messageKeyWatcher, regionName );
			}
		}
	}

	private void checkForRegionCreationWarning(MessageKeyWatcher messageKeyWatcher, String regionName) {
		for ( int i = 0; i < messageKeyWatcher.getTriggeredMessages().size(); i++ ) {
			final String message = messageKeyWatcher.getTriggeredMessages().get( i );
			if ( message.contains( "[hibernate.test." + regionName + "]" ) ) {
				return;
			}
		}

		fail( "No warning for auto creation of region " + regionName );
	}

	@Test
	public void testMissingCacheStrategyFail() {
		try ( SessionFactoryImplementor ignored = TestHelper.buildSessionFactoryWithMissingCacheStrategy( "fail" ) ) {
			fail( "Excepting failure due to missing cache strategy = fail" );
		}
		catch (ServiceException expected) {
			assertThat( expected.getCause() ).isInstanceOf( CacheException.class );
			assertThat( expected.getMessage() ).startsWith( "Unable to create requested service [" + org.hibernate.cache.spi.CacheImplementor.class.getName() + "]" );
			assertThat( expected.getCause().getMessage() ).startsWith( "On-the-fly creation of JCache Cache objects is not supported [" );
		}
		catch (CacheException expected) {
			assertThat( expected.getMessage() ).startsWith( "On-the-fly creation of JCache Cache objects is not supported [" );
		}
	}

	@Test
	public void testMissingCacheStrategyCreate() {
		try ( SessionFactoryImplementor ignored = TestHelper.buildSessionFactoryWithMissingCacheStrategy( "create" ) ) {
			// The caches should have been created automatically
			for ( String regionName : TestHelper.allDomainRegionNames ) {
				assertThat( TestHelper.getCache( regionName ) )
						.as( () -> String.format( "Cache %s should have been created", regionName ) )
						.isNotNull();
			}
		}
	}

}
