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
 * Specialized type mapping for {@link UUID} and the UUID SQL data type.
 *
 * @author Steve Ebersole
 * @author David Driscoll
 */
public class UUIDJdbcType implements JdbcType {
	/**
	 * Singleton access
	 */
	public static final UUIDJdbcType INSTANCE = new UUIDJdbcType();

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.OTHER;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.UUID;
	}

	@Override
	public String toString() {
		return "UUIDJdbcType";
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
				st.setObject( index, getJavaType().unwrap( value, UUID.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setObject( name, getJavaType().unwrap( value, UUID.class, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( rs.getObject( paramIndex, UUID.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getObject( index, UUID.class ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( statement.getObject( name, UUID.class ), options );
			}
		};
	}
}
