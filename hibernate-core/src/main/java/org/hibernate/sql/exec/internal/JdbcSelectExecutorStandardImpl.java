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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.internal.ScrollableResultsIterator;
import org.hibernate.query.spi.ExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.spi.Callback;
import org.hibernate.sql.exec.results.process.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.exec.results.process.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.exec.results.process.internal.RowReaderStandardImpl;
import org.hibernate.sql.exec.results.process.internal.values.DeferredResultSetAccess;
import org.hibernate.sql.exec.results.process.internal.values.JdbcValuesSource;
import org.hibernate.sql.exec.results.process.internal.values.JdbcValuesSourceCacheHit;
import org.hibernate.sql.exec.results.process.internal.values.JdbcValuesSourceResultSetImpl;
import org.hibernate.sql.exec.results.process.internal.values.JdbcValuesSourceResultSetImpl.ResultSetAccess;
import org.hibernate.sql.exec.results.process.spi.Initializer;
import org.hibernate.sql.exec.results.process.spi.InitializerSource;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;
import org.hibernate.sql.exec.results.process.spi.RowReader;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.exec.spi.PreparedStatementCreator;
import org.hibernate.sql.exec.spi.RowTransformer;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class JdbcSelectExecutorStandardImpl implements JdbcSelectExecutor {
	private static final Logger log = Logger.getLogger( JdbcSelectExecutorStandardImpl.class );

	@Override
	public <R> List<R> list(
			JdbcSelect jdbcSelect,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<R> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor persistenceContext,
			ExecutionContext executionContext) {
		return executeQuery(
				jdbcSelect,
				queryOptions,
				queryParameterBindings,
				rowTransformer,
				persistenceContext,
				callback,
				executionContext,
				Connection::prepareStatement,
				ListResultsConsumer.instance()
		);
	}

	@Override
	public <R> ScrollableResultsImplementor<R> scroll(
			JdbcSelect jdbcSelect,
			ScrollMode scrollMode,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<R> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor persistenceContext,
			ExecutionContext executionContext) {
		return executeQuery(
				jdbcSelect,
				queryOptions,
				queryParameterBindings,
				rowTransformer,
				persistenceContext,
				callback,
				executionContext,
				(connection, sql) -> connection.prepareStatement(
						sql,
						scrollMode.toResultSetType(),
						ResultSet.CONCUR_READ_ONLY,
						ResultSet.CLOSE_CURSORS_AT_COMMIT
				),

				null
		);
	}

	@Override
	public <R> Stream<R> stream(
			JdbcSelect jdbcSelect,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<R> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor persistenceContext,
			ExecutionContext executionContext) {
		final ScrollableResultsImplementor<R> scrollableResults = scroll(
				jdbcSelect,
				ScrollMode.FORWARD_ONLY,
				queryOptions,
				queryParameterBindings,
				rowTransformer,
				callback,
				persistenceContext,
				executionContext
		);
		final ScrollableResultsIterator<R> iterator = new ScrollableResultsIterator<>( scrollableResults );
		final Spliterator<R> spliterator = Spliterators.spliteratorUnknownSize( iterator, Spliterator.NONNULL );

		final Stream<R> stream = StreamSupport.stream( spliterator, false );
		return stream.onClose( scrollableResults::close );
	}



	private interface ResultsConsumer<T,R> {
		T consume(
				JdbcValuesSource jdbcValuesSource,
				SharedSessionContractImplementor persistenceContext,
				JdbcValuesSourceProcessingOptions processingOptions,
				JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState,
				RowProcessingStateStandardImpl rowProcessingState,
				RowReader<R> rowReader);
	}

	private static class ScrollableResultsConsumer<R> implements ResultsConsumer<ScrollableResultsImplementor<R>,R> {
		/**
		 * Singleton access
		 */
		public static final ScrollableResultsConsumer INSTANCE = new ScrollableResultsConsumer();

		@SuppressWarnings("unchecked")
		public static <R> ScrollableResultsConsumer<R> instance() {
			return INSTANCE;
		}

		@Override
		public ScrollableResultsImplementor<R> consume(
				JdbcValuesSource jdbcValuesSource,
				SharedSessionContractImplementor persistenceContext,
				JdbcValuesSourceProcessingOptions processingOptions,
				JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState,
				RowProcessingStateStandardImpl rowProcessingState,
				RowReader<R> rowReader) {
			return null;
		}
	}

	private static class ListResultsConsumer<R> implements ResultsConsumer<List<R>,R> {
		/**
		 * Singleton access
		 */
		public static final ListResultsConsumer INSTANCE = new ListResultsConsumer();

		@SuppressWarnings("unchecked")
		public static <R> ListResultsConsumer<R> instance() {
			return INSTANCE;
		}

		@Override
		public List<R> consume(
				JdbcValuesSource jdbcValuesSource,
				SharedSessionContractImplementor persistenceContext,
				JdbcValuesSourceProcessingOptions processingOptions,
				JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState,
				RowProcessingStateStandardImpl rowProcessingState,
				RowReader<R> rowReader) {
			try {
				final List<R> results = new ArrayList<>();
				while ( rowProcessingState.next() ) {
					results.add(
							rowReader.readRow( rowProcessingState, processingOptions )
					);
					rowProcessingState.finishRowProcessing();
				}
				return results;
			}
			catch (SQLException e) {
				throw persistenceContext.getJdbcServices().getSqlExceptionHelper().convert(
						e,
						"Error processing return rows"
				);
			}
			finally {
				rowReader.finishUp( jdbcValuesSourceProcessingState );
				jdbcValuesSourceProcessingState.finishUp();
				jdbcValuesSource.finishUp();
			}
		}
	}

	private <T, R> T executeQuery(
			JdbcSelect jdbcSelect,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<R> rowTransformer,
			SharedSessionContractImplementor persistenceContext,
			Callback callback,
			ExecutionContext executionContext,
			PreparedStatementCreator statementCreator,
			ResultsConsumer<T,R> resultsConsumer) {
		final JdbcValuesSource jdbcValuesSource = resolveJdbcValuesSource(
				jdbcSelect,
				queryOptions,
				new DeferredResultSetAccess(
						persistenceContext,
						jdbcSelect,
						queryOptions,
						statementCreator,
						(ps, queryOptions1, session) -> ps.executeQuery(),
						queryParameterBindings
				),
				persistenceContext
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

		final JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState = new JdbcValuesSourceProcessingStateStandardImpl(
				jdbcValuesSource,
				queryOptions,
				processingOptions,
				persistenceContext
		);

		final List<ReturnAssembler> returnAssemblers = new ArrayList<>();
		final List<Initializer> initializers = new ArrayList<>();
		for ( Return queryReturn : jdbcSelect.getReturns() ) {
			returnAssemblers.add( queryReturn.getReturnAssembler() );

			if ( queryReturn instanceof InitializerSource ) {
				// todo : break the Initializers out into types
				( (InitializerSource) queryReturn ).registerInitializers( initializers::add );
			}
		}

		final RowReader<R> rowReader = new RowReaderStandardImpl<>(
				returnAssemblers,
				initializers,
				rowTransformer,
				callback
		);
		final RowProcessingStateStandardImpl rowProcessingState = new RowProcessingStateStandardImpl(
				jdbcValuesSourceProcessingState,
				queryOptions,
				jdbcValuesSource
		);

		return resultsConsumer.consume(
				jdbcValuesSource,
				persistenceContext,
				processingOptions,
				jdbcValuesSourceProcessingState,
				rowProcessingState,
				rowReader
		);
	}

	@SuppressWarnings("unchecked")
	private JdbcValuesSource resolveJdbcValuesSource(
			JdbcSelect jdbcSelect,
			QueryOptions queryOptions,
			ResultSetAccess resultSetAccess,
			SharedSessionContractImplementor persistenceContext) {
		final List<Object[]> cachedResults;

		final boolean queryCacheEnabled = persistenceContext.getFactory().getSessionFactoryOptions().isQueryCacheEnabled();
		final CacheMode cacheMode = resolveCacheMode( queryOptions.getCacheMode(), persistenceContext );

		if ( queryCacheEnabled && queryOptions.getCacheMode().isGetEnabled() ) {
			log.debugf( "Reading Query result cache data per CacheMode#isGetEnabled [%s]", cacheMode.name() );

			final QueryCache queryCache = persistenceContext.getFactory()
					.getCache()
					.getQueryCache( queryOptions.getResultCacheRegionName() );

			final QueryKey queryResultsCacheKey = null;

			cachedResults = queryCache.get(
					// todo : QueryCache#get takes the `queryResultsCacheKey` see tat discussion above
					queryResultsCacheKey,
					// todo : QueryCache#get also takes a `Type[] returnTypes` argument which ought to either:
					// 		1) be replaced with the Return graph
					//		2) removed (and Return graph made part of the QueryKey)
					null,
					// todo : QueryCache#get also takes a `isNaturalKeyLookup` argument which should go away
					// 		that is no longer the supported way to perform a load-by-naturalId
					false,
					// todo : `querySpaces` and `session` make perfect sense as args, but its odd passing those into this method just to pass along
					null,
					null
			);

			// todo : `querySpaces` and `session` are used in QueryCache#get to verify "up-to-dateness" via UpdateTimestampsCache
			//		better imo to move UpdateTimestampsCache handling here and have QueryCache be a simple access to
			//		the underlying query result cache region.
			//
			// todo : if we go this route (^^), still beneficial to have an abstraction over different UpdateTimestampsCache-based
			//		invalidation strategies - QueryCacheInvalidationStrategy
		}
		else {
			log.debugf( "Skipping reading Query result cache data: cache-enabled = %s, cache-mode = %s",
						queryCacheEnabled,
						cacheMode.name()
			);
			cachedResults = null;
		}

		if ( cachedResults == null || cachedResults.isEmpty() ) {
			return new JdbcValuesSourceResultSetImpl(
					resultSetAccess,
					queryOptions,
					jdbcSelect.getSqlSelections(),
					persistenceContext
			);
		}
		else {
			return new JdbcValuesSourceCacheHit( cachedResults );
		}
	}

	private CacheMode resolveCacheMode(CacheMode cacheMode, SharedSessionContractImplementor persistenceContext) {
		if ( cacheMode != null ) {
			return cacheMode;
		}

		cacheMode = persistenceContext.getCacheMode();
		if ( cacheMode != null ) {
			return cacheMode;
		}

		return CacheMode.NORMAL;
	}
}
