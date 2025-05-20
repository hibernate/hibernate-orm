/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.result.Output;
import org.hibernate.result.internal.OutputsImpl;
import org.hibernate.sql.exec.ExecutionException;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcCallRefCursorExtractor;

import jakarta.persistence.ParameterMode;

/**
 * Implementation of ProcedureResult.  Defines centralized access to all of the results of a procedure call.
 *
 * @author Steve Ebersole
 */
public class ProcedureOutputsImpl extends OutputsImpl implements ProcedureOutputs {
	private final ProcedureCallImpl<?> procedureCall;
	private final CallableStatement callableStatement;

	private final Map<ProcedureParameter<?>, JdbcCallParameterRegistration> parameterRegistrations;
	private final JdbcCallRefCursorExtractor[] refCursorParameters;
	private int refCursorParamIndex;

	ProcedureOutputsImpl(
			ProcedureCallImpl<?> procedureCall,
			Map<ProcedureParameter<?>, JdbcCallParameterRegistration> parameterRegistrations,
			JdbcCallRefCursorExtractor[] refCursorParameters,
			CallableStatement callableStatement,
			String sql) {
		super( procedureCall, callableStatement, sql );
		this.procedureCall = procedureCall;
		this.callableStatement = callableStatement;
		this.parameterRegistrations = parameterRegistrations;
		this.refCursorParameters = refCursorParameters;
		executeStatement();
	}

	@Override
	public <T> T getOutputParameterValue(ProcedureParameter<T> parameter) {
		if ( parameter.getMode() == ParameterMode.IN ) {
			throw new ParameterMisuseException( "IN parameter not valid for output extraction" );
		}
		final JdbcCallParameterRegistration registration = parameterRegistrations.get( parameter );
		if ( registration == null ) {
			throw new IllegalArgumentException( "Parameter [" + parameter + "] is not registered with this procedure call" );
		}
		try {
			if ( registration.getParameterMode() == ParameterMode.REF_CURSOR ) {
				//noinspection unchecked
				return (T) registration.getRefCursorExtractor().extractResultSet(
						callableStatement,
						procedureCall.getSession()
				);
			}
			else {
				//noinspection unchecked
				return (T) registration.getParameterExtractor().extractValue(
						callableStatement,
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
	protected CurrentReturnState buildCurrentReturnState(boolean isResultSet, int updateCount) {
		return new ProcedureCurrentReturnState( isResultSet, updateCount, refCursorParamIndex );
	}

	protected class ProcedureCurrentReturnState extends CurrentReturnState {
		private final int refCursorParamIndex;

		private ProcedureCurrentReturnState(boolean isResultSet, int updateCount, int refCursorParamIndex) {
			super( isResultSet, updateCount );
			this.refCursorParamIndex = refCursorParamIndex;
		}

		@Override
		public boolean indicatesMoreOutputs() {
			return super.indicatesMoreOutputs()
					|| ProcedureOutputsImpl.this.refCursorParamIndex < refCursorParameters.length;
		}

		@Override
		protected boolean hasExtendedReturns() {
			return refCursorParamIndex < refCursorParameters.length;
		}

		@Override
		protected Output buildExtendedReturn() {
			final JdbcCallRefCursorExtractor refCursorParam = refCursorParameters[ProcedureOutputsImpl.this.refCursorParamIndex++];
			final ResultSet resultSet = refCursorParam.extractResultSet(
					callableStatement,
					procedureCall.getSession()
			);
			return buildResultSetOutput( () -> extractResults( resultSet ) );
		}

		@Override
		protected boolean hasFunctionReturns() {
			return parameterRegistrations.get( procedureCall.getFunctionReturn() ) != null;
		}

		@Override
		protected Output buildFunctionReturn() {
			final Object result = parameterRegistrations.get( procedureCall.getFunctionReturn() )
					.getParameterExtractor()
					.extractValue(
							callableStatement,
							false,
							procedureCall.getSession()
					);
			final List<Object> results = new ArrayList<>( 1 );
			results.add( result );
			return buildResultSetOutput( () -> results );
		}
	}
}
