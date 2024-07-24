/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.jdbc.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.BitSet;

import org.hibernate.JDBCException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.DataException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.caching.QueryCachePutManager;
import org.hibernate.sql.results.caching.internal.QueryCachePutManagerEnabledImpl;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * {@link AbstractJdbcValues} implementation for a JDBC {@link ResultSet} as the source
 *
 * @author Steve Ebersole
 */
public class JdbcValuesResultSetImpl extends AbstractJdbcValues {

	private final QueryCachePutManager queryCachePutManager;
	private final ResultSetAccess resultSetAccess;
	private final JdbcValuesMapping valuesMapping;
	private final ExecutionContext executionContext;
	private final boolean usesFollowOnLocking;
	private final int resultCountEstimate;

	private final SqlSelection[] sqlSelections;
	private final BitSet initializedIndexes;
	private final Object[] currentRowJdbcValues;
	private final int[] valueIndexesToCacheIndexes;
	// Is only meaningful if valueIndexesToCacheIndexes is not null
	// Contains the size of the row to cache, or if the value is negative,
	// represents the inverted index of the single value to cache
	private final int rowToCacheSize;
	private int resultCount;

	public JdbcValuesResultSetImpl(
			ResultSetAccess resultSetAccess,
			QueryKey queryCacheKey,
			String queryIdentifier,
			QueryOptions queryOptions,
			boolean usesFollowOnLocking,
			JdbcValuesMapping valuesMapping,
			JdbcValuesMetadata metadataForCache,
			ExecutionContext executionContext) {
		this.queryCachePutManager = resolveQueryCachePutManager(
				executionContext,
				queryOptions,
				queryCacheKey,
				queryIdentifier,
				metadataForCache
		);
		this.resultSetAccess = resultSetAccess;
		this.valuesMapping = valuesMapping;
		this.executionContext = executionContext;
		this.usesFollowOnLocking = usesFollowOnLocking;
		this.resultCountEstimate = determineResultCountEstimate( resultSetAccess, queryOptions, executionContext );

		final int rowSize = valuesMapping.getRowSize();
		this.sqlSelections = new SqlSelection[rowSize];
		for ( SqlSelection selection : valuesMapping.getSqlSelections() ) {
			this.sqlSelections[selection.getValuesArrayPosition()] = selection;
		}
		this.initializedIndexes = new BitSet( rowSize );
		this.currentRowJdbcValues = new Object[rowSize];
		if ( queryCachePutManager == null ) {
			this.valueIndexesToCacheIndexes = null;
			this.rowToCacheSize = -1;
		}
		else {
			final BitSet valueIndexesToCache = new BitSet( rowSize );
			for ( DomainResult<?> domainResult : valuesMapping.getDomainResults() ) {
				domainResult.collectValueIndexesToCache( valueIndexesToCache );
			}
			if ( valueIndexesToCache.nextClearBit( 0 ) == -1 ) {
				this.valueIndexesToCacheIndexes = null;
				this.rowToCacheSize = -1;
			}
			else {
				final int[] valueIndexesToCacheIndexes = new int[rowSize];
				int cacheIndex = 0;
				for ( int i = 0; i < valueIndexesToCacheIndexes.length; i++ ) {
					if ( valueIndexesToCache.get( i ) ) {
						valueIndexesToCacheIndexes[i] = cacheIndex++;
					}
					else {
						valueIndexesToCacheIndexes[i] = -1;
					}
				}

				this.valueIndexesToCacheIndexes = valueIndexesToCacheIndexes;
				if ( cacheIndex == 1 ) {
					// Special case. Set the rowToCacheSize to the inverted index of the single element to cache
					for ( int i = 0; i < valueIndexesToCacheIndexes.length; i++ ) {
						if ( valueIndexesToCacheIndexes[i] != -1 ) {
							cacheIndex = -i;
							break;
						}
					}
				}
				this.rowToCacheSize = cacheIndex;
			}
		}
	}

