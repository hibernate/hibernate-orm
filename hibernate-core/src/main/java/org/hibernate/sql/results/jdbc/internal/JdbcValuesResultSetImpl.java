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

import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.caching.QueryCachePutManager;
import org.hibernate.sql.results.caching.internal.QueryCachePutManagerDisabledImpl;
import org.hibernate.sql.results.caching.internal.QueryCachePutManagerEnabledImpl;
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

	private final SqlSelection[] sqlSelections;
	private final SqlSelection[] eagerSqlSelections;
	private final BitSet initializedIndexes;
	private final Object[] currentRowJdbcValues;

	public JdbcValuesResultSetImpl(
			ResultSetAccess resultSetAccess,
			QueryKey queryCacheKey,
			String queryIdentifier,
			QueryOptions queryOptions,
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

		this.sqlSelections = valuesMapping.getSqlSelections().toArray( new SqlSelection[0] );
		this.eagerSqlSelections = extractEagerSqlSelections( sqlSelections );
		this.initializedIndexes = new BitSet( valuesMapping.getRowSize() );
		this.currentRowJdbcValues = new Object[ valuesMapping.getRowSize() ];
	}

	/**
	 * Determine the selections which are eager i.e. safe to always extract.
	 * If a virtual selection exists, we must extract the value for that JDBC position lazily.
	 */
	private SqlSelection[] extractEagerSqlSelections(SqlSelection[] sqlSelections) {
		BitSet lazyValuesPositions = null;
		for ( int i = 0; i < sqlSelections.length; i++ ) {
			final SqlSelection sqlSelection = sqlSelections[i];
			if ( sqlSelection.isVirtual() ) {
				if ( lazyValuesPositions == null ) {
					lazyValuesPositions = new BitSet();
				}
				lazyValuesPositions.set( sqlSelection.getValuesArrayPosition() );
				// Find the one preceding selection that refers to the same JDBC position
				// and treat that as virtual to do lazy extraction
				for ( int j = 0; j < i; j++ ) {
					if ( sqlSelections[j].getJdbcResultSetIndex() == sqlSelection.getJdbcResultSetIndex() ) {
						// There can only be a single selection which also has to be non-virtual
						assert !sqlSelections[j].isVirtual();
						lazyValuesPositions.set( sqlSelections[j].getValuesArrayPosition() );
						break;
					}
				}
			}
		}
		if ( lazyValuesPositions == null ) {
			return sqlSelections;
		}
		final SqlSelection[] eagerSqlSelections = new SqlSelection[sqlSelections.length - lazyValuesPositions.cardinality()];
		int i = 0;
		for ( SqlSelection sqlSelection : sqlSelections ) {
			if ( !lazyValuesPositions.get( sqlSelection.getValuesArrayPosition() ) ) {
				eagerSqlSelections[i++] = sqlSelection;
			}
		}
		return eagerSqlSelections;
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
			return QueryCachePutManagerDisabledImpl.INSTANCE;
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
		return new ExecutionException(
				message + " [" + cause.getMessage() + "]",
				executionContext.getSession().getJdbcServices().getSqlExceptionHelper().convert(
						cause,
						message
				)
		);
	}

	private void readCurrentRowValues() {
		final ResultSet resultSet = resultSetAccess.getResultSet();
		final SharedSessionContractImplementor session = executionContext.getSession();
		initializedIndexes.clear();
		for ( final SqlSelection sqlSelection : eagerSqlSelections ) {
			initializedIndexes.set( sqlSelection.getValuesArrayPosition() );
			try {
				currentRowJdbcValues[ sqlSelection.getValuesArrayPosition() ] = sqlSelection.getJdbcValueExtractor().extract(
						resultSet,
						sqlSelection.getJdbcResultSetIndex(),
						session
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
	}

	@Override
	public final void finishUp(SharedSessionContractImplementor session) {
		queryCachePutManager.finishUp( session );
		resultSetAccess.release();
	}

	@Override
	public JdbcValuesMapping getValuesMapping() {
		return valuesMapping;
	}

	@Override
	public Object[] getCurrentRowValuesArray() {
		return currentRowJdbcValues;
	}

	@Override
	public void finishRowProcessing(RowProcessingState rowProcessingState) {
		queryCachePutManager.registerJdbcRow( currentRowJdbcValues );
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
}
