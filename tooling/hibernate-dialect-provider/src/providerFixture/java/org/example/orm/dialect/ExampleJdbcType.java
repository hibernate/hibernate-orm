/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/// JDBC descriptor supplied by the external provider fixture.
///
/// @author Steve Ebersole
public final class ExampleJdbcType implements JdbcType {
	public static final int TYPE_CODE = 60_100;
	public static final ExampleJdbcType INSTANCE = new ExampleJdbcType();

	private ExampleJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.VARCHAR;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return TYPE_CODE;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement statement, X value, int index, WrapperOptions options)
					throws SQLException {
				statement.setString( index, getJavaType().unwrap( value, String.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement statement, X value, String name, WrapperOptions options)
					throws SQLException {
				statement.setString( name, getJavaType().unwrap( value, String.class, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet resultSet, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( resultSet.getString( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
					throws SQLException {
				return getJavaType().wrap( statement.getString( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaType().wrap( statement.getString( name ), options );
			}
		};
	}
}
