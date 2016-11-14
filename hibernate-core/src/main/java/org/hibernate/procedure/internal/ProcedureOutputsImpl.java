/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.ParameterMode;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.procedure.ParameterMisuseException;
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

	private final boolean shouldUseJdbcNamedParameters;
	private final Map<ParameterRegistration, JdbcParameterExtractor> jdbcParameterExtractorMap;
	private final Iterator<JdbcRefCursorExtractor> jdbcRefCursorExtractorIterator;

	private int refCursorParamIndex;

	ProcedureOutputsImpl(
			ProcedureCallImpl procedureCall,
			CallableStatement callableStatement,
			boolean shouldUseJdbcNamedParameters,
			Map<ParameterRegistration, JdbcParameterExtractor> jdbcParameterExtractorMap,
			List<JdbcRefCursorExtractor> jdbcRefCursorExtractors) {
		super( procedureCall, callableStatement );
		this.procedureCall = procedureCall;
		this.callableStatement = callableStatement;
		this.shouldUseJdbcNamedParameters = shouldUseJdbcNamedParameters;

		this.jdbcParameterExtractorMap = jdbcParameterExtractorMap;
		this.jdbcRefCursorExtractorIterator = jdbcRefCursorExtractors.iterator();
	}

	@Override
	public <T> T getOutputParameterValue(ParameterRegistration<T> parameterRegistration) {
		final JdbcParameterExtractor<T> extractor = resolveJdbcParameterExtractor( parameterRegistration );
		return extractor.extractValue( callableStatement, shouldUseJdbcNamedParameters, procedureCall.getSession() );
	}

	@SuppressWarnings("unchecked")
	private <T> JdbcParameterExtractor<T> resolveJdbcParameterExtractor(ParameterRegistration<T> parameterRegistration) {
		final JdbcParameterExtractor<T> extractor = jdbcParameterExtractorMap.get( parameterRegistration );
		if ( extractor == null ) {
			if ( parameterRegistration.getMode() == ParameterMode.IN ) {
				throw new ParameterMisuseException( "Cannot extract value from parameter registered as IN parameter" );
			}
			if ( parameterRegistration.getMode() == ParameterMode.REF_CURSOR ) {
				throw new ParameterMisuseException( "Cannot extract value from parameter registered as REF_CURSOR parameter" );
			}
			throw new AssertionFailure( "Could not locate JdbcParameterExtractor for given ParameterRegistration :  " + parameterRegistration );
		}
		return extractor;
	}

	@Override
	public Object getOutputParameterValue(String name) {
		return getOutputParameterValue( procedureCall.getParameterRegistration( name ) );
	}

	@Override
	public Object getOutputParameterValue(int position) {
		return getOutputParameterValue( procedureCall.getParameterRegistration( position ) );
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
			return super.indicatesMoreOutputs() || jdbcRefCursorExtractorIterator.hasNext();
		}

		@Override
		protected boolean hasExtendedReturns() {
			return jdbcRefCursorExtractorIterator.hasNext();
		}

		@Override
		protected Output buildExtendedReturn() {
			if ( !jdbcRefCursorExtractorIterator.hasNext() ) {
				throw new IllegalStateException( "No more REF_CURSOR results to process" );
			}

			final JdbcRefCursorExtractor next = jdbcRefCursorExtractorIterator.next();
			return buildResultSetOutput(
					next.extractResults(
							callableStatement,
							procedureCall.getSession(),
							getLoader()
					)
			);

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
