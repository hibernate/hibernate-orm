/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.procedure.internal;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.List;
import javax.persistence.ParameterMode;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
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
			SessionImplementor session) {
		final StringBuilder buffer = new StringBuilder().append( "{call " )
				.append( procedureName )
				.append( "(" );
		String sep = "";
		for ( ParameterRegistrationImplementor parameter : parameterRegistrations ) {
			if ( parameter == null ) {
				throw new QueryException( "Parameter registrations had gaps" );
			}

			if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
				verifyRefCursorSupport( session.getFactory().getDialect() );
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
			SessionImplementor session) {
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
			throw session.getFactory().getSQLExceptionHelper().convert(
					e,
					"Error registering CallableStatement parameters",
					procedureName
			);
		}
	}
}
