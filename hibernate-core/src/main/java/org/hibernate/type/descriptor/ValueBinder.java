/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Binds values to a JDBC [PreparedStatement].
///
/// Providers normally supply a binder from
/// [org.hibernate.type.descriptor.jdbc.JdbcType#getBinder(org.hibernate.type.descriptor.java.JavaType)].
/// A binder must perform JDBC binding only. It must not apply a
/// [org.hibernate.type.descriptor.converter.spi.BasicValueConverter]; the
/// caller coordinates conversion before binding.
///
/// @param <X> the Java value type accepted by this binder
/// @author Steve Ebersole
/// @see org.hibernate.type.descriptor.jdbc.JdbcType#getBinder(org.hibernate.type.descriptor.java.JavaType)
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface ValueBinder<X> {
	/**
	 * Bind a value to a prepared statement by index
	 *
	 * @apiNote Also works for callables since {@link CallableStatement} extends
	 * {@link PreparedStatement}
	 *
	 * @throws SQLException Indicates a JDBC error occurred.
	 */
	void bind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException;

	/**
	 * Bind a value to a callable statement by name
	 *
	 * @throws SQLException Indicates a JDBC error occurred.
	 */
	void bind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException;

	default Object getBindValue(X value, WrapperOptions options) throws SQLException {
		return value;
	}
}
