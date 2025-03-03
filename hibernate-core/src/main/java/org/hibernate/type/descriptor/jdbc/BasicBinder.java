/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.type.descriptor.JdbcBindingLogging;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Convenience base implementation of {@link ValueBinder}
 *
 * @author Steve Ebersole
 */
public abstract class BasicBinder<J> implements ValueBinder<J>, Serializable {

	private final JavaType<J> javaType;
	private final JdbcType jdbcType;

	public JavaType<J> getJavaType() {
		return javaType;
	}

	public JdbcType getJdbcType() {
		return jdbcType;
	}

	public BasicBinder(JavaType<J> javaType, JdbcType jdbcType) {
		this.javaType = javaType;
		this.jdbcType = jdbcType;
	}

	@Override
	public final void bind(PreparedStatement st, J value, int index, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			if ( JdbcBindingLogging.LOGGER.isTraceEnabled() ) {
				JdbcBindingLogging.logNullBinding(
						index,
						jdbcType.getDefaultSqlTypeCode()
				);
			}
			doBindNull( st, index, options );
		}
		else {
			if ( JdbcBindingLogging.LOGGER.isTraceEnabled() ) {
				JdbcBindingLogging.logBinding(
						index,
						jdbcType.getDefaultSqlTypeCode(),
						getJavaType().extractLoggableRepresentation( value )
				);
			}
			doBind( st, value, index, options );
		}
	}

	@Override
	public final void bind(CallableStatement st, J value, String name, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			if ( JdbcBindingLogging.LOGGER.isTraceEnabled() ) {
				JdbcBindingLogging.logNullBinding(
						name,
						jdbcType.getDefaultSqlTypeCode()
				);
			}
			doBindNull( st, name, options );
		}
		else {
			if ( JdbcBindingLogging.LOGGER.isTraceEnabled() ) {
				JdbcBindingLogging.logBinding(
						name,
						jdbcType.getDefaultSqlTypeCode(),
						getJavaType().extractLoggableRepresentation( value )
				);
			}
			doBind( st, value, name, options );
		}
	}

	@Override
	public Object getBindValue(J value, WrapperOptions options) throws SQLException {
		final Class<?> preferredJavaTypeClass = jdbcType.getPreferredJavaTypeClass( options );
		return preferredJavaTypeClass == null ? value : getJavaType().unwrap( value, preferredJavaTypeClass, options );
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
		st.setNull( index, jdbcType.getJdbcTypeCode() );
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
		st.setNull( name, jdbcType.getJdbcTypeCode() );
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
