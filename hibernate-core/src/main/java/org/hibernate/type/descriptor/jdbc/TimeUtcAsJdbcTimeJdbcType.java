/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * Descriptor for {@link SqlTypes#TIME_UTC TIME_UTC} handling.
 *
 * @author Christian Beikov
 */
public class TimeUtcAsJdbcTimeJdbcType implements JdbcType {

	public static final TimeUtcAsJdbcTimeJdbcType INSTANCE = new TimeUtcAsJdbcTimeJdbcType();
	private static final Calendar UTC_CALENDAR = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );

	public TimeUtcAsJdbcTimeJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIME;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.TIME_UTC;
	}

	@Override
	public String getFriendlyName() {
		return "TIME_UTC";
	}

	@Override
	public String toString() {
		return "TimeUtcDescriptor";
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().getDescriptor( OffsetTime.class );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return OffsetTime.class;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIME );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final OffsetTime offsetTime = javaType.unwrap( value, OffsetTime.class, options );
				st.setTime( index, Time.valueOf( offsetTime.withOffsetSameInstant( ZoneOffset.UTC ).toLocalTime() ), UTC_CALENDAR );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final OffsetTime offsetTime = javaType.unwrap( value, OffsetTime.class, options );
				st.setTime( name, Time.valueOf( offsetTime.withOffsetSameInstant( ZoneOffset.UTC ).toLocalTime() ), UTC_CALENDAR );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				final Time time = rs.getTime( paramIndex, UTC_CALENDAR );
				return javaType.wrap( time == null ? null : time.toLocalTime().atOffset( ZoneOffset.UTC ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				final Time time = statement.getTime( index, UTC_CALENDAR );
				return javaType.wrap( time == null ? null : time.toLocalTime().atOffset( ZoneOffset.UTC ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				final Time time = statement.getTime( name, UTC_CALENDAR );
				return javaType.wrap( time == null ? null : time.toLocalTime().atOffset( ZoneOffset.UTC ), options );
			}
		};
	}
}
