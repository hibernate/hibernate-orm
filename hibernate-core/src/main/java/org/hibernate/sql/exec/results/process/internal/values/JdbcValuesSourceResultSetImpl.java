/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal.values;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.exec.results.process.internal.caching.QueryCachePutManager;
import org.hibernate.sql.exec.results.process.internal.caching.QueryCachePutManagerDisabledImpl;
import org.hibernate.sql.exec.results.process.internal.caching.QueryCachePutManagerEnabledImpl;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;

/**
 * JdbcValuesSource implementation for a JDBC ResultSet as the source
 *
 * @author Steve Ebersole
 */
public class JdbcValuesSourceResultSetImpl extends AbstractJdbcValuesSource {
	public interface ResultSetAccess {
		ResultSet getResultSet();
		void release();
	}

	private final ResultSetAccess resultSetAccess;
	private final List<SqlSelection> sqlSelections;
	private final SharedSessionContractImplementor persistenceContext;

	// todo : manage limit-based skips

	private final int numberOfRowsToProcess;

	// we start position at -1 prior to any next call so that the first next call
	//		increments position to 0, which is the first row
	private int position = -1;

	private Object[] currentRowJdbcValues;

	public JdbcValuesSourceResultSetImpl(
			ResultSetAccess resultSetAccess,
			QueryOptions queryOptions,
			List<SqlSelection> sqlSelections,
			SharedSessionContractImplementor persistenceContext) {
		super( resolveQueryCachePutManager( persistenceContext, queryOptions ) );
		this.resultSetAccess = resultSetAccess;
		this.sqlSelections = sqlSelections;
		this.persistenceContext = persistenceContext;

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
					resultSetAccess.getResultSet(),
					rowProcessingState.getJdbcValuesSourceProcessingState(),
					sqlSelections.get( i )
			);
		}
		return row;
	}

	@Override
	protected void release() {
		resultSetAccess.release();
	}

	@Override
	public Object[] getCurrentRowJdbcValues() {
		return currentRowJdbcValues;
	}
}
