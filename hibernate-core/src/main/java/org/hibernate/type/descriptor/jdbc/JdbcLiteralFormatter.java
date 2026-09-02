/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.io.Serializable;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Renders values of a given [JavaType] as SQL literals of a certain
/// [JdbcType].
///
/// Providers supply a formatter from
/// [JdbcType#getJdbcLiteralFormatter(JavaType)]. Implementations append the
/// literal immediately and must not retain the appender, value, Dialect, or
/// wrapper options.
///
/// @param <T> the Java type formatted as a SQL literal
///
/// @see JdbcType#getJdbcLiteralFormatter(JavaType)
///
/// @author Steve Ebersole
@FunctionalInterface
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface JdbcLiteralFormatter<T> extends Serializable {
	/**
	 * Produces a string containing a SQL literal value representing the given Java value.
	 *
	 * @param value a Java object whose value can be represented as a SQL literal
	 * @param dialect the SQL dialect
	 * @return the SQL literal as a string
	 */
	default String toJdbcLiteral(T value, Dialect dialect, WrapperOptions wrapperOptions) {
		final StringBuilder result = new StringBuilder();
		appendJdbcLiteral( new StringBuilderSqlAppender( result ), value, dialect, wrapperOptions );
		return result.toString();
	}

	/**
	 * Append a SQL literal representing the given Java value to a fragment of SQL which
	 * is being built.
	 *
	 * @param appender an operation that appends to the SQL fragment
	 * @param value a Java object whose value can be represented as a SQL literal
	 * @param dialect the SQL dialect
	 */
	void appendJdbcLiteral(SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions);
}
