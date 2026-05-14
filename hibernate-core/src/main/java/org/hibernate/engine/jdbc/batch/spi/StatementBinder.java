/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.spi;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Binds one row worth of values to a JDBC statement.
///
/// This is the single-statement counterpart to
/// `JdbcValueBindings.beforeStatement(...)`.  The batch implementation prepares
/// and owns the statement, then invokes the binder for each row just before
/// adding that row to the JDBC batch.
///
/// A binder should not execute, close, clear, or retain the statement.  It should
/// only bind parameter values for the current row.  Any [SQLException] raised
/// while binding is handled by the owning batch and converted through the
/// session's JDBC services.
///
/// @see SingleStatementBatch#addToBatch(StatementBinder, BatchedResultChecker)
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@FunctionalInterface
public interface StatementBinder {
	/// Bind parameter values for one row.
	///
	/// @param statement the prepared statement owned by the batch
	/// @param session the session associated with the batch
	///
	/// @throws SQLException if binding fails
	void bind(PreparedStatement statement, SharedSessionContractImplementor session) throws SQLException;
}
