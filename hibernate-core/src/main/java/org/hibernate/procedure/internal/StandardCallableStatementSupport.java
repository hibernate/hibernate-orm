/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.internal.JdbcCallImpl;
import org.hibernate.sql.exec.spi.JdbcCall;

import jakarta.persistence.ParameterMode;

/**
 * Standard implementation of CallableStatementSupport
 *
 * @author Steve Ebersole
 */
public class StandardCallableStatementSupport extends AbstractStandardCallableStatementSupport {
	/**
	 * Singleton access - without REF_CURSOR support
	 */
	public static final StandardCallableStatementSupport NO_REF_CURSOR_INSTANCE = new StandardCallableStatementSupport( false );

	/**
	 * Singleton access - with REF CURSOR support
	 */
	public static final StandardCallableStatementSupport REF_CURSOR_INSTANCE = new StandardCallableStatementSupport( true );

	private final boolean supportsRefCursors;

	public StandardCallableStatementSupport(boolean supportsRefCursors) {
		this.supportsRefCursors = supportsRefCursors;
	}

	@Override
	public JdbcCall interpretCall(ProcedureCallImplementor<?> procedureCall) {
		final String procedureName = procedureCall.getProcedureName();
		final FunctionReturnImplementor functionReturn = procedureCall.getFunctionReturn();
		final ProcedureParameterMetadataImplementor parameterMetadata = procedureCall.getParameterMetadata();
		final SharedSessionContractImplementor session = procedureCall.getSession();
		final List<? extends ProcedureParameterImplementor<?>> registrations = parameterMetadata.getRegistrationsAsList();
		final JdbcCallImpl.Builder builder = new JdbcCallImpl.Builder(
				parameterMetadata.hasNamedParameters() ?
						ParameterStrategy.NAMED :
						ParameterStrategy.POSITIONAL
		);
		final StringBuilder buffer;
		final int offset;
		if ( functionReturn != null ) {
			offset = 2;
			buffer = new StringBuilder( 11 + procedureName.length() + registrations.size() * 2 ).append( "{?=call " );
			builder.setFunctionReturn( functionReturn.toJdbcFunctionReturn( session ) );
		}
		else {
			offset = 1;
			buffer = new StringBuilder( 9 + procedureName.length() + registrations.size() * 2 ).append( "{call " );
		}

		buffer.append( procedureName ).append( "(" );

		String sep = "";
		for ( int i = 0; i < registrations.size(); i++ ) {
			final ProcedureParameterImplementor<?> parameter = registrations.get( i );
			if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
				verifyRefCursorSupport( session.getJdbcServices().getJdbcEnvironment().getDialect() );
			}
			buffer.append( sep ).append( "?" );
			sep = ",";
			builder.addParameterRegistration( parameter.toJdbcParameterRegistration( i + offset, procedureCall ) );
		}

		buffer.append( ")}" );

		builder.setCallableName( buffer.toString() );
		return builder.buildJdbcCall();
	}

	private void verifyRefCursorSupport(Dialect dialect) {
		if ( ! supportsRefCursors ) {
			throw new QueryException( "Dialect [" + dialect.getClass().getName() + "] not known to support REF_CURSOR parameters" );
		}
	}
}
