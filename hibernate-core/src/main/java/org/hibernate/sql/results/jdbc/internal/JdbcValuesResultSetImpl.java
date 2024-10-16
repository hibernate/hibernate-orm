/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.BitSet;

import org.hibernate.JDBCException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.dialect.OracleDialect;
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
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * {@link AbstractJdbcValues} implementation for a JDBC {@link ResultSet} as the source
 *
 * @author Steve Ebersole
 */
public class JdbcValuesResultSetImpl extends AbstractJdbcValues {

	private final QueryCachePutManager queryCachePutManager;
	private final ResultSet resultSet;
	private final ResultSetAccess resultSetAccess;
	private final JdbcValuesMapping valuesMapping;
	private final ExecutionContext executionContext;
	private final boolean usesFollowOnLocking;
	private final int resultCountEstimate;

	private final SqlSelection[] sqlSelections;
	private final BitSet initializedIndexes;
	private BitSet forceInitializedIndexes;
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
			CachedJdbcValuesMetadata metadataForCache,
			ExecutionContext executionContext) {
		this.queryCachePutManager = resolveQueryCachePutManager(
				executionContext,
				queryOptions,
				queryCacheKey,
				queryIdentifier,
				metadataForCache
		);
		this.resultSetAccess = resultSetAccess;
		this.resultSet = resultSetAccess.getResultSet();
		this.valuesMapping = valuesMapping;
		this.executionContext = executionContext;
		this.usesFollowOnLocking = usesFollowOnLocking;
		this.resultCountEstimate = determineResultCountEstimate( resultSetAccess, queryOptions, executionContext );

		final int rowSize = valuesMapping.getRowSize();
		this.sqlSelections = new SqlSelection[rowSize];
		final boolean isOracleDialect = executionContext.getSession().getJdbcServices().getDialect() instanceof OracleDialect;
		if ( isOracleDialect ) {
			// When LONG RAW and LONG type columns are not read sequentially (e.g. a following column in the ResultSet is read first)
			// Oracle raises a `ORA-17027: Stream has already been closed` exception , in order to avoid this
			// Hibernate needs force their initialization
			final ResultSetMetaData metaData = ( (AbstractResultSetAccess) resultSetAccess ).getMetaData();
			for ( SqlSelection selection : valuesMapping.getSqlSelections() ) {
				this.sqlSelections[selection.getValuesArrayPosition()] = selection;
				if ( isOracleDialect ) {
					try {
						int columnType = metaData.getColumnType( selection.getJdbcResultSetIndex() );
						if ( columnType == -4 || columnType == -1 ) {
							if ( forceInitializedIndexes == null ) {
								forceInitializedIndexes = new BitSet();
							}
							forceInitializedIndexes.set( selection.getValuesArrayPosition() );
						}
					}
					catch (SQLException e) {
						// ignore
					}
				}
			}
		}
		else {
			for ( SqlSelection selection : valuesMapping.getSqlSelections() ) {
				this.sqlSelections[selection.getValuesArrayPosition()] = selection;
			}
		}

		this.initializedIndexes = new BitSet( rowSize );
		this.currentRowJdbcValues = new Object[rowSize];
		if ( queryCachePutManager == null ) {
			this.valueIndexesToCacheIndexes = null;
			this.rowToCacheSize = -1;
		}
		else {
			this.valueIndexesToCacheIndexes = valuesMapping.getValueIndexesToCacheIndexes();
			final int rowToCacheSize = valuesMapping.getRowToCacheSize();
			assert rowToCacheSize > 0;
			int cacheIndex = rowToCacheSize;
			if ( rowToCacheSize == 1 ) {
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
			CachedJdbcValuesMetadata metadataForCache) {
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
			return resultSet.relative( numberOfRows );
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing (scroll) ResultSet position", e );
		}
	}

	@Override
	public int getPosition() {
		try {
			return resultSet.getRow() - 1;
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
			return resultSet.absolute( position );
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing (scroll) ResultSet position", e );
		}
	}

	@Override
	public boolean isBeforeFirst(RowProcessingState rowProcessingState) {
		try {
			return resultSet.isBeforeFirst();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error calling ResultSet#isBeforeFirst()", e );
		}
	}

	@Override
	public void beforeFirst(RowProcessingState rowProcessingState) {
		try {
			resultSet.beforeFirst();
			Arrays.fill( currentRowJdbcValues, null );
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error calling ResultSet#beforeFirst()", e );
		}
	}

	@Override
	public boolean isFirst(RowProcessingState rowProcessingState) {
		try {
			return resultSet.isFirst();
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
			return resultSet.isAfterLast();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error calling ResultSet#isAfterLast()", e );
		}
	}

	@Override
	public void afterLast(RowProcessingState rowProcessingState) {
		try {
			resultSet.afterLast();
			Arrays.fill( currentRowJdbcValues, null );
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error calling ResultSet#afterLast()", e );
		}
	}

	@Override
	public boolean isLast(RowProcessingState rowProcessingState) {
		try {
			return resultSet.isLast();
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
			return resultSet.next();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing (next) ResultSet position", e );
		}
	}

	private boolean advanceToLast() {
		try {
			return resultSet.last();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing (last) ResultSet position", e );
		}
	}

	private boolean advanceToFirst() {
		try {
			return resultSet.first();
		}
		catch (SQLException e) {
			throw makeExecutionException( "Error advancing (first) ResultSet position", e );
		}
	}

	private boolean advancePrevious() {
		try {
			return resultSet.previous();
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

	public void readCurrentRowValues() {
		initializedIndexes.clear();
		if ( forceInitializedIndexes != null ) {
			final ResultSet resultSet = resultSetAccess.getResultSet();
			final SharedSessionContractImplementor session = executionContext.getSession();
			for ( final SqlSelection sqlSelection : sqlSelections ) {
				try {
					final int valuesArrayPosition = sqlSelection.getValuesArrayPosition();
					if ( forceInitializedIndexes.get( valuesArrayPosition ) ) {
						currentRowJdbcValues[valuesArrayPosition] = sqlSelection.getJdbcValueExtractor().extract(
								resultSet,
								sqlSelection.getJdbcResultSetIndex(),
								session
						);
						initializedIndexes.set( valuesArrayPosition );
					}
				}
				catch (SQLException e) {
					// do not want to wrap in ExecutionException here
					throw executionContext.getSession().getJdbcServices().getSqlExceptionHelper().convert(
							e,
							"Could not extract column [" + sqlSelection.getJdbcResultSetIndex() + "] from JDBC ResultSet"
					);
				}
			}
		}

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
						resultSet,
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
			resultSet.setFetchSize( fetchSize );
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
