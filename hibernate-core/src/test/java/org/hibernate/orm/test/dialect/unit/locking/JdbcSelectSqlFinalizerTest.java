/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locking;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.PessimisticLockScope;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.internal.NoLockingSupport;
import org.hibernate.dialect.lock.internal.OracleLockingSupport;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.FollowOnLockingPolicy;
import org.hibernate.dialect.lock.spi.FollowOnLockingRequest;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteResult;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.pagination.spi.LimitOffsetLimitHandler;
import org.hibernate.dialect.pagination.spi.NoopLimitHandler;
import org.hibernate.dialect.pagination.spi.Oracle12LimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationRequest;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.Limit;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.exec.spi.JdbcPaginationApplication;
import org.hibernate.sql.results.jdbc.internal.JdbcSelectSqlFinalizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests explicit select-locking dispositions and focused follow-on policy
/// requests during completed-SQL finalization.
///
/// @author Steve Ebersole
@SuppressWarnings("removal")
class JdbcSelectSqlFinalizerTest {
	@Test
	void rawPaginationPrecedesRawLockingAndFinalComment() {
		final var queryOptions = new QueryOptionsImpl();
		queryOptions.setComment( "paged query" );
		final var limit = new Limit( 5, 10 );

		final var result = JdbcSelectSqlFinalizer.finalizeSql(
				"select p.id from person p",
				JdbcPaginationApplication.RAW_SQL,
				0,
				Integer.MAX_VALUE,
				new PaginationRequest( "select p.id from person p", 5, 10, 0, null ),
				LimitOffsetLimitHandler.INSTANCE,
				JdbcLockingApplication.RAW_SQL,
				lockOptions(),
				limit,
				LockingSupportSimple.STANDARD_SUPPORT,
				false,
				new H2Dialect(),
				queryOptions,
				true
		);

		assertEquals(
				"/* paged query */ select p.id from person p limit ? offset ? for update",
				result.sql()
		);
		assertEquals( JdbcLockingApplication.RENDERED, result.lockingApplication() );
		assertEquals( java.util.List.of(), result.paginationInstructions().parametersAtStart() );
		assertEquals( java.util.List.of( 10, 5 ), result.paginationInstructions().parametersAtEnd() );
	}

	@Test
	void followOnPolicySeesSqlAfterRawPagination() {
		final var queryOptions = new QueryOptionsImpl();
		final var limit = new Limit( 5, 10 );
		final String sql = "select distinct p.id from person p order by p.id";

		final var result = JdbcSelectSqlFinalizer.finalizeSql(
				sql,
				JdbcPaginationApplication.RAW_SQL,
				0,
				Integer.MAX_VALUE,
				new PaginationRequest( sql, 5, 10, 0, null ),
				LimitOffsetLimitHandler.INSTANCE,
				JdbcLockingApplication.RAW_SQL,
				lockOptions(),
				limit,
				OracleLockingSupport.ORACLE_LOCKING_SUPPORT,
				true,
				new H2Dialect(),
				queryOptions,
				false
		);

		assertEquals( sql + " limit ? offset ?", result.sql() );
		assertEquals( JdbcLockingApplication.FOLLOW_ON, result.lockingApplication() );
		assertEquals( java.util.List.of( 10, 5 ), result.paginationInstructions().parametersAtEnd() );
	}

	@Test
	void immutablePaginationResultUsesAbsoluteNativeMarkerPositions() {
		final var result = LimitOffsetLimitHandler.INSTANCE.processSql(
				new PaginationRequest(
						"select p.id from person p where p.name = ? and p.active = ?",
						5,
						10,
						2,
						(position, jdbcType) -> "$" + position
				)
		);

		assertEquals(
				"select p.id from person p where p.name = ? and p.active = ? limit $3 offset $4",
				result.sql()
		);
		assertEquals( java.util.List.of( 10, 5 ), result.jdbcInstructions().parametersAtEnd() );
	}