	private int determineResultCountEstimate(
			ResultSetAccess resultSetAccess,
			QueryOptions queryOptions,
			ExecutionContext executionContext) {
		final Limit limit = queryOptions.getLimit();
		if ( limit != null && limit.getMaxRows() != null ) {
			return limit.getMaxRows();
		}

		final int resultCountEstimate = resultSetAccess.getResultCountEstimate();
		if ( resultCountEstimate > 0 ) {
			return resultCountEstimate;
		}
		return -1;
	}

	private static QueryCachePutManager resolveQueryCachePutManager(
			ExecutionContext executionContext,
			QueryOptions queryOptions,
			QueryKey queryCacheKey,
			String queryIdentifier,
			JdbcValuesMetadata metadataForCache) {
		if ( queryCacheKey != null ) {
			final SessionFactoryImplementor factory = executionContext.getSession().getFactory();
			final QueryResultsCache queryCache = factory.getCache()
					.getQueryResultsCache( queryOptions.getResultCacheRegionName() );
			return new QueryCachePutManagerEnabledImpl(
					queryCache,
					factory.getStatistics(),
					queryCacheKey,
					queryIdentifier,
					metadataForCache
			);
		}
		else {
			return null;
		}
	}

	@Override
	protected final boolean processNext(RowProcessingState rowProcessingState) {
		return advance( advanceNext() );
	}

	@Override
	protected boolean processPrevious(RowProcessingState rowProcessingState) {
		return advance( advancePrevious() );
	}

	@Override
	protected boolean processScroll(int numberOfRows, RowProcessingState rowProcessingState) {
		return advance( scrollRows( numberOfRows ) );
	}

	private boolean scrollRows(final int numberOfRows) {
		try {
			return resultSetAccess.getResultSet().relative( numberOfRows );
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing (scroll) ResultSet position", e );
		}
	}

	@Override
	public int getPosition() {
		try {
			return resultSetAccess.getResultSet().getRow() - 1;
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error calling ResultSet#getRow", e );
		}
	}

	@Override
	protected boolean processPosition(int position, RowProcessingState rowProcessingState) {
		return advance( advanceToPosition( position ) );
	}

	private boolean advanceToPosition(final int position) {
		try {
			return resultSetAccess.getResultSet().absolute( position );
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing (scroll) ResultSet position", e );
		}
	}

	@Override
	public boolean isBeforeFirst(RowProcessingState rowProcessingState) {
		try {
			return resultSetAccess.getResultSet().isBeforeFirst();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error calling ResultSet#isBeforeFirst()", e );
		}
	}

	@Override
	public void beforeFirst(RowProcessingState rowProcessingState) {
		try {
			resultSetAccess.getResultSet().beforeFirst();
			Arrays.fill( currentRowJdbcValues, null );
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error calling ResultSet#beforeFirst()", e );
		}
	}

	@Override
	public boolean isFirst(RowProcessingState rowProcessingState) {
		try {
			return resultSetAccess.getResultSet().isFirst();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error calling ResultSet#isFirst()", e );
		}
	}

	@Override
	public boolean first(RowProcessingState rowProcessingState) {
		return advance( advanceToFirst() );
	}

	@Override
	public boolean isAfterLast(RowProcessingState rowProcessingState) {
		try {
			return resultSetAccess.getResultSet().isAfterLast();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error calling ResultSet#isAfterLast()", e );
		}
	}

	@Override
	public void afterLast(RowProcessingState rowProcessingState) {
		try {
			resultSetAccess.getResultSet().afterLast();
			Arrays.fill( currentRowJdbcValues, null );
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error calling ResultSet#afterLast()", e );
		}
	}

	@Override
	public boolean isLast(RowProcessingState rowProcessingState) {
		try {
			return resultSetAccess.getResultSet().isLast();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error calling ResultSet#isLast()", e );
		}
	}

	@Override
	public boolean last(RowProcessingState rowProcessingState) {
		return advance( advanceToLast() );
	}

	private boolean advanceNext() {
		try {
			return resultSetAccess.getResultSet().next();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing (next) ResultSet position", e );
		}
	}

	private boolean advanceToLast() {
		try {
			return resultSetAccess.getResultSet().last();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing (last) ResultSet position", e );
		}
	}

