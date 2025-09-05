/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.NoopLimitHandler;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcLockStrategy;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;

import static java.util.Collections.emptyMap;
import static org.hibernate.engine.jdbc.JdbcLogging.JDBC_MESSAGE_LOGGER;

/**
 * @author Steve Ebersole
 */
public class DeferredResultSetAccess extends AbstractResultSetAccess {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DeferredResultSetAccess.class );

	private final JdbcOperationQuerySelect jdbcSelect;
	private final JdbcParameterBindings jdbcParameterBindings;
	private final ExecutionContext executionContext;
	private final JdbcSelectExecutor.StatementCreator statementCreator;
	private final SqlStatementLogger sqlStatementLogger;
	private final String finalSql;
	private final Limit limit;
	private final LimitHandler limitHandler;
	private final boolean usesFollowOnLocking;
	private final int resultCountEstimate;

	private PreparedStatement preparedStatement;
	private ResultSet resultSet;

	public DeferredResultSetAccess(
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			JdbcSelectExecutor.StatementCreator statementCreator,
			int resultCountEstimate) {
		super( executionContext.getSession() );
		final JdbcServices jdbcServices = executionContext.getSession().getJdbcServices();

		this.jdbcParameterBindings = jdbcParameterBindings;
		this.executionContext = executionContext;
		this.jdbcSelect = jdbcSelect;
		this.statementCreator = statementCreator;
		this.sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		this.resultCountEstimate = resultCountEstimate;

		final QueryOptions queryOptions = executionContext.getQueryOptions();
		if ( queryOptions == null ) {
			finalSql = jdbcSelect.getSqlString();
			limit = null;
			limitHandler = NoopLimitHandler.NO_LIMIT;
			usesFollowOnLocking = false;
		}
		else {
			// Note that limit and lock aren't set for SQM as that is applied during SQL rendering
			// But for native queries, we have to adapt the SQL string
			final Dialect dialect = jdbcServices.getDialect();

			final String sql = jdbcSelect.getSqlString();

			limit = queryOptions.getLimit();
			final boolean needsLimitHandler = needsLimitHandler( jdbcSelect );
			limitHandler = needsLimitHandler ? dialect.getLimitHandler() : NoopLimitHandler.NO_LIMIT;
			final String sqlWithLimit = !needsLimitHandler ? sql : limitHandler.processSql(
					sql,
					jdbcParameterBindings.getBindings().size(),
					jdbcServices.getParameterMarkerStrategy(),
					queryOptions
			);

			final LockOptions lockOptions = queryOptions.getLockOptions();
			final JdbcLockStrategy jdbcLockStrategy = jdbcSelect.getLockStrategy();
			final String sqlWithLocking;
			if ( hasLocking( jdbcLockStrategy, lockOptions ) ) {
				usesFollowOnLocking = useFollowOnLocking( jdbcLockStrategy, sqlWithLimit, queryOptions, lockOptions, dialect );
				if ( usesFollowOnLocking ) {
					handleFollowOnLocking( executionContext, lockOptions );
					sqlWithLocking = sqlWithLimit;
				}
				else {
					sqlWithLocking = dialect.applyLocksToSql( sqlWithLimit, lockOptions, emptyMap() );
				}
			}
			else {
				usesFollowOnLocking = false;
				sqlWithLocking = sqlWithLimit;
			}

			final boolean commentsEnabled =
					executionContext.getSession().getFactory()
							.getSessionFactoryOptions().isCommentsEnabled();
			finalSql = dialect.addSqlHintOrComment( sqlWithLocking, queryOptions, commentsEnabled );
		}
	}

	private boolean needsLimitHandler(JdbcOperationQuerySelect jdbcSelect) {
		return limit != null && !limit.isEmpty() && !jdbcSelect.usesLimitParameters();
	}

	private static boolean hasLocking(JdbcLockStrategy jdbcLockStrategy, LockOptions lockOptions) {
		return jdbcLockStrategy != JdbcLockStrategy.NONE && lockOptions != null && !lockOptions.isEmpty();
	}

	private void handleFollowOnLocking(ExecutionContext executionContext, LockOptions lockOptions) {
		final LockMode lockMode = determineFollowOnLockMode( lockOptions );
		if ( lockMode != LockMode.UPGRADE_SKIPLOCKED ) {
			if ( lockOptions.getLockMode() != LockMode.NONE ) {
				LOG.usingFollowOnLocking();
			}

			final LockOptions lockOptionsToUse = new LockOptions(
					lockMode,
					lockOptions.getTimeOut(),
					lockOptions.getScope(),
					Locking.FollowOn.ALLOW
			);

			registerAfterLoadAction( executionContext, lockOptionsToUse );
		}
	}

	/**
	 * For Hibernate Reactive
	 */
	protected void registerAfterLoadAction(ExecutionContext executionContext, LockOptions lockOptionsToUse) {
		executionContext.getCallback()
				.registerAfterLoadAction( (entity, persister, session) ->
						session.lock( persister.getEntityName(), entity, lockOptionsToUse ) );
	}

	private static boolean useFollowOnLocking(
			JdbcLockStrategy jdbcLockStrategy,
			String sql,
			QueryOptions queryOptions,
			LockOptions lockOptions,
			Dialect dialect) {
		assert lockOptions != null;
		return switch ( jdbcLockStrategy ) {
			case FOLLOW_ON -> true;
			case AUTO -> interpretAutoLockStrategy( sql, queryOptions, lockOptions, dialect);
			case NONE -> false;
		};
	}

	private static boolean interpretAutoLockStrategy(
			String sql,
			QueryOptions queryOptions,
			LockOptions lockOptions,
			Dialect dialect) {
		return switch ( lockOptions.getFollowOnStrategy() ) {
			case ALLOW -> dialect.useFollowOnLocking( sql, queryOptions );
			case FORCE -> true;
			case DISALLOW, IGNORE -> false;
		};
	}

	public LimitHandler getLimitHandler() {
		return limitHandler;
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

	public boolean usesFollowOnLocking() {
		return usesFollowOnLocking;
	}

	protected void bindParameters(PreparedStatement preparedStatement) throws SQLException {
		setQueryOptions( preparedStatement );

		// bind parameters
		// todo : validate that all query parameters were bound?
		int paramBindingPosition = 1;
		paramBindingPosition += limitHandler.bindLimitParametersAtStartOfQuery( limit, preparedStatement, paramBindingPosition );
		for ( JdbcParameterBinder parameterBinder : jdbcSelect.getParameterBinders() ) {
			parameterBinder.bindParameterValue(
					preparedStatement,
					paramBindingPosition++,
					jdbcParameterBindings,
					executionContext
			);
		}
		paramBindingPosition += limitHandler.bindLimitParametersAtEndOfQuery( limit, preparedStatement, paramBindingPosition );

		if ( !jdbcSelect.usesLimitParameters() && limit != null && limit.getMaxRows() != null ) {
			limitHandler.setMaxRows( limit, preparedStatement );
		}
		else {
			final int maxRows = jdbcSelect.getMaxRows();
			if ( maxRows != Integer.MAX_VALUE ) {
				preparedStatement.setMaxRows( maxRows );
			}
		}
	}

	private void setQueryOptions(PreparedStatement preparedStatement) throws SQLException {
		final QueryOptions queryOptions = executionContext.getQueryOptions();
		// set options
		if ( queryOptions != null ) {
			final Integer fetchSize = queryOptions.getFetchSize();
			if ( fetchSize != null ) {
				JDBC_MESSAGE_LOGGER.settingFetchSize( fetchSize );
				preparedStatement.setFetchSize( fetchSize );
			}
			final Integer timeout = queryOptions.getTimeout();
			if ( timeout != null ) {
				JDBC_MESSAGE_LOGGER.settingQueryTimeout( timeout );
				preparedStatement.setQueryTimeout( timeout );
			}
		}
	}

	private void executeQuery() {
		final LogicalConnectionImplementor logicalConnection =
				getPersistenceContext().getJdbcCoordinator().getLogicalConnection();

		final SharedSessionContractImplementor session = executionContext.getSession();
		try {
			LOG.tracef( "Executing query to retrieve ResultSet: %s", finalSql );
			// prepare the query
			preparedStatement = statementCreator.createStatement( executionContext, finalSql );

			bindParameters( preparedStatement );

			final SessionEventListenerManager eventListenerManager = session.getEventListenerManager();
			long executeStartNanos = 0;
			if ( sqlStatementLogger.getLogSlowQuery() > 0 ) {
				executeStartNanos = System.nanoTime();
			}
			final EventMonitor eventMonitor = session.getEventMonitor();
			final DiagnosticEvent jdbcPreparedStatementExecutionEvent =
					eventMonitor.beginJdbcPreparedStatementExecutionEvent();
			try {
				eventListenerManager.jdbcExecuteStatementStart();
				resultSet = wrapResultSet( preparedStatement.executeQuery() );
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
		return !jdbcSelect.usesLimitParameters()
			&& limit != null && limit.getFirstRow() != null
			&& !limitHandler.supportsLimitOffset()
				? limit.getFirstRow()
				: jdbcSelect.getRowsToSkip();
	}

	protected ResultSet wrapResultSet(ResultSet resultSet) throws SQLException {
		return resultSet;
	}

	protected LockMode determineFollowOnLockMode(LockOptions lockOptions) {
		return lockOptions.getLockMode();
	}

	@Override
	public void release() {
		final JdbcCoordinator jdbcCoordinator =
				getPersistenceContext().getJdbcCoordinator();
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
