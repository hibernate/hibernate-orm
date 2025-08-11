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
import org.hibernate.vector.SparseByteVector;
import org.hibernate.vector.SparseDoubleVector;
import org.hibernate.vector.SparseFloatVector;

public class OracleJdbcLiteralFormatterSparseVector<T> extends BasicJdbcLiteralFormatter<T> {

	private final String vectorParameters;

	public OracleJdbcLiteralFormatterSparseVector(JavaType<T> javaType, String vectorParameters) {
		super( javaType );
		this.vectorParameters = vectorParameters;
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions) {
		appender.appendSql( "to_vector('" );
		final AbstractSparseVector<?> sparseVector = unwrap( value, AbstractSparseVector.class, wrapperOptions );
		appender.appendSql( '[' );
		appender.appendSql( sparseVector.size() );
		appender.appendSql( ',' );
		char separator = '[';
		for ( int index : sparseVector.indices() ) {
			appender.appendSql( separator );
			appender.appendSql( index );
			separator = ',';
		}
		appender.appendSql( "]," );
		separator = '[';
		if ( sparseVector instanceof SparseFloatVector floatVector ) {
			for ( float f : floatVector.floats() ) {
				appender.appendSql( separator );
				appender.appendSql( f );
				separator = ',';
			}
		}
		else if ( sparseVector instanceof SparseDoubleVector doubleVector ) {
			for ( double d : doubleVector.doubles() ) {
				appender.appendSql( separator );
				appender.appendSql( d );
				separator = ',';
			}
		}
		else if ( sparseVector instanceof SparseByteVector byteVector ) {
			for ( byte b : byteVector.bytes() ) {
				appender.appendSql( separator );
				appender.appendSql( b );
				separator = ',';
			}
		}
		else {
			throw new IllegalArgumentException( "Unsupported sparse vector type: " + sparseVector.getClass().getName() );
		}
		appender.appendSql( "]]'," );
		appender.appendSql( vectorParameters );
		appender.appendSql( ')' );
	}

}
