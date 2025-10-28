/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.spi.BasicJdbcLiteralFormatter;

public class DB2JdbcLiteralFormatterVector<T> extends BasicJdbcLiteralFormatter<T> {

	private final JdbcLiteralFormatter<Object> elementFormatter;
	private final AbstractDB2VectorJdbcType db2VectorJdbcType;

	public DB2JdbcLiteralFormatterVector(JavaType<T> javaType, JdbcLiteralFormatter<?> elementFormatter, AbstractDB2VectorJdbcType db2VectorJdbcType) {
		super( javaType );
		//noinspection unchecked
		this.elementFormatter = (JdbcLiteralFormatter<Object>) elementFormatter;
		this.db2VectorJdbcType = db2VectorJdbcType;
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions) {
		final Object[] objects = unwrapArray( value, wrapperOptions );
		appender.append( "vector('" );
		char separator = '[';
		for ( Object o : objects ) {
			appender.append( separator );
			elementFormatter.appendJdbcLiteral( appender, o, dialect, wrapperOptions );
			separator = ',';
		}
		appender.append( "]'," );
		appender.append( db2VectorJdbcType.getVectorParameters( new Size().setArrayLength( objects.length ) ) );
		appender.append( ')' );
	}

	private Object[] unwrapArray(Object value, WrapperOptions wrapperOptions) {
		return unwrap( value, Object[].class, wrapperOptions );
	}
}
