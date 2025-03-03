/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Descriptor for {@link Types#TINYINT TINYINT} handling, when the {@link Types#SMALLINT SMALLINT} DDL type code is used.
 * This type binds and extracts {@link Short} instead of {@link Byte} through JDBC,
 * because it is not specified whether the conversion from {@link Byte} to {@link Short} is signed or unsigned,
 * yet we need the conversion to be signed, which is properly handled by the {@link org.hibernate.type.descriptor.java.JavaType#unwrap(Object, Class, WrapperOptions)} implementations.
 */
public class TinyIntAsSmallIntJdbcType extends TinyIntJdbcType {
	public static final TinyIntAsSmallIntJdbcType INSTANCE = new TinyIntAsSmallIntJdbcType();

	public TinyIntAsSmallIntJdbcType() {
	}

	@Override
	public int getDdlTypeCode() {
		return Types.SMALLINT;
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Short.class;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setShort( index, javaType.unwrap( value, Short.class, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				st.setShort( name, javaType.unwrap( value, Short.class, options ) );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return javaType.wrap( rs.getShort( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getShort( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return javaType.wrap( statement.getShort( name ), options );
			}
		};
	}
}
