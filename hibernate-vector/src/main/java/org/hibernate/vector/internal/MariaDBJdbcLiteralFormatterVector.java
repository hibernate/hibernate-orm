/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

public class MariaDBJdbcLiteralFormatterVector<T> extends BasicJdbcLiteralFormatter<T> {

	private final JdbcLiteralFormatter<Object> elementFormatter;

	public MariaDBJdbcLiteralFormatterVector(JavaType<T> javaType, JdbcLiteralFormatter<?> elementFormatter) {
		super( javaType );
		//noinspection unchecked
		this.elementFormatter = (JdbcLiteralFormatter<Object>) elementFormatter;
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions) {
		final Object[] objects = unwrap( value, Object[].class, wrapperOptions );
		appender.appendSql( "vec_fromtext('" );
		char separator = '[';
		for ( Object o : objects ) {
			appender.appendSql( separator );
			elementFormatter.appendJdbcLiteral( appender, o, dialect, wrapperOptions );
			separator = ',';
		}
		appender.appendSql( "]')" );
	}

}
