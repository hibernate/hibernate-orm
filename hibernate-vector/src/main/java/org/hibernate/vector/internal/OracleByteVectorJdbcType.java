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
 * Specialized type mapping for single-byte integer vector {@link SqlTypes#VECTOR_INT8} SQL data type for Oracle.
 *
 * @author Hassan AL Meftah
 */
public class OracleByteVectorJdbcType extends AbstractOracleVectorJdbcType {

	public OracleByteVectorJdbcType(JdbcType elementJdbcType, boolean isVectorSupported) {
		super( elementJdbcType, isVectorSupported );
	}

	@Override
	public String getVectorParameters() {
		return "*,int8";
	}

	@Override
	public String getFriendlyName() {
		return "VECTOR_INT8";
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.VECTOR_INT8;
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
		return OracleTypes.VECTOR_INT8;
	}

}
