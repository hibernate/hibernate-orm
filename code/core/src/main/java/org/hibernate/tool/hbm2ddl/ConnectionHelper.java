package org.hibernate.tool.hbm2ddl;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Contract for delegates responsible for managing connection used by the
 * hbm2ddl tools.
 *
 * @author Steve Ebersole
 */
interface ConnectionHelper {
	/**
	 * Prepare the helper for use.
	 *
	 * @param needsAutoCommit Should connection be forced to auto-commit
	 * if not already.
	 * @throws SQLException
	 */
	public void prepare(boolean needsAutoCommit) throws SQLException;

	/**
	 * Get a reference to the connection we are using.
	 *
	 * @return The JDBC connection.
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException;

	/**
	 * Release any resources held by this helper.
	 *
	 * @throws SQLException
	 */
	public void release() throws SQLException;
}
