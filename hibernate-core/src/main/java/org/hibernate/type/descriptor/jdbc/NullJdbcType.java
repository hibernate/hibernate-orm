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
 * Descriptor for binding nulls with Types.NULL
 *
 * @author Christian Beikov
 */
public class NullJdbcType implements JdbcType {
	/**
	 * Singleton access
	 */
	public static final NullJdbcType INSTANCE = new NullJdbcType();

	@Override
	public int getJdbcTypeCode() {
		return Types.NULL;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<X>(javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return null;
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return null;
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return null;
			}
		};
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {

			@Override
			protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
				st.setNull( index, Types.NULL );
			}

			@Override
			protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
				st.setNull( name, Types.NULL );
			}

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) {
				throw new UnsupportedOperationException( getClass().getName() + " should only be used to bind null" );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) {
				throw new UnsupportedOperationException( getClass().getName() + " should only be used to bind null" );
			}
		};
	}
}
