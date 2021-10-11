/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.type.descriptor.JdbcBindingLogging;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Convenience base implementation of {@link ValueBinder}
 *
 * @author Steve Ebersole
 */
public abstract class BasicBinder<J> implements ValueBinder<J>, Serializable {
	private static final String BIND_MSG_TEMPLATE = "binding parameter [%s] as [%s] - [%s]";
	private static final String NULL_BIND_MSG_TEMPLATE = "binding parameter [%s] as [%s] - [null]";

	private final JavaType<J> javaDescriptor;
	private final JdbcTypeDescriptor jdbcTypeDescriptor;

	public JavaType<J> getJavaTypeDescriptor() {
		return javaDescriptor;
	}

	public JdbcTypeDescriptor getJdbcTypeDescriptor() {
		return jdbcTypeDescriptor;
	}

	public BasicBinder(JavaType<J> javaDescriptor, JdbcTypeDescriptor jdbcTypeDescriptor) {
		this.javaDescriptor = javaDescriptor;
		this.jdbcTypeDescriptor = jdbcTypeDescriptor;
	}

	@Override
	public final void bind(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			if ( JdbcBindingLogging.TRACE_ENABLED ) {
				JdbcBindingLogging.LOGGER.trace(
						String.format(
								NULL_BIND_MSG_TEMPLATE,
								index,
								JdbcTypeNameMapper.getTypeName( getJdbcTypeDescriptor().getJdbcTypeCode() )
						)
				);
			}
			doBindNull( st, index, options );
		}
		else {
			if ( JdbcBindingLogging.TRACE_ENABLED ) {
				JdbcBindingLogging.LOGGER.trace(
						String.format(
								BIND_MSG_TEMPLATE,
								index,
								JdbcTypeNameMapper.getTypeName( jdbcTypeDescriptor.getJdbcTypeCode() ),
								getJavaTypeDescriptor().extractLoggableRepresentation( value )
						)
				);
			}
			doBind( st, value, index, options );
		}
	}

	@Override
	public final void bind(CallableStatement st, J value, String name, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			if ( JdbcBindingLogging.TRACE_ENABLED ) {
				JdbcBindingLogging.LOGGER.trace(
						String.format(
								NULL_BIND_MSG_TEMPLATE,
								name,
								JdbcTypeNameMapper.getTypeName( getJdbcTypeDescriptor().getJdbcTypeCode() )
						)
				);
			}
			doBindNull( st, name, options );
		}
		else {
			if ( JdbcBindingLogging.TRACE_ENABLED ) {
				JdbcBindingLogging.LOGGER.trace(
						String.format(
								BIND_MSG_TEMPLATE,
								name,
								JdbcTypeNameMapper.getTypeName( jdbcTypeDescriptor.getJdbcTypeCode() ),
								getJavaTypeDescriptor().extractLoggableRepresentation( value )
						)
				);
			}
			doBind( st, value, name, options );
		}
	}

	/**
	 * Perform the null binding.
	 *
	 * @param st The prepared statement
	 * @param index The index at which to bind
	 * @param options The binding options
	 *
	 * @throws SQLException Indicates a problem binding to the prepared statement.
	 */
	protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
		st.setNull( index, jdbcTypeDescriptor.getJdbcTypeCode() );
	}

	/**
	 * Perform the null binding.
	 *
	 * @param st The CallableStatement
	 * @param name The name at which to bind
	 * @param options The binding options
	 *
	 * @throws SQLException Indicates a problem binding to the callable statement.
	 */
	protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
		st.setNull( name, jdbcTypeDescriptor.getJdbcTypeCode() );
	}

	/**
	 * Perform the binding.  Safe to assume that value is not null.
	 *
	 * @param st The prepared statement
	 * @param value The value to bind (not null).
	 * @param index The index at which to bind
	 * @param options The binding options
	 *
	 * @throws SQLException Indicates a problem binding to the prepared statement.
	 */
	protected abstract void doBind(PreparedStatement st, J value, int index, WrapperOptions options)
			throws SQLException;

	/**
	 * Perform the binding.  Safe to assume that value is not null.
	 *
	 * @param st The CallableStatement
	 * @param value The value to bind (not null).
	 * @param name The name at which to bind
	 * @param options The binding options
	 *
	 * @throws SQLException Indicates a problem binding to the callable statement.
	 */
	protected abstract void doBind(CallableStatement st, J value, String name, WrapperOptions options)
			throws SQLException;
}
