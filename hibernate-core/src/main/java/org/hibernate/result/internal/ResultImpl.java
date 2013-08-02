/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.result.internal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import org.hibernate.JDBCException;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.loader.custom.sql.SQLQueryReturnProcessor;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.result.NoMoreReturnsException;
import org.hibernate.result.Result;
import org.hibernate.result.ResultSetReturn;
import org.hibernate.result.Return;
import org.hibernate.result.UpdateCountReturn;
import org.hibernate.result.spi.ResultContext;

/**
 * @author Steve Ebersole
 */
public class ResultImpl implements Result {
	private static final Logger log = CoreLogging.logger( ResultImpl.class );

	private final ResultContext context;
	private final PreparedStatement jdbcStatement;
	private final CustomLoaderExtension loader;

	private CurrentReturnState currentReturnState;

	public ResultImpl(ResultContext context, PreparedStatement jdbcStatement) {
		this.context = context;
		this.jdbcStatement = jdbcStatement;

		// For now...  but see the LoadPlan work; eventually this should just be a ResultSetProcessor.
		this.loader = buildSpecializedCustomLoader( context );

		try {
			final boolean isResultSet = jdbcStatement.execute();
			currentReturnState = buildCurrentReturnDescriptor( isResultSet );
		}
		catch (SQLException e) {
			throw convert( e, "Error calling CallableStatement.getMoreResults" );
		}
	}

	private CurrentReturnState buildCurrentReturnDescriptor(boolean isResultSet) {
		int updateCount = -1;
		if ( ! isResultSet ) {
			try {
				updateCount = jdbcStatement.getUpdateCount();
			}
			catch (SQLException e) {
				throw convert( e, "Error calling CallableStatement.getUpdateCount" );
			}
		}

		return buildCurrentReturnDescriptor( isResultSet, updateCount );
	}

	protected CurrentReturnState buildCurrentReturnDescriptor(boolean isResultSet, int updateCount) {
		return new CurrentReturnState( isResultSet, updateCount );
	}

	@Override
	public Return getCurrentReturn() {
		if ( currentReturnState == null ) {
			return null;
		}
		return currentReturnState.getReturn();
	}

	@Override
	public boolean hasMoreReturns() {
		// prepare the next return state
		try {
			final boolean isResultSet = jdbcStatement.getMoreResults();
			currentReturnState = buildCurrentReturnDescriptor( isResultSet );
		}
		catch (SQLException e) {
			throw convert( e, "Error calling CallableStatement.getMoreResults" );
		}

		return currentReturnState != null && currentReturnState.indicatesMoreReturns();
	}

	@Override
	public Return getNextReturn() {
		if ( !hasMoreReturns() ) {
			throw new NoMoreReturnsException( "Results have been exhausted" );
		}

		return getCurrentReturn();
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

	protected JDBCException convert(SQLException e, String message) {
		return context.getSession().getFactory().getSQLExceptionHelper().convert(
				e,
				message,
				context.getSql()
		);
	}

	/**
	 * Encapsulates the information needed to interpret the current return within a result
	 */
	protected class CurrentReturnState {
		private final boolean isResultSet;
		private final int updateCount;

		private Return rtn;

		protected CurrentReturnState(boolean isResultSet, int updateCount) {
			this.isResultSet = isResultSet;
			this.updateCount = updateCount;
		}

		public boolean indicatesMoreReturns() {
			return isResultSet() || getUpdateCount() >= 0;
		}

		public boolean isResultSet() {
			return isResultSet;
		}

		public int getUpdateCount() {
			return updateCount;
		}

		public Return getReturn() {
			if ( rtn == null ) {
				rtn = buildReturn();
			}
			return rtn;
		}

		protected Return buildReturn() {
			if ( log.isDebugEnabled() ) {
				log.debugf(
						"Building Return [isResultSet=%s, updateCount=%s, extendedReturn=%s",
						isResultSet(),
						getUpdateCount(),
						hasExtendedReturns()
				);
			}
			// todo : temporary for tck testing...
			System.out.println(
					String.format(
							"Building Return [isResultSet=%s, updateCount=%s, extendedReturn=%s",
							isResultSet(),
							getUpdateCount(),
							hasExtendedReturns()
					)
			);

			if ( isResultSet() ) {
				return new ResultSetReturnImpl( extractCurrentResults() );
			}
			else if ( getUpdateCount() >= 0 ) {
				return new UpdateCountReturnImpl( updateCount );
			}
			else if ( hasExtendedReturns() ) {
				return buildExtendedReturn();
			}

			throw new NoMoreReturnsException();
		}

		// hooks for stored procedure (out param) processing ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		protected boolean hasExtendedReturns() {
			return false;
		}

		protected Return buildExtendedReturn() {
			throw new IllegalStateException( "State does not define extended returns" );
		}
	}

	protected static class ResultSetReturnImpl implements ResultSetReturn {
		private final List results;

		public ResultSetReturnImpl(List results) {
			this.results = results;
		}

		@Override
		public boolean isResultSet() {
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public List getResultList() {
			return results;
		}

		@Override
		public Object getSingleResult() {
			final List results = getResultList();
			if ( results == null || results.isEmpty() ) {
				return null;
			}
			else {
				return results.get( 0 );
			}
		}
	}

	protected static class UpdateCountReturnImpl implements UpdateCountReturn {
		private final int updateCount;

		public UpdateCountReturnImpl(int updateCount) {
			this.updateCount = updateCount;
		}

		@Override
		public int getUpdateCount() {
			return updateCount;
		}

		@Override
		public boolean isResultSet() {
			return false;
		}
	}

	private static CustomLoaderExtension buildSpecializedCustomLoader(final ResultContext context) {
		// might be better to just manually construct the Return(s).. SQLQueryReturnProcessor does a lot of
		// work that is really unnecessary here.
		final SQLQueryReturnProcessor processor = new SQLQueryReturnProcessor(
				context.getQueryReturns(),
				context.getSession().getFactory()
		);
		processor.process();
		final List<org.hibernate.loader.custom.Return> customReturns = processor.generateCustomReturns( false );

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
			public Map getNamedParameterBindPoints() {
				// no named parameters in terms of embedded in the SQL string
				return null;
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
		private QueryParameters queryParameters;
		private SessionImplementor session;

		private boolean needsDiscovery = true;

		public CustomLoaderExtension(
				CustomQuery customQuery,
				QueryParameters queryParameters,
				SessionImplementor session) {
			super( customQuery, session.getFactory() );
			this.queryParameters = queryParameters;
			this.session = session;
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
					Collections.<AfterLoadAction>emptyList()
			);
		}
	}
}
