/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.ExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.result.Outputs;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.select.SqlSelection;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.spi.Callback;
import org.hibernate.sql.convert.spi.SqmSelectInterpretation;
import org.hibernate.sql.exec.results.process.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.exec.results.process.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.exec.results.process.internal.RowReaderStandardImpl;
import org.hibernate.sql.exec.results.process.internal.values.JdbcValuesSource;
import org.hibernate.sql.exec.results.process.internal.values.JdbcValuesSourceCacheHit;
import org.hibernate.sql.exec.results.process.internal.values.JdbcValuesSourceResultSetImpl;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.process.spi.RowReader;
import org.hibernate.sql.exec.results.process.spi.Initializer;
import org.hibernate.sql.exec.results.process.spi.InitializerSource;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;
import org.hibernate.sql.exec.spi.PreparedStatementCreator;
import org.hibernate.sql.exec.spi.PreparedStatementExecutor;
import org.hibernate.sql.exec.spi.RowTransformer;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.SqlTreeExecutor;

import org.jboss.logging.Logger;

/**
 * Standard SqlTreeExecutor implementation
 *
 * @author Steve Ebersole
 */
public class SqlTreeExecutorImpl implements SqlTreeExecutor {
	private static final Logger log = Logger.getLogger( SqlTreeExecutorImpl.class );

	@SuppressWarnings("unchecked")
	@Override
	public <R, T> R executeSelect(
			SqmSelectInterpretation sqmSelectInterpretation,
			PreparedStatementCreator statementCreator,
			PreparedStatementExecutor preparedStatementExecutor,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<T> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor persistenceContext,
			ExecutionContext executionContext) {
		// Walk the SQL AST.  This produces:
		//		* SQL string
		//		* ParameterBinders
		//		* Returns

		// todo : should also pass in QueryOptions
		// 		as the rendered SQL would depend on first/max results, comment, db-hints, lock-options, entity-graph

		// todo : also need to account for multi-valued param bindings in terms of the generated SQL...

		// todo : actually, why not just pass in the SqlSelectInterpretation rather than SelectQuery (SQL AST)
		//		The only use of the SQL AST here is in building the SqlSelectInterpretation

		// todo : look at making SqlAstSelectInterpreter into an interface and having that be the thing that Dialects can hook into the translation
		//		nice tie-in with Dialect handling follow-on-locking, rendering of the actual SQL, etc


		// todo : I think we also want different SqlTreeExecutor implementors for handling ScrollableResults versus List versus Stream ...

//		if ( interpretNumberOfRowsToProcess( queryOptions ) == 0 ) {
//			return Collections.<R>emptyList();
//		}

		final JdbcSelect jdbcSelect = SqlAstSelectInterpreter.interpret(
				sqmSelectInterpretation,
				false,
				persistenceContext.getFactory(),
				queryParameterBindings,
				executionContext
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

		final JdbcValuesSource jdbcValuesSource = resolveJdbcValuesSource(
				queryOptions,
				persistenceContext,
				jdbcSelect,
				statementCreator,
				preparedStatementExecutor,
				queryParameterBindings,
				sqmSelectInterpretation.getSqlSelectAst().getQuerySpec().getSelectClause().getSqlSelections()
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

		final RowReader<T> rowReader = new RowReaderStandardImpl<>(
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

		try {
			final List<T> results = new ArrayList<T>();
			while ( rowProcessingState.next() ) {
				results.add(
						rowReader.readRow( rowProcessingState, processingOptions )
				);
				rowProcessingState.finishRowProcessing();
			}
			return (R) results;
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

	@SuppressWarnings("unchecked")
	private JdbcValuesSource resolveJdbcValuesSource(
			QueryOptions queryOptions,
			SharedSessionContractImplementor persistenceContext,
			JdbcSelect jdbcSelect,
			PreparedStatementCreator statementCreator,
			PreparedStatementExecutor statementExecutor,
			QueryParameterBindings queryParameterBindings,
			List<SqlSelection> sqlSelections) {
		List<Object[]> cachedResults = null;

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
					// todo : QueryCache#get also takes a `Type[] returnTypes` argument which ought to be replaced with the Return graph
					// 		(or ResolvedReturn graph)
					null,
					// todo : QueryCache#get also takes a `isNaturalKeyLookup` argument which should go away
					// 		that is no longer the supported way to perform a load-by-naturalId
					false,
					// todo : `querySpaces` and `session` make perfect sense as args, but its odd passing those here
					null,
					null
			);
		}
		else {
			log.debugf( "Skipping reading Query result cache data: cache-enabled = %s, cache-mode = %s",
						queryCacheEnabled,
						cacheMode.name()
			);
		}

		if ( cachedResults == null || cachedResults.isEmpty() ) {
			return new JdbcValuesSourceResultSetImpl(
					persistenceContext,
					jdbcSelect,
					queryOptions,
					statementCreator,
					statementExecutor,
					queryParameterBindings,
					sqlSelections
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

	@Override
	public Object[] executeInsert(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session,
			ExecutionContext executionContext) {
		throw new NotYetImplementedException( "DML execution is not yet implemented" );
	}

	@Override
	public int executeUpdate(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session,
			ExecutionContext executionContext) {
		throw new NotYetImplementedException( "DML execution is not yet implemented" );
	}

	@Override
	public int executeDelete(
			Object sqlTree,
			PreparedStatementCreator statementCreator,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session,
			ExecutionContext executionContext) {
		throw new NotYetImplementedException( "DML execution is not yet implemented" );
	}

	@Override
	public <T> Outputs executeCall(
			String callableName,
			QueryOptions queryOptions,
			QueryParameterBindings queryParameterBindings,
			RowTransformer<T> rowTransformer,
			Callback callback,
			SharedSessionContractImplementor session,
			ExecutionContext executionContext) {
		throw new NotYetImplementedException( "Procedure/function call execution is not yet implemented" );
	}
}
