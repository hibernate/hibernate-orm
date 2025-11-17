/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.resource.jdbc.spi.JdbcEventHandler;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;

/**
 * Standard implementation of the {@link ResultSetReturn} contract
 *
 * @author Brett Meyer
 */
public class ResultSetReturnImpl implements ResultSetReturn {
	private final JdbcCoordinator jdbcCoordinator;

	private final SqlStatementLogger sqlStatementLogger;
	private final SqlExceptionHelper sqlExceptionHelper;

	/**
	 * Constructs a ResultSetReturnImpl
	 *
	 * @param jdbcCoordinator The JdbcCoordinator
	 */
	public ResultSetReturnImpl(JdbcCoordinator jdbcCoordinator, JdbcServices jdbcServices) {
		this.jdbcCoordinator = jdbcCoordinator;
		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();
	}

	@Override
	public ResultSet extract(PreparedStatement statement, String sql) {
		// IMPL NOTE: SQL logged by caller
		final long executeStartNanos = beginSlowQueryLogging();
		try {
			final var eventMonitor = getEventManager();
			final var executionEvent = eventMonitor.beginJdbcPreparedStatementExecutionEvent();
			final ResultSet resultSet;
			try {
				jdbcExecuteStatementStart();
				resultSet = statement.executeQuery();
			}
			finally {
				eventMonitor.completeJdbcPreparedStatementExecutionEvent( executionEvent, sql );
				jdbcExecuteStatementEnd();
				endSlowQueryLogging(sql, executeStartNanos);
			}
			postExtract( resultSet, statement );
			return resultSet;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not extract ResultSet", sql );
		}
	}

	private EventMonitor getEventManager() {
		return jdbcCoordinator.getJdbcSessionOwner().getEventMonitor();
	}

	private JdbcEventHandler getEventHandler() {
		return jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getEventHandler();
	}

	private JdbcSessionContext context() {
		return jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext();
	}

	private void jdbcExecuteStatementEnd() {
		getEventHandler().jdbcExecuteStatementEnd();
	}

	private void jdbcExecuteStatementStart() {
		getEventHandler().jdbcExecuteStatementStart();
	}

	@Override
	public ResultSet extract(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		long executeStartNanos = beginSlowQueryLogging();
		try {
			final var eventMonitor = getEventManager();
			final var executionEvent = eventMonitor.beginJdbcPreparedStatementExecutionEvent();
			final ResultSet resultSet;
			try {
				jdbcExecuteStatementStart();
				resultSet = statement.executeQuery( sql );
			}
			finally {
				eventMonitor.completeJdbcPreparedStatementExecutionEvent( executionEvent, sql );
				jdbcExecuteStatementEnd();
				endSlowQueryLogging( sql, executeStartNanos );
			}
			postExtract( resultSet, statement );
			return resultSet;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not extract ResultSet", sql );
		}
	}

	@Override
	public ResultSet execute(PreparedStatement statement, String sql) {
		// sql logged by StatementPreparerImpl
		long executeStartNanos = beginSlowQueryLogging();
		try {
			final var eventMonitor = getEventManager();
			final var executionEvent = eventMonitor.beginJdbcPreparedStatementExecutionEvent();
			final ResultSet resultSet;
			try {
				jdbcExecuteStatementStart();
				if ( !statement.execute() ) {
					while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
						// do nothing until we hit the resultset
					}
				}
				resultSet = statement.getResultSet();
			}
			finally {
				eventMonitor.completeJdbcPreparedStatementExecutionEvent( executionEvent, sql );
				jdbcExecuteStatementEnd();
				endSlowQueryLogging( sql, executeStartNanos );
			}
			postExtract( resultSet, statement );
			return resultSet;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement", sql );
		}
	}

	@Override
	public ResultSet execute(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		long executeStartNanos = beginSlowQueryLogging();
		try {
			final var eventMonitor = getEventManager();
			final var executionEvent = eventMonitor.beginJdbcPreparedStatementExecutionEvent();
			final ResultSet resultSet;
			try {
				jdbcExecuteStatementStart();
				if ( !statement.execute( sql ) ) {
					while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
						// do nothing until we hit the resultset
					}
				}
				resultSet = statement.getResultSet();
			}
			finally {
				eventMonitor.completeJdbcPreparedStatementExecutionEvent( executionEvent, sql );
				jdbcExecuteStatementEnd();
				endSlowQueryLogging( sql, executeStartNanos );
			}
			postExtract( resultSet, statement );
			return resultSet;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement", sql );
		}
	}

	@Override
	public int executeUpdate(PreparedStatement statement, String sql) {
		assert statement != null;
		final long executeStartNanos = beginSlowQueryLogging();
		final var eventMonitor = getEventManager();
		final var executionEvent = eventMonitor.beginJdbcPreparedStatementExecutionEvent();
		try {
			jdbcExecuteStatementStart();
			return statement.executeUpdate();
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement", sql );
		}
		finally {
			eventMonitor.completeJdbcPreparedStatementExecutionEvent( executionEvent, sql );
			jdbcExecuteStatementEnd();
			endSlowQueryLogging( sql, executeStartNanos );
		}
	}

	@Override
	public int executeUpdate(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		long executeStartNanos = beginSlowQueryLogging();
		final var eventMonitor = getEventManager();
		final var executionEvent = eventMonitor.beginJdbcPreparedStatementExecutionEvent();
		try {
			jdbcExecuteStatementStart();
			return statement.executeUpdate( sql );
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement", sql );
		}
		finally {
			eventMonitor.completeJdbcPreparedStatementExecutionEvent( executionEvent, sql );
			jdbcExecuteStatementEnd();
			endSlowQueryLogging( sql, executeStartNanos );
		}
	}

	private void endSlowQueryLogging(String sql, long executeStartNanos) {
		sqlStatementLogger.logSlowQuery(sql, executeStartNanos, context() );
	}

	private long beginSlowQueryLogging() {
		return sqlStatementLogger.getLogSlowQuery() > 0 ? System.nanoTime() : 0;
	}

	private void postExtract(ResultSet resultSet, Statement statement) {
		if ( resultSet != null ) {
			jdbcCoordinator.getLogicalConnection().getResourceRegistry()
					.register( resultSet, statement );
		}
	}

}
