/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.OffsetDateTimeJdbcType;

/**
 * GaussDB-specific {@link OffsetDateTimeJdbcType} that reads {@code DATETIME}/{@code TIMESTAMP}
 * columns via {@link ResultSet#getTimestamp(int)} and writes via {@link PreparedStatement#setTimestamp(int, Timestamp)}
 * instead of {@code getObject}/{@code setObject(int, OffsetDateTime, int)}.
 *
 * <h3>Read path</h3>
 * gsjdbc4's {@code ResultSet.getObject(int, Class)} raises "conversion to class java.time.OffsetDateTime
 * from datetime not supported" for a {@code DATETIME} column under M mode (the failure surfaces when
 * Hibernate loads an entity whose {@code OffsetDateTime} attribute is read through the default
 * {@code GetObjectExtractor}). {@code getTimestamp(int)} reads the value fine, and
 * {@link org.hibernate.type.descriptor.java.OffsetDateTimeJavaType#wrap} converts the resulting
 * {@link Timestamp} to a {@code java.time.OffsetDateTime} (using the JVM default time zone).
 *
 * <h3>Write path</h3>
 * gsjdbc4's {@code setObject(int, OffsetDateTime, TIMESTAMP_WITH_TIMEZONE)} sends a
 * {@code timestamp with time zone} expression that M mode {@code datetime(6)} rejects with
 * "column ... is of type datetime but expression is of type timestamp with time zone".
 * {@code setTimestamp(int, Timestamp)} instead routes through {@code TimestampUtils.toString(Calendar, Timestamp)}
 * which emits a plain {@code datetime} literal and is accepted. {@code LocalDate}, {@code LocalTime},
 * {@code LocalDateTime} and {@code Instant} are handled by their own GaussDB-specific descriptors,
 * so only the {@code OFFSET_DATE_TIME} descriptor is overridden here.
 *
 * <p>Registered globally (both A and M mode): {@code getTimestamp}/{@code setTimestamp(Timestamp)} read/write
 * {@code TIMESTAMP}/{@code timestamptz} columns correctly under the openGauss PG kernel (A mode) as well,
 * so A mode is constructively unaffected.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on GaussDBLocalDateTimeJdbcType.
 */
public class GaussDBOffsetDateTimeJdbcType extends OffsetDateTimeJdbcType {
	public static final GaussDBOffsetDateTimeJdbcType INSTANCE = new GaussDBOffsetDateTimeJdbcType();

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new GaussDBOffsetDateTimeExtractor<>( javaType, this );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new GaussDBOffsetDateTimeBinder<>( javaType, this );
	}

	private static class GaussDBOffsetDateTimeExtractor<X> extends BasicExtractor<X> {
		private GaussDBOffsetDateTimeExtractor(JavaType<X> javaType, GaussDBOffsetDateTimeJdbcType jdbcType) {
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

	private static class GaussDBOffsetDateTimeBinder<X> extends BasicBinder<X> {
		private GaussDBOffsetDateTimeBinder(JavaType<X> javaType, GaussDBOffsetDateTimeJdbcType jdbcType) {
			super( javaType, jdbcType );
		}

		@Override
		protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
			final Timestamp timestamp = getJavaType().unwrap( value, Timestamp.class, options );
			st.setTimestamp( index, timestamp );
		}

		@Override
		protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
			final Timestamp timestamp = getJavaType().unwrap( value, Timestamp.class, options );
			st.setTimestamp( name, timestamp );
		}

		@Override
		protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
			st.setNull( index, Types.TIMESTAMP );
		}

		@Override
		protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
			st.setNull( name, Types.TIMESTAMP );
		}
	}
}
