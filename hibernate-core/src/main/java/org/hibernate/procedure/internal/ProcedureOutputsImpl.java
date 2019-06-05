/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.metamodel.model.domain.AllowableOutputParameterType;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.result.Output;
import org.hibernate.result.internal.OutputsImpl;
import org.hibernate.sql.exec.ExecutionException;

/**
 * Implementation of ProcedureResult.  Defines centralized access to all of the results of a procedure call.
 *
 * @author Steve Ebersole
 */
public class ProcedureOutputsImpl extends OutputsImpl implements ProcedureOutputs {
	private final ProcedureCallImpl procedureCall;
	private final CallableStatement callableStatement;

	private final ProcedureParameterImplementor[] refCursorParameters;
	private int refCursorParamIndex;

	ProcedureOutputsImpl(ProcedureCallImpl procedureCall, CallableStatement callableStatement) {
		super( procedureCall, callableStatement );
		this.procedureCall = procedureCall;
		this.callableStatement = callableStatement;

		this.refCursorParameters = procedureCall.collectRefCursorParameters();
	}

	@Override
	public <T> T getOutputParameterValue(ProcedureParameter<T> parameter) {
		final AllowableParameterType<T> hibernateType = parameter.getHibernateType();
		if ( hibernateType instanceof AllowableOutputParameterType<?> ) {
			try {
				//noinspection unchecked
				return (T) ( (AllowableOutputParameterType<?>) hibernateType ).extract(
						callableStatement,
						parameter.getPosition(),
						procedureCall.getSession()
				);
			}
			catch (SQLException e) {
				throw new ExecutionException( "Error extracting procedure output parameter value [" + parameter + "]", e );
			}
		}
		else {
			throw new ParameterMisuseException( "Parameter type cannot extract procedure output parameters" );
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
					|| ProcedureOutputsImpl.this.refCursorParamIndex < ProcedureOutputsImpl.this.refCursorParameters.length;
		}

		@Override
		protected boolean hasExtendedReturns() {
			return refCursorParamIndex < refCursorParameters.length;
		}

		@Override
		protected Output buildExtendedReturn() {
			ProcedureOutputsImpl.this.refCursorParamIndex++;
			final ProcedureParameterImplementor refCursorParam = ProcedureOutputsImpl.this.refCursorParameters[refCursorParamIndex];
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
