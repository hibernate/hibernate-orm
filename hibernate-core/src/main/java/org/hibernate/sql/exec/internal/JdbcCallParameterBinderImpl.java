/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.type.ProcedureParameterNamedBinder;
import org.hibernate.type.spi.Type;

import org.jboss.logging.Logger;

/**
 * Implementation of JdbcParameterBinder for JdbcCall handling.  HQL and Criteria
 * define JdbcParameterBinder themselves.
 *
 * @author Steve Ebersole
 */
public class JdbcCallParameterBinderImpl implements JdbcParameterBinder {
	private static final Logger log = Logger.getLogger( JdbcCallParameterBinderImpl.class );

	private final String callName;
	private final String parameterName;
	private final int parameterPosition;
	private final Type ormType;

	public JdbcCallParameterBinderImpl(
			String callName,
			String parameterName,
			int parameterPosition,
			Type ormType) {
		this.callName = callName;
		this.parameterName = parameterName;
		this.parameterPosition = parameterPosition;
		this.ormType = ormType;
	}

	@Override
	public int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session) throws SQLException {
		final QueryParameterBinding binding;
		if ( parameterName != null ) {
			binding = queryParameterBindings.getBinding( parameterName );
		}
		else {
			binding = queryParameterBindings.getBinding( parameterPosition );
		}

		if ( binding == null ) {
			// the user did not bind a value to the parameter...
			log.debugf(
					"Stored procedure [%s] IN/INOUT parameter [%s] not bound; skipping binding (assuming procedure defines default value)",
					callName,
					parameterName == null ? Integer.toString( parameterPosition ) : parameterName
			);
		}
		else  {
			final Object bindValue = binding.getBindValue();

			if ( binding.getBindValue() == null ) {
				log.debugf(
						"Binding NULL to stored procedure IN/INOUT parameter [%s.%s]",
						callName,
						parameterName == null ? Integer.toString( parameterPosition ) : parameterName
				);
			}
			else {
				log.debugf(
						"Binding [%s] to stored procedure IN/INOUT parameter [%s.%s]",
						bindValue,
						callName,
						parameterName == null ? Integer.toString( parameterPosition ) : parameterName
				);
			}

			if ( parameterName != null ) {
				( (ProcedureParameterNamedBinder) ormType ).nullSafeSet(
						(CallableStatement) statement,
						bindValue,
						parameterName,
						session
				);
			}
		}

		return ormType.sqlTypes().length;
	}
}
