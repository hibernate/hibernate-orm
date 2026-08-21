/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.logger;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.logger.LogLevelContext.withLevel;

class DelegatingLoggerLevelTest {

	static Logger SOME_LOGGER = Logger.getLogger( DelegatingLoggerLevelTest.class );

	String STRING_LOGGER_NAME = "logger.name.as.string";
	Logger SOME_OTHER_LOGGER = Logger.getLogger( STRING_LOGGER_NAME );

	@Test
	void classStaticLogger() {
		assertThat( SOME_LOGGER.isEnabled( Logger.Level.DEBUG ) ).isFalse();
		assertThat( SOME_LOGGER.isEnabled( Logger.Level.FATAL ) ).isTrue();

		try (var ignored = withLevel( DelegatingLoggerLevelTest.class, Logger.Level.DEBUG )) {
			assertThat( SOME_LOGGER.isEnabled( Logger.Level.DEBUG ) ).isTrue();
			assertThat( SOME_LOGGER.isEnabled( Logger.Level.FATAL ) ).isTrue();
		}

		assertThat( SOME_LOGGER.isEnabled( Logger.Level.DEBUG ) ).isFalse();
		assertThat( SOME_LOGGER.isEnabled( Logger.Level.FATAL ) ).isTrue();
	}

	@Test
	void stringLogger() {
		assertThat( SOME_OTHER_LOGGER.isEnabled( Logger.Level.DEBUG ) ).isFalse();
		assertThat( SOME_OTHER_LOGGER.isEnabled( Logger.Level.INFO ) ).isFalse();
		assertThat( SOME_OTHER_LOGGER.isEnabled( Logger.Level.FATAL ) ).isTrue();

		try (var ignored = withLevel( STRING_LOGGER_NAME, Logger.Level.INFO )) {
			assertThat( SOME_OTHER_LOGGER.isEnabled( Logger.Level.DEBUG ) ).isFalse();
			assertThat( SOME_OTHER_LOGGER.isEnabled( Logger.Level.INFO ) ).isTrue();
			assertThat( SOME_OTHER_LOGGER.isEnabled( Logger.Level.FATAL ) ).isTrue();
		}

		assertThat( SOME_OTHER_LOGGER.isEnabled( Logger.Level.DEBUG ) ).isFalse();
		assertThat( SOME_OTHER_LOGGER.isEnabled( Logger.Level.INFO ) ).isFalse();
		assertThat( SOME_OTHER_LOGGER.isEnabled( Logger.Level.FATAL ) ).isTrue();
	}
}
