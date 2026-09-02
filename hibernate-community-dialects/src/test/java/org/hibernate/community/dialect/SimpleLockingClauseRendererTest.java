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

/// Tests community Dialects using shared simple locking metadata.
///
/// @author Steve Ebersole
class SimpleLockingClauseRendererTest {
	@Test
	void plainClauseFamiliesUseTheSharedRenderer() {
		assertDialectRenders( new CUBRIDDialect(), LockMode.PESSIMISTIC_READ, Timeouts.NO_WAIT, " for update" );
		assertDialectRenders( new MaxDBDialect(), LockMode.PESSIMISTIC_WRITE, Timeouts.NO_WAIT, " for update" );
		assertDialectRenders( new MimerSQLDialect(), LockMode.PESSIMISTIC_WRITE, Timeouts.NO_WAIT, " for update" );
	}

	@Test
	void derbySeparatesTheLockClauseFromResultSetIsolation() {
		assertDialectRenders(
				new DerbyDialect(),
				LockMode.PESSIMISTIC_READ,
				Timeouts.WAIT_FOREVER,
				" for read only with rs"
		);
		assertDialectRenders(
				new DerbyDialect(),
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.WAIT_FOREVER,
				" for update with rs"
		);
		assertDialectRenders(
				new DerbyLegacyDialect(),
				LockMode.PESSIMISTIC_READ,
				Timeouts.WAIT_FOREVER,
				" for read only with rs"
		);
	}

	@Test
	void firebirdGatesSkipLockedAtVersionFive() {
		assertDialectRenders(
				new FirebirdDialect( DatabaseVersion.make( 4 ) ),
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.SKIP_LOCKED,
				" with lock"
		);
		assertDialectRenders(
				new FirebirdDialect( DatabaseVersion.make( 5 ) ),
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.SKIP_LOCKED,
				" with lock skip locked"
		);
	}

	@Test
	void rdmsIsExplicitlyNonLocking() {
		assertDialectRenders(
				new RDMSOS2200Dialect(),
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.WAIT_FOREVER,
				""
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
