/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.bind;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import java.sql.SQLException;

/// Used to check the results of a statement execution
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
@FunctionalInterface
public interface OperationResultChecker {
	/// Check the result of a JDBC operation
	///
	/// @param affectedRowCount The number of rows affected by the operation, as reported by the JDBC driver
	/// @param batchPosition The execution's position within the active batch, if one; if not batching, -1 will be passed
	///
	/// @return `true` indicates an execution that is considered successful; `false` indicates unsuccessful
	boolean checkResult(
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException;
}
