/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterUUIDData;

/**
 * Specialized type mapping for {@link UUID} and the UUID SQL data type,
 * which binds and reads the UUID through JDBC <code>getString</code>/<code>setString</code> methods.
 *
 */
public class VarcharUUIDJdbcType implements JdbcType {
	/**
	 * Singleton access
	 */
	public static final VarcharUUIDJdbcType INSTANCE = new VarcharUUIDJdbcType();

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.VARCHAR;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.UUID;
	}

	@Override
	public String toString() {
		return "MariaDBUUIDJdbcType";
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return UUID.class;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterUUIDData<>( javaType );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setString( index, getJavaType().unwrap( value, String.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setString( name, getJavaType().unwrap( value, String.class, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( rs.getString( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getString( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getString( name ), options );
			}
		};
	}
}
