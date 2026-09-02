/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.Timeout;

import org.hibernate.SPI;

/// A complete, read-only request to render a pessimistic locking clause.
///
/// An empty [#targets] list requests locking of the complete result set without
/// an `OF` list. Otherwise, each target retains whether the database expects a
/// table alias or a qualified column reference.
///
/// @see LockingClauseRenderer
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public record LockingClauseRequest(
		PessimisticLockKind lockKind,
		Timeout timeout,
		List<Target> targets) {
	/// Creates an immutable locking-clause request.
	public LockingClauseRequest {
		Objects.requireNonNull( lockKind, "lockKind" );
		Objects.requireNonNull( timeout, "timeout" );
		if ( lockKind == PessimisticLockKind.NONE ) {
			throw new IllegalArgumentException( "A locking-clause request requires a pessimistic lock kind" );
		}
		targets = List.copyOf( Objects.requireNonNull( targets, "targets" ) );
	}

	/// A structured target named by a locking clause.
	public sealed interface Target permits TableTarget, ColumnTarget {
	}

	/// A table-alias locking target.
	public record TableTarget(String tableAlias) implements Target {
		/// Creates a table target.
		public TableTarget {
			Objects.requireNonNull( tableAlias, "tableAlias" );
		}
	}

	/// A qualified column-reference locking target.
	public record ColumnTarget(String tableAlias, String columnName) implements Target {
		/// Creates a column target.
		public ColumnTarget {
			Objects.requireNonNull( tableAlias, "tableAlias" );
			Objects.requireNonNull( columnName, "columnName" );
		}
	}
}
