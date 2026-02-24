/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import jakarta.persistence.ParameterMode;
import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.ProcedureParameter;
import org.hibernate.procedure.NoMoreOutputsException;
import org.hibernate.procedure.Output;
import org.hibernate.query.results.spi.ResultSetMapping;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcCallRefCursorExtractor;
import org.hibernate.sql.results.internal.ResultsHelper;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.jdbc.internal.DirectResultSetAccess;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesResultSetImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.spi.RowReader;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.hibernate.procedure.internal.ProcedureOutputLogging.PROC_OUTPUT_LOGGER;
import static org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions.NO_OPTIONS;

/**
 * Encapsulates output handling for {@linkplain org.hibernate.procedure.ProcedureCall}.
 *
 * @author Steve Ebersole
 */
public class OutputsImpl implements ProcedureOutputs {
	private final ProcedureCallImpl<?> procedureCall;
	private final String sql;
	private final SqlStatementLogger sqlStatementLogger;

	private final CallableStatement jdbcStatement;

	private int resultPosition;
	private CurrentReturnState currentReturnState;

	private final Map<ProcedureParameter<?>, JdbcCallParameterRegistration> parameterRegistrations;
	private final JdbcCallRefCursorExtractor[] refCursorParameters;
	private int refCursorParamIndex;

	public OutputsImpl(
			ProcedureCallImpl<?> procedureCall,
			Map<ProcedureParameter<?>, JdbcCallParameterRegistration> parameterRegistrations,
			JdbcCallRefCursorExtractor[] refCursorParameters,
			CallableStatement jdbcStatement,
			String sql) {
		this.sqlStatementLogger = procedureCall.getSession().getJdbcServices().getSqlStatementLogger();
		this.sql = sql;
		this.procedureCall = procedureCall;
		this.jdbcStatement = jdbcStatement;
		this.parameterRegistrations = parameterRegistrations;
		this.refCursorParameters = refCursorParameters;
		executeStatement();
	}

