/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.spi;

import org.hibernate.Incubating;

/// Batch variant for a single JDBC statement shape.
///
/// A single-statement batch owns one prepared mutation statement and accepts
/// rows by binding directly to that statement.  It is designed for execution
/// paths, such as graph-based flushing, that have already planned work by
/// statement shape and therefore do not need the [GroupedBatch] statement-group
/// abstraction.
///
/// Implementations may execute implicitly when adding a row fills the configured
/// batch size.  Callers that need per-row follow-up work should therefore assume
/// that [#addToBatch(StatementBinder, BatchedResultChecker)] can execute the
/// JDBC batch before returning.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface SingleStatementBatch extends Batch {
	/// Bind and add one row to this batch.
	///
	/// The supplied binder is called with the batch-owned `PreparedStatement` and
	/// must bind exactly the values for the current row before returning.  The
	/// batch then calls `PreparedStatement.addBatch()`.
	///
	/// `resultChecker` is associated with the row being added.  It may be `null`
	/// when default expectation handling is sufficient.
	///
	/// @param statementBinder binds the current row to the batch statement
	/// @param resultChecker optional row result checker used for stale-state/result
	/// handling associated with this row
	void addToBatch(StatementBinder statementBinder, BatchedResultChecker resultChecker);
}