	@Test
	void jdbcOnlyPaginationDoesNotRewriteSql() {
		final var queryOptions = new QueryOptionsImpl();
		final var result = JdbcSelectSqlFinalizer.finalizeSql(
				"select p.id from person p",
				JdbcPaginationApplication.JDBC,
				5,
				15,
				new PaginationRequest( "select p.id from person p", 5, 10, 0, null ),
				NoopLimitHandler.INSTANCE,
				JdbcLockingApplication.NONE,
				LockOptions.NONE,
				new Limit( 5, 10 ),
				LockingSupportSimple.STANDARD_SUPPORT,
				false,
				new H2Dialect(),
				queryOptions,
				false
		);

		assertEquals( "select p.id from person p", result.sql() );
		assertEquals( 15, result.paginationInstructions().maxRows() );
		assertEquals( 5, result.paginationInstructions().rowsToSkip() );
	}

	@Test
	void singletonLimitHandlerDoesNotRetainPriorSqlShape() {
		final var plainRequest = new PaginationRequest(
				"select p.id from person p",
				5,
				10,
				0,
				null
		);
		final var lockedRequest = new PaginationRequest(
				"select p.id from person p for update",
				5,
				10,
				0,
				null
		);

		final var first = Oracle12LimitHandler.INSTANCE.processSql( plainRequest );
		final var locked = Oracle12LimitHandler.INSTANCE.processSql( lockedRequest );
		final var second = Oracle12LimitHandler.INSTANCE.processSql( plainRequest );

		assertEquals( first, second );
		assertEquals( java.util.List.of( 5, 10 ), first.jdbcInstructions().parametersAtEnd() );
		assertEquals( java.util.List.of( 15, 5 ), locked.jdbcInstructions().parametersAtEnd() );
		assertTrue( locked.sql().endsWith( " for update" ) );
	}

	@Test
	void renderedAstLockingIsNotAppliedAgain() {
		final var result = JdbcSelectSqlFinalizer.finalizeLocking(
				"select distinct p.id from person p for update",
				JdbcLockingApplication.RENDERED,
				lockOptions(),
				Limit.NONE,
				OracleLockingSupport.ORACLE_LOCKING_SUPPORT,
				false
		);

		assertEquals( "select distinct p.id from person p for update", result.sql() );
		assertEquals( JdbcLockingApplication.RENDERED, result.lockingApplication() );
	}

	@Test
	void rawSqlLockingIsAppliedExactlyOnce() {
		final var result = JdbcSelectSqlFinalizer.finalizeLocking(
				"select p.id from person p",
				JdbcLockingApplication.RAW_SQL,
				lockOptions(),
				Limit.NONE,
				LockingSupportSimple.STANDARD_SUPPORT,
				false
		);

		assertEquals( "select p.id from person p for update", result.sql() );
		assertEquals( JdbcLockingApplication.RENDERED, result.lockingApplication() );

		final var finalizedAgain = JdbcSelectSqlFinalizer.finalizeLocking(
				result.sql(),
				result.lockingApplication(),
				lockOptions(),
				Limit.NONE,
				LockingSupportSimple.STANDARD_SUPPORT,
				false
		);
		assertEquals( result, finalizedAgain );
	}

	@Test
	void oraclePolicyUsesShapeAndPaginationFacts() {
		final var distinct = JdbcSelectSqlFinalizer.finalizeLocking(
				"select distinct p.id from person p",
				JdbcLockingApplication.RAW_SQL,
				lockOptions(),
				Limit.NONE,
				OracleLockingSupport.ORACLE_LOCKING_SUPPORT,
				true
		);
		assertEquals( JdbcLockingApplication.FOLLOW_ON, distinct.lockingApplication() );

		final var orderedAndLimited = JdbcSelectSqlFinalizer.finalizeLocking(
				"select p.id from person p order by p.id",
				JdbcLockingApplication.RAW_SQL,
				lockOptions(),
				new Limit( null, 10 ),
				OracleLockingSupport.ORACLE_LOCKING_SUPPORT,
				true
		);
		assertEquals( JdbcLockingApplication.FOLLOW_ON, orderedAndLimited.lockingApplication() );

		final var simple = JdbcSelectSqlFinalizer.finalizeLocking(
				"select p.id from person p",
				JdbcLockingApplication.RAW_SQL,
				lockOptions(),
				Limit.NONE,
				OracleLockingSupport.ORACLE_LOCKING_SUPPORT,
				false
		);
		assertEquals( "select p.id from person p for update", simple.sql() );
	}

