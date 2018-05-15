/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.values;

import java.sql.SQLException;

import org.hibernate.CacheMode;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.query.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.internal.caching.QueryCachePutManager;
import org.hibernate.sql.results.internal.caching.QueryCachePutManagerDisabledImpl;
import org.hibernate.sql.results.internal.caching.QueryCachePutManagerEnabledImpl;
import org.hibernate.sql.results.spi.ResultSetAccess;
import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * JdbcValuesSource implementation for a JDBC ResultSet as the source
 *
 * @author Steve Ebersole
 */
public class JdbcValuesResultSetImpl extends AbstractJdbcValues {

	private final ResultSetAccess resultSetAccess;
	private final ResultSetMapping resultSetMapping;
	private final ExecutionContext executionContext;

	// todo (6.0) - manage limit-based skips

	private final int numberOfRowsToProcess;

	// we start position at -1 prior to any next call so that the first next call
	//		increments position to 0, which is the first row
	private int position = -1;

	private Object[] currentRowJdbcValues;

	public JdbcValuesResultSetImpl(
			ResultSetAccess resultSetAccess,
			QueryKey queryCacheKey,
			QueryOptions queryOptions,
			ResultSetMapping resultSetMapping,
			ExecutionContext executionContext) {
		super(
				resolveQueryCachePutManager( executionContext, queryOptions, queryCacheKey )
		);
		this.resultSetAccess = resultSetAccess;
		this.resultSetMapping = resultSetMapping;
		this.executionContext = executionContext;

		// todo (6.0) : decide how to handle paged/limited results
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
			ExecutionContext executionContext,
			QueryOptions queryOptions,
			QueryKey queryCacheKey) {
		final boolean queryCacheEnabled = executionContext.getSession()
				.getFactory()
				.getSessionFactoryOptions()
				.isQueryCacheEnabled();
		final CacheMode cacheMode = queryOptions.getCacheMode();

		if ( queryCacheEnabled && cacheMode.isPutEnabled() ) {
			final QueryResultsCache queryCache = executionContext.getSession().getFactory()
					.getCache()
					.getQueryResultsCache( queryOptions.getResultCacheRegionName() );

			return new QueryCachePutManagerEnabledImpl( queryCache, queryCacheKey );
		}
		else {
			return QueryCachePutManagerDisabledImpl.INSTANCE;
		}
	}

	@Override
	protected final boolean processNext(RowProcessingState rowProcessingState) {
		currentRowJdbcValues = null;

		if ( numberOfRowsToProcess != -1 && position > numberOfRowsToProcess ) {
			// numberOfRowsToProcess != -1 means we had some limit, and
			//		position > numberOfRowsToProcess means we have exceeded the
			// 		number of limited rows
			return false;
		}

		position++;

		try {
			if ( !resultSetAccess.getResultSet().next() ) {
				return false;
			}
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing JDBC ResultSet", e );
		}

		try {
			currentRowJdbcValues = readCurrentRowValues( rowProcessingState );
			return true;
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error reading JDBC row values", e );
		}
	}

	private ExecutionException makeExecutionException(String message, SQLException cause) {
		return new ExecutionException(
				message,
				executionContext.getSession().getJdbcServices().getSqlExceptionHelper().convert(
						cause,
						message
				)
		);
	}

	private Object[] readCurrentRowValues(RowProcessingState rowProcessingState) throws SQLException {
		final int numberOfSqlSelections = resultSetMapping.getSqlSelections().size();
		final Object[] row = new Object[numberOfSqlSelections];
		for ( SqlSelection sqlSelection : resultSetMapping.getSqlSelections() ) {
			row[ sqlSelection.getValuesArrayPosition() ] = sqlSelection.getJdbcValueExtractor().extract(
					resultSetAccess.getResultSet(),
					sqlSelection.getJdbcResultSetIndex(),
					executionContext
			);
		}
		return row;
	}

	@Override
	protected void release() {
		resultSetAccess.release();
	}

	@Override
	public ResultSetMapping getResultSetMapping() {
		return resultSetMapping;
	}

	@Override
	public Object[] getCurrentRowValuesArray() {
		return currentRowJdbcValues;
	}
}
