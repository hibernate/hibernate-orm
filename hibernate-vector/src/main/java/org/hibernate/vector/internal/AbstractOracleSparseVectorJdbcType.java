/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.vector.AbstractSparseVector;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractOracleSparseVectorJdbcType extends AbstractOracleVectorJdbcType {

	public AbstractOracleSparseVectorJdbcType(JdbcType elementJdbcType, boolean isVectorSupported) {
		super( elementJdbcType, isVectorSupported );
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return new OracleJdbcLiteralFormatterSparseVector<>( javaTypeDescriptor, getVectorParameters() );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				if ( isVectorSupported ) {
					st.setObject( index, getBindValue( value, options ) );
				}
				else {
					st.setString( index, stringVector( value, options ) );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				if ( isVectorSupported ) {
					st.setObject( name, getBindValue( value, options ) );
				}
				else {
					st.setString( name, stringVector( value, options ) );
				}
			}

			private String stringVector(X value, WrapperOptions options) {
				return ((AbstractOracleSparseVectorJdbcType) getJdbcType()).getStringVector( value, getJavaType(), options );
			}

			@Override
			public Object getBindValue(X value, WrapperOptions options) {
				return ((AbstractOracleSparseVectorJdbcType) getJdbcType()).getBindValue( getJavaType(), value, options );
			}
		};
	}

	protected abstract <X> Object getBindValue(JavaType<X> javaType, X value, WrapperOptions options);

	@Override
	protected <T> String getStringVector(T vector, JavaType<T> javaTypeDescriptor, WrapperOptions options) {
		return javaTypeDescriptor.unwrap( vector, AbstractSparseVector.class, options ).toString();
	}

	@Override
	protected Class<?> getNativeJavaType() {
		return Object.class;
	}

	@Override
	protected int getNativeTypeCode() {
		return SqlTypes.OTHER;
	}
}
