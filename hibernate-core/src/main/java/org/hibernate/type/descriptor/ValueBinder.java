/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Contract for binding values to a JDBC {@link PreparedStatement}.
 *
 * @apiNote Binders, as well as {@linkplain ValueExtractor extractors}, should never apply
 * {@linkplain org.hibernate.type.descriptor.converter.spi.BasicValueConverter conversions}.
 * Instead, callers of the binder are expected to coordinate between the binding and
 * conversion.
 *
 * @author Steve Ebersole
 */
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
