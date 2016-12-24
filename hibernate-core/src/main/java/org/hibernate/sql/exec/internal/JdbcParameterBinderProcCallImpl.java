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

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.type.ProcedureParameterNamedBinder;
import org.hibernate.type.spi.Type;

import org.jboss.logging.Logger;

/**
 * JdbcParameterBinder specific to ProcedureCall registered IN/INOUT parameters
 *
 * @author Steve Ebersole
 */
public class JdbcParameterBinderProcCallImpl implements JdbcParameterBinder {
	private static final Logger log = Logger.getLogger( JdbcParameterBinderProcCallImpl.class );

	private final String parameterName;
	private final int queryParameterIndex;
	private final Type ormType;
	private final boolean shouldBindNullValues;

	public JdbcParameterBinderProcCallImpl(
			String parameterName,
			int queryParameterIndex,
			Type ormType,
			boolean shouldBindNullValues) {
		this.parameterName = parameterName;
		this.queryParameterIndex = queryParameterIndex;
		this.ormType = ormType;
		this.shouldBindNullValues = shouldBindNullValues;
	}

	@Override
	public boolean shouldBindNullValues() {
		return shouldBindNullValues;
	}

	@Override
	public int bindParameterValue(
			PreparedStatement statement,
			int startPosition,
			QueryParameterBindings queryParameterBindings,
			SharedSessionContractImplementor session) throws SQLException {
		final CallableStatement callableStatement = (CallableStatement) statement;

		final QueryParameterBinding binding;
		if ( parameterName != null ) {
			binding = queryParameterBindings.getBinding( parameterName );
		}
		else {
			binding = queryParameterBindings.getBinding( queryParameterIndex );
		}

		if ( binding != null && binding.isMultiValued() ) {
			throw new QueryException( "ProcedureCall cannot accept multi-values parameter bindings" );
		}

		if ( binding == null || binding.getBindValue() == null ) {
			// the user did not bind a value to the parameter being processed, or they bound null.
			// This is the condition defined by `passNulls` and that value controls what happens
			// here.  If `passNulls` is {@code true} we will bind the NULL value into the statement;
			// if `passNulls` is {@code false} we will not.
			//
			// Unfortunately there is not a way to reliably know through JDBC metadata whether a procedure
			// parameter defines a default value.  Deferring to that information would be the best option
			if ( shouldBindNullValues ) {
				log.debugf(
						"ProcedureCall IN/INOUT parameter [%s] not bound and `shouldBindNullValues` was set to true; binding NULL",
						parameterName != null ? parameterName : queryParameterIndex
				);

				if ( parameterName != null ) {
					( (ProcedureParameterNamedBinder) ormType ).nullSafeSet(
							callableStatement,
							null,
							parameterName,
							session
					);
				}
				else {
					ormType.nullSafeSet( callableStatement, null, startPosition, session );
				}
			}
			else {
				log.debugf(
						"ProcedureCall IN/INOUT parameter [%s] not bound and `passNulls` was set to false; assuming procedure defines default value",
						parameterName != null ? parameterName : queryParameterIndex
				);
			}
		}
		else {
			if ( parameterName != null ) {
				( (ProcedureParameterNamedBinder) ormType ).nullSafeSet(
						callableStatement,
						binding.getBindValue(),
						parameterName,
						session
				);
			}
			else {
				ormType.nullSafeSet( callableStatement, binding.getBindValue(), startPosition, session );
			}
		}

		return ormType.getColumnSpan();
	}
}
