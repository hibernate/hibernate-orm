/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.internal;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JavaTimeJdbcType;

/**
 * Support for extracting values directly through `getObject` JDBC driver calls.
 *
 * @author Steve Ebersole
 */
public class GetObjectExtractor<T> extends BasicExtractor<T> {
	private final Class<?> baseClass;

	public GetObjectExtractor(JavaType<T> javaType, JavaTimeJdbcType jdbcType, Class<?> baseClass) {
		super( javaType, jdbcType );
		this.baseClass = baseClass;
	}

	@Override
	protected T doExtract(
			ResultSet rs,
			int paramIndex,
			WrapperOptions options) throws SQLException {
		return getJavaType().wrap( rs.getObject( paramIndex, baseClass ), options );
	}

	@Override
	protected T doExtract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
		return getJavaType().wrap( statement.getObject( paramIndex, baseClass ), options );
	}

	@Override
	protected T doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
		return getJavaType().wrap( statement.getObject( name, baseClass ), options );
	}
}
