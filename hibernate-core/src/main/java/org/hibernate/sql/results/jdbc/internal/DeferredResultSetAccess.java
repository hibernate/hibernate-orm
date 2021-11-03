/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.function.Function;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.NoopLimitHandler;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.query.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcLockStrategy;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;

/**
 * @author Steve Ebersole
 */
public class DeferredResultSetAccess extends AbstractResultSetAccess {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger(
			DeferredResultSetAccess.class
	);

	private final JdbcSelect jdbcSelect;
	private final JdbcParameterBindings jdbcParameterBindings;
	private final ExecutionContext executionContext;
	private final Function<String, PreparedStatement> statementCreator;
	private final SqlStatementLogger sqlStatementLogger;
	private final String finalSql;
	private final Limit limit;
	private final LimitHandler limitHandler;
	private final boolean usesFollowOnLocking;

	private PreparedStatement preparedStatement;
	private ResultSet resultSet;

	public DeferredResultSetAccess(
			JdbcSelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			Function<String, PreparedStatement> statementCreator) {
		super( executionContext.getSession() );
		this.jdbcParameterBindings = jdbcParameterBindings;
		this.executionContext = executionContext;
		this.jdbcSelect = jdbcSelect;
		this.statementCreator = statementCreator;
		this.sqlStatementLogger = executionContext.getSession().getJdbcServices().getSqlStatementLogger();

		final QueryOptions queryOptions = executionContext.getQueryOptions();
		if ( queryOptions == null ) {
			finalSql = jdbcSelect.getSql();
			limit = null;
			limitHandler = NoopLimitHandler.NO_LIMIT;
			usesFollowOnLocking = false;
		}
		else {
			// Note that limit and lock aren't set for SQM as that is applied during SQL rendering
			// But for native queries, we have to adapt the SQL string
			final Dialect dialect = executionContext.getSession().getJdbcServices().getDialect();
			String sql;
			limit = queryOptions.getLimit();
			if ( limit == null || limit.isEmpty() || jdbcSelect.usesLimitParameters() ) {
				sql = jdbcSelect.getSql();
				limitHandler = NoopLimitHandler.NO_LIMIT;
			}
			else {
				limitHandler = dialect.getLimitHandler();
				sql = limitHandler.processSql(
						jdbcSelect.getSql(),
						limit,
						queryOptions
				);
			}

			final LockOptions lockOptions = queryOptions.getLockOptions();
			boolean followOnLocking = false;
			if ( lockOptions != null && !lockOptions.isEmpty() && jdbcSelect.getLockStrategy() != JdbcLockStrategy.NONE ) {
				switch ( jdbcSelect.getLockStrategy() ) {
					case FOLLOW_ON:
						followOnLocking = true;
						break;
					case AUTO:
						if ( lockOptions.getFollowOnLocking() == null && dialect.useFollowOnLocking( sql, queryOptions )
								|| Boolean.TRUE.equals( lockOptions.getFollowOnLocking() ) ) {
							followOnLocking = true;
						}
						break;
				}
				if ( followOnLocking ) {
					final LockMode lockMode = determineFollowOnLockMode( lockOptions );
					if ( lockMode != LockMode.UPGRADE_SKIPLOCKED ) {
						// Dialect prefers to perform locking in a separate step
						if ( lockOptions.getLockMode() != LockMode.NONE ) {
							LOG.usingFollowOnLocking();
						}

						final LockOptions lockOptionsToUse = new LockOptions( lockMode );
						lockOptionsToUse.setTimeOut( lockOptions.getTimeOut() );
						lockOptionsToUse.setScope( lockOptions.getScope() );

						executionContext.getCallback().registerAfterLoadAction(
								(session, entity, persister) -> {
									( (Session) session ).buildLockRequest( lockOptionsToUse ).lock(
											persister.getEntityName(),
											entity
									);
								}
						);
					}
				}
				else {
					sql = dialect.applyLocksToSql( sql, lockOptions, Collections.emptyMap() );
				}
			}
			usesFollowOnLocking = followOnLocking;
			finalSql = dialect.addSqlHintOrComment(
					sql,
					queryOptions,
					executionContext.getSession().getFactory().getSessionFactoryOptions().isCommentsEnabled()
			);
		}
	}

