/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static org.hibernate.vector.VectorHelper.parseFloatVector;

public class MySQLVectorJdbcType extends ArrayJdbcType {

	private final int sqlType;

	public MySQLVectorJdbcType(JdbcType elementJdbcType, int sqlType) {
		super( elementJdbcType );
		this.sqlType = sqlType;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return sqlType;
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( float[].class );
	}

	@Override
	public void appendWriteExpression(String writeExpression, SqlAppender appender, Dialect dialect) {
		appender.append( "string_to_vector(" );
		appender.append( writeExpression );
		appender.append( ')' );
	}

	@Override
	public @Nullable String castFromPattern(JdbcMapping sourceMapping) {
		return sourceMapping.getJdbcType().isStringLike() ? "string_to_vector(?1)" : null;
	}

	@Override
	public @Nullable String castToPattern(JdbcMapping targetJdbcMapping) {
		return targetJdbcMapping.getJdbcType().isStringLike() ? "vector_to_string(?1)" : null;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( parseFloatVector( rs.getBytes( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( parseFloatVector( statement.getBytes( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaTypeDescriptor.wrap( parseFloatVector( statement.getBytes( name ) ), options );
			}

		};
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new BasicBinder<>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setString( index, getBindValue( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setString( name, getBindValue( value, options ) );
			}

			@Override
			public String getBindValue(X value, WrapperOptions options) {
				return Arrays.toString( getJavaType().unwrap( value, float[].class, options ) );
			}
		};
	}
}
