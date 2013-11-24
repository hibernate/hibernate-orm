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
import java.sql.Types;
import java.util.List;
import javax.persistence.ParameterMode;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.ParameterRegistrationImplementor;
import org.hibernate.procedure.spi.ParameterStrategy;

/**
 * @author Steve Ebersole
 */
public class PostgresCallableStatementSupport implements CallableStatementSupport {
	/**
	 * Singleton access
	 */
	public static final PostgresCallableStatementSupport INSTANCE = new PostgresCallableStatementSupport();

	@Override
	public String renderCallableStatement(
			String procedureName,
			ParameterStrategy parameterStrategy,
			List<ParameterRegistrationImplementor<?>> parameterRegistrations,
			SessionImplementor session) {
		// if there are any parameters, see if the first is REF_CURSOR
		final boolean firstParamIsRefCursor = ! parameterRegistrations.isEmpty()
				&& parameterRegistrations.get( 0 ).getMode() == ParameterMode.REF_CURSOR;

		if ( firstParamIsRefCursor ) {
			// validate that the parameter strategy is positional (cannot mix, and REF_CURSOR is inherently positional)
			if ( parameterStrategy == ParameterStrategy.NAMED ) {
				throw new HibernateException( "Cannot mix named parameters and REF_CURSOR parameter on PostgreSQL" );
			}
		}

		final StringBuilder buffer;
		if ( firstParamIsRefCursor ) {
			buffer = new StringBuilder().append( "{? = call " );
		}
		else {
			buffer = new StringBuilder().append( "{call " );
		}

		buffer.append( procedureName ).append( "(" );

		String sep = "";

		// skip the first registration if it was a REF_CURSOR
		final int startIndex = firstParamIsRefCursor ? 1 : 0;
		for ( int i = startIndex; i < parameterRegistrations.size(); i++ ) {
			final ParameterRegistrationImplementor parameter = parameterRegistrations.get( i );

			// any additional REF_CURSOR parameter registrations are an error
			if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
				throw new HibernateException( "PostgreSQL supports only one REF_CURSOR parameter, but multiple were registered" );
			}

			for ( int ignored : parameter.getSqlTypes() ) {
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
			List<ParameterRegistrationImplementor<?>> parameterRegistrations,
			SessionImplementor session) {
		// prepare parameters
		int i = 1;

		try {
			for ( ParameterRegistrationImplementor parameter : parameterRegistrations ) {
				if ( parameter.getMode() == ParameterMode.REF_CURSOR ) {
					statement.registerOutParameter( i, Types.OTHER );
					i++;

				}
				else {
					parameter.prepare( statement, i );
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
