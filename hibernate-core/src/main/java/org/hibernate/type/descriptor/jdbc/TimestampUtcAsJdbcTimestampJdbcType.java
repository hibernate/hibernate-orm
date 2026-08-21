/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
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
 * Descriptor for {@link SqlTypes#TIMESTAMP_UTC TIMESTAMP_UTC} handling.
 *
 * @author Christian Beikov
 */
public class TimestampUtcAsJdbcTimestampJdbcType implements JdbcType {

	public static final TimestampUtcAsJdbcTimestampJdbcType INSTANCE = new TimestampUtcAsJdbcTimestampJdbcType();
	private static final Calendar UTC_CALENDAR = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );

	public TimestampUtcAsJdbcTimestampJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TIMESTAMP;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.TIMESTAMP_UTC;
	}

	@Override
	public String getFriendlyName() {
		return "TIMESTAMP_UTC";
	}

	@Override
	public String toString() {
		return "TimestampUtcDescriptor";
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Instant.class );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Instant.class;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return new JdbcLiteralFormatterTemporal<>( javaType, TemporalType.TIMESTAMP );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Instant instant = javaType.unwrap( value, Instant.class, options );
				st.setTimestamp( index, Timestamp.from( instant ), UTC_CALENDAR );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Instant instant = javaType.unwrap( value, Instant.class, options );
				st.setTimestamp( name, Timestamp.from( instant ), UTC_CALENDAR );
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				final Timestamp timestamp = rs.getTimestamp( paramIndex, UTC_CALENDAR );
				return javaType.wrap( timestamp == null ? null : timestamp.toInstant(), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				final Timestamp timestamp = statement.getTimestamp( index, UTC_CALENDAR );
				return javaType.wrap( timestamp == null ? null : timestamp.toInstant(), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				final Timestamp timestamp = statement.getTimestamp( name, UTC_CALENDAR );
				return javaType.wrap( timestamp == null ? null : timestamp.toInstant(), options );
			}
		};
	}
}
