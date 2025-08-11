/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

public class PGVectorJdbcLiteralFormatterBinaryVector<T> extends BasicJdbcLiteralFormatter<T> {

	public PGVectorJdbcLiteralFormatterBinaryVector(JavaType<T> javaType) {
		super( javaType );
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions) {
		appender.append( "cast('" );
		appender.append( VectorHelper.toBitString( unwrap( value, byte[].class, wrapperOptions ) ) );
		appender.append( "' as varbit)" );
	}

}
