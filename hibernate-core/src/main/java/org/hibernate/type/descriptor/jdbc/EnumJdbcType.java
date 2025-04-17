/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterCharacterData;

import static org.hibernate.type.SqlTypes.ENUM;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * Represents an {@code enum} type for databases like MySQL and H2.
 * <p>
 * Hibernate will automatically use this for enums mapped
 * as {@link jakarta.persistence.EnumType#STRING}.
 *
 * @see org.hibernate.type.SqlTypes#ENUM
 * @see org.hibernate.dialect.MySQLDialect#getEnumTypeDeclaration(String, String[])
 *
 * @author Gavin King
 */
public class EnumJdbcType implements JdbcType {

	public static final EnumJdbcType INSTANCE = new EnumJdbcType();

	@Override
	public int getJdbcTypeCode() {
		return VARCHAR;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return ENUM;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterCharacterData<>( javaType );
	}

	@Override
	public String getFriendlyName() {
		return "ENUM";
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return String.class;
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

			@Override
			public Object getBindValue(X value, WrapperOptions options) throws SQLException {
				return getJavaType().unwrap( value, String.class, options );
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
