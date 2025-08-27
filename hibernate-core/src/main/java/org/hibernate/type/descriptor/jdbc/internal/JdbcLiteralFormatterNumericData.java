/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

/**
 * {@link org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter}
 * implementation for handling numeric data
 *
 * @author Steve Ebersole
 */
public class JdbcLiteralFormatterNumericData<T> extends BasicJdbcLiteralFormatter<T> {
	private final Class<? extends Number> unwrapJavaType;

	public JdbcLiteralFormatterNumericData(JavaType<T> javaType, Class<? extends Number> unwrapJavaType) {
		super( javaType );
		this.unwrapJavaType = unwrapJavaType;
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, Object value, Dialect dialect, WrapperOptions wrapperOptions) {
		appender.appendSql( unwrap( value, unwrapJavaType, wrapperOptions ).toString() );
	}
}
