/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;

/**
 * GaussDB-specific {@link LocalDateJdbcType} that reads {@code DATE} columns via
 * {@link ResultSet#getDate(int)} instead of {@code getObject(int, LocalDate.class)}.
 *
 * <p>GaussDB M mode exposes its {@code DATE} type as the non-standard {@code datea} SQL type
 * (JDBC type code {@link java.sql.Types#OTHER}), which the gsjdbc4 driver cannot convert to
 * {@code java.time.LocalDate} through {@code getObject(int, Class)} (it throws
 * "conversion to class java.time.LocalDate from datea not supported"). {@code getDate(int)}
 * reads the value fine, and {@link org.hibernate.type.descriptor.java.LocalDateJavaType#wrap}
 * converts the resulting {@link java.sql.Date} to a {@link java.time.LocalDate}. {@code TIME}
 * and {@code TIMESTAMP} columns report the standard JDBC type codes and are unaffected, so only
 * the {@code LOCAL_DATE} descriptor is overridden.
 */
public class GaussDBLocalDateJdbcType extends LocalDateJdbcType {
	public static final GaussDBLocalDateJdbcType INSTANCE = new GaussDBLocalDateJdbcType();

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new GaussDBLocalDateExtractor<>( javaType, this );
	}

	private static class GaussDBLocalDateExtractor<X> extends BasicExtractor<X> {
		private GaussDBLocalDateExtractor(JavaType<X> javaType, GaussDBLocalDateJdbcType jdbcType) {
			super( javaType, jdbcType );
		}

		@Override
		protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
			return getJavaType().wrap( rs.getDate( paramIndex ), options );
		}

		@Override
		protected X doExtract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
			return getJavaType().wrap( statement.getDate( paramIndex ), options );
		}

		@Override
		protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
			return getJavaType().wrap( statement.getDate( name ), options );
		}
	}
}
