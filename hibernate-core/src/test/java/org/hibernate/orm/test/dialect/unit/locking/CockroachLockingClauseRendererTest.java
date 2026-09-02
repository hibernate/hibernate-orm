/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locking;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.lock.internal.CockroachLockingSupport;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests the CockroachDB-family locking-clause renderer.
///
/// @author Steve Ebersole
class CockroachLockingClauseRendererTest {
	@Test
	void rendersUpdateTargetAndNoWait() {
		assertEquals(
				" for update of p nowait",
				CockroachLockingSupport.COCKROACH_LOCKING_SUPPORT.render( new LockingClauseRequest(
						PessimisticLockKind.UPDATE,
						Timeouts.NO_WAIT,
						List.of( new LockingClauseRequest.TableTarget( "p" ) )
				) )
		);
	}

	@Test
	void omitsUnsupportedSkipLockedSyntax() {
		assertEquals(
				" for share of p",
				CockroachLockingSupport.COCKROACH_LOCKING_SUPPORT.render( new LockingClauseRequest(
						PessimisticLockKind.SHARE,
						Timeouts.SKIP_LOCKED,
						List.of( new LockingClauseRequest.TableTarget( "p" ) )
				) )
		);
	}

	@Test
	void legacyFamilyRendersNoClause() {
		assertEquals(
				"",
				CockroachLockingSupport.LEGACY_COCKROACH_LOCKING_SUPPORT.render( new LockingClauseRequest(
						PessimisticLockKind.UPDATE,
						Timeouts.WAIT_FOREVER,
						List.of()
				) )
		);
	}

	@Test
	void dialectStrategyUsesSuppliedRenderer() {
		final var strategy = new CockroachDialect().getLockingClauseStrategy(
				new QuerySpec( true ),
				new LockOptions( LockMode.PESSIMISTIC_WRITE, Timeouts.NO_WAIT )
		);
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();

		strategy.render( appender );

		assertEquals( " for update nowait", appender.toString() );
	}
}
