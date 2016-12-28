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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.JDBCException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.NoMoreReturnsException;
import org.hibernate.procedure.Output;
import org.hibernate.procedure.ParameterRegistration;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.exec.spi.JdbcCall;
import org.hibernate.sql.exec.spi.JdbcCallParameterExtractor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcCallRefCursorExtractor;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.RowTransformer;

import org.jboss.logging.Logger;

/**
 * Implementation of ProcedureResult.  Defines centralized access to all of the results of a procedure call.
 *
 * @author Steve Ebersole
 */
public class ProcedureOutputsImpl implements ProcedureOutputs {
	private static final Logger log = Logger.getLogger( ProcedureOutputsImpl.class );

	private final ParameterStrategy parameterStrategy;
	private final SharedSessionContractImplementor persistenceContext;
	private final ProcedureCallImpl procedureCall;
	private final JdbcCall jdbcCall;

	private final String callableName;
	private final String callString;
	private final CallableStatement callableStatement;

	private CurrentOutputState currentOutputState;

	private Map<String, JdbcCallParameterExtractor> parameterExtractorMap;
	private Iterator<JdbcCallRefCursorExtractor> refCursorExtractorIterator;

	ProcedureOutputsImpl(
			ProcedureCallImpl procedureCall,
			JdbcCall jdbcCall,
			ParameterStrategy parameterStrategy,
			QueryOptions queryOptions,
			QueryParameterBindings bindings,
			RowTransformer rowTransformer,
			SharedSessionContractImplementor persistenceContext) {
		this.parameterStrategy = parameterStrategy;
		this.persistenceContext = persistenceContext;
		this.procedureCall = procedureCall;
		this.jdbcCall = jdbcCall;

		// JdbcCall.getSql is the callable name
		this.callableName = jdbcCall.getSql();
		this.callString = buildCallableString();
		this.callableStatement = prepareCallableStatement( queryOptions, bindings );

		try {
			final boolean isResultSet = callableStatement.execute();
			currentOutputState = buildCurrentReturnState( isResultSet );
		}
		catch (SQLException e) {
			throw convert( e, "Error calling CallableStatement.getMoreResults" );
		}
	}

	private String buildCallableString() {
		final CallableStatementSupport callableStatementSupport = persistenceContext.getJdbcServices()
				.getJdbcEnvironment()
				.getDialect().getCallableStatementSupport();

		return callableStatementSupport.renderCallableStatement(
				callableName,
				parameterStrategy,
				jdbcCall.getFunctionReturn(),
				jdbcCall.getParameterRegistrations(),
				persistenceContext
		);
	}

