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
import java.time.LocalDateTime;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

/**
 * GaussDB-specific {@link LocalDateTimeJdbcType} that reads {@code DATETIME}/{@code TIMESTAMP}
 * columns via {@link ResultSet#getTimestamp(int)} and writes via {@link PreparedStatement#setTimestamp(int, Timestamp)}
 * instead of {@code getObject}/{@code setObject(int, LocalDateTime, int)}.
 *
 * <h3>Read path</h3>
 * gsjdbc4's {@code ResultSet.getObject(int, Class)} cannot convert a {@code DATETIME} column to
 * {@code java.time.LocalDateTime} &mdash; it throws "Cannot convert the column of type DATETIME to
 * requested type timestamp." {@code getTimestamp(int)} reads the value fine, and
 * {@link org.hibernate.type.descriptor.java.LocalDateTimeJavaType#wrap} converts the resulting
 * {@link Timestamp} to a {@code java.time.LocalDateTime}.
 *
 * <h3>Write path</h3>
 * gsjdbc4's {@code setObject(int, LocalDateTime, TIMESTAMP)} routes to {@code setTimestamp(LocalDateTime)},
 * whose {@code TimestampUtils.toString(LocalDateTime)} <em>attaches the default time zone</em> (converting
 * through {@code OffsetDateTime}) and emits a string like {@code 2026-07-10 09:31:09.29287+00}. GaussDB M
 * mode {@code datetime(6)} rejects values carrying a time-zone offset ("Incorrect datetime value"), so
 * inserting a {@code LocalDateTime} with fractional seconds fails. {@code setTimestamp(int, Timestamp)}
 * instead routes through {@code TimestampUtils.toString(Calendar, Timestamp)} which emits
 * {@code 2026-07-10 09:31:09.292870} (no offset) and is accepted. {@code OffsetDateTime}, {@code LocalDate}
 * and {@code LocalTime} columns are not affected (OffsetDateTime goes through a separate accepted path;
 * LocalDate/LocalTime have no offset), so only the {@code LOCAL_DATE_TIME} descriptor is overridden.
 *
 * <p>Registered globally (both A and M mode): {@code getTimestamp}/{@code setTimestamp(Timestamp)} read
 * {@code TIMESTAMP} columns correctly under the openGauss PG kernel (A mode) as well, so A mode is
 * constructively unaffected.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on GaussDBLocalDateJdbcType.
 */
public class GaussDBLocalDateTimeJdbcType extends LocalDateTimeJdbcType {
	public static final GaussDBLocalDateTimeJdbcType INSTANCE = new GaussDBLocalDateTimeJdbcType();

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new GaussDBLocalDateTimeExtractor<>( javaType, this );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new GaussDBLocalDateTimeBinder<>( javaType, this );
	}

	private static class GaussDBLocalDateTimeExtractor<X> extends BasicExtractor<X> {
		private GaussDBLocalDateTimeExtractor(JavaType<X> javaType, GaussDBLocalDateTimeJdbcType jdbcType) {
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

	private static class GaussDBLocalDateTimeBinder<X> extends BasicBinder<X> {
		private GaussDBLocalDateTimeBinder(JavaType<X> javaType, GaussDBLocalDateTimeJdbcType jdbcType) {
			super( javaType, jdbcType );
		}

		@Override
		protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
			final LocalDateTime localDateTime = getJavaType().unwrap( value, LocalDateTime.class, options );
			st.setTimestamp( index, Timestamp.valueOf( localDateTime ) );
		}

		@Override
		protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
			final LocalDateTime localDateTime = getJavaType().unwrap( value, LocalDateTime.class, options );
			st.setTimestamp( name, Timestamp.valueOf( localDateTime ) );
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
