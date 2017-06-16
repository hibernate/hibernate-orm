/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import javax.persistence.ParameterMode;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.ParameterRegistry;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

/**
 * Standard implementation of CallableStatementSupport
 *
 * @author Steve Ebersole
 */
public class StandardCallableStatementSupport implements CallableStatementSupport {
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
	public boolean shouldUseFunctionSyntax(ParameterRegistry parameterRegistry) {
		return false;
	}

	@Override
	public String renderCallableStatement(
			String callableName,
			ParameterStrategy parameterStrategy,
			JdbcCallFunctionReturn functionReturn,
			List<JdbcCallParameterRegistration> parameterRegistrations,
			SharedSessionContractImplementor session) {
		final boolean renderAsFunctionCall = functionReturn != null;

		if ( renderAsFunctionCall ) {
			// validate that the parameter strategy is positional (cannot mix, and REF_CURSOR is inherently positional)
			if ( parameterStrategy == ParameterStrategy.NAMED ) {
				throw new HibernateException( "Cannot mix named parameters and REF_CURSOR parameter" );
			}
		}

		final StringBuilder buffer;
		if ( renderAsFunctionCall ) {
			buffer = new StringBuilder().append( "{? = call " );
		}
		else {
			buffer = new StringBuilder().append( "{call " );
		}

		buffer.append( callableName ).append( "(" );

		String sep = "";

		final int startIndex = renderAsFunctionCall ? 1 : 0;
		for ( int i = startIndex; i < parameterRegistrations.size(); i++ ) {
			final JdbcCallParameterRegistration registration = parameterRegistrations.get( i );

			if ( registration.getParameterMode() == ParameterMode.REF_CURSOR ) {
				if ( !supportsRefCursors ) {
					throw new HibernateException( "Found REF_CURSOR parameter registration, but database does not support REF_CURSOR parameters" );
				}
			}

			for ( int j = 0; j < registration.getJdbcParameterCount(); j++ ) {
				buffer.append( sep ).append( "?" );
				sep = ",";
			}
		}

		return buffer.append( ")}" ).toString();
	}

	@Override
	public void registerParameters(
			String procedureName,
			CallableStatement statement,
			ParameterStrategy parameterStrategy,
			JdbcCallFunctionReturn functionReturn,
			List<JdbcCallParameterRegistration> parameterRegistrations,
			SharedSessionContractImplementor session) {
		// prepare parameters
		int i = 1;

		try {
			if ( functionReturn != null ) {
				functionReturn.prepare( statement, session );
				i++;
			}

			for ( JdbcCallParameterRegistration registration : parameterRegistrations ) {
				if ( registration.getParameterMode() == ParameterMode.REF_CURSOR ) {
					statement.registerOutParameter( i, Types.OTHER );
					i++;

				}
				else {
					registration.registerParameter( statement, session );
					i += registration.getJdbcParameterCount();
				}
			}
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Error registering CallableStatement parameters",
					procedureName
			);
		}
	}
}
