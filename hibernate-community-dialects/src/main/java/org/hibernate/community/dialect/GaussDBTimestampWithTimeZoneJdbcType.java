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
import java.util.Calendar;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneJdbcType;

/**
 * GaussDB-specific {@link TimestampWithTimeZoneJdbcType} for {@code @JdbcTypeCode(TIMESTAMP_WITH_TIMEZONE)}
 * (JDBC type code {@link Types#TIMESTAMP_WITH_TIMEZONE}). Reads {@code DATETIME}/{@code TIMESTAMP}
 * columns via {@link ResultSet#getTimestamp(int, Calendar)} and writes via
 * {@link PreparedStatement#setTimestamp(int, Timestamp, Calendar)} instead of
 * {@code getObject}/{@code setObject(int, OffsetDateTime, TIMESTAMP_WITH_TIMEZONE)}.
 *
 * <h3>Why a separate descriptor</h3>
 * The default {@code TimestampWithTimeZoneJdbcType} binds through
 * {@code setObject(int, OffsetDateTime, Types.TIMESTAMP_WITH_TIMEZONE)}, which gsjdbc4 turns into a
 * {@code timestamp with time zone} expression. M mode {@code datetime(6)} columns reject that with
 * "column ... is of type datetime but expression is of type timestamp with time zone". Crucially the
 * {@code setObject} call itself does <em>not</em> throw &mdash; the error surfaces only when the
 * statement executes &mdash; so the built-in {@code setTimestamp} fallback in
 * {@code TimestampWithTimeZoneJdbcType} never runs. This descriptor skips {@code setObject} and binds
 * through {@code setTimestamp} directly, honoring {@link WrapperOptions#getJdbcTimeZone()} (and
 * {@link Calendar} values) exactly like the built-in fallback.
 *
 * <p>{@code OFFSET_DATE_TIME} ({@link GaussDBOffsetDateTimeJdbcType}) and {@code INSTANT}
 * ({@link GaussDBInstantJdbcType}) have their own descriptors; this one covers the remaining
 * {@code TIMESTAMP_WITH_TIMEZONE} code (2014) used by {@code @JdbcTypeCode(TIMESTAMP_WITH_TIMEZONE)}.
 *
 * <p>Registered globally (both A and M mode): {@code getTimestamp}/{@code setTimestamp} read/write
 * {@code timestamptz} columns correctly under the openGauss PG kernel (A mode) as well, so A mode
 * is constructively unaffected.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on TimestampWithTimeZoneJdbcType and GaussDBOffsetDateTimeJdbcType.
 */
public class GaussDBTimestampWithTimeZoneJdbcType extends TimestampWithTimeZoneJdbcType {
	public static final GaussDBTimestampWithTimeZoneJdbcType INSTANCE = new GaussDBTimestampWithTimeZoneJdbcType();

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new GaussDBTimestampWithTimeZoneBinder<>( javaType, this );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new GaussDBTimestampWithTimeZoneExtractor<>( javaType, this );
	}

	private static Calendar jdbcCalendar(WrapperOptions options) {
		return options.getJdbcTimeZone() != null ? Calendar.getInstance( options.getJdbcTimeZone() ) : null;
	}

	private static class GaussDBTimestampWithTimeZoneBinder<X> extends BasicBinder<X> {
		private GaussDBTimestampWithTimeZoneBinder(JavaType<X> javaType, GaussDBTimestampWithTimeZoneJdbcType jdbcType) {
			super( javaType, jdbcType );
		}

		@Override
		protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
			final Timestamp timestamp = getJavaType().unwrap( value, Timestamp.class, options );
			if ( value instanceof Calendar calendar ) {
				st.setTimestamp( index, timestamp, calendar );
			}
			else {
				final Calendar cal = jdbcCalendar( options );
				if ( cal != null ) {
					st.setTimestamp( index, timestamp, cal );
				}
				else {
					st.setTimestamp( index, timestamp );
				}
			}
		}

		@Override
		protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
			final Timestamp timestamp = getJavaType().unwrap( value, Timestamp.class, options );
			if ( value instanceof Calendar calendar ) {
				st.setTimestamp( name, timestamp, calendar );
			}
			else {
				final Calendar cal = jdbcCalendar( options );
				if ( cal != null ) {
					st.setTimestamp( name, timestamp, cal );
				}
				else {
					st.setTimestamp( name, timestamp );
				}
			}
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

	private static class GaussDBTimestampWithTimeZoneExtractor<X> extends BasicExtractor<X> {
		private GaussDBTimestampWithTimeZoneExtractor(JavaType<X> javaType, GaussDBTimestampWithTimeZoneJdbcType jdbcType) {
			super( javaType, jdbcType );
		}

		@Override
		protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
			final Calendar cal = jdbcCalendar( options );
			return getJavaType().wrap( cal != null ? rs.getTimestamp( paramIndex, cal ) : rs.getTimestamp( paramIndex ), options );
		}

		@Override
		protected X doExtract(CallableStatement statement, int paramIndex, WrapperOptions options) throws SQLException {
			final Calendar cal = jdbcCalendar( options );
			return getJavaType().wrap( cal != null ? statement.getTimestamp( paramIndex, cal ) : statement.getTimestamp( paramIndex ), options );
		}

		@Override
		protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
			final Calendar cal = jdbcCalendar( options );
			return getJavaType().wrap( cal != null ? statement.getTimestamp( name, cal ) : statement.getTimestamp( name ), options );
		}
	}
}
