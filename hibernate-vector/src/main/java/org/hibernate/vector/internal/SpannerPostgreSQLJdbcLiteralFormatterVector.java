/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

public class SpannerPostgreSQLJdbcLiteralFormatterVector<T> extends BasicJdbcLiteralFormatter<T> {

	private final JdbcLiteralFormatter<Object> elementFormatter;
	private final String baseType;

	public SpannerPostgreSQLJdbcLiteralFormatterVector(JavaType<T> javaType, JdbcLiteralFormatter<?> elementFormatter) {
		this(javaType, elementFormatter, (javaType.getJavaTypeClass() == Double[].class || javaType.getJavaTypeClass() == double[].class) ? "float8" : "float4");
	}

	public SpannerPostgreSQLJdbcLiteralFormatterVector(JavaType<T> javaType, JdbcLiteralFormatter<?> elementFormatter, String baseType) {
		super( javaType );
		//noinspection unchecked
		this.elementFormatter = (JdbcLiteralFormatter<Object>) elementFormatter;
		this.baseType = baseType;
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions) {
		if (value == null) {
			appender.appendSql("null");
			return;
		}
		final Object[] objects = unwrap( value, Object[].class, wrapperOptions );
		appender.appendSql( "ARRAY[" );
		boolean first = true;
		for ( Object o : objects ) {
			if ( !first ) {
				appender.appendSql( "," );
			}
			else {
				first = false;
			}
			elementFormatter.appendJdbcLiteral( appender, o, dialect, wrapperOptions );
		}
		appender.appendSql( "]::" );
		appender.appendSql( baseType );
		appender.appendSql( "[]" );
	}
}
