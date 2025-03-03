/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import java.util.Arrays;
import java.util.BitSet;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleTypes;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Specialized type mapping for double-precision floating-point vector {@link SqlTypes#VECTOR_FLOAT64} SQL data type for Oracle.
 *
 * @author Hassan AL Meftah
 */
public class OracleDoubleVectorJdbcType extends AbstractOracleVectorJdbcType {

	private static final double[] EMPTY = new double[0];

	public OracleDoubleVectorJdbcType(JdbcType elementJdbcType, boolean isVectorSupported) {
		super( elementJdbcType, isVectorSupported );
	}


	@Override
	public void appendWriteExpression(String writeExpression, SqlAppender appender, Dialect dialect) {
		appender.append( "to_vector(" );
		appender.append( writeExpression );
		appender.append( ", *, FLOAT64)" );
	}

	@Override
	public String getFriendlyName() {
		return "VECTOR_FLOAT64";
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.VECTOR_FLOAT64;
	}

	@Override
	protected double[] getVectorArray(String string) {
		if ( string == null ) {
			return null;
		}
		if ( string.length() == 2 ) {
			return EMPTY;
		}
		final BitSet commaPositions = new BitSet();
		int size = 1;
		for ( int i = 1; i < string.length(); i++ ) {
			final char c = string.charAt( i );
			if ( c == ',' ) {
				commaPositions.set( i );
				size++;
			}
		}
		final double[] result = new double[size];
		int doubleStartIndex = 1;
		int commaIndex;
		int index = 0;
		while ( ( commaIndex = commaPositions.nextSetBit( doubleStartIndex ) ) != -1 ) {
			result[index++] = Double.parseDouble( string.substring( doubleStartIndex, commaIndex ) );
			doubleStartIndex = commaIndex + 1;
		}
		result[index] = Double.parseDouble( string.substring( doubleStartIndex, string.length() - 1 ) );
		return result;
	}

	@Override
	protected <T> String getStringVector(T vector, JavaType<T> javaTypeDescriptor, WrapperOptions options) {
		return Arrays.toString( javaTypeDescriptor.unwrap( vector, double[].class, options ) );
	}

	protected Class<?> getNativeJavaType() {
		return double[].class;
	}

	@Override
	protected int getNativeTypeCode() {
		return OracleTypes.VECTOR_FLOAT64;
	}
}
