/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;
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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Specialized type mapping for generic vector {@link SqlTypes#VECTOR} SQL data type for DB2.
 */
public abstract class AbstractDB2VectorJdbcType extends ArrayJdbcType {

	public AbstractDB2VectorJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	public @Nullable String castToPattern(JdbcMapping targetJdbcMapping, @Nullable Size size) {
		return targetJdbcMapping.getJdbcType().isStringLike() ? "vector_serialize(?1 returning ?2)" : null;
	}

	@Override
	public @Nullable String castFromPattern(JdbcMapping sourceMapping, @Nullable Size size) {
		return sourceMapping.getJdbcType().isStringLike() ? "vector(?1," + getVectorParameters( size ) + ")" : null;
	}

	@Override
	public void appendWriteExpression(
			String writeExpression,
			@Nullable Size size,
			SqlAppender appender,
			Dialect dialect) {
		appender.append( "vector(" );
		appender.append( writeExpression );
		appender.append( ',' );
		appender.append( getVectorParameters( size ) );
		appender.append( ')' );
	}

	@Override
	public boolean isWriteExpressionTyped(Dialect dialect) {
		return true;
	}

	public abstract String getVectorParameters(@Nullable Size size);

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
			elementJavaType = ((BasicPluralJavaType<T>) javaTypeDescriptor).getElementJavaType();
		}
		else {
			throw new IllegalArgumentException( "not a BasicPluralJavaType" );
		}
		return new DB2JdbcLiteralFormatterVector<>(
				javaTypeDescriptor,
				getElementJdbcType().getJdbcLiteralFormatter( elementJavaType ),
				this
		);
	}

	@Override
	public String toString() {
		return "DB2VectorTypeDescriptor";
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setString( index,
						((AbstractDB2VectorJdbcType) getJdbcType()).getStringVector( value, getJavaType(), options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setString( name,
						((AbstractDB2VectorJdbcType) getJdbcType()).getStringVector( value, getJavaType(), options ) );
			}

		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaType().wrap(
						((AbstractDB2VectorJdbcType) getJdbcType()).getVectorArray( rs.getString( paramIndex ) ),
						options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap(
						((AbstractDB2VectorJdbcType) getJdbcType()).getVectorArray( statement.getString( index ) ),
						options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaType().wrap(
						((AbstractDB2VectorJdbcType) getJdbcType()).getVectorArray( statement.getString( name ) ),
						options );
			}

		};
	}

	protected abstract Object getVectorArray(String string);

	protected abstract <T> String getStringVector(T vector, JavaType<T> javaTypeDescriptor, WrapperOptions options);

}
