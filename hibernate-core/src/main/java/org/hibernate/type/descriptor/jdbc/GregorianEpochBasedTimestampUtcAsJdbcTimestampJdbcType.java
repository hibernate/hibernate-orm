/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import jakarta.persistence.TemporalType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterTemporal;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Descriptor for {@link SqlTypes#TIMESTAMP_UTC TIMESTAMP_UTC} handling.
 *
 * @author Christian Beikov
 */
public class GregorianEpochBasedTimestampUtcAsJdbcTimestampJdbcType implements JdbcType {

	public static final GregorianEpochBasedTimestampUtcAsJdbcTimestampJdbcType INSTANCE = new GregorianEpochBasedTimestampUtcAsJdbcTimestampJdbcType();
	private static final Calendar UTC_CALENDAR = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );

	public GregorianEpochBasedTimestampUtcAsJdbcTimestampJdbcType() {
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
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().getDescriptor( Instant.class );
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
//				final Instant instant = javaType.unwrap( value, Instant.class, options );
				st.setTimestamp( index, getBindValue( value, options ), UTC_CALENDAR );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
//				final Instant instant = javaType.unwrap( value, Instant.class, options );
				st.setTimestamp( name, getBindValue( value, options ), UTC_CALENDAR );
			}

			@Override
			public Timestamp getBindValue(X value, WrapperOptions options) {
				final Timestamp timestamp = javaType.unwrap( value, Timestamp.class, options );
				if ( timestamp.getTime() < DateTimeUtils.GREGORIAN_START_EPOCH_MILLIS ) {
					final long epochSecond =
							DateTimeUtils.toLocalDateTime( timestamp ).toEpochSecond( ZoneOffset.UTC );
					return new Timestamp( epochSecond * 1000 );
				}
				else {
					return timestamp;
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getExtractValue( rs.getTimestamp( paramIndex, UTC_CALENDAR ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getExtractValue( statement.getTimestamp( index, UTC_CALENDAR ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getExtractValue( statement.getTimestamp( name, UTC_CALENDAR ), options );
			}

			private X getExtractValue(Timestamp value, WrapperOptions options) {
				if ( value != null && value.getTime() < DateTimeUtils.GREGORIAN_START_EPOCH_MILLIS ) {
					final Timestamp julianTimestamp = Timestamp.valueOf(
							Instant.ofEpochMilli( value.getTime() ).atOffset( ZoneOffset.UTC ).toLocalDateTime()
					);
					return javaType.wrap( julianTimestamp, options );
				}
				else {
					return javaType.wrap( value, options );
				}
			}
		};
	}
}