	private boolean advanceToFirst() {
		try {
			return resultSetAccess.getResultSet().first();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing (first) ResultSet position", e );
		}
	}

	private boolean advancePrevious() {
		try {
			return resultSetAccess.getResultSet().previous();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing (previous) ResultSet position", e );
		}
	}

	private boolean advance(final boolean hasResult) {
		if ( ! hasResult ) {
			return false;
		}

		readCurrentRowValues();
		return true;
	}

	private ExecutionException makeExecutionException(String message, SQLException cause) {
		final JDBCException jdbcException = executionContext.getSession().getJdbcServices().getSqlExceptionHelper().convert(
				cause,
				message
		);
		if ( jdbcException instanceof QueryTimeoutException
				|| jdbcException instanceof DataException
				|| jdbcException instanceof LockTimeoutException ) {
			// So far, the exception helper threw these exceptions more or less directly during conversion,
			// so to retain the same behavior, we throw that directly now as well instead of wrapping it
			throw jdbcException;
		}
		return new ExecutionException( message + " [" + cause.getMessage() + "]", jdbcException );
	}

	private void readCurrentRowValues() {
		initializedIndexes.clear();
	}

	@Override
	public final void finishUp(SharedSessionContractImplementor session) {
		if ( queryCachePutManager != null ) {
			queryCachePutManager.finishUp( resultCount, session );
		}
		resultSetAccess.release();
	}

	@Override
	public JdbcValuesMapping getValuesMapping() {
		return valuesMapping;
	}

	@Override
	public boolean usesFollowOnLocking() {
		return usesFollowOnLocking;
	}

	@Override
	public void finishRowProcessing(RowProcessingState rowProcessingState, boolean wasAdded) {
		if ( queryCachePutManager != null ) {
			if ( wasAdded ) {
				resultCount++;
			}
			final Object objectToCache;
			if ( valueIndexesToCacheIndexes == null ) {
				objectToCache = Arrays.copyOf( currentRowJdbcValues, currentRowJdbcValues.length );
			}
			else if ( rowToCacheSize < 1 ) {
				if ( !wasAdded ) {
					// skip adding duplicate objects
					return;
				}
				objectToCache = currentRowJdbcValues[-rowToCacheSize];
			}
			else {
				final Object[] rowToCache = new Object[rowToCacheSize];
				for ( int i = 0; i < currentRowJdbcValues.length; i++ ) {
					final int cacheIndex = valueIndexesToCacheIndexes[i];
					if ( cacheIndex != -1 ) {
						rowToCache[cacheIndex] = initializedIndexes.get( i ) ? currentRowJdbcValues[i] : null;
					}
				}
				objectToCache = rowToCache;
			}
			queryCachePutManager.registerJdbcRow( objectToCache );
		}
	}

	@Override
	public Object getCurrentRowValue(int valueIndex) {
		if ( !initializedIndexes.get( valueIndex ) ) {
			initializedIndexes.set( valueIndex );
			final SqlSelection sqlSelection = sqlSelections[valueIndex];
			try {
				currentRowJdbcValues[valueIndex] = sqlSelection.getJdbcValueExtractor().extract(
						resultSetAccess.getResultSet(),
						sqlSelection.getJdbcResultSetIndex(),
						executionContext.getSession()
				);
			}
			catch ( SQLException e ) {
				// do not want to wrap in ExecutionException here
				throw executionContext.getSession().getJdbcServices().getSqlExceptionHelper().convert(
						e,
						"Could not extract column [" + sqlSelection.getJdbcResultSetIndex() + "] from JDBC ResultSet"
				);
			}
		}
		return currentRowJdbcValues[valueIndex];
	}

	@Override
	public void setFetchSize(int fetchSize) {
		try {
			resultSetAccess.getResultSet().setFetchSize( fetchSize );
		}
		catch ( SQLException e ) {
			throw makeExecutionException( "Error calling ResultSet.setFetchSize()", e );
		}
	}

	@Override
	public int getResultCountEstimate() {
		return resultCountEstimate;
	}
}
