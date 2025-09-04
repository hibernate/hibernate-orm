/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.dialect.OracleTypes;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.util.Arrays;

/**
 * Specialized type mapping for binary vector {@link SqlTypes#VECTOR_BINARY} SQL data type for Oracle.
 */
public class OracleBinaryVectorJdbcType extends AbstractOracleVectorJdbcType {

	public OracleBinaryVectorJdbcType(JdbcType elementJdbcType, boolean isVectorSupported) {
		super( elementJdbcType, isVectorSupported );
	}

	@Override
	public String getVectorParameters() {
		return "*,binary";
	}

	@Override
	public String getFriendlyName() {
		return "VECTOR_BINARY";
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.VECTOR_BINARY;
	}

	@Override
	protected byte[] getVectorArray(String string) {
		return VectorHelper.parseByteVector( string );
	}

	@Override
	protected <T> String getStringVector(T vector, JavaType<T> javaTypeDescriptor, WrapperOptions options) {
		return Arrays.toString( javaTypeDescriptor.unwrap( vector, byte[].class, options ) );
	}

	protected Class<?> getNativeJavaType(){
		return byte[].class;
	}

	protected int getNativeTypeCode(){
		return OracleTypes.VECTOR_BINARY;
	}

}
