/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.internal;

import java.util.UUID;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

/**
 * {@link org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter}
 * implementation for handling UUID values
 */
public class JdbcLiteralFormatterUUIDData<T> extends BasicJdbcLiteralFormatter<T> {

	public JdbcLiteralFormatterUUIDData(JavaType<T> javaType) {
		super( javaType );
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, Object value, Dialect dialect, WrapperOptions wrapperOptions) {
		final UUID literalValue = unwrap( value, UUID.class, wrapperOptions );
		dialect.appendUUIDLiteral( appender, literalValue );
	}
}
