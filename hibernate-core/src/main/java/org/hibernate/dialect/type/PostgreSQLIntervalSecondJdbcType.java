/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import org.postgresql.util.PGInterval;

/**
 * @author Christian Beikov
 */
public class PostgreSQLIntervalSecondJdbcType implements AdjustableJdbcType {
	private static final long SECONDS_PER_DAY = 86400;
	private static final long SECONDS_PER_HOUR = 3600;
	private static final long SECONDS_PER_MINUTE = 60;

	@Override
	public int getJdbcTypeCode() {
		return Types.OTHER;
	}

	@Override
	public int getDefaultSqlTypeCode() {
		return SqlTypes.INTERVAL_SECOND;
	}

	@Override
	public boolean isComparable() {
		return true;
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Duration.class;
	}

	@Override
	public JdbcType resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<?> domainJtd) {
		final int scale;
		if ( indicators.getColumnScale() == JdbcTypeIndicators.NO_COLUMN_SCALE ) {
			scale = domainJtd.getDefaultSqlScale(
					indicators.getDialect(),
					this
			);
		}
		else {
			scale = indicators.getColumnScale();
		}
		if ( scale > 6 ) {
			// Since the maximum allowed scale on PostgreSQL is 6 (microsecond precision),
			// we have to switch to the numeric type if the value is greater
			return indicators.getJdbcType( indicators.resolveJdbcTypeCode( SqlTypes.NUMERIC ) );
		}
		return this;
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return (appender, value, dialect, wrapperOptions) ->
				dialect.appendIntervalLiteral( appender, javaType.unwrap( value, Duration.class, wrapperOptions ) );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new BasicBinder<>( javaType, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final Duration duration = getJavaType().unwrap( value, Duration.class, options );
				st.setObject( index, constructInterval( duration ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Duration duration = getJavaType().unwrap( value, Duration.class, options );
				st.setObject( name, constructInterval( duration ) );
			}

			private Object constructInterval(Duration d) {
				long secondsLong = d.getSeconds();
				long minutesLong = secondsLong / 60;
				long hoursLong = minutesLong / 60;
				long daysLong = hoursLong / 24;
				int days = Math.toIntExact( daysLong );
				int hours = (int) ( hoursLong - daysLong * 24 );
				int minutes = (int) ( minutesLong - hoursLong * 60 );
				double seconds = ( (double) ( secondsLong - minutesLong * 60 ) )
						+ ( (double) d.getNano() ) / 1_000_000_000d;

				return new PGInterval(
						0,// years
						0, // months
						days,
						hours,
						minutes,
						seconds
				);
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( getValue( rs.getObject( paramIndex ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaType().wrap( getValue( statement.getObject( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaType().wrap( getValue( statement.getObject( name ) ), options );
			}

			private Object getValue(Object value) {
				if ( value instanceof PGInterval interval ) {
					final long seconds = ( (long) interval.getSeconds() )
							+ SECONDS_PER_DAY * ( (long) interval.getDays() )
							+ SECONDS_PER_HOUR * ( (long) interval.getHours() )
							+ SECONDS_PER_MINUTE * ( (long) interval.getMinutes() );
					final long nanos = 1000L * ( (long) interval.getMicroSeconds() );

					return Duration.ofSeconds( seconds, nanos );
				}
				return value;
			}
		};
	}
}
