/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.Objects;
import java.util.regex.Pattern;

import org.hibernate.Internal;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.internal.LockingSqlRewriterSupport;
import org.hibernate.dialect.lock.spi.FollowOnLockingRequest;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteResult;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationJdbcInstructions;
import org.hibernate.dialect.pagination.spi.PaginationRequest;
import org.hibernate.dialect.pagination.spi.PaginationResult;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.internal.QuerySqlDecorator;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.exec.spi.JdbcPaginationApplication;

import static java.util.Collections.emptyMap;

/// Finalizes execution-time SQL details for a JDBC select.
///
/// Owns the ordered completed-SQL pipeline: pagination, follow-on-policy
/// evaluation and raw locking, followed by SQL hints and comments.
///
/// @since 8.0
/// @author Steve Ebersole
@Internal
public final class JdbcSelectSqlFinalizer {
	private static final Pattern DISTINCT = keyword( "distinct" );
	private static final Pattern GROUP_BY = Pattern.compile( "\\bgroup\\s+by\\b", Pattern.CASE_INSENSITIVE );
	private static final Pattern ORDER_BY = Pattern.compile( "\\border\\s+by\\b", Pattern.CASE_INSENSITIVE );
	private static final Pattern SET_OPERATION = Pattern.compile(
			"\\b(?:union|intersect|except)\\b",
			Pattern.CASE_INSENSITIVE
	);

	private JdbcSelectSqlFinalizer() {
	}

	/// Finalizes execution-time SQL and returns its immutable JDBC pagination
	/// instructions.
	public static FinalizedResult finalizeSql(
			String sql,
			JdbcPaginationApplication paginationApplication,
			int rowsToSkip,
			int maxRows,
			PaginationRequest paginationRequest,
			LimitHandler limitHandler,
			JdbcLockingApplication lockingApplication,
			LockOptions lockOptions,
			Limit limit,
			LockingSupport lockingSupport,
			boolean supportsFollowOnLocking,
			Dialect dialect,
			QueryOptions queryOptions,
			boolean commentsEnabled) {
		Objects.requireNonNull( paginationApplication, "paginationApplication" );
		Objects.requireNonNull( limitHandler, "limitHandler" );
		Objects.requireNonNull( dialect, "dialect" );

		final PaginationResult paginationResult = switch ( paginationApplication ) {
			case NONE -> PaginationResult.unchanged( sql );
			case RAW_SQL -> paginationRequest.isEmpty()
					? PaginationResult.unchanged( sql )
					: limitHandler.processSql( paginationRequest );
			case RENDERED, JDBC -> new PaginationResult(
					sql,
					new PaginationJdbcInstructions(
							java.util.List.of(),
							java.util.List.of(),
							maxRows == Integer.MAX_VALUE ? null : maxRows,
							rowsToSkip
					)
			);
		};

		final Result lockingResult = finalizeLocking(
				paginationResult.sql(),
				lockingApplication,
				lockOptions,
				limit,
				lockingSupport,
				supportsFollowOnLocking
		);
		final String finalSql = QuerySqlDecorator.decorate(
				lockingResult.sql(),
				queryOptions,
				commentsEnabled,
				dialect
		);
		return new FinalizedResult(
				finalSql,
				lockingResult.lockingApplication(),
				paginationResult.jdbcInstructions()
		);
	}

	/// Finalizes completed-SQL locking for the select plan.
	public static Result finalizeLocking(
			String sql,
			JdbcLockingApplication lockingApplication,
			LockOptions lockOptions,
			Limit limit,
			LockingSupport lockingSupport,
			boolean supportsFollowOnLocking) {
		Objects.requireNonNull( sql, "sql" );
		Objects.requireNonNull( lockingApplication, "lockingApplication" );
		Objects.requireNonNull( lockingSupport, "lockingSupport" );

		if ( lockOptions == null || lockOptions.isEmpty() ) {
			return new Result( sql, JdbcLockingApplication.NONE );
		}

		return switch ( lockingApplication ) {
			case NONE, RENDERED, FOLLOW_ON -> new Result( sql, lockingApplication );
			case RAW_SQL -> finalizeRawSqlLocking(
					sql,
					lockOptions,
					limit,
					lockingSupport,
					supportsFollowOnLocking
			);
		};
	}

