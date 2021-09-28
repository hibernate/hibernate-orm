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
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.internal.JdbcCallImpl;
import org.hibernate.sql.exec.spi.JdbcCall;

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
	public JdbcCall interpretCall(
			String procedureName,
			FunctionReturnImpl functionReturn,
			ProcedureParameterMetadataImplementor parameterMetadata,
			ProcedureParamBindings paramBindings,
			SharedSessionContractImplementor session) {
		final boolean firstParamIsRefCursor = parameterMetadata.getParameterCount() != 0
				&& isFirstParameterModeRefCursor( parameterMetadata );

		if ( firstParamIsRefCursor ) {
			// validate that the parameter strategy is positional (cannot mix, and REF_CURSOR is inherently positional)
			if ( parameterMetadata.hasNamedParameters() ) {
				throw new HibernateException( "Cannot mix named parameters and REF_CURSOR parameter on PostgreSQL" );
			}
		}

		final List<? extends ProcedureParameterImplementor<?>> registrations = parameterMetadata.getRegistrationsAsList();

		final StringBuilder buffer;
		if ( firstParamIsRefCursor ) {
			buffer = new StringBuilder(11 + procedureName.length() + registrations.size() * 2).append( "{?=call " );
		}
		else {
			buffer = new StringBuilder(9 + procedureName.length() + registrations.size() * 2).append( "{call " );
		}

		buffer.append( procedureName ).append( "(" );

		// skip the first registration if it was a REF_CURSOR
		final int startIndex;
		if ( firstParamIsRefCursor ) {
			startIndex = 1;
		}
		else {
			startIndex = 0;
		}
		String sep = "";
		for ( int i = startIndex; i < registrations.size(); i++ ) {
			if ( registrations.get( i ).getMode() == ParameterMode.REF_CURSOR ) {
				throw new HibernateException(
						"PostgreSQL supports only one REF_CURSOR parameter, but multiple were registered" );
			}
			buffer.append( sep ).append( "?" );
			sep = ",";
		}

		buffer.append( ")}" );
		return new JdbcCallImpl.Builder(
				buffer.toString(),
				parameterMetadata.hasNamedParameters() ?
						ParameterStrategy.NAMED :
						ParameterStrategy.POSITIONAL
		).buildJdbcCall();
	}

	private static boolean isFirstParameterModeRefCursor(ProcedureParameterMetadataImplementor parameterMetadata) {
		return parameterMetadata.getRegistrationsAsList().get( 0 ).getMode() == ParameterMode.REF_CURSOR;
	}

}
