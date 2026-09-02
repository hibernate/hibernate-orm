/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import jakarta.persistence.Timeout;
import jakarta.persistence.PessimisticLockScope;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.community.dialect.lock.internal.TeradataLockingSupport;
import org.hibernate.community.dialect.lock.internal.TiDBLockingSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.FollowOnLockingRequest;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteResult;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests the TimesTen, TiDB, and Teradata locking-clause renderer profiles.
///
/// @author Steve Ebersole
class TimesTenTiDBTeradataLockingClauseRendererTest {
	@Test
	void timesTenRendersTargetsAndQueryLevelTimeouts() {
		assertEquals(
				" for update of account.id,account.tenant_id nowait",
				StandardLockingSupports.timesTen().getLockingClauseRenderer().render(
						request(
								PessimisticLockKind.SHARE,
								Timeouts.NO_WAIT,
								new LockingClauseRequest.ColumnTarget( "account", "id" ),
								new LockingClauseRequest.ColumnTarget( "account", "tenant_id" )
						)
				)
		);
		assertEquals(
				" for update wait 2",
				StandardLockingSupports.timesTen().getLockingClauseRenderer().render(
						request( PessimisticLockKind.UPDATE, Timeout.milliseconds( 1_500 ) )
				)
		);
	}

	@Test
	void tiDbPreservesItsReadWriteTimeoutDistinction() {
		assertEquals(
				" for update",
				TiDBLockingSupport.TIDB_LOCKING_SUPPORT.render(
						request( PessimisticLockKind.SHARE, Timeout.seconds( 2 ) )
				)
		);
		assertEquals(
				" for update wait 2",
				TiDBLockingSupport.TIDB_LOCKING_SUPPORT.render(
						request( PessimisticLockKind.UPDATE, Timeout.seconds( 2 ) )
				)
		);
		assertEquals(
				" for update nowait",
				TiDBLockingSupport.TIDB_LOCKING_SUPPORT.render(
						request( PessimisticLockKind.SHARE, Timeouts.NO_WAIT )
				)
		);
	}

	@Test
	void teradataRendersStatementPrefixes() {
		final TeradataLockingSupport lockingSupport = new TeradataLockingSupport();
		assertEquals(
				" Locking row for read  ",
				lockingSupport.render( request( PessimisticLockKind.SHARE, Timeouts.WAIT_FOREVER ) )
		);
		assertEquals(
				" Locking row for write  nowait ",
				lockingSupport.render( request( PessimisticLockKind.UPDATE, Timeouts.NO_WAIT ) )
		);
	}

	@Test
	void teradataCompletedSqlPreservesTheVersionedPlacement() {
		final String sql = "select * from person";
		assertEquals(
				"select * from person Locking row for write ",
				new TeradataDialect( DatabaseVersion.make( 13 ) )
						.getLockingSupport()
						.getLockingSqlRewriter()
						.rewrite( new LockingSqlRewriteRequest(
								sql,
								PessimisticLockKind.UPDATE,
								Timeouts.WAIT_FOREVER,
								List.of()
						) )
						.sql()
		);
		assertEquals(
				" Locking row for write  select * from person",
				new TeradataDialect( DatabaseVersion.make( 14 ) )
						.getLockingSupport()
						.getLockingSqlRewriter()
						.rewrite( new LockingSqlRewriteRequest(
								sql,
								PessimisticLockKind.UPDATE,
								Timeouts.WAIT_FOREVER,
								List.of()
						) )
						.sql()
		);
	}

	@Test
	void teradataFollowOnPolicyPreservesTheVersionBoundary() {
		final FollowOnLockingRequest request = new FollowOnLockingRequest(
				"select * from person",
				new FollowOnLockingRequest.StatementShape( false, false, false, false ),
				FollowOnLockingRequest.Pagination.NONE,
				PessimisticLockKind.UPDATE,
				Timeouts.WAIT_FOREVER,
				PessimisticLockScope.NORMAL,
				LockingSqlRewriteResult.Outcome.APPLIED
		);
		assertFalse(
				new TeradataDialect( DatabaseVersion.make( 13 ) )
						.getLockingSupport()
						.getFollowOnLockingPolicy()
						.useFollowOnLocking( request )
		);
		assertTrue(
				new TeradataDialect( DatabaseVersion.make( 14 ) )
						.getLockingSupport()
						.getFollowOnLockingPolicy()
						.useFollowOnLocking( request )
		);
	}

	@Test
	void dialectStrategiesUseTheSuppliedProfiles() {
		assertDialectRenders(
				new TimesTenDialect(),
				LockMode.PESSIMISTIC_READ,
				Timeouts.NO_WAIT,
				" for update nowait"
		);
		assertDialectRenders(
				new TiDBDialect(),
				LockMode.PESSIMISTIC_WRITE,
				Timeout.seconds( 2 ),
				" for update wait 2"
		);
		assertDialectRenders(
				new TeradataDialect( DatabaseVersion.make( 13 ) ),
				LockMode.PESSIMISTIC_WRITE,
				Timeouts.NO_WAIT,
				""
		);
		assertDialectRenders(
				new TeradataDialect( DatabaseVersion.make( 14 ) ),
				LockMode.PESSIMISTIC_READ,
				Timeouts.NO_WAIT,
				" Locking row for read   nowait "
		);
	}

	private static LockingClauseRequest request(
			PessimisticLockKind lockKind,
			Timeout timeout,
			LockingClauseRequest.Target... targets) {
		return new LockingClauseRequest( lockKind, timeout, List.of( targets ) );
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
