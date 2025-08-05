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

public class OracleJdbcLiteralFormatterVector<T> extends BasicJdbcLiteralFormatter<T> {

	private final JdbcLiteralFormatter<Object> elementFormatter;
	private final String vectorParameters;

	public OracleJdbcLiteralFormatterVector(JavaType<T> javaType, JdbcLiteralFormatter<?> elementFormatter, String vectorParameters) {
		super( javaType );
		//noinspection unchecked
		this.elementFormatter = (JdbcLiteralFormatter<Object>) elementFormatter;
		this.vectorParameters = vectorParameters;
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, Object value, Dialect dialect, WrapperOptions wrapperOptions) {
		final Object[] objects = unwrapArray( value, wrapperOptions );
		appender.append( "to_vector('" );
		char separator = '[';
		for ( Object o : objects ) {
			appender.append( separator );
			elementFormatter.appendJdbcLiteral( appender, o, dialect, wrapperOptions );
			separator = ',';
		}
		appender.append( "]'," );
		appender.append( vectorParameters );
		appender.append( ')' );
	}

	private Object[] unwrapArray(Object value, WrapperOptions wrapperOptions) {
		return unwrap( value, Object[].class, wrapperOptions );
	}
}
