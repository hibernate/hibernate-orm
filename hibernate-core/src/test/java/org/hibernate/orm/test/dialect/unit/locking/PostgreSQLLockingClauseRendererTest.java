/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locking;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.lock.internal.PostgreSQLLockingSupport;
import org.hibernate.dialect.lock.spi.LockingClauseRenderer;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests the PostgreSQL-family locking-clause renderer.
///
/// @author Steve Ebersole
class PostgreSQLLockingClauseRendererTest {
	private final LockingClauseRenderer renderer = new PostgreSQLLockingSupport( true, true )
			.getLockingClauseRenderer();

	@Test
	void rendersUpdateTargetsAndNoWait() {
		assertEquals(
				" for no key update of p,o nowait",
				renderer.render( new LockingClauseRequest(
						PessimisticLockKind.UPDATE,
						Timeouts.NO_WAIT,
						List.of(
								new LockingClauseRequest.TableTarget( "p" ),
								new LockingClauseRequest.TableTarget( "o" )
						)
				) )
		);
	}

	@Test
	void rendersShareTargetAndSkipLocked() {
		assertEquals(
				" for share of p skip locked",
				renderer.render( new LockingClauseRequest(
						PessimisticLockKind.SHARE,
						Timeouts.SKIP_LOCKED,
						List.of( new LockingClauseRequest.TableTarget( "p" ) )
				) )
		);
	}

	@Test
	void omitsUnsupportedTimeoutSyntax() {
		final LockingClauseRenderer legacyRenderer = new PostgreSQLLockingSupport( false, false )
				.getLockingClauseRenderer();

		assertEquals(
				" for no key update",
				legacyRenderer.render( new LockingClauseRequest(
						PessimisticLockKind.UPDATE,
						Timeouts.NO_WAIT,
						List.of()
				) )
		);
	}

	@Test
	void dialectStrategyUsesSuppliedRenderer() {
		final LockingClauseStrategy strategy = new PostgreSQLDialect().getLockingClauseStrategy(
				new QuerySpec( true ),
				new LockOptions( LockMode.PESSIMISTIC_WRITE, Timeouts.NO_WAIT )
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( " for no key update nowait", appender.toString() );
	}
}
