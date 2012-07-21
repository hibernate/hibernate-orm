/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.internal;

import javax.persistence.ParameterMode;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.JDBCException;
import org.hibernate.StoredProcedureCall.StoredProcedureParameter;
import org.hibernate.StoredProcedureOutputs;
import org.hibernate.StoredProcedureResultSetReturn;
import org.hibernate.StoredProcedureReturn;
import org.hibernate.StoredProcedureUpdateCountReturn;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.StoredProcedureCallImpl.StoredProcedureParameterImplementor;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.loader.custom.Return;
import org.hibernate.loader.custom.sql.SQLQueryReturnProcessor;
import org.hibernate.service.jdbc.cursor.spi.RefCursorSupport;

/**
 * @author Steve Ebersole
 */
public class StoredProcedureOutputsImpl implements StoredProcedureOutputs {
	private final StoredProcedureCallImpl procedureCall;
	private final CallableStatement callableStatement;

	private final StoredProcedureParameterImplementor[] refCursorParameters;
	private final CustomLoaderExtension loader;

	private CurrentReturnDescriptor currentReturnDescriptor;

	private boolean executed = false;
	private int refCursorParamIndex = 0;

	StoredProcedureOutputsImpl(StoredProcedureCallImpl procedureCall, CallableStatement callableStatement) {
		this.procedureCall = procedureCall;
		this.callableStatement = callableStatement;

		this.refCursorParameters = procedureCall.collectRefCursorParameters();
		// For now...
		this.loader = buildSpecializedCustomLoader( procedureCall );
	}

	@Override
	public Object getOutputParameterValue(String name) {
		return procedureCall.getRegisteredParameter( name ).extract( callableStatement );
	}

	@Override
	public Object getOutputParameterValue(int position) {
		return procedureCall.getRegisteredParameter( position ).extract( callableStatement );
	}

	@Override
	public boolean hasMoreReturns() {
		if ( currentReturnDescriptor == null ) {
			final boolean isResultSet;

			if ( executed ) {
				try {
					isResultSet = callableStatement.getMoreResults();
				}
				catch (SQLException e) {
					throw convert( e, "Error calling CallableStatement.getMoreResults" );
				}
			}
			else {
				try {
					isResultSet = callableStatement.execute();
				}
				catch (SQLException e) {
					throw convert( e, "Error calling CallableStatement.execute" );
				}
				executed = true;
			}

			int updateCount = -1;
			if ( ! isResultSet ) {
				try {
					updateCount = callableStatement.getUpdateCount();
				}
				catch (SQLException e) {
					throw convert( e, "Error calling CallableStatement.getUpdateCount" );
				}
			}

			currentReturnDescriptor = new CurrentReturnDescriptor( isResultSet, updateCount, refCursorParamIndex );
		}

		return hasMoreResults( currentReturnDescriptor );
	}

	private boolean hasMoreResults(CurrentReturnDescriptor descriptor) {
		return descriptor.isResultSet
				|| descriptor.updateCount >= 0
				|| descriptor.refCursorParamIndex < refCursorParameters.length;
	}

	@Override
	public StoredProcedureReturn getNextReturn() {
		if ( currentReturnDescriptor == null ) {
			if ( executed ) {
				throw new IllegalStateException( "Unexpected condition" );
			}
			else {
				throw new IllegalStateException( "hasMoreReturns() not called before getNextReturn()" );
			}
		}

		if ( ! hasMoreResults( currentReturnDescriptor ) ) {
			throw new IllegalStateException( "Results have been exhausted" );
		}

		CurrentReturnDescriptor copyReturnDescriptor = currentReturnDescriptor;
		currentReturnDescriptor = null;

		if ( copyReturnDescriptor.isResultSet ) {
			try {
				return new ResultSetReturn( this, callableStatement.getResultSet() );
			}
			catch (SQLException e) {
				throw convert( e, "Error calling CallableStatement.getResultSet" );
			}
		}
		else if ( copyReturnDescriptor.updateCount >= 0 ) {
			return new UpdateCountReturn( this, copyReturnDescriptor.updateCount );
		}
		else {
			this.refCursorParamIndex++;
			ResultSet resultSet;
			int refCursorParamIndex = copyReturnDescriptor.refCursorParamIndex;
			StoredProcedureParameterImplementor refCursorParam = refCursorParameters[refCursorParamIndex];
			if ( refCursorParam.getName() != null ) {
				resultSet = procedureCall.session().getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.getResultSet( callableStatement, refCursorParam.getName() );
			}
			else {
				resultSet = procedureCall.session().getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.getResultSet( callableStatement, refCursorParam.getPosition() );
			}
			return new ResultSetReturn( this, resultSet );
		}
	}

	protected JDBCException convert(SQLException e, String message) {
		return procedureCall.session().getFactory().getSQLExceptionHelper().convert(
				e,
				message,
				procedureCall.getProcedureName()
		);
	}

	private static class CurrentReturnDescriptor {
		private final boolean isResultSet;
		private final int updateCount;
		private final int refCursorParamIndex;

		private CurrentReturnDescriptor(boolean isResultSet, int updateCount, int refCursorParamIndex) {
			this.isResultSet = isResultSet;
			this.updateCount = updateCount;
			this.refCursorParamIndex = refCursorParamIndex;
		}
	}

	private static class ResultSetReturn implements StoredProcedureResultSetReturn {
		private final StoredProcedureOutputsImpl storedProcedureOutputs;
		private final ResultSet resultSet;

		public ResultSetReturn(StoredProcedureOutputsImpl storedProcedureOutputs, ResultSet resultSet) {
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

	private class UpdateCountReturn implements StoredProcedureUpdateCountReturn {
		private final StoredProcedureOutputsImpl storedProcedureOutputs;
		private final int updateCount;

		public UpdateCountReturn(StoredProcedureOutputsImpl storedProcedureOutputs, int updateCount) {
			this.storedProcedureOutputs = storedProcedureOutputs;
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

	private static CustomLoaderExtension buildSpecializedCustomLoader(final StoredProcedureCallImpl procedureCall) {
		final SQLQueryReturnProcessor processor = new SQLQueryReturnProcessor(
				procedureCall.getQueryReturns(),
				procedureCall.session().getFactory()
		);
		processor.process();
		final List<Return> customReturns = processor.generateCustomReturns( false );

		CustomQuery customQuery = new CustomQuery() {
			@Override
			public String getSQL() {
				return procedureCall.getProcedureName();
			}

			@Override
			public Set<String> getQuerySpaces() {
				return procedureCall.getSynchronizedQuerySpacesSet();
			}

			@Override
			public Map getNamedParameterBindPoints() {
				// no named parameters in terms of embedded in the SQL string
				return null;
			}

			@Override
			public List<Return> getCustomQueryReturns() {
				return customReturns;
			}
		};

		return new CustomLoaderExtension(
				customQuery,
				procedureCall.buildQueryParametersObject(),
				procedureCall.session()
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

		public List processResultSet(ResultSet resultSet) throws SQLException {
			super.autoDiscoverTypes( resultSet );
			return super.processResultSet(
					resultSet,
					queryParameters,
					session,
					true,
					null,
					Integer.MAX_VALUE
			);
		}
	}
}
