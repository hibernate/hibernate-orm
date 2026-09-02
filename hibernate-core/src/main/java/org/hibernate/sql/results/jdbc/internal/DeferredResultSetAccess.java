/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import jakarta.persistence.Timeout;
import org.hibernate.Timeouts;
import org.hibernate.dialect.pagination.spi.NoopLimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationJdbcInstructions;
import org.hibernate.dialect.pagination.spi.PaginationRequest;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.spi.Limit;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcPaginationApplication;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_LOGGER;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard.isStandardRenderer;

/**
 * @author Steve Ebersole
 */
public class DeferredResultSetAccess extends AbstractResultSetAccess {

	private final JdbcSelect jdbcSelect;
	private final JdbcParameterBindings jdbcParameterBindings;
	private final ExecutionContext executionContext;
	private final JdbcSelectExecutor.StatementCreator statementCreator;
	private final SqlStatementLogger sqlStatementLogger;
	private final String finalSql;
	private final JdbcLockingApplication lockingApplication;
	private final Limit limit;
	private final PaginationJdbcInstructions paginationInstructions;
	private final int resultCountEstimate;

	private PreparedStatement preparedStatement;
	private ResultSet resultSet;

	public DeferredResultSetAccess(
			JdbcSelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			JdbcSelectExecutor.StatementCreator statementCreator,
			int resultCountEstimate) {
		super( executionContext.getSession() );
		final var jdbcServices = executionContext.getSession().getJdbcServices();

		this.jdbcParameterBindings = jdbcParameterBindings;
		this.executionContext = executionContext;
		this.jdbcSelect = jdbcSelect;
		this.statementCreator = statementCreator;
		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.resultCountEstimate = resultCountEstimate;

		final var queryOptions = executionContext.getQueryOptions();
		if ( queryOptions == null ) {
			finalSql = jdbcSelect.getSqlString();
			lockingApplication = JdbcLockingApplication.NONE;
			limit = null;
			paginationInstructions = new PaginationJdbcInstructions(
					java.util.List.of(),
					java.util.List.of(),
					jdbcSelect.getMaxRows() == Integer.MAX_VALUE ? null : jdbcSelect.getMaxRows(),
					jdbcSelect.getRowsToSkip()
			);
		}
		else {
			// Note that limit and lock aren't set for SQM as that is applied during SQL rendering
			// But for native queries, we have to adapt the SQL string
			final var dialect = jdbcServices.getDialect();

			limit = queryOptions.getLimit();
			final var parameterMarkerStrategy = jdbcServices.getParameterMarkerStrategy();
			final var paginationRequest = new PaginationRequest(
					jdbcSelect.getSqlString(),
					limit.getFirstRow(),
					limit.getMaxRows(),
					jdbcParameterBindings.getBindings().size(),
					isStandardRenderer( parameterMarkerStrategy ) ? null : parameterMarkerStrategy
			);
			final var limitHandler = jdbcSelect.getPaginationApplication() == JdbcPaginationApplication.RAW_SQL
					&& !paginationRequest.isEmpty()
					? dialect.getLimitHandler()
					: NoopLimitHandler.INSTANCE;
			final JdbcSelectSqlFinalizer.FinalizedResult result = JdbcSelectSqlFinalizer.finalizeSql(
					jdbcSelect.getSqlString(),
					jdbcSelect.getPaginationApplication(),
					jdbcSelect.getRowsToSkip(),
					jdbcSelect.getMaxRows(),
					paginationRequest,
					limitHandler,
					jdbcSelect.getLockingApplication(),
					queryOptions.getLockOptions(),
					limit,
					dialect.getLockingSupport(),
					jdbcSelect.getLoadedValuesCollectorFactory() != null,
					dialect,
					queryOptions,
					executionContext.getSession().getFactory()
							.getSessionFactoryOptions().isCommentsEnabled()
			);
			finalSql = result.sql();
			lockingApplication = result.lockingApplication();
			paginationInstructions = result.paginationInstructions();
		}
	}

	public Limit getLimit() {
		return limit;
	}

	@Override
	public ResultSet getResultSet() {
		if ( resultSet == null ) {
			executeQuery();
		}
		return resultSet;
	}

	@Override
	protected SessionFactoryImplementor getFactory() {
		return executionContext.getSession().getFactory();
	}

	public String getFinalSql() {
		return finalSql;
	}

	public JdbcLockingApplication getLockingApplication() {
		return lockingApplication;
	}

	protected void bindParameters(PreparedStatement preparedStatement) throws SQLException {
		setQueryOptions( preparedStatement );

		// bind parameters
		// todo : validate that all query parameters were bound?
		int paramBindingPosition = 1;
		for ( int value : paginationInstructions.parametersAtStart() ) {
			preparedStatement.setInt( paramBindingPosition++, value );
		}
		for ( var parameterBinder : jdbcSelect.getParameterBinders() ) {
			parameterBinder.bindParameterValue(
					preparedStatement,
					paramBindingPosition++,
					jdbcParameterBindings,
					executionContext
			);
		}
		for ( int value : paginationInstructions.parametersAtEnd() ) {
			preparedStatement.setInt( paramBindingPosition++, value );
		}

		if ( paginationInstructions.maxRows() != null ) {
			preparedStatement.setMaxRows( paginationInstructions.maxRows() );
		}
	}

