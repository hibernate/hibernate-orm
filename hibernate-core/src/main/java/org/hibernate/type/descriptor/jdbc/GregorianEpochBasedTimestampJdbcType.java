/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.type.descriptor.DateTimeUtils;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Calendar;

/**
 * Special descriptor for {@link Types#TIMESTAMP TIMESTAMP} handling where the driver interprets the epoch milliseconds
 * as being Gregorian calendar based. java.util.Date uses the Julian calendar for dates before 15th October 1582 which
 * leads to different dates for the same epoch when compared to the Gregorian calendar.
 * Workaround this problem by converting Julian to Gregorian epoch milliseconds in bind and extract.
 */
public class GregorianEpochBasedTimestampJdbcType extends TimestampJdbcType {
	public static final GregorianEpochBasedTimestampJdbcType INSTANCE = new GregorianEpochBasedTimestampJdbcType();

	public GregorianEpochBasedTimestampJdbcType() {
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Timestamp timestamp = getBindValue( value, options );
				if ( value instanceof Calendar calendar ) {
					st.setTimestamp( index, timestamp, calendar );
				}
				else if ( options.getJdbcTimeZone() != null ) {
					st.setTimestamp( index, timestamp, Calendar.getInstance( options.getJdbcTimeZone() ) );
				}
				else {
					st.setTimestamp( index, timestamp );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Timestamp timestamp = getBindValue( value, options );
				if ( value instanceof Calendar calendar ) {
					st.setTimestamp( name, timestamp, calendar );
				}
				else if ( options.getJdbcTimeZone() != null ) {
					st.setTimestamp( name, timestamp, Calendar.getInstance( options.getJdbcTimeZone() ) );
				}
				else {
					st.setTimestamp( name, timestamp );
				}
			}

			@Override
			public Timestamp getBindValue(X value, WrapperOptions options) {
				final Timestamp timestamp = javaType.unwrap( value, Timestamp.class, options );
				if ( value instanceof Calendar ) {
					return timestamp;
				}
				else if ( timestamp.getTime() < DateTimeUtils.GREGORIAN_START_EPOCH_MILLIS ) {
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
				return options.getJdbcTimeZone() != null ?
						getExtractValue( rs.getTimestamp( paramIndex, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						getExtractValue( rs.getTimestamp( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						getExtractValue( statement.getTimestamp( index, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						getExtractValue( statement.getTimestamp( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return options.getJdbcTimeZone() != null ?
						getExtractValue( statement.getTimestamp( name, Calendar.getInstance( options.getJdbcTimeZone() ) ), options ) :
						getExtractValue( statement.getTimestamp( name ), options );
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
