/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.result.internal;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.query.sql.internal.ResultSetMappingDescriptorUndefined;
import org.hibernate.result.NoMoreOutputsException;
import org.hibernate.result.Output;
import org.hibernate.result.Outputs;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.internal.Helper;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.ParameterBindingContext;
import org.hibernate.sql.results.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.values.DirectResultSetAccess;
import org.hibernate.sql.results.internal.values.JdbcValuesResultSetImpl;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;
import org.hibernate.sql.results.spi.RowReader;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class OutputsImpl
		implements Outputs, ExecutionContext, AssemblerCreationContext, ParameterBindingContext, Callback {
	private static final Logger log = CoreLogging.logger( OutputsImpl.class );

	private final ResultContext context;

	private CurrentReturnState currentReturnState;

	private ResultSetMappingDescriptor currentResultSetMapping;
	private int currentResultSetMappingIndex = -1;


	public OutputsImpl(ResultContext context) {
		this.context = context;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return context.getSession().getSessionFactory();
	}

	protected void prime(PreparedStatement jdbcStatement) {
		try {
			final boolean isResultSet = jdbcStatement.execute();
			currentReturnState = buildCurrentReturnState( isResultSet, jdbcStatement );
		}
		catch (SQLException e) {
			throw context.convertException( e, "Error calling CallableStatement.getMoreResults" );
		}
	}

	protected CurrentReturnState buildCurrentReturnState(boolean isResultSet, PreparedStatement jdbcStatement) {
		int updateCount = -1;
		if ( ! isResultSet ) {
			try {
				updateCount = jdbcStatement.getUpdateCount();
			}
			catch (SQLException e) {
				throw context.convertException( e, "Error calling CallableStatement.getUpdateCount" );
			}
		}

		return buildCurrentReturnState( isResultSet, updateCount, jdbcStatement );
	}

	protected CurrentReturnState buildCurrentReturnState(boolean isResultSet, int updateCount, PreparedStatement jdbcStatement) {
		return new CurrentReturnState( isResultSet, updateCount, jdbcStatement );
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
				final boolean isResultSet = nextResult();
				currentReturnState = buildCurrentReturnState( isResultSet );
			}
			catch (SQLException e) {
				throw context.convertException( e, "Error calling CallableStatement.getMoreResults" );
			}
		}

		// and return
		return currentReturnState != null && currentReturnState.indicatesMoreOutputs();
	}

	protected abstract CurrentReturnState buildCurrentReturnState(boolean isResultSet) throws SQLException;

	protected abstract boolean nextResult() throws SQLException;


	private List extractCurrentResults(PreparedStatement jdbcStatement) {
		try {
			return extractResults( jdbcStatement.getResultSet(), jdbcStatement );
		}
		catch (SQLException e) {
			throw context.convertException( e, "Error calling CallableStatement.getResultSet" );
		}
	}

	protected List extractResults(ResultSet resultSet, PreparedStatement jdbcStatement) {
		final DirectResultSetAccess resultSetAccess = new DirectResultSetAccess( context.getSession(), jdbcStatement, resultSet );
		final ResultSetMapping resultSetMapping = resolveResultSetMapping( resultSetAccess );
		final JdbcValuesResultSetImpl jdbcValuesSource = new JdbcValuesResultSetImpl(
				resultSetAccess,
				null,
				context.getQueryOptions(),
				resultSetMapping,
				context
		);

		final RowReader<Object[]> rowReader = Helper.createRowReader(
				getSessionFactory(),
				this,
				row -> row,
				jdbcValuesSource
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
				new JdbcValuesSourceProcessingStateStandardImpl( context, processingOptions );

		final RowProcessingStateStandardImpl rowProcessingState = new RowProcessingStateStandardImpl(
				jdbcValuesSourceProcessingState,
				context.getQueryOptions(),
				rowReader, jdbcValuesSource
		);

		try {
			final List results = new ArrayList<>();
			while ( rowProcessingState.next() ) {
				results.add( rowReader.readRow( rowProcessingState, processingOptions ) );
				rowProcessingState.finishRowProcessing();
			}
			return results;
		}
		catch (SQLException e) {
			throw context.convertException( e, "Error processing return rows" );
		}
		finally {
			rowReader.finishUp( jdbcValuesSourceProcessingState );
			jdbcValuesSourceProcessingState.finishUp();
			jdbcValuesSource.finishUp();
		}
	}


	private ResultSetMapping resolveResultSetMapping(DirectResultSetAccess resultSetAccess) {
		currentResultSetMappingIndex++;
		if ( context.getResultSetMappings() == null || context.getResultSetMappings().isEmpty() ) {
			if ( currentResultSetMapping == null ) {
				currentResultSetMapping = new ResultSetMappingDescriptorUndefined();
			}
		}
		else {
			if ( currentResultSetMappingIndex >= context.getResultSetMappings().size() ) {
				throw new HibernateException(
						"Needed ResultSetMapping for ResultSet #%s, but query defined only %s ResultSetMapping(s)" );
			}
			currentResultSetMapping = context.getResultSetMappings().get( currentResultSetMappingIndex );
		}

		return currentResultSetMapping.resolve( resultSetAccess, getSessionFactory() );
	}

	/**
	 * Encapsulates the information needed to interpret the current return within a result
	 */
	protected class CurrentReturnState {
		private final boolean isResultSet;
		private final int updateCount;
		private final PreparedStatement jdbcStatement;

		private Output rtn;

		public CurrentReturnState(boolean isResultSet, int updateCount, PreparedStatement jdbcStatement) {
			this.isResultSet = isResultSet;
			this.updateCount = updateCount;
			this.jdbcStatement = jdbcStatement;
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
						"Building Return [isResultSet=%s, updateCount=%s, extendedReturn=%s",
						isResultSet(),
						getUpdateCount(),
						hasExtendedReturns()
				);
			}

			if ( isResultSet() ) {
				return buildResultSetOutput( extractCurrentResults( jdbcStatement ) );
			}
			else if ( getUpdateCount() >= 0 ) {
				return buildUpdateCountOutput( updateCount );
			}
			else if ( hasExtendedReturns() ) {
				return buildExtendedReturn();
			}

			throw new NoMoreOutputsException();
		}

		// hooks for stored procedure (out param) processing ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		protected Output buildResultSetOutput(List list) {
			return new ResultSetOutputImpl( list );
		}

		protected Output buildResultSetOutput(Supplier<List> listSupplier) {
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
	}
}
