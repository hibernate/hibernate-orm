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
 * Specialized type mapping for single-precision floating-point vector {@link SqlTypes#VECTOR_FLOAT32} SQL data type for DB2.
 */

public class DB2FloatVectorJdbcType extends AbstractDB2VectorJdbcType {

	public DB2FloatVectorJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	public String getVectorParameters(@Nullable Size size) {
		assert size != null;
		return size.getArrayLength() + ",float32";
	}

	@Override
	public String getFriendlyName() {
		return "VECTOR_FLOAT32";
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.VECTOR_FLOAT32;
	}

	@Override
	protected float[] getVectorArray(String string) {
		return VectorHelper.parseFloatVector( string );
	}

	@Override
	protected <T> String getStringVector(T vector, JavaType<T> javaTypeDescriptor, WrapperOptions options) {
		return Arrays.toString( javaTypeDescriptor.unwrap( vector, float[].class, options ) );
	}
}
