/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.batch.spi;

import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/// Checks the result for one row in a JDBC batch.
///
/// The checker gives higher-level mutation code a row-specific hook for handling
/// optimistic-lock and stale-state outcomes after JDBC batch execution.  It is
/// associated with a row when that row is added to a [SingleStatementBatch].
///
/// Implementations return `true` when the observed outcome is acceptable and
/// `false` when the batch should keep treating the row as a stale-state failure.
/// A thrown [SQLException] is converted by the owning batch.
///
/// @see SingleStatementBatch#addToBatch(StatementBinder, BatchedResultChecker)
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@FunctionalInterface
public interface BatchedResultChecker {
	/// Check the result for one row of a JDBC batch.
	///
	/// @param affectedRowCount the row count reported for this batch entry
	/// @param batchPosition zero-based position of the row within the executed JDBC batch
	/// @param sqlString SQL string for the batch statement
	/// @param sessionFactory the session factory associated with execution
	///
	/// @return `true` if the result is acceptable, `false` to keep treating it as
	/// a stale-state failure
	///
	/// @throws SQLException if result checking needs to report a JDBC failure
	boolean checkResult(
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException;
}
