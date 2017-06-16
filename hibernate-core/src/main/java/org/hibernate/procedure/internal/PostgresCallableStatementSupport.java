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
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterRegistry;
import org.hibernate.procedure.spi.ParameterStrategy;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;

/**
 * @author Steve Ebersole
 */
public class PostgresCallableStatementSupport implements CallableStatementSupport {
	/**
	 * Singleton access
	 */
	public static final PostgresCallableStatementSupport INSTANCE = new PostgresCallableStatementSupport();

	@Override
	public boolean shouldUseFunctionSyntax(ParameterRegistry parameterRegistry) {
		for ( ParameterRegistrationImplementor registration : parameterRegistry.getParameterRegistrations() ) {
			if ( registration.getMode() == ParameterMode.REF_CURSOR ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String renderCallableStatement(
			String callableName,
			ParameterStrategy parameterStrategy,
			JdbcCallFunctionReturn functionReturn,
			List<JdbcCallParameterRegistration> parameterRegistrations,
			SharedSessionContractImplementor session) {
		final boolean isFunctionCall = functionReturn != null;
		final boolean firstParamIsRefCursor = ! parameterRegistrations.isEmpty()
				&& parameterRegistrations.get( 0 ).getParameterMode() == ParameterMode.REF_CURSOR;
		final boolean renderAsFunctionCall = isFunctionCall || firstParamIsRefCursor;

		if ( renderAsFunctionCall ) {
			// validate that the parameter strategy is positional (cannot mix, and REF_CURSOR is inherently positional)
			if ( parameterStrategy == ParameterStrategy.NAMED ) {
				throw new HibernateException( "Cannot mix named parameters and REF_CURSOR parameter on PostgreSQL" );
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

		// skip the first registration if it was a REF_CURSOR
		final int startIndex = firstParamIsRefCursor ? 1 : 0;
		for ( int i = startIndex; i < parameterRegistrations.size(); i++ ) {
			final JdbcCallParameterRegistration registration = parameterRegistrations.get( i );

			// any additional REF_CURSOR parameter registrations are an error
			if ( registration.getParameterMode() == ParameterMode.REF_CURSOR ) {
				throw new HibernateException( "PostgreSQL supports only one REF_CURSOR parameter, but multiple were registered" );
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
