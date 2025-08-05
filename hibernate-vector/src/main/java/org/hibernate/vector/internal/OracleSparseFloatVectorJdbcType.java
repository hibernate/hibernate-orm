/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import oracle.sql.VECTOR;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.vector.SparseFloatVector;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Specialized type mapping for sparse single-precision floating-point vector {@link SqlTypes#SPARSE_VECTOR_FLOAT32} SQL data type for Oracle.
 */
public class OracleSparseFloatVectorJdbcType extends AbstractOracleSparseVectorJdbcType {

	public OracleSparseFloatVectorJdbcType(JdbcType elementJdbcType, boolean isVectorSupported) {
		super( elementJdbcType, isVectorSupported );
	}

	@Override
	public String getVectorParameters() {
		return "*,float32,sparse";
	}

	@Override
	public String getFriendlyName() {
		return "SPARSE_VECTOR_FLOAT32";
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.SPARSE_VECTOR_FLOAT32;
	}

	@Override
	protected <X> Object getBindValue(JavaType<X> javaType, X value, WrapperOptions options) {
		if ( isVectorSupported ) {
			final SparseFloatVector sparseVector = javaType.unwrap( value, SparseFloatVector.class, options );
			return VECTOR.SparseFloatArray.of( sparseVector.size(), sparseVector.indices(), sparseVector.floats() );
		}
		else {
			return getStringVector( value, javaType, options );
		}
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				if ( isVectorSupported ) {
					return getJavaType().wrap( wrapNativeValue( rs.getObject( paramIndex, VECTOR.SparseFloatArray.class ) ), options );
				}
				else {
					return getJavaType().wrap( wrapStringValue( rs.getString( paramIndex ) ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				if ( isVectorSupported ) {
					return getJavaType().wrap( wrapNativeValue( statement.getObject( index, VECTOR.SparseFloatArray.class ) ), options );
				}
				else {
					return getJavaType().wrap( wrapStringValue( statement.getString( index ) ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				if ( isVectorSupported ) {
					return getJavaType().wrap( wrapNativeValue( statement.getObject( name, VECTOR.SparseFloatArray.class ) ), options );
				}
				else {
					return getJavaType().wrap( wrapStringValue( statement.getString( name ) ), options );
				}
			}

			private Object wrapNativeValue(VECTOR.SparseFloatArray nativeValue) {
				return nativeValue == null
						? null
						: new SparseFloatVector( nativeValue.length(), nativeValue.indices(), nativeValue.values() );
			}

			private Object wrapStringValue(String value) {
				return ((AbstractOracleVectorJdbcType) getJdbcType() ).getVectorArray( value );
			}

		};
	}

	@Override
	protected SparseFloatVector getVectorArray(String string) {
		if ( string == null ) {
			return null;
		}
		return new SparseFloatVector( string );
	}

	@Override
	protected Class<?> getNativeJavaType() {
		return VECTOR.SparseFloatArray.class;
	}

}
