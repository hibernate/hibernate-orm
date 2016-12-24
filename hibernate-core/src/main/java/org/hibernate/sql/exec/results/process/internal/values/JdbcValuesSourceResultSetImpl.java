/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal.values;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.exec.results.process.internal.caching.QueryCachePutManager;
import org.hibernate.sql.exec.results.process.internal.caching.QueryCachePutManagerDisabledImpl;
import org.hibernate.sql.exec.results.process.internal.caching.QueryCachePutManagerEnabledImpl;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;
import org.hibernate.sql.exec.spi.PreparedStatementCreator;
import org.hibernate.sql.exec.spi.PreparedStatementExecutor;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

/**
 * JdbcValuesSource implementation for a JDBC ResultSet as the source
 *
 * @author Steve Ebersole
 */
public class JdbcValuesSourceResultSetImpl extends AbstractJdbcValuesSource {
	// todo : re-purpose PreparedStatementExecutor to simply expose access to the ResultSet rather than List/ScrollableResult etc
	//		^^ that is the job of org.hibernate.sql.exec.spi.SqlTreeExecutor

	private final SharedSessionContractImplementor persistenceContext;
	private final JdbcSelect jdbcSelect;
	private final QueryOptions queryOptions;
	private final PreparedStatementCreator statementCreator;
	private final PreparedStatementExecutor preparedStatementExecutor;
	private final QueryParameterBindings queryParameterBindings;
	private final List<SqlSelection> sqlSelections;
	private final int numberOfRowsToProcess;

	private PreparedStatement preparedStatement;
	private ResultSet resultSet;

	private Object[] currentRowJdbcValues;
	private int position = -1;

	public JdbcValuesSourceResultSetImpl(
			SharedSessionContractImplementor persistenceContext,
			JdbcSelect jdbcSelect,
			QueryOptions queryOptions,
			PreparedStatementCreator statementCreator,
			PreparedStatementExecutor preparedStatementExecutor,
			QueryParameterBindings queryParameterBindings,
			List<SqlSelection> sqlSelections) {
		super( resolveQueryCachePutManager( persistenceContext, queryOptions ) );
		this.persistenceContext = persistenceContext;
		this.jdbcSelect = jdbcSelect;
		this.queryOptions = queryOptions;
		this.statementCreator = statementCreator;
		this.preparedStatementExecutor = preparedStatementExecutor;
		this.queryParameterBindings = queryParameterBindings;
		this.sqlSelections = sqlSelections;

		this.numberOfRowsToProcess = interpretNumberOfRowsToProcess( queryOptions );
	}

	private static int interpretNumberOfRowsToProcess(QueryOptions queryOptions) {
		if ( queryOptions.getLimit() == null ) {
			return -1;
		}
		final Limit limit = queryOptions.getLimit();
		if ( limit.getMaxRows() == null ) {
			return -1;
		}

		return limit.getMaxRows();
	}

	private static QueryCachePutManager resolveQueryCachePutManager(
			SharedSessionContractImplementor persistenceContext,
			QueryOptions queryOptions) {
		final boolean queryCacheEnabled = persistenceContext.getFactory().getSessionFactoryOptions().isQueryCacheEnabled();
		final CacheMode cacheMode = queryOptions.getCacheMode();

		if ( queryCacheEnabled && cacheMode.isPutEnabled() ) {
			final QueryCache queryCache = persistenceContext.getFactory()
					.getCache()
					.getQueryCache( queryOptions.getResultCacheRegionName() );

			final QueryKey queryResultsCacheKey = null;

			return new QueryCachePutManagerEnabledImpl( queryCache, queryResultsCacheKey );
		}
		else {
			return QueryCachePutManagerDisabledImpl.INSTANCE;
		}
	}

	@Override
	protected final boolean processNext(RowProcessingState rowProcessingState) {
		if ( position == 0 ) {
			return false;
		}
		else if ( position <= -1 ) {
			initializeState();
			position = -1;
		}

		if ( numberOfRowsToProcess != -1 && position > numberOfRowsToProcess ) {
			currentRowJdbcValues = null;
			return false;
		}

		position++;

		if ( numberOfRowsToProcess != -1 && position > numberOfRowsToProcess ) {
			currentRowJdbcValues = null;
			return false;
		}

		try {
			if ( !resultSet.next() ) {
				return false;
			}
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing JDBC ResultSet", e );
		}

		try {
			currentRowJdbcValues = readCurrentRowValues( rowProcessingState );
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error reading JDBC row values", e );
		}

		return true;
	}

	private ExecutionException makeExecutionException(String message, SQLException cause) {
		return new ExecutionException(
				message,
				persistenceContext.getJdbcServices().getSqlExceptionHelper().convert(
						cause,
						message
				)
		);
	}

	private Object[] readCurrentRowValues(RowProcessingState rowProcessingState) throws SQLException {
		final int numberOfSqlSelections = sqlSelections.size();
		final Object[] row = new Object[numberOfSqlSelections];
		for ( int i = 0; i < numberOfSqlSelections; i++ ) {
			row[i] = sqlSelections.get( i ).getSqlSelectable().getSqlSelectionReader().read(
					resultSet,
					rowProcessingState.getJdbcValuesSourceProcessingState(),
					sqlSelections.get( i )
			);
		}
		return row;
	}

	private void initializeState() {
		final LogicalConnectionImplementor logicalConnection = persistenceContext.getJdbcCoordinator().getLogicalConnection();
		final Connection connection = logicalConnection.getPhysicalConnection();

		final JdbcServices jdbcServices = persistenceContext.getFactory().getServiceRegistry().getService( JdbcServices.class );

		final String sql = jdbcSelect.getSql();
		try {
			jdbcServices.getSqlStatementLogger().logStatement( sql );

			// prepare the query
			preparedStatement = statementCreator.create( connection, sql );
			logicalConnection.getResourceRegistry().register( preparedStatement, true );

			// set options
			if ( queryOptions.getFetchSize() != null ) {
				preparedStatement.setFetchSize( queryOptions.getFetchSize() );
			}
			if ( queryOptions.getTimeout() != null ) {
				preparedStatement.setQueryTimeout( queryOptions.getTimeout() );
			}

			// todo : limit/offset


			// bind parameters
			// 		todo : validate that all query parameters were bound?
			int paramBindingPosition = 1;
			for ( JdbcParameterBinder parameterBinder : jdbcSelect.getParameterBinders() ) {
				paramBindingPosition += parameterBinder.bindParameterValue(
						preparedStatement,
						paramBindingPosition,
						queryParameterBindings,
						persistenceContext
				);
			}

			resultSet = preparedStatementExecutor.execute( preparedStatement, queryOptions, persistenceContext );
			logicalConnection.getResourceRegistry().register( resultSet, preparedStatement );

		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"JDBC exception executing SQL [" + sql + "]"
			);
		}
		finally {
			logicalConnection.afterStatement();
		}

	}

	@Override
	protected void release() {
		if ( resultSet != null ) {
			persistenceContext.getJdbcCoordinator()
					.getLogicalConnection()
					.getResourceRegistry()
					.release( resultSet, preparedStatement );
			resultSet = null;
		}

		if ( preparedStatement != null ) {
			persistenceContext.getJdbcCoordinator()
					.getLogicalConnection()
					.getResourceRegistry()
					.release( preparedStatement );
			preparedStatement = null;
		}
	}

	@Override
	public Object[] getCurrentRowJdbcValues() {
		return currentRowJdbcValues;
	}
}
