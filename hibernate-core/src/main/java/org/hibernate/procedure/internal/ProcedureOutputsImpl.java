/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.result.Output;
import org.hibernate.result.internal.OutputsImpl;

/**
 * Implementation of ProcedureResult.  Defines centralized access to all of the results of a procedure call.
 *
 * @author Steve Ebersole
 */
public class ProcedureOutputsImpl extends OutputsImpl implements ProcedureOutputs {
	private final ProcedureCallImpl procedureCall;
	private final CallableStatement callableStatement;

	private final ParameterRegistrationImplementor[] refCursorParameters;
	private int refCursorParamIndex;

	ProcedureOutputsImpl(ProcedureCallImpl procedureCall, CallableStatement callableStatement) {
		super( procedureCall, callableStatement );
		this.procedureCall = procedureCall;
		this.callableStatement = callableStatement;

		this.refCursorParameters = procedureCall.collectRefCursorParameters();
	}

	@Override
	public <T> T getOutputParameterValue(ParameterRegistration<T> parameterRegistration) {
		return ( (ParameterRegistrationImplementor<T>) parameterRegistration ).extract( callableStatement );
	}

	@Override
	public Object getOutputParameterValue(String name) {
		return procedureCall.getParameterRegistration( name ).extract( callableStatement );
	}

	@Override
	public Object getOutputParameterValue(int position) {
		return procedureCall.getParameterRegistration( position ).extract( callableStatement );
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
					|| ProcedureOutputsImpl.this.refCursorParamIndex < ProcedureOutputsImpl.this.refCursorParameters.length;
		}

		@Override
		protected boolean hasExtendedReturns() {
			return refCursorParamIndex < refCursorParameters.length;
		}

		@Override
		protected Output buildExtendedReturn() {
			ProcedureOutputsImpl.this.refCursorParamIndex++;
			final ParameterRegistrationImplementor refCursorParam = ProcedureOutputsImpl.this.refCursorParameters[refCursorParamIndex];
			ResultSet resultSet;
			if ( refCursorParam.getName() != null ) {
				resultSet = ProcedureOutputsImpl.this.procedureCall.getSession().getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.getResultSet( ProcedureOutputsImpl.this.callableStatement, refCursorParam.getName() );
			}
			else {
				resultSet = ProcedureOutputsImpl.this.procedureCall.getSession().getFactory().getServiceRegistry()
						.getService( RefCursorSupport.class )
						.getResultSet( ProcedureOutputsImpl.this.callableStatement, refCursorParam.getPosition() );
			}
			return buildResultSetOutput( () -> extractResults( resultSet ) );
		}
	}

}
