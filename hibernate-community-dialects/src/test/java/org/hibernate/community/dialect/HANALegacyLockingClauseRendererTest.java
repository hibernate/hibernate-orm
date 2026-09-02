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
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests version-sensitive locking-clause rendering for legacy HANA.
///
/// @author Steve Ebersole
class HANALegacyLockingClauseRendererTest {
	@Test
	void hana1OmitsUnsupportedTimeoutSyntax() {
		assertStrategyRenders( DatabaseVersion.make( 1, 0, 120 ), Timeouts.NO_WAIT, " for update" );
		assertStrategyRenders( DatabaseVersion.make( 1, 0, 120 ), Timeout.seconds( 1 ), " for update" );
	}

	@Test
	void hana2010SupportsNoWaitAndFiniteWait() {
		assertStrategyRenders( DatabaseVersion.make( 2, 0, 10 ), Timeouts.NO_WAIT, " for update nowait" );
		assertStrategyRenders( DatabaseVersion.make( 2, 0, 10 ), Timeout.seconds( 2 ), " for update wait 2" );
		assertStrategyRenders( DatabaseVersion.make( 2, 0, 10 ), Timeouts.SKIP_LOCKED, " for update" );
	}

	@Test
	void hana2030SupportsIgnoreLocked() {
		assertStrategyRenders(
				DatabaseVersion.make( 2, 0, 30 ),
				Timeouts.SKIP_LOCKED,
				" for update ignore locked"
		);
	}

	private static void assertStrategyRenders(DatabaseVersion version, Timeout timeout, String expected) {
		final var strategy = new HANALegacyDialect( version ).getLockingClauseStrategy(
				new QuerySpec( true ),
				new LockOptions( LockMode.PESSIMISTIC_WRITE, timeout )
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( expected, appender.toString() );
	}
}
