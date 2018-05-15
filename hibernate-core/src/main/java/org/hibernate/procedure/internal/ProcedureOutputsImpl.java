/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureParamBindings;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.result.Output;
import org.hibernate.result.internal.OutputsImpl;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcCall;
import org.hibernate.sql.exec.spi.JdbcCallParameterExtractor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcCallRefCursorExtractor;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

import org.jboss.logging.Logger;

/**
 * Implementation of ProcedureResult.  Defines centralized access to all of the results of a procedure call.
 *
 * @author Steve Ebersole
 */
public class ProcedureOutputsImpl extends OutputsImpl
		implements ProcedureOutputs, ExecutionContext, ParameterBindingContext, Callback {
	private static final Logger log = Logger.getLogger( ProcedureOutputsImpl.class );

	private final ParameterStrategy parameterStrategy;
	private final QueryOptions queryOptions;
	private final SharedSessionContractImplementor session;
	private final ProcedureCallImpl procedureCall;
	private final JdbcCall jdbcCall;

	private final String callableName;
	private final String callString;
	private final CallableStatement callableStatement;

	private Map<String, JdbcCallParameterExtractor> parameterExtractorMap;
	private Iterator<JdbcCallRefCursorExtractor> refCursorExtractorIterator;

	ProcedureOutputsImpl(
			ProcedureCallImpl procedureCall,
			JdbcCall jdbcCall,
			ParameterStrategy parameterStrategy,
			QueryOptions queryOptions,
			ProcedureParamBindings bindings,
			SharedSessionContractImplementor session) {
		super( procedureCall );
		this.parameterStrategy = parameterStrategy;
		this.queryOptions = queryOptions;
		this.session = session;
		this.procedureCall = procedureCall;
		this.jdbcCall = jdbcCall;

		// JdbcCall.getSql is the callable name
		this.callableName = jdbcCall.getSql();
		this.callString = buildCallableString( bindings );
		this.callableStatement = prepareCallableStatement( queryOptions, bindings );

		prime( callableStatement );

		this.refCursorExtractorIterator = jdbcCall.getCallRefCursorExtractors().iterator();
	}

	private String buildCallableString(ProcedureParamBindings bindings) {
		final CallableStatementSupport callableStatementSupport = session.getJdbcServices()
				.getJdbcEnvironment()
				.getDialect().getCallableStatementSupport();

		return callableStatementSupport.renderCallableStatement(
				callableName,
				jdbcCall,
				bindings,
				session
		);
	}

	private CallableStatement prepareCallableStatement(QueryOptions queryOptions, QueryParameterBindings bindings) {
		try {
			log.debugf( "Preparing procedure/function call (%s) : %s", jdbcCall.getSql(), callString );
			final CallableStatement callableStatement = (CallableStatement) session.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( callString, true );

			List<JdbcCallRefCursorExtractor> refCursorExtractors = null;

			if ( jdbcCall.getFunctionReturn() != null ) {
				jdbcCall.getFunctionReturn().registerParameter( callableStatement, session );

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

			// todo : bind query-options

			for ( JdbcCallParameterRegistration registration : jdbcCall.getParameterRegistrations() ) {
				int jdbcPosition = 1;
				registration.registerParameter( callableStatement, session );

				// todo : ok to bind right away?  Or do we need to wait until after all parameters are registered?
				final JdbcParameterBinder binder = registration.getParameterBinder();
				if ( binder != null ) {
					binder.bindParameterValue( callableStatement, jdbcPosition, this );
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
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Error preparing CallableStatement [" + jdbcCall.getSql() + "]",
					callString
			);
		}
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return procedureCall.getSession().getFactory();
	}

	@Override
	protected boolean nextResult() throws SQLException {
		return callableStatement.getMoreResults();
	}

	@Override
	protected CurrentReturnState buildCurrentReturnState(boolean isResultSet) throws SQLException {
		return buildCurrentReturnState( isResultSet, callableStatement );
	}

	@Override
	protected CurrentReturnState buildCurrentReturnState(
			boolean isResultSet,
			int updateCount,
			PreparedStatement jdbcStatement) {
		return new CurrentReturnState( isResultSet, updateCount, jdbcStatement ) {
			@Override
			protected boolean hasExtendedReturns() {
				return !jdbcCall.getCallRefCursorExtractors().isEmpty();
			}

			@Override
			protected Output buildExtendedReturn() {
				final JdbcCallRefCursorExtractor extractor = refCursorExtractorIterator.next();
				final ResultSet resultSet = extractor.extractResultSet( callableStatement, session );
				return buildResultSetOutput( extractResults( resultSet, callableStatement ) );
			}
		};
	}

	private JDBCException convert(SQLException e, String message) {
		return session.getJdbcServices().getSqlExceptionHelper().convert(
				e,
				message,
				callString
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getOutputParameterValue(ProcedureParameterImplementor<T> parameterRegistration) {
		final JdbcCallParameterExtractor extractor = resolveJdbcParameterExtractor( parameterRegistration );
		return (T) extractor.extractValue( callableStatement, parameterStrategy == ParameterStrategy.NAMED, this );
	}

	@SuppressWarnings("unchecked")
	private <T> JdbcCallParameterExtractor resolveJdbcParameterExtractor(ProcedureParameterImplementor<T> parameterRegistration) {
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
	public void release() {
		try {
			callableStatement.close();
		}
		catch (SQLException e) {
			log.debug( "Unable to close PreparedStatement", e );
		}
	}

	@Override
	public <T> List<T> getLoadIdentifiers() {
		return Collections.emptyList();
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return procedureCall.getQueryParameterBindings();
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return session;
	}

	@Override
	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public ParameterBindingContext getParameterBindingContext() {
		return this;
	}

	@Override
	public Callback getCallback() {
		return this;
	}

	@Override
	public void registerAfterLoadAction(AfterLoadAction afterLoadAction) {
		// todo (6.0) : hook this up with follow-on locking, etc
	}
}
