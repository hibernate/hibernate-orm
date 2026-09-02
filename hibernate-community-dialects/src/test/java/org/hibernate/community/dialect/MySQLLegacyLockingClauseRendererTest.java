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
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests legacy MySQL and MariaDB locking-clause rendering.
///
/// @author Steve Ebersole
class MySQLLegacyLockingClauseRendererTest {
	@Test
	void mysql5UsesShareModeWithoutTimeoutSuffix() {
		assertDialectRenders(
				new MySQLLegacyDialect( DatabaseVersion.make( 5, 7 ) ),
				LockMode.PESSIMISTIC_READ,
				Timeouts.NO_WAIT,
				" lock in share mode"
		);
	}

	@Test
	void mysql8UsesForShareAndSupportsSkipLocked() {
		assertDialectRenders(
				new MySQLLegacyDialect( DatabaseVersion.make( 8 ) ),
				LockMode.PESSIMISTIC_READ,
				Timeouts.SKIP_LOCKED,
				" for share skip locked"
		);
	}

	@Test
	void mariaDbVersionGatesSkipLockedButRetainsNoWait() {
		assertDialectRenders(
				new MariaDBLegacyDialect( DatabaseVersion.make( 10, 5 ) ),
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.NO_WAIT,
				" for update nowait"
		);
		assertDialectRenders(
				new MariaDBLegacyDialect( DatabaseVersion.make( 10, 5 ) ),
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.SKIP_LOCKED,
				" for update"
		);
		assertDialectRenders(
				new MariaDBLegacyDialect( DatabaseVersion.make( 10, 6 ) ),
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.SKIP_LOCKED,
				" for update skip locked"
		);
	}

	private static void assertDialectRenders(
			Dialect dialect,
			LockMode lockMode,
			Timeout timeout,
			String expected) {
		final var strategy = dialect.getLockingClauseStrategy(
				new QuerySpec( true ),
				new LockOptions( lockMode, timeout )
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( expected, appender.toString() );
	}
}
