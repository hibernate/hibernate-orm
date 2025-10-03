/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache;

import org.hibernate.cache.spi.SecondLevelCacheLogger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hibernate.orm.test.jcache.TestHelper.queryRegionLegacyNames1;
import static org.hibernate.orm.test.jcache.TestHelper.queryRegionLegacyNames2;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@MessageKeyInspection( messageKey = "HHH90001007",
		logger = @Logger( loggerName = SecondLevelCacheLogger.LOGGER_NAME ) )
public class LegacyRegionNamingTests {
	@Test
	public void testMissingCacheStrategyFailLegacyNames1(MessageKeyWatcher watcher) {
		checkLegacyNameHandling( watcher, queryRegionLegacyNames1, queryRegionLegacyNames2 );
	}

	@Test
	public void testMissingCacheStrategyFailLegacyNames2(MessageKeyWatcher watcher) {
		checkLegacyNameHandling( watcher, queryRegionLegacyNames2, queryRegionLegacyNames1 );
	}

	private void checkLegacyNameHandling(
			MessageKeyWatcher watcher,
			String[] existingLegacyCaches,
			String[] nonExistingLegacyCaches) {
		watcher.reset();

		// make sure that the regions used for model caches exist
		TestHelper.preBuildDomainCaches();

		// and that caches exist with legacy configurations
		for ( int i = 0; i < TestHelper.queryRegionNames.length; ++i ) {
			String legacyName = existingLegacyCaches[i];
			TestHelper.createCache( legacyName );
		}

		// and then lets make sure that the region names we think
		// are non-existent really do not exist
		verifyNonExistence( nonExistingLegacyCaches, TestHelper.queryRegionNames );

		// and now let's try to build the standard testing SessionFactory
		try ( SessionFactoryImplementor ignored = TestHelper.buildSessionFactoryWithMissingCacheStrategy( "fail" ) ) {
			// The session-factory should start successfully : if we reach this line, we're good

			// Logs should have been to notify that legacy cache names are being used
			for ( String regionName : existingLegacyCaches ) {
				verifyWarningForRegion( regionName, watcher );
			}

			// and these caches still shouldn't exist
			verifyNonExistence( nonExistingLegacyCaches, TestHelper.queryRegionNames );
		}
	}

	private void verifyWarningForRegion(String regionName, MessageKeyWatcher watcher) {
		for ( String triggeredMessage : watcher.getTriggeredMessages() ) {
			if ( triggeredMessage.contains( "[" + regionName + "]" ) ) {
				return;
			}
		}
		fail( "Warning about legacy region name was not triggered for - %s", regionName );
	}

	private static void verifyNonExistence(String[]... regionNameGroups) {
		for ( String[] regionNameGroup : regionNameGroups ) {
			for ( String regionName : regionNameGroup ) {
				assertThat( TestHelper.getCache( regionName ) ).isNull();
			}
		}
	}
}
