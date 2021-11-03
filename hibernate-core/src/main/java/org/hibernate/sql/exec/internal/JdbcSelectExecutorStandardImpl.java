/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.CacheMode;
import org.hibernate.Internal;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.internal.ScrollableResultsIterator;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.exec.SqlExecLogger;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.internal.ResultsHelper;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.results.internal.RowTransformerTupleTransformerAdapter;
import org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesCacheHit;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesResultSetImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.internal.ResultSetAccess;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.sql.results.spi.ScrollableResultsConsumer;
import org.hibernate.stat.spi.StatisticsImplementor;

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

	@Override
	public <R> List<R> list(
			JdbcSelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			ListResultsConsumer.UniqueSemantic uniqueSemantic) {
		// Only do auto flushing for top level queries
		return executeQuery(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				rowTransformer,
				(sql) -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				ListResultsConsumer.instance( uniqueSemantic )
		);
	}

	@Internal
	public <R> List<R> list(
			JdbcSelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			ListResultsConsumer.UniqueSemantic uniqueSemantic,
			Function<Function<String, PreparedStatement>, DeferredResultSetAccess> resultSetAccessCreator) {
		// Only do auto flushing for top level queries
		return executeQuery(
				jdbcSelect,
				executionContext,
				rowTransformer,
				(sql) -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				resultSetAccessCreator,
				ListResultsConsumer.instance( uniqueSemantic )
		);
	}

	@Override
	public <R> ScrollableResultsImplementor<R> scroll(
			JdbcSelect jdbcSelect,
			ScrollMode scrollMode,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		session.autoFlushIfRequired( jdbcSelect.getAffectedTableNames() );
		return executeQuery(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				rowTransformer,
				(sql) -> executionContext.getSession().getJdbcCoordinator().getStatementPreparer().prepareQueryStatement(
						sql,
						false,
						scrollMode
				),
				ScrollableResultsConsumer.instance()
		);
	}

	@Internal
	public <R> ScrollableResultsImplementor<R> scroll(
			JdbcSelect jdbcSelect,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Function<Function<String, PreparedStatement>, DeferredResultSetAccess> resultSetAccessCreator) {
		// Only do auto flushing for top level queries
		return executeQuery(
				jdbcSelect,
				executionContext,
				rowTransformer,
				(sql) -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				resultSetAccessCreator,
				ScrollableResultsConsumer.instance()
		);
	}

	@Override
	public <R> Stream<R> stream(
			JdbcSelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer) {
		final ScrollableResultsImplementor<R> scrollableResults = scroll(
				jdbcSelect,
				ScrollMode.FORWARD_ONLY,
				jdbcParameterBindings,
				executionContext,
				rowTransformer
		);
		final ScrollableResultsIterator<R> iterator = new ScrollableResultsIterator<>( scrollableResults );
		final Spliterator<R> spliterator = Spliterators.spliteratorUnknownSize( iterator, Spliterator.NONNULL );

		final Stream<R> stream = StreamSupport.stream( spliterator, false );
		return stream.onClose( scrollableResults::close );
	}

	@Internal
	public <R> Stream<R> stream(
			JdbcSelect jdbcSelect,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Function<Function<String, PreparedStatement>, DeferredResultSetAccess> resultSetAccessCreator) {
		final ScrollableResultsImplementor<R> scrollableResults = scroll(
				jdbcSelect,
				executionContext,
				rowTransformer,
				resultSetAccessCreator
		);
		final ScrollableResultsIterator<R> iterator = new ScrollableResultsIterator<>( scrollableResults );
		final Spliterator<R> spliterator = Spliterators.spliteratorUnknownSize( iterator, Spliterator.NONNULL );

		final Stream<R> stream = StreamSupport.stream( spliterator, false );
		return stream.onClose( scrollableResults::close );
	}

	private <T, R> T executeQuery(
			JdbcSelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Function<String, PreparedStatement> statementCreator,
			ResultsConsumer<T, R> resultsConsumer) {
		final PersistenceContext persistenceContext = executionContext.getSession().getPersistenceContext();
		boolean defaultReadOnlyOrig = persistenceContext.isDefaultReadOnly();
		Boolean readOnly = executionContext.getQueryOptions().isReadOnly();
		if ( readOnly != null ) {
			// The read-only/modifiable mode for the query was explicitly set.
			// Temporarily set the default read-only/modifiable setting to the query's setting.
			persistenceContext.setDefaultReadOnly( readOnly );
		}
		try {
			return doExecuteQuery(
					jdbcSelect,
					jdbcParameterBindings,
					executionContext,
					rowTransformer,
					statementCreator,
					resultsConsumer
			);
		}
		finally {
			if ( readOnly != null ) {
				persistenceContext.setDefaultReadOnly( defaultReadOnlyOrig );
			}
		}
	}

	private <T, R> T doExecuteQuery(
			JdbcSelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Function<String, PreparedStatement> statementCreator,
			ResultsConsumer<T, R> resultsConsumer) {
		return executeQuery(
				jdbcSelect,
				executionContext,
				rowTransformer,
				(sql) -> executionContext.getSession()
						.getJdbcCoordinator()
						.getStatementPreparer()
						.prepareStatement( sql ),
				(stmntCreator) -> new DeferredResultSetAccess(
						jdbcSelect,
						jdbcParameterBindings,
						executionContext,
						statementCreator
				),
				resultsConsumer
		);
	}

	private <T, R> T executeQuery(
			JdbcSelect jdbcSelect,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Function<String,PreparedStatement> statementCreator,
			Function<Function<String, PreparedStatement>, DeferredResultSetAccess> resultSetAccessCreator,
			ResultsConsumer<T,R> resultsConsumer) {
		final DeferredResultSetAccess resultSetAccess = resultSetAccessCreator.apply( statementCreator );
		final JdbcValues jdbcValues = resolveJdbcValuesSource(
				executionContext.getQueryIdentifier( resultSetAccess.getFinalSql() ),
				jdbcSelect,
				resultsConsumer.canResultsBeCached(),
				executionContext,
				resultSetAccess
		);

		if ( rowTransformer == null ) {
			final TupleTransformer<R> tupleTransformer = executionContext.getQueryOptions().getTupleTransformer();
			if ( tupleTransformer == null ) {
				rowTransformer = RowTransformerPassThruImpl.instance();
			}
			else {
				final List<DomainResult<?>> domainResults = jdbcValues.getValuesMapping().getDomainResults();
				final String[] aliases = new String[domainResults.size()];
				for ( int i = 0; i < domainResults.size(); i++ ) {
					aliases[i] = domainResults.get( i ).getResultVariable();
				}
				rowTransformer = new RowTransformerTupleTransformerAdapter<>( aliases, tupleTransformer );
			}
		}

		final boolean stats;
		long startTime = 0;
		final StatisticsImplementor statistics = executionContext.getSession().getFactory().getStatistics();
		if ( executionContext.hasQueryExecutionToBeAddedToStatistics()
				&& jdbcValues instanceof JdbcValuesResultSetImpl ) {
			stats = statistics.isStatisticsEnabled();
			if ( stats ) {
				startTime = System.nanoTime();
			}
		}
		else {
			stats = false;
		}

		/*
		 * Processing options effectively are only used for entity loading.  Here we don't need these values.
		 */
		final JdbcValuesSourceProcessingOptions processingOptions = new JdbcValuesSourceProcessingOptions() {
			@Override
			public Object getEffectiveOptionalObject() {
				return executionContext.getEntityInstance();
			}

			@Override
			public String getEffectiveOptionalEntityName() {
				return null;
			}

			@Override
			public Object getEffectiveOptionalId() {
				return executionContext.getEntityId();
			}

			@Override
			public boolean shouldReturnProxies() {
				return true;
			}
		};

		final JdbcValuesSourceProcessingStateStandardImpl valuesProcessingState = new JdbcValuesSourceProcessingStateStandardImpl(
				executionContext,
				processingOptions,
				executionContext::registerLoadingEntityEntry
		);

		final RowReader<R> rowReader = ResultsHelper.createRowReader(
				executionContext,
				// If follow on locking is used, we must omit the lock options here,
				// because these lock options are only for Initializers.
				// If we wouldn't omit this, the follow on lock requests would be no-ops,
				// because the EntityEntrys would already have the desired lock mode
				resultSetAccess.usesFollowOnLocking()
						? LockOptions.NONE
						: executionContext.getQueryOptions().getLockOptions(),
				rowTransformer,
				jdbcValues
		);

		final RowProcessingStateStandardImpl rowProcessingState = new RowProcessingStateStandardImpl(
				valuesProcessingState,
				executionContext,
				rowReader,
				jdbcValues
		);

		final T result = resultsConsumer.consume(
				jdbcValues,
				executionContext.getSession(),
				processingOptions,
				valuesProcessingState,
				rowProcessingState,
				rowReader
		);

		if ( stats ) {
			final long endTime = System.nanoTime();
			final long milliseconds = TimeUnit.MILLISECONDS.convert( endTime - startTime, TimeUnit.NANOSECONDS );
			statistics.queryExecuted(
					executionContext.getQueryIdentifier( jdbcSelect.getSql() ),
					getResultSize( result ),
					milliseconds
			);
		}

		return result;
	}

	private <T> int getResultSize(T result) {
		if ( result instanceof List ) {
			return ( (List) result ).size();
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	private JdbcValues resolveJdbcValuesSource(
			String queryIdentifier,
			JdbcSelect jdbcSelect,
			boolean canBeCached,
			ExecutionContext executionContext,
			ResultSetAccess resultSetAccess) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final boolean queryCacheEnabled = factory.getSessionFactoryOptions().isQueryCacheEnabled();

		final List<Object[]> cachedResults;


		final CacheMode cacheMode = JdbcExecHelper.resolveCacheMode( executionContext );

		final JdbcValuesMappingProducer mappingProducer = jdbcSelect.getJdbcValuesMappingProducer();
		final JdbcValuesMapping jdbcValuesMapping = mappingProducer.resolve( resultSetAccess, factory );

		final QueryKey queryResultsCacheKey;

		if ( queryCacheEnabled && cacheMode.isGetEnabled() && canBeCached ) {
			SqlExecLogger.INSTANCE.debugf( "Reading Query result cache data per CacheMode#isGetEnabled [%s]", cacheMode.name() );

			final QueryResultsCache queryCache = factory
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
					executionContext.getQueryParameterBindings(),
					session
			);

			cachedResults = queryCache.get(
					// todo (6.0) : QueryCache#get takes the `queryResultsCacheKey` see tat discussion above
					queryResultsCacheKey,
					// todo (6.0) : `querySpaces` and `session` make perfect sense as args, but its odd passing those into this method just to pass along
					//		atm we do not even collect querySpaces, but we need to
					jdbcSelect.getAffectedTableNames(),
					session
			);

			// todo (6.0) : `querySpaces` and `session` are used in QueryCache#get to verify "up-to-dateness" via UpdateTimestampsCache
			//		better imo to move UpdateTimestampsCache handling here and have QueryCache be a simple access to
			//		the underlying query result cache region.
			//
			// todo (6.0) : if we go this route (^^), still beneficial to have an abstraction over different UpdateTimestampsCache-based
			//		invalidation strategies - QueryCacheInvalidationStrategy

			final StatisticsImplementor statistics = factory.getStatistics();
			if ( statistics.isStatisticsEnabled() ) {
				if ( cachedResults == null ) {
					statistics.queryCacheMiss( queryIdentifier, queryCache.getRegion().getName() );
				}
				else {
					statistics.queryCacheHit( queryIdentifier, queryCache.getRegion().getName() );
				}
			}
		}
		else {
			SqlExecLogger.INSTANCE.debugf( "Skipping reading Query result cache data: cache-enabled = %s, cache-mode = %s",
						queryCacheEnabled,
						cacheMode.name()
			);
			cachedResults = null;
			if ( queryCacheEnabled && canBeCached ) {
				queryResultsCacheKey = QueryKey.from(
						jdbcSelect.getSql(),
						executionContext.getQueryOptions().getLimit(),
						executionContext.getQueryParameterBindings(),
						session
				);
			}
			else {
				queryResultsCacheKey = null;
			}
		}

		if ( cachedResults == null ) {
			return new JdbcValuesResultSetImpl(
					resultSetAccess,
					queryResultsCacheKey,
					queryIdentifier,
					executionContext.getQueryOptions(),
					jdbcValuesMapping,
					executionContext
			);
		}
		else {
			return new JdbcValuesCacheHit( cachedResults, jdbcValuesMapping );
		}
	}

}
