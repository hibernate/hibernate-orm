/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * This {@link ConnectionProvider} extends any other ConnectionProvider that would be used by default taken the current configuration properties, and it
 * just sets the READ_COMMITTED_SNAPSHOT isolation level for SQL Server.
 *
 * @author Vlad Mihalcea
 */
public class SQLServerSnapshotIsolationConnectionProvider
		extends ConnectionProviderDelegate {

	private static final String RCS = "ALTER DATABASE %s SET READ_COMMITTED_SNAPSHOT %s";
	private static final String SI = "ALTER DATABASE %s SET ALLOW_SNAPSHOT_ISOLATION %s";

	private String dbName = null;

	@Override
	public Connection getConnection() throws SQLException {
		Connection connection = super.getConnection();
		try(Statement statement = connection.createStatement()) {
			if ( dbName == null ) {
				try(ResultSet rs = statement.executeQuery( "SELECT DB_NAME()" )) {
					rs.next();
					dbName = rs.getString( 1 );
				}
			}
			statement.executeUpdate(String.format( RCS, dbName, "ON" ));
			statement.executeUpdate(String.format( SI, dbName, "ON" ));
		}
		catch (SQLException se) {
			fail( se.getMessage());
		}
		return connection;
	}
}
