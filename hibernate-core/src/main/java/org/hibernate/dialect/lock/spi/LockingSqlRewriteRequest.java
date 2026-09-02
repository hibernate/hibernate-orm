/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.Timeout;

import org.hibernate.SPI;

/// A complete, read-only request to apply pessimistic locking to already-
/// rendered SQL.
///
/// The target list contains only structure known by the caller. An empty list
/// does not authorize a rewriter to discover table references by parsing the
/// SQL string.
///
/// @see LockingSqlRewriter
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public record LockingSqlRewriteRequest(
		String sql,
		PessimisticLockKind lockKind,
		Timeout timeout,
		List<LockingClauseRequest.Target> targets) {
	/// Creates an immutable locking SQL rewrite request.
	public LockingSqlRewriteRequest {
		Objects.requireNonNull( sql, "sql" );
		Objects.requireNonNull( lockKind, "lockKind" );
		Objects.requireNonNull( timeout, "timeout" );
		targets = List.copyOf( Objects.requireNonNull( targets, "targets" ) );
	}
}
