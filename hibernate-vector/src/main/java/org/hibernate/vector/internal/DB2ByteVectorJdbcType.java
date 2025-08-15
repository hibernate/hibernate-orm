/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.util.Arrays;

/**
 * Specialized type mapping for single-byte integer vector {@link SqlTypes#VECTOR_INT8} SQL data type for DB2.
 */
public class DB2ByteVectorJdbcType extends AbstractDB2VectorJdbcType {

	public DB2ByteVectorJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	public String getVectorParameters(@Nullable Size size) {
		assert size != null;
		return size.getArrayLength() + ",int8";
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

}
