/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import java.util.Objects;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;

import org.hibernate.SPI;

/// The immutable facts used to decide whether a completed SQL statement
/// requires follow-on locking.
///
/// Hibernate creates this request after SQL rendering and any raw-SQL locking
/// attempt. A [FollowOnLockingPolicy] should make its decision only from these
/// stable facts and must not retain the request.
///
/// @param sql the completed SQL considered for execution
/// @param statementShape known structural characteristics of the SQL statement
/// @param pagination effective pagination characteristics
/// @param lockKind requested pessimistic lock kind
/// @param timeout effective lock timeout
/// @param scope requested pessimistic lock scope
/// @param rawSqlRewriteOutcome outcome of any already-rendered SQL rewrite
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public record FollowOnLockingRequest(
		String sql,
		StatementShape statementShape,
		Pagination pagination,
		PessimisticLockKind lockKind,
		Timeout timeout,
		PessimisticLockScope scope,
		LockingSqlRewriteResult.Outcome rawSqlRewriteOutcome) {
	/// Creates a complete follow-on locking request.
	public FollowOnLockingRequest {
		Objects.requireNonNull( sql, "sql" );
		Objects.requireNonNull( statementShape, "statementShape" );
		Objects.requireNonNull( pagination, "pagination" );
		Objects.requireNonNull( lockKind, "lockKind" );
		Objects.requireNonNull( timeout, "timeout" );
		Objects.requireNonNull( scope, "scope" );
		Objects.requireNonNull( rawSqlRewriteOutcome, "rawSqlRewriteOutcome" );
	}

	/// Known structural characteristics of the completed statement. These are
	/// semantic facts collected by Hibernate, not the result of parsing [#sql].
	public record StatementShape(
			boolean distinct,
			boolean grouped,
			boolean ordered,
			boolean setOperation) {
	}

	/// Effective pagination characteristics of the completed statement.
	public record Pagination(boolean limited, boolean offset) {
		/// No requested pagination.
		public static final Pagination NONE = new Pagination( false, false );
	}
}
