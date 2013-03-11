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

import org.hibernate.JDBCException;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.loader.custom.sql.SQLQueryReturnProcessor;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.result.NoMoreReturnsException;
import org.hibernate.result.Result;
import org.hibernate.result.Return;
import org.hibernate.result.spi.ResultContext;

/**
 * @author Steve Ebersole
 */
public class ResultImpl implements Result {
	private final ResultContext context;
	private final PreparedStatement jdbcStatement;
	private final CustomLoaderExtension loader;

	private CurrentReturnDescriptor currentReturnDescriptor;

	private boolean executed = false;

	public ResultImpl(ResultContext context, PreparedStatement jdbcStatement) {
		this.context = context;
		this.jdbcStatement = jdbcStatement;

		// For now...
		this.loader = buildSpecializedCustomLoader( context );
	}

	@Override
	public boolean hasMoreReturns() {
		if ( currentReturnDescriptor == null ) {
			final boolean isResultSet;

			if ( executed ) {
				try {
					isResultSet = jdbcStatement.getMoreResults();
				}
				catch (SQLException e) {
					throw convert( e, "Error calling CallableStatement.getMoreResults" );
				}
			}
			else {
				try {
					isResultSet = jdbcStatement.execute();
				}
				catch (SQLException e) {
					throw convert( e, "Error calling CallableStatement.execute" );
				}
				executed = true;
			}

			int updateCount = -1;
			if ( ! isResultSet ) {
				try {
					updateCount = jdbcStatement.getUpdateCount();
				}
				catch (SQLException e) {
					throw convert( e, "Error calling CallableStatement.getUpdateCount" );
				}
			}

			currentReturnDescriptor = buildCurrentReturnDescriptor( isResultSet, updateCount );
		}

		return hasMoreReturns( currentReturnDescriptor );
	}

	protected CurrentReturnDescriptor buildCurrentReturnDescriptor(boolean isResultSet, int updateCount) {
		return new CurrentReturnDescriptor( isResultSet, updateCount );
	}

	protected boolean hasMoreReturns(CurrentReturnDescriptor descriptor) {
		return descriptor.isResultSet
				|| descriptor.updateCount >= 0;
	}

	@Override
	public Return getNextReturn() {
		if ( currentReturnDescriptor == null ) {
			if ( executed ) {
				throw new IllegalStateException( "Unexpected condition" );
			}
			else {
				throw new IllegalStateException( "hasMoreReturns() not called before getNextReturn()" );
			}
		}

		if ( ! hasMoreReturns( currentReturnDescriptor ) ) {
			throw new NoMoreReturnsException( "Results have been exhausted" );
		}

		CurrentReturnDescriptor copyReturnDescriptor = currentReturnDescriptor;
		currentReturnDescriptor = null;

		if ( copyReturnDescriptor.isResultSet ) {
			try {
				return new ResultSetReturn( this, jdbcStatement.getResultSet() );
			}
			catch (SQLException e) {
				throw convert( e, "Error calling CallableStatement.getResultSet" );
			}
		}
		else if ( copyReturnDescriptor.updateCount >= 0 ) {
			return new UpdateCountReturn( this, copyReturnDescriptor.updateCount );
		}
		else {
			return buildExtendedReturn( copyReturnDescriptor );
		}
	}

	protected Return buildExtendedReturn(CurrentReturnDescriptor copyReturnDescriptor) {
		throw new NoMoreReturnsException( "Results have been exhausted" );
	}

	protected JDBCException convert(SQLException e, String message) {
		return context.getSession().getFactory().getSQLExceptionHelper().convert(
				e,
				message,
				context.getSql()
		);
	}

	protected static class CurrentReturnDescriptor {
		private final boolean isResultSet;
		private final int updateCount;

		protected CurrentReturnDescriptor(boolean isResultSet, int updateCount) {
			this.isResultSet = isResultSet;
			this.updateCount = updateCount;
		}
	}

	protected static class ResultSetReturn implements org.hibernate.result.ResultSetReturn {
		private final ResultImpl storedProcedureOutputs;
		private final ResultSet resultSet;

		public ResultSetReturn(ResultImpl storedProcedureOutputs, ResultSet resultSet) {
			this.storedProcedureOutputs = storedProcedureOutputs;
			this.resultSet = resultSet;
		}

		@Override
		public boolean isResultSet() {
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public List getResultList() {
			try {
				return storedProcedureOutputs.loader.processResultSet( resultSet );
			}
			catch (SQLException e) {
				throw storedProcedureOutputs.convert( e, "Error calling ResultSet.next" );
			}
		}

		@Override
		public Object getSingleResult() {
			List results = getResultList();
			if ( results == null || results.isEmpty() ) {
				return null;
			}
			else {
				return results.get( 0 );
			}
		}
	}

	protected static class UpdateCountReturn implements org.hibernate.result.UpdateCountReturn {
		private final ResultImpl result;
		private final int updateCount;

		public UpdateCountReturn(ResultImpl result, int updateCount) {
			this.result = result;
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
			super.autoDiscoverTypes( resultSet );
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
