/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateEvent;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;

/**
 * Standard implementation of the ResultSetReturn contract
 *
 * @author Brett Meyer
 */
public class ResultSetReturnImpl implements ResultSetReturn {
	private final JdbcCoordinator jdbcCoordinator;

	private final Dialect dialect;
	private final SqlStatementLogger sqlStatementLogger;
	private final SqlExceptionHelper sqlExceptionHelper;

	/**
	 * Constructs a ResultSetReturnImpl
	 *
	 * @param jdbcCoordinator The JdbcCoordinator
	 */
	public ResultSetReturnImpl(JdbcCoordinator jdbcCoordinator, JdbcServices jdbcServices) {
		this.jdbcCoordinator = jdbcCoordinator;
		this.dialect = jdbcServices.getDialect();
		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();
	}

	@Override
	public ResultSet extract(PreparedStatement statement, String sql) {
		// IMPL NOTE : SQL logged by caller
		long executeStartNanos = 0;
		if ( this.sqlStatementLogger.getLogSlowQuery() > 0 ) {
			executeStartNanos = System.nanoTime();
		}
		try {
			final ResultSet rs;
			final EventManager eventManager = jdbcCoordinator.getJdbcSessionOwner().getEventManager();
			final HibernateEvent jdbcPreparedStatementExecutionEvent = eventManager.beginJdbcPreparedStatementExecutionEvent();
			try {
				jdbcExecuteStatementStart();
				rs = statement.executeQuery();
			}
			finally {
				eventManager.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, sql );
				jdbcExecuteStatementEnd();
				sqlStatementLogger.logSlowQuery( sql, executeStartNanos, context() );
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not extract ResultSet", sql );
		}
	}

	private JdbcSessionContext context() {
		return jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext();
	}

	private void jdbcExecuteStatementEnd() {
		jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getObserver().jdbcExecuteStatementEnd();
	}

	private void jdbcExecuteStatementStart() {
		jdbcCoordinator.getJdbcSessionOwner().getJdbcSessionContext().getObserver().jdbcExecuteStatementStart();
	}

	@Override
	public ResultSet extract(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		long executeStartNanos = 0;
		if ( this.sqlStatementLogger.getLogSlowQuery() > 0 ) {
			executeStartNanos = System.nanoTime();
		}
		try {
			final ResultSet rs;
			final EventManager eventManager = jdbcCoordinator.getJdbcSessionOwner().getEventManager();
			final HibernateEvent jdbcPreparedStatementExecutionEvent = eventManager.beginJdbcPreparedStatementExecutionEvent();
			try {
				jdbcExecuteStatementStart();
				rs = statement.executeQuery( sql );
			}
			finally {
				eventManager.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, sql );
				jdbcExecuteStatementEnd();
				sqlStatementLogger.logSlowQuery( sql, executeStartNanos, context() );
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not extract ResultSet", sql );
		}
	}

	@Override
	public ResultSet execute(PreparedStatement statement, String sql) {
		// sql logged by StatementPreparerImpl
		long executeStartNanos = 0;
		if ( this.sqlStatementLogger.getLogSlowQuery() > 0 ) {
			executeStartNanos = System.nanoTime();
		}
		try {
			final ResultSet rs;
			final EventManager eventManager = jdbcCoordinator.getJdbcSessionOwner().getEventManager();
			final HibernateEvent jdbcPreparedStatementExecutionEvent = eventManager.beginJdbcPreparedStatementExecutionEvent();
			try {
				jdbcExecuteStatementStart();
				if ( !statement.execute() ) {
					while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
						// do nothing until we hit the resultset
					}
				}
				rs = statement.getResultSet();
			}
			finally {
				eventManager.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, sql );
				jdbcExecuteStatementEnd();
				sqlStatementLogger.logSlowQuery( sql, executeStartNanos, context() );
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement", sql );
		}
	}

	@Override
	public ResultSet execute(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		long executeStartNanos = 0;
		if ( this.sqlStatementLogger.getLogSlowQuery() > 0 ) {
			executeStartNanos = System.nanoTime();
		}
		try {
			final ResultSet rs;
			final EventManager eventManager = jdbcCoordinator.getJdbcSessionOwner().getEventManager();
			final HibernateEvent jdbcPreparedStatementExecutionEvent = eventManager.beginJdbcPreparedStatementExecutionEvent();
			try {
				jdbcExecuteStatementStart();
				if ( !statement.execute( sql ) ) {
					while ( !statement.getMoreResults() && statement.getUpdateCount() != -1 ) {
						// do nothing until we hit the resultset
					}
				}
				rs = statement.getResultSet();
			}
			finally {
				eventManager.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, sql );
				jdbcExecuteStatementEnd();
				sqlStatementLogger.logSlowQuery( sql, executeStartNanos, context() );
			}
			postExtract( rs, statement );
			return rs;
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement", sql );
		}
	}

	@Override
	public int executeUpdate(PreparedStatement statement, String sql) {
		assert statement != null;

		long executeStartNanos = 0;
		if ( this.sqlStatementLogger.getLogSlowQuery() > 0 ) {
			executeStartNanos = System.nanoTime();
		}
		final EventManager eventManager = jdbcCoordinator.getJdbcSessionOwner().getEventManager();
		final HibernateEvent jdbcPreparedStatementExecutionEvent = eventManager.beginJdbcPreparedStatementExecutionEvent();
		try {
			jdbcExecuteStatementStart();
			return statement.executeUpdate();
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement", sql );
		}
		finally {
			eventManager.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, sql );
			jdbcExecuteStatementEnd();
			sqlStatementLogger.logSlowQuery( sql, executeStartNanos, context() );
		}
	}

	@Override
	public int executeUpdate(Statement statement, String sql) {
		sqlStatementLogger.logStatement( sql );
		long executeStartNanos = 0;
		if ( this.sqlStatementLogger.getLogSlowQuery() > 0 ) {
			executeStartNanos = System.nanoTime();
		}
		final EventManager eventManager = jdbcCoordinator.getJdbcSessionOwner().getEventManager();
		final HibernateEvent jdbcPreparedStatementExecutionEvent = eventManager.beginJdbcPreparedStatementExecutionEvent();
		try {
			jdbcExecuteStatementStart();
			return statement.executeUpdate( sql );
		}
		catch (SQLException e) {
			throw sqlExceptionHelper.convert( e, "could not execute statement", sql );
		}
		finally {
			eventManager.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, sql );
			jdbcExecuteStatementEnd();
			sqlStatementLogger.logSlowQuery( sql, executeStartNanos, context() );
		}
	}

	private void postExtract(ResultSet rs, Statement st) {
		if ( rs != null ) {
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().register( rs, st );
		}
	}

}
