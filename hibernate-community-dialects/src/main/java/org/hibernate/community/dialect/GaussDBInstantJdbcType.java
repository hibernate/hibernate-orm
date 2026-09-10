/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.InstantJdbcType;

/**
 * GaussDB-specific {@link InstantJdbcType} that reads {@code DATETIME}/{@code TIMESTAMP}
 * columns via {@link ResultSet#getTimestamp(int)} instead of {@code getObject(int, Instant.class)}.
 *
 * <p>gsjdbc4's {@code ResultSet.getObject(int, Class)} cannot convert a {@code DATETIME} column to
 * {@code java.time.Instant} &mdash; it throws "conversion to class java.time.Instant from 93 not
 * supported" (93 is the JDBC {@code TIMESTAMP}/{@code DATETIME} type code). {@code getTimestamp(int)}
 * reads the value fine, and {@link org.hibernate.type.descriptor.java.InstantJavaType#wrap} converts
 * the resulting {@link Timestamp} to an {@code java.time.Instant}. Only the {@code INSTANT} descriptor
 * is overridden.
 *
 * <p>Registered globally (both A and M mode): {@code getTimestamp} reads {@code TIMESTAMP} columns
 * correctly under the openGauss PG kernel (A mode) as well, so A mode is constructively unaffected.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on GaussDBLocalDateJdbcType.
 */
public class GaussDBInstantJdbcType extends InstantJdbcType {
	public static final GaussDBInstantJdbcType INSTANCE = new GaussDBInstantJdbcType();

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new GaussDBInstantExtractor<>( javaType, this );
	}

	private static class GaussDBInstantExtractor<X> extends BasicExtractor<X> {
		private GaussDBInstantExtractor(JavaType<X> javaType, GaussDBInstantJdbcType jdbcType) {
			super( javaType, jdbcType );
		}

		@Override
		protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
			return getJavaType().wrap( rs.getTimestamp( paramIndex ), options );
		}

		@Override
		protected X doExtract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
			return getJavaType().wrap( statement.getTimestamp( paramIndex ), options );
		}

		@Override
		protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
			return getJavaType().wrap( statement.getTimestamp( name ), options );
		}
	}
}
