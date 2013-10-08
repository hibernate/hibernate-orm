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
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;

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
			return buildResultSetOutput( extractResults( resultSet ) );
		}
	}

}
