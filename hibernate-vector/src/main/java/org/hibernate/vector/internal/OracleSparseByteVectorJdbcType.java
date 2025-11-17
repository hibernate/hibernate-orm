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
import org.hibernate.vector.SparseByteVector;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Specialized type mapping for sparse single-byte integer vector {@link SqlTypes#SPARSE_VECTOR_INT8} SQL data type for Oracle.
 */
public class OracleSparseByteVectorJdbcType extends AbstractOracleSparseVectorJdbcType {

	public OracleSparseByteVectorJdbcType(JdbcType elementJdbcType, boolean isVectorSupported) {
		super( elementJdbcType, isVectorSupported );
	}

	@Override
	public String getVectorParameters() {
		return "*,int8,sparse";
	}

	@Override
	public String getFriendlyName() {
		return "SPARSE_VECTOR_INT8";
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.SPARSE_VECTOR_INT8;
	}

	@Override
	protected <X> Object getBindValue(JavaType<X> javaType, X value, WrapperOptions options) {
		if ( isVectorSupported ) {
			final SparseByteVector sparseVector = javaType.unwrap( value, SparseByteVector.class, options );
			return VECTOR.SparseByteArray.of( sparseVector.size(), sparseVector.indices(), sparseVector.bytes() );
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
					return getJavaType().wrap( wrapNativeValue( rs.getObject( paramIndex, VECTOR.SparseByteArray.class ) ), options );
				}
				else {
					return getJavaType().wrap( wrapStringValue( rs.getString( paramIndex ) ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				if ( isVectorSupported ) {
					return getJavaType().wrap( wrapNativeValue( statement.getObject( index, VECTOR.SparseByteArray.class ) ), options );
				}
				else {
					return getJavaType().wrap( wrapStringValue( statement.getString( index ) ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				if ( isVectorSupported ) {
					return getJavaType().wrap( wrapNativeValue( statement.getObject( name, VECTOR.SparseByteArray.class ) ), options );
				}
				else {
					return getJavaType().wrap( wrapStringValue( statement.getString( name ) ), options );
				}
			}

			private Object wrapNativeValue(VECTOR.SparseByteArray nativeValue) {
				return nativeValue == null
						? null
						: new SparseByteVector( nativeValue.length(), nativeValue.indices(), nativeValue.values() );
			}

			private Object wrapStringValue(String value) {
				return ((AbstractOracleVectorJdbcType) getJdbcType() ).getVectorArray( value );
			}

		};
	}

	@Override
	protected SparseByteVector getVectorArray(String string) {
		if ( string == null ) {
			return null;
		}
		return new SparseByteVector( string );
	}

	@Override
	protected Class<?> getNativeJavaType() {
		return VECTOR.SparseByteArray.class;
	}

}
