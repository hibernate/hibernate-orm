/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.mutation;

import java.sql.SQLException;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;

/**
 * Used to check the results of a statement execution
 *
 * @author Steve Ebersole
 */
@Incubating
@FunctionalInterface
public interface OperationResultChecker {
	/**
	 * Check the result of a JDBC operation
	 *
	 * @param statementDetails Details for the SQL statement executed
	 * @param affectedRowCount The number of rows affected by the operation, as reported by the JDBC driver
	 * @param batchPosition The execution's position within the active batch, if one; if not batching, -1 will be passed
	 *
	 * @return {@code true} indicates an execution that is considered successful; {@code false} indicates unsuccessful
	 */
	boolean checkResult(PreparedStatementDetails statementDetails, int affectedRowCount, int batchPosition) throws SQLException;
}
