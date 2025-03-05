/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * A formatter object for rendering values of a given {@linkplain JavaType Java type}
 * as SQL literals of a certain {@linkplain JdbcType JDBC/SQL type}. Usually, an
 * instance is obtained by calling {@link JdbcType#getJdbcLiteralFormatter(JavaType)}.
 *
 * @param <T> the Java type that this instance formats as a SQL literal
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
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
