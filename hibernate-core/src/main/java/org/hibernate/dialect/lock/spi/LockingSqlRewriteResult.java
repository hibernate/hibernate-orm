/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import java.util.Objects;

import org.hibernate.SPI;

/// The immutable outcome of applying pessimistic locking to already-rendered
/// SQL.
///
/// Rewriters must distinguish an inapplicable request from one they cannot
/// safely fulfill. Hibernate may use [Outcome#UNSUPPORTED] when deciding whether
/// to perform follow-on locking; it must not interpret that outcome as
/// successful locking.
///
/// @param sql rewritten SQL for [Outcome#APPLIED], otherwise the unchanged input
/// SQL
/// @param outcome semantic outcome of the rewrite attempt
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public record LockingSqlRewriteResult(String sql, Outcome outcome) {
	/// Creates a rewrite result.
	public LockingSqlRewriteResult {
		Objects.requireNonNull( sql, "sql" );
		Objects.requireNonNull( outcome, "outcome" );
	}

	/// Creates a result for a successfully applied rewrite.
	public static LockingSqlRewriteResult applied(String sql) {
		return new LockingSqlRewriteResult( sql, Outcome.APPLIED );
	}

	/// Creates a result for a request which requires no SQL rewrite.
	public static LockingSqlRewriteResult notApplicable(String sql) {
		return new LockingSqlRewriteResult( sql, Outcome.NOT_APPLICABLE );
	}

	/// Creates a result for a request which cannot be applied safely.
	public static LockingSqlRewriteResult unsupported(String sql) {
		return new LockingSqlRewriteResult( sql, Outcome.UNSUPPORTED );
	}

	/// Classification of the rewrite outcome.
	public enum Outcome {
		/// Locking was applied to the returned SQL.
		APPLIED,
		/// The request did not require a rewrite.
		NOT_APPLICABLE,
		/// The available SQL and target information could not be rewritten safely.
		UNSUPPORTED
	}
}
