/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.hbm2ddl;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Contract for delegates responsible for managing connection used by the
 * hbm2ddl tools.
 *
 * @author Steve Ebersole
 *
 * @deprecated Everything in this package has been replaced with
 * {@link org.hibernate.tool.schema.spi.SchemaManagementTool} and friends.
 */
@Deprecated
public interface ConnectionHelper {
	/**
	 * Prepare the helper for use.
	 *
	 * @param needsAutoCommit Should connection be forced to auto-commit
	 * if not already.
	 */
	void prepare(boolean needsAutoCommit) throws SQLException;

	/**
	 * Get a reference to the connection we are using.
	 *
	 * @return The JDBC connection.
	 */
	Connection getConnection() throws SQLException;

	/**
	 * Release any resources held by this helper.
	 */
	void release() throws SQLException;
}
