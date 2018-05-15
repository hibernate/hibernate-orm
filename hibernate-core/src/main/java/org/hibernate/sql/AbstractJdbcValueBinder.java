/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Convenience base implementation of {@link JdbcValueBinder}
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJdbcValueBinder<J> implements JdbcValueBinder<J> {
	private static final String BIND_MSG_TEMPLATE = "binding parameter [%s] as [%s] - [%s]";
	private static final String NULL_BIND_MSG_TEMPLATE = "binding parameter [%s] as [%s] - [null]";

	private final JavaTypeDescriptor<J> javaDescriptor;
	private final SqlTypeDescriptor sqlDescriptor;

	public JavaTypeDescriptor<J> getJavaDescriptor() {
		return javaDescriptor;
	}

	public SqlTypeDescriptor getSqlDescriptor() {
		return sqlDescriptor;
	}

	public AbstractJdbcValueBinder(JavaTypeDescriptor<J> javaDescriptor, SqlTypeDescriptor sqlDescriptor) {
		this.javaDescriptor = javaDescriptor;
		this.sqlDescriptor = sqlDescriptor;
	}

	@Override
	public final void bind(PreparedStatement st, int parameterPosition, J value, ExecutionContext executionContext) throws SQLException {
		final boolean traceEnabled = BINDING_LOGGER.isTraceEnabled();
		if ( value == null ) {
			if ( traceEnabled ) {
				BINDING_LOGGER.trace(
						String.format(
								NULL_BIND_MSG_TEMPLATE,
								parameterPosition,
								JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getJdbcTypeCode() )
						)
				);
			}
			st.setNull( parameterPosition, sqlDescriptor.getJdbcTypeCode() );
		}
		else {
			if ( traceEnabled ) {
				BINDING_LOGGER.trace(
						String.format(
								BIND_MSG_TEMPLATE,
								parameterPosition,
								JdbcTypeNameMapper.getTypeName( sqlDescriptor.getJdbcTypeCode() ),
								getJavaDescriptor().extractLoggableRepresentation( value )
						)
				);
			}
			doBind( st, parameterPosition, value, executionContext );
		}
	}

	@Override
	public final void bind(CallableStatement st, String parameterName, J value, ExecutionContext executionContext) throws SQLException {
		final boolean traceEnabled = BINDING_LOGGER.isTraceEnabled();
		if ( value == null ) {
			if ( traceEnabled ) {
				BINDING_LOGGER.trace(
						String.format(
								NULL_BIND_MSG_TEMPLATE,
								parameterName,
								JdbcTypeNameMapper.getTypeName( getSqlDescriptor().getJdbcTypeCode() )
						)
				);
			}
			st.setNull( parameterName, sqlDescriptor.getJdbcTypeCode() );
		}
		else {
			if ( traceEnabled ) {
				BINDING_LOGGER.trace(
						String.format(
								BIND_MSG_TEMPLATE,
								parameterName,
								JdbcTypeNameMapper.getTypeName( sqlDescriptor.getJdbcTypeCode() ),
								getJavaDescriptor().extractLoggableRepresentation( value )
						)
				);
			}
			doBind( st, parameterName, value, executionContext );
		}
	}

	/**
	 * Perform the binding.  Safe to assume that value is not null.
	 *
	 * @param st The prepared statement
	 * @param index The index at which to bind
	 * @param value The value to bind (not null).
	 * @param executionContext The binding options
	 *
	 * @throws SQLException Indicates a problem binding to the prepared statement.
	 */
	protected abstract void doBind(
			PreparedStatement st,
			int index,
			J value,
			ExecutionContext executionContext) throws SQLException;

	/**
	 * Perform the binding.  Safe to assume that value is not null.
	 *
	 * @param st The CallableStatement
	 * @param name The name at which to bind
	 * @param value The value to bind (not null).
	 * @param executionContext The binding options
	 *
	 * @throws SQLException Indicates a problem binding to the prepared statement.
	 */
	protected abstract void doBind(
			CallableStatement st,
			String name,
			J value,
			ExecutionContext executionContext) throws SQLException;
}