	@Test
	void followOnPolicyReceivesFocusedFacts() {
		final AtomicReference<FollowOnLockingRequest> observed = new AtomicReference<>();
		final var support = new LockingSupportSimple(
				PessimisticLockStyle.CLAUSE,
				LockTimeoutType.QUERY,
				OuterJoinLockingType.FULL,
				ConnectionLockTimeoutStrategy.NONE
		) {
			@Override
			public FollowOnLockingPolicy getFollowOnLockingPolicy() {
				return request -> {
					observed.set( request );
					return true;
				};
			}
		};
		final Limit limit = new Limit( 5, 10 );
		final LockOptions lockOptions = new LockOptions(
				LockMode.UPGRADE_NOWAIT,
				Timeouts.WAIT_FOREVER_MILLI,
				PessimisticLockScope.EXTENDED,
				Locking.FollowOn.ALLOW
		);

		final var result = JdbcSelectSqlFinalizer.finalizeLocking(
				"select distinct p.id from person p union select a.id from archive a order by 1",
				JdbcLockingApplication.RAW_SQL,
				lockOptions,
				limit,
				support,
				true
		);

		assertEquals( JdbcLockingApplication.FOLLOW_ON, result.lockingApplication() );
		final FollowOnLockingRequest request = observed.get();
		assertEquals( result.sql(), request.sql() );
		assertTrue( request.statementShape().distinct() );
		assertFalse( request.statementShape().grouped() );
		assertTrue( request.statementShape().ordered() );
		assertTrue( request.statementShape().setOperation() );
		assertTrue( request.pagination().limited() );
		assertTrue( request.pagination().offset() );
		assertEquals( PessimisticLockKind.UPDATE, request.lockKind() );
		assertEquals( Timeouts.NO_WAIT, request.timeout() );
		assertEquals( PessimisticLockScope.EXTENDED, request.scope() );
		assertEquals(
				LockingSqlRewriteResult.Outcome.APPLIED,
				request.rawSqlRewriteOutcome()
		);
	}

	@Test
	void unsupportedRawRewriteFailsWhenFollowOnIsDisallowed() {
		final LockOptions lockOptions = lockOptions()
				.setFollowOnStrategy( Locking.FollowOn.DISALLOW );

		assertThrows(
				IllegalQueryOperationException.class,
				() -> JdbcSelectSqlFinalizer.finalizeLocking(
						"select p.id from person p",
						JdbcLockingApplication.RAW_SQL,
						lockOptions,
						Limit.NONE,
						NoLockingSupport.NO_LOCKING_SUPPORT,
						false
				)
		);
	}

	@Test
	void unsupportedRawRewriteIsIgnoredWhenFollowOnIsAllowedButUnavailable() {
		final var result = JdbcSelectSqlFinalizer.finalizeLocking(
				"select p.id from person p",
				JdbcLockingApplication.RAW_SQL,
				lockOptions(),
				Limit.NONE,
				NoLockingSupport.NO_LOCKING_SUPPORT,
				false
		);

		assertEquals( "select p.id from person p", result.sql() );
		assertEquals( JdbcLockingApplication.NONE, result.lockingApplication() );
	}

	private static LockOptions lockOptions() {
		return new LockOptions( LockMode.PESSIMISTIC_WRITE );
	}
}