	@Override
	public ResultSet getResultSet() {
		if ( resultSet == null ) {
			executeQuery();
		}
		return resultSet;
	}

	@Override
	public SessionFactoryImplementor getFactory() {
		return executionContext.getSession().getFactory();
	}

	public String getFinalSql() {
		return finalSql;
	}

	public boolean usesFollowOnLocking() {
		return usesFollowOnLocking;
	}

	private void executeQuery() {
		final LogicalConnectionImplementor logicalConnection = getPersistenceContext().getJdbcCoordinator().getLogicalConnection();
		final QueryOptions queryOptions = executionContext.getQueryOptions();

		try {
			LOG.tracef( "Executing query to retrieve ResultSet : %s", finalSql );
			// prepare the query
			preparedStatement = statementCreator.apply( finalSql );

			// set options
			if ( queryOptions != null ) {
				if ( queryOptions.getFetchSize() != null ) {
					preparedStatement.setFetchSize( queryOptions.getFetchSize() );
				}
				if ( queryOptions.getTimeout() != null ) {
					preparedStatement.setQueryTimeout( queryOptions.getTimeout() );
				}
			}

			// bind parameters
			// 		todo : validate that all query parameters were bound?
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

			final SessionEventListenerManager eventListenerManager = executionContext.getSession()
					.getEventListenerManager();

			long executeStartNanos = 0;
			if ( this.sqlStatementLogger.getLogSlowQuery() > 0 ) {
				executeStartNanos = System.nanoTime();
			}
			try {
				eventListenerManager.jdbcExecuteStatementStart();
				resultSet = wrapResultSet( preparedStatement.executeQuery() );
			}
			finally {
				eventListenerManager.jdbcExecuteStatementEnd();
				sqlStatementLogger.logSlowQuery( preparedStatement, executeStartNanos );
			}

			// For dialects that don't support an offset clause
			final int rowsToSkip;
			if ( !jdbcSelect.usesLimitParameters() && limit != null && limit.getFirstRow() != null && !limitHandler.supportsLimitOffset() ) {
				rowsToSkip = limit.getFirstRow();
			}
			else {
				rowsToSkip = jdbcSelect.getRowsToSkip();
			}
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
			logicalConnection.getResourceRegistry().register( resultSet, preparedStatement );

		}
		catch (SQLException e) {
			throw executionContext.getSession().getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"JDBC exception executing SQL [" + finalSql + "]"
			);
		}
		finally {
			logicalConnection.afterStatement();
		}
	}

	protected ResultSet wrapResultSet(ResultSet resultSet) throws SQLException {
		return resultSet;
	}

	protected LockMode determineFollowOnLockMode(LockOptions lockOptions) {
		final LockMode lockModeToUse = lockOptions.findGreatestLockMode();

		if ( lockOptions.hasAliasSpecificLockModes() ) {
			if ( lockOptions.getLockMode() == LockMode.NONE && lockModeToUse == LockMode.NONE ) {
				return lockModeToUse;
			}
			else {
				LOG.aliasSpecificLockingWithFollowOnLocking( lockModeToUse );
			}
		}
		return lockModeToUse;
	}

	@Override
	public void release() {
		if ( resultSet != null ) {
			getPersistenceContext().getJdbcCoordinator()
					.getLogicalConnection()
					.getResourceRegistry()
					.release( resultSet, preparedStatement );
			resultSet = null;
		}

		if ( preparedStatement != null ) {
			getPersistenceContext().getJdbcCoordinator()
					.getLogicalConnection()
					.getResourceRegistry()
					.release( preparedStatement );
			preparedStatement = null;
		}
	}
}
