/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Contract for extracting {@link ResultSet}s from {@link Statement}s, executing the statements,
 * managing resources, and logging statement calls.
 * <p>
 * Generally the methods here for dealing with {@link CallableStatement} are extremely limited
 *
 * @author Brett Meyer
 * @author Steve Ebersole
 *
 * @see JdbcCoordinator#getResultSetReturn()
 */
public interface ResultSetReturn {

	/**
	 * Extract the {@link ResultSet} from the {@link PreparedStatement}.
	 *
	 * @param statement The {@link PreparedStatement} from which to extract the {@link ResultSet}
	 *
	 * @return The extracted {@link ResultSet}
	 */
	ResultSet extract(PreparedStatement statement, String sql);

	/**
	 * Performs the given SQL statement, expecting a {@link ResultSet} in return
	 *
	 * @param statement The JDBC {@link Statement} object to use
	 * @param sql The SQL to execute
	 *
	 * @return The resulting {@link ResultSet}
	 */
	ResultSet extract(Statement statement, String sql);

	/**
	 * Execute the {@link PreparedStatement} return its first {@link ResultSet}, if any.
	 * If there is no {@link ResultSet}, returns {@code null}
	 *
	 * @param statement The {@link PreparedStatement} to execute
	 * @param sql For error reporting
	 *
	 * @return The extracted {@link ResultSet}, or {@code null}
	 */
	ResultSet execute(PreparedStatement statement, String sql);

	/**
	 * Performs the given SQL statement, returning its first {@link ResultSet}, if any.
	 * If there is no {@link ResultSet}, returns {@code null}
	 *
	 * @param statement The JDBC {@link Statement} object to use
	 * @param sql The SQL to execute
	 *
	 * @return The extracted {@link ResultSet}, or {@code null}
	 */
	ResultSet execute(Statement statement, String sql);

	/**
	 * Execute the {@link PreparedStatement}, returning its "affected row count".
	 *
	 * @param statement The {@link PreparedStatement} to execute
	 * @param sql For error reporting
	 *
	 * @return The {@link PreparedStatement#executeUpdate()} result
	 */
	int executeUpdate(PreparedStatement statement, String sql);

	/**
	 * Execute the given SQL statement returning its "affected row count".
	 *
	 * @param statement The JDBC {@link Statement} object to use
	 * @param sql The SQL to execute
	 *
	 * @return The {@link PreparedStatement#executeUpdate(String)} result
	 */
	int executeUpdate(Statement statement, String sql);
}
