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
import org.hibernate.procedure.ProcedureResult;
import org.hibernate.result.Return;
import org.hibernate.result.internal.ResultImpl;

/**
 * Implementation of ProcedureResult.  Defines centralized access to all of the results of a procedure call.
 *
 * @author Steve Ebersole
 */
public class ProcedureResultImpl extends ResultImpl implements ProcedureResult {
	private final ProcedureCallImpl procedureCall;
	private final CallableStatement callableStatement;

	private final ParameterRegistrationImplementor[] refCursorParameters;
	private int refCursorParamIndex;

	ProcedureResultImpl(ProcedureCallImpl procedureCall, CallableStatement callableStatement) {
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
	protected CurrentReturnState buildCurrentReturnDescriptor(boolean isResultSet, int updateCount) {
		return new ProcedureCurrentReturnState( isResultSet, updateCount, refCursorParamIndex );
	}

	protected boolean hasMoreReturns(CurrentReturnState descriptor) {
		return super.hasMoreReturns( descriptor )
				|| ( (ProcedureCurrentReturnState) descriptor ).refCursorParamIndex < refCursorParameters.length;
	}

	@Override
	protected boolean hasExtendedReturns(CurrentReturnState currentReturnState) {
		return ProcedureCurrentReturnState.class.isInstance( currentReturnState )
				&& ( (ProcedureCurrentReturnState) currentReturnState ).refCursorParamIndex < refCursorParameters.length;
	}

	@Override
	protected Return buildExtendedReturn(CurrentReturnState returnDescriptor) {
		this.refCursorParamIndex++;
		final int refCursorParamIndex = ( (ProcedureCurrentReturnState) returnDescriptor ).refCursorParamIndex;
		final ParameterRegistrationImplementor refCursorParam = refCursorParameters[refCursorParamIndex];
		ResultSet resultSet;
		if ( refCursorParam.getName() != null ) {
			resultSet = procedureCall.getSession().getFactory().getServiceRegistry()
					.getService( RefCursorSupport.class )
					.getResultSet( callableStatement, refCursorParam.getName() );
		}
		else {
			resultSet = procedureCall.getSession().getFactory().getServiceRegistry()
					.getService( RefCursorSupport.class )
					.getResultSet( callableStatement, refCursorParam.getPosition() );
		}
		return new ResultSetReturnImpl( extractResults( resultSet ) );
	}

	protected class ProcedureCurrentReturnState extends CurrentReturnState {
		private final int refCursorParamIndex;

		private ProcedureCurrentReturnState(boolean isResultSet, int updateCount, int refCursorParamIndex) {
			super( isResultSet, updateCount );
			this.refCursorParamIndex = refCursorParamIndex;
		}
	}

}
