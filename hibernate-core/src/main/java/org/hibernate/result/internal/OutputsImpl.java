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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.JDBCException;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.RootReturn;
import org.hibernate.loader.custom.sql.SQLQueryReturnProcessor;
import org.hibernate.param.ParameterBinder;
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
	private final CustomLoaderExtension loader;

	private CurrentReturnState currentReturnState;

	public OutputsImpl(ResultContext context, PreparedStatement jdbcStatement) {
		this.context = context;
		this.jdbcStatement = jdbcStatement;

		// For now...  but see the LoadPlan work; eventually this should just be a ResultSetProcessor.
		this.loader = buildSpecializedCustomLoader( context );

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
		return context.getSession().getJdbcServices().getSqlExceptionHelper().convert(
				e,
				message,
				context.getSql()
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
		try {
			return loader.processResultSet( resultSet );
		}
		catch (SQLException e) {
			throw convert( e, "Error extracting results from CallableStatement" );
		}
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Hooks into Hibernate's Loader hierarchy for ResultSet -> Object mapping

	private static CustomLoaderExtension buildSpecializedCustomLoader(final ResultContext context) {
		// might be better to just manually construct the Return(s).. SQLQueryReturnProcessor does a lot of
		// work that is really unnecessary here.
		final SQLQueryReturnProcessor processor = new SQLQueryReturnProcessor(
				context.getQueryReturns(),
				context.getSession().getFactory()
		);
		processor.process();
		final List<org.hibernate.loader.custom.Return> customReturns = processor.generateCallableReturns();

		CustomQuery customQuery = new CustomQuery() {
			@Override
			public String getSQL() {
				return context.getSql();
			}

			@Override
			public Set<String> getQuerySpaces() {
				return context.getSynchronizedQuerySpaces();
			}

			@Override
			public List<ParameterBinder> getParameterValueBinders() {
				// no parameters in terms of embedded in the SQL string
				return Collections.emptyList();
			}

			@Override
			public List<org.hibernate.loader.custom.Return> getCustomQueryReturns() {
				return customReturns;
			}
		};

		return new CustomLoaderExtension(
				customQuery,
				context.getQueryParameters(),
				context.getSession()
		);
	}

	private static class CustomLoaderExtension extends CustomLoader {
		private static final EntityAliases[] NO_ALIASES = new EntityAliases[0];

		private final QueryParameters queryParameters;
		private final SharedSessionContractImplementor session;
		private final EntityAliases[] entityAliases;

		private boolean needsDiscovery = true;

		public CustomLoaderExtension(
				CustomQuery customQuery,
				QueryParameters queryParameters,
				SharedSessionContractImplementor session) {
			super( customQuery, session.getFactory() );
			this.queryParameters = queryParameters;
			this.session = session;

			entityAliases = interpretEntityAliases( customQuery.getCustomQueryReturns() );
		}

		private EntityAliases[] interpretEntityAliases(List<Return> customQueryReturns) {
			final List<EntityAliases> entityAliases = new ArrayList<>();
			for ( Return queryReturn : customQueryReturns ) {
				if ( !RootReturn.class.isInstance( queryReturn ) ) {
					continue;
				}

				entityAliases.add( ( (RootReturn) queryReturn ).getEntityAliases() );
			}

			if ( entityAliases.isEmpty() ) {
				return NO_ALIASES;
			}

			return entityAliases.toArray( new EntityAliases[ entityAliases.size() ] );
		}

		@Override
		protected EntityAliases[] getEntityAliases() {
			return entityAliases;
		}


		// todo : this would be a great way to add locking to stored procedure support (at least where returning entities).

		public List processResultSet(ResultSet resultSet) throws SQLException {
			if ( needsDiscovery ) {
				super.autoDiscoverTypes( resultSet );
				// todo : EntityAliases discovery
				needsDiscovery = false;
			}
			return super.processResultSet(
					resultSet,
					queryParameters,
					session,
					true,
					null,
					Integer.MAX_VALUE,
					Collections.emptyList()
			);
		}
	}
}
