/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.internal.JdbcCallImpl;
import org.hibernate.sql.exec.spi.JdbcCall;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

import jakarta.persistence.ParameterMode;

/**
 * @author Steve Ebersole
 */
public class PostgresCallableStatementSupport extends AbstractStandardCallableStatementSupport {
	/**
	 * Singleton access
	 */
	public static final PostgresCallableStatementSupport INSTANCE = new PostgresCallableStatementSupport();

	@Override
	public JdbcCall interpretCall(ProcedureCallImplementor<?> procedureCall) {
		final String procedureName = procedureCall.getProcedureName();
		final FunctionReturnImplementor functionReturn = procedureCall.getFunctionReturn();
		final ProcedureParameterMetadataImplementor parameterMetadata = procedureCall.getParameterMetadata();
		final SharedSessionContractImplementor session = procedureCall.getSession();
		final boolean firstParamIsRefCursor = parameterMetadata.getParameterCount() != 0
				&& isFirstParameterModeRefCursor( parameterMetadata );

		if ( firstParamIsRefCursor || functionReturn != null ) {
			// validate that the parameter strategy is positional (cannot mix, and REF_CURSOR is inherently positional)
			if ( parameterMetadata.hasNamedParameters() ) {
				throw new HibernateException( "Cannot mix named parameters and REF_CURSOR parameter on PostgreSQL" );
			}
		}

		final List<? extends ProcedureParameterImplementor<?>> registrations = parameterMetadata.getRegistrationsAsList();
		final ParameterStrategy parameterStrategy = parameterMetadata.hasNamedParameters() ?
				ParameterStrategy.NAMED :
				ParameterStrategy.POSITIONAL;
		final JdbcCallImpl.Builder builder = new JdbcCallImpl.Builder( parameterStrategy );

		final StringBuilder buffer;
		final int offset;
		final int startIndex;
		if ( functionReturn != null ) {
			offset = 2;
			startIndex = 0;
			buffer = new StringBuilder( 11 + procedureName.length() + registrations.size() * 2 ).append( "{?=call " );
			builder.setFunctionReturn( functionReturn.toJdbcFunctionReturn( session ) );
		}
		else if ( firstParamIsRefCursor ) {
			offset = 1;
			startIndex = 1;
			buffer = new StringBuilder( 11 + procedureName.length() + registrations.size() * 2 ).append( "{?=call " );
			builder.addParameterRegistration( registrations.get( 0 ).toJdbcParameterRegistration( 1, procedureCall ) );
		}
		else {
			offset = 1;
			startIndex = 0;
			buffer = new StringBuilder( 9 + procedureName.length() + registrations.size() * 2 ).append( "{call " );
		}

		buffer.append( procedureName ).append( '(' );

		String sep = "";
		for ( int i = startIndex; i < registrations.size(); i++ ) {
			final ProcedureParameterImplementor<?> parameter = registrations.get( i );
			if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
				throw new HibernateException(
						"PostgreSQL supports only one REF_CURSOR parameter, but multiple were registered" );
			}
			buffer.append( sep );
			final JdbcCallParameterRegistration registration = parameter.toJdbcParameterRegistration(
					i + offset,
					procedureCall
			);
			if ( registration.getName() != null ) {
				buffer.append( ':' ).append( registration.getName() );
			}
			else {
				buffer.append( '?' );
			}
			sep = ",";
			builder.addParameterRegistration( registration );
		}

		buffer.append( ")}" );
		builder.setCallableName( buffer.toString() );
		return builder.buildJdbcCall();
	}

	private static boolean isFirstParameterModeRefCursor(ProcedureParameterMetadataImplementor parameterMetadata) {
		return parameterMetadata.getRegistrationsAsList().get( 0 ).getMode() == ParameterMode.REF_CURSOR;
	}

}
