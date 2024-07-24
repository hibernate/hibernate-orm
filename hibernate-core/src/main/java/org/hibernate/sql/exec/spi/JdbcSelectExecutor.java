/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.sql.results.spi.ScrollableResultsConsumer;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

/**
 * An executor for JdbcSelect operations.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JdbcSelectExecutor {

	/**
	 * @since 6.4
	 */
	<T, R> T executeQuery(
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Class<R> domainResultType,
			Function<String, PreparedStatement> statementCreator,
			ResultsConsumer<T, R> resultsConsumer);

	/**
	 * @since 6.6
	 */
	default <T, R> T executeQuery(
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Class<R> domainResultType,
			int resultCountEstimate,
			ResultsConsumer<T, R> resultsConsumer) {
		return executeQuery(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				rowTransformer,
				domainResultType,
				resultCountEstimate,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareQueryStatement( sql, false, null ),
				resultsConsumer
		);
	}

	/**
	 * @since 6.6
	 */
	default <T, R> T executeQuery(
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Class<R> domainResultType,
			int resultCountEstimate,
			Function<String, PreparedStatement> statementCreator,
			ResultsConsumer<T, R> resultsConsumer) {
		return executeQuery(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				rowTransformer,
				domainResultType,
				statementCreator,
				resultsConsumer
		);
	}

	default <R> List<R> list(
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			ListResultsConsumer.UniqueSemantic uniqueSemantic) {
		return list( jdbcSelect, jdbcParameterBindings, executionContext, rowTransformer, null, uniqueSemantic );
	}

	default <R> List<R> list(
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Class<R> requestedJavaType,
			ListResultsConsumer.UniqueSemantic uniqueSemantic) {
		return list(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				rowTransformer,
				requestedJavaType,
				uniqueSemantic,
				-1
		);
	}

	/**
	 * @since 6.6
	 */
	default <R> List<R> list(
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Class<R> requestedJavaType,
			ListResultsConsumer.UniqueSemantic uniqueSemantic,
			int resultCountEstimate) {
		// Only do auto flushing for top level queries
		return executeQuery(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				rowTransformer,
				requestedJavaType,
				resultCountEstimate,
				ListResultsConsumer.instance( uniqueSemantic )
		);
	}

	default <R> ScrollableResultsImplementor<R> scroll(
			JdbcOperationQuerySelect jdbcSelect,
			ScrollMode scrollMode,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer) {
		return scroll( jdbcSelect, scrollMode, jdbcParameterBindings, executionContext, rowTransformer, -1 );
	}

	/**
	 * @since 6.6
	 */
	default <R> ScrollableResultsImplementor<R> scroll(
			JdbcOperationQuerySelect jdbcSelect,
			ScrollMode scrollMode,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			int resultCountEstimate) {
		return executeQuery(
				jdbcSelect,
				jdbcParameterBindings,
				getScrollContext( executionContext ),
				rowTransformer,
				null,
				resultCountEstimate,
				sql -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareQueryStatement( sql, false, scrollMode ),
				ScrollableResultsConsumer.instance()
		);
	}


	/*
		When `Query#scroll()` is call the query is not executed immediately, a new ExecutionContext with the values of the `persistenceContext.isDefaultReadOnly()` and of the `queryOptions.isReadOnly()`
		set at the moment of the Query#scroll() call is created in order to use it when the query will be executed.
	 */
	private ExecutionContext getScrollContext(ExecutionContext context) {
		class ScrollableExecutionContext extends BaseExecutionContext implements QueryOptions {

			private final Integer timeout;
			private final FlushMode flushMode;
			private final Boolean readOnly;
			private final AppliedGraph appliedGraph;
			private final TupleTransformer<?> tupleTransformer;
			private final ResultListTransformer<?> resultListTransformer;
			private final Boolean resultCachingEnabled;
			private final CacheRetrieveMode cacheRetrieveMode;
			private final CacheStoreMode cacheStoreMode;
			private final String resultCacheRegionName;
			private final LockOptions lockOptions;
			private final String comment;
			private final List<String> databaseHints;
			private final Integer fetchSize;
			private final Limit limit;
			private final ExecutionContext context;

			public ScrollableExecutionContext(
					Integer timeout,
					FlushMode flushMode,
					Boolean readOnly,
					AppliedGraph appliedGraph,
					TupleTransformer<?> tupleTransformer,
					ResultListTransformer<?> resultListTransformer,
					Boolean resultCachingEnabled,
					CacheRetrieveMode cacheRetrieveMode,
					CacheStoreMode cacheStoreMode,
					String resultCacheRegionName,
					LockOptions lockOptions,
					String comment,
					List<String> databaseHints,
					Integer fetchSize,
					Limit limit,
					ExecutionContext context) {
				super( context.getSession() );
				this.timeout = timeout;
				this.flushMode = flushMode;
				this.readOnly = readOnly;
				this.appliedGraph = appliedGraph;
				this.tupleTransformer = tupleTransformer;
				this.resultListTransformer = resultListTransformer;
				this.resultCachingEnabled = resultCachingEnabled;
				this.cacheRetrieveMode = cacheRetrieveMode;
				this.cacheStoreMode = cacheStoreMode;
				this.resultCacheRegionName = resultCacheRegionName;
				this.lockOptions = lockOptions;
				this.comment = comment;
				this.databaseHints = databaseHints;
				this.fetchSize = fetchSize;
				this.limit = limit;
				this.context = context;
			}

			@Override
			public boolean isScrollResult() {
				return true;
			}

			@Override
			public QueryOptions getQueryOptions() {
				return this;
			}

			@Override
			public Integer getTimeout() {
				return timeout;
			}

			@Override
			public FlushMode getFlushMode() {
				return flushMode;
			}

			@Override
			public Boolean isReadOnly() {
				return readOnly;
			}

			@Override
			public AppliedGraph getAppliedGraph() {
				return appliedGraph;
			}

			@Override
			public TupleTransformer<?> getTupleTransformer() {
				return tupleTransformer;
			}

			@Override
			public ResultListTransformer<?> getResultListTransformer() {
				return resultListTransformer;
			}

			@Override
			public Boolean isResultCachingEnabled() {
				return resultCachingEnabled;
			}

			@Override
			public Boolean getQueryPlanCachingEnabled() {
				return null;
			}

			@Override
			public CacheRetrieveMode getCacheRetrieveMode() {
				return cacheRetrieveMode;
			}

			@Override
			public CacheStoreMode getCacheStoreMode() {
				return cacheStoreMode;
			}

			@Override
			public String getResultCacheRegionName() {
				return resultCacheRegionName;
			}

			@Override
			public LockOptions getLockOptions() {
				return lockOptions;
			}

			@Override
			public String getComment() {
				return comment;
			}

			@Override
			public List<String> getDatabaseHints() {
				return databaseHints;
			}

			@Override
			public Integer getFetchSize() {
				return fetchSize;
			}

			@Override
			public Limit getLimit() {
				return limit;
			}

			@Override
			public QueryParameterBindings getQueryParameterBindings() {
				return context.getQueryParameterBindings();
			}

			@Override
			public Callback getCallback() {
				return context.getCallback();
			}

			@Override
			public boolean hasCallbackActions() {
				return context.hasCallbackActions();
			}

			@Override
			public Set<String> getEnabledFetchProfiles() {
				return null;
			}

			@Override
			public Set<String> getDisabledFetchProfiles() {
				return null;
			}
		}

		final QueryOptions queryOptions = context.getQueryOptions();
		final Boolean readOnly;
		if ( queryOptions.isReadOnly() == null ) {
			readOnly = context.getSession().getPersistenceContext().isDefaultReadOnly();
		}
		else {
			readOnly = queryOptions.isReadOnly();
		}
		final Integer timeout = queryOptions.getTimeout();
		final FlushMode flushMode = queryOptions.getFlushMode();
		final AppliedGraph appliedGraph = queryOptions.getAppliedGraph();
		final TupleTransformer<?> tupleTransformer = queryOptions.getTupleTransformer();
		final ResultListTransformer<?> resultListTransformer = queryOptions.getResultListTransformer();
		final Boolean resultCachingEnabled = queryOptions.isResultCachingEnabled();
		final CacheRetrieveMode cacheRetrieveMode = queryOptions.getCacheRetrieveMode();
		final CacheStoreMode cacheStoreMode = queryOptions.getCacheStoreMode();
		final String resultCacheRegionName = queryOptions.getResultCacheRegionName();
		final LockOptions lockOptions = queryOptions.getLockOptions();
		final String comment = queryOptions.getComment();
		final List<String> databaseHints = queryOptions.getDatabaseHints();
		final Integer fetchSize = queryOptions.getFetchSize();
		final Limit limit = queryOptions.getLimit();

		return new ScrollableExecutionContext(
				timeout,
				flushMode,
				readOnly,
				appliedGraph,
				tupleTransformer,
				resultListTransformer,
				resultCachingEnabled,
				cacheRetrieveMode,
				cacheStoreMode,
				resultCacheRegionName,
				lockOptions,
				comment,
				databaseHints,
				fetchSize,
				limit,
				context
		);
	}

}
