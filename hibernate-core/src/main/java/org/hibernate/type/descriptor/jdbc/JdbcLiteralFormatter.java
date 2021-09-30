/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * A formatter object for creating JDBC literals of a given type.
 * <p/>
 * Generally this is obtained from the {@link JdbcTypeDescriptor#getJdbcLiteralFormatter} method
 * and would be specific to that Java and SQL type.
 * <p/>
 * Performs a similar function as the legacy LiteralType, except continuing the paradigm
 * shift from inheritance to composition/delegation.
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface JdbcLiteralFormatter<T> {
	String NULL = "null";

	default String toJdbcLiteral(T value, Dialect dialect, WrapperOptions wrapperOptions) {
		final StringBuilder sb = new StringBuilder();
		appendJdbcLiteral( sb::append, value, dialect, wrapperOptions );
		return sb.toString();
	}

	void appendJdbcLiteral(SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions);
}
