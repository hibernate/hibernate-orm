/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * @author Steve Ebersole
 */
public class TargetDatabaseImpl implements GenerationTarget {
	private final JdbcConnectionAccess connectionAccess;

	private Connection connection;
	private Statement statement;

	public TargetDatabaseImpl(JdbcConnectionAccess connectionAccess) {
		this.connectionAccess = connectionAccess;
	}

	@Override
	public void prepare() {
		try {
			connection = connectionAccess.obtainConnection();
			connection.setAutoCommit( true );
		}
		catch (SQLException e) {
			throw new SchemaManagementException( "Unable to open JDBC connection for schema management target", e );
		}

		try {
			statement = connection.createStatement();
		}
		catch (SQLException e) {
			throw new SchemaManagementException( "Unable to create JDBC Statement for schema management target", e );
		}
	}

	@Override
	public void accept(String action) {
		try {
			statement.executeUpdate( action );
		}
		catch (SQLException e) {
			throw new SchemaManagementException( "Unable to execute schema management to JDBC target [" + action + "]", e );
		}
	}

	@Override
	public void release() {
		if ( statement != null ) {
			try {
				statement.close();
			}
			catch (SQLException ignore) {
			}
		}
		if ( connection != null ) {
			try {

				connectionAccess.releaseConnection( connection );
			}
			catch (SQLException ignore) {
			}
		}
	}
}
