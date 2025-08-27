/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.GetObjectExtractor;

/**
 * Variant of the {@link GetObjectExtractor} that catches a {@link NullPointerException},
 * because the DB2 JDBC driver runs into that exception when trying to access a {@code null} value
 * with the {@code getObject(int, Class)} and {@code getObject(String, Class)} methods.
 *
 * @author Christian Beikov
 */
public class DB2GetObjectExtractor<T> extends GetObjectExtractor<T> {
	public DB2GetObjectExtractor(
			JavaType<T> javaType,
			JavaTimeJdbcType jdbcType,
			Class<?> baseClass) {
		super( javaType, jdbcType, baseClass );
	}

	@Override
	protected T doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
		try {
			return super.doExtract( rs, paramIndex, options );
		}
		catch (NullPointerException ex) {
			final Object object = rs.getObject( paramIndex );
			if ( object == null ) {
				return null;
			}
			throw ex;
		}
	}

	@Override
	protected T doExtract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
		try {
			return super.doExtract( statement, paramIndex, options );
		}
		catch (NullPointerException ex) {
			final Object object = statement.getObject( paramIndex );
			if ( object == null ) {
				return null;
			}
			throw ex;
		}
	}

	@Override
	protected T doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
		try {
			return super.doExtract( statement, name, options );
		}
		catch (NullPointerException ex) {
			final Object object = statement.getObject( name );
			if ( object == null ) {
				return null;
			}
			throw ex;
		}
	}
}