	protected void executeStatement() {
		long executeStartNanos = 0;
		if ( sqlStatementLogger.getLogSlowQuery() > 0 ) {
			executeStartNanos = System.nanoTime();
		}
		final var session = procedureCall.getSession();
		final var eventMonitor = session.getEventMonitor();
		final var jdbcPreparedStatementExecutionEvent =
				eventMonitor.beginJdbcPreparedStatementExecutionEvent();
		try {
			final boolean isResultSet = jdbcStatement.execute();
			currentReturnState = buildCurrentReturnState( isResultSet );
		}
		catch (SQLException e) {
			session.getJdbcCoordinator().afterFailedStatementExecution( e );
			throw convert( e, "Error calling CallableStatement.getMoreResults" );
		}
		finally {
			eventMonitor.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, sql );
			sqlStatementLogger.logSlowQuery( sql, executeStartNanos, session.getJdbcSessionContext() );
		}
	}

	private CurrentReturnState buildCurrentReturnState(boolean isResultSet) {
		int updateCount = -1;
		if ( ! isResultSet ) {
			try {
				updateCount = jdbcStatement.getUpdateCount();
			}
			catch (SQLException e) {
				throw convert( e, "Error calling CallableStatement.getUpdateCount" );
			}
		}

		return buildCurrentReturnState( isResultSet, updateCount );
	}

	protected CurrentReturnState buildCurrentReturnState(boolean isResultSet, int updateCount) {
		return new CurrentReturnState( this, isResultSet, updateCount );
	}

	@Override
	public <T> T getOutputParameterValue(ProcedureParameter<T> parameter) {
		if ( parameter.getMode() == ParameterMode.IN ) {
			throw new ParameterMisuseException( "IN parameter not valid for output extraction" );
		}
		final var registration = parameterRegistrations.get( parameter );
		if ( registration == null ) {
			throw new IllegalArgumentException( "Parameter [" + parameter + "] is not registered with this procedure call" );
		}
		try {
			if ( registration.getParameterMode() == ParameterMode.REF_CURSOR ) {
				//noinspection unchecked
				return (T) registration.getRefCursorExtractor().extractResultSet(
						jdbcStatement,
						procedureCall.getSession()
				);
			}
			else {
				//noinspection unchecked
				return (T) registration.getParameterExtractor().extractValue(
						jdbcStatement,
						parameter.getPosition() == null,
						procedureCall.getSession()
				);
			}
		}
		catch (Exception e) {
			throw new ExecutionException(
					"Error extracting procedure output parameter value [" + parameter + "]",
					e
			);
		}
	}

	@Override
	public Object getOutputParameterValue(String name) {
		return getOutputParameterValue( procedureCall.getParameterMetadata().getQueryParameter( name ) );
	}

	@Override
	public Object getOutputParameterValue(int position) {
		return getOutputParameterValue( procedureCall.getParameterMetadata().getQueryParameter( position ) );
	}


	@Override
	public Output getCurrent() {
		return currentReturnState == null ? null : currentReturnState.getOutput();
	}

	@Override
	public boolean goToNext() {
		if ( currentReturnState == null ) {
			return false;
		}

		if ( currentReturnState.indicatesMoreOutputs() ) {
			// prepare the next return state
			try {
				final boolean isResultSet = jdbcStatement.getMoreResults();
				currentReturnState = buildCurrentReturnState( isResultSet );
			}
			catch (SQLException e) {
				throw convert( e, "Error calling CallableStatement.getMoreResults" );
			}
		}

		// and return
		return currentReturnState != null && currentReturnState.indicatesMoreOutputs();
	}

	@Override
	public void release() {
		final var jdbcCoordinator = procedureCall.getSession().getJdbcCoordinator();
		jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( jdbcStatement );
		jdbcCoordinator.afterStatementExecution();
	}

	protected JDBCException convert(SQLException e, String message) {
		return procedureCall.getSession()
				.getJdbcServices()
				.getSqlExceptionHelper()
				.convert( e, message, jdbcStatement.toString() );
	}

	private List<?> extractCurrentResults(ResultSetMapping resultSetMapping) {
		try {
			return extractResults( jdbcStatement.getResultSet(), resultSetMapping );
		}
		catch (SQLException e) {
			throw convert( e, "Error calling CallableStatement.getResultSet" );
		}
	}

	protected List<Object> extractResults(ResultSet resultSet, ResultSetMapping resultSetMapping) {
		final var session = procedureCall.getSession();
		final var factory = session.getFactory();
		final var influencers = session.getLoadQueryInfluencers();

		final var resultSetAccess = new DirectResultSetAccess( session, jdbcStatement, resultSet );

		final ExecutionContext executionContext = new OutputsExecutionContext( session );

		final JdbcValues jdbcValues = new JdbcValuesResultSetImpl(
				resultSetAccess,
				null,
				null,
				procedureCall.getQueryOptions(),
				true,
				resultSetMapping.resolve( resultSetAccess, influencers, factory ),
				null,
				executionContext
		);

		try {

			final RowReader<?> rowReader = ResultsHelper.createRowReader(
					factory,
					RowTransformerStandardImpl.instance(),
					null,
					jdbcValues
			);


			final var jdbcValuesSourceProcessingState =
					new JdbcValuesSourceProcessingStateStandardImpl( executionContext, NO_OPTIONS );
			final ArrayList<Object> results = new ArrayList<>();
			final var rowProcessingState = new RowProcessingStateStandardImpl(
					jdbcValuesSourceProcessingState,
					executionContext,
					rowReader,
					jdbcValues
			);
			try {

				rowReader.startLoading( rowProcessingState );

				while ( rowProcessingState.next() ) {
					results.add( rowReader.readRow( rowProcessingState ) );
					rowProcessingState.finishRowProcessing( true );
				}
				if ( resultSetMapping.getNumberOfResultBuilders() == 0
					&& procedureCall.isFunctionCall()
					&& procedureCall.getFunctionReturn().getJdbcTypeCode() == Types.REF_CURSOR
					&& results.size() == 1
					&& results.get( 0 ) instanceof ResultSet onlyResult ) {
					// When calling a function that returns a ref_cursor with as table function,
					// we have to unnest the ResultSet manually here
					return extractResults( onlyResult, resultSetMapping );
				}
				return results;
			}
			finally {
				rowReader.finishUp( rowProcessingState );
				jdbcValuesSourceProcessingState.finishUp( results.size() > 1 );
			}
		}
		finally {
			jdbcValues.finishUp( session );
		}
	}



	protected static class CurrentReturnState {
		private final OutputsImpl outputs;
		private final boolean isResultSet;
		private final int updateCount;

		private Output rtn;

		private CurrentReturnState(OutputsImpl outputs, boolean isResultSet, int updateCount) {
			this.outputs = outputs;
			this.isResultSet = isResultSet;
			this.updateCount = updateCount;
		}

		public boolean isResultSet() {
			return isResultSet;
		}

		public int getUpdateCount() {
			return updateCount;
		}

		public boolean indicatesMoreOutputs() {
			return isResultSet() || getUpdateCount() >= 0
				|| outputs.refCursorParamIndex < outputs.refCursorParameters.length;
		}

		public Output getOutput() {
			if ( rtn == null ) {
				rtn = buildOutput();
			}
			return rtn;
		}

		protected Output buildOutput() {
			if ( PROC_OUTPUT_LOGGER.isTraceEnabled() ) {
				PROC_OUTPUT_LOGGER.tracef( "Building Return [isResultSet=%s, updateCount=%s, extendedReturn=%s]",
						isResultSet(), getUpdateCount(), hasExtendedReturns() );
			}

			if ( isResultSet() ) {
				return buildResultSetOutput( outputs::extractCurrentResults );
			}
			else if ( getUpdateCount() >= 0 ) {
				return buildUpdateCountOutput( updateCount );
			}
			else if ( hasExtendedReturns() ) {
				return buildExtendedReturn();
			}
			else if ( hasFunctionReturns() ) {
				return buildFunctionReturn();
			}

			throw new NoMoreOutputsException();
		}

		protected <T> Output buildResultSetOutput(Function<ResultSetMapping, List<T>> listSupplier) {
			return new ResultSetOutputImpl<>(
					listSupplier,
					determineDeclaredMapping( outputs.resultPosition++ ),
					outputs.procedureCall.getSessionFactory()
			);
		}

		private ResultSetMapping determineDeclaredMapping(int resultPosition) {
			final var resultSetMappings = outputs.procedureCall.getResultSetMappings();
			assert CollectionHelper.isNotEmpty( resultSetMappings );
			if ( resultSetMappings.size() == 1 ) {
				if ( resultSetMappings.get( 0 ).isDynamic() ) {
					// there was exactly one mapping and it is dynamic...
					// this means that the query was created without any mappings.
					// just keep reusing that dynamic one.
					return resultSetMappings.get( 0 );
				}
			}
			if ( resultSetMappings.size() <= resultPosition ) {
				// return a dynamic mapping
				return outputs.procedureCall.getSessionFactory()
						.getJdbcValuesMappingProducerProvider()
						.buildResultSetMapping( null, true, outputs.procedureCall.getSessionFactory() );
			}

			return resultSetMappings.get( resultPosition );

		}

		protected Output buildUpdateCountOutput(int updateCount) {
			return new UpdateCountOutputImpl( updateCount );
		}

		protected boolean hasExtendedReturns() {
			return outputs.refCursorParamIndex < outputs.refCursorParameters.length;
		}

		protected Output buildExtendedReturn() {
			final var refCursorParam = outputs.refCursorParameters[outputs.refCursorParamIndex++];
			final var resultSet = refCursorParam.extractResultSet(
					outputs.jdbcStatement,
					outputs.procedureCall.getSession()
			);
			return buildResultSetOutput( (resultSetMapping) -> outputs.extractResults( resultSet, resultSetMapping ) );
		}

		protected boolean hasFunctionReturns() {
			return outputs.parameterRegistrations.get( outputs.procedureCall.getFunctionReturn() ) != null;
		}

		protected Output buildFunctionReturn() {
			final Object result =
					outputs.parameterRegistrations.get( outputs.procedureCall.getFunctionReturn() )
							.getParameterExtractor()
							.extractValue(
									outputs.jdbcStatement,
									false,
									outputs.procedureCall.getSession()
							);
			final List<Object> results = new ArrayList<>( 1 );
			results.add( result );
			return buildResultSetOutput( (resultSetMapping) -> results );
		}
	}
}
