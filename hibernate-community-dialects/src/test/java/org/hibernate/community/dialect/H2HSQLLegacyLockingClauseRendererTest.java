/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.Timeout;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests legacy H2 and HSQLDB locking-clause renderer profiles.
///
/// @author Steve Ebersole
class H2HSQLLegacyLockingClauseRendererTest {
	@Test
	void h2VersionGatesForUpdateOptions() {
		assertDialectRenders(
				new H2LegacyDialect( DatabaseVersion.make( 2, 2, 219 ) ),
				Timeouts.NO_WAIT,
				" for update"
		);
		assertDialectRenders(
				new H2LegacyDialect( DatabaseVersion.make( 2, 2, 220 ) ),
				Timeouts.NO_WAIT,
				" for update nowait"
		);
	}

	@Test
	void hsqldbVersionGatesTheLockingClause() {
		assertDialectRenders(
				new HSQLLegacyDialect( TestingDialectResolutionInfo.forDatabaseInfo( "HSQL Database Engine", 1, 8 ) ),
				Timeouts.WAIT_FOREVER,
				""
		);
		assertDialectRenders(
				new HSQLLegacyDialect( DatabaseVersion.make( 2 ) ),
				Timeouts.WAIT_FOREVER,
				" for update"
		);
	}

	private static void assertDialectRenders(Dialect dialect, Timeout timeout, String expected) {
		final var strategy = dialect.getLockingClauseStrategy(
				new QuerySpec( true ),
				new LockOptions( LockMode.PESSIMISTIC_WRITE, timeout )
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( expected, appender.toString() );
	}
}
