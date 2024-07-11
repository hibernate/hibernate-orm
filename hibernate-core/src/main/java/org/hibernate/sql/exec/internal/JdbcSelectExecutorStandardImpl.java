/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.hibernate.CacheMode;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.TupleTransformer;
import org.hibernate.sql.exec.SqlExecLogger;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.internal.ResultsHelper;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.internal.RowTransformerTupleTransformerAdapter;
import org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesCacheHit;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesResultSetImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.internal.ResultSetAccess;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Standard JdbcSelectExecutor implementation used by Hibernate,
 * through {@link JdbcSelectExecutorStandardImpl#INSTANCE}
 *
 * @author Steve Ebersole
 */
public class JdbcSelectExecutorStandardImpl implements JdbcSelectExecutor {
	/**
	 * Singleton access
	 */
	public static final JdbcSelectExecutorStandardImpl INSTANCE = new JdbcSelectExecutorStandardImpl();

	@Override
	public <T, R> T executeQuery(
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Class<R> domainResultType,
			Function<String, PreparedStatement> statementCreator,
			ResultsConsumer<T, R> resultsConsumer) {
		return executeQuery(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				rowTransformer,
				domainResultType,
				-1,
				statementCreator,
				resultsConsumer
		);
	}

	@Override
	public <T, R> T executeQuery(
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Class<R> domainResultType,
			int resultCountEstimate,
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
					domainResultType,
					resultCountEstimate,
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
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			Class<R> domainResultType,
			int resultCountEstimate,
			Function<String, PreparedStatement> statementCreator,
			ResultsConsumer<T, R> resultsConsumer) {

		final DeferredResultSetAccess deferredResultSetAccess = new DeferredResultSetAccess(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				statementCreator,
				resultCountEstimate
		);
		final JdbcValues jdbcValues = resolveJdbcValuesSource(
				executionContext.getQueryIdentifier( deferredResultSetAccess.getFinalSql() ),
				jdbcSelect,
				resultsConsumer.canResultsBeCached(),
				executionContext,
				deferredResultSetAccess
		);

		if ( rowTransformer == null ) {
			@SuppressWarnings("unchecked")
			final TupleTransformer<R> tupleTransformer = (TupleTransformer<R>) executionContext
					.getQueryOptions()
					.getTupleTransformer();

			if ( tupleTransformer == null ) {
				rowTransformer = RowTransformerStandardImpl.instance();
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

		final SharedSessionContractImplementor session = executionContext.getSession();

		final boolean stats;
		long startTime = 0;
		final StatisticsImplementor statistics = session.getFactory().getStatistics();
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
				processingOptions
		);

		final RowReader<R> rowReader = ResultsHelper.createRowReader(
				session.getFactory(),
				rowTransformer,
				domainResultType,
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
				session,
				processingOptions,
				valuesProcessingState,
				rowProcessingState,
				rowReader
		);

		if ( stats ) {
			final long endTime = System.nanoTime();
			final long milliseconds = TimeUnit.MILLISECONDS.convert( endTime - startTime, TimeUnit.NANOSECONDS );
			statistics.queryExecuted(
					executionContext.getQueryIdentifier( jdbcSelect.getSqlString() ),
					getResultSize( result ),
					milliseconds
			);
		}

		return result;
	}

	private <T> int getResultSize(T result) {
		if ( result instanceof List ) {
			return ( (List<?>) result ).size();
		}
		return -1;
	}

	private JdbcValues resolveJdbcValuesSource(
			String queryIdentifier,
			JdbcOperationQuerySelect jdbcSelect,
			boolean canBeCached,
			ExecutionContext executionContext,
			DeferredResultSetAccess resultSetAccess) {
		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final boolean queryCacheEnabled = factory.getSessionFactoryOptions().isQueryCacheEnabled();

		final List<?> cachedResults;
		final CacheMode cacheMode = JdbcExecHelper.resolveCacheMode( executionContext );

		final JdbcValuesMappingProducer mappingProducer = jdbcSelect.getJdbcValuesMappingProducer();
		final boolean cacheable = queryCacheEnabled && canBeCached
				&& executionContext.getQueryOptions().isResultCachingEnabled() == Boolean.TRUE;
		final QueryKey queryResultsCacheKey;

		if ( cacheable && cacheMode.isGetEnabled() ) {
			SqlExecLogger.SQL_EXEC_LOGGER.debugf( "Reading Query result cache data per CacheMode#isGetEnabled [%s]", cacheMode.name() );
			final Set<String> querySpaces = jdbcSelect.getAffectedTableNames();
			if ( querySpaces == null || querySpaces.size() == 0 ) {
				SqlExecLogger.SQL_EXEC_LOGGER.tracef( "Unexpected querySpaces is empty" );
			}
			else {
				SqlExecLogger.SQL_EXEC_LOGGER.tracef( "querySpaces is `%s`", querySpaces );
			}

			final QueryResultsCache queryCache = factory.getCache()
					.getQueryResultsCache( executionContext.getQueryOptions().getResultCacheRegionName() );

			queryResultsCacheKey = QueryKey.from(
					jdbcSelect.getSqlString(),
					executionContext.getQueryOptions().getLimit(),
					executionContext.getQueryParameterBindings(),
					session
			);

			cachedResults = queryCache.get(
					// todo (6.0) : QueryCache#get takes the `queryResultsCacheKey` see tat discussion above
					queryResultsCacheKey,
					// todo (6.0) : `querySpaces` and `session` make perfect sense as args, but its odd passing those into this method just to pass along
					//		atm we do not even collect querySpaces, but we need to
					querySpaces,
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
			SqlExecLogger.SQL_EXEC_LOGGER.debugf( "Skipping reading Query result cache data: cache-enabled = %s, cache-mode = %s",
					queryCacheEnabled,
					cacheMode.name()
			);
			cachedResults = null;
			if ( cacheable && cacheMode.isPutEnabled() ) {
				queryResultsCacheKey = QueryKey.from(
						jdbcSelect.getSqlString(),
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
			final JdbcValuesMetadata metadataForCache;
			final JdbcValuesMapping jdbcValuesMapping;
			if ( queryResultsCacheKey == null ) {
				jdbcValuesMapping = mappingProducer.resolve( resultSetAccess, session.getLoadQueryInfluencers(), factory );
				metadataForCache = null;
			}
			else {
				// If we need to put the values into the cache, we need to be able to capture the JdbcValuesMetadata
				final CapturingJdbcValuesMetadata capturingMetadata = new CapturingJdbcValuesMetadata( resultSetAccess );
				jdbcValuesMapping = mappingProducer.resolve( capturingMetadata, session.getLoadQueryInfluencers(), factory );
				metadataForCache = capturingMetadata.resolveMetadataForCache();
			}

			return new JdbcValuesResultSetImpl(
					resultSetAccess,
					queryResultsCacheKey,
					queryIdentifier,
					executionContext.getQueryOptions(),
					resultSetAccess.usesFollowOnLocking(),
					jdbcValuesMapping,
					metadataForCache,
					executionContext
			);
		}
		else {
			final JdbcValuesMapping jdbcValuesMapping;
			if ( cachedResults.isEmpty() || !( cachedResults.get( 0 ) instanceof JdbcValuesMetadata ) ) {
				jdbcValuesMapping = mappingProducer.resolve( resultSetAccess, session.getLoadQueryInfluencers(), factory );
			}
			else {
				jdbcValuesMapping = mappingProducer.resolve( (JdbcValuesMetadata) cachedResults.get( 0 ), session.getLoadQueryInfluencers(), factory );
			}
			return new JdbcValuesCacheHit( cachedResults, jdbcValuesMapping );
		}
	}

	public static class CapturingJdbcValuesMetadata implements JdbcValuesMetadata {
		private final ResultSetAccess resultSetAccess;
		private String[] columnNames;
		private BasicType<?>[] types;

		public CapturingJdbcValuesMetadata(ResultSetAccess resultSetAccess) {
			this.resultSetAccess = resultSetAccess;
		}

		private void initializeArrays() {
			final int columnCount = resultSetAccess.getColumnCount();
			columnNames = new String[columnCount];
			types = new BasicType[columnCount];
		}

		@Override
		public int getColumnCount() {
			if ( columnNames == null ) {
				initializeArrays();
			}
			return columnNames.length;
		}

		@Override
		public int resolveColumnPosition(String columnName) {
			if ( columnNames == null ) {
				initializeArrays();
			}
			int position;
			if ( columnNames == null ) {
				position = resultSetAccess.resolveColumnPosition( columnName );
				columnNames[position - 1] = columnName;
			}
			else if ( ( position = ArrayHelper.indexOf( columnNames, columnName ) + 1 ) == 0 ) {
				position = resultSetAccess.resolveColumnPosition( columnName );
				columnNames[position - 1] = columnName;
			}
			return position;
		}

		@Override
		public String resolveColumnName(int position) {
			if ( columnNames == null ) {
				initializeArrays();
			}
			String name;
			if ( columnNames == null ) {
				name = resultSetAccess.resolveColumnName( position );
				columnNames[position - 1] = name;
			}
			else if ( ( name = columnNames[position - 1] ) == null ) {
				name = resultSetAccess.resolveColumnName( position );
				columnNames[position - 1] = name;
			}
			return name;
		}

		@Override
		public <J> BasicType<J> resolveType(
				int position,
				JavaType<J> explicitJavaType,
				TypeConfiguration typeConfiguration) {
			if ( columnNames == null ) {
				initializeArrays();
			}
			final BasicType<J> basicType = resultSetAccess.resolveType(
					position,
					explicitJavaType,
					typeConfiguration
			);
			types[position - 1] = basicType;
			return basicType;
		}

		public JdbcValuesMetadata resolveMetadataForCache() {
			if ( columnNames == null ) {
				return null;
			}
			return new CachedJdbcValuesMetadata( columnNames, types );
		}
	}

	private static class CachedJdbcValuesMetadata implements JdbcValuesMetadata, Serializable {
		private final String[] columnNames;
		private final BasicType<?>[] types;

		public CachedJdbcValuesMetadata(String[] columnNames, BasicType<?>[] types) {
			this.columnNames = columnNames;
			this.types = types;
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int resolveColumnPosition(String columnName) {
			final int position = ArrayHelper.indexOf( columnNames, columnName ) + 1;
			if ( position == 0 ) {
				throw new IllegalStateException( "Unexpected resolving of unavailable column: " + columnName );
			}
			return position;
		}

		@Override
		public String resolveColumnName(int position) {
			final String name = columnNames[position - 1];
			if ( name == null ) {
				throw new IllegalStateException( "Unexpected resolving of unavailable column at position: " + position );
			}
			return name;
		}

		@Override
		public <J> BasicType<J> resolveType(
				int position,
				JavaType<J> explicitJavaType,
				TypeConfiguration typeConfiguration) {
			final BasicType<?> type = types[position - 1];
			if ( type == null ) {
				throw new IllegalStateException( "Unexpected resolving of unavailable column at position: " + position );
			}
			if ( explicitJavaType == null || type.getJavaTypeDescriptor() == explicitJavaType ) {
				//noinspection unchecked
				return (BasicType<J>) type;
			}
			else {
				return typeConfiguration.getBasicTypeRegistry().resolve(
						explicitJavaType,
						type.getJdbcType()
				);
			}
		}

	}
}
