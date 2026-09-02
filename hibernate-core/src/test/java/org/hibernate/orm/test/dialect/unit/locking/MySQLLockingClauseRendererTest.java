/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locking;

import java.util.List;

import jakarta.persistence.Timeout;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.lock.internal.MariaDBLockingSupport;
import org.hibernate.dialect.lock.internal.MySQLLockingSupport;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests the MySQL and MariaDB locking-clause renderers.
///
/// @author Steve Ebersole
class MySQLLockingClauseRendererTest {
	@Test
	void mysql8RendersShareTargetsAndSkipLocked() {
		assertEquals(
				" for share of p skip locked",
				new MySQLLockingSupport().render( new LockingClauseRequest(
						PessimisticLockKind.SHARE,
						Timeouts.SKIP_LOCKED,
						List.of( new LockingClauseRequest.TableTarget( "p" ) )
				) )
		);
	}

	@Test
	void legacyMysqlUsesShareModeAndOmitsUnsupportedOptions() {
		assertEquals(
				" lock in share mode",
				new MySQLLockingSupport( DatabaseVersion.make( 5, 7 ) ).render( new LockingClauseRequest(
						PessimisticLockKind.SHARE,
						Timeouts.SKIP_LOCKED,
						List.of( new LockingClauseRequest.TableTarget( "p" ) )
				) )
		);
	}

	@Test
	void mariaDbUsesShareModeWithoutTargets() {
		assertEquals(
				" lock in share mode skip locked",
				new MariaDBLockingSupport( DatabaseVersion.make( 10, 6 ) ).render( new LockingClauseRequest(
						PessimisticLockKind.SHARE,
						Timeouts.SKIP_LOCKED,
						List.of( new LockingClauseRequest.TableTarget( "p" ) )
				) )
		);
	}

	@Test
	void mariaDbOmitsFiniteWaitAndVersionGatesSkipLocked() {
		final var support = new MariaDBLockingSupport( DatabaseVersion.make( 10, 5 ) );
		assertEquals(
				" for update nowait",
				support.render( new LockingClauseRequest(
						PessimisticLockKind.UPDATE,
						Timeouts.NO_WAIT,
						List.of()
				) )
		);
		assertEquals(
				" for update",
				support.render( new LockingClauseRequest(
						PessimisticLockKind.UPDATE,
						Timeout.seconds( 1 ),
						List.of()
				) )
		);
		assertEquals(
				" for update",
				support.render( new LockingClauseRequest(
						PessimisticLockKind.UPDATE,
						Timeouts.SKIP_LOCKED,
						List.of()
				) )
		);
	}

	@Test
	void currentDialectStrategiesUseSuppliedRenderers() {
		assertDialectRenders(
				new MySQLDialect(),
				LockMode.PESSIMISTIC_READ,
				Timeouts.SKIP_LOCKED,
				" for share skip locked"
		);
		assertDialectRenders(
				new MariaDBDialect(),
				LockMode.PESSIMISTIC_READ,
				Timeouts.SKIP_LOCKED,
				" lock in share mode skip locked"
		);
	}

	private static void assertDialectRenders(
			org.hibernate.dialect.Dialect dialect,
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
