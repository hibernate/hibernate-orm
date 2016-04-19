/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.List;
import javax.persistence.ParameterMode;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;

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
	public String renderCallableStatement(
			String procedureName,
			ParameterStrategy parameterStrategy,
			List<ParameterRegistrationImplementor<?>> parameterRegistrations,
			SharedSessionContractImplementor session) {
		final StringBuilder buffer = new StringBuilder().append( "{call " )
				.append( procedureName )
				.append( "(" );
		String sep = "";
		for ( ParameterRegistrationImplementor parameter : parameterRegistrations ) {
			if ( parameter == null ) {
				throw new QueryException( "Parameter registrations had gaps" );
			}

			if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
				verifyRefCursorSupport( session.getJdbcServices().getJdbcEnvironment().getDialect() );
				buffer.append( sep ).append( "?" );
				sep = ",";
			}
			else {
				for ( int i = 0; i < parameter.getSqlTypes().length; i++ ) {
					buffer.append( sep ).append( "?" );
					sep = ",";
				}
			}
		}

		return buffer.append( ")}" ).toString();
	}

	private void verifyRefCursorSupport(Dialect dialect) {
		if ( ! supportsRefCursors ) {
			throw new QueryException( "Dialect [" + dialect.getClass().getName() + "] not known to support REF_CURSOR parameters" );
		}
	}

	@Override
	public void registerParameters(
			String procedureName,
			CallableStatement statement,
			ParameterStrategy parameterStrategy,
			List<ParameterRegistrationImplementor<?>> parameterRegistrations,
			SharedSessionContractImplementor session) {
		// prepare parameters
		int i = 1;

		try {
			for ( ParameterRegistrationImplementor parameter : parameterRegistrations ) {
				parameter.prepare( statement, i );
				if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
					i++;
				}
				else {
					i += parameter.getSqlTypes().length;
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
