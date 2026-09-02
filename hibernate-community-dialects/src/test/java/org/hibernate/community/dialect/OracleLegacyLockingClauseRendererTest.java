/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests version-sensitive locking-clause rendering for legacy Oracle.
///
/// @author Steve Ebersole
class OracleLegacyLockingClauseRendererTest {
	@Test
	void oracle8OmitsUnsupportedNoWait() {
		assertStrategyRenders( DatabaseVersion.make( 8 ), Timeouts.NO_WAIT, " for update" );
	}

	@Test
	void oracle9SupportsNoWaitButNotSkipLocked() {
		assertStrategyRenders( DatabaseVersion.make( 9 ), Timeouts.NO_WAIT, " for update nowait" );
		assertStrategyRenders( DatabaseVersion.make( 9 ), Timeouts.SKIP_LOCKED, " for update" );
	}

	@Test
	void oracle10SupportsSkipLocked() {
		assertStrategyRenders( DatabaseVersion.make( 10 ), Timeouts.SKIP_LOCKED, " for update skip locked" );
	}

	private static void assertStrategyRenders(
			DatabaseVersion version,
			jakarta.persistence.Timeout timeout,
			String expected) {
		final var strategy = new OracleLegacyDialect( version ).getLockingClauseStrategy(
				new QuerySpec( true ),
				new LockOptions( LockMode.PESSIMISTIC_WRITE, timeout )
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( expected, appender.toString() );
	}
}