	private CallableStatement prepareCallableStatement(QueryOptions queryOptions, QueryParameterBindings bindings) {
		try {
			log.debugf( "Preparing procedure/function call (%s) : %s", jdbcCall.getSql(), callString );
			final CallableStatement callableStatement = (CallableStatement) persistenceContext.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( callString, true );

			List<JdbcCallRefCursorExtractor> refCursorExtractors = null;

			if ( jdbcCall.getFunctionReturn() != null ) {
				jdbcCall.getFunctionReturn().registerParameter( callableStatement, persistenceContext );

				final JdbcCallParameterExtractor parameterExtractor = jdbcCall.getFunctionReturn().getParameterExtractor();
				final JdbcCallRefCursorExtractor refCursorExtractor = jdbcCall.getFunctionReturn().getRefCursorExtractor();

				if ( parameterExtractor != null ) {
					assert refCursorExtractor == null;
					final String key = parameterExtractor.getParameterName() == null
							? Integer.toString( parameterExtractor.getParameterPosition() )
							: parameterExtractor.getParameterName();
					parameterExtractorMap = new HashMap<>();
					parameterExtractorMap.put( key, parameterExtractor );
				}
				else if ( refCursorExtractor != null ) {
					refCursorExtractors = new ArrayList<>();
					refCursorExtractors.add( refCursorExtractor );
				}
			}

			for ( JdbcCallParameterRegistration registration : jdbcCall.getParameterRegistrations() ) {
				int jdbcPosition = 1;
				registration.registerParameter( callableStatement, persistenceContext );

				// todo : ok to bind right away?  Or do we need to wait until after all parameters are registered?
				final JdbcParameterBinder binder = registration.getParameterBinder();
				if ( binder != null ) {
					binder.bindParameterValue( callableStatement, jdbcPosition, bindings, persistenceContext );
				}

				final JdbcCallParameterExtractor parameterExtractor = registration.getParameterExtractor();
				final JdbcCallRefCursorExtractor refCursorExtractor = registration.getRefCursorExtractor();

				if ( parameterExtractor != null ) {
					assert refCursorExtractor == null;

					if ( parameterExtractorMap == null ) {
						parameterExtractorMap = new HashMap<>();
					}
					final String key = parameterExtractor.getParameterName() == null
							? Integer.toString( parameterExtractor.getParameterPosition() )
							: parameterExtractor.getParameterName();
					parameterExtractorMap.put( key, parameterExtractor );
				}
				else {
					if ( refCursorExtractors == null ) {
						refCursorExtractors = new ArrayList<>();
					}
					refCursorExtractors.add( refCursorExtractor );
				}
			}

			if ( refCursorExtractors == null ) {
				refCursorExtractorIterator = Collections.emptyIterator();
			}
			else {
				refCursorExtractorIterator = refCursorExtractors.iterator();
			}

			return callableStatement;
		}
		catch (SQLException e) {
			throw persistenceContext.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Error preparing CallableStatement [" + jdbcCall.getSql() + "]",
					callString
			);
		}
	}

	private CurrentOutputState buildCurrentReturnState(boolean isResultSet) {
		int updateCount = -1;
		if ( ! isResultSet ) {
			try {
				updateCount = callableStatement.getUpdateCount();
			}
			catch (SQLException e) {
				throw convert( e, "Error calling CallableStatement.getUpdateCount" );
			}
		}

		return buildCurrentReturnState( isResultSet, updateCount );
	}

	private CurrentOutputState buildCurrentReturnState(boolean isResultSet, int updateCount) {
		return new CurrentOutputState( isResultSet, updateCount );
	}

	private JDBCException convert(SQLException e, String message) {
		return persistenceContext.getJdbcServices().getSqlExceptionHelper().convert(
				e,
				message,
				callString
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getOutputParameterValue(ParameterRegistration<T> parameterRegistration) {
		final JdbcCallParameterExtractor extractor = resolveJdbcParameterExtractor( parameterRegistration );
		return (T) extractor.extractValue( callableStatement, parameterStrategy == ParameterStrategy.NAMED, procedureCall.getSession() );
	}

	@SuppressWarnings("unchecked")
	private <T> JdbcCallParameterExtractor resolveJdbcParameterExtractor(ParameterRegistration<T> parameterRegistration) {
		final String key = parameterRegistration.getName() != null
				? parameterRegistration.getName()
				: Integer.toString( parameterRegistration.getPosition() );
		final JdbcCallParameterExtractor<T> extractor = parameterExtractorMap.get( key );
		if ( extractor == null ) {
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
	public Output getCurrent() {
		if ( currentOutputState == null ) {
			return null;
		}
		return currentOutputState.buildOutput();
	}

	@Override
	public boolean goToNext() {
		if ( currentOutputState == null ) {
			return false;
		}

		if ( currentOutputState.indicatesMoreOutputs() ) {
			// prepare the next return state
			try {
				final boolean isResultSet = callableStatement.getMoreResults();
				currentOutputState = buildCurrentReturnState( isResultSet );
			}
			catch (SQLException e) {
				throw convert( e, "Error calling CallableStatement.getMoreResults" );
			}
		}

		// and return
		return currentOutputState != null && currentOutputState.indicatesMoreOutputs();
	}

	@Override
	public void release() {
		try {
			callableStatement.close();
		}
		catch (SQLException e) {
			log.debug( "Unable to close PreparedStatement", e );
		}
	}

	private List extractCurrentResults() {
		final JdbcCallRefCursorExtractor refCursorExtractor = refCursorExtractorIterator.next();

		return extractResults(
				refCursorExtractor.extractResultSet( callableStatement, persistenceContext )
		);
	}

	protected List extractResults(ResultSet resultSet) {
		// todo : need to hook this in with RowReader, Initializers, etc
		throw new NotYetImplementedException( );
	}

	protected class CurrentOutputState {
		private final boolean isResultSet;
		private final int updateCount;

		private CurrentOutputState(boolean isResultSet, int updateCount) {
			this.isResultSet = isResultSet;
			this.updateCount = updateCount;
		}

		public boolean isResultSet() {
			return isResultSet;
		}

		public int getUpdateCount() {
			return updateCount;
		}

		public boolean indicatesMoreOutputs() {
			return isResultSet() || getUpdateCount() >= 0 || refCursorExtractorIterator.hasNext();
		}

		protected Output buildOutput() {
			if ( !indicatesMoreOutputs() ) {
				throw new IllegalStateException( "No more REF_CURSOR results to process" );
			}

			if ( log.isDebugEnabled() ) {
				log.debugf(
						"Building Return [isResultSet=%s, updateCount=%s]",
						isResultSet(),
						getUpdateCount()
				);
			}

			if ( isResultSet() ) {
				return buildResultSetOutput( extractCurrentResults() );
			}
			else if ( getUpdateCount() >= 0 ) {
				return buildUpdateCountOutput( updateCount );
			}

			throw new NoMoreReturnsException();
		}

		private Output buildResultSetOutput(List list) {
			return new ResultSetOutputImpl( list );
		}

		private Output buildUpdateCountOutput(int updateCount) {
			return new UpdateCountOutputImpl( updateCount );
		}
	}

}
