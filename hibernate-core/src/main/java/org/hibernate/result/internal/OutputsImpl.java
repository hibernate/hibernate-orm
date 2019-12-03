/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.result.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.JDBCException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.internal.CoreLogging;
import org.hibernate.result.NoMoreReturnsException;
import org.hibernate.result.Output;
import org.hibernate.result.Outputs;
import org.hibernate.result.spi.ResultContext;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class OutputsImpl implements Outputs {
	private static final Logger log = CoreLogging.logger( OutputsImpl.class );

	private final ResultContext context;
	private final PreparedStatement jdbcStatement;

	private CurrentReturnState currentReturnState;

	public OutputsImpl(ResultContext context, PreparedStatement jdbcStatement) {
		this.context = context;
		this.jdbcStatement = jdbcStatement;

		try {
			final boolean isResultSet = jdbcStatement.execute();
			currentReturnState = buildCurrentReturnState( isResultSet );
		}
		catch (SQLException e) {
			throw convert( e, "Error calling CallableStatement.getMoreResults" );
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
//		return context.getSession().getJdbcServices().getSqlExceptionHelper().convert(
//				e,
//				message,
//				context.getSql()
//		);
		throw new NotYetImplementedFor6Exception( getClass() );
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
		try {
			jdbcStatement.close();
		}
		catch (SQLException e) {
			log.debug( "Unable to close PreparedStatement", e );
		}
	}

	private List extractCurrentResults() {
		try {
			return extractResults( jdbcStatement.getResultSet() );
		}
		catch (SQLException e) {
			throw convert( e, "Error calling CallableStatement.getResultSet" );
		}
	}

	protected List extractResults(ResultSet resultSet) {
		throw new NotYetImplementedFor6Exception( getClass() );
//		try {
//			return loader.processResultSet( resultSet );
//		}
//		catch (SQLException e) {
//			throw convert( e, "Error extracting results from CallableStatement" );
//		}
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
						"Building Return [isResultSet=%s, updateCount=%s, extendedReturn=%s",
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

			throw new NoMoreReturnsException();
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