	private static Result finalizeRawSqlLocking(
			String sql,
			LockOptions lockOptions,
			Limit limit,
			LockingSupport lockingSupport,
			boolean supportsFollowOnLocking) {
		final LockingSqlRewriteResult rewriteResult = LockingSqlRewriterSupport.rewrite(
				lockingSupport,
				sql,
				lockOptions,
				emptyMap()
		);
		final FollowOnLockingRequest request = request( sql, lockOptions, limit, rewriteResult );
		final boolean policyRequiresFollowOn = lockingSupport.getFollowOnLockingPolicy()
				.useFollowOnLocking( request );
		final boolean rewriteUnsupported = rewriteResult.outcome() == LockingSqlRewriteResult.Outcome.UNSUPPORTED;

		return switch ( lockOptions.getFollowOnStrategy() ) {
			case FORCE -> followOn( sql, supportsFollowOnLocking );
			case ALLOW -> policyRequiresFollowOn || rewriteUnsupported
					? supportsFollowOnLocking
							? followOn( sql, true )
							: new Result( sql, JdbcLockingApplication.NONE )
					: rewritten( rewriteResult );
			case DISALLOW -> {
				if ( policyRequiresFollowOn || rewriteUnsupported ) {
					throw new IllegalQueryOperationException(
							"Follow-on locking is required for the completed SQL statement but was disallowed"
					);
				}
				yield rewritten( rewriteResult );
			}
			case IGNORE -> policyRequiresFollowOn || rewriteUnsupported
					? new Result( sql, JdbcLockingApplication.NONE )
					: rewritten( rewriteResult );
		};
	}

	private static Result followOn(String sql, boolean supported) {
		if ( !supported ) {
			throw new IllegalQueryOperationException(
					"Follow-on locking is required but the JDBC select plan does not provide follow-on actions"
			);
		}
		return new Result( sql, JdbcLockingApplication.FOLLOW_ON );
	}

	private static Result rewritten(LockingSqlRewriteResult rewriteResult) {
		return new Result(
				rewriteResult.sql(),
				rewriteResult.outcome() == LockingSqlRewriteResult.Outcome.APPLIED
						? JdbcLockingApplication.RENDERED
						: JdbcLockingApplication.NONE
		);
	}

	private static FollowOnLockingRequest request(
			String sql,
			LockOptions lockOptions,
			Limit limit,
			LockingSqlRewriteResult rewriteResult) {
		return new FollowOnLockingRequest(
				sql,
				new FollowOnLockingRequest.StatementShape(
						DISTINCT.matcher( sql ).find(),
						GROUP_BY.matcher( sql ).find(),
						ORDER_BY.matcher( sql ).find(),
						SET_OPERATION.matcher( sql ).find()
				),
				pagination( limit ),
				LockingSqlRewriterSupport.interpretLockKind( lockOptions ),
				LockingSqlRewriterSupport.effectiveTimeout( lockOptions ),
				lockOptions.getScope(),
				rewriteResult.outcome()
		);
	}

	private static FollowOnLockingRequest.Pagination pagination(Limit limit) {
		return limit == null || limit.isEmpty()
				? FollowOnLockingRequest.Pagination.NONE
				: new FollowOnLockingRequest.Pagination(
						limit.getMaxRows() != null,
						limit.getFirstRow() != null
				);
	}

	private static Pattern keyword(String keyword) {
		return Pattern.compile( "\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE );
	}

	/// The finalized SQL and the resulting locking disposition.
	public record Result(String sql, JdbcLockingApplication lockingApplication) {
		public Result {
			Objects.requireNonNull( sql, "sql" );
			Objects.requireNonNull( lockingApplication, "lockingApplication" );
		}
	}

	/// The fully finalized SQL and its execution metadata.
	public record FinalizedResult(
			String sql,
			JdbcLockingApplication lockingApplication,
			PaginationJdbcInstructions paginationInstructions) {
		public FinalizedResult {
			Objects.requireNonNull( sql, "sql" );
			Objects.requireNonNull( lockingApplication, "lockingApplication" );
			Objects.requireNonNull( paginationInstructions, "paginationInstructions" );
		}
	}
}
