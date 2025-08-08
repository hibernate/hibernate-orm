/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import java.util.Arrays;

import org.hibernate.dialect.OracleTypes;
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

	public OracleDoubleVectorJdbcType(JdbcType elementJdbcType, boolean isVectorSupported) {
		super( elementJdbcType, isVectorSupported );
	}

	@Override
	public String getVectorParameters() {
		return "*,float64";
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
		return VectorHelper.parseDoubleVector( string );
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
