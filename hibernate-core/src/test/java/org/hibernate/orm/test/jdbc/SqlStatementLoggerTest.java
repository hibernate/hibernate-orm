/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc;

import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/// @author Steve Ebersole
class SqlStatementLoggerTest {
	@Test
	void enablingSlowQueryLoggingAfterExecutionStarted() {
		final var logger = new SqlStatementLogger();
		final long startTimeNanos = logger.getLogSlowQuery() > 0 ? System.nanoTime() : 0;
		logger.setLogSlowQuery( 1 );

		assertDoesNotThrow( () -> logger.logSlowQuery( "select 1", startTimeNanos, null ) );
	}
}
