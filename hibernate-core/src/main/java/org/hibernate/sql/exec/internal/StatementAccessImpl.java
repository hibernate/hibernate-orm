/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.LogicalConnection;
import org.hibernate.sql.exec.spi.StatementAccess;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lazy access to a JDBC {@linkplain Statement}.
 * Manages various tasks around creation and ensuring it gets cleaned up.
 *
 * @author Steve Ebersole
 */
public class StatementAccessImpl implements StatementAccess {
	private final Connection jdbcConnection;
	private final LogicalConnection logicalConnection;
	private final SessionFactoryImplementor factory;

	private Statement jdbcStatement;

	public StatementAccessImpl(Connection jdbcConnection, LogicalConnection logicalConnection, SessionFactoryImplementor factory) {
		this.jdbcConnection = jdbcConnection;
		this.logicalConnection = logicalConnection;
		this.factory = factory;
	}

	@Override public Statement getJdbcStatement() {
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
}
