/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.BasicType;
import org.hibernate.type.ProcedureParameterNamedBinder;

/**
 * @author Steve Ebersole
 */
public class BasicTypeBinderAdapter<T> implements JdbcValueBinder<T> {
	private final BasicType basicType;

	private final boolean canBindToCallable;

	public BasicTypeBinderAdapter(BasicType basicType) {
		this.basicType = basicType;

		this.canBindToCallable = basicType instanceof ProcedureParameterNamedBinder;
	}

	@Override
	public void bind(
			PreparedStatement statement,
			int parameterPosition,
			T value,
			ExecutionContext executionContext) throws SQLException {
		basicType.nullSafeSet( statement, value, parameterPosition, executionContext.getSession() );
	}

	@Override
	public void bind(
			CallableStatement statement,
			String parameterName,
			T value,
			ExecutionContext executionContext) throws SQLException {
		if ( canBindToCallable ) {
			( (ProcedureParameterNamedBinder) basicType ).nullSafeSet(
					statement,
					value,
					parameterName,
					executionContext.getSession()
			);
		}
		else {
			throw new UnsupportedOperationException(
					"BasicType [" + basicType.getClass().getName() + "] does not implement "
							+ ProcedureParameterNamedBinder.class.getName()
							+ ", cannot bind to JDBC CallableStatement"
			);
		}
	}
}
