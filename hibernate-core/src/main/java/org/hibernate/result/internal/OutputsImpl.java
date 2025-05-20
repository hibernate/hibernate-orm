/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result.internal;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.JDBCException;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventManager;
import org.hibernate.event.spi.HibernateMonitoringEvent;
import org.hibernate.internal.CoreLogging;
import org.hibernate.procedure.internal.ProcedureCallImpl;
import org.hibernate.procedure.internal.ScalarDomainResultBuilder;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.result.Output;
import org.hibernate.result.Outputs;
import org.hibernate.result.spi.ResultContext;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.NoMoreOutputsException;
import org.hibernate.sql.results.internal.ResultsHelper;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.jdbc.internal.DirectResultSetAccess;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesResultSetImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;

import org.jboss.logging.Logger;

import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
public class OutputsImpl implements Outputs {
	private static final Logger log = CoreLogging.logger( OutputsImpl.class );

	private final ResultContext context;
	private final PreparedStatement jdbcStatement;
	private final SqlStatementLogger sqlStatementLogger;
	private final String sql;

	private CurrentReturnState currentReturnState;

	public OutputsImpl(ResultContext context, PreparedStatement jdbcStatement, String sql) {
		this.context = context;
		this.jdbcStatement = jdbcStatement;
		this.sqlStatementLogger = context.getSession().getJdbcServices().getSqlStatementLogger();
		this.sql = sql;
	}

	protected ResultContext getResultContext(){
		return context;
	}

	protected void executeStatement() {
		long executeStartNanos = 0;
		if ( sqlStatementLogger.getLogSlowQuery() > 0 ) {
			executeStartNanos = System.nanoTime();
		}
		final EventManager eventManager = context.getSession().getEventManager();
		final HibernateMonitoringEvent jdbcPreparedStatementExecutionEvent = eventManager.beginJdbcPreparedStatementExecutionEvent();
		try {
			final boolean isResultSet = jdbcStatement.execute();
			currentReturnState = buildCurrentReturnState( isResultSet );
		}
		catch (SQLException e) {
			throw convert( e, "Error calling CallableStatement.getMoreResults" );
		}
		finally {
			eventManager.completeJdbcPreparedStatementExecutionEvent( jdbcPreparedStatementExecutionEvent, sql );
			sqlStatementLogger.logSlowQuery( sql, executeStartNanos, this.context.getSession().getJdbcSessionContext() );
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
		return new CurrentReturnState( isResultSet, updateCount );
	}

	protected JDBCException convert(SQLException e, String message) {
		return context.getSession().getJdbcServices().getSqlExceptionHelper().convert(
				e,
				message,
				jdbcStatement.toString()
		);
	}

	@Override
	public Output getCurrent() {
		if ( currentReturnState == null ) {
			return null;
		}
		return currentReturnState.getOutput();
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
		final JdbcCoordinator jdbcCoordinator = context.getSession().getJdbcCoordinator();
		jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( jdbcStatement );
		jdbcCoordinator.afterStatementExecution();
	}

	private List<?> extractCurrentResults() {
		try {
			return extractResults( jdbcStatement.getResultSet() );
		}
		catch (SQLException e) {
			throw convert( e, "Error calling CallableStatement.getResultSet" );
		}
	}

	protected List<Object> extractResults(ResultSet resultSet) {

		final DirectResultSetAccess resultSetAccess = new DirectResultSetAccess(
				context.getSession(),
				jdbcStatement,
				resultSet
		);

		final ProcedureCallImpl<?> procedureCall = (ProcedureCallImpl<?>) context;
		final ResultSetMapping resultSetMapping = procedureCall.getResultSetMapping();

		final ExecutionContext executionContext = new OutputsExecutionContext( context.getSession() );

		final JdbcValues jdbcValues = new JdbcValuesResultSetImpl(
				resultSetAccess,
				null,
				null,
				this.context.getQueryOptions(),
				true,
				resultSetMapping.resolve( resultSetAccess, context.getSession().getLoadQueryInfluencers(), getSessionFactory() ),
				null,
				executionContext
		);

		try {

			//noinspection unchecked
			final RowReader<Object> rowReader = (RowReader<Object>) ResultsHelper.createRowReader(
					getSessionFactory(),
					RowTransformerStandardImpl.INSTANCE,
					null,
					jdbcValues
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
					new JdbcValuesSourceProcessingStateStandardImpl(
							executionContext,
							processingOptions
					);
			final ArrayList<Object> results = new ArrayList<>();
			final RowProcessingStateStandardImpl rowProcessingState = new RowProcessingStateStandardImpl(
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
						&& results.get( 0 ) instanceof ResultSet ) {
					// When calling a function that returns a ref_cursor with as table function,
					// we have to unnest the ResultSet manually here
					return extractResults( (ResultSet) results.get( 0 ) );
				}
				return results;
			}
			finally {
				rowReader.finishUp( rowProcessingState );
				jdbcValuesSourceProcessingState.finishUp( results.size() > 1 );
			}
		}
		finally {
			jdbcValues.finishUp( this.context.getSession() );
		}
	}

	private SessionFactoryImplementor getSessionFactory() {
		return context.getSession().getFactory();
	}

	/**
	 * Encapsulates the information needed to interpret the current return within a result
	 */
	protected class CurrentReturnState {
		private final boolean isResultSet;
		private final int updateCount;

		private Output rtn;

		protected CurrentReturnState(boolean isResultSet, int updateCount) {
			this.isResultSet = isResultSet;
			this.updateCount = updateCount;
		}

		public boolean indicatesMoreOutputs() {
			return isResultSet() || getUpdateCount() >= 0;
		}

		public boolean isResultSet() {
			return isResultSet;
		}

		public int getUpdateCount() {
			return updateCount;
		}

		public Output getOutput() {
			if ( rtn == null ) {
				rtn = buildOutput();
			}
			return rtn;
		}

		protected Output buildOutput() {
			if ( log.isDebugEnabled() ) {
				log.debugf(
						"Building Return [isResultSet=%s, updateCount=%s, extendedReturn=%s]",
						isResultSet(),
						getUpdateCount(),
						hasExtendedReturns()
				);
			}

			if ( isResultSet() ) {
				return buildResultSetOutput( extractCurrentResults() );
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

		// hooks for stored procedure (out param) processing ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		protected Output buildResultSetOutput(List<?> list) {
			return new ResultSetOutputImpl( list );
		}

		protected Output buildResultSetOutput(Supplier<List<?>> listSupplier) {
			return new ResultSetOutputImpl( listSupplier );
		}

		protected Output buildUpdateCountOutput(int updateCount) {
			return new UpdateCountOutputImpl( updateCount );
		}

		protected boolean hasExtendedReturns() {
			return false;
		}

		protected Output buildExtendedReturn() {
			throw new IllegalStateException( "State does not define extended returns" );
		}

		protected boolean hasFunctionReturns() {
			return false;
		}

		protected Output buildFunctionReturn() {
			throw new IllegalStateException( "State does not define function returns" );
		}
	}


}
