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

public class PGVectorJdbcLiteralFormatterSparseVector<T> extends BasicJdbcLiteralFormatter<T> {

	public PGVectorJdbcLiteralFormatterSparseVector(JavaType<T> javaType) {
		super( javaType );
	}

	@Override
	public void appendJdbcLiteral(SqlAppender appender, T value, Dialect dialect, WrapperOptions wrapperOptions) {
		appender.append( "cast('" );
		final AbstractSparseVector<?> sparseVector = unwrap( value, AbstractSparseVector.class, wrapperOptions );
		char separator = '{';
		final int[] indices = sparseVector.indices();
		if ( sparseVector instanceof SparseFloatVector floatVector ) {
			final float[] floats = floatVector.floats();
			for ( int i = 0; i < floats.length; i++ ) {
				appender.appendSql( separator );
				appender.appendSql( indices[i] + 1 );
				appender.appendSql( ':' );
				appender.appendSql( floats[i] );
				separator = ',';
			}
		}
		else if ( sparseVector instanceof SparseDoubleVector doubleVector ) {
			final double[] doubles = doubleVector.doubles();
			for ( int i = 0; i < doubles.length; i++ ) {
				appender.appendSql( separator );
				appender.appendSql( indices[i] + 1 );
				appender.appendSql( ':' );
				appender.appendSql( doubles[i] );
				separator = ',';
			}
		}
		else if ( sparseVector instanceof SparseByteVector byteVector ) {
			final byte[] bytes = byteVector.bytes();
			for ( int i = 0; i < bytes.length; i++ ) {
				appender.appendSql( separator );
				appender.appendSql( indices[i] + 1 );
				appender.appendSql( ':' );
				appender.appendSql( bytes[i] );
				separator = ',';
			}
		}
		else {
			throw new IllegalArgumentException( "Unsupported sparse vector type: " + sparseVector.getClass().getName() );
		}
		appender.append( "}/" );
		appender.appendSql( sparseVector.size() );
		appender.append( "' as sparsevec)" );
	}

}
