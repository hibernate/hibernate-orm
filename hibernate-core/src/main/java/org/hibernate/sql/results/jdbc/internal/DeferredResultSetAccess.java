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
import java.util.function.Function;

import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.NoopLimitHandler;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.query.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class DeferredResultSetAccess extends AbstractResultSetAccess {
	private static final Logger log = CoreLogging.logger( DeferredResultSetAccess.class );

	private final JdbcSelect jdbcSelect;
	private final JdbcParameterBindings jdbcParameterBindings;
	private final ExecutionContext executionContext;
	private final Function<String, PreparedStatement> statementCreator;

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

	private void executeQuery() {
		final LogicalConnectionImplementor logicalConnection = getPersistenceContext().getJdbcCoordinator().getLogicalConnection();
		final JdbcServices jdbcServices = getPersistenceContext().getFactory().getServiceRegistry().getService( JdbcServices.class );
		final QueryOptions queryOptions = executionContext.getQueryOptions();
		final String finalSql;
		final Limit limit;
		final LimitHandler limitHandler;
		if ( queryOptions == null ) {
			finalSql = jdbcSelect.getSql();
			limit = null;
			limitHandler = NoopLimitHandler.NO_LIMIT;
		}
		else {
			// Note that limit and lock aren't set for SQM as that is applied during SQL rendering
			// But for native queries, we have to adapt the SQL string
			final Dialect dialect = executionContext.getSession().getJdbcServices().getDialect();
			final String sql;
			limit = queryOptions.getLimit();
			if ( limit == null || limit.isEmpty() || jdbcSelect.usesLimitParameters() ) {
				sql = jdbcSelect.getSql();
				limitHandler = NoopLimitHandler.NO_LIMIT;
			}
			else {
				limitHandler = dialect.getLimitHandler();
				sql = limitHandler.processSql(
						jdbcSelect.getSql(),
						limit
				);
			}

			finalSql = dialect.addSqlHintOrComment(
					applyLocks( sql, queryOptions.getLockOptions() ),
					queryOptions,
					executionContext.getSession().getFactory().getSessionFactoryOptions().isCommentsEnabled()
			);
		}

		try {
			log.tracef( "Executing query to retrieve ResultSet : %s", finalSql );
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

			executionContext.getSession().getEventListenerManager().jdbcExecuteStatementStart();
			try {
				resultSet = preparedStatement.executeQuery();
			}
			finally {
				executionContext.getSession().getEventListenerManager().jdbcExecuteStatementEnd();
			}

			// For dialects that don't support an offset clause
			final int rowsToSkip;
			if ( !jdbcSelect.usesLimitParameters() && limit != null && limit.getFirstRow() != null && !limitHandler.supportsOffset() ) {
				rowsToSkip = limit.getFirstRow();
			}
			else {
				rowsToSkip = jdbcSelect.getRowsToSkip();
			}
			if ( rowsToSkip != 0 ) {
				resultSet.absolute( rowsToSkip );
			}
			logicalConnection.getResourceRegistry().register( resultSet, preparedStatement );

		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"JDBC exception executing SQL [" + finalSql + "]"
			);
		}
		finally {
			logicalConnection.afterStatement();
		}
	}

	private String applyLocks(String sql, LockOptions lockOptions) {
		if ( lockOptions != null && !lockOptions.isEmpty() ) {
			// Locks are applied during SQL rendering, but for native queries, we apply locks separately
			final LockOptions originalLockOptions = lockOptions.makeCopy();
			executionContext.getCallback().registerAfterLoadAction(
					(session, entity, persister) -> {
						( (Session) session ).buildLockRequest( originalLockOptions ).lock(
								persister.getEntityName(),
								entity
						);
					}
			);
		}
		return sql;
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
