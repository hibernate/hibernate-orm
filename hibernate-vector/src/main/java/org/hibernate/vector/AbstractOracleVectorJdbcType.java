/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.ByteJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterArray;

/**
 * Specialized type mapping for generic vector {@link SqlTypes#VECTOR} SQL data type for Oracle.
 * <p>
 * This class handles generic vectors represented by an asterisk (*) in the format,
 * allowing for different element types within the vector.
 *
 * @author Hassan AL Meftah
 */
public abstract class AbstractOracleVectorJdbcType extends ArrayJdbcType {

	final boolean isVectorSupported;

	public AbstractOracleVectorJdbcType(JdbcType elementJdbcType, boolean isVectorSupported) {
		super( elementJdbcType );
		this.isVectorSupported = isVectorSupported;
	}

	public abstract void appendWriteExpression(String writeExpression, SqlAppender appender, Dialect dialect);

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.VECTOR;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		final JavaType<T> elementJavaType;
		if ( javaTypeDescriptor instanceof PrimitiveByteArrayJavaType ) {
			// Special handling needed for Byte[], because that would conflict with the VARBINARY mapping
			//noinspection unchecked
			elementJavaType = (JavaType<T>) ByteJavaType.INSTANCE;
		}
		else if ( javaTypeDescriptor instanceof BasicPluralJavaType ) {
			//noinspection unchecked
			elementJavaType = ( (BasicPluralJavaType<T>) javaTypeDescriptor ).getElementJavaType();
		}
		else {
			throw new IllegalArgumentException( "not a BasicPluralJavaType" );
		}
		return new JdbcLiteralFormatterArray<>(
				javaTypeDescriptor,
				getElementJdbcType().getJdbcLiteralFormatter( elementJavaType )
		);
	}

	@Override
	public String toString() {
		return "OracleVectorTypeDescriptor";
	}


	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				if ( isVectorSupported ) {
					st.setObject( index, value, ( (AbstractOracleVectorJdbcType) getJdbcType() ).getNativeTypeCode() );
				}
				else {
					st.setString( index, ( (AbstractOracleVectorJdbcType) getJdbcType() ).getStringVector( value, getJavaType(), options ) );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				if ( isVectorSupported ) {
					st.setObject( name, value, ( (AbstractOracleVectorJdbcType) getJdbcType() ).getNativeTypeCode() );
				}
				else {
					st.setString( name, ( (AbstractOracleVectorJdbcType) getJdbcType() ).getStringVector( value, getJavaType(), options ) );
				}
			}

		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				if ( isVectorSupported ) {
					return getJavaType().wrap( rs.getObject( paramIndex, ((AbstractOracleVectorJdbcType) getJdbcType() ).getNativeJavaType() ), options );
				}
				else {
					return getJavaType().wrap( ((AbstractOracleVectorJdbcType) getJdbcType() ).getVectorArray( rs.getString( paramIndex ) ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				if ( isVectorSupported ) {
					return getJavaType().wrap( statement.getObject( index, ((AbstractOracleVectorJdbcType) getJdbcType() ).getNativeJavaType() ), options );
				}
				else {
					return getJavaType().wrap( ((AbstractOracleVectorJdbcType) getJdbcType() ).getVectorArray( statement.getString( index ) ), options );
				}
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				if ( isVectorSupported ) {
					return getJavaType().wrap( statement.getObject( name, ((AbstractOracleVectorJdbcType) getJdbcType() ).getNativeJavaType() ), options );
				}
				else {
					return getJavaType().wrap( ((AbstractOracleVectorJdbcType) getJdbcType() ).getVectorArray( statement.getString( name ) ), options );
				}
			}

		};
	}

	protected abstract <T> T getVectorArray(String string);

	protected abstract <T> String getStringVector(T vector, JavaType<T> javaTypeDescriptor, WrapperOptions options);

	protected abstract Class<?> getNativeJavaType();

	protected abstract int getNativeTypeCode();

}
