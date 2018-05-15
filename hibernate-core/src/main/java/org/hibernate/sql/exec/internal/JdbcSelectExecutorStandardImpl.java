/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.query.internal.ScrollableResultsIterator;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.exec.spi.PreparedStatementCreator;
import org.hibernate.sql.exec.spi.RowTransformer;
import org.hibernate.sql.results.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.values.DeferredResultSetAccess;
import org.hibernate.sql.results.internal.values.JdbcValues;
import org.hibernate.sql.results.internal.values.JdbcValuesCacheHit;
import org.hibernate.sql.results.internal.values.JdbcValuesResultSetImpl;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.ResultSetAccess;
import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.sql.results.spi.RowReader;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class JdbcSelectExecutorStandardImpl implements JdbcSelectExecutor {
	// todo (6.0) : Make resolving these executors swappable - JdbcServices?
	//		Since JdbcServices is just a "composition service", this is actually
	//		a very good option...

	// todo (6.0) : where do affected-table-names get checked for up-to-date?
	//		who is responsible for that?  Here?

	/**
	 * Singleton access
	 */
	public static final JdbcSelectExecutorStandardImpl INSTANCE = new JdbcSelectExecutorStandardImpl();

	private static final Logger log = Logger.getLogger( JdbcSelectExecutorStandardImpl.class );

	@Override
	public <R> List<R> list(
			JdbcSelect jdbcSelect,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer) {
		return executeQuery(
				jdbcSelect,
				executionContext,
				rowTransformer,
				Connection::prepareStatement,
				ListResultsConsumer.instance()
		);
	}

	@Override
	public <R> ScrollableResultsImplementor<R> scroll(
			JdbcSelect jdbcSelect,
			ScrollMode scrollMode,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer) {
		return executeQuery(
				jdbcSelect,
				executionContext,
				rowTransformer,
				(connection, sql) -> connection.prepareStatement(
						sql,
						scrollMode.toResultSetType(),
						ResultSet.CONCUR_READ_ONLY,
						ResultSet.CLOSE_CURSORS_AT_COMMIT
				),
				ScrollableResultsConsumer.instance()
		);
	}

	@Override
	public <R> Stream<R> stream(
			JdbcSelect jdbcSelect,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer) {
		final ScrollableResultsImplementor<R> scrollableResults = scroll(
				jdbcSelect,
				ScrollMode.FORWARD_ONLY,
				executionContext,
				rowTransformer
		);
		final ScrollableResultsIterator<R> iterator = new ScrollableResultsIterator<>( scrollableResults );
		final Spliterator<R> spliterator = Spliterators.spliteratorUnknownSize( iterator, Spliterator.NONNULL );

		final Stream<R> stream = StreamSupport.stream( spliterator, false );
		return stream.onClose( scrollableResults::close );
	}


	private enum ExecuteAction {
		EXECUTE_QUERY,

	}

	private <T, R> T executeQuery(
			JdbcSelect jdbcSelect,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			PreparedStatementCreator statementCreator,
			ResultsConsumer<T,R> resultsConsumer) {

		final JdbcValues jdbcValues = resolveJdbcValuesSource(
				jdbcSelect,
				executionContext,
				new DeferredResultSetAccess(
						jdbcSelect,
						executionContext,
						statementCreator
				)
		);

		/*
		 * Processing options effectively are only used for entity loading.  Here we don't need these values.
		 */
		final JdbcValuesSourceProcessingOptions processingOptions = new JdbcValuesSourceProcessingOptions() {
			@Override
			public Object getEffectiveOptionalObject() {
				return null;
			}

			@Override
			public String getEffectiveOptionalEntityName() {
				return null;
			}

			@Override
			public Serializable getEffectiveOptionalId() {
				return null;
			}

			@Override
			public boolean shouldReturnProxies() {
				return true;
			}
		};

		final JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState =
				new JdbcValuesSourceProcessingStateStandardImpl( executionContext, processingOptions );

		final List<AfterLoadAction> afterLoadActions = new ArrayList<>();

		final RowReader<R> rowReader = Helper.createRowReader(
				executionContext.getSession().getSessionFactory(),
				afterLoadActions::add,
				rowTransformer,
				jdbcValues
		);

		final RowProcessingStateStandardImpl rowProcessingState = new RowProcessingStateStandardImpl(
				jdbcValuesSourceProcessingState,
				executionContext.getQueryOptions(),
				rowReader,
				jdbcValues
		);

		final T result = resultsConsumer.consume(
				jdbcValues,
				executionContext.getSession(),
				processingOptions,
				jdbcValuesSourceProcessingState,
				rowProcessingState,
				rowReader
		);

		for ( AfterLoadAction afterLoadAction : afterLoadActions ) {
			afterLoadAction.afterLoad( executionContext.getSession() );
		}


		return result;
	}

	@SuppressWarnings("unchecked")
	private JdbcValues resolveJdbcValuesSource(
			JdbcSelect jdbcSelect,
			ExecutionContext executionContext,
			ResultSetAccess resultSetAccess) {
		final List<Object[]> cachedResults;

		final boolean queryCacheEnabled = executionContext.getSession().getFactory().getSessionFactoryOptions().isQueryCacheEnabled();
		final CacheMode cacheMode = resolveCacheMode(  executionContext );

		final ResultSetMapping resultSetMapping = jdbcSelect.getResultSetMapping()
				.resolve( resultSetAccess, executionContext.getSession().getSessionFactory() );

		final QueryKey queryResultsCacheKey;

		if ( queryCacheEnabled && executionContext.getQueryOptions().getCacheMode().isGetEnabled() ) {
			log.debugf( "Reading Query result cache data per CacheMode#isGetEnabled [%s]", cacheMode.name() );

			final QueryResultsCache queryCache = executionContext.getSession().getFactory()
					.getCache()
					.getQueryResultsCache( executionContext.getQueryOptions().getResultCacheRegionName() );

			// todo (6.0) : not sure that it is at all important that we account for QueryResults
			//		these cached values are "lower level" than that, representing the
			// 		"raw" JDBC values.
			//
			// todo (6.0) : relatedly ^^, pretty sure that SqlSelections are also irrelevant

			queryResultsCacheKey = QueryKey.from(
					jdbcSelect.getSql(),
					executionContext.getQueryOptions().getLimit(),
					executionContext.getParameterBindingContext().getQueryParameterBindings(),
					executionContext.getSession()
			);

			cachedResults = queryCache.get(
					// todo (6.0) : QueryCache#get takes the `queryResultsCacheKey` see tat discussion above
					queryResultsCacheKey,
					// todo (6.0) : `querySpaces` and `session` make perfect sense as args, but its odd passing those into this method just to pass along
					//		atm we do not even collect querySpaces, but we need to
					jdbcSelect.getAffectedTableNames(),
					executionContext.getSession()
			);

			// todo (6.0) : `querySpaces` and `session` are used in QueryCache#get to verify "up-to-dateness" via UpdateTimestampsCache
			//		better imo to move UpdateTimestampsCache handling here and have QueryCache be a simple access to
			//		the underlying query result cache region.
			//
			// todo (6.0) : if we go this route (^^), still beneficial to have an abstraction over different UpdateTimestampsCache-based
			//		invalidation strategies - QueryCacheInvalidationStrategy
		}
		else {
			log.debugf( "Skipping reading Query result cache data: cache-enabled = %s, cache-mode = %s",
						queryCacheEnabled,
						cacheMode.name()
			);
			cachedResults = null;
			queryResultsCacheKey = null;
		}

		if ( cachedResults == null || cachedResults.isEmpty() ) {
			return new JdbcValuesResultSetImpl(
					resultSetAccess,
					queryResultsCacheKey,
					executionContext.getQueryOptions(),
					resultSetMapping,
					executionContext
			);
		}
		else {
			return new JdbcValuesCacheHit(
					cachedResults,
					resultSetMapping
			);
		}
	}

	private CacheMode resolveCacheMode(ExecutionContext executionContext) {
		CacheMode cacheMode = executionContext.getQueryOptions().getCacheMode();
		if ( cacheMode != null ) {
			return cacheMode;
		}

		cacheMode = executionContext.getSession().getCacheMode();
		if ( cacheMode != null ) {
			return cacheMode;
		}

		return CacheMode.NORMAL;
	}
}
