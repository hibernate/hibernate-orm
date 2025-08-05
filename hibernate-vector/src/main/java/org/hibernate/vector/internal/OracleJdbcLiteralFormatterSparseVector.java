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
import org.hibernate.vector.AbstractSparseVector;

public class OracleJdbcLiteralFormatterSparseVector<T> extends BasicJdbcLiteralFormatter<T> {

	private final String vectorParameters;

	public OracleJdbcLiteralFormatterSparseVector(JavaType<T> javaType, String vectorParameters) {
		super( javaType );
		this.vectorParameters = vectorParameters;
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions) {
		appender.append( "to_vector(" );
		appender.append( getJavaType().unwrap( value, AbstractSparseVector.class, wrapperOptions ).toString() );
		appender.append( "," );
		appender.append( vectorParameters );
		appender.append( ')' );
	}

}
