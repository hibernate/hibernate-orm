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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Calendar;

/**
 * Special descriptor for {@link Types#DATE DATE} handling where the driver interprets the epoch milliseconds
 * as being Gregorian calendar based. java.util.Date uses the Julian calendar for dates before 15th October 1582 which
 * leads to different dates for the same epoch when compared to the Gregorian calendar.
 * Workaround this problem by converting Julian to Gregorian epoch milliseconds in bind and extract.
 */
public class GregorianEpochBasedDateJdbcType extends DateJdbcType {
	public static final GregorianEpochBasedDateJdbcType INSTANCE = new GregorianEpochBasedDateJdbcType();

	public GregorianEpochBasedDateJdbcType() {
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Date date = getBindValue( value, options );
				if ( value instanceof Calendar calendar ) {
					st.setDate( index, date, calendar );
				}
				else {
					st.setDate( index, date );
				}
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Date date = getBindValue( value, options );
				if ( value instanceof Calendar calendar ) {
					st.setDate( name, date, calendar );
				}
				else {
					st.setDate( name, date );
				}
			}

			@Override
			public Date getBindValue(X value, WrapperOptions options) {
				final Date date = javaType.unwrap( value, Date.class, options );
				if ( date.getTime() < DateTimeUtils.GREGORIAN_START_EPOCH_MILLIS ) {
					final long epochSecond =
							DateTimeUtils.toLocalDate( date ).toEpochSecond( LocalTime.MIN, ZoneOffset.UTC );
					return new java.sql.Date( epochSecond * 1000 );
				}
				else {
					return date;
				}
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getExtractValue( rs.getDate( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getExtractValue( statement.getDate( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getExtractValue( statement.getDate( name ), options );
			}

			private X getExtractValue(Date value, WrapperOptions options) {
				if ( value != null && value.getTime() < DateTimeUtils.GREGORIAN_START_EPOCH_MILLIS ) {
					final Date julianDate = Date.valueOf(
							Instant.ofEpochMilli( value.getTime() ).atOffset( ZoneOffset.UTC ).toLocalDate()
					);
					return javaType.wrap( julianDate, options );
				}
				else {
					return javaType.wrap( value, options );
				}
			}
		};
	}
}
