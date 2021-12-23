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

import org.hibernate.HibernateException;
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
 * JdbcValuesSource implementation for a JDBC ResultSet as the source
 *
 * @author Steve Ebersole
 */
public class JdbcValuesResultSetImpl extends AbstractJdbcValues {

	private final ResultSetAccess resultSetAccess;
	private final JdbcValuesMapping valuesMapping;
	private final ExecutionContext executionContext;

	private final SqlSelection[] sqlSelections;
	private final Object[] currentRowJdbcValues;

	public JdbcValuesResultSetImpl(
			ResultSetAccess resultSetAccess,
			QueryKey queryCacheKey,
			String queryIdentifier,
			QueryOptions queryOptions,
			JdbcValuesMapping valuesMapping,
			JdbcValuesMetadata metadataForCache,
			ExecutionContext executionContext) {
		super( resolveQueryCachePutManager( executionContext, queryOptions, queryCacheKey, queryIdentifier, metadataForCache ) );
		this.resultSetAccess = resultSetAccess;
		this.valuesMapping = valuesMapping;
		this.executionContext = executionContext;

		this.sqlSelections = valuesMapping.getSqlSelections().toArray( new SqlSelection[0] );
		this.currentRowJdbcValues = new Object[ valuesMapping.getRowSize() ];
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
		return advance(
				() -> {
					try {
						//noinspection RedundantIfStatement
						if ( ! resultSetAccess.getResultSet().next() ) {
							return false;
						}

						return true;
					}
					catch (SQLException e) {
						throw makeExecutionException( "Error advancing (next) ResultSet position", e );
					}
				}
		);
	}

	@Override
	protected boolean processPrevious(RowProcessingState rowProcessingState) {
		return advance(
				() -> {
					try {
						//noinspection RedundantIfStatement
						if ( ! resultSetAccess.getResultSet().previous() ) {
							return false;
						}
						return true;
					}
					catch (SQLException e) {
						throw makeExecutionException( "Error advancing (previous) ResultSet position", e );
					}
				}
		);
	}

	@Override
	protected boolean processScroll(int numberOfRows, RowProcessingState rowProcessingState) {
		return advance(
				() -> {
					try {
						//noinspection RedundantIfStatement
						if ( ! resultSetAccess.getResultSet().relative( numberOfRows ) ) {
							return false;
						}

						return true;
					}
					catch (SQLException e) {
						throw makeExecutionException( "Error advancing (scroll) ResultSet position", e );
					}
				}
		);
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
		return advance(
				() -> {
					try {
						//noinspection RedundantIfStatement
						if ( ! resultSetAccess.getResultSet().absolute( position ) ) {
							return false;
						}

						return true;
					}
					catch (SQLException e) {
						throw makeExecutionException( "Error advancing (scroll) ResultSet position", e );
					}
				}
		);
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
		return advance(
				() -> {
					try {
						//noinspection RedundantIfStatement
						if ( ! resultSetAccess.getResultSet().first() ) {
							return false;
						}

						return true;
					}
					catch (SQLException e) {
						throw makeExecutionException( "Error advancing (first) ResultSet position", e );
					}
				}
		);
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
		return advance(
				() -> {
					try {
						//noinspection RedundantIfStatement
						if ( ! resultSetAccess.getResultSet().last() ) {
							return false;
						}

						return true;
					}
					catch (SQLException e) {
						throw makeExecutionException( "Error advancing (last) ResultSet position", e );
					}
				}
		);
	}

	@FunctionalInterface
	private interface Advancer {
		boolean advance();
	}

	private boolean advance(Advancer advancer) {
		final boolean hasResult = advancer.advance();
		if ( ! hasResult ) {
			return false;
		}

		readCurrentRowValues();
		return true;
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

	private void readCurrentRowValues() {
		final ResultSet resultSet = resultSetAccess.getResultSet();
		final SharedSessionContractImplementor session = executionContext.getSession();
		for ( final SqlSelection sqlSelection : sqlSelections ) {
			try {
				currentRowJdbcValues[ sqlSelection.getValuesArrayPosition() ] = sqlSelection.getJdbcValueExtractor().extract(
						resultSet,
						sqlSelection.getJdbcResultSetIndex(),
						session
				);
			}
			catch (Exception e) {
				throw new HibernateException(
						"Unable to extract JDBC value for position `" + sqlSelection.getJdbcResultSetIndex() + "`",
						e
				);
			}
		}
	}

	@Override
	protected void release() {
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
}
