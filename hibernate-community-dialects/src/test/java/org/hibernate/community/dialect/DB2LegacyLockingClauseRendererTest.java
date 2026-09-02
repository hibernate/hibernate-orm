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

/// Tests legacy DB2-family locking-clause renderer profiles.
///
/// @author Steve Ebersole
class DB2LegacyLockingClauseRendererTest {
	@Test
	void luwVersionGatesSkipLockedData() {
		assertDialectRenders(
				new DB2LegacyDialect( DatabaseVersion.make( 11, 1 ) ),
				LockMode.PESSIMISTIC_READ,
				Timeouts.SKIP_LOCKED,
				" for read only with rs use and keep share locks"
		);
		assertDialectRenders(
				new DB2LegacyDialect( DatabaseVersion.make( 11, 5 ) ),
				LockMode.PESSIMISTIC_READ,
				Timeouts.SKIP_LOCKED,
				" for read only with rs use and keep share locks skip locked data"
		);
	}

	@Test
	void db2iPreservesHistoricalLegacyClauseForm() {
		assertDialectRenders(
				new DB2iLegacyDialect(),
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.SKIP_LOCKED,
				" for read only with rs use and keep update locks skip locked data"
		);
	}

	@Test
	void db2zPreservesInheritedClauseForm() {
		assertDialectRenders(
				new DB2zLegacyDialect(),
				LockMode.PESSIMISTIC_READ,
				Timeouts.SKIP_LOCKED,
				" for read only with rs use and keep share locks skip locked data"
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
