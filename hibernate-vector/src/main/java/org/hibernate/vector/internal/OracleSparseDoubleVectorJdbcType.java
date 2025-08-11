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
import org.hibernate.vector.SparseDoubleVector;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Specialized type mapping for sparse double-precision floating-point vector {@link SqlTypes#SPARSE_VECTOR_FLOAT64} SQL data type for Oracle.
 */
public class OracleSparseDoubleVectorJdbcType extends AbstractOracleSparseVectorJdbcType {

	public OracleSparseDoubleVectorJdbcType(JdbcType elementJdbcType, boolean isVectorSupported) {
		super( elementJdbcType, isVectorSupported );
	}

	@Override
	public String getVectorParameters() {
		return "*,float64,sparse";
	}

	@Override
	public String getFriendlyName() {
		return "SPARSE_VECTOR_FLOAT64";
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.SPARSE_VECTOR_FLOAT64;
	}

	@Override
	protected <X> Object getBindValue(JavaType<X> javaType, X value, WrapperOptions options) {
		if ( isVectorSupported ) {
			final SparseDoubleVector sparseVector = javaType.unwrap( value, SparseDoubleVector.class, options );
			return VECTOR.SparseDoubleArray.of( sparseVector.size(), sparseVector.indices(), sparseVector.doubles() );
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
					return getJavaType().wrap( wrapNativeValue( rs.getObject( paramIndex, VECTOR.SparseDoubleArray.class ) ), options );
				}
				else {
					return getJavaType().wrap( wrapStringValue( rs.getString( paramIndex ) ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				if ( isVectorSupported ) {
					return getJavaType().wrap( wrapNativeValue( statement.getObject( index, VECTOR.SparseDoubleArray.class ) ), options );
				}
				else {
					return getJavaType().wrap( wrapStringValue( statement.getString( index ) ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				if ( isVectorSupported ) {
					return getJavaType().wrap( wrapNativeValue( statement.getObject( name, VECTOR.SparseDoubleArray.class ) ), options );
				}
				else {
					return getJavaType().wrap( wrapStringValue( statement.getString( name ) ), options );
				}
			}

			private Object wrapNativeValue(VECTOR.SparseDoubleArray nativeValue) {
				return nativeValue == null
						? null
						: new SparseDoubleVector( nativeValue.length(), nativeValue.indices(), nativeValue.values() );
			}

			private Object wrapStringValue(String value) {
				return ((AbstractOracleVectorJdbcType) getJdbcType() ).getVectorArray( value );
			}

		};
	}

	@Override
	protected SparseDoubleVector getVectorArray(String string) {
		if ( string == null ) {
			return null;
		}
		return new SparseDoubleVector( string );
	}

	@Override
	protected Class<?> getNativeJavaType() {
		return VECTOR.SparseDoubleArray.class;
	}

}