	private void setQueryOptions(PreparedStatement preparedStatement) throws SQLException {
		final var queryOptions = executionContext.getQueryOptions();
		// set options
		if ( queryOptions != null ) {
			final Integer fetchSize = queryOptions.getFetchSize();
			if ( fetchSize != null ) {
				JDBC_LOGGER.settingFetchSize( fetchSize );
				preparedStatement.setFetchSize( fetchSize );
			}
			final Timeout timeout = queryOptions.getTimeout();
			if ( Timeouts.isRealTimeout( timeout ) ) {
				// JDBC expects timeout in seconds
				final int timeoutInSeconds = Timeouts.getTimeoutInSeconds( timeout );
				JDBC_LOGGER.settingQueryTimeout( timeoutInSeconds );
				preparedStatement.setQueryTimeout( timeoutInSeconds );
			}
		}
	}

	private void executeQuery() {
		final var logicalConnection =
				getPersistenceContext().getJdbcCoordinator().getLogicalConnection();

		final var session = executionContext.getSession();
		try {
			CORE_LOGGER.tracef( "Executing query to retrieve ResultSet: %s", finalSql );
			// prepare the query
			preparedStatement = statementCreator.createStatement( executionContext, finalSql );

			bindParameters( preparedStatement );

			final var eventListenerManager = session.getEventListenerManager();
			long executeStartNanos = 0;
			if ( sqlStatementLogger.getLogSlowQuery() > 0 ) {
				executeStartNanos = System.nanoTime();
			}
			final var eventMonitor = session.getEventMonitor();
			final var jdbcPreparedStatementExecutionEvent =
					eventMonitor.beginJdbcPreparedStatementExecutionEvent();
			try {
				eventListenerManager.jdbcExecuteStatementStart();
				resultSet = wrapResultSet( preparedStatement.executeQuery() );
			}
			catch (SQLException exception) {
				session.getJdbcCoordinator().afterFailedStatementExecution( exception );
				throw exception;
			}
			finally {
				eventMonitor.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, finalSql );
				eventListenerManager.jdbcExecuteStatementEnd();
				sqlStatementLogger.logSlowQuery( finalSql, executeStartNanos, context() );
			}

			skipRows( resultSet );
			logicalConnection.getResourceRegistry().register( resultSet, preparedStatement );
		}
		catch (SQLException exception) {
			try {
				release();
			}
			catch (RuntimeException suppressed) {
				exception.addSuppressed( suppressed );
			}
			throw session.getJdbcServices().getSqlExceptionHelper()
					.convert( exception, "JDBC exception executing SQL", finalSql );
		}
	}

	private JdbcSessionContext context() {
		return executionContext.getSession().getJdbcCoordinator().getJdbcSessionOwner().getJdbcSessionContext();
	}

	protected void skipRows(ResultSet resultSet) throws SQLException {
		// For dialects that don't support an offset clause
		final int rowsToSkip = getRowsToSkip();
		if ( rowsToSkip != 0 ) {
			try {
				resultSet.absolute( rowsToSkip );
			}
			catch (SQLException ex) {
				// This could happen with the jTDS driver which throws an exception on non-scrollable result sets
				// To avoid throwing a wrong exception in case this was some other error, check if we can advance to next
				try {
					resultSet.next();
				}
				catch (SQLException ex2) {
					throw ex;
				}
				// Traverse to the actual row
				for (int i = 1; i < rowsToSkip && resultSet.next(); i++) {}
			}
		}
	}

	private int getRowsToSkip() {
		return paginationInstructions.rowsToSkip();
	}

	protected ResultSet wrapResultSet(ResultSet resultSet) throws SQLException {
		return resultSet;
	}

	@Override
	public void release() {
		final var jdbcCoordinator = getPersistenceContext().getJdbcCoordinator();
		final LogicalConnectionImplementor logicalConnection = jdbcCoordinator.getLogicalConnection();
		if ( resultSet != null ) {
			logicalConnection.getResourceRegistry().release( resultSet, preparedStatement );
			resultSet = null;
		}

		if ( preparedStatement != null ) {
			logicalConnection.getResourceRegistry().release( preparedStatement );
			preparedStatement = null;
			jdbcCoordinator.afterStatementExecution();
		}
	}

	@Override
	public int getResultCountEstimate() {
		if ( limit != null && limit.getMaxRows() != null ) {
			return limit.getMaxRows();
		}
		else if ( jdbcSelect.getLimitParameter() != null ) {
			return (int) jdbcParameterBindings.getBinding( jdbcSelect.getLimitParameter() ).getBindValue();
		}
		else if ( resultCountEstimate > 0 ) {
			return resultCountEstimate;
		}
		else {
			return super.getResultCountEstimate();
		}
	}
}
