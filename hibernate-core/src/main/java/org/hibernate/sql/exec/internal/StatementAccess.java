/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.LogicalConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Steve Ebersole
 */
public class StatementAccess implements AutoCloseable {
	private final Connection jdbcConnection;
	private final LogicalConnection logicalConnection;
	private final SessionFactoryImplementor factory;

	private Statement jdbcStatement;

	public StatementAccess(Connection jdbcConnection, LogicalConnection logicalConnection, SessionFactoryImplementor factory) {
		this.jdbcConnection = jdbcConnection;
		this.logicalConnection = logicalConnection;
		this.factory = factory;
	}

	public Statement getJdbcStatement() {
		if ( jdbcStatement == null ) {
			try {
				jdbcStatement = jdbcConnection.createStatement();
				logicalConnection.getResourceRegistry().register( jdbcStatement, false );
			}
			catch (SQLException e) {
				throw factory.getJdbcServices()
						.getSqlExceptionHelper()
						.convert( e, "Unable to create JDBC Statement" );
			}
		}
		return jdbcStatement;
	}

	public void release() {
		if ( jdbcStatement != null ) {
			try {
				jdbcStatement.close();
				logicalConnection.getResourceRegistry().release( jdbcStatement );
			}
			catch (SQLException e) {
				throw factory.getJdbcServices()
						.getSqlExceptionHelper()
						.convert( e, "Unable to release JDBC Statement" );
			}
		}
	}

	@Override
	public void close() {
		release();
	}
}
